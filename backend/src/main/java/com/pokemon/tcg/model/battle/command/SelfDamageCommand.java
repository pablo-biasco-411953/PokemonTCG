package com.pokemon.tcg.model.battle.command;

import com.pokemon.tcg.model.battle.Partida;
import com.pokemon.tcg.model.battle.TableroJugador;

public class SelfDamageCommand implements BattleCommand {
    private final int amount;

    public SelfDamageCommand(int amount) {
        this.amount = amount;
    }

    public int getAmount() {
        return amount;
    }

    @Override
    public void execute(Partida partida, TableroJugador atacante, TableroJugador defensor) {
        if (atacante.getActivo() != null) {
            int currentHp = atacante.getActivo().getHpActual();
            atacante.getActivo().setHpActual(Math.max(0, currentHp - amount));
        }
    }
}
