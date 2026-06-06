package com.pokemon.tcg.service;

import com.pokemon.tcg.model.Card;
import com.pokemon.tcg.model.Jugador;
import com.pokemon.tcg.model.Mazo;
import com.pokemon.tcg.repository.JugadorRepository;
import com.pokemon.tcg.repository.MazoRepository;
import com.pokemon.tcg.repository.CardRepository;
import com.pokemon.tcg.model.battle.*;
import com.pokemon.tcg.model.battle.state.*;
import com.pokemon.tcg.service.battle.command.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.List;
import java.util.Random;
import java.util.Map;
import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class BattleEngineService {

    private final JugadorRepository jugadorRepo;
    private final MazoRepository mazoRepo;
    private final CardRepository cardRepo;
    private final Random random = new Random();
    private static final long ONLINE_DISCONNECT_TIMEOUT_MS = 30_000L;
    private final Map<String, Partida> partidasEnCurso = new ConcurrentHashMap<>();
    private final BotAIService botAIService;
    private final BattleAttackService battleAttackService;
    private final BattleKoService battleKoService;

    public BattleEngineService(JugadorRepository jugadorRepo, MazoRepository mazoRepo,
                               CardRepository cardRepo, BotAIService botAIService,
                               BattleAttackService battleAttackService, BattleKoService battleKoService) {
        this.jugadorRepo = jugadorRepo;
        this.mazoRepo = mazoRepo;
        this.cardRepo = cardRepo;
        this.botAIService = botAIService;
        this.battleAttackService = battleAttackService;
        this.battleKoService = battleKoService;
    }

    private void ejecutarComando(Partida partida, ComandoTurno comando) {
        if (!partida.getEstado().permiteAccionesDeJuego()) {
            throw new IllegalStateException("Acción no permitida en fase " + partida.getFaseActual() + ": " + comando.getNombre());
        }
        if (!comando.puedeEjecutar(partida)) {
            throw new IllegalStateException("Acción no permitida en el estado actual: " + comando.getNombre());
        }
        comando.ejecutar(partida);
    }

    public Partida startBattle(String username, Long mazoId) {
        Jugador jugador = jugadorRepo.findByUsername(username);
        if (jugador == null) throw new IllegalArgumentException("Jugador no encontrado: " + username);

        Mazo mazoSeleccionado = mazoRepo.findById(mazoId)
                .orElseThrow(() -> new IllegalArgumentException("Mazo no encontrado: " + mazoId));

        List<Card> cartasMazo = mazoSeleccionado.getCartas();
        if (cartasMazo.size() < 60)
            throw new IllegalStateException("El mazo debe tener 60 cartas. Tiene: " + cartasMazo.size());

        TableroJugador tableroJugador = new TableroJugador();
        TableroJugador tableroBot = new TableroJugador();

        List<Card> mazoJugador = new ArrayList<>(cartasMazo);
        Collections.shuffle(mazoJugador);
        tableroJugador.setMazo(mazoJugador);

        List<Card> mazoBot = new ArrayList<>(cartasMazo);
        Collections.shuffle(mazoBot);
        tableroBot.setMazo(mazoBot);

        int mulligansJugador = prepararManoConMulligan(tableroJugador);
        int mulligansBot = prepararManoConMulligan(tableroBot);

        robarCartas(tableroJugador, mulligansBot);
        robarCartas(tableroBot, mulligansJugador);

        prepararPremios(tableroJugador);
        prepararPremios(tableroBot);

        Partida partida = new Partida(tableroJugador, tableroBot);
        partida.setJugadorUsername(username);
        partida.setCoinFlipCallerUsername(username);
        partida.setMulligansJugador(mulligansJugador);
        partida.setMulligansBot(mulligansBot);
        partida.transicionarA(new EstadoLanzamientoMoneda());
        long now = System.currentTimeMillis();
        partida.setJugadorLastSeenAt(now);
        partida.setBotLastSeenAt(now);

        partidasEnCurso.put(partida.getId(), partida);
        System.out.println("✅ Partida creada con ID: " + partida.getId());
        return partida;
    }

    private int prepararManoConMulligan(TableroJugador tablero) {
        int mulligans = 0;
        int intentos = 0;

        do {
            tablero.getMazo().addAll(tablero.getMano());
            tablero.getMano().clear();
            Collections.shuffle(tablero.getMazo());
            robarCartas(tablero, 7);

            if (tienePokemonBasico(tablero)) return mulligans;

            mulligans++;
            intentos++;
        } while (intentos < 20);

        throw new IllegalStateException("El mazo no pudo generar una mano inicial con Pokemon Basico.");
    }

    private void prepararPremios(TableroJugador tablero) {
        for (int i = 0; i < 6; i++) {
            if (!tablero.getMazo().isEmpty())
                tablero.getPremios().add(tablero.getMazo().remove(0));
        }
    }

    private void robarCartas(TableroJugador tablero, int cantidad) {
        for (int i = 0; i < cantidad && !tablero.getMazo().isEmpty(); i++) {
            tablero.getMano().add(tablero.getMazo().remove(0));
        }
    }

    public synchronized Partida lanzarMoneda(String matchId, String callerUsername, String eleccion) {
        Partida partida = getPartidaOThrow(matchId);
        if (!partida.isCoinFlipped()) {
            String callerAutorizado = partida.getCoinFlipCallerUsername();
            if (partida.getBotUsername() != null && callerAutorizado != null && !callerAutorizado.equals(callerUsername)) {
                throw new IllegalStateException("El sorteo lo debe lanzar " + callerAutorizado + ".");
            }

            String resultado = random.nextBoolean() ? "CARA" : "CRUZ";
            String eleccionNormalizada = "CRUZ".equalsIgnoreCase(eleccion) ? "CRUZ" : "CARA";
            boolean callerWon = resultado.equals(eleccionNormalizada);
            String oponente = callerUsername != null && callerUsername.equals(partida.getJugadorUsername())
                    ? partida.getBotUsername()
                    : partida.getJugadorUsername();
            if (oponente == null || oponente.isBlank()) {
                oponente = "BOT";
            }

            partida.setCoinFlipped(true);
            partida.setCoinFlipResult(resultado);
            partida.setCoinFlipWinner(callerWon ? callerUsername : oponente);

            if (partida.getBotUsername() == null) {
                partida.transicionarA(new EstadoTurnoNormal());
            }
        }
        return partida;
    }

    public synchronized Partida actualizarHandshakeMoneda(String matchId, String username, boolean holding, int power) {
        Partida partida = getPartidaOThrow(matchId);
        if (partida.getFaseActual() != Partida.Fase.LANZAMIENTO_MONEDA || partida.isCoinFlipped()) {
            return partida;
        }

        int clampedPower = Math.max(0, Math.min(100, power));
        if (username != null && username.equals(partida.getJugadorUsername())) {
            partida.setCoinHandshakeJugadorHolding(holding);
            partida.setCoinHandshakeJugadorPower(clampedPower);
        } else if (username != null && username.equals(partida.getBotUsername())) {
            partida.setCoinHandshakeBotHolding(holding);
            partida.setCoinHandshakeBotPower(clampedPower);
        }

        if (partida.getBotUsername() == null) {
            partida.setCoinHandshakeBotHolding(true);
            partida.setCoinHandshakeBotPower(100);
        }

        partida.setCoinHandshakeComplete(
                partida.getCoinHandshakeJugadorPower() >= 100 && partida.getCoinHandshakeBotPower() >= 100
        );
        return partida;
    }

    public void elegirTurno(String matchId, boolean vaPrimero, String callerUsername) {
        Partida partida = getPartidaOThrow(matchId);
        if (partida.getBotUsername() == null) {
            if (vaPrimero) {
                partida.setTurnoActual(Partida.Turno.JUGADOR);
            } else {
                partida.setTurnoActual(Partida.Turno.BOT);
                ejecutarTurnoBot(matchId);
            }
            partida.transicionarA(new EstadoTurnoNormal());
        } else {
            if (partida.getFaseActual() == Partida.Fase.TURNO_NORMAL) {
                return;
            }
            if (partida.getCoinFlipWinner() != null && !partida.getCoinFlipWinner().equals(callerUsername)) {
                return;
            }
            if (vaPrimero) {
                partida.setTurnoActual(callerUsername.equals(partida.getJugadorUsername()) ? Partida.Turno.JUGADOR : Partida.Turno.BOT);
            } else {
                partida.setTurnoActual(callerUsername.equals(partida.getJugadorUsername()) ? Partida.Turno.BOT : Partida.Turno.JUGADOR);
            }
            if (partida.getTurnoActual() == Partida.Turno.JUGADOR) {
                robarCarta(partida.getJugador());
            } else {
                robarCarta(partida.getBot());
            }
            partida.transicionarA(new EstadoTurnoNormal());
        }
    }

    public Partida getEstadoPartida(String matchId) {
        return getEstadoPartida(matchId, null);
    }

    public synchronized Partida getEstadoPartida(String matchId, String username) {
        Partida partida = partidasEnCurso.get(matchId);
        if (partida == null) return null;
        evaluarDesconexionOnline(partida, username);
        registrarHeartbeat(partida, username);
        return partida;
    }

    public synchronized Partida registrarHeartbeat(String matchId, String username) {
        Partida partida = partidasEnCurso.get(matchId);
        if (partida == null) return null;
        evaluarDesconexionOnline(partida, username);
        registrarHeartbeat(partida, username);
        return partida;
    }

    private void registrarHeartbeat(Partida partida, String username) {
        if (username == null || partida.getFaseActual() == Partida.Fase.FIN_PARTIDA) return;
        long now = System.currentTimeMillis();
        if (username.equals(partida.getJugadorUsername())) {
            partida.setJugadorLastSeenAt(now);
        } else if (username.equals(partida.getBotUsername())) {
            partida.setBotLastSeenAt(now);
        }
    }

    private void evaluarDesconexionOnline(Partida partida, String callerUsername) {
        if (partida == null
                || partida.getBotUsername() == null
                || partida.getFaseActual() == Partida.Fase.FIN_PARTIDA) {
            return;
        }

        long now = System.currentTimeMillis();
        boolean jugadorAusente = now - partida.getJugadorLastSeenAt() > ONLINE_DISCONNECT_TIMEOUT_MS;
        boolean botAusente = now - partida.getBotLastSeenAt() > ONLINE_DISCONNECT_TIMEOUT_MS;

        if (jugadorAusente && !partida.getJugadorUsername().equals(callerUsername)) {
            cerrarPorDesconexion(partida, partida.getBotUsername());
        } else if (botAusente && !partida.getBotUsername().equals(callerUsername)) {
            cerrarPorDesconexion(partida, partida.getJugadorUsername());
        }
    }

    private void cerrarPorDesconexion(Partida partida, String ganador) {
        partida.transicionarA(new EstadoFinPartida());
        partida.setGanador(ganador);
        partida.setRazonFinPartida("El rival se ha desconectado");
    }

    public synchronized Partida rendirse(String matchId, String username) {
        Partida partida = getPartidaOThrow(matchId);
        if (partida.getFaseActual() == Partida.Fase.FIN_PARTIDA) {
            return partida;
        }
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Jugador no identificado.");
        }

        String ganador;
        if (username.equals(partida.getJugadorUsername())) {
            ganador = partida.getBotUsername() != null ? partida.getBotUsername() : "BOT";
        } else if (username.equals(partida.getBotUsername())) {
            ganador = partida.getJugadorUsername();
        } else {
            throw new IllegalArgumentException("El jugador no pertenece a esta partida.");
        }

        partida.transicionarA(new EstadoFinPartida());
        partida.setGanador(ganador);
        partida.setRazonFinPartida("El jugador se ha rendido");
        return partida;
    }

    public void jugarPokemon(String matchId, String cartaId, String callerUsername) {
        Partida partida = getPartidaOThrow(matchId);
        validarTurno(partida, callerUsername);

        TableroJugador tablero = getTableroDeJugador(partida, callerUsername);
        Card carta = encontrarCartaEnMano(tablero, cartaId);

        if (carta == null) throw new IllegalArgumentException("La carta no está en tu mano.");
        if (!esPokemonBasico(carta)) throw new IllegalArgumentException("Solo podés bajar Pokémon básicos.");

        ejecutarComando(partida, new ComandoJugarPokemon(carta, tablero));
    }

    public void unirEnergia(String matchId, String cartaId, String energiaId, String callerUsername) {
        Partida partida = getPartidaOThrow(matchId);
        validarTurno(partida, callerUsername);

        TableroJugador tablero = getTableroDeJugador(partida, callerUsername);
        CartaEnJuego objetivo = encontrarCartaEnTablero(tablero, cartaId);
        Card energia = encontrarCartaEnMano(tablero, energiaId);

        if (objetivo == null) throw new IllegalArgumentException("Pokemon objetivo no encontrado.");
        if (energia == null || !esEnergia(energia)) throw new IllegalArgumentException("Energía no encontrada.");

        ejecutarComando(partida, new ComandoUnirEnergia(objetivo, energia, tablero));
    }

    public void realizarRetirada(String matchId, String nuevoActivoId, String callerUsername) {
        Partida partida = getPartidaOThrow(matchId);
        validarTurno(partida, callerUsername);
        TableroJugador tablero = getTableroDeJugador(partida, callerUsername);
        ejecutarComando(partida, new ComandoRetirarse(nuevoActivoId, tablero));
    }

    public void subirAActivoDesdeBanca(String matchId, String cartaIdEnBanca, String callerUsername) {
        Partida partida = getPartidaOThrow(matchId);
        validarTurno(partida, callerUsername);
        TableroJugador tablero = getTableroDeJugador(partida, callerUsername);
        ejecutarComando(partida, new ComandoSubirActivo(cartaIdEnBanca, tablero));
    }

    public void realizarAtaque(String matchId, String nombreAtaqueElegido, String callerUsername) {
        Partida partida = partidasEnCurso.get(matchId);
        if (partida == null) return;
        validarTurno(partida, callerUsername);

        TableroJugador tableroAtacante = getTableroDeJugador(partida, callerUsername);
        TableroJugador tableroDefensor = getTableroOponente(partida, callerUsername);

        ejecutarComando(partida, new ComandoAtacar(
                nombreAtaqueElegido, tableroAtacante, tableroDefensor,
                battleAttackService, battleKoService
        ));

        if (partida.getFaseActual() == Partida.Fase.FIN_PARTIDA) {
            System.out.println("🏆 Partida terminada por ataque.");
            return;
        }
        System.out.println("🔄 Ataque finalizado. Pasando turno al oponente...");
        this.pasarTurno(matchId, callerUsername);
    }

    private void aplicarMantenimientoEntreTurnos(Partida partida) {
        System.out.println("🔄 --- INICIANDO MANTENIMIENTO ENTRE TURNOS ---");

        procesarEstado(partida.getJugador(), partida.getBot(), partida);
        procesarEstado(partida.getBot(), partida.getJugador(), partida);

        System.out.println("🔄 --- FIN MANTENIMIENTO ---");
    }

    private void procesarEstado(TableroJugador dueno, TableroJugador rival, Partida partida) {
        CartaEnJuego activo = dueno.getActivo();
        if (activo == null) return;

        if (activo.getCondicionesEspeciales().contains("Poisoned")) {
            System.out.println("☠️ Veneno: " + activo.getCard().getNombre() + " recibe 10 de daño.");
            activo.setHpActual(Math.max(0, activo.getHpActual() - 10));
        }

        if (activo.getCondicionesEspeciales().contains("Burned")) {
            System.out.println("🔥 Quemadura: " + activo.getCard().getNombre() + " recibe 20 de daño.");
            activo.setHpActual(Math.max(0, activo.getHpActual() - 20));

            if (random.nextBoolean()) {
                System.out.println("🔥 ¡Salió CARA! " + activo.getCard().getNombre() + " se curó de la Quemadura.");
                activo.getCondicionesEspeciales().remove("Burned");
            } else {
                System.out.println("🔥 Salió CRUZ. " + activo.getCard().getNombre() + " sigue Quemado.");
            }
        }

        if (activo.getCondicionesEspeciales().contains("Asleep")) {
            if (random.nextBoolean()) {
                System.out.println("💤 ¡Salió CARA! " + activo.getCard().getNombre() + " se despertó.");
                activo.getCondicionesEspeciales().remove("Asleep");
            } else {
                System.out.println("💤 Salió CRUZ. " + activo.getCard().getNombre() + " sigue Dormido.");
            }
        }

        if (activo.getHpActual() <= 0) {
            System.out.println("💀 " + activo.getCard().getNombre() + " murió por un estado alterado.");
            battleKoService.resolverKO(partida, rival.getActivo(), activo);
        }
    }

    public void pasarTurno(String matchId, String callerUsername) {
        Partida partida = getPartidaOThrow(matchId);
        validarTurno(partida, callerUsername);

        TableroJugador jugador = getTableroDeJugador(partida, callerUsername);
        TableroJugador bot = getTableroOponente(partida, callerUsername);

        if (jugador.getActivo() == null && !jugador.getBanca().isEmpty()) {
            throw new IllegalStateException("Debés subir un Pokémon de tu banca a la posición Activa antes de terminar tu turno.");
        }

        // LIMPIEZA DE TU POKÉMON (PERO DEJAMOS EL ESCUDO PRENDIDO)
        if (jugador.getActivo() != null) {
            jugador.getActivo().getCondicionesEspeciales().remove("CantRetreat");
            jugador.getActivo().getCondicionesEspeciales().remove("Paralyzed");
            jugador.getActivo().setPuedeAtacar(true);
            // ❌ ACÁ YA NO SE APAGA TU ESCUDO
        }

        // 🚩 EL ESCUDO DEL OPONENTE SE APAGA ACÁ (Ya pasó tu turno para atacarlo)
        if (bot.getActivo() != null) {
            bot.getActivo().setInvulnerable(false);
        }

        aplicarMantenimientoEntreTurnos(partida);

        if (partida.getFaseActual() == Partida.Fase.FIN_PARTIDA) {
            System.out.println("🏆 Partida terminada durante mantenimiento.");
            return;
        }

        partida.setYaSeRetiroEsteTurno(false);

        if (partida.getBotUsername() == null) {
            partida.setTurnoActual(Partida.Turno.BOT);
            ejecutarTurnoBot(matchId);
        } else {
            if (partida.getTurnoActual() == Partida.Turno.JUGADOR) {
                partida.setTurnoActual(Partida.Turno.BOT);
                robarCarta(partida.getBot());
            } else {
                partida.setTurnoActual(Partida.Turno.JUGADOR);
                robarCarta(partida.getJugador());
            }
        }
    }




    public void ejecutarTurnoBot(String matchId) {
        Partida partida = partidasEnCurso.get(matchId);
        if (partida == null) return;

        robarCarta(partida.getBot());
        botAIService.ejecutarTurno(partida);

        if (partida.getFaseActual() == Partida.Fase.FIN_PARTIDA) {
            System.out.println("🏆 Partida terminada durante turno del bot.");
            return;
        }

        // LIMPIEZA DEL POKÉMON DEL BOT (PERO DEJAMOS SU ESCUDO PRENDIDO)
        if (partida.getBot().getActivo() != null) {
            partida.getBot().getActivo().setPuedeAtacar(true);
            partida.getBot().getActivo().getCondicionesEspeciales().remove("CantRetreat");
            partida.getBot().getActivo().getCondicionesEspeciales().remove("Paralyzed");
            // ❌ ACÁ YA NO SE APAGA EL ESCUDO DEL BOT
        }

        // 🚩 TU ESCUDO SE APAGA ACÁ (El bot ya jugó su turno y te intentó pegar)
        if (partida.getJugador().getActivo() != null) {
            partida.getJugador().getActivo().setInvulnerable(false);
        }

        aplicarMantenimientoEntreTurnos(partida);

        partida.setTurnoActual(Partida.Turno.JUGADOR);
        robarCarta(partida.getJugador());
    }
    private void robarCarta(TableroJugador tablero) {
        if (!tablero.getMazo().isEmpty())
            tablero.getMano().add(tablero.getMazo().remove(0));
    }

    private Partida getPartidaOThrow(String matchId) {
        Partida p = partidasEnCurso.get(matchId);
        if (p == null) throw new IllegalArgumentException("Partida no encontrada: " + matchId);
        return p;
    }

    public void validarTurno(Partida partida, String callerUsername) {
        if (partida.getBotUsername() == null) {
            if (partida.getTurnoActual() != Partida.Turno.JUGADOR) {
                throw new IllegalStateException("No es tu turno.");
            }
        } else {
            if (callerUsername == null) {
                throw new IllegalStateException("Se requiere usuario para partida online.");
            }
            if (callerUsername.equals(partida.getJugadorUsername())) {
                if (partida.getTurnoActual() != Partida.Turno.JUGADOR) {
                    throw new IllegalStateException("No es tu turno.");
                }
            } else if (callerUsername.equals(partida.getBotUsername())) {
                if (partida.getTurnoActual() != Partida.Turno.BOT) {
                    throw new IllegalStateException("No es tu turno.");
                }
            } else {
                throw new IllegalStateException("No eres parte de esta partida.");
            }
        }
    }

    public TableroJugador getTableroDeJugador(Partida partida, String callerUsername) {
        if (partida.getBotUsername() == null || callerUsername == null) {
            return partida.getJugador();
        }
        if (callerUsername.equals(partida.getJugadorUsername())) {
            return partida.getJugador();
        } else if (callerUsername.equals(partida.getBotUsername())) {
            return partida.getBot();
        }
        throw new IllegalArgumentException("Usuario no válido para esta partida.");
    }

    public TableroJugador getTableroOponente(Partida partida, String callerUsername) {
        if (partida.getBotUsername() == null || callerUsername == null) {
            return partida.getBot();
        }
        if (callerUsername.equals(partida.getJugadorUsername())) {
            return partida.getBot();
        } else if (callerUsername.equals(partida.getBotUsername())) {
            return partida.getJugador();
        }
        throw new IllegalArgumentException("Usuario no válido para esta partida.");
    }

    private Card encontrarCartaEnMano(TableroJugador tablero, String id) {
        return tablero.getMano().stream()
                .filter(c -> c.getId().equals(id))
                .findFirst().orElse(null);
    }

    private CartaEnJuego encontrarCartaEnTablero(TableroJugador tablero, String id) {
        if (tablero.getActivo() != null && tablero.getActivo().getCard() != null) {
            if (tablero.getActivo().getCard().getId().equals(id)) {
                return tablero.getActivo();
            }
        }

        return tablero.getBanca().stream()
                .filter(c -> c != null && c.getCard() != null)
                .filter(c -> c.getCard().getId().equals(id))
                .findFirst()
                .orElse(null);
    }

    private boolean esPokemonBasico(Card c) {
        if (c == null) return false;

        if (c.getSupertype() == null) {
            System.out.println("❌ ERROR: La carta " + c.getNombre() + " no tiene supertype.");
            return false;
        }

        String supertype = normalizarTexto(c.getSupertype());
        boolean esPokemon = "pokemon".equals(supertype);

        boolean esBasico = false;
        if (c.getSubtypes() != null) {
            for (String subtype : c.getSubtypes()) {
                if ("Basic".equalsIgnoreCase(subtype)) {
                    esBasico = true;
                    break;
                }
            }
        }

        return esPokemon && esBasico;
    }

    private boolean tienePokemonBasico(TableroJugador tablero) {
        return tablero.getMano().stream().anyMatch(this::esPokemonBasico);
    }

    private String normalizarTexto(String texto) {
        if (texto == null) return "";
        return Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase();
    }

    private boolean esEnergia(Card c) {
        if (c == null || c.getSupertype() == null) return false;
        return c.getSupertype().equalsIgnoreCase("Energy");
    }

    public void evolucionarPokemon(String matchId, String cartaManoId, String cartaTableroId, String callerUsername) {
        Partida partida = getPartidaOThrow(matchId);
        validarTurno(partida, callerUsername);

        TableroJugador tablero = getTableroDeJugador(partida, callerUsername);
        Card cartaEvolucion = encontrarCartaEnMano(tablero, cartaManoId);
        if (cartaEvolucion == null) throw new IllegalArgumentException("La carta de evolución no está en tu mano.");
        CartaEnJuego objetivo = encontrarCartaEnTablero(tablero, cartaTableroId);
        if (objetivo == null) throw new IllegalArgumentException("El Pokémon objetivo no está en tu tablero.");

        ejecutarComando(partida, new ComandoEvolucionar(cartaEvolucion, objetivo, tablero));
    }

    public Partida debugRobarCarta(String matchId, String cardId) {
        Partida partida = getPartidaOThrow(matchId);

        Card cartaMagica = cardRepo.findById(cardId)
                .orElseThrow(() -> new IllegalArgumentException("Carta no encontrada en DB: " + cardId));

        partida.getJugador().getMano().add(cartaMagica);
        System.out.println("🛠️ [GOD MODE] Carta inyectada a la mano: " + cartaMagica.getNombre());

        return partida;
    }

    public Partida debugForzarEstado(String matchId, String objetivo, String estado) {
        Partida partida = getPartidaOThrow(matchId);

        CartaEnJuego activo = objetivo.equals("BOT") ?
                partida.getBot().getActivo() :
                partida.getJugador().getActivo();

        if (activo != null) {
            activo.limpiarCondiciones();
            activo.agregarCondicion(estado);
            System.out.println("🛠️ [GOD MODE] Estado " + estado + " aplicado a " + activo.getCard().getNombre());
        }

        return partida;
    }

    public Partida debugSetHp(String matchId, String objetivo, int hp) {
        Partida partida = getPartidaOThrow(matchId);

        CartaEnJuego activoVictima = objetivo.equals("BOT") ? partida.getBot().getActivo() : partida.getJugador().getActivo();
        CartaEnJuego rivalAtacante = objetivo.equals("BOT") ? partida.getJugador().getActivo() : partida.getBot().getActivo();

        if (activoVictima != null) {
            activoVictima.setHpActual(Math.max(0, hp));
            System.out.println("🛠️ [GOD MODE] HP de " + activoVictima.getCard().getNombre() + " seteado a " + hp);

            if (activoVictima.getHpActual() <= 0 && rivalAtacante != null) {
                System.out.println("🛠️ [GOD MODE] Ejecutando Muerte Súbita...");
                battleKoService.resolverKO(partida, rivalAtacante, activoVictima);
            }
        }

        return partida;
    }

    public java.util.List<com.pokemon.tcg.model.Card> obtenerCatalogoCartasDebug() {
        return cardRepo.findAll();
    }

    public Partida startBattleOnline(String player1, Long player1MazoId, String player2, Long player2MazoId) {
        Jugador j1 = jugadorRepo.findByUsername(player1);
        if (j1 == null) throw new IllegalArgumentException("Jugador no encontrado: " + player1);
        Jugador j2 = jugadorRepo.findByUsername(player2);
        if (j2 == null) throw new IllegalArgumentException("Jugador no encontrado: " + player2);

        Mazo mazo1 = mazoRepo.findById(player1MazoId)
                .orElseThrow(() -> new IllegalArgumentException("Mazo no encontrado: " + player1MazoId));
        Mazo mazo2 = mazoRepo.findById(player2MazoId)
                .orElseThrow(() -> new IllegalArgumentException("Mazo no encontrado: " + player2MazoId));

        if (mazo1.getCartas().size() < 60 || mazo2.getCartas().size() < 60)
            throw new IllegalStateException("Los mazos deben tener 60 cartas.");

        TableroJugador tableroJugador = new TableroJugador();
        TableroJugador tableroBot = new TableroJugador();

        List<Card> m1 = new ArrayList<>(mazo1.getCartas());
        Collections.shuffle(m1);
        tableroJugador.setMazo(m1);

        List<Card> m2 = new ArrayList<>(mazo2.getCartas());
        Collections.shuffle(m2);
        tableroBot.setMazo(m2);

        int mulligansJugador = prepararManoConMulligan(tableroJugador);
        int mulligansBot = prepararManoConMulligan(tableroBot);

        robarCartas(tableroJugador, mulligansBot);
        robarCartas(tableroBot, mulligansJugador);

        prepararPremios(tableroJugador);
        prepararPremios(tableroBot);

        Partida partida = new Partida(tableroJugador, tableroBot);
        partida.setJugadorUsername(player1);
        partida.setBotUsername(player2);
        partida.setCoinFlipCallerUsername(player1);
        partida.setMulligansJugador(mulligansJugador);
        partida.setMulligansBot(mulligansBot);
        partida.transicionarA(new EstadoLanzamientoMoneda());
        long now = System.currentTimeMillis();
        partida.setJugadorLastSeenAt(now);
        partida.setBotLastSeenAt(now);

        partidasEnCurso.put(partida.getId(), partida);
        System.out.println("✅ Partida Online creada con ID: " + partida.getId());
        return partida;
    }
}
