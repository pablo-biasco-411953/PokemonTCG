package com.pokemon.tcg.model.battle.command;

import com.pokemon.tcg.model.battle.Partida;
import com.pokemon.tcg.model.battle.TableroJugador;

public class DamageCommand implements BattleCommand {
    private int amount;

    public DamageCommand(int amount) {
        this.amount = amount;
    }

    public int getAmount() {
        return amount;
    }

    @Override
    public void execute(Partida partida, TableroJugador atacante, TableroJugador defensor) {
        if (defensor.getActivo() != null) {
            int currentHp = defensor.getActivo().getHpActual();
            defensor.getActivo().setHpActual(Math.max(0, currentHp - amount));
        }
    }
}
