package com.pokemon.tcg.model.battle.command;

import com.pokemon.tcg.model.battle.Partida;
import com.pokemon.tcg.model.battle.TableroJugador;

public class AtaquePotenciadoSiguienteTurnoCommand implements BattleCommand {
    private String ataquePotenciado;
    private int danioExtra;

    public AtaquePotenciadoSiguienteTurnoCommand(String ataquePotenciado, int danioExtra) {
        this.ataquePotenciado = ataquePotenciado;
        this.danioExtra = danioExtra;
    }

    @Override
    public void execute(Partida partida, TableroJugador atacante, TableroJugador defensor) {
        if (atacante.getActivo() != null) {
            atacante.getActivo().setAtaquePotenciadoSiguienteTurno(ataquePotenciado);
            atacante.getActivo().setDanioExtraSiguienteTurno(danioExtra);
        }
    }
}
