package com.pokemon.tcg.model.battle.command;

import com.pokemon.tcg.model.battle.Partida;
import com.pokemon.tcg.model.battle.TableroJugador;

public class ConditionalDamageMultiplierCommand implements BattleCommand {
    private int baseDamage;
    private int multiplier;
    private String conditionType; // e.g. "PRIZE_CARDS", "OPPONENT_TYPE"
    private String conditionValue;

    public ConditionalDamageMultiplierCommand(int baseDamage, int multiplier, String conditionType, String conditionValue) {
        this.baseDamage = baseDamage;
        this.multiplier = multiplier;
        this.conditionType = conditionType;
        this.conditionValue = conditionValue;
    }

    @Override
    public void execute(Partida partida, TableroJugador atacante, TableroJugador defensor) {
        int finalDamage = baseDamage;

        if ("PRIZE_CARDS".equals(conditionType)) {
            finalDamage += (atacante.getPremios().size() * multiplier);
        } else if ("OPPONENT_TYPE".equals(conditionType)) {
            if (defensor.getActivo() != null && defensor.getActivo().getCard().getTipo().equalsIgnoreCase(conditionValue)) {
                finalDamage += multiplier;
            }
        } else if ("BENCHED_POKEMON".equals(conditionType)) {
            finalDamage += (atacante.getBanca().size() * multiplier);
        }

        if (finalDamage > 0 && defensor.getActivo() != null) {
            int currentHp = defensor.getActivo().getHpActual();
            defensor.getActivo().setHpActual(Math.max(0, currentHp - finalDamage));
        }
    }
}
