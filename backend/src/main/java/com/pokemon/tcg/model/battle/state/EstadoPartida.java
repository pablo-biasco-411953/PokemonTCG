package com.pokemon.tcg.model.battle.state;

import com.pokemon.tcg.model.battle.Partida;

public interface EstadoPartida {
    boolean permiteAccionesDeJuego();
    Partida.Fase getFase();
}
