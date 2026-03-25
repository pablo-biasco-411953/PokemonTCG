package com.pokemon.tcg.controller;

import com.pokemon.tcg.model.battle.Partida;
import com.pokemon.tcg.service.BattleEngineService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/battle")
@CrossOrigin(origins = "http://localhost:4200") // <--- FUNDAMENTAL PARA ANGULAR
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

    // Cambiamos el orden para que coincida con el frontend: /state/{id}
    @GetMapping("/state/{matchId}")
    public ResponseEntity<Partida> getEstadoPartida(@PathVariable String matchId) {
        Partida partida = battleEngine.getEstadoPartida(matchId);
        if (partida == null) {
            return ResponseEntity.notFound().build();
        }
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

    @PostMapping("/{matchId}/play-pokemon")
    public ResponseEntity<Void> jugarPokemon(@PathVariable String matchId, @RequestBody JugarPokemonRequest request) {
        battleEngine.jugarPokemon(matchId, request.getCartaId(), request.getPosicion());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{matchId}/attach-energy")
    public ResponseEntity<Void> unirEnergia(@PathVariable String matchId, @RequestBody UnirEnergiaRequest request) {
        battleEngine.unirEnergia(matchId, request.getCartaId(), request.getEnergiaId());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{matchId}/pass-turn")
    public ResponseEntity<Void> pasarTurno(@PathVariable String matchId) {
        battleEngine.pasarTurno(matchId);
        return ResponseEntity.ok().build();
    }

    // --- DTOs Internos ---
    public static class StartBattleRequest {
        private Long mazoId;
        public Long getMazoId() { return mazoId; }
        public void setMazoId(Long mazoId) { this.mazoId = mazoId; }
    }

    public static class ChooseTurnRequest {
        private boolean vaPrimero;
        public boolean isVaPrimero() { return vaPrimero; }
        public void setVaPrimero(boolean vaPrimero) { this.vaPrimero = vaPrimero; }
    }

    public static class JugarPokemonRequest {
        private String cartaId;
        private int posicion;
        public String getCartaId() { return cartaId; }
        public void setCartaId(String cartaId) { this.cartaId = cartaId; }
        public int getPosicion() { return posicion; }
        public void setPosicion(int posicion) { this.posicion = posicion; }
    }

    public static class UnirEnergiaRequest {
        private String cartaId;
        private String energiaId;
        public String getCartaId() { return cartaId; }
        public void setCartaId(String cartaId) { this.cartaId = cartaId; }
        public String getEnergiaId() { return energiaId; }
        public void setEnergiaId(String energiaId) { this.energiaId = energiaId; }
    }
}