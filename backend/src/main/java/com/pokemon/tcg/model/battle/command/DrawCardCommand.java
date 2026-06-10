package com.pokemon.tcg.model.battle.command;

import com.pokemon.tcg.model.battle.Partida;
import com.pokemon.tcg.model.battle.TableroJugador;

public class DrawCardCommand implements BattleCommand {
    private int amount;
    private Target target;

    public DrawCardCommand(int amount, Target target) {
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
        for (int i = 0; i < amount; i++) {
            if (!targetBoard.getMazo().isEmpty()) {
                targetBoard.getMano().add(targetBoard.getMazo().remove(0));
            }
        }
    }
}
