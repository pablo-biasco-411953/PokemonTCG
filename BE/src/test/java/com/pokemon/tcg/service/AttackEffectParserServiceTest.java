package com.pokemon.tcg.service;

import com.pokemon.tcg.model.battle.command.CoinFlipCommand;
import com.pokemon.tcg.model.battle.command.DiscardEnergyCommand;
import com.pokemon.tcg.model.battle.command.MultiCoinDamageCommand;
import com.pokemon.tcg.model.battle.command.SetInvulnerableCommand;
import com.pokemon.tcg.service.battle.command.AttackEffectParserService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AttackEffectParserServiceTest {
    private final AttackEffectParserService parser = new AttackEffectParserService();

    @Test
    void digInterpretaCaraComoProteccion() {
        var commands = parser.parseEffects(
                "Flip a coin. If heads, prevent all effects of attacks, including damage, "
                        + "done to this Pokemon during your opponent's next turn.",
                null
        );

        assertTrue(commands.stream().anyMatch(command ->
                command instanceof CoinFlipCommand coin
                        && coin.getOnHeads() instanceof SetInvulnerableCommand
        ));
    }

    @Test
    void pinMissileInterpretaMultiplesMonedasComoDanioPorCara() {
        var commands = parser.parseEffects(
                "Flip 4 coins. This attack does 10 damage times the number of heads.",
                null
        );

        assertTrue(commands.stream().anyMatch(command -> command instanceof MultiCoinDamageCommand));
    }

    @Test
    void splashBombInterpretaRetrocesoSoloEnCruz() {
        var commands = parser.parseEffects(
                "Flip a coin. If tails, this Pokemon does 30 damage to itself.",
                null
        );

        CoinFlipCommand coin = commands.stream()
                .filter(CoinFlipCommand.class::isInstance)
                .map(CoinFlipCommand.class::cast)
                .findFirst()
                .orElseThrow();
        assertInstanceOf(com.pokemon.tcg.model.battle.command.SelfDamageCommand.class, coin.getOnTails());
    }

    @Test
    void cutDownDescartaEnergiaRivalSoloEnCara() {
        var commands = parser.parseEffects(
                "Flip a coin. If heads, discard an Energy attached to your opponent's Active Pokemon.",
                null
        );

        CoinFlipCommand coin = commands.stream()
                .filter(CoinFlipCommand.class::isInstance)
                .map(CoinFlipCommand.class::cast)
                .findFirst()
                .orElseThrow();
        assertInstanceOf(DiscardEnergyCommand.class, coin.getOnHeads());
    }

    @Test
    void dynamicPunchUsaUnaSolaMonedaParaDanioYConfusion() {
        var commands = parser.parseEffects(
                "Flip a coin. If heads, this attack does 40 more damage and "
                        + "your opponent's Active Pokemon is now Confused.",
                null
        );

        assertEquals(1, commands.stream().filter(CoinFlipCommand.class::isInstance).count());
        CoinFlipCommand coin = (CoinFlipCommand) commands.getFirst();
        assertInstanceOf(com.pokemon.tcg.model.battle.command.SequenceCommand.class, coin.getOnHeads());
    }

    @Test
    void parseaRockWreckerComoSetNoPuedeAtacarSiguienteTurno() {
        var commands = parser.parseEffects("This Pokémon can't attack during your next turn.", null);
        assertTrue(commands.stream().anyMatch(command -> command instanceof com.pokemon.tcg.model.battle.command.SetNoPuedeAtacarSiguienteTurnoCommand));
    }

    @Test
    void parseaHocusPinkusComoSetCannotAttackDefending() {
        var commands = parser.parseEffects("The Defending Pokémon can't attack during your opponent's next turn.", null);
        assertTrue(commands.stream().anyMatch(command -> command instanceof com.pokemon.tcg.model.battle.command.SetCannotAttackDefendingCommand));
    }

    @Test
    void parseaKingsShieldComoInvulnerableYBlockAttackNextTurn() {
        var commands = parser.parseEffects("Prevent all damage done to this Pokémon by attacks during your opponent's next turn. This Pokémon can't use King's Shield during your next turn.", null);
        assertTrue(commands.stream().anyMatch(command -> command instanceof com.pokemon.tcg.model.battle.command.SetInvulnerableCommand));
        assertTrue(commands.stream().anyMatch(command -> command instanceof com.pokemon.tcg.model.battle.command.BlockAttackNextTurnCommand));
    }

    @Test
    void parseaDarknessBladeComoCoinFlipConSetNoPuedeAtacarSiguienteTurnoEnCruz() {
        var commands = parser.parseEffects("Flip a coin. If tails, this Pokémon can't attack during your next turn.", null);
        CoinFlipCommand coin = commands.stream()
                .filter(CoinFlipCommand.class::isInstance)
                .map(CoinFlipCommand.class::cast)
                .findFirst()
                .orElseThrow();
        assertInstanceOf(com.pokemon.tcg.model.battle.command.SetNoPuedeAtacarSiguienteTurnoCommand.class, coin.getOnTails());
    }

    @Test
    void parseaCircleCircuitComoBenchedPokemonMultiplier() {
        var commands = parser.parseEffects("This attack does 20 damage times the number of your Benched Pokémon.", null);
        assertTrue(commands.stream().anyMatch(command ->
                command instanceof com.pokemon.tcg.model.battle.command.ConditionalDamageMultiplierCommand
        ));
    }

    @Test
    void parseaEarthquakeComoSelfBenchDamage() {
        var commands = parser.parseEffects("This attack does 10 damage to each of your Benched Pokémon.", null);
        assertTrue(commands.stream().anyMatch(command ->
                command instanceof com.pokemon.tcg.model.battle.command.SelfBenchDamageCommand cmd && cmd.getAmount() == 10
        ));
    }

    @Test
    void parseaHydroBombardComoDamageOpponentBenched() {
        // El efecto debe producir un solo comando, aunque coincida con patrones generales.
        var commands = parser.parseEffects("This attack does 30 damage to 2 of your opponent's Benched Pokémon.", null);
        assertEquals(1, commands.stream().filter(
                command -> command instanceof com.pokemon.tcg.model.battle.command.DamageOpponentBenchedCommand
        ).count());
        assertTrue(commands.stream().anyMatch(command ->
                command instanceof com.pokemon.tcg.model.battle.command.DamageOpponentBenchedCommand cmd && cmd.getAmount() == 30 && cmd.getCount() == 2
        ));
    }

    @Test
    void parseaBreakThroughComoDamageOpponentBenchedConUno() {
        var commands = parser.parseEffects("This attack does 30 damage to 1 of your opponent's Benched Pokémon.", null);
        assertEquals(1, commands.stream().filter(
                command -> command instanceof com.pokemon.tcg.model.battle.command.DamageOpponentBenchedCommand
        ).count());
        assertTrue(commands.stream().anyMatch(command ->
                command instanceof com.pokemon.tcg.model.battle.command.DamageOpponentBenchedCommand cmd && cmd.getAmount() == 30 && cmd.getCount() == 1
        ));
    }

    @Test
    void parseaAstonishComoShuffleRandomHandToDeck() {
        var commands = parser.parseEffects("Choose a random card from your opponent's hand. Your opponent reveals that card and shuffles it into his or her deck.", null);
        assertTrue(commands.stream().anyMatch(command ->
                command instanceof com.pokemon.tcg.model.battle.command.ShuffleRandomHandToDeckCommand
        ));
    }

    @Test
    void parseaDigOutComoDiscardTopDeckAttachEnergyCommand() {
        var commands = parser.parseEffects("Discard the top card of your deck. If that card is a Fighting Energy, attach it to this Pokémon.", null);
        assertTrue(commands.stream().anyMatch(command ->
                command instanceof com.pokemon.tcg.model.battle.command.DiscardTopDeckAttachEnergyCommand
        ));
    }

    @Test
    void parseaHammerArmComoDiscardTopDeckCommand() {
        var commands = parser.parseEffects("Discard the top card of your opponent's deck.", null);
        assertTrue(commands.stream().anyMatch(command ->
                command instanceof com.pokemon.tcg.model.battle.command.DiscardTopDeckCommand
        ));
    }

    @Test
    void parseaMadMountainComoRhydonMadMountainCommand() {
        var commands = parser.parseEffects("Flip 2 coins. If both of them are heads, discard the top card of your opponent's deck for each damage counter on this Pokémon.", null);
        assertTrue(commands.stream().anyMatch(command ->
                command instanceof com.pokemon.tcg.model.battle.command.RhydonMadMountainCommand
        ));
    }

    @Test
    void parseaMagmaMantleComoMagcargoMagmaMantleCommand() {
        var commands = parser.parseEffects("You may discard the top card of your deck. If that card is a Fire Energy card, this attack does 50 more damage.", null);
        assertTrue(commands.stream().anyMatch(command ->
                command instanceof com.pokemon.tcg.model.battle.command.MagcargoMagmaMantleCommand
        ));
    }

    @Test
    void parseaLeafMunchComoOpponentTypeGrass() {
        var commands = parser.parseEffects("If your opponent's Active Pokémon is a Grass Pokémon, this attack does 20 more damage.", null);
        assertTrue(commands.stream().anyMatch(command ->
                command instanceof com.pokemon.tcg.model.battle.command.ConditionalDamageMultiplierCommand cmd
                && "OPPONENT_TYPE".equals(cmd.getConditionType())
                && "Grass".equals(cmd.getConditionValue())
                && cmd.getMultiplier() == 20
        ));
    }

    @Test
    void parseaTormentComoTormentBlockAttackCommand() {
        var commands = parser.parseEffects("Choose 1 of your opponent's Active Pokémon's attacks. That Pokémon can't use that attack during your opponent's next turn.", "Scratch");
        assertTrue(commands.stream().anyMatch(command ->
                command instanceof com.pokemon.tcg.model.battle.command.TormentBlockAttackCommand
        ));
    }

    @Test
    void parseaTormentConComillasCurvasCorrectamente() {
        var commands = parser.parseEffects("Choose 1 of your opponent’s Active Pokémon’s attacks. That Pokémon can’t use that attack during your opponent’s next turn.", "Scratch");
        assertTrue(commands.stream().anyMatch(command ->
                command instanceof com.pokemon.tcg.model.battle.command.TormentBlockAttackCommand
        ));
    }

    @Test
    void parseaChargeDashComoGogoatChargeDashCommand() {
        var commands = parser.parseEffects("You may do 20 more damage. If you do, this Pokemon does 20 damage to itself.", "yes");
        assertTrue(commands.stream().anyMatch(command ->
                command instanceof com.pokemon.tcg.model.battle.command.GogoatChargeDashCommand
        ));
    }

    @Test
    void parseaFlamethrowerComoDiscardAttachedEnergyOfTypeCommand() {
        var commands = parser.parseEffects("Discard a Fire Energy attached to this Pokémon.", null);
        assertTrue(commands.stream().anyMatch(command ->
                command instanceof com.pokemon.tcg.model.battle.command.DiscardAttachedEnergyOfTypeCommand
        ));
    }

    @Test
    void parseaHardenComoSetPreventDamageThresholdCommand() {
        var commands = parser.parseEffects("During your opponent's next turn, if this Pokemon would be damaged by an attack, prevent that attack's damage done to this Pokemon if that damage is 60 or less.", null);
        assertTrue(commands.stream().anyMatch(command ->
                command instanceof com.pokemon.tcg.model.battle.command.SetPreventDamageThresholdCommand cmd
                && cmd.getThreshold() == 60
        ));
    }

    @Test
    void parseaDevastatingWindComoOpponentShuffleHandDrawCommand() {
        var commands = parser.parseEffects("Your opponent shuffles his or her hand into his or her deck and draws 4 cards.", null);
        assertTrue(commands.stream().anyMatch(command ->
                command instanceof com.pokemon.tcg.model.battle.command.OpponentShuffleHandDrawCommand cmd
                && cmd.getDrawCount() == 4
        ));
    }

    @Test
    void parseaFlareBlitzComoDiscardAttachedEnergyOfTypeCommandAll() {
        var commands = parser.parseEffects("Discard all Fire Energy attached to this Pokemon.", null);
        assertTrue(commands.stream().anyMatch(command ->
                command instanceof com.pokemon.tcg.model.battle.command.DiscardAttachedEnergyOfTypeCommand
        ));
    }

    @Test
    void parseaRecoverComoDiscardAnyYHealAll() {
        var commands = parser.parseEffects("Discard an Energy attached to this Pokemon and heal all damage from it.", null);
        assertTrue(commands.stream().anyMatch(command -> command instanceof com.pokemon.tcg.model.battle.command.DiscardAttachedEnergyOfTypeCommand));
        assertTrue(commands.stream().anyMatch(command -> command instanceof com.pokemon.tcg.model.battle.command.HealCommand cmd && cmd.getAmount() == -1));
    }

    @Test
    void parseaCoreSplashComoConditionalDamageHasEnergyType() {
        var commands = parser.parseEffects("If this Pokemon has any Psychic Energy attached to it, this attack does 30 more damage.", null);
        assertTrue(commands.stream().anyMatch(command ->
                command instanceof com.pokemon.tcg.model.battle.command.ConditionalDamageMultiplierCommand cmd
                && "HAS_ENERGY_TYPE".equals(cmd.getConditionType())
                && "Psychic".equals(cmd.getConditionValue())
                && cmd.getMultiplier() == 30
        ));
    }

    @Test
    void parseaSeafaringComoLaprasSeafaringCommand() {
        var commands = parser.parseEffects("Flip 3 coins. For each heads, attach a Water Energy card from your discard pile to your Benched Pokemon in any way you like.", null);
        assertTrue(commands.stream().anyMatch(command -> command instanceof com.pokemon.tcg.model.battle.command.LaprasSeafaringCommand));
    }

    @Test
    void parseaHydroPumpComoAddDamageByAttachedEnergyCommand() {
        var commands = parser.parseEffects("This attack does 20 more damage for each Water Energy attached to this Pokemon.", null);
        assertTrue(commands.stream().anyMatch(command ->
                command instanceof com.pokemon.tcg.model.battle.command.AddDamageByAttachedEnergyCommand
        ));
    }

    @Test
    void parseaRefreshComoCorsolaRefreshCommand() {
        var commands = parser.parseEffects("Heal 30 damage and remove all Special Conditions from this Pokemon.", null);
        assertTrue(commands.stream().anyMatch(command -> command instanceof com.pokemon.tcg.model.battle.command.CorsolaRefreshCommand));
    }

    @Test
    void parseaTailspinPiledriverComoOpponentHasDamageCounters() {
        var commands = parser.parseEffects("If your opponent's Active Pokémon already has any damage counters on it, this attack does 40 more damage.", null);
        assertTrue(commands.stream().anyMatch(command ->
                command instanceof com.pokemon.tcg.model.battle.command.ConditionalDamageMultiplierCommand cmd
                && "OPPONENT_HAS_DAMAGE_COUNTERS".equals(cmd.getConditionType())
                && cmd.getMultiplier() == 40
        ));
    }

    @Test
    void parseaMentalTrashComoDiscardRandomHandCardsByCoinTailsCommand() {
        var commands = parser.parseEffects("Your opponent flips 4 coins. For each tails, he or she discards a card from his or her hand.", null);
        assertTrue(commands.stream().anyMatch(command ->
                command instanceof com.pokemon.tcg.model.battle.command.DiscardRandomHandCardsByCoinTailsCommand
        ));
    }
}
