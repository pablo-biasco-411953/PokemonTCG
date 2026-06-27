package com.pokemon.tcg.model.battle.command;

import com.pokemon.tcg.model.Card;
import com.pokemon.tcg.model.battle.Partida;
import com.pokemon.tcg.model.battle.TableroJugador;
import com.pokemon.tcg.model.battle.state.EstadoEsperandoInteraccion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PickupCommandTest {

    private Partida partida;
    private TableroJugador jugador;
    private TableroJugador bot;

    @BeforeEach
    void setUp() {
        jugador = new TableroJugador();
        bot = new TableroJugador();
        partida = new Partida(jugador, bot);
    }

    @Test
    void execute_NoItemsInDiscard_DoesNothing() {
        PickupCommand cmd = new PickupCommand(1);
        cmd.execute(partida, jugador, bot);
        assertTrue(partida.getTurnLogs().stream().anyMatch(l -> l.contains("PICKUP_NO_ITEMS_FOUND")));
    }

    @Test
    void execute_Jugador_RequestsInteraction() {
        partida.setJugadorUsername("TestUser");
        Card item = new Card();
        item.setSupertype("Trainer");
        item.setSubtypes(java.util.List.of("Item"));
        jugador.getPilaDescarte().add(item);
        
        PickupCommand cmd = new PickupCommand(1);
        cmd.execute(partida, jugador, bot);
        
        assertTrue(partida.getEstado() instanceof EstadoEsperandoInteraccion);
        assertEquals("SELECT_DISCARD_ITEMS_FOR_PICKUP", partida.getPendingAction().getType());
    }

    @Test
    void execute_Bot_RetrievesItem() {
        Card item = new Card();
        item.setNombre("Potion");
        item.setSupertype("Trainer");
        item.setSubtypes(java.util.List.of("Item"));
        bot.getPilaDescarte().add(item);
        
        PickupCommand cmd = new PickupCommand(1);
        cmd.execute(partida, bot, jugador);
        
        assertTrue(bot.getPilaDescarte().isEmpty());
        assertEquals(1, bot.getMano().size());
        assertTrue(partida.getTurnLogs().stream().anyMatch(l -> l.contains("PICKUP_RESOLVED")));
    }
}
