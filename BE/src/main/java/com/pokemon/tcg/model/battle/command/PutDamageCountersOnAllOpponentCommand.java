package com.pokemon.tcg.model.battle.command;

import com.pokemon.tcg.model.battle.CartaEnJuego;
import com.pokemon.tcg.model.battle.Partida;
import com.pokemon.tcg.model.battle.TableroJugador;

public class PutDamageCountersOnAllOpponentCommand implements BattleCommand {
    private final int counters;

    public PutDamageCountersOnAllOpponentCommand(int counters) {
        this.counters = counters;
    }

    @Override
    public void execute(Partida partida, TableroJugador atacante, TableroJugador defensor) {
        int damage = counters * 10;
        if (defensor.getActivo() != null) {
            defensor.getActivo().setHpActual(Math.max(0, defensor.getActivo().getHpActual() - damage));
        }
        for (CartaEnJuego benched : defensor.getBanca()) {
            if (benched != null) {
                benched.setHpActual(Math.max(0, benched.getHpActual() - damage));
            }
        }
    }
}
