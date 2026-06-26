package com.pokemon.tcg.model.battle.command;

import com.pokemon.tcg.model.Card;
import com.pokemon.tcg.model.battle.CartaEnJuego;
import com.pokemon.tcg.model.battle.Partida;
import com.pokemon.tcg.model.battle.TableroJugador;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BattleCommandsExtendedTest {

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

    private Card energyCard(String nombre, String tipo) {
        Card c = new Card();
        c.setId("e-" + nombre);
        c.setNombre(nombre);
        c.setTipo(tipo);
        c.setSupertype("Energy");
        return c;
    }

    private CartaEnJuego activePokemon(String id, String nombre, String hp) {
        CartaEnJuego p = new CartaEnJuego(card(id, nombre, hp));
        p.setHpActual(Integer.parseInt(hp));
        return p;
    }

    // =================== ConditionalDamageMultiplierCommand ===================

    @Test
    void conditionalDamage_prizeCards_calculaCorectamente() {
        jugador.getPremios().add(card("p1", "Prize1", "60"));
        jugador.getPremios().add(card("p2", "Prize2", "60"));

        new ConditionalDamageMultiplierCommand(10, 20, "PRIZE_CARDS", null)
                .execute(partida, jugador, bot);

        DamageCommand cmd = (DamageCommand) partida.getExecutionQueue().peekFirst();
        assertNotNull(cmd);
        assertEquals(50, cmd.getAmount()); // 10 base + 2 prizes * 20
    }

    @Test
    void conditionalDamage_opponentType_matchingType_suma() {
        CartaEnJuego oponente = activePokemon("p1", "Charizard", "120");
        oponente.getCard().setTipo("Fire");
        bot.setActivo(oponente);

        new ConditionalDamageMultiplierCommand(20, 30, "OPPONENT_TYPE", "Fire")
                .execute(partida, jugador, bot);

        DamageCommand cmd = (DamageCommand) partida.getExecutionQueue().peekFirst();
        assertEquals(50, cmd.getAmount()); // 20 + 30
    }

    @Test
    void conditionalDamage_opponentType_noMatch_baseOnly() {
        CartaEnJuego oponente = activePokemon("p1", "Squirtle", "50");
        oponente.getCard().setTipo("Water");
        bot.setActivo(oponente);

        new ConditionalDamageMultiplierCommand(20, 30, "OPPONENT_TYPE", "Fire")
                .execute(partida, jugador, bot);

        DamageCommand cmd = (DamageCommand) partida.getExecutionQueue().peekFirst();
        assertEquals(20, cmd.getAmount());
    }

    @Test
    void conditionalDamage_benchedPokemon_porCadaBanca() {
        jugador.getBanca().add(activePokemon("b1", "Bulbasaur", "60"));
        jugador.getBanca().add(activePokemon("b2", "Charmander", "50"));

        new ConditionalDamageMultiplierCommand(10, 10, "BENCHED_POKEMON", null)
                .execute(partida, jugador, bot);

        DamageCommand cmd = (DamageCommand) partida.getExecutionQueue().peekFirst();
        assertEquals(30, cmd.getAmount()); // 10 + 2*10
    }

    @Test
    void conditionalDamage_bothActiveEnergy_sumaBoth() {
        CartaEnJuego activo = activePokemon("p1", "Pikachu", "60");
        activo.getEnergiasUnidas().add(energyCard("Lightning Energy", "Lightning"));
        jugador.setActivo(activo);

        CartaEnJuego oponente = activePokemon("p2", "Squirtle", "50");
        oponente.getEnergiasUnidas().add(energyCard("Water Energy", "Water"));
        oponente.getEnergiasUnidas().add(energyCard("Water Energy 2", "Water"));
        bot.setActivo(oponente);

        new ConditionalDamageMultiplierCommand(0, 10, "BOTH_ACTIVE_ENERGY", null)
                .execute(partida, jugador, bot);

        DamageCommand cmd = (DamageCommand) partida.getExecutionQueue().peekFirst();
        assertEquals(30, cmd.getAmount()); // (1+2) * 10
    }

    @Test
    void conditionalDamage_hasEnergyType_tieneEnergia_suma() {
        CartaEnJuego activo = activePokemon("p1", "Charmander", "50");
        activo.getEnergiasUnidas().add(energyCard("Fire Energy", "Fire"));
        jugador.setActivo(activo);

        new ConditionalDamageMultiplierCommand(10, 30, "HAS_ENERGY_TYPE", "Fire")
                .execute(partida, jugador, bot);

        DamageCommand cmd = (DamageCommand) partida.getExecutionQueue().peekFirst();
        assertEquals(40, cmd.getAmount());
    }

    @Test
    void conditionalDamage_energyCountOfType_cuentaEnergiasEspecificas() {
        CartaEnJuego activo = activePokemon("p1", "Pikachu", "60");
        activo.getEnergiasUnidas().add(energyCard("Lightning Energy", "Lightning"));
        activo.getEnergiasUnidas().add(energyCard("Lightning Energy 2", "Lightning"));
        activo.getEnergiasUnidas().add(energyCard("Water Energy", "Water"));
        jugador.setActivo(activo);

        new ConditionalDamageMultiplierCommand(0, 20, "ENERGY_COUNT_OF_TYPE", "Lightning")
                .execute(partida, jugador, bot);

        DamageCommand cmd = (DamageCommand) partida.getExecutionQueue().peekFirst();
        assertEquals(40, cmd.getAmount()); // 2 lightning * 20
    }

    // =================== AddDamageByDamageCountersCommand ===================

    @Test
    void damageByCounters_selfConDanio_agrega() {
        CartaEnJuego activo = activePokemon("p1", "Pikachu", "60");
        activo.setHpActual(30); // 30 damage taken = 3 counters
        jugador.setActivo(activo);

        new AddDamageByDamageCountersCommand(Target.SELF, 10)
                .execute(partida, jugador, bot);

        DamageCommand cmd = (DamageCommand) partida.getExecutionQueue().peekFirst();
        assertNotNull(cmd);
        assertEquals(30, cmd.getAmount()); // 3 counters * 10
    }

    @Test
    void damageByCounters_sinDanio_noAgrega() {
        CartaEnJuego activo = activePokemon("p1", "Bulbasaur", "60");
        activo.setHpActual(60); // full hp, no counters
        jugador.setActivo(activo);

        new AddDamageByDamageCountersCommand(Target.SELF, 10)
                .execute(partida, jugador, bot);

        assertTrue(partida.getExecutionQueue().isEmpty());
    }

    @Test
    void damageByCounters_opponentConDanio_agrega() {
        CartaEnJuego oponente = activePokemon("p1", "Squirtle", "50");
        oponente.setHpActual(10); // 40 damage = 4 counters
        bot.setActivo(oponente);

        new AddDamageByDamageCountersCommand(Target.OPPONENT, 20)
                .execute(partida, jugador, bot);

        DamageCommand cmd = (DamageCommand) partida.getExecutionQueue().peekFirst();
        assertEquals(80, cmd.getAmount()); // 4 counters * 20
    }

    // =================== AddDamageByDifferentBasicEnergyTypesCommand ===================

    @Test
    void damageByEnergyTypes_dostipos_calculaCorrectamente() {
        CartaEnJuego activo = activePokemon("p1", "Charizard", "120");
        activo.getEnergiasUnidas().add(energyCard("Fire Energy", "Fire"));
        activo.getEnergiasUnidas().add(energyCard("Water Energy", "Water"));
        jugador.setActivo(activo);

        new AddDamageByDifferentBasicEnergyTypesCommand(20)
                .execute(partida, jugador, bot);

        DamageCommand cmd = (DamageCommand) partida.getExecutionQueue().peekFirst();
        assertNotNull(cmd);
        assertEquals(40, cmd.getAmount()); // 2 types * 20
    }

    @Test
    void damageByEnergyTypes_unSoloTipo_calculaUno() {
        CartaEnJuego activo = activePokemon("p1", "Pikachu", "60");
        activo.getEnergiasUnidas().add(energyCard("Lightning Energy", "Lightning"));
        activo.getEnergiasUnidas().add(energyCard("Lightning Energy 2", "Lightning"));
        jugador.setActivo(activo);

        new AddDamageByDifferentBasicEnergyTypesCommand(30)
                .execute(partida, jugador, bot);

        DamageCommand cmd = (DamageCommand) partida.getExecutionQueue().peekFirst();
        assertEquals(30, cmd.getAmount()); // 1 type * 30
    }

    @Test
    void damageByEnergyTypes_sinActivo_noHaceNada() {
        new AddDamageByDifferentBasicEnergyTypesCommand(20)
                .execute(partida, jugador, bot);

        assertTrue(partida.getExecutionQueue().isEmpty());
    }

    // =================== ApplyBothActiveStatusConditionCommand ===================

    @Test
    void applyBothStatus_ambosActivos_aplicaAmbos() {
        jugador.setActivo(activePokemon("p1", "Bulbasaur", "60"));
        bot.setActivo(activePokemon("p2", "Squirtle", "50"));

        new ApplyBothActiveStatusConditionCommand("Poisoned")
                .execute(partida, jugador, bot);

        assertTrue(jugador.getActivo().getCondicionesEspeciales().contains("Poisoned"));
        assertTrue(bot.getActivo().getCondicionesEspeciales().contains("Poisoned"));
    }

    @Test
    void applyBothStatus_sinActivo_noFalla() {
        assertDoesNotThrow(() ->
                new ApplyBothActiveStatusConditionCommand("Asleep")
                        .execute(partida, jugador, bot));
    }

    // =================== DiscardTopDeckCommand ===================

    @Test
    void discardTopDeck_self_descartaCartasDelMazo() {
        jugador.getMazo().add(card("xy1-1", "Bulbasaur", "60"));
        jugador.getMazo().add(card("xy1-2", "Charmander", "50"));

        new DiscardTopDeckCommand(Target.SELF, 1)
                .execute(partida, jugador, bot);

        assertEquals(1, jugador.getMazo().size());
        assertEquals(1, jugador.getPilaDescarte().size());
        assertEquals("Bulbasaur", jugador.getPilaDescarte().get(0).getNombre());
    }

    @Test
    void discardTopDeck_multiple_descartaVarias() {
        for (int i = 0; i < 5; i++) {
            jugador.getMazo().add(card("xy1-" + i, "Pokemon" + i, "60"));
        }

        new DiscardTopDeckCommand(Target.SELF, 3)
                .execute(partida, jugador, bot);

        assertEquals(2, jugador.getMazo().size());
        assertEquals(3, jugador.getPilaDescarte().size());
    }

    @Test
    void discardTopDeck_opponent_descartaDeOponente() {
        bot.getMazo().add(card("xy1-1", "Squirtle", "50"));

        new DiscardTopDeckCommand(Target.OPPONENT, 1)
                .execute(partida, jugador, bot);

        assertTrue(bot.getMazo().isEmpty());
        assertEquals(1, bot.getPilaDescarte().size());
    }

    @Test
    void discardTopDeck_mazoVacio_noFalla() {
        assertDoesNotThrow(() ->
                new DiscardTopDeckCommand(Target.SELF, 2)
                        .execute(partida, jugador, bot));
    }

    // =================== SelfBenchDamageCommand ===================

    @Test
    void selfBenchDamage_reduceHpATodoLaBanca() {
        CartaEnJuego b1 = activePokemon("b1", "Bulbasaur", "60");
        CartaEnJuego b2 = activePokemon("b2", "Charmander", "50");
        jugador.getBanca().add(b1);
        jugador.getBanca().add(b2);

        new SelfBenchDamageCommand(10).execute(partida, jugador, bot);

        assertEquals(50, b1.getHpActual());
        assertEquals(40, b2.getHpActual());
    }

    @Test
    void selfBenchDamage_noPuedeBajarDeCero() {
        CartaEnJuego b1 = activePokemon("b1", "Bulbasaur", "60");
        b1.setHpActual(5);
        jugador.getBanca().add(b1);

        new SelfBenchDamageCommand(20).execute(partida, jugador, bot);

        assertEquals(0, b1.getHpActual());
    }

    @Test
    void selfBenchDamage_getAmount() {
        assertEquals(30, new SelfBenchDamageCommand(30).getAmount());
    }

    // =================== AttachEnergyFromDiscardToBenchCommand ===================

    @Test
    void attachEnergyFromDiscard_exitoso_muevePilaDescarte() {
        CartaEnJuego bancado = activePokemon("b1", "Pikachu", "60");
        jugador.getBanca().add(bancado);

        Card energyInDiscard = new Card();
        energyInDiscard.setId("e1");
        energyInDiscard.setNombre("Lightning Energy");
        energyInDiscard.setTipo("Lightning");
        energyInDiscard.setSupertype("Energy");
        jugador.getPilaDescarte().add(energyInDiscard);

        new AttachEnergyFromDiscardToBenchCommand("Lightning", 1)
                .execute(partida, jugador, bot);

        assertEquals(1, bancado.getEnergiasUnidas().size());
        assertTrue(jugador.getPilaDescarte().isEmpty());
    }

    @Test
    void attachEnergyFromDiscard_bancaVacia_noFalla() {
        jugador.getPilaDescarte().add(energyCard("Fire Energy", "Fire"));

        assertDoesNotThrow(() ->
                new AttachEnergyFromDiscardToBenchCommand("Fire", 1)
                        .execute(partida, jugador, bot));
    }

    @Test
    void attachEnergyFromDiscard_tipoNull_adjuntaTodo() {
        CartaEnJuego bancado = activePokemon("b1", "Bulbasaur", "60");
        jugador.getBanca().add(bancado);

        Card energy = energyCard("Grass Energy", "Grass");
        jugador.getPilaDescarte().add(energy);

        new AttachEnergyFromDiscardToBenchCommand(null, 1)
                .execute(partida, jugador, bot);

        assertEquals(1, bancado.getEnergiasUnidas().size());
    }

    // =================== SwitchOpponentActiveCommand ===================

    @Test
    void switchOpponentActive_botAtaca_switchInmediato() {
        CartaEnJuego activoRival = activePokemon("p1", "Bulbasaur", "60");
        activoRival.agregarCondicion("Poisoned");
        jugador.setActivo(activoRival);

        CartaEnJuego bancado = activePokemon("b1", "Charmander", "50");
        jugador.getBanca().add(bancado);

        new SwitchOpponentActiveCommand().execute(partida, bot, jugador);

        assertEquals("Charmander", jugador.getActivo().getCard().getNombre());
        assertEquals(1, jugador.getBanca().size());
        assertEquals("Bulbasaur", jugador.getBanca().get(0).getCard().getNombre());
        // Cleared conditions when benched
        assertFalse(jugador.getBanca().get(0).getCondicionesEspeciales().contains("Poisoned"));
    }

    @Test
    void switchOpponentActive_bancaVacia_noHaceNada() {
        jugador.setActivo(activePokemon("p1", "Bulbasaur", "60"));

        assertDoesNotThrow(() ->
                new SwitchOpponentActiveCommand().execute(partida, bot, jugador));
        assertEquals("Bulbasaur", jugador.getActivo().getCard().getNombre());
    }

    @Test
    void switchOpponentActive_jugadorAtaca_setPendingAction() {
        bot.setActivo(activePokemon("p1", "Squirtle", "50"));
        bot.getBanca().add(activePokemon("b1", "Pikachu", "60"));

        new SwitchOpponentActiveCommand().execute(partida, jugador, bot);

        assertNotNull(partida.getPendingAction());
        assertEquals("CHOOSE_OPPONENT_BENCH_TO_ACTIVE", partida.getPendingAction().getType());
    }

    // =================== MoveEnergyCommand ===================

    @Test
    void moveEnergy_deActivoABanca_funcionaCorrectamente() {
        CartaEnJuego activo = activePokemon("p1", "Pikachu", "60");
        Card lightning = energyCard("Lightning Energy", "Lightning");
        activo.getEnergiasUnidas().add(lightning);
        jugador.setActivo(activo);

        CartaEnJuego bancado = activePokemon("b1", "Raichu", "90");
        jugador.getBanca().add(bancado);

        new MoveEnergyCommand("Lightning", 1).execute(partida, jugador, bot);

        assertTrue(jugador.getActivo().getEnergiasUnidas().isEmpty());
        assertEquals(1, bancado.getEnergiasUnidas().size());
    }

    @Test
    void moveEnergy_tipoNoCoincide_noMueve() {
        CartaEnJuego activo = activePokemon("p1", "Pikachu", "60");
        activo.getEnergiasUnidas().add(energyCard("Lightning Energy", "Lightning"));
        jugador.setActivo(activo);
        jugador.getBanca().add(activePokemon("b1", "Raichu", "90"));

        new MoveEnergyCommand("Fire", 1).execute(partida, jugador, bot);

        assertEquals(1, jugador.getActivo().getEnergiasUnidas().size());
    }

    // =================== ShuffleRandomHandToDeckCommand ===================

    @Test
    void shuffleRandomHandToDeck_manoConCartas_moveUnaAlMazo() {
        bot.getMano().add(card("xy1-1", "Bulbasaur", "60"));
        bot.getMano().add(card("xy1-2", "Charmander", "50"));

        new ShuffleRandomHandToDeckCommand().execute(partida, jugador, bot);

        assertEquals(1, bot.getMano().size());
        assertEquals(1, bot.getMazo().size());
    }

    @Test
    void shuffleRandomHandToDeck_manoVacia_noFalla() {
        assertDoesNotThrow(() ->
                new ShuffleRandomHandToDeckCommand().execute(partida, jugador, bot));
    }

    // =================== DiscardRandomHandCardsByCoinTailsCommand ===================

    @Test
    void discardRandomByCoinTails_manoVacia_noFalla() {
        assertDoesNotThrow(() ->
                new DiscardRandomHandCardsByCoinTailsCommand(3, Target.OPPONENT)
                        .execute(partida, jugador, bot));
    }

    @Test
    void discardRandomByCoinTails_registraMonedas() {
        jugador.getMano().add(card("xy1-1", "Bulbasaur", "60"));

        new DiscardRandomHandCardsByCoinTailsCommand(2, Target.SELF)
                .execute(partida, jugador, bot);

        assertEquals(2, partida.getUltimasMonedasLanzadas().size());
    }
}
