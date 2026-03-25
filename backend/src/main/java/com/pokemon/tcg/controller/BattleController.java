package com.pokemon.tcg.controller;

import com.pokemon.tcg.model.battle.Partida;
import com.pokemon.tcg.service.BattleEngineService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/battle")
@CrossOrigin(origins = "http://localhost:4200")
public class BattleController {

    private final BattleEngineService battleEngine;

    public BattleController(BattleEngineService battleEngine) {
        this.battleEngine = battleEngine;
    }

    @PostMapping("/start/{username}")
    public ResponseEntity<?> startBattle(@PathVariable String username,
                                         @RequestBody StartBattleRequest request) {
        try {
            Partida partida = battleEngine.startBattle(username, request.getMazoId());
            return ResponseEntity.ok(partida);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/state/{matchId}")
    public ResponseEntity<?> getEstadoPartida(@PathVariable String matchId) {
        Partida partida = battleEngine.getEstadoPartida(matchId);
        if (partida == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(partida);
    }

    @PostMapping("/{matchId}/coin-flip")
    public ResponseEntity<?> lanzarMoneda(@PathVariable String matchId) {
        try {
            boolean jugadorGana = battleEngine.lanzarMoneda(matchId);
            return ResponseEntity.ok(jugadorGana);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/{matchId}/choose-turn")
    public ResponseEntity<?> elegirTurno(@PathVariable String matchId,
                                         @RequestBody ChooseTurnRequest request) {
        try {
            battleEngine.elegirTurno(matchId, request.isVaPrimero());
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/{matchId}/play-pokemon")
    public ResponseEntity<?> jugarPokemon(@PathVariable String matchId,
                                          @RequestBody JugarPokemonRequest request) {
        try {
            battleEngine.jugarPokemon(matchId, request.getCartaId(), request.getPosicion());
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/{matchId}/attach-energy")
    public ResponseEntity<?> unirEnergia(@PathVariable String matchId,
                                         @RequestBody UnirEnergiaRequest request) {
        try {
            battleEngine.unirEnergia(matchId, request.getCartaId(), request.getEnergiaId());
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // FIX: endpoint de ataque que faltaba — el frontend lo llama al hacer clic en el activo
    @PostMapping("/{matchId}/attack")
    public ResponseEntity<?> atacar(@PathVariable String matchId) {
        try {
            battleEngine.realizarAtaque(matchId);
            // 🚨 IMPORTANTE: Pasar el turno después de atacar
            battleEngine.pasarTurno(matchId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    @PostMapping("/{matchId}/pass-turn")
    public ResponseEntity<?> pasarTurno(@PathVariable String matchId) {
        try {
            battleEngine.pasarTurno(matchId);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────
    // DTOs
    // ─────────────────────────────────────────────────────────────

    public static class StartBattleRequest {
        private Long mazoId;
        public Long getMazoId() { return mazoId; }
        public void setMazoId(Long mazoId) { this.mazoId = mazoId; }
    }

    public static class ChooseTurnRequest {
        private boolean vaPrimero;
        public boolean isVaPrimero() { return vaPrimero; }
        public void setVaPrimero(boolean v) { this.vaPrimero = v; }
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