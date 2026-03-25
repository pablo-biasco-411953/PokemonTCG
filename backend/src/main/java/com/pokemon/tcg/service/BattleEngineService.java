package com.pokemon.tcg.service;

import com.pokemon.tcg.model.Card;
import com.pokemon.tcg.model.Jugador;
import com.pokemon.tcg.model.Mazo;
import com.pokemon.tcg.repository.JugadorRepository;
import com.pokemon.tcg.repository.MazoRepository;
import com.pokemon.tcg.repository.CardRepository;
import com.pokemon.tcg.model.battle.*;
import org.springframework.stereotype.Service;

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
    private final Map<String, Partida> partidasEnCurso = new ConcurrentHashMap<>();
    private final BotAIService botAIService;

    public BattleEngineService(JugadorRepository jugadorRepo, MazoRepository mazoRepo, CardRepository cardRepo, BotAIService botAIService) {
        this.jugadorRepo = jugadorRepo;
        this.mazoRepo = mazoRepo;
        this.cardRepo = cardRepo;
        this.botAIService = botAIService;
    }

    /**
     * Inicia una partida entre el jugador y un bot usando el mazo seleccionado.
     */
    public Partida startBattle(String username, Long mazoId) {
        Jugador jugador = jugadorRepo.findByUsername(username);
        if (jugador == null) {
            throw new IllegalArgumentException("Jugador no encontrado: " + username);
        }

        Mazo mazoSeleccionado = mazoRepo.findById(mazoId).orElse(null);
        if (mazoSeleccionado == null) {
            throw new IllegalArgumentException("Mazo no encontrado: " + mazoId);
        }

        // 1. Inicializar tableros
        TableroJugador tableroJugador = new TableroJugador();
        TableroJugador tableroBot = new TableroJugador();

        // 2. Cargar mazo del Jugador
        List<Card> cartasMazo = mazoSeleccionado.getCartas();
        if (cartasMazo.size() < 60) {
            throw new IllegalStateException("El mazo debe tener 60 cartas. Tiene: " + cartasMazo.size());
        }

        // Usamos una copia para poder hacer shuffle sin afectar la base de datos
        List<Card> mazoListaJugador = new ArrayList<>(cartasMazo);
        Collections.shuffle(mazoListaJugador);
        tableroJugador.setMazo(mazoListaJugador);

        // 3. Cargar mazo del Bot (Usamos el mismo mazo para asegurar que hay 60 cartas)
        List<Card> mazoListaBot = new ArrayList<>(cartasMazo);
        Collections.shuffle(mazoListaBot);
        tableroBot.setMazo(mazoListaBot);

        // 4. Repartir cartas iniciales (Mano y Premios)
        prepararJuegoInicial(tableroJugador);
        prepararJuegoInicial(tableroBot);

        // 5. Crear partida y guardarla en memoria
        Partida partida = new Partida(tableroJugador, tableroBot);
        partida.setFaseActual(Partida.Fase.LANZAMIENTO_MONEDA);
        partidasEnCurso.put(partida.getId(), partida);

        return partida;
    }

    /**
     * Lógica estándar TCG: 7 cartas a la mano y 6 a premios.
     */
    private void prepararJuegoInicial(TableroJugador tablero) {
        // Robar 7 para la mano
        for (int i = 0; i < 7; i++) {
            if (!tablero.getMazo().isEmpty()) {
                tablero.getMano().add(tablero.getMazo().remove(0));
            }
        }
        // Separar 6 para premios
        for (int i = 0; i < 6; i++) {
            if (!tablero.getMazo().isEmpty()) {
                tablero.getPremios().add(tablero.getMazo().remove(0));
            }
        }
    }

    public boolean lanzarMoneda(String matchId) {
        Partida partida = partidasEnCurso.get(matchId);
        if (partida == null) throw new IllegalArgumentException("Partida no encontrada.");
        boolean jugadorGana = random.nextBoolean();
        partida.setFaseActual(Partida.Fase.TURNO_NORMAL);
        return jugadorGana;
    }

    public void elegirTurno(String matchId, boolean vaPrimero) {
        Partida partida = partidasEnCurso.get(matchId);
        if (partida == null) throw new IllegalArgumentException("Partida no encontrada.");

        partida.setTurnoActual(vaPrimero ? Partida.Turno.JUGADOR : Partida.Turno.BOT);

        if (!vaPrimero) {
            botAIService.ejecutarTurno(partida);
            partida.setTurnoActual(Partida.Turno.JUGADOR);
        }
    }

    public Partida getEstadoPartida(String matchId) {
        return partidasEnCurso.get(matchId);
    }

    public void jugarPokemon(String matchId, String cartaId, int posicion) {
        Partida partida = partidasEnCurso.get(matchId);
        if (partida == null) throw new IllegalArgumentException("Partida no encontrada.");

        TableroJugador tableroJugador = partida.getJugador();
        Card carta = encontrarCartaEnMano(tableroJugador, cartaId);

        if (carta == null) throw new IllegalArgumentException("Carta no encontrada.");
        if (!esPokemonBasico(carta)) throw new IllegalArgumentException("Solo Pokémon básicos.");

        CartaEnJuego nuevoPokemon = new CartaEnJuego(carta);
        tableroJugador.getMano().remove(carta);

        if (tableroJugador.getActivo() == null) {
            tableroJugador.setActivo(nuevoPokemon);
        } else {
            if (tableroJugador.getBanca().size() >= 5) throw new IllegalStateException("Banca llena.");
            tableroJugador.getBanca().add(nuevoPokemon);
        }
    }

    public void unirEnergia(String matchId, String cartaId, String energiaId) {
        Partida partida = partidasEnCurso.get(matchId);
        TableroJugador tablero = partida.getJugador();
        CartaEnJuego objetivo = encontrarCartaEnTablero(tablero, cartaId);
        Card energia = encontrarCartaEnMano(tablero, energiaId);

        if (objetivo != null && energia != null && esEnergia(energia)) {
            objetivo.getEnergiasUnidas().add(energia);
            tablero.getMano().remove(energia);
        }
    }

    public void pasarTurno(String matchId) {
        Partida partida = partidasEnCurso.get(matchId);
        if (partida != null && partida.getTurnoActual() == Partida.Turno.JUGADOR) {
            partida.setTurnoActual(Partida.Turno.BOT);
            botAIService.ejecutarTurno(partida);
            partida.setTurnoActual(Partida.Turno.JUGADOR);
            // Al volver al jugador, robamos carta de turno
            robarCarta(partida.getJugador());
        }
    }

    private void robarCarta(TableroJugador tablero) {
        if (!tablero.getMazo().isEmpty()) {
            tablero.getMano().add(tablero.getMazo().remove(0));
        }
    }

    public void realizarAtaque(String matchId, CartaEnJuego atacante, CartaEnJuego defensor, Ataque ataque) {
        Partida partida = partidasEnCurso.get(matchId);
        if (partida == null) return;

        int danio = ataque.getDanio();
        defensor.setHpActual(defensor.getHpActual() - danio);

        if (defensor.getHpActual() <= 0) {
            resolverKO(partida, atacante, defensor);
        }
    }

    private void resolverKO(Partida partida, CartaEnJuego atacante, CartaEnJuego defensor) {
        TableroJugador tableroVictima = encontrarTableroPorCartaEnJuego(partida, defensor);
        TableroJugador tableroGanador = encontrarTableroPorCartaEnJuego(partida, atacante);

        if (tableroVictima != null && tableroGanador != null) {
            // Mover al descarte
            tableroVictima.getPilaDescarte().add(defensor.getCard());
            if (defensor.equals(tableroVictima.getActivo())) tableroVictima.setActivo(null);
            else tableroVictima.getBanca().remove(defensor);

            // Tomar premio
            if (!tableroGanador.getPremios().isEmpty()) {
                tableroGanador.getMano().add(tableroGanador.getPremios().remove(0));
            }

            // Validar fin de partida
            if (tableroGanador.getPremios().isEmpty() ||
                    (tableroVictima.getActivo() == null && tableroVictima.getBanca().isEmpty())) {
                partida.setFaseActual(Partida.Fase.FIN_PARTIDA);
            }
        }
    }

    // --- HELPERS ---

    private Card encontrarCartaEnMano(TableroJugador tablero, String id) {
        return tablero.getMano().stream().filter(c -> c.getId().equals(id)).findFirst().orElse(null);
    }

    private CartaEnJuego encontrarCartaEnTablero(TableroJugador tablero, String id) {
        if (tablero.getActivo() != null && tablero.getActivo().getCard().getId().equals(id)) return tablero.getActivo();
        return tablero.getBanca().stream().filter(c -> c.getCard().getId().equals(id)).findFirst().orElse(null);
    }

    private TableroJugador encontrarTableroPorCartaEnJuego(Partida p, CartaEnJuego c) {
        if (encontrarCartaEnTablero(p.getJugador(), c.getCard().getId()) != null) return p.getJugador();
        if (encontrarCartaEnTablero(p.getBot(), c.getCard().getId()) != null) return p.getBot();
        return null;
    }

    private boolean esPokemonBasico(Card c) {
        return c.getTipo() != null && c.getTipo().toLowerCase().contains("basic");
    }

    private boolean esEnergia(Card c) {
        return c.getTipo() != null && c.getTipo().toLowerCase().contains("energy");
    }
}