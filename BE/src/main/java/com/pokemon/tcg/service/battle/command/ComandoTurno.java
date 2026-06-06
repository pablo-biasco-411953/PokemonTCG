package com.pokemon.tcg.service.battle.command;

import com.pokemon.tcg.model.battle.Partida;

public interface ComandoTurno {
    // Chequeo rápido de pre-condiciones — usado para feedback al FE o IA.
    boolean puedeEjecutar(Partida partida);

    // Ejecución completa con validación específica y cambios de estado.
    void ejecutar(Partida partida);

    String getNombre();
}
