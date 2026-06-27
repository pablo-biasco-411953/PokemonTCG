package com.pokemon.tcg.model.battle.command;

import com.pokemon.tcg.model.battle.CartaEnJuego;
import com.pokemon.tcg.model.battle.Partida;
import com.pokemon.tcg.model.battle.TableroJugador;

public class DamageOwnBenchedCommand implements BattleCommand {
    private final int amount;

    public DamageOwnBenchedCommand(int amount) {
        this.amount = amount;
    }

    @Override
    public void execute(Partida partida, TableroJugador atacante, TableroJugador defensor) {
        for (CartaEnJuego benched : atacante.getBanca()) {
            if (benched != null) {
                benched.setHpActual(Math.max(0, benched.getHpActual() - amount));
            }
        }
    }
}
