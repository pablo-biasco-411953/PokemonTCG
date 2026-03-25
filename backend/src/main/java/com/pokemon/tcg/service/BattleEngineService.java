package com.pokemon.tcg.service;

import com.pokemon.tcg.model.Card;
import com.pokemon.tcg.model.Jugador;
import com.pokemon.tcg.model.Mazo;
import com.pokemon.tcg.repository.JugadorRepository;
import com.pokemon.tcg.repository.MazoRepository;
import com.pokemon.tcg.repository.CardRepository;
import com.pokemon.tcg.model.battle.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Random;
import java.util.Map;
import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;
import java.util.UUID;

@Service
public class BattleEngineService {

    private final JugadorRepository jugadorRepo;
    private final MazoRepository mazoRepo;
    private final CardRepository cardRepo;
    private final Random random = new Random();
    private final Map<String, Partida> partidasEnCurso = new ConcurrentHashMap<>();
    private final BotAIService botAIService;

    public BattleEngineService(JugadorRepository jugadorRepo, MazoRepository mazoRepo,
                               CardRepository cardRepo, BotAIService botAIService) {
        this.jugadorRepo = jugadorRepo;
        this.mazoRepo = mazoRepo;
        this.cardRepo = cardRepo;
        this.botAIService = botAIService;
    }

    // ─────────────────────────────────────────────────────────────
    // INICIO DE PARTIDA
    // ─────────────────────────────────────────────────────────────

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

        prepararJuegoInicial(tableroJugador);
        prepararJuegoInicial(tableroBot);

        // FIX: Partida genera su propio UUID en el constructor — lo reutilizamos directamente
        Partida partida = new Partida(tableroJugador, tableroBot);
        partida.setFaseActual(Partida.Fase.LANZAMIENTO_MONEDA);

        partidasEnCurso.put(partida.getId(), partida);
        System.out.println("✅ Partida creada con ID: " + partida.getId());
        return partida;
    }

    private void prepararJuegoInicial(TableroJugador tablero) {
        for (int i = 0; i < 7; i++) {
            if (!tablero.getMazo().isEmpty())
                tablero.getMano().add(tablero.getMazo().remove(0));
        }
        for (int i = 0; i < 6; i++) {
            if (!tablero.getMazo().isEmpty())
                tablero.getPremios().add(tablero.getMazo().remove(0));
        }
    }

    // ─────────────────────────────────────────────────────────────
    // SETUP INICIAL
    // ─────────────────────────────────────────────────────────────

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
            // FIX: bot corre en hilo separado para no bloquear el HTTP request
            partida.setTurnoActual(Partida.Turno.BOT);
            ejecutarTurnoBot(matchId);
        }
    }

    // ─────────────────────────────────────────────────────────────
    // CONSULTA DE ESTADO
    // ─────────────────────────────────────────────────────────────

    public Partida getEstadoPartida(String matchId) {
        return partidasEnCurso.get(matchId);
    }

    // ─────────────────────────────────────────────────────────────
    // ACCIONES DEL JUGADOR
    // ─────────────────────────────────────────────────────────────

    public void jugarPokemon(String matchId, String cartaId, int posicion) {
        Partida partida = getPartidaOThrow(matchId);
        validarTurnoJugador(partida);

        TableroJugador tablero = partida.getJugador();
        Card carta = encontrarCartaEnMano(tablero, cartaId);
        if (carta == null) throw new IllegalArgumentException("Carta no encontrada en la mano.");
        if (!esPokemonBasico(carta)) throw new IllegalArgumentException("Solo podés bajar Pokémon básicos.");

        CartaEnJuego nuevoPokemon = new CartaEnJuego(carta);
        tablero.getMano().remove(carta);

        if (tablero.getActivo() == null) {
            tablero.setActivo(nuevoPokemon);
        } else {
            if (tablero.getBanca().size() >= 5)
                throw new IllegalStateException("La banca está llena (máximo 5).");
            tablero.getBanca().add(nuevoPokemon);
        }
    }

    public void unirEnergia(String matchId, String cartaId, String energiaId) {
        Partida partida = getPartidaOThrow(matchId);
        validarTurnoJugador(partida);

        TableroJugador tablero = partida.getJugador();
        CartaEnJuego objetivo = encontrarCartaEnTablero(tablero, cartaId);
        Card energia = encontrarCartaEnMano(tablero, energiaId);

        if (objetivo == null) throw new IllegalArgumentException("Pokémon objetivo no encontrado.");
        if (energia == null || !esEnergia(energia)) throw new IllegalArgumentException("Energía no encontrada.");

        objetivo.getEnergiasUnidas().add(energia);
        tablero.getMano().remove(energia);
    }

    /**
     * FIX: El jugador ataca con su Pokémon activo al activo del bot.
     * Ya no recibe atacante/defensor como parámetros — los deduce del estado.
     */
    public void realizarAtaque(String matchId) {
        Partida partida = partidasEnCurso.get(matchId);

        // 1. Validaciones de estado
        if (partida == null) return;
        if (partida.getTurnoActual() != Partida.Turno.JUGADOR) {
            throw new IllegalStateException("No es tu turno para atacar.");
        }

        CartaEnJuego activoJugador = partida.getJugador().getActivo();
        CartaEnJuego activoBot = partida.getBot().getActivo();

        // 2. Validar que tengas un Pokémon y que tenga al menos 1 energía
        if (activoJugador == null) {
            throw new IllegalStateException("No tenés un Pokémon activo.");
        }

        if (activoJugador.getEnergiasUnidas().isEmpty()) {
            throw new IllegalStateException("Necesitás al menos 1 energía unida para atacar.");
        }

        if (activoBot != null) {
            // 3. Lógica de daño (usamos 30 fijo por ahora)
            int danio = 30;
            int nuevaHp = activoBot.getHpActual() - danio;
            activoBot.setHpActual(Math.max(0, nuevaHp));

            System.out.println("⚔️ [BATTLE] " + activoJugador.getCard().getNombre() +
                    " atacó a " + activoBot.getCard().getNombre() + " por " + danio);

            // 4. Verificar K.O.
            if (activoBot.getHpActual() <= 0) {
                resolverKO(partida, activoJugador, activoBot);

                // 🚨 FIX ESTRATÉGICO: Si el bot tiene banca, subimos uno inmediatamente
                // para que no se quede tildado en su próximo turno
                if (!partida.getBot().getBanca().isEmpty()) {
                    CartaEnJuego nuevoActivoBot = partida.getBot().getBanca().remove(0);
                    partida.getBot().setActivo(nuevoActivoBot);
                    System.out.println("🤖 [BOT] Reemplazó su activo derrotado por: " + nuevoActivoBot.getCard().getNombre());
                }
            }

            // 5. REGLA TCG: Atacar termina el turno automáticamente
            System.out.println("🔄 Ataque finalizado. Pasando turno al BOT...");
            this.pasarTurno(matchId);

        } else {
            throw new IllegalStateException("El oponente no tiene un Pokémon activo al cual atacar.");
        }
    }

    public void pasarTurno(String matchId) {
        Partida partida = getPartidaOThrow(matchId);
        validarTurnoJugador(partida);

        // Resetear el flag de ataque del activo del jugador para el próximo turno
        if (partida.getJugador().getActivo() != null)
            partida.getJugador().getActivo().setPuedeAtacar(true);

        partida.setTurnoActual(Partida.Turno.BOT);

        // FIX: turno del bot en hilo separado — no bloquea el thread HTTP
        ejecutarTurnoBot(matchId);
    }

    // ─────────────────────────────────────────────────────────────
    // TURNO DEL BOT (asíncrono)
    // ─────────────────────────────────────────────────────────────

    /**
     * FIX: @Async evita que el request del frontend quede colgado esperando
     * que el bot termine. Requiere @EnableAsync en la clase de configuración.
     */
    @Async
    public void ejecutarTurnoBot(String matchId) {
        Partida partida = partidasEnCurso.get(matchId);
        if (partida == null) return;

        try {
            // Pequeña pausa para que el frontend perciba que "el bot piensa"
            Thread.sleep(1200);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        botAIService.ejecutarTurno(partida);

        // Robar carta al inicio del turno del jugador
        robarCarta(partida.getJugador());

        // Resetear flag de ataque del bot para su próximo turno
        if (partida.getBot().getActivo() != null)
            partida.getBot().getActivo().setPuedeAtacar(true);

        partida.setTurnoActual(Partida.Turno.JUGADOR);
        System.out.println("✅ Turno del bot finalizado. Turno del jugador.");
    }

    // ─────────────────────────────────────────────────────────────
    // LÓGICA INTERNA
    // ─────────────────────────────────────────────────────────────

    private void resolverKO(Partida partida, CartaEnJuego atacante, CartaEnJuego defensor) {
        TableroJugador tableroVictima  = encontrarTableroPorCarta(partida, defensor);
        TableroJugador tableroGanador  = encontrarTableroPorCarta(partida, atacante);

        if (tableroVictima == null || tableroGanador == null) return;

        // Mover el Pokémon K.O. al descarte
        tableroVictima.getPilaDescarte().add(defensor.getCard());
        if (defensor.equals(tableroVictima.getActivo())) {
            tableroVictima.setActivo(null);
        } else {
            tableroVictima.getBanca().remove(defensor);
        }

        // El ganador toma una carta de premio
        if (!tableroGanador.getPremios().isEmpty()) {
            tableroGanador.getMano().add(tableroGanador.getPremios().remove(0));
        }

        System.out.println("💀 K.O.! Premios restantes del ganador: " + tableroGanador.getPremios().size());

        // Verificar fin de partida
        boolean ganadorSinPremios = tableroGanador.getPremios().isEmpty();
        boolean victimasinPokemon = tableroVictima.getActivo() == null
                && tableroVictima.getBanca().isEmpty();

        if (ganadorSinPremios || victimasinPokemon) {
            partida.setFaseActual(Partida.Fase.FIN_PARTIDA);
            System.out.println("🏆 ¡Partida terminada!");
        }
    }

    /**
     * Obtiene el daño base del primer ataque de la carta.
     * Si la carta no tiene ataques parseados, devuelve 10 como fallback.
     */
    private int calcularDanio(CartaEnJuego cartaEnJuego) {
        Card card = cartaEnJuego.getCard();

        if (card.getAtaques() != null && !card.getAtaques().isEmpty()) {
            // Como no tenés daño real, devolvemos un valor base
            return 20;
        }

        return 10;
    }

    private void robarCarta(TableroJugador tablero) {
        if (!tablero.getMazo().isEmpty())
            tablero.getMano().add(tablero.getMazo().remove(0));
    }

    // ─────────────────────────────────────────────────────────────
    // HELPERS / FINDERS
    // ─────────────────────────────────────────────────────────────

    private Partida getPartidaOThrow(String matchId) {
        Partida p = partidasEnCurso.get(matchId);
        if (p == null) throw new IllegalArgumentException("Partida no encontrada: " + matchId);
        return p;
    }

    private void validarTurnoJugador(Partida partida) {
        if (partida.getTurnoActual() != Partida.Turno.JUGADOR)
            throw new IllegalStateException("No es tu turno.");
    }

    private Card encontrarCartaEnMano(TableroJugador tablero, String id) {
        return tablero.getMano().stream()
                .filter(c -> c.getId().equals(id))
                .findFirst().orElse(null);
    }

    private CartaEnJuego encontrarCartaEnTablero(TableroJugador tablero, String id) {
        if (tablero.getActivo() != null && tablero.getActivo().getCard().getId().equals(id))
            return tablero.getActivo();
        return tablero.getBanca().stream()
                .filter(c -> c.getCard().getId().equals(id))
                .findFirst().orElse(null);
    }

    private TableroJugador encontrarTableroPorCarta(Partida p, CartaEnJuego c) {
        if (encontrarCartaEnTablero(p.getJugador(), c.getCard().getId()) != null) return p.getJugador();
        if (encontrarCartaEnTablero(p.getBot(),     c.getCard().getId()) != null) return p.getBot();
        return null;
    }

    private boolean esPokemonBasico(Card c) {
        if (c.getTipo() == null) return false;
        String tipo = c.getTipo().toLowerCase();
        return !tipo.contains("stage") && !tipo.contains("energy") && !tipo.contains("energía");
    }

    private boolean esEnergia(Card c) {
        return c.getTipo() != null &&
                (c.getTipo().toLowerCase().contains("energy") ||
                        c.getTipo().toLowerCase().contains("energía"));
    }
}