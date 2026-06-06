package com.pokemon.tcg.controller;

import com.pokemon.tcg.model.battle.Partida;
import com.pokemon.tcg.model.battle.TableroJugador;
import com.pokemon.tcg.service.BattleEngineService;
import com.pokemon.tcg.dto.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/battle")
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

    @PostMapping("/start-online")
    public ResponseEntity<?> startBattleOnline(@RequestBody StartBattleOnlineRequest request) {
        try {
            Partida partida = battleEngine.startBattleOnline(
                request.getPlayer1(), request.getPlayer1MazoId(),
                request.getPlayer2(), request.getPlayer2MazoId()
            );
            return ResponseEntity.ok(partida);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/state/{matchId}")
    public ResponseEntity<?> getEstadoPartida(@PathVariable String matchId,
                                             @RequestHeader(value = "X-Username", required = false) String username) {
        Partida partida = battleEngine.getEstadoPartida(matchId, username);
        if (partida == null) return ResponseEntity.notFound().build();

        if (username != null && username.equals(partida.getBotUsername())) {
            return ResponseEntity.ok(swapPerspective(partida));
        }
        return ResponseEntity.ok(partida);
    }

    @PostMapping("/{matchId}/heartbeat")
    public ResponseEntity<?> heartbeat(@PathVariable String matchId,
                                       @RequestHeader(value = "X-Username", required = false) String username) {
        Partida partida = battleEngine.registrarHeartbeat(matchId, username);
        if (partida == null) return ResponseEntity.notFound().build();
        if (username != null && username.equals(partida.getBotUsername())) {
            return ResponseEntity.ok(swapPerspective(partida));
        }
        return ResponseEntity.ok(partida);
    }

    @PostMapping("/{matchId}/evolve")
    public ResponseEntity<?> evolucionarPokemon(@PathVariable String matchId,
                                                @RequestHeader(value = "X-Username", required = false) String username,
                                                @RequestBody EvolveRequest request) {
        try {
            battleEngine.evolucionarPokemon(matchId, request.getCartaManoId(), request.getCartaTableroId(), username);
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
            e.printStackTrace();
            return ResponseEntity.badRequest().body("Error del bot: " + e.getMessage());
        }
    }

    @PostMapping("/{matchId}/coin-flip")
    public ResponseEntity<?> lanzarMoneda(@PathVariable String matchId,
                                          @RequestHeader(value = "X-Username", required = false) String username,
                                          @RequestBody(required = false) Map<String, String> payload) {
        try {
            String eleccion = payload != null ? payload.get("eleccion") : null;
            Partida partida = battleEngine.lanzarMoneda(matchId, username, eleccion);
            if (username != null && username.equals(partida.getBotUsername())) {
                return ResponseEntity.ok(swapPerspective(partida));
            }
            return ResponseEntity.ok(partida);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/{matchId}/coin-handshake")
    public ResponseEntity<?> actualizarHandshakeMoneda(@PathVariable String matchId,
                                                       @RequestHeader(value = "X-Username", required = false) String username,
                                                       @RequestBody(required = false) Map<String, Object> payload) {
        try {
            boolean holding = payload != null && Boolean.TRUE.equals(payload.get("holding"));
            int power = 0;
            if (payload != null && payload.get("power") instanceof Number number) {
                power = number.intValue();
            }

            Partida partida = battleEngine.actualizarHandshakeMoneda(matchId, username, holding, power);
            if (username != null && username.equals(partida.getBotUsername())) {
                return ResponseEntity.ok(swapPerspective(partida));
            }
            return ResponseEntity.ok(partida);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/{matchId}/choose-turn")
    public ResponseEntity<?> elegirTurno(@PathVariable String matchId,
                                         @RequestHeader(value = "X-Username", required = false) String username,
                                         @RequestBody ChooseTurnRequest request) {
        try {
            battleEngine.elegirTurno(matchId, request.isVaPrimero(), username);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/{matchId}/play-pokemon")
    public ResponseEntity<?> jugarPokemon(@PathVariable String matchId,
                                          @RequestHeader(value = "X-Username", required = false) String username,
                                          @RequestBody JugarPokemonRequest request) {
        try {
            battleEngine.jugarPokemon(matchId, request.getCartaId(), username);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/{matchId}/attach-energy")
    public ResponseEntity<?> unirEnergia(@PathVariable String matchId,
                                         @RequestHeader(value = "X-Username", required = false) String username,
                                         @RequestBody UnirEnergiaRequest request) {
        try {
            battleEngine.unirEnergia(matchId, request.getCartaId(), request.getEnergiaId(), username);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/{matchId}/attack")
    public ResponseEntity<?> atacar(@PathVariable String matchId,
                                    @RequestHeader(value = "X-Username", required = false) String username,
                                    @RequestParam String nombreAtaque) {
        try {
            battleEngine.realizarAtaque(matchId, nombreAtaque, username);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/{matchId}/promote")
    public ResponseEntity<?> promoteToActive(@PathVariable String matchId,
                                             @RequestHeader(value = "X-Username", required = false) String username,
                                             @RequestBody String cartaId) {
        try {
            battleEngine.subirAActivoDesdeBanca(matchId, cartaId, username);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/{matchId}/pass-turn")
    public ResponseEntity<?> pasarTurno(@PathVariable String matchId,
                                        @RequestHeader(value = "X-Username", required = false) String username) {
        try {
            battleEngine.pasarTurno(matchId, username);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/{matchId}/retreat")
    public ResponseEntity<?> retirarPokemon(@PathVariable String matchId,
                                            @RequestHeader(value = "X-Username", required = false) String username,
                                            @RequestBody String nuevoActivoId) {
        try {
            battleEngine.realizarRetirada(matchId, nuevoActivoId, username);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
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
            String objetivo = payload.get("objetivo");
            String estado = payload.get("estado");

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

    private Partida swapPerspective(Partida p) {
        TableroJugador oldJugador = p.getJugador();
        TableroJugador oldBot = p.getBot();

        Partida swapped = new Partida(oldBot, oldJugador);
        swapped.setId(p.getId());
        swapped.setFaseActual(p.getFaseActual());
        swapped.setYaSeRetiroEsteTurno(p.isYaSeRetiroEsteTurno());
        swapped.setMulligansJugador(p.getMulligansBot());
        swapped.setMulligansBot(p.getMulligansJugador());
        swapped.setUltimasMonedasLanzadas(p.getUltimasMonedasLanzadas());
        swapped.setJugadorUsername(p.getBotUsername());
        swapped.setBotUsername(p.getJugadorUsername());
        swapped.setCoinFlipped(p.isCoinFlipped());
        swapped.setCoinFlipWinner(p.getCoinFlipWinner());
        swapped.setCoinFlipResult(p.getCoinFlipResult());
        swapped.setCoinFlipCallerUsername(p.getCoinFlipCallerUsername());
        swapped.setGanador(p.getGanador());
        swapped.setRazonFinPartida(p.getRazonFinPartida());
        swapped.setCoinHandshakeJugadorPower(p.getCoinHandshakeBotPower());
        swapped.setCoinHandshakeBotPower(p.getCoinHandshakeJugadorPower());
        swapped.setCoinHandshakeJugadorHolding(p.isCoinHandshakeBotHolding());
        swapped.setCoinHandshakeBotHolding(p.isCoinHandshakeJugadorHolding());
        swapped.setCoinHandshakeComplete(p.isCoinHandshakeComplete());

        if (p.getTurnoActual() == Partida.Turno.JUGADOR) {
            swapped.setTurnoActual(Partida.Turno.BOT);
        } else {
            swapped.setTurnoActual(Partida.Turno.JUGADOR);
        }

        return swapped;
    }
}
