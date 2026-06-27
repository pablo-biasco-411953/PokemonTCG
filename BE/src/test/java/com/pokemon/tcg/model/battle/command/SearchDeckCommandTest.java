package com.pokemon.tcg.model.battle.command;

import com.pokemon.tcg.model.Card;
import com.pokemon.tcg.model.battle.CartaEnJuego;
import com.pokemon.tcg.model.battle.Partida;
import com.pokemon.tcg.model.battle.TableroJugador;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SearchDeckCommandTest {

    private Card card(String id, String supertype, String subtype, String type, String nombre) {
        Card c = new Card();
        c.setId(id);
        c.setSupertype(supertype);
        c.setSubtypes(subtype != null ? List.of(subtype) : null);
        c.setTipo(type);
        c.setNombre(nombre != null ? nombre : "TestCard");
        return c;
    }

    @Test
    void execute_Bot_SelectPokemonForGatherEnergy_Active() {
        SearchDeckCommand cmd = new SearchDeckCommand("Energy", null, null, "SELECT_POKEMON_FOR_GATHER_ENERGY", 2, "Search energy");
        TableroJugador bot = new TableroJugador();
        bot.getMazo().add(card("1", "Energy", null, null, null));
        bot.getMazo().add(card("2", "Energy", null, null, null));
        
        CartaEnJuego activo = new CartaEnJuego(card("p1", "Pokemon", null, null, "Pikachu"));
        bot.setActivo(activo);

        Partida partida = new Partida(new TableroJugador(), bot);
        cmd.execute(partida, bot, partida.getJugador());

        assertEquals(2, activo.getEnergiasUnidas().size());
        assertEquals(0, bot.getMazo().size());
        assertTrue(partida.getTurnLogs().stream().anyMatch(l -> l.contains("ENERGY_ATTACHED:BOT:Pikachu")));
    }

    @Test
    void execute_Bot_SelectPokemonForGatherEnergy_Banca() {
        SearchDeckCommand cmd = new SearchDeckCommand("Energy", null, null, "SELECT_POKEMON_FOR_GATHER_ENERGY", 1, "Search energy");
        TableroJugador bot = new TableroJugador();
        bot.getMazo().add(card("1", "Energy", null, null, null));
        
        CartaEnJuego banca = new CartaEnJuego(card("p1", "Pokemon", null, null, "Pikachu"));
        bot.getBanca().add(banca);

        Partida partida = new Partida(new TableroJugador(), bot);
        cmd.execute(partida, bot, partida.getJugador());

        assertEquals(1, banca.getEnergiasUnidas().size());
    }

    @Test
    void execute_Bot_SelectPokemonForGatherEnergy_HandFallback() {
        SearchDeckCommand cmd = new SearchDeckCommand("Energy", null, null, "SELECT_POKEMON_FOR_GATHER_ENERGY", 1, "Search energy");
        TableroJugador bot = new TableroJugador();
        bot.getMazo().add(card("1", "Energy", null, null, null));
        
        Partida partida = new Partida(new TableroJugador(), bot);
        cmd.execute(partida, bot, partida.getJugador());

        assertEquals(1, bot.getMano().size());
    }

    @Test
    void execute_Bot_AttachActiveAndSwitch() {
        SearchDeckCommand cmd = new SearchDeckCommand("Energy", null, null, "ATTACH_ACTIVE_AND_SWITCH", 1, "Search");
        TableroJugador bot = new TableroJugador();
        bot.getMazo().add(card("1", "Energy", null, null, null));
        
        CartaEnJuego activo = new CartaEnJuego(card("p1", "Pokemon", null, null, "Pikachu"));
        bot.setActivo(activo);
        CartaEnJuego banca = new CartaEnJuego(card("p2", "Pokemon", null, null, "Bulbasaur"));
        bot.getBanca().add(banca);

        Partida partida = new Partida(new TableroJugador(), bot);
        cmd.execute(partida, bot, partida.getJugador());

        assertEquals(1, bot.getBanca().get(0).getEnergiasUnidas().size()); // It was active, attached, then switched!
        assertEquals("Bulbasaur", bot.getActivo().getCard().getNombre());
        assertTrue(partida.getTurnLogs().stream().anyMatch(l -> l.contains("ACTIVE_SWITCHED:BOT:Bulbasaur")));
    }

    @Test
    void execute_Bot_ApplyCardsHand() {
        SearchDeckCommand cmd = new SearchDeckCommand(null, null, null, "HAND", 1, "Search");
        TableroJugador bot = new TableroJugador();
        bot.getMazo().add(card("1", "Trainer", null, null, null));
        
        Partida partida = new Partida(new TableroJugador(), bot);
        cmd.execute(partida, bot, partida.getJugador());

        assertEquals(1, bot.getMano().size());
    }

    @Test
    void execute_EmptyLegal_ShufflesAndReturns() {
        SearchDeckCommand cmd = new SearchDeckCommand("Energy", null, null, "HAND", 1, "Search");
        TableroJugador bot = new TableroJugador();
        bot.getMazo().add(card("1", "Trainer", null, null, null)); // No energy!
        
        Partida partida = new Partida(new TableroJugador(), bot);
        cmd.execute(partida, bot, partida.getJugador());

        assertEquals(0, bot.getMano().size());
    }

    @Test
    void execute_Jugador_CreatesPendingAction() {
        SearchDeckCommand cmd = new SearchDeckCommand("Energy", "Basic", "Fire", "HAND", 1, "Search energy");
        TableroJugador jugador = new TableroJugador();
        jugador.getMazo().add(card("1", "Energy", "Basic", "Fire", "Fire Energy"));

        Partida partida = new Partida(jugador, new TableroJugador());
        partida.setJugadorUsername("ash");
        cmd.execute(partida, jugador, partida.getBot());

        assertNotNull(partida.getPendingAction());
        assertEquals("SEARCH_DECK", partida.getPendingAction().getType());
        assertEquals(1, partida.getPendingAction().getOptions().size());
    }
    
    @Test
    void matches_ComplexCriteria() {
        SearchDeckCommand cmd = new SearchDeckCommand("Pokemon", "EX", "Water", "HAND", 1, "Search");
        TableroJugador jugador = new TableroJugador();
        jugador.getMazo().add(card("1", "Pokémon", "EX", "Water", "Blastoise-EX"));
        jugador.getMazo().add(card("2", "Pokémon", "EX", "Fire", "Charizard-EX")); // Wrong type
        jugador.getMazo().add(card("3", "Energy", null, null, "Water Energy")); // Wrong supertype
        
        Partida partida = new Partida(jugador, new TableroJugador());
        partida.setJugadorUsername("ash");
        cmd.execute(partida, jugador, partida.getBot());
        
        assertNotNull(partida.getPendingAction());
        assertEquals(1, partida.getPendingAction().getOptions().size());
        assertEquals("1", partida.getPendingAction().getOptions().get(0).getId());
    }
}
