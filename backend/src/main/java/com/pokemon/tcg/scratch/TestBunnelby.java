package com.pokemon.tcg.scratch;

import com.pokemon.tcg.service.battle.command.AttackEffectParserService;
import com.pokemon.tcg.model.battle.command.BattleCommand;
import java.util.List;

public class TestBunnelby {
    public static void main(String[] args) {
        AttackEffectParserService parser = new AttackEffectParserService();
        String text = "Flip a coin. If heads, prevent all effects of attacks, including damage, done to this Pokémon during your opponent's next turn.";
        for (BattleCommand cmd : parser.parseEffects(text, null)) {
            System.out.println(cmd.getClass().getSimpleName());
        }
    }
}
