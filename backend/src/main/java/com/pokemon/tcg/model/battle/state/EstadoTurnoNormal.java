package com.pokemon.tcg.model.battle.state;

import com.pokemon.tcg.model.battle.Partida;

public class EstadoTurnoNormal implements EstadoPartida {
    @Override public boolean permiteAccionesDeJuego() { return true; }
    @Override public Partida.Fase getFase() { return Partida.Fase.TURNO_NORMAL; }
}
