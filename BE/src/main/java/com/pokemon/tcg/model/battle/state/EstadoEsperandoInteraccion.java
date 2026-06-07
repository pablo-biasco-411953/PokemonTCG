package com.pokemon.tcg.model.battle.state;

import com.pokemon.tcg.model.battle.Partida;

public class EstadoEsperandoInteraccion implements EstadoPartida {
    @Override
    public Partida.Fase getFase() {
        return Partida.Fase.ESPERANDO_INTERACCION;
    }


    @Override
    public boolean permiteAccionesDeJuego() {
        return false;
    }
}
