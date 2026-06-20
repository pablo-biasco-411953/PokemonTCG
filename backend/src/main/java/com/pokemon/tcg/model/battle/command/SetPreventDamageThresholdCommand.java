package com.pokemon.tcg.model.battle.command;

import com.pokemon.tcg.model.battle.Partida;
import com.pokemon.tcg.model.battle.TableroJugador;

public class SetPreventDamageThresholdCommand implements BattleCommand {
    private final int threshold;

    public SetPreventDamageThresholdCommand(int threshold) {
        this.threshold = threshold;
    }

    public int getThreshold() {
        return threshold;
    }

    @Override
    public void execute(Partida partida, TableroJugador atacante, TableroJugador defensor) {
        if (atacante.getActivo() != null) {
            atacante.getActivo().setPreventDamageThreshold(threshold);
            atacante.getActivo().setPreventDamageThresholdYaConsumido(false);
        }
    }
}
