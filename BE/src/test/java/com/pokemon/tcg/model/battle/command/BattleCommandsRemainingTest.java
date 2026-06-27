package com.pokemon.tcg.model.battle.command;

import com.pokemon.tcg.model.Card;
import com.pokemon.tcg.model.battle.CartaEnJuego;
import com.pokemon.tcg.model.battle.Partida;
import com.pokemon.tcg.model.battle.TableroJugador;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BattleCommandsRemainingTest {

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

    private Card card(String id, String nombre, String hp) {
        Card c = new Card();
        c.setId(id);
        c.setNombre(nombre);
        c.setHp(hp);
        return c;
    }

    private CartaEnJuego activePokemon(String id, String nombre, int hp) {
        CartaEnJuego p = new CartaEnJuego(card(id, nombre, String.valueOf(hp)));
        p.setHpActual(hp);
        return p;
    }

    // =================== AtaquePotenciadoSiguienteTurnoCommand ===================

    @Test
    void ataquePotenciado_conActivo_setAtaqueYDanio() {
        jugador.setActivo(activePokemon("p1", "Pikachu", 60));

        new AtaquePotenciadoSiguienteTurnoCommand("Thunder", 40)
                .execute(partida, jugador, bot);

        assertEquals("Thunder", jugador.getActivo().getAtaquePotenciadoSiguienteTurno());
        assertEquals(40, jugador.getActivo().getDanioExtraSiguienteTurno());
    }

    @Test
    void ataquePotenciado_sinActivo_noFalla() {
        assertDoesNotThrow(() ->
                new AtaquePotenciadoSiguienteTurnoCommand("Thunder", 40)
                        .execute(partida, jugador, bot));
    }

    // =================== SetAttackBlockNextTurnCommand ===================

    @Test
    void setAttackBlock_self_setDebeLanzarMoneda() {
        jugador.setActivo(activePokemon("p1", "Pikachu", 60));

        new SetAttackBlockNextTurnCommand(Target.SELF)
                .execute(partida, jugador, bot);

        assertTrue(jugador.getActivo().isDebeLanzarMonedaSiAtaca());
    }

    @Test
    void setAttackBlock_opponent_setDebeLanzarMonedaEnOponente() {
        bot.setActivo(activePokemon("p1", "Squirtle", 50));

        new SetAttackBlockNextTurnCommand(Target.OPPONENT)
                .execute(partida, jugador, bot);

        assertTrue(bot.getActivo().isDebeLanzarMonedaSiAtaca());
    }

    @Test
    void setAttackBlock_sinActivo_noFalla() {
        assertDoesNotThrow(() ->
                new SetAttackBlockNextTurnCommand(Target.SELF)
                        .execute(partida, jugador, bot));
    }

    // =================== SetCannotAttackDefendingCommand ===================

    @Test
    void setCannotAttackDefending_marcaNoPuedeAtacar() {
        bot.setActivo(activePokemon("p1", "Squirtle", 50));

        new SetCannotAttackDefendingCommand()
                .execute(partida, jugador, bot);

        assertFalse(bot.getActivo().isPuedeAtacar());
    }

    @Test
    void setCannotAttackDefending_sinActivo_noFalla() {
        assertDoesNotThrow(() ->
                new SetCannotAttackDefendingCommand()
                        .execute(partida, jugador, bot));
    }

    // =================== SetPreventDamageThresholdCommand ===================

    @Test
    void setPreventDamageThreshold_setUmbral() {
        jugador.setActivo(activePokemon("p1", "Squirtle", 50));

        new SetPreventDamageThresholdCommand(30)
                .execute(partida, jugador, bot);

        assertEquals(30, jugador.getActivo().getPreventDamageThreshold());
    }

    @Test
    void setPreventDamageThreshold_sinActivo_noFalla() {
        assertDoesNotThrow(() ->
                new SetPreventDamageThresholdCommand(30)
                        .execute(partida, jugador, bot));
    }

    // =================== TormentBlockAttackCommand ===================

    @Test
    void tormentBlock_bloqueaAtaqueEspecificado() {
        CartaEnJuego activo = activePokemon("p1", "Pikachu", 60);
        com.pokemon.tcg.model.battle.Ataque thunder = new com.pokemon.tcg.model.battle.Ataque();
        thunder.setNombre("Thunder");
        thunder.setDanio(60);
        activo.getCard().cargarAtaquesDesdeJson(null); // clear
        activo.getCard().reemplazarAtaques(java.util.List.of(thunder));
        bot.setActivo(activo);

        new TormentBlockAttackCommand("Thunder")
                .execute(partida, jugador, bot);

        assertEquals("Thunder", bot.getActivo().getAtaqueBloqueadoSiguienteTurno());
    }

    @Test
    void tormentBlock_sinAtaques_noFalla() {
        bot.setActivo(activePokemon("p1", "Pikachu", 60));

        assertDoesNotThrow(() ->
                new TormentBlockAttackCommand("Thunder")
                        .execute(partida, jugador, bot));
    }

    // =================== DiscardAttachedEnergyOfTypeCommand ===================

    @Test
    void discardAttachedEnergyOfType_descartaEnergiaDelTipo() {
        CartaEnJuego activo = activePokemon("p1", "Pikachu", 60);
        Card lightning = new Card();
        lightning.setId("e1");
        lightning.setNombre("Lightning Energy");
        lightning.setTipo("Lightning");
        lightning.setSupertype("Energy");
        activo.getEnergiasUnidas().add(lightning);
        bot.setActivo(activo);

        new DiscardAttachedEnergyOfTypeCommand("Lightning", Target.OPPONENT)
                .execute(partida, jugador, bot);

        assertTrue(bot.getActivo().getEnergiasUnidas().isEmpty());
        assertEquals(1, bot.getPilaDescarte().size());
    }

    @Test
    void discardAttachedEnergyOfType_Any_descartaCualquier() {
        CartaEnJuego activo = activePokemon("p1", "Bulbasaur", 60);
        Card fire = new Card();
        fire.setId("e1");
        fire.setNombre("Fire Energy");
        fire.setSupertype("Energy");
        activo.getEnergiasUnidas().add(fire);
        jugador.setActivo(activo);

        new DiscardAttachedEnergyOfTypeCommand("Any", Target.SELF)
                .execute(partida, jugador, bot);

        assertTrue(jugador.getActivo().getEnergiasUnidas().isEmpty());
    }

    // =================== MoveOpponentActiveEnergyToBenchCommand ===================

    @Test
    void moveOpponentEnergyToBench_conBancada_mueveEnergia() {
        CartaEnJuego activoRival = activePokemon("p1", "Charizard", 120);
        Card fire = new Card();
        fire.setId("e1");
        fire.setNombre("Fire Energy");
        fire.setSupertype("Energy");
        activoRival.getEnergiasUnidas().add(fire);
        jugador.setActivo(activoRival);

        CartaEnJuego bancado = activePokemon("b1", "Charmander", 50);
        jugador.getBanca().add(bancado);

        new MoveOpponentActiveEnergyToBenchCommand("yes")
                .execute(partida, bot, jugador);

        assertTrue(jugador.getActivo().getEnergiasUnidas().isEmpty());
        assertEquals(1, bancado.getEnergiasUnidas().size());
    }

    @Test
    void moveOpponentEnergyToBench_sinBanca_noFalla() {
        jugador.setActivo(activePokemon("p1", "Charizard", 120));

        assertDoesNotThrow(() ->
                new MoveOpponentActiveEnergyToBenchCommand()
                        .execute(partida, bot, jugador));
    }

    // =================== OptionalDiscardEnergyForDamageCommand ===================

    @Test
    void optionalDiscardEnergyForDamage_conEnergia_descartaYDana() {
        CartaEnJuego activo = activePokemon("p1", "Pikachu", 60);
        Card lightning = new Card();
        lightning.setId("e1");
        lightning.setNombre("Lightning Energy");
        lightning.setSupertype("Energy");
        activo.getEnergiasUnidas().add(lightning);
        bot.setActivo(activo);  // bot attacks, so bot discards own energy

        new OptionalDiscardEnergyForDamageCommand(50)
                .execute(partida, bot, jugador);

        assertTrue(bot.getActivo().getEnergiasUnidas().isEmpty());
        assertFalse(partida.getExecutionQueue().isEmpty());
    }

    @Test
    void optionalDiscardEnergyForDamage_sinEnergia_noDescarta() {
        bot.setActivo(activePokemon("p1", "Pikachu", 60));

        new OptionalDiscardEnergyForDamageCommand(50)
                .execute(partida, bot, jugador);

        assertTrue(partida.getExecutionQueue().isEmpty());
    }

    // =================== PeekTopDeckCommand ===================

    @Test
    void peekTopDeck_botAtaca_logueaTopDeck() {
        Card top = card("xy1-1", "Bulbasaur", "60");
        bot.getMazo().add(top);

        new PeekTopDeckCommand(1, "Peek top deck")
                .execute(partida, bot, jugador);

        assertTrue(partida.getTurnLogs().stream().anyMatch(l -> l.contains("DECK_PEEKED")));
    }

    @Test
    void peekTopDeck_mazoVacio_noFalla() {
        assertDoesNotThrow(() ->
                new PeekTopDeckCommand(3, "Peek top deck").execute(partida, bot, jugador));
    }

    // =================== DiscardTopDeckAttachEnergyCommand ===================

    @Test
    void discardTopDeckAttachEnergy_energyOnTop_attachesToActive() {
        CartaEnJuego activo = activePokemon("p1", "Pikachu", 60);
        jugador.setActivo(activo);

        Card energyTop = new Card();
        energyTop.setId("e1");
        energyTop.setNombre("Lightning Energy");
        energyTop.setSupertype("Energy");
        jugador.getMazo().add(energyTop);

        new DiscardTopDeckAttachEnergyCommand("lightning")
                .execute(partida, jugador, bot);

        assertEquals(1, jugador.getActivo().getEnergiasUnidas().size());
        assertTrue(jugador.getMazo().isEmpty());
    }

    @Test
    void discardTopDeckAttachEnergy_nonEnergy_goesToDiscard() {
        CartaEnJuego activo = activePokemon("p1", "Pikachu", 60);
        jugador.setActivo(activo);
        jugador.getMazo().add(card("xy1-1", "Bulbasaur", "60"));

        new DiscardTopDeckAttachEnergyCommand("lightning")
                .execute(partida, jugador, bot);

        assertTrue(jugador.getActivo().getEnergiasUnidas().isEmpty());
        assertEquals(1, jugador.getPilaDescarte().size());
    }

    // =================== RandomAsleepOrPoisonedCommand ===================

    @Test
    void randomAsleepOrPoisoned_conActivo_aplicaCondicion() {
        bot.setActivo(activePokemon("p1", "Squirtle", 50));

        new RandomAsleepOrPoisonedCommand(Target.OPPONENT).execute(partida, jugador, bot);

        boolean hasCondition = bot.getActivo().getCondicionesEspeciales().contains("Asleep")
                || bot.getActivo().getCondicionesEspeciales().contains("Poisoned");
        assertTrue(hasCondition);
    }

    @Test
    void randomAsleepOrPoisoned_sinActivo_noFalla() {
        assertDoesNotThrow(() ->
                new RandomAsleepOrPoisonedCommand(Target.OPPONENT).execute(partida, jugador, bot));
    }

    // =================== OpponentShuffleHandDrawCommand ===================

    @Test
    void opponentShuffleHandDraw_conMano_shuffleYRoba() {
        bot.getMano().add(card("xy1-1", "Bulbasaur", "60"));
        bot.getMano().add(card("xy1-2", "Charmander", "50"));
        bot.getMazo().add(card("xy1-3", "Squirtle", "40"));
        bot.getMazo().add(card("xy1-4", "Jigglypuff", "70"));
        bot.getMazo().add(card("xy1-5", "Geodude", "80"));

        new OpponentShuffleHandDrawCommand(3).execute(partida, jugador, bot);

        assertTrue(bot.getMano().size() <= 3);
    }

    @Test
    void opponentShuffleHandDraw_manoVacia_roba() {
        bot.getMazo().add(card("xy1-1", "Bulbasaur", "60"));

        new OpponentShuffleHandDrawCommand(1).execute(partida, jugador, bot);

        // hand becomes empty after shuffle + re-draw
        assertFalse(bot.getMano().isEmpty());
    }
}
