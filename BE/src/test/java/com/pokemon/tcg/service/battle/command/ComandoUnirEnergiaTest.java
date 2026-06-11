package com.pokemon.tcg.service.battle.command;

import com.pokemon.tcg.model.Card;
import com.pokemon.tcg.model.battle.CartaEnJuego;
import com.pokemon.tcg.model.battle.Partida;
import com.pokemon.tcg.model.battle.TableroJugador;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ComandoUnirEnergiaTest {

    @Test
    void permiteSoloUnaEnergiaPorTurno() {
        TableroJugador jugador = new TableroJugador();
        Partida partida = new Partida(jugador, new TableroJugador());
        CartaEnJuego objetivo = new CartaEnJuego(pokemon());
        Card primera = energy("Fire Energy");
        Card segunda = energy("Water Energy");
        jugador.getMano().add(primera);
        jugador.getMano().add(segunda);

        ComandoUnirEnergia comando = new ComandoUnirEnergia(objetivo, primera, jugador);
        assertTrue(comando.puedeEjecutar(partida));
        comando.ejecutar(partida);

        assertFalse(new ComandoUnirEnergia(objetivo, segunda, jugador).puedeEjecutar(partida));
    }

    private Card pokemon() {
        Card card = new Card();
        card.setNombre("Pikachu");
        card.setHp("60");
        return card;
    }

    private Card energy(String name) {
        Card card = new Card();
        card.setNombre(name);
        card.setSupertype("Energy");
        return card;
    }
}
