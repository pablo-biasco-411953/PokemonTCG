package com.pokemon.tcg.service.strategy;

import com.pokemon.tcg.model.battle.Partida;
import com.pokemon.tcg.model.battle.TableroJugador;

public interface EfectoTrainerStrategy {
    void ejecutar(Partida partida, TableroJugador jugador, Object target);
}
