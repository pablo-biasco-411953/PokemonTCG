package com.pokemon.tcg.service.battle.strategy;

import com.pokemon.tcg.model.battle.Partida;

public interface EstrategiaBot {
    void ejecutarTurno(Partida partida);
    void ejecutarSetup(Partida partida);
}
