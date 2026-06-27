package com.pokemon.tcg.service.battle.command;

import com.pokemon.tcg.model.battle.command.*;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class AttackEffectParserService {

    private static final Pattern HEAL_PATTERN = Pattern.compile("Heal (\\d+) damage from this Pok.{1,2}mon", Pattern.CASE_INSENSITIVE);
    private static final Pattern COIN_FLIP_DAMAGE_PATTERN = Pattern.compile("Flip a coin\\. If heads, this attack does (\\d+) more damage", Pattern.CASE_INSENSITIVE);
    private static final Pattern DRAW_CARD_PATTERN = Pattern.compile("Draw (?:(\\d+) cards?|a card)", Pattern.CASE_INSENSITIVE);
    private static final Pattern APPLY_STATUS_PATTERN = Pattern.compile("(?:The Defending Pok.{1,2}mon|Your opponent's Active Pok.{1,2}mon) is now (Poisoned|Asleep|Burned|Paralyzed|Confused)", Pattern.CASE_INSENSITIVE);
    private static final Pattern SELF_DAMAGE_PATTERN = Pattern.compile("does (\\d+) damage to itself", Pattern.CASE_INSENSITIVE);
    private static final Pattern FIXED_MULTI_COIN_DAMAGE_PATTERN = Pattern.compile(
            "Flip (\\d+) coins?\\. This attack does (\\d+)(?: more)? damage (?:times the number of|for each) heads",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern UNTIL_TAILS_DAMAGE_PATTERN = Pattern.compile(
            "Flip a coin until you get tails\\. This attack does (\\d+) (?:more )?damage (?:times the number of|for each) heads",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern ENERGY_COIN_DAMAGE_PATTERN = Pattern.compile(
            "Flip a coin for each(?: ([A-Za-z]+))? Energy attached to this Pok.{1,2}mon\\. This attack does (\\d+) damage times the number of heads",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern DAMAGE_COUNTER_COIN_PATTERN = Pattern.compile(
            "Flip a coin for each damage counter on this Pok.{1,2}mon\\. This attack does (\\d+) damage times the number of heads",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern COIN_DAMAGE_AND_STATUS_PATTERN = Pattern.compile(
            "Flip a coin\\. If heads, this attack does (\\d+) more damage and your opponent's Active Pok.{1,2}mon is now (Poisoned|Asleep|Burned|Paralyzed|Confused)",
            Pattern.CASE_INSENSITIVE
    );

    public List<BattleCommand> parseEffects(String text, String extraParams) {
        List<BattleCommand> commands = new ArrayList<>();
        if (text == null || text.trim().isEmpty()) {
            return commands;
        }
        text = normalizeQuotes(text);
        String lowerText = text.toLowerCase();
        Matcher combinedCoinMatcher = COIN_DAMAGE_AND_STATUS_PATTERN.matcher(text);
        boolean combinedCoinHandled = combinedCoinMatcher.find();
        if (combinedCoinHandled) {
            commands.add(new CoinFlipCommand(new SequenceCommand(
                    new DamageCommand(Integer.parseInt(combinedCoinMatcher.group(1))),
                    new ApplyStatusConditionCommand(combinedCoinMatcher.group(2), Target.OPPONENT)
            )));
        }

        Matcher healMatcher = Pattern.compile("Heal (\\d+) damage from this Pok.{1,2}mon", Pattern.CASE_INSENSITIVE).matcher(text);
        if (healMatcher.find()) {
            int amount = Integer.parseInt(healMatcher.group(1));
            commands.add(new HealCommand(amount, Target.SELF));
        }

        Matcher healOwnMatcher = Pattern.compile("Heal (\\d+) damage from 1 of your Pok.{1,2}mon", Pattern.CASE_INSENSITIVE).matcher(text);
        if (healOwnMatcher.find()) {
            int amount = Integer.parseInt(healOwnMatcher.group(1));
            commands.add(new SelectOwnPokemonToHealCommand(amount));
        }

        Matcher healOwnBenchedMatcher = Pattern.compile("Heal (\\d+) damage from 1 of your Benched Pok.{1,2}mon", Pattern.CASE_INSENSITIVE).matcher(text);
        if (healOwnBenchedMatcher.find()) {
            int amount = Integer.parseInt(healOwnBenchedMatcher.group(1));
            commands.add(new SelectOwnPokemonToHealCommand(amount, true));
        }

        Matcher fixedMultiMatcher = FIXED_MULTI_COIN_DAMAGE_PATTERN.matcher(text);
        Matcher untilTailsMatcher = UNTIL_TAILS_DAMAGE_PATTERN.matcher(text);
        Matcher energyCoinMatcher = ENERGY_COIN_DAMAGE_PATTERN.matcher(text);
        Matcher damageCounterCoinMatcher = DAMAGE_COUNTER_COIN_PATTERN.matcher(text);
        Matcher coinFlipMatcher = COIN_FLIP_DAMAGE_PATTERN.matcher(text);
        if (fixedMultiMatcher.find()) {
            commands.add(new MultiCoinDamageCommand(
                    MultiCoinDamageCommand.CountMode.FIXED,
                    Integer.parseInt(fixedMultiMatcher.group(1)),
                    Integer.parseInt(fixedMultiMatcher.group(2)),
                    null,
                    lowerText.contains("if all of them are heads") && lowerText.contains("prevent all")
            ));
        } else if (untilTailsMatcher.find()) {
            commands.add(new MultiCoinDamageCommand(
                    MultiCoinDamageCommand.CountMode.UNTIL_TAILS,
                    0,
                    Integer.parseInt(untilTailsMatcher.group(1)),
                    null,
                    false
            ));
        } else if (energyCoinMatcher.find()) {
            String energyType = energyCoinMatcher.group(1);
            commands.add(new MultiCoinDamageCommand(
                    energyType == null
                            ? MultiCoinDamageCommand.CountMode.ATTACHED_ENERGY
                            : MultiCoinDamageCommand.CountMode.ATTACHED_ENERGY_TYPE,
                    0,
                    Integer.parseInt(energyCoinMatcher.group(2)),
                    energyType,
                    false
            ));
        } else if (damageCounterCoinMatcher.find()) {
            commands.add(new MultiCoinDamageCommand(
                    MultiCoinDamageCommand.CountMode.DAMAGE_COUNTERS,
                    0,
                    Integer.parseInt(damageCounterCoinMatcher.group(1)),
                    null,
                    false
            ));
        } else if (!combinedCoinHandled && coinFlipMatcher.find()) {
            int extraDamage = Integer.parseInt(coinFlipMatcher.group(1));
            commands.add(new CoinFlipCommand(new DamageCommand(extraDamage)));
        }

        Matcher putCountersAllOpponent = Pattern.compile("Put (\\d+) damage counters each of your opponent's Pok.{1,2}mon", Pattern.CASE_INSENSITIVE).matcher(text);
        if (putCountersAllOpponent.find()) {
            commands.add(new PutDamageCountersOnAllOpponentCommand(Integer.parseInt(putCountersAllOpponent.group(1))));
        }

        Matcher setRemainingHpBothActive = Pattern.compile("Put damage counters on both Active Pok.{1,2}mon until the remaining HP of each Pok.{1,2}mon is (\\d+)", Pattern.CASE_INSENSITIVE).matcher(text);
        if (setRemainingHpBothActive.find()) {
            commands.add(new SetRemainingHpBothActiveCommand(Integer.parseInt(setRemainingHpBothActive.group(1))));
        }

        if (lowerText.contains("look at the top card of your opponent's deck. then, you may have your opponent shuffle")) {
            commands.add(new AutomatedLookAtTopCardAndShuffleCommand());
        }

        Matcher damageOwnBenched = Pattern.compile("This attack does (\\d+) damage to each of your Benched Pok.{1,2}mon", Pattern.CASE_INSENSITIVE).matcher(text);
        if (damageOwnBenched.find()) {
            commands.add(new DamageOwnBenchedCommand(Integer.parseInt(damageOwnBenched.group(1))));
        }

        Matcher rhydonMadMountain = Pattern.compile("Flip (\\d+) coins. If both of them are heads, discard the top card of your opponent's deck for each damage counter on this Pok.{1,2}mon", Pattern.CASE_INSENSITIVE).matcher(text);
        if (rhydonMadMountain.find()) {
            commands.add(new CoinFlipConditionCommand(Integer.parseInt(rhydonMadMountain.group(1)), Integer.parseInt(rhydonMadMountain.group(1)), new DiscardOpponentDeckPerDamageCounterCommand(Integer.parseInt(rhydonMadMountain.group(1)))));
        }

        Matcher drawMatcher = DRAW_CARD_PATTERN.matcher(text);
        if (drawMatcher.find()) {
            int cards = drawMatcher.group(1) == null ? 1 : Integer.parseInt(drawMatcher.group(1));
            commands.add(new DrawCardCommand(cards, Target.SELF));
        }

        Matcher statusMatcher = APPLY_STATUS_PATTERN.matcher(text);
        while (!combinedCoinHandled && statusMatcher.find()) {
            ApplyStatusConditionCommand statusCommand =
                    new ApplyStatusConditionCommand(statusMatcher.group(1), Target.OPPONENT);
            if (lowerText.contains("flip a coin") && lowerText.contains("if heads")) {
                commands.add(new CoinFlipCommand(statusCommand));
            } else {
                commands.add(statusCommand);
            }
        }
        if (lowerText.matches("(?s).*is now [^.]*paralyzed and poisoned.*")) {
            boolean conditional = lowerText.contains("flip a coin") && lowerText.contains("if heads");
            ApplyStatusConditionCommand poison = new ApplyStatusConditionCommand("Poisoned", Target.OPPONENT);
            if (conditional) commands.add(new CoinFlipCommand(poison));
            else commands.add(poison);
        }
        if (lowerText.contains("both active pok") && lowerText.contains("are now confused")) {
            commands.add(new ApplyBothActiveStatusConditionCommand("Confused"));
        }

        if (lowerText.contains("if heads, prevent all effects of attacks")
                || lowerText.contains("if heads, prevent all damage done to this pok")) {
            commands.add(new CoinFlipCommand(new SetInvulnerableCommand()));
        }

        if (lowerText.contains("if heads, your opponent's active pok")
                && lowerText.contains("is now asleep")
                && lowerText.contains("if tails")
                && lowerText.contains("is now confused")) {
            commands.removeIf(command -> command instanceof CoinFlipCommand);
            commands.add(new CoinFlipCommand(
                    new ApplyStatusConditionCommand("Asleep", Target.OPPONENT),
                    new ApplyStatusConditionCommand("Confused", Target.OPPONENT)
            ));
        }

        if (lowerText.contains("can't retreat") || lowerText.contains("cannot retreat")) {
            commands.add(new ApplyStatusConditionCommand("CantRetreat", Target.OPPONENT));
        }

        Matcher selfDamageMatcher = SELF_DAMAGE_PATTERN.matcher(text);
        if (selfDamageMatcher.find() && !lowerText.contains("you may do")) {
            int amount = Integer.parseInt(selfDamageMatcher.group(1));
            if (lowerText.contains("if tails")) {
                commands.add(new CoinFlipCommand(null, new SelfDamageCommand(amount)));
            } else {
                commands.add(new SelfDamageCommand(amount));
            }
        }

        Matcher opponentBenchDamageMatcher = Pattern.compile("does (\\d+) damage to (\\d+) of your opponent's benched", Pattern.CASE_INSENSITIVE).matcher(text);
        if (opponentBenchDamageMatcher.find()) {
            int amount = Integer.parseInt(opponentBenchDamageMatcher.group(1));
            int count = Integer.parseInt(opponentBenchDamageMatcher.group(2));
            commands.add(new DamageOpponentBenchedCommand(amount, count));
        }
        Matcher optionalDiscardDamageMatcher = Pattern.compile("you may discard an energy attached to this pok.{1,2}mon\\.\\s*if you do, this attack does (\\d+) more damage", Pattern.CASE_INSENSITIVE).matcher(text);
        if (optionalDiscardDamageMatcher.find()) {
            int extraDamage = Integer.parseInt(optionalDiscardDamageMatcher.group(1));
            commands.add(new OptionalDiscardEnergyForDamageCommand(extraDamage, extraParams));
        } else if (lowerText.contains("discard an energy attached to this pok") || lowerText.contains("discard 2 energy attached to this pok")) {
            int amount = lowerText.contains("discard 2 energy") ? 2 : 1;
            if (lowerText.contains("flip a coin") && lowerText.contains("if tails")) {
                commands.add(new CoinFlipCommand(null, new DiscardEnergyCommand(amount, Target.SELF)));
            } else {
                commands.add(new DiscardEnergyCommand(amount, Target.SELF));
            }
        }
        
        Matcher differentBasicEnergyMatcher = Pattern.compile("does (\\d+) more damage for each different type of basic energy attached", Pattern.CASE_INSENSITIVE).matcher(text);
        if (differentBasicEnergyMatcher.find()) {
            int extraDamage = Integer.parseInt(differentBasicEnergyMatcher.group(1));
            commands.add(new AddDamageByDifferentBasicEnergyTypesCommand(extraDamage));
        }

        Matcher reduceDamageDealt = Pattern.compile("During your opponent's next turn, any damage done by attacks from the Defending Pok.{1,2}mon is reduced by (\\d+)", Pattern.CASE_INSENSITIVE).matcher(text);
        if (reduceDamageDealt.find()) {
            commands.add(new ReduceNextTurnDamageDealtCommand(Integer.parseInt(reduceDamageDealt.group(1))));
        }

        Matcher damageIfBench = Pattern.compile("If ([A-Za-z]+) is on your Bench, this attack does (\\d+) more damage", Pattern.CASE_INSENSITIVE).matcher(text);
        if (damageIfBench.find()) {
            commands.add(new AddDamageIfPokemonOnBenchCommand(damageIfBench.group(1), Integer.parseInt(damageIfBench.group(2))));
        }

        Matcher damageIfStatusAndRemove = Pattern.compile("If your opponent's Active Pok.{1,2}mon is affected by a Special Condition, this attack does (\\d+) more damage\\. Then, remove all Special Conditions", Pattern.CASE_INSENSITIVE).matcher(text);
        if (damageIfStatusAndRemove.find()) {
            commands.add(new AddDamageIfStatusConditionAndRemoveCommand(Integer.parseInt(damageIfStatusAndRemove.group(1))));
        }

        if (lowerText.contains("flip a coin. if heads, your opponent can't play any supporter cards")) {
            commands.add(new CoinFlipCommand(new BlockSupporterCardsNextTurnCommand()));
        }

        if (lowerText.contains("your opponent switches his or her active pok") && lowerText.contains("with 1 of his or her benched pok")) {
            commands.add(new ForceOpponentSwitchCommand());
        }
        
        if (lowerText.contains("choose either asleep or poisoned. your opponent's active pok")) {
            if ("Asleep".equalsIgnoreCase(extraParams)) {
                commands.add(new ApplyStatusConditionCommand("Asleep", Target.OPPONENT));
            } else if ("Poisoned".equalsIgnoreCase(extraParams)) {
                commands.add(new ApplyStatusConditionCommand("Poisoned", Target.OPPONENT));
            } else {
                commands.add(new RandomAsleepOrPoisonedCommand(Target.OPPONENT));
            }
        }

        if (lowerText.contains("if heads, discard an energy attached to your opponent's active pok")) {
            commands.add(new CoinFlipCommand(new DiscardEnergyCommand(1, Target.OPPONENT)));
        }

        if (lowerText.contains("discard an energy attached to 1 of your opponent's pok")) {
            commands.add(new CoinFlipCommand(new SelectOpponentPokemonToDiscardEnergyCommand()));
        }

        if (lowerText.contains("put a card from your discard pile on top of your deck")) {
            commands.add(new MoveDiscardCardToTopDeckCommand(
                    Target.SELF,
                    1,
                    "Elegi una carta de tu descarte para poner arriba del mazo."
            ));
        }

        if (lowerText.contains("tries to attack during your opponent's next turn")
                && lowerText.contains("if tails")
                && lowerText.contains("that attack does nothing")) {
            commands.add(new SetAttackBlockNextTurnCommand(Target.OPPONENT));
        }

        if (lowerText.contains("search your deck for up to 3 different types of basic energy")) {
            commands.add(new SearchDeckCommand(
                    "Energy", "Basic", null, "HAND", 3,
                    "Elegi hasta 3 Energias Basicas para revelar y poner en tu mano."
            ));
        } else if (lowerText.contains("search your deck for a grass pok")) {
            commands.add(new SearchDeckCommand(
                    "Pokemon", null, "Grass", "HAND", 1,
                    "Elegí un Pokémon Planta para poner en tu mano."
            ));
        } else if (lowerText.contains("search your deck for up to 2 supporter")) {
            commands.add(new SearchDeckCommand(
                    "Trainer", "Supporter", null, "HAND", 2,
                    "Elegí hasta 2 cartas de Partidario para poner en tu mano."
            ));
        } else if (lowerText.contains("search your deck for a supporter")) {
            commands.add(new SearchDeckCommand(
                    "Trainer", "Supporter", null, "HAND", 1,
                    "Elegí una carta de Partidario para poner en tu mano."
            ));
        } else if (lowerText.contains("search your deck for a fire energy")) {
            commands.add(new SearchDeckCommand(
                    "Energy", null, "Fire", "ATTACH_ACTIVE", 1,
                    "Elegí una Energía Fuego para unir a tu Pokémon Activo."
            ));
        } else if (lowerText.contains("search your deck for a darkness energy")) {
            commands.add(new SearchDeckCommand(
                    "Energy", null, "Darkness", "ATTACH_ACTIVE", 1,
                    "Elegi una Energia Oscuridad para unir a tu Pokemon Activo."
            ));
        } else if (lowerText.contains("search your deck for a lightning energy")) {
            String dest = lowerText.contains("switch this pok") ? "ATTACH_ACTIVE_AND_SWITCH" : "ATTACH_ACTIVE";
            commands.add(new SearchDeckCommand(
                    "Energy", null, "Lightning", dest, 1,
                    "Elegí una Energía Eléctrica para unir a tu Pokémon Activo."
            ));
        } else if (lowerText.contains("search your deck for a card and put it into your hand")) {
            commands.add(new SearchDeckCommand(
                    null, null, null, "HAND", 1,
                    "Elegí una carta de tu mazo para poner en tu mano."
            ));
        } else if (lowerText.contains("search your deck for a basic energy card") && lowerText.contains("attach it to 1 of your")) {
            commands.add(new SearchDeckCommand(
                    "Energy", "Basic", null, "SELECT_POKEMON_FOR_GATHER_ENERGY", 1,
                    "Gather Energy: Seleccioná 1 Energía Básica de tu mazo"
            ));
        } else if (lowerText.contains("choose 2 of your benched") && lowerText.contains("search your deck for a fairy energy card")) {
            commands.add(new GeomancyCommand());
        }

        if (lowerText.contains("choose a random card from your opponent's hand") && lowerText.contains("shuffle")) {
            commands.add(new ShuffleRandomHandToDeckCommand());
        }

        if (lowerText.contains("move a basic energy from this pok") || lowerText.contains("move an energy from this pok") || lowerText.contains("move as many")) {
            commands.add(new MoveEnergyCommand(null, 1));
        }

        if (lowerText.contains("move an energy attached to your opponent's active pok")) {
            commands.add(new MoveOpponentActiveEnergyToBenchCommand(extraParams));
        }

        if (lowerText.contains("attach a darkness energy card from your discard pile to 1 of your benched")) {
            commands.add(new AttachEnergyFromDiscardToBenchCommand("Darkness", 1));
        }

        Matcher oppBenchDamageMatcher = Pattern
                .compile("does (\\d+) damage to (\\d+) of your opponent's benched", Pattern.CASE_INSENSITIVE)
                .matcher(text);
        boolean matchedBenchDamage = false;
        if (oppBenchDamageMatcher.find()) {
            matchedBenchDamage = true;
            commands.add(new DamageOpponentBenchedCommand(
                    Integer.parseInt(oppBenchDamageMatcher.group(1)),
                    Integer.parseInt(oppBenchDamageMatcher.group(2))
            ));
        }

        Matcher oppBenchEachDamageMatcher = Pattern
                .compile("does (\\d+) damage to each of your opponent's benched", Pattern.CASE_INSENSITIVE)
                .matcher(text);
        if (oppBenchEachDamageMatcher.find()) {
            matchedBenchDamage = true;
            commands.add(new DamageOpponentBenchedCommand(
                    Integer.parseInt(oppBenchEachDamageMatcher.group(1)),
                    6
            ));
        }

        Matcher oppBench1DamageMatcher = Pattern
                .compile("does (\\d+) damage to 1 of your opponent's benched", Pattern.CASE_INSENSITIVE)
                .matcher(text);
        if (!matchedBenchDamage && oppBench1DamageMatcher.find()) {
            commands.add(new DamageOpponentBenchedCommand(
                    Integer.parseInt(oppBench1DamageMatcher.group(1)),
                    1
            ));
        }

        if (lowerText.contains("times the number of your remaining prize cards")) {
            commands.add(new ConditionalDamageMultiplierCommand(0, 20, "PRIZE_CARDS", null));
        }
        
        Matcher benchMultiplierMatcher = Pattern
                .compile("does (\\d+) damage times the number of your benched", Pattern.CASE_INSENSITIVE)
                .matcher(text);
        if (benchMultiplierMatcher.find()) {
            commands.add(new ConditionalDamageMultiplierCommand(0, Integer.parseInt(benchMultiplierMatcher.group(1)), "BENCHED_POKEMON", null));
        }
        if (lowerText.contains("if your opponent's active pok") && lowerText.contains("is a grass pok") && lowerText.contains("20 more damage")) {
            commands.add(new ConditionalDamageMultiplierCommand(0, 20, "OPPONENT_TYPE", "Grass"));
        }
        if (lowerText.contains("already has any damage counters on it") && lowerText.contains("more damage")) {
            Matcher m = Pattern.compile("does (\\d+) more damage", Pattern.CASE_INSENSITIVE).matcher(text);
            if (m.find()) {
                commands.add(new ConditionalDamageMultiplierCommand(0, Integer.parseInt(m.group(1)), "OPPONENT_HAS_DAMAGE_COUNTERS", null));
            }
        }
        Matcher typedEnergyDamageMatcher = Pattern
                .compile("does (\\d+) more damage for each ([a-zA-Z]+) energy attached to this pok", Pattern.CASE_INSENSITIVE)
                .matcher(text);
        if (typedEnergyDamageMatcher.find()) {
            commands.add(new AddDamageByAttachedEnergyCommand(
                    typedEnergyDamageMatcher.group(2),
                    Integer.parseInt(typedEnergyDamageMatcher.group(1)),
                    false
            ));
        }
        Matcher allEnergyDamageMatcher = Pattern
                .compile("does (\\d+) more damage times the amount of energy attached to both active pok", Pattern.CASE_INSENSITIVE)
                .matcher(text);
        if (allEnergyDamageMatcher.find()) {
            commands.add(new AddDamageByAttachedEnergyCommand(
                    null,
                    Integer.parseInt(allEnergyDamageMatcher.group(1)),
                    true
            ));
        }
        Matcher selfCounterDamageMatcher = Pattern
                .compile("does (\\d+) more damage for each damage counter on this pok", Pattern.CASE_INSENSITIVE)
                .matcher(text);
        if (selfCounterDamageMatcher.find()) {
            commands.add(new AddDamageByDamageCountersCommand(Target.SELF, Integer.parseInt(selfCounterDamageMatcher.group(1))));
        }
        Matcher opponentCounterDamageMatcher = Pattern
                .compile("does (\\d+) more damage for each damage counter on your opponent's active pok", Pattern.CASE_INSENSITIVE)
                .matcher(text);
        if (opponentCounterDamageMatcher.find()) {
            commands.add(new AddDamageByDamageCountersCommand(Target.OPPONENT, Integer.parseInt(opponentCounterDamageMatcher.group(1))));
        }

        // Restricciones de ataque para el siguiente turno
        if (lowerText.contains("can't attack during your next turn") || lowerText.contains("cannot attack during your next turn")) {
            if (lowerText.contains("flip a coin") && lowerText.contains("if tails")) {
                commands.add(new CoinFlipCommand(null, new SetNoPuedeAtacarSiguienteTurnoCommand(Target.SELF)));
            } else {
                commands.add(new SetNoPuedeAtacarSiguienteTurnoCommand(Target.SELF));
            }
        }

        if (lowerText.contains("defending pok") && (lowerText.contains("can't attack during your opponent's next turn") || lowerText.contains("cannot attack during your opponent's next turn"))) {
            commands.add(new SetCannotAttackDefendingCommand());
        }

        Pattern blockAttackPattern = Pattern.compile("this pok.{1,2}mon can't use ([a-zA-Z'\\s-]+) during your next turn", Pattern.CASE_INSENSITIVE);
        Matcher blockAttackMatcher = blockAttackPattern.matcher(text);
        if (blockAttackMatcher.find()) {
            commands.add(new BlockAttackNextTurnCommand(blockAttackMatcher.group(1).trim(), Target.SELF));
        }

        Pattern buffAttackPattern = Pattern.compile("during your next turn, this pok.{1,2}mon's ([a-zA-Z'\\s-]+) attack does (\\d+) more damage", Pattern.CASE_INSENSITIVE);
        Matcher buffAttackMatcher = buffAttackPattern.matcher(text);
        if (buffAttackMatcher.find()) {
            commands.add(new AtaquePotenciadoSiguienteTurnoCommand(buffAttackMatcher.group(1).trim(), Integer.parseInt(buffAttackMatcher.group(2))));
        }

        if (lowerText.contains("prevent all damage done to this pok") && !lowerText.contains("if heads")) {
            commands.add(new SetInvulnerableCommand());
        }

        Pattern preventThresholdPattern = Pattern.compile("prevent that attack's damage done to this pok.{1,2}mon if that damage is (\\d+) or less", Pattern.CASE_INSENSITIVE);
        Matcher preventThresholdMatcher = preventThresholdPattern.matcher(text);
        if (preventThresholdMatcher.find()) {
            int amount = Integer.parseInt(preventThresholdMatcher.group(1));
            commands.add(new SetPreventDamageThresholdCommand(amount));
        }

        // Escalado de daño según la banca
        Pattern benchDamageMultiplierPattern = Pattern.compile("does (\\d+) damage times the number of your benched", Pattern.CASE_INSENSITIVE);
        Matcher benchDamageMultiplierMatcher = benchDamageMultiplierPattern.matcher(text);
        if (benchDamageMultiplierMatcher.find()) {
            int multiplier = Integer.parseInt(benchDamageMultiplierMatcher.group(1));
            commands.add(new ConditionalDamageMultiplierCommand(0, multiplier, "BENCHED_POKEMON", null));
        }

        // Daño a la propia banca
        Pattern selfBenchDamagePattern = Pattern.compile("does (\\d+) damage to each of your benched", Pattern.CASE_INSENSITIVE);
        Matcher selfBenchDamageMatcher = selfBenchDamagePattern.matcher(text);
        if (selfBenchDamageMatcher.find()) {
            int amount = Integer.parseInt(selfBenchDamageMatcher.group(1));
            commands.add(new SelfBenchDamageCommand(amount));
        }

        // Ya procesado arriba

        if (lowerText.contains("search your deck for 3 different types of basic energy cards") || lowerText.contains("search your deck for up to 3 basic energy cards")) {
            commands.add(new SearchDeckCommand(
                    "Energy", null, null, "HAND", 3,
                    "Elegí hasta 3 cartas de Energía para poner en tu mano."
            ));
        }

        if (lowerText.contains("you may do") && lowerText.contains("more damage") && lowerText.contains("if you do, this pok") && lowerText.contains("is now asleep")) {
            Matcher m = Pattern.compile("you may do (\\d+) more damage", Pattern.CASE_INSENSITIVE).matcher(text);
            if (m.find()) {
                commands.add(new DamageCommand(Integer.parseInt(m.group(1))));
                commands.add(new ApplyStatusConditionCommand("Asleep", Target.SELF));
            }
        }

        // Descarte de cartas del tope del mazo e interacciones relacionadas (Rhyhorn, Gurdurr, Rhydon, Magcargo)
        if (lowerText.contains("discard the top card of your deck") && lowerText.contains("fighting energy")) {
            commands.add(new DiscardTopDeckAttachEnergyCommand("Fighting"));
        }
        if (lowerText.contains("discard the top card of your deck") && lowerText.contains("fire energy")) {
            commands.add(new MagcargoMagmaMantleCommand(extraParams));
        }

        // Simisage's Torment
        if (lowerText.contains("choose 1 of your opponent's active") && lowerText.contains("attacks") && lowerText.contains("can't use that attack")) {
            commands.add(new TormentBlockAttackCommand(extraParams));
        }

        // Gogoat's Charge Dash
        if (lowerText.contains("you may do 20 more damage") && lowerText.contains("if you do, this pok") && lowerText.contains("does 20 damage to itself")) {
            commands.add(new GogoatChargeDashCommand(extraParams));
        }

        // Simisear's Flamethrower
        if (lowerText.contains("discard a fire energy attached to this pok")) {
            commands.add(new DiscardAttachedEnergyOfTypeCommand("Fire", Target.SELF));
        }

        // Talonflame's Devastating Wind
        Pattern opponentShuffleHandDrawPattern = Pattern.compile("your opponent shuffles (?:his or her|their) hand into (?:his or her|their) deck and draws (\\d+) cards", Pattern.CASE_INSENSITIVE);
        Matcher opponentShuffleHandDrawMatcher = opponentShuffleHandDrawPattern.matcher(text);
        if (opponentShuffleHandDrawMatcher.find()) {
            int amount = Integer.parseInt(opponentShuffleHandDrawMatcher.group(1));
            commands.add(new OpponentShuffleHandDrawCommand(amount));
        }

        // Talonflame's Flare Blitz
        if (lowerText.contains("discard all fire energy attached to this pok")) {
            commands.add(new DiscardAttachedEnergyOfTypeCommand("Fire", Target.SELF, true));
        }

        if (lowerText.contains("discard the top card of your opponent's deck") && !lowerText.contains("damage counter")) {
            commands.add(new DiscardTopDeckCommand(Target.OPPONENT, 1));
        }
        if (lowerText.contains("discard the top card of your opponent's deck") && lowerText.contains("damage counter")) {
            commands.add(new RhydonMadMountainCommand());
        }

        // Vistazo y reordenación del tope del mazo (Braixen Clairvoyant Eye, etc.)
        if (lowerText.contains("look at the top 3 cards of your deck") && lowerText.contains("put them back on top")) {
            commands.add(new PeekTopDeckCommand(3,
                    "Mirá las 3 cartas del tope de tu mazo. Seleccionalas en el orden que quieras que queden (la primera elegida quedará arriba)."));
        }

        if (lowerText.contains("switch 1 of your opponent's benched") && lowerText.contains("with your opponent's active")) {
            commands.add(new SwitchOpponentActiveCommand());
        }

        // Starmie's Recover
        if (lowerText.contains("discard an energy attached to this pokemon and heal all damage from it")
                || (lowerText.contains("discard") && lowerText.contains("energy attached to this pok") && lowerText.contains("heal all damage"))) {
            commands.add(new DiscardAttachedEnergyOfTypeCommand("Any", Target.SELF));
            commands.add(new HealCommand(-1, Target.SELF));
        }

        // Starmie's Core Splash
        if (lowerText.contains("has any psychic energy attached to it") && lowerText.contains("does 30 more damage")) {
            commands.add(new ConditionalDamageMultiplierCommand(0, 30, "HAS_ENERGY_TYPE", "Psychic"));
        }

        // Lapras's Seafaring
        if (lowerText.contains("attach a water energy card from your discard pile to your benched pok") && lowerText.contains("heads")) {
            commands.add(new LaprasSeafaringCommand(extraParams));
        }


        // Corsola's Refresh
        if (lowerText.contains("heal 30 damage and remove all special conditions from this pok")) {
            commands.add(new CorsolaRefreshCommand());
        }

        // Raichu's Thunderbolt
        if (lowerText.contains("discard all energy attached to this pok")) {
            commands.add(new DiscardAttachedEnergyOfTypeCommand("Any", Target.SELF, true));
        }

        // Froakie's Bounce
        if (lowerText.contains("flip a coin. if heads, switch this pok") && lowerText.contains("benched pok")) {
            commands.add(new FroakieBounceCommand());
        }

        return commands;
    }

    private String normalizeQuotes(String text) {
        if (text == null) return "";
        return text
                .replace("’", "'")
                .replace("‘", "'")
                .replace("´", "'")
                .replace("`", "'");
    }
}
