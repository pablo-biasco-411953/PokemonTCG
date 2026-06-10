package com.pokemon.tcg.model.battle.command;

import com.pokemon.tcg.model.battle.Partida;
import com.pokemon.tcg.model.battle.TableroJugador;

public class SetCannotAttackDefendingCommand implements BattleCommand {
    @Override
    public void execute(Partida partida, TableroJugador atacante, TableroJugador defensor) {
        if (defensor.getActivo() != null) {
            defensor.getActivo().setPuedeAtacar(false);
        }
    }
}
