package com.pokemon.tcg.model.battle.command;

import com.pokemon.tcg.model.Card;
import com.pokemon.tcg.model.battle.CartaEnJuego;
import com.pokemon.tcg.model.battle.Partida;
import com.pokemon.tcg.model.battle.TableroJugador;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BattleCommandsTest {

    private Partida partida;
    private TableroJugador atacante;
    private TableroJugador defensor;
    private CartaEnJuego activoAtacante;
    private CartaEnJuego activoDefensor;

    @BeforeEach
    void setUp() {
        activoAtacante = cartaEnJuego("Bulbasaur", "60");
        activoDefensor = cartaEnJuego("Charmander", "50");

        atacante = tablero(activoAtacante);
        defensor = tablero(activoDefensor);

        partida = new Partida(atacante, defensor);
        partida.setJugadorUsername("ash");
        partida.setBotUsername("BOT");
    }

    private Card card(String id, String nombre, String hp) {
        Card c = new Card();
        c.setId(id);
        c.setNombre(nombre);
        c.setHp(hp);
        return c;
    }

    private CartaEnJuego cartaEnJuego(String nombre, String hp) {
        Card c = new Card();
        c.setId("id-" + nombre);
        c.setNombre(nombre);
        c.setHp(hp);
        CartaEnJuego cej = new CartaEnJuego(c);
        cej.setHpActual(Integer.parseInt(hp));
        return cej;
    }

    private TableroJugador tablero(CartaEnJuego activo) {
        TableroJugador t = new TableroJugador();
        t.setActivo(activo);
        t.setBanca(new ArrayList<>());
        t.setMano(new ArrayList<>());
        t.setMazo(new ArrayList<>());
        t.setPilaDescarte(new ArrayList<>());
        t.setPremios(new ArrayList<>());
        return t;
    }

    // =================== DamageCommand ===================

    @Test
    void damage_reducirHpDefensor() {
        new DamageCommand(20).execute(partida, atacante, defensor);
        assertEquals(30, activoDefensor.getHpActual());
    }

    @Test
    void damage_noPuedeNegativo() {
        new DamageCommand(999).execute(partida, atacante, defensor);
        assertEquals(0, activoDefensor.getHpActual());
    }

    @Test
    void damage_defensorSinActivo_noFalla() {
        defensor.setActivo(null);
        assertDoesNotThrow(() -> new DamageCommand(20).execute(partida, atacante, defensor));
    }

    @Test
    void damage_getAmount_correcto() {
        assertEquals(30, new DamageCommand(30).getAmount());
    }

    // =================== SelfDamageCommand ===================

    @Test
    void selfDamage_reducirHpAtacante() {
        new SelfDamageCommand(10).execute(partida, atacante, defensor);
        assertEquals(50, activoAtacante.getHpActual());
    }

    @Test
    void selfDamage_noPuedeNegativo() {
        new SelfDamageCommand(999).execute(partida, atacante, defensor);
        assertEquals(0, activoAtacante.getHpActual());
    }

    @Test
    void selfDamage_atacanteSinActivo_noFalla() {
        atacante.setActivo(null);
        assertDoesNotThrow(() -> new SelfDamageCommand(10).execute(partida, atacante, defensor));
    }

    @Test
    void selfDamage_getAmount_correcto() {
        assertEquals(15, new SelfDamageCommand(15).getAmount());
    }

    // =================== HealCommand ===================

    @Test
    void heal_self_aumentaHpAtacante() {
        activoAtacante.setHpActual(30);
        new HealCommand(20, Target.SELF).execute(partida, atacante, defensor);
        assertEquals(50, activoAtacante.getHpActual());
    }

    @Test
    void heal_self_noPasaMaxHp() {
        activoAtacante.setHpActual(55);
        new HealCommand(20, Target.SELF).execute(partida, atacante, defensor);
        assertEquals(60, activoAtacante.getHpActual());
    }

    @Test
    void heal_opponent_aumentaHpDefensor() {
        activoDefensor.setHpActual(20);
        new HealCommand(10, Target.OPPONENT).execute(partida, atacante, defensor);
        assertEquals(30, activoDefensor.getHpActual());
    }

    @Test
    void heal_full_restauraTodoHp() {
        activoAtacante.setHpActual(10);
        new HealCommand(-1, Target.SELF).execute(partida, atacante, defensor);
        assertEquals(60, activoAtacante.getHpActual());
    }

    @Test
    void heal_sinActivo_noFalla() {
        atacante.setActivo(null);
        assertDoesNotThrow(() -> new HealCommand(20, Target.SELF).execute(partida, atacante, defensor));
    }

    // =================== DrawCardCommand ===================

    @Test
    void drawCard_self_agregaAMano() {
        Card c = card("xy1-1", "Squirtle", "40");
        atacante.getMazo().add(c);

        new DrawCardCommand(1, Target.SELF).execute(partida, atacante, defensor);

        assertEquals(1, atacante.getMano().size());
        assertTrue(atacante.getMazo().isEmpty());
    }

    @Test
    void drawCard_opponent_agregaAManoRival() {
        Card c = card("xy1-2", "Psyduck", "40");
        defensor.getMazo().add(c);

        new DrawCardCommand(1, Target.OPPONENT).execute(partida, atacante, defensor);

        assertEquals(1, defensor.getMano().size());
    }

    @Test
    void drawCard_mazoVacio_noFalla() {
        assertDoesNotThrow(() -> new DrawCardCommand(2, Target.SELF).execute(partida, atacante, defensor));
    }

    @Test
    void drawCard_logueaCuandoRoba() {
        Card c = card("xy1-3", "Magikarp", "30");
        atacante.getMazo().add(c);

        new DrawCardCommand(1, Target.SELF).execute(partida, atacante, defensor);

        assertTrue(partida.getTurnLogs().stream().anyMatch(l -> l.startsWith("CARDS_DRAWN:")));
    }

    // =================== ApplyStatusConditionCommand ===================

    @Test
    void applyStatus_self_aplicaCondicion() {
        new ApplyStatusConditionCommand("Poisoned", Target.SELF).execute(partida, atacante, defensor);
        assertTrue(activoAtacante.getCondicionesEspeciales().contains("Poisoned"));
    }

    @Test
    void applyStatus_opponent_aplicaCondicionRival() {
        new ApplyStatusConditionCommand("Burned", Target.OPPONENT).execute(partida, atacante, defensor);
        assertTrue(activoDefensor.getCondicionesEspeciales().contains("Burned"));
    }

    @Test
    void applyStatus_sinActivo_noFalla() {
        defensor.setActivo(null);
        assertDoesNotThrow(() -> new ApplyStatusConditionCommand("Poisoned", Target.OPPONENT).execute(partida, atacante, defensor));
    }

    @Test
    void applyStatus_getters() {
        ApplyStatusConditionCommand cmd = new ApplyStatusConditionCommand("Asleep", Target.SELF);
        assertEquals("Asleep", cmd.getCondition());
        assertEquals(Target.SELF, cmd.getTarget());
    }

    // =================== SetInvulnerableCommand ===================

    @Test
    void setInvulnerable_activaInvulnerabilidad() {
        new SetInvulnerableCommand().execute(partida, atacante, defensor);
        assertTrue(activoAtacante.isInvulnerable());
    }

    @Test
    void setInvulnerable_sinActivo_noFalla() {
        atacante.setActivo(null);
        assertDoesNotThrow(() -> new SetInvulnerableCommand().execute(partida, atacante, defensor));
    }

    // =================== DiscardEnergyCommand ===================

    @Test
    void discardEnergy_self_descartaEnergia() {
        Card energia = card("energy-1", "Fire Energy", "0");
        activoAtacante.getEnergiasUnidas().add(energia);

        new DiscardEnergyCommand(1, Target.SELF).execute(partida, atacante, defensor);

        assertTrue(activoAtacante.getEnergiasUnidas().isEmpty());
        assertFalse(atacante.getPilaDescarte().isEmpty());
    }

    @Test
    void discardEnergy_opponent_descartaEnergiaRival() {
        Card energia = card("energy-2", "Water Energy", "0");
        activoDefensor.getEnergiasUnidas().add(energia);

        new DiscardEnergyCommand(1, Target.OPPONENT).execute(partida, atacante, defensor);

        assertTrue(activoDefensor.getEnergiasUnidas().isEmpty());
    }

    @Test
    void discardEnergy_sinEnergia_noFalla() {
        assertDoesNotThrow(() -> new DiscardEnergyCommand(1, Target.SELF).execute(partida, atacante, defensor));
    }

    // =================== SequenceCommand ===================

    @Test
    void sequence_encola_comandos() {
        BattleCommand cmd1 = new DamageCommand(10);
        BattleCommand cmd2 = new HealCommand(5, Target.SELF);

        new SequenceCommand(cmd1, cmd2).execute(partida, atacante, defensor);

        assertFalse(partida.getExecutionQueue().isEmpty());
    }

    @Test
    void sequence_orden_correcto_primer_en_queue() {
        BattleCommand cmd1 = new DamageCommand(10);
        BattleCommand cmd2 = new DrawCardCommand(1, Target.SELF);

        new SequenceCommand(cmd1, cmd2).execute(partida, atacante, defensor);

        assertEquals(cmd1, partida.getExecutionQueue().peekFirst());
    }

    // =================== CoinFlipCommand ===================

    @Test
    void coinFlip_registra_enUltimasMonedas() {
        new CoinFlipCommand(new DamageCommand(10)).execute(partida, atacante, defensor);
        assertFalse(partida.getUltimasMonedasLanzadas().isEmpty());
    }

    @Test
    void coinFlip_getOnHeads_correcto() {
        BattleCommand cmd = new DamageCommand(20);
        CoinFlipCommand flip = new CoinFlipCommand(cmd);
        assertEquals(cmd, flip.getOnHeads());
    }

    @Test
    void coinFlip_getOnTails_correcto() {
        BattleCommand heads = new DamageCommand(20);
        BattleCommand tails = new HealCommand(10, Target.SELF);
        CoinFlipCommand flip = new CoinFlipCommand(heads, tails);
        assertEquals(tails, flip.getOnTails());
    }

    @Test
    void coinFlip_sinTails_noFalla() {
        assertDoesNotThrow(() -> new CoinFlipCommand(new DamageCommand(10)).execute(partida, atacante, defensor));
    }

    // =================== BlockAttackNextTurnCommand ===================

    @Test
    void blockAttack_self_poneBloqueadoSiguienteTurno() {
        new BlockAttackNextTurnCommand("Thunderbolt", Target.SELF).execute(partida, atacante, defensor);
        assertEquals("Thunderbolt", activoAtacante.getAtaqueBloqueadoSiguienteTurno());
    }

    @Test
    void blockAttack_opponent_poneBloqueadoSiguienteTurnoConsumed() {
        new BlockAttackNextTurnCommand("Tackle", Target.OPPONENT).execute(partida, atacante, defensor);
        assertEquals("Tackle", activoDefensor.getAtaqueBloqueadoSiguienteTurno());
        assertTrue(activoDefensor.isAtaqueBloqueadoYaConsumido());
    }

    @Test
    void blockAttack_sinActivo_noFalla() {
        atacante.setActivo(null);
        assertDoesNotThrow(() -> new BlockAttackNextTurnCommand("Test", Target.SELF).execute(partida, atacante, defensor));
    }

    // =================== SetNoPuedeAtacarSiguienteTurnoCommand ===================

    @Test
    void setNoPuedeAtacar_self_marcaAtacante() {
        new SetNoPuedeAtacarSiguienteTurnoCommand(Target.SELF).execute(partida, atacante, defensor);
        assertTrue(activoAtacante.isNoPuedeAtacarSiguienteTurno());
        assertFalse(activoAtacante.isNoPuedeAtacarYaConsumido());
    }

    @Test
    void setNoPuedeAtacar_opponent_marcaDefensor() {
        new SetNoPuedeAtacarSiguienteTurnoCommand(Target.OPPONENT).execute(partida, atacante, defensor);
        assertTrue(activoDefensor.isNoPuedeAtacarSiguienteTurno());
    }

    @Test
    void setNoPuedeAtacar_sinActivo_noFalla() {
        defensor.setActivo(null);
        assertDoesNotThrow(() -> new SetNoPuedeAtacarSiguienteTurnoCommand(Target.OPPONENT).execute(partida, atacante, defensor));
    }

    // =================== DamageOpponentBenchedCommand ===================

    @Test
    void damageOpponentBenched_bancaVacia_noFalla() {
        assertDoesNotThrow(() -> new DamageOpponentBenchedCommand(10, 1).execute(partida, atacante, defensor));
    }

    @Test
    void damageOpponentBenched_getters() {
        DamageOpponentBenchedCommand cmd = new DamageOpponentBenchedCommand(20, 2);
        assertEquals(20, cmd.getAmount());
        assertEquals(2, cmd.getCount());
    }

    @Test
    void damageOpponentBenched_bot_atacaBanca_jugador() {
        CartaEnJuego bancaPoke = cartaEnJuego("Squirtle", "50");
        atacante.getBanca().add(bancaPoke); // atacante is jugador, defensor is bot
        // simulate bot attacking: swap roles
        Partida p2 = new Partida(atacante, defensor);
        p2.setJugadorUsername("ash");
        p2.setBotUsername("BOT");

        new DamageOpponentBenchedCommand(10, 1).execute(p2, defensor, atacante);

        assertTrue(bancaPoke.getHpActual() < 50);
    }

    // =================== AddDamageByAttachedEnergyCommand ===================

    @Test
    void addDamageByEnergy_conEnergias_encolaExtraDanio() {
        Card energia = card("e1", "Fire Energy", "0");
        activoAtacante.getEnergiasUnidas().add(energia);

        new AddDamageByAttachedEnergyCommand(null, 10, false).execute(partida, atacante, defensor);

        assertFalse(partida.getExecutionQueue().isEmpty());
    }

    @Test
    void addDamageByEnergy_sinEnergias_noEncola() {
        new AddDamageByAttachedEnergyCommand("Fire", 10, false).execute(partida, atacante, defensor);
        assertTrue(partida.getExecutionQueue().isEmpty());
    }

    @Test
    void addDamageByEnergy_includeOpponentActive_cuentaAmbos() {
        Card e1 = card("e1", "Fire Energy", "0");
        Card e2 = card("e2", "Fire Energy", "0");
        activoAtacante.getEnergiasUnidas().add(e1);
        activoDefensor.getEnergiasUnidas().add(e2);

        new AddDamageByAttachedEnergyCommand(null, 10, true).execute(partida, atacante, defensor);

        DamageCommand dmg = (DamageCommand) partida.getExecutionQueue().peekFirst();
        assertEquals(20, dmg.getAmount()); // 2 energías * 10
    }

    // =================== MultiCoinDamageCommand ===================

    @Test
    void multiCoinDamage_fixed_registraMonedas() {
        new MultiCoinDamageCommand(MultiCoinDamageCommand.CountMode.FIXED, 2, 20, null, false)
                .execute(partida, atacante, defensor);
        assertEquals(2, partida.getUltimasMonedasLanzadas().size());
    }

    @Test
    void multiCoinDamage_untilTails_registraAlMenos1() {
        new MultiCoinDamageCommand(MultiCoinDamageCommand.CountMode.UNTIL_TAILS, 0, 20, null, false)
                .execute(partida, atacante, defensor);
        assertFalse(partida.getUltimasMonedasLanzadas().isEmpty());
    }

    @Test
    void multiCoinDamage_attachedEnergy_cuentaEnergias() {
        Card e = card("e1", "Water Energy", "0");
        activoAtacante.getEnergiasUnidas().add(e);
        activoAtacante.getEnergiasUnidas().add(e);

        new MultiCoinDamageCommand(MultiCoinDamageCommand.CountMode.ATTACHED_ENERGY, 0, 20, null, false)
                .execute(partida, atacante, defensor);
        assertEquals(2, partida.getUltimasMonedasLanzadas().size());
    }

    @Test
    void multiCoinDamage_sinActivo_noFalla() {
        atacante.setActivo(null);
        assertDoesNotThrow(() -> new MultiCoinDamageCommand(MultiCoinDamageCommand.CountMode.FIXED, 2, 10, null, false)
                .execute(partida, atacante, defensor));
    }
    // =================== Nuevos Comandos (11 Cartas) ===================

    @Test
    void putDamageCountersOnAllOpponent() {
        CartaEnJuego benched = cartaEnJuego("Benched", "50");
        defensor.getBanca().add(benched);
        new PutDamageCountersOnAllOpponentCommand(2).execute(partida, atacante, defensor);
        assertEquals(30, activoDefensor.getHpActual()); // 50 - 20
        assertEquals(30, benched.getHpActual()); // 50 - 20
    }

    @Test
    void setRemainingHpBothActive() {
        new SetRemainingHpBothActiveCommand(10).execute(partida, atacante, defensor);
        assertEquals(10, activoAtacante.getHpActual());
        assertEquals(10, activoDefensor.getHpActual());
    }

    @Test
    void automatedLookAtTopCardAndShuffle() {
        Card topCard = card("e1", "Fire Energy", "0");
        topCard.setSupertype("Energy");
        defensor.getMazo().add(topCard);
        new AutomatedLookAtTopCardAndShuffleCommand().execute(partida, atacante, defensor);
        // It should shuffle, but checking the log output is tricky. At least it shouldn't crash.
        assertFalse(defensor.getMazo().isEmpty());
    }

    @Test
    void damageOwnBenched() {
        CartaEnJuego benched = cartaEnJuego("Benched", "50");
        atacante.getBanca().add(benched);
        new DamageOwnBenchedCommand(10).execute(partida, atacante, defensor);
        assertEquals(40, benched.getHpActual());
    }

    @Test
    void discardOpponentDeckPerDamageCounter() {
        CartaEnJuego atacante100 = cartaEnJuego("Bulbasaur100", "100");
        atacante100.setHpActual(80); // 100 max, 20 missing = 2 counters
        atacante.setActivo(atacante100);
        partida.getUltimasMonedasLanzadas().add(true);
        partida.getUltimasMonedasLanzadas().add(true);
        defensor.getMazo().add(card("1", "A", "0"));
        defensor.getMazo().add(card("2", "A", "0"));
        defensor.getMazo().add(card("3", "A", "0"));

        new DiscardOpponentDeckPerDamageCounterCommand(2).execute(partida, atacante, defensor);
        assertEquals(1, defensor.getMazo().size());
        assertEquals(2, defensor.getPilaDescarte().size());
    }

    @Test
    void reduceNextTurnDamageDealt() {
        new ReduceNextTurnDamageDealtCommand(20).execute(partida, atacante, defensor);
        assertEquals(20, activoDefensor.getReduccionDanioCausadoSiguienteTurno());
    }

    @Test
    void addDamageIfPokemonOnBench() {
        CartaEnJuego lunatone = cartaEnJuego("Lunatone", "70");
        atacante.getBanca().add(lunatone);
        new AddDamageIfPokemonOnBenchCommand("Lunatone", 30).execute(partida, atacante, defensor);
        assertFalse(partida.getExecutionQueue().isEmpty());
        DamageCommand dmg = (DamageCommand) partida.getExecutionQueue().poll();
        assertEquals(30, dmg.getAmount());
    }

    @Test
    void addDamageIfStatusConditionAndRemove() {
        activoDefensor.agregarCondicion("Poisoned");
        new AddDamageIfStatusConditionAndRemoveCommand(60).execute(partida, atacante, defensor);
        assertTrue(activoDefensor.getCondicionesEspeciales().isEmpty());
        assertFalse(partida.getExecutionQueue().isEmpty());
    }

    @Test
    void blockSupporterCardsNextTurn() {
        new BlockSupporterCardsNextTurnCommand().execute(partida, atacante, defensor);
        assertTrue(defensor.isSupporterBlockedNextTurn());
    }

    @Test
    void forceOpponentSwitch_bot() {
        CartaEnJuego benched = cartaEnJuego("Benched", "50");
        defensor.getBanca().add(benched);
        
        Partida p2 = new Partida(atacante, defensor);
        p2.setBotUsername("Jugador2");
        
        new ForceOpponentSwitchCommand().execute(p2, atacante, defensor);
        // Bot switches active automatically
        assertEquals("Benched", defensor.getActivo().getCard().getNombre());
    }

    @Test
    void forceOpponentSwitch_player() {
        CartaEnJuego benched = cartaEnJuego("Benched", "50");
        atacante.getBanca().add(benched);

        Partida p2 = new Partida(atacante, defensor);
        p2.setJugadorUsername("Jugador2"); // The opponent is the player

        new ForceOpponentSwitchCommand().execute(p2, defensor, atacante);
        // Should wait for interaction
        assertNotNull(p2.getPendingAction());
        assertEquals("SWITCH_ACTIVE", p2.getPendingAction().getType());
    }

    @Test
    void coinFlipConditionCommand() {
        new CoinFlipConditionCommand(2, 2, new DamageCommand(50)).execute(partida, atacante, defensor);
        assertEquals(2, partida.getUltimasMonedasLanzadas().size());
    }
}
