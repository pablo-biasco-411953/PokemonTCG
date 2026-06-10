package com.pokemon.tcg.service;

import com.pokemon.tcg.model.Card;
import com.pokemon.tcg.model.CardAttribute;
import com.pokemon.tcg.model.Jugador;
import com.pokemon.tcg.model.Mazo;
import com.pokemon.tcg.repository.JugadorRepository;
import com.pokemon.tcg.repository.MazoRepository;
import com.pokemon.tcg.repository.CardRepository;
import com.pokemon.tcg.model.battle.*;
import com.pokemon.tcg.model.battle.Ataque;
import com.pokemon.tcg.model.battle.state.*;
import com.pokemon.tcg.service.battle.command.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    @Transactional
    public Partida startBattle(String username, Long mazoId) {
        return startBattle(username, mazoId, "NORMAL");
    }

    @Transactional
    public Partida startBattle(String username, Long mazoId, String botDifficulty) {
        Jugador jugador = jugadorRepo.findByUsername(username);
        if (jugador == null) throw new IllegalArgumentException("Jugador no encontrado: " + username);

        Mazo mazoSeleccionado = mazoRepo.findById(mazoId)
                .orElseThrow(() -> new IllegalArgumentException("Mazo no encontrado: " + mazoId));

        List<Card> cartasMazo = crearSnapshotMazo(mazoSeleccionado.getCartas());
        if (cartasMazo.size() < 60)
            throw new IllegalStateException("El mazo debe tener 60 cartas. Tiene: " + cartasMazo.size());

        TableroJugador tableroJugador = new TableroJugador();
        TableroJugador tableroBot = new TableroJugador();

        List<Card> mazoJugador = new ArrayList<>(cartasMazo);
        Collections.shuffle(mazoJugador);
        tableroJugador.setMazo(mazoJugador);

        List<Card> mazoBot = generarMazoBot(cartasMazo, botDifficulty);
        Collections.shuffle(mazoBot);
        tableroBot.setMazo(mazoBot);

        // Ya no preparamos la mano con mulligan automático, ni repartimos premios.
        // Solo sacamos 7 cartas (esto es temporal, si decidimos que la animación la hace el front, tal vez solo transicionemos).
        // Para que coincida con el backend, repartiremos 7 cartas a las manos ahora pero no los premios.
        robarCartas(tableroJugador, 7);
        robarCartas(tableroBot, 7);

        Partida partida = new Partida(tableroJugador, tableroBot);
        partida.setJugadorUsername(username);
        partida.setCoinFlipCallerUsername(username);
        partida.setMulligansJugador(0);
        partida.setMulligansBot(0);
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

        }
        return partida;
    }

    public synchronized Partida actualizarLoading(String matchId, String username, int percentage) {
        Partida partida = getPartidaOThrow(matchId);
        if (username != null && username.equals(partida.getJugadorUsername())) {
            partida.setJugadorLoadingPercentage(percentage);
        } else if (username != null && username.equals(partida.getBotUsername())) {
            partida.setBotLoadingPercentage(percentage);
        } else if (partida.getBotUsername() == null) {
            // Si es singleplayer, forzamos que el "bot" siempre acompañe el porcentaje del jugador (o vaya a 100).
            partida.setBotLoadingPercentage(100);
            partida.setJugadorLoadingPercentage(percentage);
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
            }
            partida.transicionarA(new EstadoSetupInitialDraw());
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
            
            partida.transicionarA(new EstadoSetupInitialDraw());
        }
    }

    public synchronized void evaluarSetupInitialDraw(String matchId, String username) {
        Partida partida = getPartidaOThrow(matchId);
        if (partida.getFaseActual() != Partida.Fase.SETUP_INITIAL_DRAW) return;

        if (username.equals(partida.getJugadorUsername())) {
            partida.setSetupJugadorListo(true);
        } else if (username.equals(partida.getBotUsername())) {
            partida.setSetupBotListo(true);
        }

        if (partida.getBotUsername() == null) {
            partida.setSetupBotListo(true);
        }

        if (partida.isSetupJugadorListo() && partida.isSetupBotListo()) {
            // Evaluar ambas manos
            boolean jugadorTieneBasico = tienePokemonBasico(partida.getJugador());
            boolean botTieneBasico = tienePokemonBasico(partida.getBot());

            if (!jugadorTieneBasico || !botTieneBasico) {
                partida.transicionarA(new EstadoSetupMulliganReveal());
            } else {
                partida.transicionarA(new EstadoSetupPlaceActive());
            }
            partida.setSetupJugadorListo(false);
            partida.setSetupBotListo(false);
        }
    }

    public synchronized void ejecutarMulligan(String matchId, String username) {
        Partida partida = getPartidaOThrow(matchId);
        if (partida.getFaseActual() != Partida.Fase.SETUP_MULLIGAN_REVEAL) return;

        if (username.equals(partida.getJugadorUsername())) {
            partida.setSetupJugadorListo(true);
        } else if (username.equals(partida.getBotUsername())) {
            partida.setSetupBotListo(true);
        }

        if (partida.getBotUsername() == null) {
            partida.setSetupBotListo(true);
        }

        if (partida.isSetupJugadorListo() && partida.isSetupBotListo()) {
            boolean jugadorMulligan = !tienePokemonBasico(partida.getJugador());
            boolean botMulligan = !tienePokemonBasico(partida.getBot());

            if (jugadorMulligan) {
                partida.setMulligansJugador(partida.getMulligansJugador() + 1);
                partida.getJugador().getMazo().addAll(partida.getJugador().getMano());
                partida.getJugador().getMano().clear();
                Collections.shuffle(partida.getJugador().getMazo());
                robarCartas(partida.getJugador(), 7);
            }
            
            if (botMulligan) {
                partida.setMulligansBot(partida.getMulligansBot() + 1);
                partida.getBot().getMazo().addAll(partida.getBot().getMano());
                partida.getBot().getMano().clear();
                Collections.shuffle(partida.getBot().getMazo());
                robarCartas(partida.getBot(), 7);
            }

            partida.setSetupJugadorListo(false);
            partida.setSetupBotListo(false);

            boolean jugadorTieneBasico = tienePokemonBasico(partida.getJugador());
            boolean botTieneBasico = tienePokemonBasico(partida.getBot());

            if (!jugadorTieneBasico || !botTieneBasico) {
                partida.transicionarA(new EstadoSetupMulliganReveal());
            } else {
                partida.transicionarA(new EstadoSetupPlaceActive());
            }
        }
    }

    public synchronized void colocarActivoSetup(String matchId, String username, String cartaId) {
        Partida partida = getPartidaOThrow(matchId);
        if (partida.getFaseActual() != Partida.Fase.SETUP_PLACE_ACTIVE) return;

        TableroJugador tablero = getTableroByUsername(partida, username);
        if (tablero.getActivo() != null) return; // Ya colocó

        Card carta = tablero.getMano().stream()
                .filter(c -> c.getId().equals(cartaId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Carta no en mano"));

        if (!esPokemonBasico(carta)) {
            throw new IllegalArgumentException("Debe ser un Pokémon Básico");
        }

        CartaEnJuego cartaEnJuego = new CartaEnJuego(carta);
        cartaEnJuego.setBocaAbajo(true);
        tablero.setActivo(cartaEnJuego);
        tablero.getMano().remove(carta);
        agregarLog(partida, "ACTIVE_PLACED", username);

        if (username.equals(partida.getJugadorUsername())) {
            partida.setSetupJugadorListo(true);
        } else if (username.equals(partida.getBotUsername())) {
            partida.setSetupBotListo(true);
        }

        if (partida.getBotUsername() == null) {
            partida.setSetupBotListo(true);
        }

        if (partida.isSetupJugadorListo() && partida.isSetupBotListo()) {
            partida.setSetupJugadorListo(false);
            partida.setSetupBotListo(false);
            partida.transicionarA(new EstadoSetupPlaceBench());
        }
    }

    public synchronized void colocarBancaSetup(String matchId, String username, String cartaId) {
        Partida partida = getPartidaOThrow(matchId);
        if (partida.getFaseActual() != Partida.Fase.SETUP_PLACE_BENCH && 
            partida.getFaseActual() != Partida.Fase.SETUP_PLACE_BENCH_EXTRA) return;

        TableroJugador tablero = getTableroByUsername(partida, username);
        if (tablero.getBanca().size() >= 5) return;

        Card carta = tablero.getMano().stream()
                .filter(c -> c.getId().equals(cartaId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Carta no en mano"));

        if (!esPokemonBasico(carta)) {
            throw new IllegalArgumentException("Debe ser un Pokémon Básico");
        }

        CartaEnJuego cartaEnJuego = new CartaEnJuego(carta);
        cartaEnJuego.setBocaAbajo(true);
        tablero.getBanca().add(cartaEnJuego);
        tablero.getMano().remove(carta);
        agregarLog(partida, "BENCH_PLACED", username);
    }

    public synchronized void confirmarBancaSetup(String matchId, String username) {
        Partida partida = getPartidaOThrow(matchId);
        if (partida.getFaseActual() != Partida.Fase.SETUP_PLACE_BENCH && 
            partida.getFaseActual() != Partida.Fase.SETUP_PLACE_BENCH_EXTRA) return;

        if (username.equals(partida.getJugadorUsername())) {
            partida.setSetupJugadorListo(true);
        } else if (username.equals(partida.getBotUsername())) {
            partida.setSetupBotListo(true);
        }

        if (partida.getBotUsername() == null) {
            partida.setSetupBotListo(true);
        }

        if (partida.isSetupJugadorListo() && partida.isSetupBotListo()) {
            partida.setSetupJugadorListo(false);
            partida.setSetupBotListo(false);
            if (partida.getFaseActual() == Partida.Fase.SETUP_PLACE_BENCH) {
                partida.transicionarA(new EstadoSetupPrizePlacement());
            } else {
                partida.transicionarA(new EstadoSetupReveal());
            }
        }
    }

    private void transicionarAExtraDraw(Partida partida) {
        int diffJugador = partida.getMulligansBot() - partida.getMulligansJugador();
        int diffBot = partida.getMulligansJugador() - partida.getMulligansBot();
        
        partida.setCartasMulliganExtraPendientesJugador(Math.max(0, diffJugador));
        partida.setCartasMulliganExtraPendientesBot(Math.max(0, diffBot));
        partida.setSetupJugadorRoboExtraMulligan(false);
        partida.setSetupBotRoboExtraMulligan(false);

        if (partida.getCartasMulliganExtraPendientesJugador() > 0 || partida.getCartasMulliganExtraPendientesBot() > 0) {
            partida.transicionarA(new EstadoSetupMulliganExtraDraw());
        } else {
            partida.transicionarA(new EstadoSetupReveal());
        }
    }

    public synchronized void resolverCartasExtra(String matchId, String username, int cantidad) {
        Partida partida = getPartidaOThrow(matchId);
        if (partida.getFaseActual() != Partida.Fase.SETUP_MULLIGAN_EXTRA_DRAW) return;

        if (username.equals(partida.getJugadorUsername())) {
            int max = partida.getCartasMulliganExtraPendientesJugador();
            int aRobar = Math.min(cantidad, max);
            robarCartas(partida.getJugador(), aRobar);
            partida.setSetupJugadorRoboExtraMulligan(aRobar > 0);
            partida.setCartasMulliganExtraPendientesJugador(0);
        } else if (username.equals(partida.getBotUsername())) {
            int max = partida.getCartasMulliganExtraPendientesBot();
            int aRobar = Math.min(cantidad, max);
            robarCartas(partida.getBot(), aRobar);
            partida.setSetupBotRoboExtraMulligan(aRobar > 0);
            partida.setCartasMulliganExtraPendientesBot(0);
        }

        if (partida.getBotUsername() == null) {
            int max = partida.getCartasMulliganExtraPendientesBot();
            robarCartas(partida.getBot(), max);
            partida.setSetupBotRoboExtraMulligan(max > 0);
            partida.setCartasMulliganExtraPendientesBot(0);
        }

        if (partida.getCartasMulliganExtraPendientesJugador() == 0 && partida.getCartasMulliganExtraPendientesBot() == 0) {
            // Solo quien tuvo derecho a robar por mulligan puede bajar basicos nuevos en esta ventana extra.
            boolean jugadorRoboExtra = partida.isSetupJugadorRoboExtraMulligan();
            boolean botRoboExtra = partida.isSetupBotRoboExtraMulligan();

            if ((jugadorRoboExtra && puedeColocarBasicosExtra(partida.getJugador())) ||
                (botRoboExtra && puedeColocarBasicosExtra(partida.getBot()))) {
                partida.setSetupJugadorListo(false);
                partida.setSetupBotListo(false);
                if (!jugadorRoboExtra) {
                    partida.setSetupJugadorListo(true);
                }
                if (!botRoboExtra) {
                    partida.setSetupBotListo(true);
                }
                partida.transicionarA(new EstadoSetupPlaceBenchExtra());
            } else {
                revelarSetupEIniciarTurno(partida);
            }
        }
    }

    public synchronized void colocarPremios(String matchId, String username) {
        Partida partida = getPartidaOThrow(matchId);
        if (partida.getFaseActual() != Partida.Fase.SETUP_PRIZE_PLACEMENT) return;

        if (username.equals(partida.getJugadorUsername())) {
            partida.setSetupJugadorListo(true);
        } else if (username.equals(partida.getBotUsername())) {
            partida.setSetupBotListo(true);
        }

        if (partida.getBotUsername() == null) {
            partida.setSetupBotListo(true);
        }

        if (partida.isSetupJugadorListo() && partida.isSetupBotListo()) {
            prepararPremios(partida.getJugador());
            prepararPremios(partida.getBot());
            agregarLog(partida, "PRIZES_PLACED", username);
            partida.setSetupJugadorListo(false);
            partida.setSetupBotListo(false);

            transicionarAExtraDraw(partida);
        }
    }

    public synchronized void confirmarRevealSetup(String matchId, String username) {
        Partida partida = getPartidaOThrow(matchId);
        if (partida.getFaseActual() != Partida.Fase.SETUP_REVEAL) return;

        if (username != null && username.equals(partida.getJugadorUsername())) {
            partida.setSetupJugadorListo(true);
        } else if (username != null && username.equals(partida.getBotUsername())) {
            partida.setSetupBotListo(true);
        }

        if (partida.getBotUsername() == null) {
            partida.setSetupBotListo(true);
        }

        if (partida.isSetupJugadorListo() && partida.isSetupBotListo()) {
            revelarSetupEIniciarTurno(partida);
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
        String desconectado = ganador != null && ganador.equals(partida.getJugadorUsername())
                ? partida.getBotUsername()
                : partida.getJugadorUsername();
        partida.transicionarA(new EstadoFinPartida());
        partida.setGanador(ganador);
        partida.setRazonFinPartida("El rival se ha desconectado");
        agregarLog(partida, "DISCONNECTED", desconectado);
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
        agregarLog(partida, "SURRENDERED", username);
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
        agregarLog(partida, "POKEMON_PLAYED", callerUsername, nombreCarta(carta));
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
        agregarLog(partida, "ENERGY_ATTACHED", callerUsername, nombreCarta(objetivo));
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
        if (partida.getNumeroTurno() <= 1) {
            throw new IllegalStateException("No se puede atacar en el primer turno.");
        }

        TableroJugador tableroAtacante = getTableroDeJugador(partida, callerUsername);
        TableroJugador tableroDefensor = getTableroOponente(partida, callerUsername);
        CartaEnJuego defensorAntes = tableroDefensor.getActivo();
        int hpDefensorAntes = defensorAntes != null ? defensorAntes.getHpActual() : 0;
        java.util.Set<String> condicionesAntes = defensorAntes == null
                ? java.util.Collections.emptySet()
                : new java.util.HashSet<>(defensorAntes.getCondicionesEspeciales());

        ejecutarComando(partida, new ComandoAtacar(
                nombreAtaqueElegido, tableroAtacante, tableroDefensor,
                battleAttackService, battleKoService
        ));

        CartaEnJuego defensorDespues = tableroDefensor.getActivo();
        int hpDefensorDespues = defensorDespues != null ? defensorDespues.getHpActual() : 0;
        int danio = Math.max(0, hpDefensorAntes - hpDefensorDespues);
        agregarLog(partida, "ATTACK_USED", callerUsername, nombreAtaqueElegido, nombreCarta(defensorAntes), danio > 0 ? String.valueOf(danio) : "");
        if (defensorAntes != null && tableroAtacante.getActivo() != null) {
            String tipoAtacante = tableroAtacante.getActivo().getCard().getTipo();
            if (coincideTipo(defensorAntes.getCard().getDebilidades(), tipoAtacante)) {
                agregarLog(partida, "SUPER_EFFECTIVE", callerUsername, nombreCarta(defensorAntes));
            } else if (coincideTipo(defensorAntes.getCard().getResistencias(), tipoAtacante)) {
                agregarLog(partida, "RESISTED", callerUsername, nombreCarta(defensorAntes));
            }
        }
        if (defensorDespues != null) {
            for (String condicion : defensorDespues.getCondicionesEspeciales()) {
                if (!condicionesAntes.contains(condicion)) {
                    agregarLog(partida, "STATUS_APPLIED", callerUsername, nombreCarta(defensorDespues), condicion);
                }
            }
        }

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
        String owner = dueno == partida.getJugador()
                ? sanitizarLog(partida.getJugadorUsername())
                : sanitizarLog(partida.getBotUsername() != null ? partida.getBotUsername() : "BOT");
        String cardName = sanitizarLog(nombreCarta(activo));

        if (activo.getCondicionesEspeciales().contains("Poisoned")) {
            System.out.println("☠️ Veneno: " + activo.getCard().getNombre() + " recibe 10 de daño.");
            activo.setHpActual(Math.max(0, activo.getHpActual() - 10));
            partida.getTurnLogs().add("POISON_DAMAGE:" + owner + ":" + cardName + ":10");
        }

        if (activo.getCondicionesEspeciales().contains("Burned")) {
            System.out.println("🔥 Quemadura: " + activo.getCard().getNombre() + " recibe 20 de daño.");
            activo.setHpActual(Math.max(0, activo.getHpActual() - 20));
            partida.getTurnLogs().add("BURN_DAMAGE:" + owner + ":" + cardName + ":20");

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
            CartaEnJuego activo = jugador.getActivo();
            activo.getCondicionesEspeciales().remove("CantRetreat");
            activo.getCondicionesEspeciales().remove("Paralyzed");
            
            // Evaluar noPuedeAtacarSiguienteTurno
            if (activo.isNoPuedeAtacarSiguienteTurno()) {
                if (activo.isNoPuedeAtacarYaConsumido()) {
                    activo.setNoPuedeAtacarSiguienteTurno(false);
                    activo.setNoPuedeAtacarYaConsumido(false);
                    activo.setPuedeAtacar(true);
                } else {
                    activo.setNoPuedeAtacarYaConsumido(true);
                    activo.setPuedeAtacar(false);
                }
            } else {
                activo.setPuedeAtacar(true);
            }

            // Evaluar ataqueBloqueadoSiguienteTurno
            if (activo.getAtaqueBloqueadoSiguienteTurno() != null) {
                if (activo.isAtaqueBloqueadoYaConsumido()) {
                    activo.setAtaqueBloqueadoSiguienteTurno(null);
                    activo.setAtaqueBloqueadoYaConsumido(false);
                } else {
                    activo.setAtaqueBloqueadoYaConsumido(true);
                }
            }

            activo.setDebeLanzarMonedaSiAtaca(false);
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
        agregarLog(partida, "TURN_PASSED", callerUsername);

        if (partida.getBotUsername() == null) {
            partida.setNumeroTurno(partida.getNumeroTurno() + 1);
            partida.setTurnoActual(Partida.Turno.BOT);
            agregarLog(partida, "TURN_STARTED", "BOT");
        } else {
            partida.setNumeroTurno(partida.getNumeroTurno() + 1);
            if (partida.getTurnoActual() == Partida.Turno.JUGADOR) {
                partida.setTurnoActual(Partida.Turno.BOT);
                robarCarta(partida.getBot());
                agregarLog(partida, "TURN_STARTED", partida.getBotUsername());
            } else {
                partida.setTurnoActual(Partida.Turno.JUGADOR);
                robarCarta(partida.getJugador());
                agregarLog(partida, "TURN_STARTED", partida.getJugadorUsername());
            }
        }
    }

    public void ejecutarSetupBot(String matchId) {
        Partida partida = partidasEnCurso.get(matchId);
        if (partida == null || partida.getBotUsername() != null) return; // Solo para offline
        
        botAIService.ejecutarSetup(partida);
        
        if (partida.isSetupJugadorListo() && partida.isSetupBotListo()) {
            partida.setSetupJugadorListo(false);
            partida.setSetupBotListo(false);
            
            if (partida.getFaseActual() == Partida.Fase.SETUP_PLACE_ACTIVE) {
                partida.transicionarA(new EstadoSetupPlaceBench());
            } else if (partida.getFaseActual() == Partida.Fase.SETUP_PLACE_BENCH) {
                partida.transicionarA(new EstadoSetupPrizePlacement());
            } else if (partida.getFaseActual() == Partida.Fase.SETUP_PLACE_BENCH_EXTRA) {
                partida.transicionarA(new EstadoSetupReveal());
            } else if (partida.getFaseActual() == Partida.Fase.SETUP_PRIZE_PLACEMENT) {
                prepararPremios(partida.getJugador());
                prepararPremios(partida.getBot());
                transicionarAExtraDraw(partida);
            }
        }
    }

    public synchronized Partida resolverAccionPendiente(
            String matchId,
            String callerUsername,
            List<String> selectedIds
    ) {
        Partida partida = getPartidaOThrow(matchId);
        com.pokemon.tcg.model.battle.PendingBattleAction pending = partida.getPendingAction();
        if (pending == null) throw new IllegalStateException("No hay una acción pendiente.");
        if (callerUsername == null || !callerUsername.equals(pending.getActor())) {
            throw new IllegalStateException("Esta decisión le corresponde al otro jugador.");
        }

        List<String> ids = selectedIds == null ? List.of() : selectedIds.stream().distinct().toList();
        if (ids.size() < pending.getMinSelections() || ids.size() > pending.getMaxSelections()) {
            throw new IllegalArgumentException("Cantidad de cartas seleccionadas inválida.");
        }
        java.util.Set<String> legalIds = pending.getOptions().stream()
                .map(com.pokemon.tcg.model.battle.PendingBattleAction.Option::getId)
                .collect(java.util.stream.Collectors.toSet());
        if (!legalIds.containsAll(ids)) {
            throw new IllegalArgumentException("La selección contiene cartas no permitidas.");
        }

        TableroJugador board = getTableroDeJugador(partida, callerUsername);
        if ("DISCARD_TO_TOP_DECK".equals(pending.getType())) {
            for (String id : ids) {
                Card card = board.getPilaDescarte().stream()
                        .filter(candidate -> id.equals(candidate.getId()))
                        .findFirst()
                        .orElseThrow(() -> new IllegalArgumentException("La carta ya no esta en el descarte."));
                board.getPilaDescarte().remove(card);
                board.getMazo().add(0, card);
                agregarLog(partida, "DISCARD_TO_TOP_DECK", callerUsername, card.getNombre());
            }
        } else {
            for (String id : ids) {
                Card card = board.getMazo().stream()
                        .filter(candidate -> id.equals(candidate.getId()))
                        .findFirst()
                        .orElseThrow(() -> new IllegalArgumentException("La carta ya no esta en el mazo."));
                board.getMazo().remove(card);
                if ("ATTACH_ACTIVE".equals(pending.getDestination()) && board.getActivo() != null) {
                    board.getActivo().getEnergiasUnidas().add(card);
                    agregarLog(partida, "ENERGY_ATTACHED", callerUsername, board.getActivo().getCard().getNombre());
                } else {
                    board.getMano().add(card);
                }
            }
            Collections.shuffle(board.getMazo());
            agregarLog(partida, "DECK_SEARCHED", callerUsername, String.valueOf(ids.size()));
        }
        partida.setPendingAction(null);
        partida.transicionarA(new EstadoTurnoNormal());
        return partida;
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
            CartaEnJuego botActivo = partida.getBot().getActivo();
            botActivo.getCondicionesEspeciales().remove("CantRetreat");
            botActivo.getCondicionesEspeciales().remove("Paralyzed");

            // Evaluar noPuedeAtacarSiguienteTurno
            if (botActivo.isNoPuedeAtacarSiguienteTurno()) {
                if (botActivo.isNoPuedeAtacarYaConsumido()) {
                    botActivo.setNoPuedeAtacarSiguienteTurno(false);
                    botActivo.setNoPuedeAtacarYaConsumido(false);
                    botActivo.setPuedeAtacar(true);
                } else {
                    botActivo.setNoPuedeAtacarYaConsumido(true);
                    botActivo.setPuedeAtacar(false);
                }
            } else {
                botActivo.setPuedeAtacar(true);
            }

            // Evaluar ataqueBloqueadoSiguienteTurno
            if (botActivo.getAtaqueBloqueadoSiguienteTurno() != null) {
                if (botActivo.isAtaqueBloqueadoYaConsumido()) {
                    botActivo.setAtaqueBloqueadoSiguienteTurno(null);
                    botActivo.setAtaqueBloqueadoYaConsumido(false);
                } else {
                    botActivo.setAtaqueBloqueadoYaConsumido(true);
                }
            }

            botActivo.setDebeLanzarMonedaSiAtaca(false);
            // ❌ ACÁ YA NO SE APAGA EL ESCUDO DEL BOT
        }

        // 🚩 TU ESCUDO SE APAGA ACÁ (El bot ya jugó su turno y te intentó pegar)
        if (partida.getJugador().getActivo() != null) {
            partida.getJugador().getActivo().setInvulnerable(false);
        }

        aplicarMantenimientoEntreTurnos(partida);

        partida.setNumeroTurno(partida.getNumeroTurno() + 1);
        partida.setTurnoActual(Partida.Turno.JUGADOR);
        robarCarta(partida.getJugador());
    }
    private void robarCarta(TableroJugador tablero) {
        if (!tablero.getMazo().isEmpty())
            tablero.getMano().add(tablero.getMazo().remove(0));
    }

    private boolean puedeColocarBasicosExtra(TableroJugador tablero) {
        return tablero.getBanca().size() < 5 && tablero.getMano().stream().anyMatch(this::esPokemonBasico);
    }

    private void revelarSetupEIniciarTurno(Partida partida) {
        revelarTablero(partida.getJugador());
        revelarTablero(partida.getBot());
        partida.setSetupJugadorListo(false);
        partida.setSetupBotListo(false);
        partida.setNumeroTurno(1);
        partida.transicionarA(new EstadoTurnoNormal());
    }

    private void revelarTablero(TableroJugador tablero) {
        if (tablero.getActivo() != null) {
            tablero.getActivo().setBocaAbajo(false);
        }
        tablero.getBanca().forEach(carta -> carta.setBocaAbajo(false));
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

    private TableroJugador getTableroByUsername(Partida partida, String username) {
        if (username != null && username.equals(partida.getJugadorUsername())) {
            return partida.getJugador();
        }
        if (username != null && username.equals(partida.getBotUsername())) {
            return partida.getBot();
        }
        if (partida.getBotUsername() == null) {
            return partida.getJugador();
        }
        throw new IllegalArgumentException("Usuario no valido para esta partida.");
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

    @Transactional
    public Partida startBattleOnline(String player1, Long player1MazoId, String player2, Long player2MazoId) {
        Jugador j1 = jugadorRepo.findByUsername(player1);
        if (j1 == null) throw new IllegalArgumentException("Jugador no encontrado: " + player1);
        Jugador j2 = jugadorRepo.findByUsername(player2);
        if (j2 == null) throw new IllegalArgumentException("Jugador no encontrado: " + player2);

        Mazo mazo1 = mazoRepo.findById(player1MazoId)
                .orElseThrow(() -> new IllegalArgumentException("Mazo no encontrado: " + player1MazoId));
        Mazo mazo2 = mazoRepo.findById(player2MazoId)
                .orElseThrow(() -> new IllegalArgumentException("Mazo no encontrado: " + player2MazoId));

        List<Card> cartasMazo1 = crearSnapshotMazo(mazo1.getCartas());
        List<Card> cartasMazo2 = crearSnapshotMazo(mazo2.getCartas());

        if (cartasMazo1.size() < 60 || cartasMazo2.size() < 60)
            throw new IllegalStateException("Los mazos deben tener 60 cartas.");

        TableroJugador tableroJugador = new TableroJugador();
        TableroJugador tableroBot = new TableroJugador();

        List<Card> m1 = new ArrayList<>(cartasMazo1);
        Collections.shuffle(m1);
        tableroJugador.setMazo(m1);

        List<Card> m2 = new ArrayList<>(cartasMazo2);
        Collections.shuffle(m2);
        tableroBot.setMazo(m2);

        robarCartas(tableroJugador, 7);
        robarCartas(tableroBot, 7);

        Partida partida = new Partida(tableroJugador, tableroBot);
        partida.setJugadorUsername(player1);
        partida.setBotUsername(player2);
        partida.setCoinFlipCallerUsername(player1);
        partida.setMulligansJugador(0);
        partida.setMulligansBot(0);
        partida.transicionarA(new EstadoLanzamientoMoneda());
        long now = System.currentTimeMillis();
        partida.setJugadorLastSeenAt(now);
        partida.setBotLastSeenAt(now);

        partidasEnCurso.put(partida.getId(), partida);
        System.out.println("✅ Partida Online creada con ID: " + partida.getId());
        return partida;
    }

    private List<Card> crearSnapshotMazo(List<Card> cartas) {
        inicializarGrafoCartas(cartas);
        return cartas.stream().map(this::copiarCartaParaPartida).toList();
    }

    private List<Card> generarMazoBot(List<Card> mazoJugador, String difficulty) {
        List<Card> catalogo = crearSnapshotMazo(cardRepo.findAll());
        List<Card> pokemon = catalogo.stream()
                .filter(card -> "Pokemon".equalsIgnoreCase(card.getSupertype()))
                .toList();
        List<Card> energias = catalogo.stream()
                .filter(card -> "Energy".equalsIgnoreCase(card.getSupertype()))
                .toList();
        if (pokemon.isEmpty() || energias.isEmpty()) {
            List<Card> fallback = new ArrayList<>(mazoJugador);
            Collections.shuffle(fallback);
            return fallback;
        }

        String dominantType = mazoJugador.stream()
                .filter(card -> "Pokemon".equalsIgnoreCase(card.getSupertype()))
                .map(Card::getTipo)
                .filter(java.util.Objects::nonNull)
                .collect(java.util.stream.Collectors.groupingBy(type -> type, java.util.stream.Collectors.counting()))
                .entrySet().stream()
                .max(java.util.Map.Entry.comparingByValue())
                .map(java.util.Map.Entry::getKey)
                .orElse("Colorless");

        String level = difficulty == null ? "NORMAL" : difficulty.trim().toUpperCase();
        String preferredType = "HARD".equals(level) ? counterType(dominantType) : randomPokemonType(pokemon);
        if ("EASY".equals(level)) preferredType = randomPokemonType(pokemon);
        final String selectedType = preferredType;

        List<Card> basics = pokemon.stream()
                .filter(this::esPokemonBasico)
                .filter(card -> selectedType.equalsIgnoreCase(card.getTipo()))
                .toList();
        if (basics.size() < 5) basics = pokemon.stream().filter(this::esPokemonBasico).toList();

        List<Card> result = new ArrayList<>(60);
        List<Card> shuffledBasics = new ArrayList<>(basics);
        Collections.shuffle(shuffledBasics);
        int pokemonTarget = "EASY".equals(level) ? 24 : 28;
        for (int i = 0; i < pokemonTarget; i++) {
            result.add(copiarCartaParaPartida(shuffledBasics.get(i % shuffledBasics.size())));
        }

        List<Card> matchingEnergy = energias.stream()
                .filter(card -> tipoEnergiaCoincide(card, selectedType))
                .toList();
        if (matchingEnergy.isEmpty()) matchingEnergy = energias;
        for (int i = result.size(); i < 60; i++) {
            result.add(copiarCartaParaPartida(matchingEnergy.get(i % matchingEnergy.size())));
        }
        Collections.shuffle(result);
        return result;
    }

    private String randomPokemonType(List<Card> pokemon) {
        List<String> types = pokemon.stream()
                .map(Card::getTipo)
                .filter(type -> type != null && !type.isBlank())
                .distinct()
                .toList();
        return types.isEmpty() ? "Colorless" : types.get(random.nextInt(types.size()));
    }

    private String counterType(String type) {
        if (type == null) return "Colorless";
        return switch (normalizarTexto(type)) {
            case "fire" -> "Water";
            case "water" -> "Lightning";
            case "grass" -> "Fire";
            case "lightning" -> "Fighting";
            case "fighting" -> "Psychic";
            case "psychic" -> "Darkness";
            case "darkness" -> "Grass";
            case "metal" -> "Fire";
            default -> "Colorless";
        };
    }

    private boolean tipoEnergiaCoincide(Card energy, String type) {
        String combined = (energy.getTipo() == null ? "" : energy.getTipo()) + " "
                + (energy.getNombre() == null ? "" : energy.getNombre());
        return normalizarTexto(combined).contains(normalizarTexto(type));
    }

    private Card copiarCartaParaPartida(Card source) {
        if (source == null) return null;

        Card copy = new Card();
        copy.setId(source.getId());
        copy.setNombre(source.getNombre());
        copy.setHp(source.getHp());
        copy.setTipo(source.getTipo());
        copy.setImagen(source.getImagen());
        copy.setCostoRetirada(source.getCostoRetirada());
        copy.setSupertype(source.getSupertype());
        copy.setEvolvesFrom(source.getEvolvesFrom());
        copy.setSubtypes(new ArrayList<>(source.getSubtypes()));
        copy.setReglas(new ArrayList<>(source.getReglas()));
        copy.reemplazarAtaques(source.getAtaques().stream().map(this::copiarAtaqueParaPartida).toList());
        copy.setDebilidades(source.getDebilidades().stream().map(this::copiarAtributoCarta).toList());
        copy.setResistencias(source.getResistencias().stream().map(this::copiarAtributoCarta).toList());
        return copy;
    }

    private Ataque copiarAtaqueParaPartida(Ataque source) {
        Ataque copy = new Ataque();
        copy.setNombre(source.getNombre());
        copy.setDanio(source.getDanio());
        copy.setTiposEnergia(source.getCosto() == null ? new ArrayList<>() : new ArrayList<>(source.getCosto()));
        copy.setTexto(source.getTexto());
        return copy;
    }

    private CardAttribute copiarAtributoCarta(CardAttribute source) {
        if (source == null) return null;
        return new CardAttribute(source.getType(), source.getValue());
    }

    private void agregarLog(Partida partida, String evento, String actor, String... detalles) {
        if (partida == null || evento == null) return;
        StringBuilder log = new StringBuilder(sanitizarLog(evento))
                .append(":")
                .append(sanitizarLog(actor == null || actor.isBlank() ? "Sistema" : actor));
        if (detalles != null) {
            for (String detalle : detalles) {
                log.append(":").append(sanitizarLog(detalle));
            }
        }
        partida.getTurnLogs().add(log.toString());
        while (partida.getTurnLogs().size() > 80) {
            partida.getTurnLogs().remove(0);
        }
    }

    private String nombreCarta(Card card) {
        return card == null ? "" : card.getNombre();
    }

    private String nombreCarta(CartaEnJuego carta) {
        return carta == null ? "" : nombreCarta(carta.getCard());
    }

    private String sanitizarLog(String value) {
        if (value == null) return "";
        return value.replace(':', '-').replace('\n', ' ').replace('\r', ' ').trim();
    }

    private boolean coincideTipo(List<CardAttribute> atributos, String tipo) {
        if (atributos == null || tipo == null) return false;
        return atributos.stream()
                .filter(java.util.Objects::nonNull)
                .map(CardAttribute::getType)
                .filter(java.util.Objects::nonNull)
                .anyMatch(valor -> valor.equalsIgnoreCase(tipo));
    }

    private void inicializarGrafoCartas(List<Card> cartas) {
        for (Card card : cartas) {
            if (card == null) continue;
            card.getAtaques().size();
            card.getReglas().size();
            card.getDebilidades().size();
            card.getResistencias().size();
            card.getSubtypes().size();
        }
    }
}
