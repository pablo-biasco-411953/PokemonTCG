package com.pokemon.tcg.service.battle.command;

import com.pokemon.tcg.model.battle.command.*;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class AttackEffectParserService {

    private static final Pattern HEAL_PATTERN = Pattern.compile("Heal (\\d+) damage from this Pokémon", Pattern.CASE_INSENSITIVE);
    private static final Pattern COIN_FLIP_DAMAGE_PATTERN = Pattern.compile("Flip a coin\\. If heads, this attack does (\\d+) more damage", Pattern.CASE_INSENSITIVE);
    private static final Pattern DRAW_CARD_PATTERN = Pattern.compile("Draw (\\d+) card", Pattern.CASE_INSENSITIVE);
    private static final Pattern APPLY_STATUS_PATTERN = Pattern.compile("The Defending Pokémon is now (Poisoned|Asleep|Burned|Paralyzed|Confused)", Pattern.CASE_INSENSITIVE);

    public List<BattleCommand> parseEffects(String text) {
        List<BattleCommand> commands = new ArrayList<>();
        if (text == null || text.trim().isEmpty()) {
            return commands;
        }

        // Heal
        Matcher healMatcher = HEAL_PATTERN.matcher(text);
        if (healMatcher.find()) {
            int amount = Integer.parseInt(healMatcher.group(1));
            commands.add(new HealCommand(amount, Target.SELF));
        }

        // Coin Flip Damage
        Matcher coinFlipMatcher = COIN_FLIP_DAMAGE_PATTERN.matcher(text);
        if (coinFlipMatcher.find()) {
            int extraDamage = Integer.parseInt(coinFlipMatcher.group(1));
            commands.add(new CoinFlipCommand(new DamageCommand(extraDamage)));
        }

        // Draw cards
        Matcher drawMatcher = DRAW_CARD_PATTERN.matcher(text);
        if (drawMatcher.find()) {
            int cards = Integer.parseInt(drawMatcher.group(1));
            commands.add(new DrawCardCommand(cards, Target.SELF));
        }

        // Status
        Matcher statusMatcher = APPLY_STATUS_PATTERN.matcher(text);
        if (statusMatcher.find()) {
            String status = statusMatcher.group(1);
            commands.add(new ApplyStatusConditionCommand(status, Target.OPPONENT));
        }
        // Complex: Discard Energy
        if (text.toLowerCase().contains("discard an energy attached to this pokémon")) {
            commands.add(new DiscardEnergyCommand(1, Target.SELF));
        }

        // Complex: Search Deck for Basic Pokemon
        if (text.toLowerCase().contains("search your deck for a basic pokémon") || text.toLowerCase().contains("search your deck for a grass pokémon")) {
            commands.add(new SearchDeckCommand("Pokemon", "Basic", Target.OPPONENT)); // Using OPPONENT to mean Bench for now
        }

        // Complex: Move Energy
        if (text.toLowerCase().contains("move a basic energy from this pokémon to 1 of your benched pokémon") || text.toLowerCase().contains("move as many")) {
            commands.add(new MoveEnergyCommand(null, 1)); // Simplified logic
        }

        // Complex: Conditional Damage
        if (text.toLowerCase().contains("times the number of your remaining prize cards")) {
            commands.add(new ConditionalDamageMultiplierCommand(0, 20, "PRIZE_CARDS", null));
        }
        if (text.toLowerCase().contains("if your opponent's active pokémon is a grass pokémon, this attack does 20 more damage")) {
            commands.add(new ConditionalDamageMultiplierCommand(0, 20, "OPPONENT_TYPE", "Grass"));
        }

        return commands;
    }
}
