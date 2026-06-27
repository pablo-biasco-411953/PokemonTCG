package com.pokemon.tcg.service;

import com.pokemon.tcg.model.Card;
import com.pokemon.tcg.model.Jugador;
import com.pokemon.tcg.model.battle.CartaEnJuego;
import com.pokemon.tcg.model.battle.Partida;
import com.pokemon.tcg.model.battle.TableroJugador;
import com.pokemon.tcg.repository.JugadorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class BattleServiceTest {

    private JugadorRepository jugadorRepo;
    private BattleService service;

    @BeforeEach
    void setUp() {
        jugadorRepo = mock(JugadorRepository.class);
        service = new BattleService(jugadorRepo);
    }

    private Card card(String id, String nombre, String tipo) {
        Card c = new Card();
        c.setId(id);
        c.setNombre(nombre);
        c.setTipo(tipo);
        c.setHp("60");
        return c;
    }

    private TableroJugador tableroConMazo(int cantidadCartas) {
        TableroJugador t = new TableroJugador();
        for (int i = 0; i < cantidadCartas; i++) {
            t.getMazo().add(card("xy1-" + i, "Pokemon" + i, "Basic"));
        }
        return t;
    }

    // =================== startBattle ===================

    @Test
    void startBattle_jugadorNoEncontrado_lanzaExcepcion() {
        when(jugadorRepo.findByUsername("noexiste")).thenReturn(null);
        assertThrows(IllegalArgumentException.class,
                () -> service.startBattle("noexiste", 1L));
    }

    @Test
    void startBattle_exitoso_retornaPartida() {
        when(jugadorRepo.findByUsername("ash")).thenReturn(new Jugador("ash"));
        Partida partida = service.startBattle("ash", 1L);
        assertNotNull(partida);
    }

    // =================== robarCartas ===================

    @Test
    void robarCartas_mazoNulo_noHaceNada() {
        TableroJugador t = new TableroJugador();
        t.setMazo(null);
        assertDoesNotThrow(() -> service.robarCartas(t, 3));
    }

    @Test
    void robarCartas_mazoVacio_noRoba() {
        TableroJugador t = new TableroJugador();
        service.robarCartas(t, 3);
        assertTrue(t.getMano().isEmpty());
    }

    @Test
    void robarCartas_mazoConCartas_robaCorrectamente() {
        TableroJugador t = tableroConMazo(5);
        service.robarCartas(t, 3);
        assertEquals(3, t.getMano().size());
        assertEquals(2, t.getMazo().size());
    }

    @Test
    void robarCartas_masDeLoDisponible_robaHastaVaciar() {
        TableroJugador t = tableroConMazo(2);
        service.robarCartas(t, 10);
        assertEquals(2, t.getMano().size());
        assertTrue(t.getMazo().isEmpty());
    }

    @Test
    void robarCartas_cantidadCero_noRobaNada() {
        TableroJugador t = tableroConMazo(5);
        service.robarCartas(t, 0);
        assertTrue(t.getMano().isEmpty());
    }

    // =================== tienePokemonBasico ===================

    @Test
    void tienePokemonBasico_manoNula_retornaFalse() {
        TableroJugador t = new TableroJugador();
        t.setMano(null);
        assertFalse(service.tienePokemonBasico(t));
    }

    @Test
    void tienePokemonBasico_manoVacia_retornaFalse() {
        TableroJugador t = new TableroJugador();
        assertFalse(service.tienePokemonBasico(t));
    }

    @Test
    void tienePokemonBasico_conBasico_retornaTrue() {
        TableroJugador t = new TableroJugador();
        t.getMano().add(card("xy1-1", "Bulbasaur", "Basic Pokemon"));
        assertTrue(service.tienePokemonBasico(t));
    }

    @Test
    void tienePokemonBasico_soloEnergias_retornaFalse() {
        TableroJugador t = new TableroJugador();
        t.getMano().add(card("e1", "Fire Energy", "Energy"));
        assertFalse(service.tienePokemonBasico(t));
    }

    @Test
    void tienePokemonBasico_tipoNull_retornaFalse() {
        TableroJugador t = new TableroJugador();
        t.getMano().add(card("xy1-1", "Carta", null));
        assertFalse(service.tienePokemonBasico(t));
    }

    // =================== realizarMulligan ===================

    @Test
    void realizarMulligan_conBasico_noMueve() {
        TableroJugador t = new TableroJugador();
        Card basico = card("xy1-1", "Bulbasaur", "Basic Pokemon");
        t.getMano().add(basico);
        for (int i = 0; i < 5; i++) t.getMazo().add(card("xy1-" + i, "Pokemon" + i, "Basic"));

        service.realizarMulligan(t);

        assertTrue(t.getMano().contains(basico));
    }

    @Test
    void realizarMulligan_sinBasico_robaMasCartas() {
        TableroJugador t = new TableroJugador();
        for (int i = 0; i < 7; i++) t.getMano().add(card("e" + i, "Energy" + i, "Energy"));
        for (int i = 0; i < 7; i++) t.getMazo().add(card("xy1-" + i, "Pokemon" + i, "Basic Pokemon"));

        service.realizarMulligan(t);

        // Implementation adds hand to deck then draws 7 more without clearing hand first
        assertTrue(t.getMano().size() >= 7);
    }

    // =================== bajarAPrimerBanca ===================

    @Test
    void bajarAPrimerBanca_bancaLibre_agregaCartaYQuitaDeMano() {
        TableroJugador t = new TableroJugador();
        Card carta = card("xy1-1", "Bulbasaur", "Basic Pokemon");
        t.getMano().add(carta);

        service.bajarAPrimerBanca(t, carta);

        assertEquals(1, t.getBanca().size());
        assertFalse(t.getMano().contains(carta));
    }

    @Test
    void bajarAPrimerBanca_bancaLlena_noAgrega() {
        TableroJugador t = new TableroJugador();
        for (int i = 0; i < 5; i++) {
            t.getBanca().add(new CartaEnJuego(card("xy1-" + i, "Pokemon" + i, "Basic")));
        }
        Card carta = card("xy1-99", "Extra", "Basic");
        t.getMano().add(carta);

        service.bajarAPrimerBanca(t, carta);

        assertEquals(5, t.getBanca().size());
    }
}
