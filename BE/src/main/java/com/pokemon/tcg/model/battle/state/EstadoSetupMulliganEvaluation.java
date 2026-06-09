package com.pokemon.tcg.model.battle.state;

import com.pokemon.tcg.model.battle.Partida;

public class EstadoSetupMulliganEvaluation implements EstadoPartida {
    @Override public boolean permiteAccionesDeJuego() { return false; }
    @Override public Partida.Fase getFase() { return Partida.Fase.SETUP_MULLIGAN_EVALUATION; }
}
