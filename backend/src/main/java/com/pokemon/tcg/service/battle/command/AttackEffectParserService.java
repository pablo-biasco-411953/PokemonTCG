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
    private static final Pattern APPLY_STATUS_PATTERN = Pattern.compile("(?:the )?Defending Pok.{1,2}mon is now (Poisoned|Asleep|Burned|Paralyzed|Confused)", Pattern.CASE_INSENSITIVE);
    private static final Pattern SELF_DAMAGE_PATTERN = Pattern.compile("does (\\d+) damage to itself", Pattern.CASE_INSENSITIVE);

    public List<BattleCommand> parseEffects(String text) {
        List<BattleCommand> commands = new ArrayList<>();
        if (text == null || text.trim().isEmpty()) {
            return commands;
        }

        Matcher healMatcher = HEAL_PATTERN.matcher(text);
        if (healMatcher.find()) {
            int amount = Integer.parseInt(healMatcher.group(1));
            commands.add(new HealCommand(amount, Target.SELF));
        }

        Matcher coinFlipMatcher = COIN_FLIP_DAMAGE_PATTERN.matcher(text);
        if (coinFlipMatcher.find()) {
            int extraDamage = Integer.parseInt(coinFlipMatcher.group(1));
            commands.add(new CoinFlipCommand(new DamageCommand(extraDamage)));
        }

        Matcher drawMatcher = DRAW_CARD_PATTERN.matcher(text);
        if (drawMatcher.find()) {
            int cards = drawMatcher.group(1) == null ? 1 : Integer.parseInt(drawMatcher.group(1));
            commands.add(new DrawCardCommand(cards, Target.SELF));
        }

        Matcher statusMatcher = APPLY_STATUS_PATTERN.matcher(text);
        if (statusMatcher.find()) {
            ApplyStatusConditionCommand statusCommand =
                    new ApplyStatusConditionCommand(statusMatcher.group(1), Target.OPPONENT);
            String lowerText = text.toLowerCase();
            if (lowerText.contains("flip a coin") && lowerText.contains("if heads")) {
                commands.add(new CoinFlipCommand(statusCommand));
            } else {
                commands.add(statusCommand);
            }
        }

        Matcher selfDamageMatcher = SELF_DAMAGE_PATTERN.matcher(text);
        if (selfDamageMatcher.find()) {
            int amount = Integer.parseInt(selfDamageMatcher.group(1));
            commands.add(new SelfDamageCommand(amount));
        }

        String lowerText = text.toLowerCase();
        if (lowerText.contains("discard an energy attached to this pok")) {
            commands.add(new DiscardEnergyCommand(1, Target.SELF));
        }

        if (lowerText.contains("search your deck for a basic pok") || lowerText.contains("search your deck for a grass pok")) {
            commands.add(new SearchDeckCommand("Pokemon", "Basic", Target.OPPONENT));
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
