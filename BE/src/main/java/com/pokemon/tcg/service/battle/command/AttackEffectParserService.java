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
    private static final Pattern APPLY_STATUS_PATTERN = Pattern.compile(
            "(?:(?:the )?Defending|your opponent's Active) Pok.{1,2}mon is now (Poisoned|Asleep|Burned|Paralyzed|Confused)",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern SELF_DAMAGE_PATTERN = Pattern.compile("does (\\d+) damage to itself", Pattern.CASE_INSENSITIVE);
    private static final Pattern FIXED_MULTI_COIN_DAMAGE_PATTERN = Pattern.compile(
            "Flip (\\d+) coins?\\. This attack does (\\d+) damage (?:times the number of|for each) heads",
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

    public List<BattleCommand> parseEffects(String text) {
        List<BattleCommand> commands = new ArrayList<>();
        if (text == null || text.trim().isEmpty()) {
            return commands;
        }
        String lowerText = text.toLowerCase();
        Matcher combinedCoinMatcher = COIN_DAMAGE_AND_STATUS_PATTERN.matcher(text);
        boolean combinedCoinHandled = combinedCoinMatcher.find();
        if (combinedCoinHandled) {
            commands.add(new CoinFlipCommand(new SequenceCommand(
                    new DamageCommand(Integer.parseInt(combinedCoinMatcher.group(1))),
                    new ApplyStatusConditionCommand(combinedCoinMatcher.group(2), Target.OPPONENT)
            )));
        }

        Matcher healMatcher = HEAL_PATTERN.matcher(text);
        if (healMatcher.find()) {
            int amount = Integer.parseInt(healMatcher.group(1));
            commands.add(new HealCommand(amount, Target.SELF));
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
        if (selfDamageMatcher.find()) {
            int amount = Integer.parseInt(selfDamageMatcher.group(1));
            if (lowerText.contains("if tails")) {
                commands.add(new CoinFlipCommand(null, new SelfDamageCommand(amount)));
            } else {
                commands.add(new SelfDamageCommand(amount));
            }
        }

        if (lowerText.contains("discard an energy attached to this pok")) {
            commands.add(new DiscardEnergyCommand(1, Target.SELF));
        }

        if (lowerText.contains("search your deck for a grass pok")) {
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
        } else if (lowerText.contains("search your deck for a lightning energy")) {
            commands.add(new SearchDeckCommand(
                    "Energy", null, "Lightning", "ATTACH_ACTIVE", 1,
                    "Elegí una Energía Eléctrica para unir a tu Pokémon Activo."
            ));
        } else if (lowerText.contains("search your deck for a card and put it into your hand")) {
            commands.add(new SearchDeckCommand(
                    null, null, null, "HAND", 1,
                    "Elegí una carta de tu mazo para poner en tu mano."
            ));
        }

        if (lowerText.contains("move a basic energy from this pok") || lowerText.contains("move as many")) {
            commands.add(new MoveEnergyCommand(null, 1));
        }

        if (lowerText.contains("times the number of your remaining prize cards")) {
            commands.add(new ConditionalDamageMultiplierCommand(0, 20, "PRIZE_CARDS", null));
        }
        if (lowerText.contains("if your opponent's active pok") && lowerText.contains("is a grass pok") && lowerText.contains("20 more damage")) {
            commands.add(new ConditionalDamageMultiplierCommand(0, 20, "OPPONENT_TYPE", "Grass"));
        }

        return commands;
    }
}
