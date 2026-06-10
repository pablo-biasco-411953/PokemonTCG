package com.pokemon.tcg.model.battle.command;

import com.pokemon.tcg.model.battle.Partida;
import com.pokemon.tcg.model.battle.TableroJugador;

public class SetAttackBlockNextTurnCommand implements BattleCommand {
    private final Target target;

    public SetAttackBlockNextTurnCommand(Target target) {
        this.target = target;
    }

    @Override
    public void execute(Partida partida, TableroJugador atacante, TableroJugador defensor) {
        TableroJugador affectedBoard = target == Target.SELF ? atacante : defensor;
        if (affectedBoard.getActivo() == null) {
            return;
        }

        affectedBoard.getActivo().setDebeLanzarMonedaSiAtaca(true);
    }
}
