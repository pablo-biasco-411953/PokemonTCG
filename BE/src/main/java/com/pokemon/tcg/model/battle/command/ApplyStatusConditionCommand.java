package com.pokemon.tcg.model.battle.command;

import com.pokemon.tcg.model.battle.Partida;
import com.pokemon.tcg.model.battle.TableroJugador;

public class ApplyStatusConditionCommand implements BattleCommand {
    private String condition;
    private Target target;

    public ApplyStatusConditionCommand(String condition, Target target) {
        this.condition = condition;
        this.target = target;
    }

    public String getCondition() {
        return condition;
    }

    public Target getTarget() {
        return target;
    }

    @Override
    public void execute(Partida partida, TableroJugador atacante, TableroJugador defensor) {
        TableroJugador targetBoard = (target == Target.SELF) ? atacante : defensor;
        if (targetBoard.getActivo() != null) {
            targetBoard.getActivo().agregarCondicion(condition);
        }
    }
}
