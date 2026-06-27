package com.pokemon.tcg.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pokemon.tcg.dto.ChooseTurnRequest;
import com.pokemon.tcg.dto.EvolveRequest;
import com.pokemon.tcg.dto.JugarPokemonRequest;
import com.pokemon.tcg.dto.StartBattleOnlineRequest;
import com.pokemon.tcg.dto.StartBattleRequest;
import com.pokemon.tcg.dto.UnirEnergiaRequest;
import com.pokemon.tcg.model.battle.Partida;
import com.pokemon.tcg.model.battle.TableroJugador;
import com.pokemon.tcg.service.BattleEngineService;
import com.pokemon.tcg.service.LobbyRoomService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class BattleControllerTest {

    private MockMvc mockMvc;
    private BattleEngineService battleEngine;
    private LobbyRoomService lobbyRoomService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        battleEngine = mock(BattleEngineService.class);
        lobbyRoomService = mock(LobbyRoomService.class);
        BattleController controller = new BattleController(battleEngine, lobbyRoomService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    private Partida partidaSimple() {
        Partida p = new Partida(new TableroJugador(), new TableroJugador());
        p.setId("match-123");
        p.setJugadorUsername("ash");
        p.setBotUsername("BOT");
        return p;
    }

    @Test
    void startBattle_exitoso_retorna200() throws Exception {
        Partida p = partidaSimple();
        when(battleEngine.startBattle(eq("ash"), any(), any())).thenReturn(p);

        StartBattleRequest req = new StartBattleRequest();
        req.setMazoId(1L);
        req.setBotDifficulty("NORMAL");

        mockMvc.perform(post("/api/battle/start/ash")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    @Test
    void startBattle_error_retorna400() throws Exception {
        when(battleEngine.startBattle(any(), any(), any()))
                .thenThrow(new IllegalArgumentException("Mazo no encontrado"));

        StartBattleRequest req = new StartBattleRequest();
        req.setMazoId(99L);

        mockMvc.perform(post("/api/battle/start/ash")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Mazo no encontrado"));
    }

    @Test
    void startBattleOnline_exitoso_retorna200() throws Exception {
        Partida p = partidaSimple();
        when(battleEngine.startBattleOnline(any(), any(), any(), any())).thenReturn(p);

        StartBattleOnlineRequest req = new StartBattleOnlineRequest();
        req.setPlayer1("ash");
        req.setPlayer1MazoId(1L);
        req.setPlayer2("misty");
        req.setPlayer2MazoId(2L);

        mockMvc.perform(post("/api/battle/start-online")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    @Test
    void startBattleOnline_error_retorna400() throws Exception {
        when(battleEngine.startBattleOnline(any(), any(), any(), any()))
                .thenThrow(new RuntimeException("Error al iniciar"));

        StartBattleOnlineRequest req = new StartBattleOnlineRequest();

        mockMvc.perform(post("/api/battle/start-online")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getEstadoPartida_usuarioNormal_retornaPartida() throws Exception {
        Partida p = partidaSimple();
        when(battleEngine.getEstadoPartida("match-123", "ash")).thenReturn(p);
        when(lobbyRoomService.isSpectator("match-123", "ash")).thenReturn(false);

        mockMvc.perform(get("/api/battle/state/match-123")
                .header("X-Username", "ash"))
                .andExpect(status().isOk());
    }

    @Test
    void getEstadoPartida_noEncontrada_retorna404() throws Exception {
        when(battleEngine.getEstadoPartida("noexiste", null)).thenReturn(null);

        mockMvc.perform(get("/api/battle/state/noexiste"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getEstadoPartida_esBot_retornaSwappedPerspective() throws Exception {
        Partida p = partidaSimple();
        when(battleEngine.getEstadoPartida("match-123", "BOT")).thenReturn(p);
        when(lobbyRoomService.isSpectator("match-123", "BOT")).thenReturn(false);

        mockMvc.perform(get("/api/battle/state/match-123")
                .header("X-Username", "BOT"))
                .andExpect(status().isOk());
    }

    @Test
    void getEstadoPartida_esEspectador_retornaVistaEspectador() throws Exception {
        Partida p = partidaSimple();
        when(battleEngine.getEstadoPartida("match-123", "spectator")).thenReturn(p);
        when(lobbyRoomService.isSpectator("match-123", "spectator")).thenReturn(true);

        mockMvc.perform(get("/api/battle/state/match-123")
                .header("X-Username", "spectator"))
                .andExpect(status().isOk());
    }

    @Test
    void heartbeat_exitoso_retorna200() throws Exception {
        Partida p = partidaSimple();
        when(battleEngine.registrarHeartbeat("match-123", "ash")).thenReturn(p);

        mockMvc.perform(post("/api/battle/match-123/heartbeat")
                .header("X-Username", "ash"))
                .andExpect(status().isOk());
    }

    @Test
    void heartbeat_noEncontrado_retorna404() throws Exception {
        when(battleEngine.registrarHeartbeat("noexiste", "ash")).thenReturn(null);

        mockMvc.perform(post("/api/battle/noexiste/heartbeat")
                .header("X-Username", "ash"))
                .andExpect(status().isNotFound());
    }

    @Test
    void heartbeat_esBot_retornaSwappedPerspective() throws Exception {
        Partida p = partidaSimple();
        when(battleEngine.registrarHeartbeat("match-123", "BOT")).thenReturn(p);

        mockMvc.perform(post("/api/battle/match-123/heartbeat")
                .header("X-Username", "BOT"))
                .andExpect(status().isOk());
    }

    @Test
    void rendirse_exitoso_retorna200() throws Exception {
        Partida p = partidaSimple();
        when(battleEngine.rendirse("match-123", "ash")).thenReturn(p);

        mockMvc.perform(post("/api/battle/match-123/surrender")
                .header("X-Username", "ash"))
                .andExpect(status().isOk());
    }

    @Test
    void rendirse_error_retorna400() throws Exception {
        when(battleEngine.rendirse("match-123", "ash"))
                .thenThrow(new IllegalArgumentException("No puedes rendirte"));

        mockMvc.perform(post("/api/battle/match-123/surrender")
                .header("X-Username", "ash"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void evolucionarPokemon_exitoso_retorna200() throws Exception {
        doNothing().when(battleEngine).evolucionarPokemon(any(), any(), any(), any());

        EvolveRequest req = new EvolveRequest();
        req.setCartaManoId("xy1-10");
        req.setCartaTableroId("xy1-5");

        mockMvc.perform(post("/api/battle/match-123/evolve")
                .header("X-Username", "ash")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    @Test
    void evolucionarPokemon_error_retorna400() throws Exception {
        doThrow(new IllegalArgumentException("No puedes evolucionar"))
                .when(battleEngine).evolucionarPokemon(any(), any(), any(), any());

        EvolveRequest req = new EvolveRequest();
        req.setCartaManoId("xy1-10");
        req.setCartaTableroId("xy1-5");

        mockMvc.perform(post("/api/battle/match-123/evolve")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void jugarBot_exitoso_retorna200() throws Exception {
        Partida p = partidaSimple();
        doNothing().when(battleEngine).ejecutarTurnoBot("match-123");
        when(battleEngine.getEstadoPartida("match-123")).thenReturn(p);

        mockMvc.perform(post("/api/battle/match-123/jugar-bot"))
                .andExpect(status().isOk());
    }

    @Test
    void jugarBot_error_retorna400() throws Exception {
        doThrow(new RuntimeException("Error del bot"))
                .when(battleEngine).ejecutarTurnoBot("match-123");

        mockMvc.perform(post("/api/battle/match-123/jugar-bot"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void jugarBotSetup_exitoso_retorna200() throws Exception {
        Partida p = partidaSimple();
        doNothing().when(battleEngine).ejecutarSetupBot("match-123");
        when(battleEngine.getEstadoPartida("match-123")).thenReturn(p);

        mockMvc.perform(post("/api/battle/match-123/jugar-bot-setup"))
                .andExpect(status().isOk());
    }

    @Test
    void lanzarMoneda_exitoso_retorna200() throws Exception {
        Partida p = partidaSimple();
        when(battleEngine.lanzarMoneda("match-123", "ash", "cara")).thenReturn(p);

        mockMvc.perform(post("/api/battle/match-123/coin-flip")
                .header("X-Username", "ash")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"eleccion\":\"cara\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void lanzarMoneda_sinPayload_retorna200() throws Exception {
        Partida p = partidaSimple();
        when(battleEngine.lanzarMoneda("match-123", "ash", null)).thenReturn(p);

        mockMvc.perform(post("/api/battle/match-123/coin-flip")
                .header("X-Username", "ash"))
                .andExpect(status().isOk());
    }

    @Test
    void lanzarMoneda_error_retorna400() throws Exception {
        when(battleEngine.lanzarMoneda(any(), any(), any()))
                .thenThrow(new IllegalArgumentException("Moneda ya lanzada"));

        mockMvc.perform(post("/api/battle/match-123/coin-flip")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void coinHandshake_exitoso_retorna200() throws Exception {
        Partida p = partidaSimple();
        when(battleEngine.actualizarHandshakeMoneda("match-123", "ash", true, 5)).thenReturn(p);

        mockMvc.perform(post("/api/battle/match-123/coin-handshake")
                .header("X-Username", "ash")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"holding\":true,\"power\":5}"))
                .andExpect(status().isOk());
    }

    @Test
    void actualizarLoading_exitoso_retorna200() throws Exception {
        Partida p = partidaSimple();
        when(battleEngine.actualizarLoading("match-123", "ash", 75)).thenReturn(p);

        mockMvc.perform(post("/api/battle/match-123/loading")
                .header("X-Username", "ash")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"percentage\":75}"))
                .andExpect(status().isOk());
    }

    @Test
    void elegirTurno_exitoso_retorna200() throws Exception {
        doNothing().when(battleEngine).elegirTurno(any(), anyBoolean(), any());

        ChooseTurnRequest req = new ChooseTurnRequest();
        req.setVaPrimero(true);

        mockMvc.perform(post("/api/battle/match-123/choose-turn")
                .header("X-Username", "ash")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    @Test
    void jugarPokemon_exitoso_retorna200() throws Exception {
        doNothing().when(battleEngine).jugarPokemon(any(), any(), any());

        JugarPokemonRequest req = new JugarPokemonRequest();
        req.setCartaId("xy1-5");

        mockMvc.perform(post("/api/battle/match-123/play-pokemon")
                .header("X-Username", "ash")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    @Test
    void jugarPokemon_error_retorna400() throws Exception {
        doThrow(new IllegalArgumentException("No puedes jugar ese Pokemon"))
                .when(battleEngine).jugarPokemon(any(), any(), any());

        JugarPokemonRequest req = new JugarPokemonRequest();
        req.setCartaId("xy1-5");

        mockMvc.perform(post("/api/battle/match-123/play-pokemon")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void jugarTrainer_exitoso_retorna200() throws Exception {
        doNothing().when(battleEngine).jugarTrainer(any(), any(), any());

        mockMvc.perform(post("/api/battle/match-123/play-trainer")
                .header("X-Username", "ash")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"cartaId\":\"xy1-train\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void unirEnergia_exitoso_retorna200() throws Exception {
        doNothing().when(battleEngine).unirEnergia(any(), any(), any(), any());

        UnirEnergiaRequest req = new UnirEnergiaRequest();
        req.setCartaId("xy1-5");
        req.setEnergiaId("xy1-e1");

        mockMvc.perform(post("/api/battle/match-123/attach-energy")
                .header("X-Username", "ash")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    @Test
    void atacar_exitoso_retorna200() throws Exception {
        doNothing().when(battleEngine).realizarAtaque(any(), any(), any(), any());

        mockMvc.perform(post("/api/battle/match-123/attack")
                .header("X-Username", "ash")
                .param("nombreAtaque", "Tackle"))
                .andExpect(status().isOk());
    }

    @Test
    void atacar_error_retorna400() throws Exception {
        doThrow(new IllegalArgumentException("Ataque no disponible"))
                .when(battleEngine).realizarAtaque(any(), any(), any(), any());

        mockMvc.perform(post("/api/battle/match-123/attack")
                .param("nombreAtaque", "Tackle"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void promoteToActive_exitoso_retorna200() throws Exception {
        doNothing().when(battleEngine).subirAActivoDesdeBanca(any(), any(), any());

        mockMvc.perform(post("/api/battle/match-123/promote")
                .header("X-Username", "ash")
                .contentType(MediaType.APPLICATION_JSON)
                .content("\"xy1-5\""))
                .andExpect(status().isOk());
    }

    @Test
    void pasarTurno_exitoso_retorna200() throws Exception {
        doNothing().when(battleEngine).pasarTurno(any(), any());

        mockMvc.perform(post("/api/battle/match-123/pass-turn")
                .header("X-Username", "ash"))
                .andExpect(status().isOk());
    }

    @Test
    void pasarTurno_error_retorna400() throws Exception {
        doThrow(new IllegalArgumentException("No es tu turno"))
                .when(battleEngine).pasarTurno(any(), any());

        mockMvc.perform(post("/api/battle/match-123/pass-turn")
                .header("X-Username", "ash"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void retirarPokemon_exitoso_retorna200() throws Exception {
        doNothing().when(battleEngine).realizarRetirada(any(), any(), any());

        mockMvc.perform(post("/api/battle/match-123/retreat")
                .header("X-Username", "ash")
                .contentType(MediaType.APPLICATION_JSON)
                .content("\"xy1-bench\""))
                .andExpect(status().isOk());
    }

    @Test
    void debugDrawCard_exitoso_retorna200() throws Exception {
        Partida p = partidaSimple();
        when(battleEngine.debugRobarCarta("match-123", "xy1-5", "ash")).thenReturn(p);

        mockMvc.perform(post("/api/battle/match-123/debug/draw")
                .header("X-Username", "ash")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"cardId\":\"xy1-5\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void debugForzarEstado_exitoso_retorna200() throws Exception {
        Partida p = partidaSimple();
        when(battleEngine.debugForzarEstado("match-123", "jugador", "PARALIZADO", "ash")).thenReturn(p);

        mockMvc.perform(post("/api/battle/match-123/debug/status")
                .header("X-Username", "ash")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"objetivo\":\"jugador\",\"estado\":\"PARALIZADO\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void debugSetHp_exitoso_retorna200() throws Exception {
        Partida p = partidaSimple();
        when(battleEngine.debugSetHp("match-123", "jugador", 50, "ash")).thenReturn(p);

        mockMvc.perform(post("/api/battle/match-123/debug/hp")
                .header("X-Username", "ash")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"objetivo\":\"jugador\",\"hp\":50}"))
                .andExpect(status().isOk());
    }

    @Test
    void getCatalogoDebug_retorna200() throws Exception {
        when(battleEngine.obtenerCatalogoCartasDebug()).thenReturn(List.of());

        mockMvc.perform(get("/api/battle/debug/catalog"))
                .andExpect(status().isOk());
    }

    @Test
    void resolveEffect_exitoso_retorna200() throws Exception {
        Partida p = partidaSimple();
        when(battleEngine.resolverAccionPendiente(any(), any(), any())).thenReturn(p);

        mockMvc.perform(post("/api/battle/match-123/resolve-effect")
                .header("X-Username", "ash")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"selectedIds\":[\"xy1-5\"]}"))
                .andExpect(status().isOk());
    }

    @Test
    void resolveEffect_error_retorna400() throws Exception {
        when(battleEngine.resolverAccionPendiente(any(), any(), any()))
                .thenThrow(new IllegalArgumentException("Accion no valida"));

        mockMvc.perform(post("/api/battle/match-123/resolve-effect")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"selectedIds\":[]}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void evaluateSetup_exitoso_retorna200() throws Exception {
        Partida p = partidaSimple();
        doNothing().when(battleEngine).evaluarSetupInitialDraw(any(), any());
        when(battleEngine.getEstadoPartida("match-123", "ash")).thenReturn(p);

        mockMvc.perform(post("/api/battle/match-123/setup/evaluate")
                .header("X-Username", "ash"))
                .andExpect(status().isOk());
    }

    @Test
    void confirmBenchSetup_exitoso_retorna200() throws Exception {
        Partida p = partidaSimple();
        doNothing().when(battleEngine).confirmarBancaSetup(any(), any());
        when(battleEngine.getEstadoPartida("match-123", "ash")).thenReturn(p);

        mockMvc.perform(post("/api/battle/match-123/setup/confirm-bench")
                .header("X-Username", "ash"))
                .andExpect(status().isOk());
    }
}
