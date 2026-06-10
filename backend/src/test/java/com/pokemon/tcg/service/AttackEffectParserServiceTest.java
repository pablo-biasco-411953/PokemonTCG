package com.pokemon.tcg.service;

import com.pokemon.tcg.model.battle.command.CoinFlipCommand;
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
                        + "done to this Pokemon during your opponent's next turn."
        );

        assertTrue(commands.stream().anyMatch(command ->
                command instanceof CoinFlipCommand coin
                        && coin.getOnHeads() instanceof SetInvulnerableCommand
        ));
    }

    @Test
    void pinMissileInterpretaMultiplesMonedasComoDanioPorCara() {
        var commands = parser.parseEffects(
                "Flip 4 coins. This attack does 10 damage times the number of heads."
        );

        assertTrue(commands.stream().anyMatch(command -> command instanceof MultiCoinDamageCommand));
    }

    @Test
    void splashBombInterpretaRetrocesoSoloEnCruz() {
        var commands = parser.parseEffects(
                "Flip a coin. If tails, this Pokemon does 30 damage to itself."
        );

        CoinFlipCommand coin = commands.stream()
                .filter(CoinFlipCommand.class::isInstance)
                .map(CoinFlipCommand.class::cast)
                .findFirst()
                .orElseThrow();
        assertInstanceOf(com.pokemon.tcg.model.battle.command.SelfDamageCommand.class, coin.getOnTails());
    }

    @Test
    void dynamicPunchUsaUnaSolaMonedaParaDanioYConfusion() {
        var commands = parser.parseEffects(
                "Flip a coin. If heads, this attack does 40 more damage and "
                        + "your opponent's Active Pokemon is now Confused."
        );

        assertEquals(1, commands.stream().filter(CoinFlipCommand.class::isInstance).count());
        CoinFlipCommand coin = (CoinFlipCommand) commands.getFirst();
        assertInstanceOf(com.pokemon.tcg.model.battle.command.SequenceCommand.class, coin.getOnHeads());
    }
}
