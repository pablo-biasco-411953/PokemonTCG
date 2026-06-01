package com.pokemon.tcg.service;

import com.pokemon.tcg.model.Card;
import com.pokemon.tcg.model.PokemonCard;
import com.pokemon.tcg.model.battle.CartaEnJuego;
import com.pokemon.tcg.model.battle.Partida;
import com.pokemon.tcg.model.battle.TableroJugador;
import org.junit.jupiter.api.Test;

import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BattleTurnServiceTest {

    private final BattleTurnService service = new BattleTurnService();

    @Test
    void limpiarActivoFinTurnoJugadorRemueveBloqueosYHabilitaAtaque() {
        TableroJugador jugador = new TableroJugador();
        CartaEnJuego activo = new CartaEnJuego(card("atk-1", "Pikachu", "60"));
        activo.agregarCondicion("CantRetreat");
        activo.agregarCondicion("Paralyzed");
        activo.setPuedeAtacar(false);
        jugador.setActivo(activo);

        service.limpiarActivoFinTurnoJugador(jugador);

        assertFalse(activo.getCondicionesEspeciales().contains("CantRetreat"));
        assertFalse(activo.getCondicionesEspeciales().contains("Paralyzed"));
        assertTrue(activo.isPuedeAtacar());
    }

    @Test
    void mantenimientoAplicaVenenoYDisparaKoSiElActivoCae() {
        TableroJugador jugador = new TableroJugador();
        CartaEnJuego activoJugador = new CartaEnJuego(card("atk-2", "Bulbasaur", "50"));
        activoJugador.setHpActual(10);
        activoJugador.agregarCondicion("Poisoned");
        jugador.setActivo(activoJugador);

        TableroJugador bot = new TableroJugador();
        CartaEnJuego activoBot = new CartaEnJuego(card("atk-3", "Charmander", "50"));
        bot.setActivo(activoBot);

        Partida partida = new Partida(jugador, bot);
        AtomicBoolean koInvocado = new AtomicBoolean(false);

        service.aplicarMantenimientoEntreTurnos(
                partida,
                new FixedRandom(false),
                (p, atacante, defensor) -> koInvocado.set(true)
        );

        assertEquals(0, activoJugador.getHpActual());
        assertTrue(koInvocado.get());
    }

    @Test
    void mantenimientoPuedeCurarQuemaduraYDormidoConCara() {
        TableroJugador jugador = new TableroJugador();
        CartaEnJuego activoJugador = new CartaEnJuego(card("atk-4", "Squirtle", "70"));
        activoJugador.setHpActual(70);
        activoJugador.agregarCondicion("Burned");
        activoJugador.agregarCondicion("Asleep");
        jugador.setActivo(activoJugador);

        TableroJugador bot = new TableroJugador();
        bot.setActivo(new CartaEnJuego(card("atk-5", "Eevee", "50")));

        Partida partida = new Partida(jugador, bot);

        service.aplicarMantenimientoEntreTurnos(
                partida,
                new FixedRandom(true),
                (p, atacante, defensor) -> {}
        );

        assertEquals(50, activoJugador.getHpActual());
        assertFalse(activoJugador.getCondicionesEspeciales().contains("Burned"));
        assertFalse(activoJugador.getCondicionesEspeciales().contains("Asleep"));
    }

    private Card card(String id, String nombre, String hp) {
        PokemonCard card = new PokemonCard();
        card.setId(id);
        card.setNombre(nombre);
        card.setHp(hp);
        return card;
    }

    private static final class FixedRandom extends Random {
        private final boolean value;

        private FixedRandom(boolean value) {
            this.value = value;
        }

        @Override
        public boolean nextBoolean() {
            return value;
        }
    }
}
