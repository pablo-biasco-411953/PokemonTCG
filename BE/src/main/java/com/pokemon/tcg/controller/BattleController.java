package com.pokemon.tcg.controller;

import com.pokemon.tcg.model.battle.Partida;
import com.pokemon.tcg.service.BattleEngineService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/battle")
public class BattleController {
    private final BattleEngineService battleEngine;

    public BattleController(BattleEngineService battleEngine) {
        this.battleEngine = battleEngine;
    }

    @PostMapping("/start/{username}")
    public ResponseEntity<Partida> startBattle(@PathVariable String username, @RequestBody StartBattleRequest request) {
        Partida partida = battleEngine.startBattle(username, request.getMazoId());
        return ResponseEntity.ok(partida);
    }
    
    @PostMapping("/{matchId}/coin-flip")
    public ResponseEntity<Boolean> lanzarMoneda(@PathVariable String matchId) {
        boolean jugadorGana = battleEngine.lanzarMoneda(matchId);
        return ResponseEntity.ok(jugadorGana);
    }
    
    @PostMapping("/{matchId}/choose-turn")
    public ResponseEntity<Void> elegirTurno(@PathVariable String matchId, @RequestBody ChooseTurnRequest request) {
        battleEngine.elegirTurno(matchId, request.isVaPrimero());
        return ResponseEntity.ok().build();
    }
    
    @GetMapping("/{matchId}/state")
    public ResponseEntity<Partida> getEstadoPartida(@PathVariable String matchId) {
        Partida partida = battleEngine.getEstadoPartida(matchId);
        if (partida == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(partida);
    }

    public static class StartBattleRequest {
        private Long mazoId;

        public Long getMazoId() {
            return mazoId;
        }

        public void setMazoId(Long mazoId) {
            this.mazoId = mazoId;
        }
    }
    
    public static class ChooseTurnRequest {
        private boolean vaPrimero;

        public boolean isVaPrimero() {
            return vaPrimero;
        }

        public void setVaPrimero(boolean vaPrimero) {
            this.vaPrimero = vaPrimero;
        }
    }
}
