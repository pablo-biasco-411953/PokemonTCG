package com.pokemon.tcg.service.battle.command;

import com.pokemon.tcg.model.battle.command.*;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class AttackEffectParserServiceTest {

    private final AttackEffectParserService service = new AttackEffectParserService();

    @Test
    void parseEffects_EmptyText_ReturnsEmptyList() {
        assertTrue(service.parseEffects("", null).isEmpty());
        assertTrue(service.parseEffects(null, null).isEmpty());
    }





    @Test
    void parseEffects_HealOwn() {
        List<BattleCommand> commands = service.parseEffects("Heal 30 damage from this Pokémon.", null);
        assertEquals(1, commands.size());
        assertTrue(commands.get(0) instanceof HealCommand);
    }
    
    @Test
    void parseEffects_Poison() {
        List<BattleCommand> commands = service.parseEffects("The Defending Pokémon is now Poisoned.", null);
        assertEquals(1, commands.size());
        assertTrue(commands.get(0) instanceof ApplyStatusConditionCommand);
    }

    @Test
    void parseEffects_CoinFlipDamage() {
        List<BattleCommand> commands = service.parseEffects("Flip 2 coins. This attack does 20 damage times the number of heads.", null);
        assertEquals(1, commands.size());
        assertTrue(commands.get(0) instanceof MultiCoinDamageCommand);
    }

    @Test
    void parseEffects_SelfDamage() {
        List<BattleCommand> commands = service.parseEffects("This Pokémon does 20 damage to itself.", null);
        assertEquals(1, commands.size());
        assertTrue(commands.get(0) instanceof SelfDamageCommand);
    }

    @Test
    void parseEffects_CantRetreat() {
        List<BattleCommand> commands = service.parseEffects("The Defending Pokémon can't retreat during your opponent's next turn.", null);
        assertEquals(1, commands.size());
        assertTrue(commands.get(0) instanceof ApplyStatusConditionCommand);
    }

    @Test
    void parseEffects_HealOwn_Benched() {
        List<BattleCommand> commands = service.parseEffects("Heal 20 damage from 1 of your Benched Pokémon", null);
        assertEquals(1, commands.size());
        assertTrue(commands.get(0) instanceof SelectOwnPokemonToHealCommand);
    }

    @Test
    void parseEffects_UntilTailsDamage() {
        List<BattleCommand> commands = service.parseEffects("Flip a coin until you get tails. This attack does 10 damage times the number of heads", null);
        assertEquals(1, commands.size());
        assertTrue(commands.get(0) instanceof MultiCoinDamageCommand);
    }

    @Test
    void parseEffects_EnergyCoinDamage() {
        List<BattleCommand> commands = service.parseEffects("Flip a coin for each Fire Energy attached to this Pokémon. This attack does 30 damage times the number of heads", null);
        assertEquals(1, commands.size());
        assertTrue(commands.get(0) instanceof MultiCoinDamageCommand);
    }

    @Test
    void parseEffects_DamageCounterCoinDamage() {
        List<BattleCommand> commands = service.parseEffects("Flip a coin for each damage counter on this Pokémon. This attack does 10 damage times the number of heads", null);
        assertEquals(1, commands.size());
        assertTrue(commands.get(0) instanceof MultiCoinDamageCommand);
    }

    @Test
    void parseEffects_CoinDamageAndStatus() {
        List<BattleCommand> commands = service.parseEffects("Flip a coin. If heads, this attack does 20 more damage and your opponent's Active Pokémon is now Poisoned", null);
        assertEquals(1, commands.size());
        assertTrue(commands.get(0) instanceof CoinFlipCommand);
    }

    @Test
    void parseEffects_PutDamageCountersOnAllOpponent() {
        List<BattleCommand> commands = service.parseEffects("Put 10 damage counters each of your opponent's Pokémon", null);
        assertEquals(1, commands.size());
        assertTrue(commands.get(0) instanceof PutDamageCountersOnAllOpponentCommand);
    }

    @Test
    void parseEffects_SetRemainingHpBothActive() {
        List<BattleCommand> commands = service.parseEffects("Put damage counters on both Active Pokémon until the remaining HP of each Pokémon is 10", null);
        assertEquals(1, commands.size());
        assertTrue(commands.get(0) instanceof SetRemainingHpBothActiveCommand);
    }

    @Test
    void parseEffects_AutomatedLookAtTopCardAndShuffle() {
        List<BattleCommand> commands = service.parseEffects("Look at the top card of your opponent's deck. Then, you may have your opponent shuffle", null);
        assertEquals(1, commands.size());
        assertTrue(commands.get(0) instanceof AutomatedLookAtTopCardAndShuffleCommand);
    }

    @Test
    void parseEffects_DamageOwnBenched() {
        List<BattleCommand> commands = service.parseEffects("This attack does 10 damage to each of your Benched Pokémon", null);
        assertTrue(commands.size() >= 1);
        assertTrue(commands.get(0) instanceof DamageOwnBenchedCommand || commands.get(1) instanceof DamageOwnBenchedCommand);
    }

    @Test
    void parseEffects_RhydonMadMountain() {
        List<BattleCommand> commands = service.parseEffects("Flip 2 coins. If both of them are heads, discard the top card of your opponent's deck for each damage counter on this Pokémon", null);
        assertTrue(commands.size() >= 1);
        assertTrue(commands.get(0) instanceof CoinFlipConditionCommand || commands.get(1) instanceof CoinFlipConditionCommand);
    }

    @Test
    void parseEffects_DrawCard() {
        List<BattleCommand> commands = service.parseEffects("Draw 2 cards", null);
        assertEquals(1, commands.size());
        assertTrue(commands.get(0) instanceof DrawCardCommand);
    }

    @Test
    void parseEffects_AsleepAndConfused() {
        List<BattleCommand> commands = service.parseEffects("If heads, your opponent's active pokemon is now asleep. if tails, is now confused", null);
        assertTrue(commands.size() >= 1);
        assertTrue(commands.get(0) instanceof CoinFlipCommand || commands.get(1) instanceof CoinFlipCommand);
    }

    @Test
    void parseEffects_SelfDamageCoin() {
        List<BattleCommand> commands = service.parseEffects("Flip a coin. If tails, this Pokemon does 10 damage to itself", null);
        assertEquals(1, commands.size());
        assertTrue(commands.get(0) instanceof CoinFlipCommand);
    }

    @Test
    void parseEffects_OpponentBenchDamage() {
        List<BattleCommand> commands = service.parseEffects("does 20 damage to 2 of your opponent's benched", null);
        assertTrue(commands.get(0) instanceof DamageOpponentBenchedCommand);
    }
    
    @Test
    void parseEffects_OptionalDiscardEnergyForDamage() {
        List<BattleCommand> commands = service.parseEffects("you may discard an energy attached to this pokémon. if you do, this attack does 30 more damage", null);
        assertTrue(commands.get(0) instanceof OptionalDiscardEnergyForDamageCommand);
    }
    
    @Test
    void parseEffects_DiscardEnergySelf() {
        List<BattleCommand> commands1 = service.parseEffects("discard an energy attached to this pok", null);
        assertTrue(commands1.get(0) instanceof DiscardEnergyCommand);
        
        List<BattleCommand> commands2 = service.parseEffects("discard 2 energy attached to this pok", null);
        assertTrue(commands2.get(0) instanceof DiscardEnergyCommand);
        
        List<BattleCommand> commands3 = service.parseEffects("discard an energy attached to this pok. flip a coin, if tails", null);
        assertTrue(commands3.get(0) instanceof CoinFlipCommand);
    }
    
    @Test
    void parseEffects_DifferentBasicEnergy() {
        List<BattleCommand> commands = service.parseEffects("does 30 more damage for each different type of basic energy attached", null);
        assertTrue(commands.get(0) instanceof AddDamageByDifferentBasicEnergyTypesCommand);
    }
    
    @Test
    void parseEffects_ReduceNextTurnDamageDealt() {
        List<BattleCommand> commands = service.parseEffects("During your opponent's next turn, any damage done by attacks from the Defending Pokémon is reduced by 20", null);
        assertTrue(commands.get(0) instanceof ReduceNextTurnDamageDealtCommand);
    }
    
    @Test
    void parseEffects_DamageIfBench() {
        List<BattleCommand> commands = service.parseEffects("If Pikachu is on your Bench, this attack does 50 more damage", null);
        assertTrue(commands.get(0) instanceof AddDamageIfPokemonOnBenchCommand);
    }
    
    @Test
    void parseEffects_DamageIfStatusAndRemove() {
        List<BattleCommand> commands = service.parseEffects("If your opponent's Active Pokémon is affected by a Special Condition, this attack does 60 more damage. Then, remove all Special Conditions", null);
        assertTrue(commands.get(0) instanceof AddDamageIfStatusConditionAndRemoveCommand);
    }
    
    @Test
    void parseEffects_BlockSupporter() {
        List<BattleCommand> commands = service.parseEffects("flip a coin. if heads, your opponent can't play any supporter cards", null);
        assertTrue(commands.get(0) instanceof CoinFlipCommand);
    }
    
    @Test
    void parseEffects_ForceSwitch() {
        List<BattleCommand> commands = service.parseEffects("your opponent switches his or her active pok with 1 of his or her benched pok", null);
        assertTrue(commands.get(0) instanceof ForceOpponentSwitchCommand);
    }
    
    @Test
    void parseEffects_RandomAsleepOrPoisoned() {
        List<BattleCommand> commands1 = service.parseEffects("choose either asleep or poisoned. your opponent's active pok", "Asleep");
        assertTrue(commands1.get(0) instanceof ApplyStatusConditionCommand);
        List<BattleCommand> commands2 = service.parseEffects("choose either asleep or poisoned. your opponent's active pok", "Poisoned");
        assertTrue(commands2.get(0) instanceof ApplyStatusConditionCommand);
        List<BattleCommand> commands3 = service.parseEffects("choose either asleep or poisoned. your opponent's active pok", null);
        assertTrue(commands3.get(0) instanceof RandomAsleepOrPoisonedCommand);
    }
    
    @Test
    void parseEffects_DiscardEnergyOpponent() {
        List<BattleCommand> commands = service.parseEffects("if heads, discard an energy attached to your opponent's active pok", null);
        assertTrue(commands.get(0) instanceof CoinFlipCommand);
    }
    
    @Test
    void parseEffects_DiscardEnergyOpponentSelect() {
        List<BattleCommand> commands = service.parseEffects("discard an energy attached to 1 of your opponent's pok", null);
        assertTrue(commands.get(0) instanceof CoinFlipCommand); // Code wraps it in CoinFlipCommand ? Wait, yes.
    }
    
    @Test
    void parseEffects_MoveDiscardToTopDeck() {
        List<BattleCommand> commands = service.parseEffects("put a card from your discard pile on top of your deck", null);
        assertTrue(commands.get(0) instanceof MoveDiscardCardToTopDeckCommand);
    }
    
    @Test
    void parseEffects_SetAttackBlockNextTurn() {
        List<BattleCommand> commands = service.parseEffects("tries to attack during your opponent's next turn. if tails, that attack does nothing", null);
        assertTrue(commands.get(0) instanceof SetAttackBlockNextTurnCommand);
    }
    
    @Test
    void parseEffects_SearchDeck() {
        List<BattleCommand> commands1 = service.parseEffects("search your deck for up to 3 different types of basic energy", null);
        assertTrue(commands1.get(0) instanceof SearchDeckCommand);
        List<BattleCommand> commands2 = service.parseEffects("search your deck for a grass pok", null);
        assertTrue(commands2.get(0) instanceof SearchDeckCommand);
        List<BattleCommand> commands3 = service.parseEffects("search your deck for up to 2 supporter", null);
        assertTrue(commands3.get(0) instanceof SearchDeckCommand);
        List<BattleCommand> commands4 = service.parseEffects("search your deck for a supporter", null);
        assertTrue(commands4.get(0) instanceof SearchDeckCommand);
        List<BattleCommand> commands5 = service.parseEffects("search your deck for a fire energy", null);
        assertTrue(commands5.get(0) instanceof SearchDeckCommand);
    }




    @Test
    void parseEffects_BlockSupporterCards() {
        List<BattleCommand> commands = service.parseEffects("Flip a coin. If heads, your opponent can't play any Supporter cards", null);
        assertEquals(1, commands.size());
        assertTrue(commands.get(0) instanceof CoinFlipCommand);
    }

    @Test
    void parseEffects_ForceOpponentSwitch() {
        List<BattleCommand> commands = service.parseEffects("Your opponent switches his or her Active Pokémon with 1 of his or her Benched Pokémon", null);
        assertEquals(1, commands.size());
        assertTrue(commands.get(0) instanceof ForceOpponentSwitchCommand);
    }

    @Test
    void parseEffects_ChooseAsleepOrPoisoned() {
        List<BattleCommand> commands = service.parseEffects("Choose either Asleep or Poisoned. Your opponent's Active Pokémon is now that Special Condition", "Asleep");
        assertEquals(1, commands.size());
        assertTrue(commands.get(0) instanceof ApplyStatusConditionCommand);
    }
}
