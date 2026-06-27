package com.pokemon.tcg.service.battle.command;

import com.pokemon.tcg.model.battle.command.*;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AttackEffectParserServiceMoreTest {

    private final AttackEffectParserService service = new AttackEffectParserService();

    // ====== Heal variants ======

    @Test
    void healFrom1OfYourPokemon_returnsSelectOwnHeal() {
        List<BattleCommand> cmds = service.parseEffects("Heal 20 damage from 1 of your Pokémon", null);
        assertEquals(1, cmds.size());
        assertInstanceOf(SelectOwnPokemonToHealCommand.class, cmds.get(0));
    }

    // ====== Cannot retreat variant ======

    @Test
    void cannotRetreat_returnsStatusCommand() {
        List<BattleCommand> cmds = service.parseEffects("The Defending Pokémon cannot retreat during your opponent's next turn.", null);
        assertEquals(1, cmds.size());
        assertInstanceOf(ApplyStatusConditionCommand.class, cmds.get(0));
    }

    // ====== Draw a card (single) ======

    @Test
    void drawACard_returnsDrawCardCommand() {
        List<BattleCommand> cmds = service.parseEffects("Draw a card.", null);
        assertEquals(1, cmds.size());
        assertInstanceOf(DrawCardCommand.class, cmds.get(0));
    }

    // ====== Both active confused ======

    @Test
    void bothActiveConfused_returnsApplyBothActiveStatus() {
        List<BattleCommand> cmds = service.parseEffects(
                "Both Active Pokémon are now Confused.", null);
        assertTrue(cmds.stream().anyMatch(c -> c instanceof ApplyBothActiveStatusConditionCommand));
    }

    // ====== Invulnerable (direct, no coin) ======

    @Test
    void preventAllDamage_directNoHeads_returnsSetInvulnerable() {
        List<BattleCommand> cmds = service.parseEffects(
                "Prevent all damage done to this Pokémon by attacks during your opponent's next turn.", null);
        assertTrue(cmds.stream().anyMatch(c -> c instanceof SetInvulnerableCommand));
    }

    // ====== Invulnerable (coin - prevent effects) ======

    @Test
    void preventEffectsIfHeads_returnsCoinFlipCommand() {
        List<BattleCommand> cmds = service.parseEffects(
                "Flip a coin. If heads, prevent all effects of attacks done to this Pokémon.", null);
        assertTrue(cmds.stream().anyMatch(c -> c instanceof CoinFlipCommand));
    }

    // ====== Prevent threshold ======

    @Test
    void preventThreshold_returnsSetPreventDamageThreshold() {
        List<BattleCommand> cmds = service.parseEffects(
                "Prevent that attack's damage done to this Pokémon if that damage is 60 or less.", null);
        assertTrue(cmds.stream().anyMatch(c -> c instanceof SetPreventDamageThresholdCommand));
    }

    // ====== Can't attack next turn (no coin) ======

    @Test
    void cantAttackNextTurn_direct_returnsSetNoPuedeAtacar() {
        List<BattleCommand> cmds = service.parseEffects(
                "This Pokémon can't attack during your next turn.", null);
        assertTrue(cmds.stream().anyMatch(c -> c instanceof SetNoPuedeAtacarSiguienteTurnoCommand));
    }

    // ====== Can't attack next turn (coin tails) ======

    @Test
    void cantAttackNextTurn_coinTails_returnsCoinWithSetNoPuedeAtacar() {
        List<BattleCommand> cmds = service.parseEffects(
                "Flip a coin. If tails, this Pokémon cannot attack during your next turn.", null);
        assertTrue(cmds.stream().anyMatch(c -> c instanceof CoinFlipCommand));
    }

    // ====== Defending can't attack ======

    @Test
    void defendingCannotAttack_returnsSetCannotAttackDefending() {
        List<BattleCommand> cmds = service.parseEffects(
                "The Defending Pokémon can't attack during your opponent's next turn.", null);
        assertTrue(cmds.stream().anyMatch(c -> c instanceof SetCannotAttackDefendingCommand));
    }

    // ====== Block specific attack next turn ======

    @Test
    void blockSpecificAttack_returnsBlockAttackNextTurn() {
        List<BattleCommand> cmds = service.parseEffects(
                "This Pokémon can't use Tackle during your next turn.", null);
        assertTrue(cmds.stream().anyMatch(c -> c instanceof BlockAttackNextTurnCommand));
    }

    // ====== Buff attack next turn ======

    @Test
    void buffAttackNextTurn_returnsAtaquePotenciado() {
        List<BattleCommand> cmds = service.parseEffects(
                "During your next turn, this Pokémon's Vine Whip attack does 30 more damage.", null);
        assertTrue(cmds.stream().anyMatch(c -> c instanceof AtaquePotenciadoSiguienteTurnoCommand));
    }

    // ====== Self bench damage ======

    @Test
    void selfBenchDamage_returnsSelfBenchDamageCommand() {
        List<BattleCommand> cmds = service.parseEffects(
                "This attack does 10 damage to each of your Benched Pokémon.", null);
        assertTrue(cmds.stream().anyMatch(c -> c instanceof SelfBenchDamageCommand));
    }

    // ====== Opponent bench - each ======

    @Test
    void damageEachOpponentBenched_returnsDamageOpponentBenched() {
        List<BattleCommand> cmds = service.parseEffects(
                "This attack does 20 damage to each of your opponent's Benched Pokémon.", null);
        assertTrue(cmds.stream().anyMatch(c -> c instanceof DamageOpponentBenchedCommand));
    }

    // ====== Opponent bench - 1 ======

    @Test
    void damage1OpponentBenched_returnsDamageOpponentBenched() {
        List<BattleCommand> cmds = service.parseEffects(
                "This attack does 30 damage to 1 of your opponent's Benched Pokémon.", null);
        assertTrue(cmds.stream().anyMatch(c -> c instanceof DamageOpponentBenchedCommand));
    }

    // ====== Prize cards multiplier ======

    @Test
    void timesRemainingPrizeCards_returnsConditionalMultiplier() {
        List<BattleCommand> cmds = service.parseEffects(
                "This attack does 20 damage times the number of your remaining prize cards.", null);
        assertTrue(cmds.stream().anyMatch(c -> c instanceof ConditionalDamageMultiplierCommand));
    }

    // ====== Benched pokemon multiplier ======

    @Test
    void benchedPokemonMultiplier_returnsConditionalMultiplier() {
        List<BattleCommand> cmds = service.parseEffects(
                "This attack does 20 damage times the number of your Benched Pokémon.", null);
        assertTrue(cmds.stream().anyMatch(c -> c instanceof ConditionalDamageMultiplierCommand));
    }

    // ====== Opponent type multiplier ======

    @Test
    void opponentGrassType_returnsConditionalMultiplier() {
        List<BattleCommand> cmds = service.parseEffects(
                "If your opponent's Active Pokémon is a Grass Pokémon, this attack does 20 more damage.", null);
        assertTrue(cmds.stream().anyMatch(c -> c instanceof ConditionalDamageMultiplierCommand));
    }

    // ====== Opponent has damage counters ======

    @Test
    void opponentHasDamageCounters_returnsConditionalMultiplier() {
        List<BattleCommand> cmds = service.parseEffects(
                "If the Defending Pokémon already has any damage counters on it, this attack does 40 more damage.", null);
        assertTrue(cmds.stream().anyMatch(c -> c instanceof ConditionalDamageMultiplierCommand));
    }

    // ====== Typed energy damage ======

    @Test
    void typedEnergyDamage_returnsAddDamageByAttachedEnergy() {
        List<BattleCommand> cmds = service.parseEffects(
                "This attack does 20 more damage for each Fire energy attached to this Pokémon.", null);
        assertTrue(cmds.stream().anyMatch(c -> c instanceof AddDamageByAttachedEnergyCommand));
    }

    // ====== All energy (both active) ======

    @Test
    void allEnergyBothActive_returnsAddDamageByAttachedEnergy() {
        List<BattleCommand> cmds = service.parseEffects(
                "This attack does 10 more damage times the amount of energy attached to both Active Pokémon.", null);
        assertTrue(cmds.stream().anyMatch(c -> c instanceof AddDamageByAttachedEnergyCommand));
    }

    // ====== Self damage counters ======

    @Test
    void selfDamageCounters_returnsAddDamageByDamageCounters() {
        List<BattleCommand> cmds = service.parseEffects(
                "This attack does 10 more damage for each damage counter on this Pokémon.", null);
        assertTrue(cmds.stream().anyMatch(c -> c instanceof AddDamageByDamageCountersCommand));
    }

    // ====== Opponent damage counters ======

    @Test
    void opponentDamageCounters_returnsAddDamageByDamageCounters() {
        List<BattleCommand> cmds = service.parseEffects(
                "This attack does 20 more damage for each damage counter on your opponent's Active Pokémon.", null);
        assertTrue(cmds.stream().anyMatch(c -> c instanceof AddDamageByDamageCountersCommand));
    }

    // ====== Search deck - darkness energy ======

    @Test
    void searchDeckDarknessEnergy_returnsSearchDeckCommand() {
        List<BattleCommand> cmds = service.parseEffects(
                "Search your deck for a Darkness Energy card and attach it to this Pokémon.", null);
        assertTrue(cmds.stream().anyMatch(c -> c instanceof SearchDeckCommand));
    }

    // ====== Search deck - lightning energy ======

    @Test
    void searchDeckLightningEnergy_returnsSearchDeckCommand() {
        List<BattleCommand> cmds = service.parseEffects(
                "Search your deck for a Lightning Energy card and attach it to your Active Pokémon.", null);
        assertTrue(cmds.stream().anyMatch(c -> c instanceof SearchDeckCommand));
    }

    // ====== Search deck - lightning energy + switch ======

    @Test
    void searchDeckLightningEnergyAndSwitch_returnsSearchDeckCommand() {
        List<BattleCommand> cmds = service.parseEffects(
                "Search your deck for a Lightning Energy card and attach it to your Active Pokémon. Then, switch this Pokémon with one of your Benched Pokémon.", null);
        assertTrue(cmds.stream().anyMatch(c -> c instanceof SearchDeckCommand));
    }

    // ====== Search deck - any card ======

    @Test
    void searchDeckAnyCard_returnsSearchDeckCommand() {
        List<BattleCommand> cmds = service.parseEffects(
                "Search your deck for a card and put it into your hand. Shuffle your deck afterward.", null);
        assertTrue(cmds.stream().anyMatch(c -> c instanceof SearchDeckCommand));
    }

    // ====== Search deck - basic energy attach ======

    @Test
    void searchDeckBasicEnergyAttach_returnsSearchDeckCommand() {
        List<BattleCommand> cmds = service.parseEffects(
                "Search your deck for a Basic Energy card and attach it to 1 of your Pokémon.", null);
        assertTrue(cmds.stream().anyMatch(c -> c instanceof SearchDeckCommand));
    }

    // ====== Geomancy ======

    @Test
    void geomancy_returnsGeomancyCommand() {
        List<BattleCommand> cmds = service.parseEffects(
                "Choose 2 of your Benched Pokémon. Search your deck for a Fairy Energy card and attach it to each of those Pokémon.", null);
        assertTrue(cmds.stream().anyMatch(c -> c instanceof GeomancyCommand));
    }

    // ====== Search 3 basic energy cards ======

    @Test
    void search3BasicEnergyCards_returnsSearchDeckCommand() {
        List<BattleCommand> cmds = service.parseEffects(
                "Search your deck for 3 different types of basic energy cards and put them into your hand.", null);
        assertTrue(cmds.stream().anyMatch(c -> c instanceof SearchDeckCommand));
    }

    // ====== Search up to 3 basic energy ======

    @Test
    void searchUpTo3BasicEnergy_returnsSearchDeckCommand() {
        List<BattleCommand> cmds = service.parseEffects(
                "Search your deck for up to 3 basic energy cards and put them into your hand.", null);
        assertTrue(cmds.stream().anyMatch(c -> c instanceof SearchDeckCommand));
    }

    // ====== Shuffle random hand card to deck ======

    @Test
    void shuffleRandomHandToDeck_returnsShuffleCommand() {
        List<BattleCommand> cmds = service.parseEffects(
                "Choose a random card from your opponent's hand and shuffle it into their deck.", null);
        assertTrue(cmds.stream().anyMatch(c -> c instanceof ShuffleRandomHandToDeckCommand));
    }

    // ====== Discard random hand cards by coin tails ======

    @Test
    void discardRandomHandByCoinTails_returnsDiscardRandomCommand() {
        List<BattleCommand> cmds = service.parseEffects(
                "Your opponent flips 3 coins. For each tails, discard a card from their hand.", null);
        assertTrue(cmds.stream().anyMatch(c -> c instanceof DiscardRandomHandCardsByCoinTailsCommand));
    }

    // ====== Move energy from this pok ======

    @Test
    void moveBasicEnergyFromThisPok_returnsMoveEnergyCommand() {
        List<BattleCommand> cmds = service.parseEffects(
                "Move a basic energy from this Pokémon to 1 of your Benched Pokémon.", null);
        assertTrue(cmds.stream().anyMatch(c -> c instanceof MoveEnergyCommand));
    }

    @Test
    void moveEnergyFromThisPok_returnsMoveEnergyCommand() {
        List<BattleCommand> cmds = service.parseEffects(
                "Move an energy from this Pokémon to 1 of your Benched Pokémon.", null);
        assertTrue(cmds.stream().anyMatch(c -> c instanceof MoveEnergyCommand));
    }

    @Test
    void moveAsManyEnergy_returnsMoveEnergyCommand() {
        List<BattleCommand> cmds = service.parseEffects(
                "Move as many energy cards attached to your Pokémon as you like.", null);
        assertTrue(cmds.stream().anyMatch(c -> c instanceof MoveEnergyCommand));
    }

    // ====== Move opponent energy to bench ======

    @Test
    void moveOpponentActiveEnergyToBench_returnsCommand() {
        List<BattleCommand> cmds = service.parseEffects(
                "Move an energy attached to your opponent's Active Pokémon to one of their Benched Pokémon.", null);
        assertTrue(cmds.stream().anyMatch(c -> c instanceof MoveOpponentActiveEnergyToBenchCommand));
    }

    // ====== Attach darkness energy from discard ======

    @Test
    void attachDarknessEnergyFromDiscard_returnsCommand() {
        List<BattleCommand> cmds = service.parseEffects(
                "Attach a Darkness Energy card from your discard pile to 1 of your Benched Pokémon.", null);
        assertTrue(cmds.stream().anyMatch(c -> c instanceof AttachEnergyFromDiscardToBenchCommand));
    }

    // ====== You may do more damage + asleep ======

    @Test
    void youMayDoMoreDamageAndAsleep_returnsCommands() {
        List<BattleCommand> cmds = service.parseEffects(
                "You may do 20 more damage. If you do, this Pokémon is now Asleep.", null);
        assertTrue(cmds.stream().anyMatch(c -> c instanceof DamageCommand));
        assertTrue(cmds.stream().anyMatch(c -> c instanceof ApplyStatusConditionCommand));
    }

    // ====== Discard top deck + fighting energy ======

    @Test
    void discardTopDeckFightingEnergy_returnsDiscardTopDeckAttachEnergy() {
        List<BattleCommand> cmds = service.parseEffects(
                "Discard the top card of your deck. If that card is a Fighting Energy, attach it to this Pokémon.", null);
        assertTrue(cmds.stream().anyMatch(c -> c instanceof DiscardTopDeckAttachEnergyCommand));
    }

    // ====== Discard top deck + fire energy (Magcargo) ======

    @Test
    void discardTopDeckFireEnergy_returnsMagcargoMagmaMantle() {
        List<BattleCommand> cmds = service.parseEffects(
                "Discard the top card of your deck. If that card is a Fire Energy, attach it to this Pokémon.", null);
        assertTrue(cmds.stream().anyMatch(c -> c instanceof MagcargoMagmaMantleCommand));
    }

    // ====== Torment (Simisage) ======

    @Test
    void tormentBlockAttack_returnsCommand() {
        List<BattleCommand> cmds = service.parseEffects(
                "Choose 1 of your opponent's Active Pokémon's attacks. That Pokémon can't use that attack next turn.", null);
        assertTrue(cmds.stream().anyMatch(c -> c instanceof TormentBlockAttackCommand));
    }

    // ====== Gogoat ChargeDash ======

    @Test
    void gogoatChargeDash_returnsCommand() {
        List<BattleCommand> cmds = service.parseEffects(
                "You may do 20 more damage. If you do, this Pokémon does 20 damage to itself.", null);
        assertTrue(cmds.stream().anyMatch(c -> c instanceof GogoatChargeDashCommand));
    }

    // ====== Discard fire energy (Simisear) ======

    @Test
    void discardFireEnergy_returnsDiscardAttachedEnergyOfType() {
        List<BattleCommand> cmds = service.parseEffects(
                "Discard a Fire Energy attached to this Pokémon.", null);
        assertTrue(cmds.stream().anyMatch(c -> c instanceof DiscardAttachedEnergyOfTypeCommand));
    }

    // ====== Opponent shuffles hand and draws ======

    @Test
    void opponentShuffleHandDraw_returnsCommand() {
        List<BattleCommand> cmds = service.parseEffects(
                "Your opponent shuffles his or her hand into his or her deck and draws 4 cards.", null);
        assertTrue(cmds.stream().anyMatch(c -> c instanceof OpponentShuffleHandDrawCommand));
    }

    // ====== Discard ALL fire energy (Flare Blitz) ======

    @Test
    void discardAllFireEnergy_returnsDiscardAttachedEnergyOfType() {
        List<BattleCommand> cmds = service.parseEffects(
                "Discard all Fire Energy attached to this Pokémon.", null);
        assertTrue(cmds.stream().anyMatch(c -> c instanceof DiscardAttachedEnergyOfTypeCommand));
    }

    // ====== Discard ALL energy (Raichu Thunderbolt) ======

    @Test
    void discardAllEnergy_returnsDiscardAttachedEnergyOfType() {
        List<BattleCommand> cmds = service.parseEffects(
                "Discard all energy attached to this Pokémon.", null);
        assertTrue(cmds.stream().anyMatch(c -> c instanceof DiscardAttachedEnergyOfTypeCommand));
    }

    // ====== Discard top of opponent deck (no damage counter) ======

    @Test
    void discardTopOpponentDeck_returnsDiscardTopDeckCommand() {
        List<BattleCommand> cmds = service.parseEffects(
                "Discard the top card of your opponent's deck.", null);
        assertTrue(cmds.stream().anyMatch(c -> c instanceof DiscardTopDeckCommand));
    }

    // ====== Rhydon Mad Mountain (discard top opponent deck + damage counter) ======

    @Test
    void rhydonMadMountainCommand_returnsRhydonMadMountainCommand() {
        List<BattleCommand> cmds = service.parseEffects(
                "Discard the top card of your opponent's deck for each damage counter on this Pokémon.", null);
        assertTrue(cmds.stream().anyMatch(c -> c instanceof RhydonMadMountainCommand));
    }

    // ====== Peek top deck ======

    @Test
    void peekTop3Cards_returnsPeekTopDeckCommand() {
        List<BattleCommand> cmds = service.parseEffects(
                "Look at the top 3 cards of your deck. Then, put them back on top of your deck in any order.", null);
        assertTrue(cmds.stream().anyMatch(c -> c instanceof PeekTopDeckCommand));
    }

    // ====== Switch opponent active ======

    @Test
    void switchOpponentActive_returnsSwitchOpponentActiveCommand() {
        List<BattleCommand> cmds = service.parseEffects(
                "Switch 1 of your opponent's Benched Pokémon with your opponent's Active Pokémon.", null);
        assertTrue(cmds.stream().anyMatch(c -> c instanceof SwitchOpponentActiveCommand));
    }

    // ====== Starmie Recover (discard energy + heal all) ======

    @Test
    void starmieRecover_returnsDiscardAndHeal() {
        List<BattleCommand> cmds = service.parseEffects(
                "Discard an Energy attached to this Pokémon and heal all damage from it.", null);
        assertTrue(cmds.stream().anyMatch(c -> c instanceof DiscardAttachedEnergyOfTypeCommand));
        assertTrue(cmds.stream().anyMatch(c -> c instanceof HealCommand));
    }

    // ====== Starmie Core Splash (has psychic energy) ======

    @Test
    void starmiePsychicEnergy_returnsConditionalMultiplier() {
        List<BattleCommand> cmds = service.parseEffects(
                "If this Pokémon has any Psychic Energy attached to it, this attack does 30 more damage.", null);
        assertTrue(cmds.stream().anyMatch(c -> c instanceof ConditionalDamageMultiplierCommand));
    }

    // ====== Lapras Seafaring ======

    @Test
    void laprasSeafaring_returnsCommand() {
        List<BattleCommand> cmds = service.parseEffects(
                "Flip a coin. If heads, attach a Water Energy card from your discard pile to your Benched Pokémon.", null);
        assertTrue(cmds.stream().anyMatch(c -> c instanceof LaprasSeafaringCommand));
    }

    // ====== Corsola Refresh ======

    @Test
    void corsolaRefresh_returnsCommand() {
        List<BattleCommand> cmds = service.parseEffects(
                "Heal 30 damage and remove all Special Conditions from this Pokémon.", null);
        assertTrue(cmds.stream().anyMatch(c -> c instanceof CorsolaRefreshCommand));
    }

    // ====== Froakie Bounce ======

    @Test
    void froakieBounce_returnsCommand() {
        List<BattleCommand> cmds = service.parseEffects(
                "Flip a coin. If heads, switch this Pokémon with one of your Benched Pokémon.", null);
        assertTrue(cmds.stream().anyMatch(c -> c instanceof FroakieBounceCommand));
    }

    // ====== Pickup 2 item cards ======

    @Test
    void pickupItemCards_returnsPickupCommand() {
        List<BattleCommand> cmds = service.parseEffects(
                "Put 2 Item cards from your discard pile into your hand.", null);
        assertTrue(cmds.stream().anyMatch(c -> c instanceof PickupCommand));
    }

    // ====== Coin flip damage (simple - not combined) ======

    @Test
    void simpleCoinFlipDamage_returnsCoinFlipCommand() {
        List<BattleCommand> cmds = service.parseEffects(
                "Flip a coin. If heads, this attack does 20 more damage.", null);
        assertTrue(cmds.stream().anyMatch(c -> c instanceof CoinFlipCommand));
    }

    // ====== Curly quotes normalization ======

    @Test
    void curlyQuotesNormalized_parsesCorrectly() {
        List<BattleCommand> cmds = service.parseEffects(
                "This Pokémon can’t retreat.", null);
        assertTrue(cmds.stream().anyMatch(c -> c instanceof ApplyStatusConditionCommand));
    }

    // ====== Paralyzed status ======

    @Test
    void paralyzeStatus_returnsStatusCommand() {
        List<BattleCommand> cmds = service.parseEffects(
                "The Defending Pokémon is now Paralyzed.", null);
        assertTrue(cmds.stream().anyMatch(c -> c instanceof ApplyStatusConditionCommand));
    }

    // ====== Burned status ======

    @Test
    void burnedStatus_returnsStatusCommand() {
        List<BattleCommand> cmds = service.parseEffects(
                "Your opponent's Active Pokémon is now Burned.", null);
        assertTrue(cmds.stream().anyMatch(c -> c instanceof ApplyStatusConditionCommand));
    }

    // ====== Asleep with coin flip ======

    @Test
    void asleepWithCoin_returnsCoinFlipCommand() {
        List<BattleCommand> cmds = service.parseEffects(
                "Flip a coin. If heads, your opponent's Active Pokémon is now Asleep.", null);
        assertTrue(cmds.stream().anyMatch(c -> c instanceof CoinFlipCommand));
    }

    // ====== Energy coin - no specific type (attached energy) ======

    @Test
    void energyCoinNoType_returnsMultiCoinCommand() {
        List<BattleCommand> cmds = service.parseEffects(
                "Flip a coin for each Energy attached to this Pokémon. This attack does 20 damage times the number of heads.", null);
        assertTrue(cmds.stream().anyMatch(c -> c instanceof MultiCoinDamageCommand));
    }

    // ====== All heads prevent damage ======

    @Test
    void allHeadsPreventDamage_returnsMultiCoin() {
        List<BattleCommand> cmds = service.parseEffects(
                "Flip 3 coins. This attack does 20 damage times the number of heads. If all of them are heads, prevent all damage done to this Pokémon.", null);
        assertTrue(cmds.stream().anyMatch(c -> c instanceof MultiCoinDamageCommand));
    }

    // ====== Poison + Paralyzed (both active status) ======

    @Test
    void paralyzedAndPoisoned_returnsExtraPoison() {
        List<BattleCommand> cmds = service.parseEffects(
                "Your opponent's Active Pokémon is now Paralyzed and Poisoned.", null);
        long poisonCount = cmds.stream().filter(c -> c instanceof ApplyStatusConditionCommand).count();
        assertTrue(poisonCount >= 1);
    }

    // ====== Discard 2 energy + coin tails ======

    @Test
    void discard2EnergyWithCoinTails_returnsCoinFlip() {
        List<BattleCommand> cmds = service.parseEffects(
                "Flip a coin. If tails, discard 2 energy attached to this Pokémon.", null);
        assertTrue(cmds.stream().anyMatch(c -> c instanceof CoinFlipCommand));
    }
}
