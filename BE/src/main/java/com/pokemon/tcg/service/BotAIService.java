package com.pokemon.tcg.service;

import com.pokemon.tcg.model.battle.Partida;
import com.pokemon.tcg.service.battle.strategy.EstrategiaBasica;
import com.pokemon.tcg.service.battle.strategy.EstrategiaBot;
import org.springframework.stereotype.Service;

@Service
public class BotAIService {

    private final EstrategiaBot estrategia = new EstrategiaBasica();

    public void ejecutarTurno(Partida partida) {
        estrategia.ejecutarTurno(partida);
    }

    public void ejecutarSetup(Partida partida) {
        estrategia.ejecutarSetup(partida);
    }
}
