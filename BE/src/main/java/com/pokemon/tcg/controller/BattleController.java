package com.pokemon.tcg.controller;

import com.pokemon.tcg.model.battle.Partida;
import com.pokemon.tcg.model.battle.TableroJugador;
import com.pokemon.tcg.model.battle.CartaEnJuego;
import com.pokemon.tcg.model.Card;
import com.pokemon.tcg.service.BattleEngineService;
import com.pokemon.tcg.service.LobbyRoomService;
import com.pokemon.tcg.dto.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/battle")
public class BattleController {

    private final BattleEngineService battleEngine;
    private final LobbyRoomService lobbyRoomService;

    public BattleController(BattleEngineService battleEngine, LobbyRoomService lobbyRoomService) {
        this.battleEngine = battleEngine;
        this.lobbyRoomService = lobbyRoomService;
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

        if (lobbyRoomService.isSpectator(matchId, username)) {
            return ResponseEntity.ok(toSpectatorView(partida));
        }

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

    @PostMapping("/{matchId}/surrender")
    public ResponseEntity<?> rendirse(@PathVariable String matchId,
                                      @RequestHeader(value = "X-Username", required = false) String username) {
        try {
            Partida partida = battleEngine.rendirse(matchId, username);
            if (username != null && username.equals(partida.getBotUsername())) {
                return ResponseEntity.ok(swapPerspective(partida));
            }
            return ResponseEntity.ok(partida);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
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

    @PostMapping("/{matchId}/jugar-bot-setup")
    public ResponseEntity<?> jugarBotSetup(@PathVariable String matchId) {
        try {
            battleEngine.ejecutarSetupBot(matchId);
            Partida partidaActualizada = battleEngine.getEstadoPartida(matchId);
            return ResponseEntity.ok(partidaActualizada);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body("Error del bot en setup: " + e.getMessage());
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

    @PostMapping("/{matchId}/loading")
    public ResponseEntity<?> actualizarLoading(@PathVariable String matchId,
                                               @RequestHeader(value = "X-Username", required = false) String username,
                                               @RequestBody java.util.Map<String, Object> payload) {
        try {
            int percentage = 0;
            if (payload != null && payload.get("percentage") instanceof Number number) {
                percentage = number.intValue();
            }
            Partida partida = battleEngine.actualizarLoading(matchId, username, percentage);
            if (username != null && username.equals(partida.getBotUsername())) {
                return ResponseEntity.ok(swapPerspective(partida));
            }
            return ResponseEntity.ok(partida);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/{matchId}/setup/evaluate")
    public ResponseEntity<?> evaluateSetup(@PathVariable String matchId,
                                           @RequestHeader(value = "X-Username", required = false) String username) {
        try {
            battleEngine.evaluarSetupInitialDraw(matchId, username);
            Partida partida = battleEngine.getEstadoPartida(matchId, username);
            return ResponseEntity.ok(username != null && username.equals(partida.getBotUsername()) ? swapPerspective(partida) : partida);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/{matchId}/setup/execute-mulligan")
    public ResponseEntity<?> executeMulligan(@PathVariable String matchId,
                                           @RequestHeader(value = "X-Username", required = false) String username) {
        try {
            battleEngine.ejecutarMulligan(matchId, username);
            Partida partida = battleEngine.getEstadoPartida(matchId, username);
            return ResponseEntity.ok(username != null && username.equals(partida.getBotUsername()) ? swapPerspective(partida) : partida);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/{matchId}/setup/extra-draw")
    public ResponseEntity<?> extraDraw(@PathVariable String matchId,
                                           @RequestHeader(value = "X-Username", required = false) String username,
                                           @RequestBody(required = false) Map<String, Integer> payload) {
        try {
            int cantidad = payload != null && payload.containsKey("cantidad") ? payload.get("cantidad") : 0;
            battleEngine.resolverCartasExtra(matchId, username, cantidad);
            Partida partida = battleEngine.getEstadoPartida(matchId, username);
            return ResponseEntity.ok(username != null && username.equals(partida.getBotUsername()) ? swapPerspective(partida) : partida);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/{matchId}/setup/place-prizes")
    public ResponseEntity<?> placePrizes(@PathVariable String matchId,
                                           @RequestHeader(value = "X-Username", required = false) String username) {
        try {
            battleEngine.colocarPremios(matchId, username);
            Partida partida = battleEngine.getEstadoPartida(matchId, username);
            return ResponseEntity.ok(username != null && username.equals(partida.getBotUsername()) ? swapPerspective(partida) : partida);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/{matchId}/setup/reveal")
    public ResponseEntity<?> revealSetup(@PathVariable String matchId,
                                         @RequestHeader(value = "X-Username", required = false) String username) {
        try {
            battleEngine.confirmarRevealSetup(matchId, username);
            Partida partida = battleEngine.getEstadoPartida(matchId, username);
            return ResponseEntity.ok(username != null && username.equals(partida.getBotUsername()) ? swapPerspective(partida) : partida);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/{matchId}/setup/place-active")
    public ResponseEntity<?> placeActiveSetup(@PathVariable String matchId,
                                           @RequestHeader(value = "X-Username", required = false) String username,
                                           @RequestBody Map<String, String> payload) {
        try {
            String cartaId = payload.get("cartaId");
            battleEngine.colocarActivoSetup(matchId, username, cartaId);
            Partida partida = battleEngine.getEstadoPartida(matchId, username);
            return ResponseEntity.ok(username != null && username.equals(partida.getBotUsername()) ? swapPerspective(partida) : partida);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/{matchId}/setup/place-bench")
    public ResponseEntity<?> placeBenchSetup(@PathVariable String matchId,
                                           @RequestHeader(value = "X-Username", required = false) String username,
                                           @RequestBody Map<String, String> payload) {
        try {
            String cartaId = payload.get("cartaId");
            battleEngine.colocarBancaSetup(matchId, username, cartaId);
            Partida partida = battleEngine.getEstadoPartida(matchId, username);
            return ResponseEntity.ok(username != null && username.equals(partida.getBotUsername()) ? swapPerspective(partida) : partida);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/{matchId}/setup/confirm-bench")
    public ResponseEntity<?> confirmBenchSetup(@PathVariable String matchId,
                                           @RequestHeader(value = "X-Username", required = false) String username) {
        try {
            battleEngine.confirmarBancaSetup(matchId, username);
            Partida partida = battleEngine.getEstadoPartida(matchId, username);
            return ResponseEntity.ok(username != null && username.equals(partida.getBotUsername()) ? swapPerspective(partida) : partida);
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
        swapped.setNumeroTurno(p.getNumeroTurno());
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
        swapped.setTurnLogs(p.getTurnLogs());
        swapped.setJugadorLoadingPercentage(p.getBotLoadingPercentage());
        swapped.setBotLoadingPercentage(p.getJugadorLoadingPercentage());
        swapped.setSetupJugadorListo(p.isSetupBotListo());
        swapped.setSetupBotListo(p.isSetupJugadorListo());
        swapped.setCartasMulliganExtraPendientesJugador(p.getCartasMulliganExtraPendientesBot());
        swapped.setCartasMulliganExtraPendientesBot(p.getCartasMulliganExtraPendientesJugador());
        swapped.setSetupJugadorRoboExtraMulligan(p.isSetupBotRoboExtraMulligan());
        swapped.setSetupBotRoboExtraMulligan(p.isSetupJugadorRoboExtraMulligan());

        if (p.getTurnoActual() == Partida.Turno.JUGADOR) {
            swapped.setTurnoActual(Partida.Turno.BOT);
        } else {
            swapped.setTurnoActual(Partida.Turno.JUGADOR);
        }

        return swapped;
    }

    private Partida toSpectatorView(Partida p) {
        Partida view = new Partida(toSpectatorBoard(p.getJugador()), toSpectatorBoard(p.getBot()));
        view.setId(p.getId());
        view.setFaseActual(p.getFaseActual());
        view.setTurnoActual(p.getTurnoActual());
        view.setNumeroTurno(p.getNumeroTurno());
        view.setYaSeRetiroEsteTurno(p.isYaSeRetiroEsteTurno());
        view.setMulligansJugador(p.getMulligansJugador());
        view.setMulligansBot(p.getMulligansBot());
        view.setUltimasMonedasLanzadas(p.getUltimasMonedasLanzadas());
        view.setJugadorUsername(p.getJugadorUsername());
        view.setBotUsername(p.getBotUsername());
        view.setCoinFlipped(p.isCoinFlipped());
        view.setCoinFlipWinner(p.getCoinFlipWinner());
        view.setCoinFlipResult(p.getCoinFlipResult());
        view.setCoinFlipCallerUsername(p.getCoinFlipCallerUsername());
        view.setGanador(p.getGanador());
        view.setRazonFinPartida(p.getRazonFinPartida());
        view.setTurnLogs(p.getTurnLogs());
        view.setSetupJugadorListo(p.isSetupJugadorListo());
        view.setSetupBotListo(p.isSetupBotListo());
        view.setJugadorLoadingPercentage(p.getJugadorLoadingPercentage());
        view.setBotLoadingPercentage(p.getBotLoadingPercentage());
        return view;
    }

    private TableroJugador toSpectatorBoard(TableroJugador source) {
        TableroJugador board = new TableroJugador();
        board.setMazo(hiddenCards(source.getMazo().size(), "deck"));
        board.setMano(java.util.Collections.emptyList());
        board.setPremios(hiddenCards(source.getPremios().size(), "prize"));
        board.setPilaDescarte(source.getPilaDescarte());
        board.setActivo(copyInPlay(source.getActivo()));
        board.setBanca(source.getBanca().stream().map(this::copyInPlay).toList());
        return board;
    }

    private java.util.List<Card> hiddenCards(int count, String prefix) {
        java.util.List<Card> hidden = new java.util.ArrayList<>();
        for (int i = 0; i < count; i++) {
            Card card = new Card();
            card.setId("hidden-" + prefix + "-" + i);
            card.setNombre("Carta oculta");
            card.setImagen("/images/cards/back.png");
            card.setSupertype("Hidden");
            hidden.add(card);
        }
        return hidden;
    }

    private CartaEnJuego copyInPlay(CartaEnJuego source) {
        if (source == null) return null;
        CartaEnJuego copy = new CartaEnJuego(source.getCard());
        copy.setHpActual(source.getHpActual());
        copy.setPuedeAtacar(source.isPuedeAtacar());
        copy.setInvulnerable(source.isInvulnerable());
        copy.setBocaAbajo(source.isBocaAbajo());
        copy.getEnergiasUnidas().addAll(source.getEnergiasUnidas());
        source.getCondicionesEspeciales().forEach(copy::agregarCondicion);
        return copy;
    }
}
