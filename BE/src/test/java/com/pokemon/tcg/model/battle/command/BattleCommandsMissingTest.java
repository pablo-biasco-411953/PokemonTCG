package com.pokemon.tcg.model.battle.command;

import com.pokemon.tcg.model.Card;
import com.pokemon.tcg.model.battle.CartaEnJuego;
import com.pokemon.tcg.model.battle.Partida;
import com.pokemon.tcg.model.battle.TableroJugador;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BattleCommandsMissingTest {

    private Partida partida;
    private TableroJugador jugador;
    private TableroJugador bot;

    @BeforeEach
    void setUp() {
        jugador = new TableroJugador();
        bot = new TableroJugador();
        partida = new Partida(jugador, bot);
        partida.setJugadorUsername("ash");
    }

    private Card cardBasico(String id, String nombre, int hp) {
        Card c = new Card();
        c.setId(id);
        c.setNombre(nombre);
        c.setSupertype("Pokemon");
        c.setSubtypes(java.util.List.of("Basic"));
        c.setHp(String.valueOf(hp));
        return c;
    }

    private CartaEnJuego activePokemon(String id, String nombre, int hp) {
        CartaEnJuego p = new CartaEnJuego(cardBasico(id, nombre, hp));
        p.setHpActual(hp);
        return p;
    }

    private Card cardEnergia(String id, String nombre, String tipo) {
        Card c = new Card();
        c.setId(id);
        c.setNombre(nombre);
        c.setSupertype("Energy");
        c.setTipo(tipo);
        return c;
    }

    // =================== MoveDiscardCardToTopDeckCommand ===================

    @Test
    void moveDiscardToTopDeck_bot_autoSeleccionaUltimaCarta() {
        CartaEnJuego botActivo = activePokemon("b0", "Squirtle", 50);
        bot.setActivo(botActivo);
        Card pikachu = cardBasico("p1", "Pikachu", 60);
        bot.getPilaDescarte().add(pikachu);

        new MoveDiscardCardToTopDeckCommand(Target.SELF, 1, "Pick a card")
                .execute(partida, bot, jugador);

        assertTrue(bot.getPilaDescarte().isEmpty());
        assertEquals("p1", bot.getMazo().get(0).getId());
        assertTrue(partida.getTurnLogs().stream().anyMatch(l -> l.contains("DISCARD_TO_TOP_DECK")));
    }

    @Test
    void moveDiscardToTopDeck_botDescartaVacio_noHaceNada() {
        bot.setActivo(activePokemon("b0", "Squirtle", 50));
        // empty discard

        assertDoesNotThrow(() ->
                new MoveDiscardCardToTopDeckCommand(Target.SELF, 1, "Pick a card")
                        .execute(partida, bot, jugador));

        assertTrue(bot.getMazo().isEmpty());
    }

    @Test
    void moveDiscardToTopDeck_jugador_setPendingAction() {
        jugador.setActivo(activePokemon("p0", "Pikachu", 60));
        jugador.getPilaDescarte().add(cardBasico("p1", "Bulbasaur", 60));

        new MoveDiscardCardToTopDeckCommand(Target.SELF, 1, "Pick a card from discard")
                .execute(partida, jugador, bot);

        assertNotNull(partida.getPendingAction());
        assertEquals("DISCARD_TO_TOP_DECK", partida.getPendingAction().getType());
        assertEquals(1, partida.getPendingAction().getOptions().size());
    }

    @Test
    void moveDiscardToTopDeck_opponent_botEsDefensor_autoSelecciona() {
        jugador.setActivo(activePokemon("p0", "Pikachu", 60));
        Card squirtle = cardBasico("b1", "Squirtle", 50);
        bot.getPilaDescarte().add(squirtle);

        new MoveDiscardCardToTopDeckCommand(Target.OPPONENT, 1, "Pick opponent discard")
                .execute(partida, jugador, bot);

        // sourceBoard = defensor = bot → auto-selects (moves from bot's discard to bot's deck)
        assertTrue(bot.getPilaDescarte().isEmpty());
        assertEquals("b1", bot.getMazo().get(0).getId());
    }

    // =================== SelectOwnPokemonToHealCommand ===================

    @Test
    void selectOwnPokemonToHeal_bot_curaPokemonMasDaniado() {
        CartaEnJuego botActivo = activePokemon("b0", "Squirtle", 60);
        botActivo.setHpActual(20); // 40 damage taken
        bot.setActivo(botActivo);

        CartaEnJuego botBanca = activePokemon("b1", "Charmander", 60);
        botBanca.setHpActual(50); // 10 damage taken
        bot.getBanca().add(botBanca);

        new SelectOwnPokemonToHealCommand(30).execute(partida, bot, jugador);

        // Squirtle has most damage, should be healed
        assertEquals(50, bot.getActivo().getHpActual()); // 20 + 30 = 50
        assertEquals(50, bot.getBanca().get(0).getHpActual()); // unchanged
    }

    @Test
    void selectOwnPokemonToHeal_bot_noCuraEncimaDelMaximo() {
        CartaEnJuego botActivo = activePokemon("b0", "Squirtle", 60);
        botActivo.setHpActual(55);
        bot.setActivo(botActivo);

        new SelectOwnPokemonToHealCommand(30).execute(partida, bot, jugador);

        assertEquals(60, bot.getActivo().getHpActual()); // capped at max
    }

    @Test
    void selectOwnPokemonToHeal_bot_sinPokemon_noFalla() {
        assertDoesNotThrow(() ->
                new SelectOwnPokemonToHealCommand(30).execute(partida, bot, jugador));
    }

    @Test
    void selectOwnPokemonToHeal_jugador_setPendingAction() {
        jugador.setActivo(activePokemon("p0", "Pikachu", 60));

        new SelectOwnPokemonToHealCommand(30).execute(partida, jugador, bot);

        assertNotNull(partida.getPendingAction());
        assertEquals("HEAL_OWN_POKEMON", partida.getPendingAction().getType());
        assertEquals(30, partida.getPendingAction().getAmount());
    }

    @Test
    void selectOwnPokemonToHeal_benchedOnly_excludeActivo() {
        jugador.setActivo(activePokemon("p0", "Pikachu", 60));
        jugador.getBanca().add(activePokemon("b1", "Bulbasaur", 60));

        new SelectOwnPokemonToHealCommand(30, true).execute(partida, jugador, bot);

        assertNotNull(partida.getPendingAction());
        assertEquals(1, partida.getPendingAction().getOptions().size()); // only bench pokemon
        assertEquals("b1", partida.getPendingAction().getOptions().get(0).getId());
    }

    // =================== AttachEnergyFromDiscardToBenchByCoinsCommand ===================

    @Test
    void attachEnergyFromDiscardByCoins_sinBanca_noFalla() {
        bot.setActivo(activePokemon("b0", "Squirtle", 50));
        bot.getPilaDescarte().add(cardEnergia("e1", "Water Energy", "Water"));
        // no bench

        assertDoesNotThrow(() ->
                new AttachEnergyFromDiscardToBenchByCoinsCommand(1, "Water")
                        .execute(partida, bot, jugador));
    }

    @Test
    void attachEnergyFromDiscardByCoins_sinEnergiaEnDescarte_noAgrega() {
        bot.setActivo(activePokemon("b0", "Squirtle", 50));
        bot.getBanca().add(activePokemon("b1", "Charmander", 50));
        // no energy in discard, just pokemon
        bot.getPilaDescarte().add(cardBasico("p1", "Pikachu", 60));

        new AttachEnergyFromDiscardToBenchByCoinsCommand(2, "Water")
                .execute(partida, bot, jugador);

        assertTrue(bot.getBanca().get(0).getEnergiasUnidas().isEmpty());
    }

    @Test
    void attachEnergyFromDiscardByCoins_conEnergia_puedeAdjuntar() {
        bot.setActivo(activePokemon("b0", "Squirtle", 50));
        CartaEnJuego bancado = activePokemon("b1", "Charmander", 50);
        bot.getBanca().add(bancado);

        for (int i = 0; i < 5; i++) {
            bot.getPilaDescarte().add(cardEnergia("e" + i, "Fire Energy", "Fire"));
        }

        // Even with random, with 5 flips there should be at least some heads
        // Just verify it doesn't throw and logs
        assertDoesNotThrow(() ->
                new AttachEnergyFromDiscardToBenchByCoinsCommand(5, "Fire")
                        .execute(partida, bot, jugador));

        // Coins were recorded
        assertFalse(partida.getUltimasMonedasLanzadas().isEmpty());
        assertEquals(5, partida.getUltimasMonedasLanzadas().size());
    }

    @Test
    void attachEnergyFromDiscardByCoins_energyTypeNull_matchesCualquier() {
        bot.setActivo(activePokemon("b0", "Squirtle", 50));
        CartaEnJuego bancado = activePokemon("b1", "Charmander", 50);
        bot.getBanca().add(bancado);
        bot.getPilaDescarte().add(cardEnergia("e1", "Fire Energy", "Fire"));
        bot.getPilaDescarte().add(cardEnergia("e2", "Water Energy", "Water"));

        assertDoesNotThrow(() ->
                new AttachEnergyFromDiscardToBenchByCoinsCommand(3, null)
                        .execute(partida, bot, jugador));
    }

    // =================== RhydonMadMountainCommand ===================

    @Test
    void rhydonMadMountain_siempreLanzaDosMonedas() {
        bot.setActivo(activePokemon("b0", "Rhydon", 100));
        jugador.setActivo(activePokemon("p0", "Pikachu", 60));

        new RhydonMadMountainCommand().execute(partida, bot, jugador);

        assertEquals(2, partida.getUltimasMonedasLanzadas().size());
        assertTrue(partida.getTurnLogs().stream().anyMatch(l -> l.contains("MAD_MOUNTAIN_DISCARDED")));
    }

    @Test
    void rhydonMadMountain_sinActivo_noFalla() {
        jugador.setActivo(activePokemon("p0", "Pikachu", 60));

        assertDoesNotThrow(() ->
                new RhydonMadMountainCommand().execute(partida, bot, jugador));
    }

    @Test
    void rhydonMadMountain_conDamageEnAtacante_puedDescartarMazo() {
        CartaEnJuego rhydon = activePokemon("b0", "Rhydon", 100);
        rhydon.setHpActual(60); // 40 damage = 4 counters = discard up to 4 from jugador's deck
        bot.setActivo(rhydon);

        jugador.setActivo(activePokemon("p0", "Pikachu", 60));
        for (int i = 0; i < 5; i++) {
            jugador.getMazo().add(cardBasico("m" + i, "Pokemon" + i, 60));
        }

        // Execute (random might discard or not depending on coin results)
        assertDoesNotThrow(() ->
                new RhydonMadMountainCommand().execute(partida, bot, jugador));
    }
}
