package com.pokemon.tcg.service.battle.strategy;

import com.pokemon.tcg.model.battle.Partida;

// Placeholder — estrategia avanzada para implementar en el futuro.
// Para activarla: inyectar en BotAIService en lugar de EstrategiaBasica.
public class EstrategiaDificil implements EstrategiaBot {

    @Override
    public void ejecutarSetup(Partida partida) {
        new EstrategiaBasica().ejecutarSetup(partida);
    }

    @Override
    public void ejecutarTurno(Partida partida) {
        throw new UnsupportedOperationException("EstrategiaDificil aún no implementada.");
    }
}
