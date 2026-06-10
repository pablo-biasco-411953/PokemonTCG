package com.pokemon.tcg.service;

import com.pokemon.tcg.model.battle.Partida;
import com.pokemon.tcg.service.battle.strategy.EstrategiaBasica;
import com.pokemon.tcg.service.battle.strategy.EstrategiaBot;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class BotAIService {

    private final EstrategiaBot estrategia;

    BotAIService() {
        this(new BattleAttackService(), new BattleKoService());
    }

    @Autowired
    public BotAIService(BattleAttackService battleAttackService, BattleKoService battleKoService) {
        this.estrategia = new EstrategiaBasica(battleAttackService, battleKoService);
    }

    public void ejecutarTurno(Partida partida) {
        estrategia.ejecutarTurno(partida);
    }

    public void ejecutarSetup(Partida partida) {
        estrategia.ejecutarSetup(partida);
    }
}
