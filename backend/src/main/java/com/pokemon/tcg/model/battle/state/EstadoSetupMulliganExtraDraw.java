package com.pokemon.tcg.model.battle.state;

import com.pokemon.tcg.model.battle.Partida;

public class EstadoSetupMulliganExtraDraw implements EstadoPartida {
    @Override public boolean permiteAccionesDeJuego() { return false; }
    @Override public Partida.Fase getFase() { return Partida.Fase.SETUP_MULLIGAN_EXTRA_DRAW; }
}
