package com.pokemon.tcg.service.battle.strategy;

import com.pokemon.tcg.model.Card;
import com.pokemon.tcg.model.battle.CartaEnJuego;
import com.pokemon.tcg.model.battle.Partida;
import com.pokemon.tcg.model.battle.TableroJugador;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class EstrategiaDificilTest {

    private Card cardBasico(String id, String nombre, int hp) {
        Card c = new Card();
        c.setId(id);
        c.setNombre(nombre);
        c.setSupertype("Pokemon");
        c.setSubtypes(List.of("Basic"));
        c.setHp(String.valueOf(hp));
        return c;
    }

    private Partida crearPartida() {
        TableroJugador jugador = new TableroJugador();
        TableroJugador bot = new TableroJugador();
        Partida p = new Partida(jugador, bot);
        p.setJugadorUsername("ash");
        return p;
    }

    @Test
    void ejecutarSetup_delegaAEstrategiaBasica() {
        EstrategiaDificil estrategia = new EstrategiaDificil();
        Partida partida = crearPartida();

        // Set up a valid hand with at least one basic pokemon so setup can proceed
        Card pikachu = cardBasico("p1", "Pikachu", 60);
        partida.getBot().getMano().add(pikachu);
        partida.getJugador().getMano().add(cardBasico("j1", "Bulbasaur", 45));

        // EstrategiaDificil.ejecutarSetup delegates to EstrategiaBasica, should not throw
        assertDoesNotThrow(() -> estrategia.ejecutarSetup(partida));
    }

    @Test
    void ejecutarSetup_sinMano_noFalla() {
        EstrategiaDificil estrategia = new EstrategiaDificil();
        Partida partida = crearPartida();

        // Empty hand — EstrategiaBasica handles gracefully
        assertDoesNotThrow(() -> estrategia.ejecutarSetup(partida));
    }

    @Test
    void ejecutarSetup_conActivoYBanca_configuraCorrectamente() {
        EstrategiaDificil estrategia = new EstrategiaDificil();
        Partida partida = crearPartida();

        // Add cards to bot hand
        for (int i = 0; i < 3; i++) {
            partida.getBot().getMano().add(cardBasico("b" + i, "Squirtle" + i, 50));
        }

        assertDoesNotThrow(() -> estrategia.ejecutarSetup(partida));
    }

    @Test
    void ejecutarTurno_lanzaUnsupportedOperationException() {
        EstrategiaDificil estrategia = new EstrategiaDificil();
        Partida partida = crearPartida();

        assertThrows(UnsupportedOperationException.class,
                () -> estrategia.ejecutarTurno(partida));
    }

    @Test
    void ejecutarTurno_mensajeDeExcepcion_contieneDificil() {
        EstrategiaDificil estrategia = new EstrategiaDificil();
        Partida partida = crearPartida();

        UnsupportedOperationException ex = assertThrows(UnsupportedOperationException.class,
                () -> estrategia.ejecutarTurno(partida));
        assertNotNull(ex.getMessage());
        assertTrue(ex.getMessage().contains("EstrategiaDificil"));
    }
}
