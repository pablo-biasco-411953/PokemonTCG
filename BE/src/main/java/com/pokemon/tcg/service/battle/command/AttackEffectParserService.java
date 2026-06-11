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
        if (selfDamageMatcher.find()) {
            int amount = Integer.parseInt(selfDamageMatcher.group(1));
            if (lowerText.contains("if tails")) {
                commands.add(new CoinFlipCommand(null, new SelfDamageCommand(amount)));
            } else {
                commands.add(new SelfDamageCommand(amount));
            }
        }

        if (lowerText.contains("discard an energy attached to this pok") || lowerText.contains("discard 2 energy attached to this pok")) {
            int amount = lowerText.contains("discard 2 energy") ? 2 : 1;
            if (lowerText.contains("flip a coin") && lowerText.contains("if tails")) {
                commands.add(new CoinFlipCommand(null, new DiscardEnergyCommand(amount, Target.SELF)));
            } else {
                commands.add(new DiscardEnergyCommand(amount, Target.SELF));
            }
        }
        if (lowerText.contains("if heads, discard an energy attached to your opponent's active pok")) {
            commands.add(new CoinFlipCommand(new DiscardEnergyCommand(1, Target.OPPONENT)));
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
        }

        if (lowerText.contains("choose a random card from your opponent's hand") && lowerText.contains("shuffle")) {
            commands.add(new ShuffleRandomHandToDeckCommand());
        }

        if (lowerText.contains("move a basic energy from this pok") || lowerText.contains("move an energy from this pok") || lowerText.contains("move as many")) {
            commands.add(new MoveEnergyCommand(null, 1));
        }

        if (lowerText.contains("attach a darkness energy card from your discard pile to 1 of your benched")) {
            commands.add(new AttachEnergyFromDiscardToBenchCommand("Darkness", 1));
        }

        if (lowerText.contains("times the number of your remaining prize cards")) {
            commands.add(new ConditionalDamageMultiplierCommand(0, 20, "PRIZE_CARDS", null));
        }
        if (lowerText.contains("if your opponent's active pok") && lowerText.contains("is a grass pok") && lowerText.contains("20 more damage")) {
            commands.add(new ConditionalDamageMultiplierCommand(0, 20, "OPPONENT_TYPE", "Grass"));
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

        // Daño a la banca del oponente
        Pattern oppBenchDamagePattern = Pattern.compile("(?:does|this attack does) (\\d+) damage to (\\d+|1) of your opponent's benched", Pattern.CASE_INSENSITIVE);
        Matcher oppBenchDamageMatcher = oppBenchDamagePattern.matcher(text);
        if (oppBenchDamageMatcher.find()) {
            int amount = Integer.parseInt(oppBenchDamageMatcher.group(1));
            String countStr = oppBenchDamageMatcher.group(2);
            int count = "one".equalsIgnoreCase(countStr) || "1".equals(countStr) ? 1 : Integer.parseInt(countStr);
            commands.add(new DamageOpponentBenchedCommand(amount, count));
        }

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
            commands.add(new MagcargoMagmaMantleCommand());
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

        return commands;
    }
}
