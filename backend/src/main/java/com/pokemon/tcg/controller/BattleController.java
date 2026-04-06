package com.pokemon.tcg.controller;

import com.pokemon.tcg.model.battle.Partida;
import com.pokemon.tcg.service.BattleEngineService;
import com.pokemon.tcg.dto.*;
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
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }



    @GetMapping("/state/{matchId}")
    public ResponseEntity<?> getEstadoPartida(@PathVariable String matchId) {
        Partida partida = battleEngine.getEstadoPartida(matchId);
        if (partida == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(partida);
    }

    @PostMapping("/{matchId}/evolve")
    public ResponseEntity<?> evolucionarPokemon(@PathVariable String matchId,
                                                @RequestBody EvolveRequest request) {
        try {
            battleEngine.evolucionarPokemon(matchId, request.getCartaManoId(), request.getCartaTableroId());
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }



    @PostMapping("/{matchId}/jugar-bot")
    public ResponseEntity<?> jugarBot(@PathVariable String matchId) {
        try {
            battleEngine.ejecutarTurnoBot(matchId);
            Partida partidaActualizada = battleEngine.getEstadoPartida(matchId);
            return ResponseEntity.ok(partidaActualizada);
        } catch (Exception e) {
            // 🚩 ESTA LÍNEA ES CLAVE: Nos va a imprimir en rojo exactamente dónde falló el bot
            e.printStackTrace();
            return ResponseEntity.badRequest().body("Error del bot: " + e.getMessage());
        }
    }

    @PostMapping("/{matchId}/coin-flip")
    public ResponseEntity<?> lanzarMoneda(@PathVariable String matchId) {
        try {
            boolean jugadorGana = battleEngine.lanzarMoneda(matchId);
            return ResponseEntity.ok(jugadorGana);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/{matchId}/choose-turn")
    public ResponseEntity<?> elegirTurno(@PathVariable String matchId,
                                         @RequestBody ChooseTurnRequest request) {
        try {
            battleEngine.elegirTurno(matchId, request.isVaPrimero());
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // 🚩 ACÁ ESTÁ EL FIX: Solo 2 parámetros, sin 'posicion'
    @PostMapping("/{matchId}/play-pokemon")
    public ResponseEntity<?> jugarPokemon(@PathVariable String matchId,
                                          @RequestBody JugarPokemonRequest request) {
        try {
            battleEngine.jugarPokemon(matchId, request.getCartaId());
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/{matchId}/attach-energy")
    public ResponseEntity<?> unirEnergia(@PathVariable String matchId,
                                         @RequestBody UnirEnergiaRequest request) {
        try {
            battleEngine.unirEnergia(matchId, request.getCartaId(), request.getEnergiaId());
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/{matchId}/attack")
    public ResponseEntity<?> atacar(@PathVariable String matchId, @RequestParam String nombreAtaque) {
        try {
            battleEngine.realizarAtaque(matchId, nombreAtaque);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/{matchId}/promote")
    public ResponseEntity<?> promoteToActive(@PathVariable String matchId, @RequestBody String cartaId) {
        try {
            battleEngine.subirAActivoDesdeBanca(matchId, cartaId);
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
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/{matchId}/retreat")
    public ResponseEntity<?> retirarPokemon(@PathVariable String matchId,
                                            @RequestBody String nuevoActivoId) {
        try {
            battleEngine.realizarRetirada(matchId, nuevoActivoId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            // Si no tiene energías suficientes, acá va a saltar el error
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    @PostMapping("/{matchId}/debug/draw")
    public ResponseEntity<?> debugDrawCard(@PathVariable String matchId, @RequestBody java.util.Map<String, String> payload) {
        try {
            String cardId = payload.get("cardId");
            Partida partidaActualizada = battleEngine.debugRobarCarta(matchId, cardId);
            return ResponseEntity.ok(partidaActualizada);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error en God Mode (Robar): " + e.getMessage());
        }
    }

    @PostMapping("/{matchId}/debug/status")
    public ResponseEntity<?> debugForzarEstado(@PathVariable String matchId, @RequestBody java.util.Map<String, String> payload) {
        try {
            String objetivo = payload.get("objetivo"); // "JUGADOR" o "BOT"
            String estado = payload.get("estado");     // "ASLEEP", "PARALYZED", etc.

            Partida partidaActualizada = battleEngine.debugForzarEstado(matchId, objetivo, estado);
            return ResponseEntity.ok(partidaActualizada);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error en God Mode (Estado): " + e.getMessage());
        }
    }

    @PostMapping("/{matchId}/debug/hp")
    public ResponseEntity<?> debugSetHp(@PathVariable String matchId, @RequestBody java.util.Map<String, Object> payload) {
        try {
            String objetivo = (String) payload.get("objetivo");
            // Usamos toString() y parseInt para evitar problemas si Angular manda un número o un string
            int hp = Integer.parseInt(payload.get("hp").toString());

            Partida partidaActualizada = battleEngine.debugSetHp(matchId, objetivo, hp);
            return ResponseEntity.ok(partidaActualizada);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error en God Mode (HP): " + e.getMessage());
        }
    }
    @GetMapping("/debug/catalog")
    public ResponseEntity<?> getCatalogoDebug() {
        return ResponseEntity.ok(battleEngine.obtenerCatalogoCartasDebug());
    }
}