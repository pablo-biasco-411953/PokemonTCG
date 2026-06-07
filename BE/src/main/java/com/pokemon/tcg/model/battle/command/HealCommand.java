package com.pokemon.tcg.model.battle.command;

import com.pokemon.tcg.model.battle.Partida;
import com.pokemon.tcg.model.battle.TableroJugador;

public class HealCommand implements BattleCommand {
    private int amount;
    private Target target;

    public HealCommand(int amount, Target target) {
        this.amount = amount;
        this.target = target;
    }

    public int getAmount() {
        return amount;
    }

    public Target getTarget() {
        return target;
    }

    @Override
    public void execute(Partida partida, TableroJugador atacante, TableroJugador defensor) {
        TableroJugador targetBoard = (target == Target.SELF) ? atacante : defensor;
        if (targetBoard.getActivo() != null) {
            int currentHp = targetBoard.getActivo().getHpActual();
            int maxHp = Integer.parseInt(targetBoard.getActivo().getCard().getHp()); // Or properly fetch max HP
            targetBoard.getActivo().setHpActual(Math.min(maxHp, currentHp + amount));
        }
    }
}
