package com.pokemon.tcg.model.battle.command;

import com.pokemon.tcg.model.battle.Partida;
import com.pokemon.tcg.model.battle.TableroJugador;

public class ReduceNextTurnDamageDealtCommand implements BattleCommand {
    private final int amount;

    public ReduceNextTurnDamageDealtCommand(int amount) {
        this.amount = amount;
    }

    @Override
    public void execute(Partida partida, TableroJugador atacante, TableroJugador defensor) {
        if (defensor.getActivo() != null) {
            defensor.getActivo().setReduccionDanioCausadoSiguienteTurno(amount);
        }
    }
}
