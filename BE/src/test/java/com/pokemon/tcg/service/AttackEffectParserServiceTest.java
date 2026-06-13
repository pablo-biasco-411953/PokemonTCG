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
        var commands = parser.parseEffects("This attack does 30 damage to 2 of your opponent's Benched Pokémon.", null);
        assertTrue(commands.stream().anyMatch(command ->
                command instanceof com.pokemon.tcg.model.battle.command.DamageOpponentBenchedCommand cmd && cmd.getAmount() == 30 && cmd.getCount() == 2
        ));
    }

    @Test
    void parseaBreakThroughComoDamageOpponentBenchedConUno() {
        var commands = parser.parseEffects("This attack does 30 damage to 1 of your opponent's Benched Pokémon.", null);
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
}
