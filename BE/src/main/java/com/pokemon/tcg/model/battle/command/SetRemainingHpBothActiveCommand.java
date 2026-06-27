package com.pokemon.tcg.model.battle.command;

import com.pokemon.tcg.model.battle.Partida;
import com.pokemon.tcg.model.battle.TableroJugador;

public class SetRemainingHpBothActiveCommand implements BattleCommand {
    private final int targetHp;

    public SetRemainingHpBothActiveCommand(int targetHp) {
        this.targetHp = targetHp;
    }

    @Override
    public void execute(Partida partida, TableroJugador atacante, TableroJugador defensor) {
        if (atacante.getActivo() != null) {
            if (atacante.getActivo().getHpActual() > targetHp) {
                atacante.getActivo().setHpActual(targetHp);
            }
        }
        if (defensor.getActivo() != null) {
            if (defensor.getActivo().getHpActual() > targetHp) {
                defensor.getActivo().setHpActual(targetHp);
            }
        }
    }
}
