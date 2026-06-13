package com.pokemon.tcg.model.battle.command;

import com.pokemon.tcg.model.battle.Partida;
import com.pokemon.tcg.model.battle.TableroJugador;

public class SetInvulnerableCommand implements BattleCommand {
    @Override
    public void execute(Partida partida, TableroJugador atacante, TableroJugador defensor) {
        if (atacante.getActivo() != null) {
            atacante.getActivo().setInvulnerable(true);
        }
    }
}
