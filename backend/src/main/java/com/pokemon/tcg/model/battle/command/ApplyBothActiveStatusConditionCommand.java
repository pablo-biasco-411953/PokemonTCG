package com.pokemon.tcg.model.battle.command;

import com.pokemon.tcg.model.battle.Partida;
import com.pokemon.tcg.model.battle.TableroJugador;

public class ApplyBothActiveStatusConditionCommand implements BattleCommand {
    private final String condition;

    public ApplyBothActiveStatusConditionCommand(String condition) {
        this.condition = condition;
    }

    @Override
    public void execute(Partida partida, TableroJugador atacante, TableroJugador defensor) {
        if (atacante.getActivo() != null) {
            atacante.getActivo().agregarCondicion(condition);
        }
        if (defensor.getActivo() != null) {
            defensor.getActivo().agregarCondicion(condition);
        }
    }
}
