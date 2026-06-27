package com.pokemon.tcg.model.battle.command;

import com.pokemon.tcg.model.Card;
import com.pokemon.tcg.model.battle.CartaEnJuego;
import com.pokemon.tcg.model.battle.Partida;
import com.pokemon.tcg.model.battle.TableroJugador;
import com.pokemon.tcg.model.battle.state.EstadoEsperandoInteraccion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GeomancyCommandTest {

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
    void execute_Jugador_RequestsInteraction() {
        partida.setJugadorUsername("TestUser");
        Card pCard = new Card();
        pCard.setId("xy1-1");
        pCard.setHp("100");
        jugador.getBanca().add(new CartaEnJuego(pCard));
        
        GeomancyCommand cmd = new GeomancyCommand();
        cmd.execute(partida, jugador, bot);
        
        assertTrue(partida.getEstado() instanceof EstadoEsperandoInteraccion);
        assertEquals("SELECT_BENCHED_POKEMON_FOR_GEOMANCY", partida.getPendingAction().getType());
        assertEquals("TestUser", partida.getPendingAction().getActor());
    }

    @Test
    void execute_Bot_AttachesFairyEnergy() {
        Card pCard = new Card();
        pCard.setId("xy1-1");
        pCard.setNombre("Benched");
        bot.getBanca().add(new CartaEnJuego(pCard));
        
        Card fairyEnergy = new Card();
        fairyEnergy.setSupertype("Energy");
        fairyEnergy.setTipo("Fairy");
        bot.getMazo().add(fairyEnergy);
        
        GeomancyCommand cmd = new GeomancyCommand();
        cmd.execute(partida, bot, jugador);
        
        assertTrue(bot.getMazo().isEmpty());
        assertEquals(1, bot.getBanca().get(0).getEnergiasUnidas().size());
        assertTrue(partida.getTurnLogs().stream().anyMatch(l -> l.contains("ENERGY_ATTACHED")));
    }
}
