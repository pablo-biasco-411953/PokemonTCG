package com.pokemon.tcg.service;

import com.pokemon.tcg.model.Card;
import com.pokemon.tcg.model.Jugador;
import com.pokemon.tcg.model.Mazo;
import com.pokemon.tcg.model.battle.Ataque;
import com.pokemon.tcg.model.battle.CartaEnJuego;
import com.pokemon.tcg.model.battle.Partida;
import com.pokemon.tcg.model.battle.ResultadoAtaque;
import com.pokemon.tcg.model.battle.TableroJugador;
import com.pokemon.tcg.repository.CardRepository;
import com.pokemon.tcg.repository.JugadorRepository;
import com.pokemon.tcg.repository.MazoRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class BattleEngineService {

    private final JugadorRepository jugadorRepo;
    private final MazoRepository mazoRepo;
    private final CardRepository cardRepo;
    private final Random random = new Random();
    private final Map<String, Partida> partidasEnCurso = new ConcurrentHashMap<>();
    private final BotAIService botAIService;
    private final BattleTurnService battleTurnService;
    private final BattleAttackService battleAttackService;
    private final BattleKoService battleKoService;

    public BattleEngineService(
            JugadorRepository jugadorRepo,
            MazoRepository mazoRepo,
            CardRepository cardRepo,
            BotAIService botAIService,
            BattleTurnService battleTurnService,
            BattleAttackService battleAttackService,
            BattleKoService battleKoService
    ) {
        this.jugadorRepo = jugadorRepo;
        this.mazoRepo = mazoRepo;
        this.cardRepo = cardRepo;
        this.botAIService = botAIService;
        this.battleTurnService = battleTurnService;
        this.battleAttackService = battleAttackService;
        this.battleKoService = battleKoService;
    }

    public Partida startBattle(String username, Long mazoId) {
        Jugador jugador = jugadorRepo.findByUsername(username);
        if (jugador == null) {
            throw new IllegalArgumentException("Jugador no encontrado: " + username);
        }

        Mazo mazoSeleccionado = mazoRepo.findById(mazoId)
                .orElseThrow(() -> new IllegalArgumentException("Mazo no encontrado: " + mazoId));

        List<Card> cartasMazo = mazoSeleccionado.getCartas();
        if (cartasMazo.size() < 60) {
            throw new IllegalStateException("El mazo debe tener 60 cartas. Tiene: " + cartasMazo.size());
        }

        TableroJugador tableroJugador = new TableroJugador();
        TableroJugador tableroBot = new TableroJugador();

        List<Card> mazoJugador = new ArrayList<>(cartasMazo);
        Collections.shuffle(mazoJugador);
        tableroJugador.setMazo(mazoJugador);

        List<Card> mazoBot = new ArrayList<>(cartasMazo);
        Collections.shuffle(mazoBot);
        tableroBot.setMazo(mazoBot);

        prepararJuegoInicial(tableroJugador);
        prepararJuegoInicial(tableroBot);

        Partida partida = new Partida(tableroJugador, tableroBot);
        partida.setFaseActual(Partida.Fase.LANZAMIENTO_MONEDA);

        partidasEnCurso.put(partida.getId(), partida);
        System.out.println("Partida creada con ID: " + partida.getId());
        return partida;
    }

    private void prepararJuegoInicial(TableroJugador tablero) {
        for (int i = 0; i < 7; i++) {
            if (!tablero.getMazo().isEmpty()) {
                tablero.getMano().add(tablero.getMazo().remove(0));
            }
        }
        for (int i = 0; i < 6; i++) {
            if (!tablero.getMazo().isEmpty()) {
                tablero.getPremios().add(tablero.getMazo().remove(0));
            }
        }
    }

    public boolean lanzarMoneda(String matchId) {
        Partida partida = getPartidaOThrow(matchId);
        boolean jugadorGana = random.nextBoolean();
        partida.setFaseActual(Partida.Fase.TURNO_NORMAL);
        return jugadorGana;
    }

    public void elegirTurno(String matchId, boolean vaPrimero) {
        Partida partida = getPartidaOThrow(matchId);
        if (vaPrimero) {
            partida.setTurnoActual(Partida.Turno.JUGADOR);
        } else {
            partida.setTurnoActual(Partida.Turno.BOT);
            ejecutarTurnoBot(matchId);
        }
    }

    public Partida getEstadoPartida(String matchId) {
        return partidasEnCurso.get(matchId);
    }

    public void jugarPokemon(String matchId, String cartaId) {
        Partida partida = getPartidaOThrow(matchId);
        validarTurnoJugador(partida);

        TableroJugador tablero = partida.getJugador();
        Card carta = encontrarCartaEnMano(tablero, cartaId);

        if (carta == null) {
            throw new IllegalArgumentException("La carta no esta en tu mano.");
        }
        if (!esPokemonBasico(carta)) {
            throw new IllegalArgumentException("Solo podes bajar Pokemon basicos.");
        }

        CartaEnJuego nuevoPokemon = new CartaEnJuego(carta);
        if (tablero.getActivo() == null) {
            tablero.setActivo(nuevoPokemon);
            tablero.getMano().remove(carta);
            System.out.println(carta.getNombre() + " entro como activo.");
            return;
        }

        if (tablero.getBanca().size() >= 5) {
            throw new IllegalStateException("La banca esta llena (maximo 5).");
        }

        tablero.getBanca().add(nuevoPokemon);
        tablero.getMano().remove(carta);
        System.out.println(carta.getNombre() + " se unio a la banca.");
    }

    public void unirEnergia(String matchId, String cartaId, String energiaId) {
        Partida partida = getPartidaOThrow(matchId);
        validarTurnoJugador(partida);

        TableroJugador tablero = partida.getJugador();
        CartaEnJuego objetivo = encontrarCartaEnTablero(tablero, cartaId);
        Card energia = encontrarCartaEnMano(tablero, energiaId);

        if (objetivo == null) {
            throw new IllegalArgumentException("Pokemon objetivo no encontrado.");
        }
        if (energia == null || !esEnergia(energia)) {
            throw new IllegalArgumentException("Energia no encontrada.");
        }

        objetivo.getEnergiasUnidas().add(energia);
        tablero.getMano().remove(energia);
    }

    public void realizarRetirada(String matchId, String nuevoActivoId) {
        Partida partida = getPartidaOThrow(matchId);
        validarTurnoJugador(partida);
        TableroJugador tablero = partida.getJugador();
        CartaEnJuego activoViejo = tablero.getActivo();

        if (activoViejo == null) {
            throw new IllegalStateException("No hay un Pokemon activo para retirar.");
        }
        if (partida.isYaSeRetiroEsteTurno()) {
            throw new IllegalStateException("Solo podes realizar una retirada por turno.");
        }
        if (activoViejo.getCondicionesEspeciales().contains("Asleep")
                || activoViejo.getCondicionesEspeciales().contains("Paralyzed")
                || activoViejo.getCondicionesEspeciales().contains("CantRetreat")) {
            throw new IllegalStateException("No podes retirarte por un estado alterado o efecto de ataque.");
        }

        CartaEnJuego suplente = tablero.getBanca().stream()
                .filter(c -> c.getCard().getId().equals(nuevoActivoId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("El Pokemon elegido no esta en la banca."));

        int costo = activoViejo.getCard().getCostoRetirada();
        if (activoViejo.getEnergiasUnidas().size() < costo) {
            throw new IllegalStateException("Energias insuficientes. Necesitas " + costo + " para retirar.");
        }

        for (int i = 0; i < costo; i++) {
            Card energia = activoViejo.getEnergiasUnidas().remove(0);
            tablero.getPilaDescarte().add(energia);
        }

        activoViejo.limpiarCondiciones();
        tablero.getBanca().remove(suplente);
        tablero.getBanca().add(activoViejo);
        tablero.setActivo(suplente);
        partida.setYaSeRetiroEsteTurno(true);

        System.out.println("Retirada: " + activoViejo.getCard().getNombre()
                + " a la banca. Entra " + suplente.getCard().getNombre());
    }

    public void subirAActivoDesdeBanca(String matchId, String cartaIdEnBanca) {
        Partida partida = getPartidaOThrow(matchId);
        validarTurnoJugador(partida);

        TableroJugador tablero = partida.getJugador();
        if (tablero.getActivo() != null) {
            throw new IllegalStateException("Ya tenes un Pokemon activo. Debes retirarlo primero.");
        }

        CartaEnJuego elegido = tablero.getBanca().stream()
                .filter(c -> c.getCard().getId().equals(cartaIdEnBanca))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Pokemon no encontrado en la banca."));

        tablero.getBanca().remove(elegido);
        tablero.setActivo(elegido);
        System.out.println(elegido.getCard().getNombre() + " ahora es tu Pokemon activo.");
    }

    public void realizarAtaque(String matchId, String nombreAtaqueElegido) {
        Partida partida = getPartidaOThrow(matchId);
        validarTurnoJugador(partida);

        CartaEnJuego activoJugador = partida.getJugador().getActivo();
        CartaEnJuego activoBot = partida.getBot().getActivo();

        if (activoJugador == null) {
            throw new IllegalStateException("No tenes un Pokemon activo.");
        }
        if (activoBot == null) {
            throw new IllegalStateException("El oponente no tiene un Pokemon activo.");
        }
        if (activoBot.isInvulnerable()) {
            System.out.println("El ataque reboto: " + activoBot.getCard().getNombre() + " es invulnerable.");
            pasarTurno(matchId);
            return;
        }
        if (activoJugador.getEnergiasUnidas().isEmpty()) {
            throw new IllegalStateException("Necesitas al menos 1 energia unida para atacar.");
        }
        if (activoJugador.getCondicionesEspeciales().contains("Asleep")
                || activoJugador.getCondicionesEspeciales().contains("Paralyzed")) {
            throw new IllegalStateException("Tu Pokemon activo no puede atacar por su estado actual.");
        }

        Ataque ataqueUsado = encontrarAtaque(activoJugador, nombreAtaqueElegido);
        BattleAttackService.AttackResolution resolution = battleAttackService.resolveAttack(
                partida,
                ataqueUsado,
                activoJugador,
                activoBot,
                battleKoService::resolverKO
        );
        ResultadoAtaque resultado = resolution.resultado();

        System.out.println("[BATTLE] " + activoJugador.getCard().getNombre()
                + " uso [" + nombreAtaqueElegido + "] por " + resultado.danioFinal());

        partida.setUltimasMonedasLanzadas(resolution.historialMonedas());
        pasarTurno(matchId);
    }

    private Ataque encontrarAtaque(CartaEnJuego atacante, String nombreAtaqueElegido) {
        List<Ataque> ataques = atacante.getCard().getAtaques();
        if (ataques != null) {
            for (Ataque ataque : ataques) {
                if (ataque.getNombre().equals(nombreAtaqueElegido)) {
                    return ataque;
                }
            }
        }
        throw new IllegalStateException("Ataque no encontrado.");
    }

    public void pasarTurno(String matchId) {
        Partida partida = getPartidaOThrow(matchId);
        validarTurnoJugador(partida);

        TableroJugador jugador = partida.getJugador();
        TableroJugador bot = partida.getBot();

        if (jugador.getActivo() == null && !jugador.getBanca().isEmpty()) {
            throw new IllegalStateException("Debes subir un Pokemon de tu banca a la posicion activa antes de terminar tu turno.");
        }

        if (jugador.getActivo() != null) {
            battleTurnService.limpiarActivoFinTurnoJugador(jugador);
        }
        if (bot.getActivo() != null) {
            bot.getActivo().setInvulnerable(false);
        }

        battleTurnService.aplicarMantenimientoEntreTurnos(partida, random, battleKoService::resolverKO);

        partida.setYaSeRetiroEsteTurno(false);
        partida.setTurnoActual(Partida.Turno.BOT);
    }

    public void ejecutarTurnoBot(String matchId) {
        Partida partida = partidasEnCurso.get(matchId);
        if (partida == null) {
            return;
        }

        robarCarta(partida.getBot());
        botAIService.ejecutarTurno(partida);

        if (partida.getBot().getActivo() != null) {
            battleTurnService.limpiarActivoFinTurnoBot(partida.getBot());
        }
        if (partida.getJugador().getActivo() != null) {
            partida.getJugador().getActivo().setInvulnerable(false);
        }

        battleTurnService.aplicarMantenimientoEntreTurnos(partida, random, battleKoService::resolverKO);

        partida.setTurnoActual(Partida.Turno.JUGADOR);
        robarCarta(partida.getJugador());
    }

    private void robarCarta(TableroJugador tablero) {
        if (!tablero.getMazo().isEmpty()) {
            tablero.getMano().add(tablero.getMazo().remove(0));
        }
    }

    private Partida getPartidaOThrow(String matchId) {
        Partida partida = partidasEnCurso.get(matchId);
        if (partida == null) {
            throw new IllegalArgumentException("Partida no encontrada: " + matchId);
        }
        return partida;
    }

    private void validarTurnoJugador(Partida partida) {
        if (partida.getTurnoActual() != Partida.Turno.JUGADOR) {
            throw new IllegalStateException("No es tu turno.");
        }
    }

    private Card encontrarCartaEnMano(TableroJugador tablero, String id) {
        return tablero.getMano().stream()
                .filter(c -> c.getId().equals(id))
                .findFirst()
                .orElse(null);
    }

    private CartaEnJuego encontrarCartaEnTablero(TableroJugador tablero, String id) {
        if (tablero.getActivo() != null
                && tablero.getActivo().getCard() != null
                && tablero.getActivo().getCard().getId().equals(id)) {
            return tablero.getActivo();
        }

        return tablero.getBanca().stream()
                .filter(c -> c != null && c.getCard() != null)
                .filter(c -> c.getCard().getId().equals(id))
                .findFirst()
                .orElse(null);
    }

    private boolean esPokemonBasico(Card carta) {
        if (carta == null || carta.getSupertype() == null) {
            return false;
        }

        boolean esPokemon = "Pokemon".equalsIgnoreCase(carta.getSupertype())
                || "Pokémon".equalsIgnoreCase(carta.getSupertype());
        if (!esPokemon || carta.getSubtypes() == null) {
            return false;
        }

        for (String subtype : carta.getSubtypes()) {
            if ("Basic".equalsIgnoreCase(subtype)) {
                return true;
            }
        }
        return false;
    }

    private boolean esEnergia(Card carta) {
        return carta != null
                && carta.getSupertype() != null
                && carta.getSupertype().equalsIgnoreCase("Energy");
    }

    public void evolucionarPokemon(String matchId, String cartaManoId, String cartaTableroId) {
        Partida partida = getPartidaOThrow(matchId);
        validarTurnoJugador(partida);

        TableroJugador tablero = partida.getJugador();

        Card cartaEvolucion = encontrarCartaEnMano(tablero, cartaManoId);
        if (cartaEvolucion == null) {
            throw new IllegalArgumentException("La carta de evolucion no esta en tu mano.");
        }

        CartaEnJuego objetivo = encontrarCartaEnTablero(tablero, cartaTableroId);
        if (objetivo == null) {
            throw new IllegalArgumentException("El Pokemon objetivo no esta en tu tablero.");
        }

        String evolvesFrom = cartaEvolucion.getEvolvesFrom();
        String nombreObjetivo = objetivo.getCard().getNombre();

        if (evolvesFrom == null || !evolvesFrom.equalsIgnoreCase(nombreObjetivo)) {
            throw new IllegalStateException("Evolucion invalida: " + cartaEvolucion.getNombre()
                    + " evoluciona de " + evolvesFrom + ", no de " + nombreObjetivo + ".");
        }

        int hpMaximoAnterior = Integer.parseInt(objetivo.getCard().getHp());
        int danioAcumulado = hpMaximoAnterior - objetivo.getHpActual();

        objetivo.setCard(cartaEvolucion);

        int nuevoHpMaximo = Integer.parseInt(cartaEvolucion.getHp());
        objetivo.setHpActual(Math.max(0, nuevoHpMaximo - danioAcumulado));
        objetivo.limpiarCondiciones();

        tablero.getMano().remove(cartaEvolucion);
    }

    public Partida debugRobarCarta(String matchId, String cardId) {
        Partida partida = getPartidaOThrow(matchId);

        Card cartaMagica = cardRepo.findById(cardId)
                .orElseThrow(() -> new IllegalArgumentException("Carta no encontrada en DB: " + cardId));

        partida.getJugador().getMano().add(cartaMagica);
        System.out.println("[GOD MODE] Carta inyectada a la mano: " + cartaMagica.getNombre());
        return partida;
    }

    public Partida debugForzarEstado(String matchId, String objetivo, String estado) {
        Partida partida = getPartidaOThrow(matchId);

        CartaEnJuego activo = objetivo.equals("BOT")
                ? partida.getBot().getActivo()
                : partida.getJugador().getActivo();

        if (activo != null) {
            activo.limpiarCondiciones();
            activo.agregarCondicion(estado);
            System.out.println("[GOD MODE] Estado " + estado + " aplicado a " + activo.getCard().getNombre());
        }

        return partida;
    }

    public Partida debugSetHp(String matchId, String objetivo, int hp) {
        Partida partida = getPartidaOThrow(matchId);

        CartaEnJuego activoVictima = objetivo.equals("BOT")
                ? partida.getBot().getActivo()
                : partida.getJugador().getActivo();
        CartaEnJuego rivalAtacante = objetivo.equals("BOT")
                ? partida.getJugador().getActivo()
                : partida.getBot().getActivo();

        if (activoVictima != null) {
            activoVictima.setHpActual(Math.max(0, hp));
            System.out.println("[GOD MODE] HP de " + activoVictima.getCard().getNombre() + " seteado a " + hp);

            if (activoVictima.getHpActual() <= 0 && rivalAtacante != null) {
                System.out.println("[GOD MODE] Ejecutando Muerte Subita...");
                battleKoService.resolverKO(partida, rivalAtacante, activoVictima);
            }
        }

        return partida;
    }

    public List<Card> obtenerCatalogoCartasDebug() {
        return cardRepo.findAll();
    }
}
