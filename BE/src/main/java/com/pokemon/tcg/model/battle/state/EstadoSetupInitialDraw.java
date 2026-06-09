package com.pokemon.tcg.model.battle.state;

import com.pokemon.tcg.model.battle.Partida;

public class EstadoSetupInitialDraw implements EstadoPartida {
    @Override public boolean permiteAccionesDeJuego() { return false; }
    @Override public Partida.Fase getFase() { return Partida.Fase.SETUP_INITIAL_DRAW; }
}
