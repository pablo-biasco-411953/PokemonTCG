package com.pokemon.tcg.model.battle.state;

import com.pokemon.tcg.model.battle.Partida;

public class EstadoSetupPrizePlacement implements EstadoPartida {
    @Override public boolean permiteAccionesDeJuego() { return false; }
    @Override public Partida.Fase getFase() { return Partida.Fase.SETUP_PRIZE_PLACEMENT; }
}
