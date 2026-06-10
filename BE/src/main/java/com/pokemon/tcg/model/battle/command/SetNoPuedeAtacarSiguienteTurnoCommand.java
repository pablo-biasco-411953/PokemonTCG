package com.pokemon.tcg.model.battle.command;

import com.pokemon.tcg.model.battle.Partida;
import com.pokemon.tcg.model.battle.TableroJugador;

public class SetNoPuedeAtacarSiguienteTurnoCommand implements BattleCommand {
    private final Target target;

    public SetNoPuedeAtacarSiguienteTurnoCommand(Target target) {
        this.target = target;
    }

    @Override
    public void execute(Partida partida, TableroJugador atacante, TableroJugador defensor) {
        TableroJugador affectedBoard = (target == Target.SELF) ? atacante : defensor;
        if (affectedBoard.getActivo() != null) {
            affectedBoard.getActivo().setNoPuedeAtacarSiguienteTurno(true);
            affectedBoard.getActivo().setNoPuedeAtacarYaConsumido(false);
        }
    }
}
