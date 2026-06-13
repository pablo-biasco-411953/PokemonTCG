package com.pokemon.tcg.model.battle.command;

import com.pokemon.tcg.model.battle.Partida;
import com.pokemon.tcg.model.battle.TableroJugador;

public class BlockAttackNextTurnCommand implements BattleCommand {
    private final String attackName;
    private final Target target;

    public BlockAttackNextTurnCommand(String attackName, Target target) {
        this.attackName = attackName;
        this.target = target;
    }

    @Override
    public void execute(Partida partida, TableroJugador atacante, TableroJugador defensor) {
        TableroJugador affectedBoard = (target == Target.SELF) ? atacante : defensor;
        if (affectedBoard.getActivo() != null) {
            affectedBoard.getActivo().setAtaqueBloqueadoSiguienteTurno(attackName);
            affectedBoard.getActivo().setAtaqueBloqueadoYaConsumido(false);
        }
    }
}
