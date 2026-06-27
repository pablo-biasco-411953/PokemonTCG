package com.pokemon.tcg.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pokemon.tcg.dto.UseAbilityRequest;
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

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class BattleControllerMoreTest {

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

    private Partida partida() {
        Partida p = new Partida(new TableroJugador(), new TableroJugador());
        p.setId("m1");
        p.setJugadorUsername("ash");
        p.setBotUsername("BOT");
        return p;
    }

    // =================== executeMulligan ===================

    @Test
    void executeMulligan_exitoso_retorna200() throws Exception {
        Partida p = partida();
        doNothing().when(battleEngine).ejecutarMulligan("m1", "ash");
        when(battleEngine.getEstadoPartida("m1", "ash")).thenReturn(p);

        mockMvc.perform(post("/api/battle/m1/setup/execute-mulligan")
                .header("X-Username", "ash"))
                .andExpect(status().isOk());
    }

    @Test
    void executeMulligan_error_retorna400() throws Exception {
        doThrow(new IllegalArgumentException("error"))
                .when(battleEngine).ejecutarMulligan(any(), any());

        mockMvc.perform(post("/api/battle/m1/setup/execute-mulligan"))
                .andExpect(status().isBadRequest());
    }

    // =================== extraDraw ===================

    @Test
    void extraDraw_conCantidad_exitoso_retorna200() throws Exception {
        Partida p = partida();
        doNothing().when(battleEngine).resolverCartasExtra(any(), any(), anyInt());
        when(battleEngine.getEstadoPartida("m1", "ash")).thenReturn(p);

        mockMvc.perform(post("/api/battle/m1/setup/extra-draw")
                .header("X-Username", "ash")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"cantidad\":3}"))
                .andExpect(status().isOk());
    }

    @Test
    void extraDraw_sinPayload_retorna200() throws Exception {
        Partida p = partida();
        doNothing().when(battleEngine).resolverCartasExtra(any(), any(), eq(0));
        when(battleEngine.getEstadoPartida(any(), any())).thenReturn(p);

        mockMvc.perform(post("/api/battle/m1/setup/extra-draw")
                .header("X-Username", "ash"))
                .andExpect(status().isOk());
    }

    @Test
    void extraDraw_error_retorna400() throws Exception {
        doThrow(new IllegalArgumentException("error"))
                .when(battleEngine).resolverCartasExtra(any(), any(), anyInt());

        mockMvc.perform(post("/api/battle/m1/setup/extra-draw"))
                .andExpect(status().isBadRequest());
    }

    // =================== placePrizes ===================

    @Test
    void placePrizes_exitoso_retorna200() throws Exception {
        Partida p = partida();
        doNothing().when(battleEngine).colocarPremios(any(), any());
        when(battleEngine.getEstadoPartida(any(), any())).thenReturn(p);

        mockMvc.perform(post("/api/battle/m1/setup/place-prizes")
                .header("X-Username", "ash"))
                .andExpect(status().isOk());
    }

    @Test
    void placePrizes_error_retorna400() throws Exception {
        doThrow(new RuntimeException("error"))
                .when(battleEngine).colocarPremios(any(), any());

        mockMvc.perform(post("/api/battle/m1/setup/place-prizes"))
                .andExpect(status().isBadRequest());
    }

    // =================== revealSetup ===================

    @Test
    void revealSetup_exitoso_retorna200() throws Exception {
        Partida p = partida();
        doNothing().when(battleEngine).confirmarRevealSetup(any(), any());
        when(battleEngine.getEstadoPartida(any(), any())).thenReturn(p);

        mockMvc.perform(post("/api/battle/m1/setup/reveal")
                .header("X-Username", "ash"))
                .andExpect(status().isOk());
    }

    @Test
    void revealSetup_error_retorna400() throws Exception {
        doThrow(new RuntimeException("error"))
                .when(battleEngine).confirmarRevealSetup(any(), any());

        mockMvc.perform(post("/api/battle/m1/setup/reveal"))
                .andExpect(status().isBadRequest());
    }

    // =================== placeActiveSetup ===================

    @Test
    void placeActiveSetup_exitoso_retorna200() throws Exception {
        Partida p = partida();
        doNothing().when(battleEngine).colocarActivoSetup(any(), any(), any());
        when(battleEngine.getEstadoPartida(any(), any())).thenReturn(p);

        mockMvc.perform(post("/api/battle/m1/setup/place-active")
                .header("X-Username", "ash")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"cartaId\":\"poke-1\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void placeActiveSetup_error_retorna400() throws Exception {
        doThrow(new IllegalArgumentException("error"))
                .when(battleEngine).colocarActivoSetup(any(), any(), any());

        mockMvc.perform(post("/api/battle/m1/setup/place-active")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"cartaId\":\"poke-1\"}"))
                .andExpect(status().isBadRequest());
    }

    // =================== placeBenchSetup ===================

    @Test
    void placeBenchSetup_exitoso_retorna200() throws Exception {
        Partida p = partida();
        doNothing().when(battleEngine).colocarBancaSetup(any(), any(), any());
        when(battleEngine.getEstadoPartida(any(), any())).thenReturn(p);

        mockMvc.perform(post("/api/battle/m1/setup/place-bench")
                .header("X-Username", "ash")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"cartaId\":\"poke-2\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void placeBenchSetup_error_retorna400() throws Exception {
        doThrow(new RuntimeException("error"))
                .when(battleEngine).colocarBancaSetup(any(), any(), any());

        mockMvc.perform(post("/api/battle/m1/setup/place-bench")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"cartaId\":\"poke-2\"}"))
                .andExpect(status().isBadRequest());
    }

    // =================== usarHabilidad ===================

    @Test
    void usarHabilidad_exitoso_retorna200() throws Exception {
        doNothing().when(battleEngine).usarHabilidad(any(), any(), any(), any(), any(), any());

        UseAbilityRequest req = new UseAbilityRequest();
        req.setSourcePokemonId("poke-1");
        req.setAbilityName("Energize");
        req.setTargetPokemonId("poke-2");

        mockMvc.perform(post("/api/battle/m1/use-ability")
                .header("X-Username", "ash")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    @Test
    void usarHabilidad_error_retorna400() throws Exception {
        doThrow(new IllegalArgumentException("Habilidad no disponible"))
                .when(battleEngine).usarHabilidad(any(), any(), any(), any(), any(), any());

        UseAbilityRequest req = new UseAbilityRequest();
        req.setSourcePokemonId("poke-1");
        req.setAbilityName("Energize");

        mockMvc.perform(post("/api/battle/m1/use-ability")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    // =================== Error paths for partially-covered methods ===================

    @Test
    void jugarBotSetup_error_retorna400() throws Exception {
        doThrow(new RuntimeException("Error bot setup"))
                .when(battleEngine).ejecutarSetupBot("m1");

        mockMvc.perform(post("/api/battle/m1/jugar-bot-setup"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void actualizarHandshakeMoneda_error_retorna400() throws Exception {
        when(battleEngine.actualizarHandshakeMoneda(any(), any(), anyBoolean(), anyInt()))
                .thenThrow(new RuntimeException("error"));

        mockMvc.perform(post("/api/battle/m1/coin-handshake")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"holding\":true,\"power\":5}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void actualizarLoading_error_retorna400() throws Exception {
        when(battleEngine.actualizarLoading(any(), any(), anyInt()))
                .thenThrow(new RuntimeException("error"));

        mockMvc.perform(post("/api/battle/m1/loading")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"percentage\":50}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void evaluateSetup_error_retorna400() throws Exception {
        doThrow(new RuntimeException("error"))
                .when(battleEngine).evaluarSetupInitialDraw(any(), any());

        mockMvc.perform(post("/api/battle/m1/setup/evaluate"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void confirmBenchSetup_error_retorna400() throws Exception {
        doThrow(new RuntimeException("error"))
                .when(battleEngine).confirmarBancaSetup(any(), any());

        mockMvc.perform(post("/api/battle/m1/setup/confirm-bench"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void elegirTurno_error_retorna400() throws Exception {
        doThrow(new RuntimeException("error"))
                .when(battleEngine).elegirTurno(any(), anyBoolean(), any());

        mockMvc.perform(post("/api/battle/m1/choose-turn")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"vaPrimero\":true}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void jugarTrainer_error_retorna400() throws Exception {
        doThrow(new RuntimeException("error"))
                .when(battleEngine).jugarTrainer(any(), any(), any());

        mockMvc.perform(post("/api/battle/m1/play-trainer")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"cartaId\":\"trainer-1\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void unirEnergia_error_retorna400() throws Exception {
        doThrow(new RuntimeException("error"))
                .when(battleEngine).unirEnergia(any(), any(), any(), any(), any());

        UnirEnergiaRequest req = new UnirEnergiaRequest();
        req.setCartaId("poke-1");
        req.setEnergiaId("energy-1");

        mockMvc.perform(post("/api/battle/m1/attach-energy")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void promoteToActive_error_retorna400() throws Exception {
        doThrow(new RuntimeException("error"))
                .when(battleEngine).subirAActivoDesdeBanca(any(), any(), any());

        mockMvc.perform(post("/api/battle/m1/promote")
                .contentType(MediaType.APPLICATION_JSON)
                .content("\"poke-bench-1\""))
                .andExpect(status().isBadRequest());
    }

    @Test
    void retirarPokemon_error_retorna400() throws Exception {
        doThrow(new RuntimeException("error"))
                .when(battleEngine).realizarRetirada(any(), any(), any());

        mockMvc.perform(post("/api/battle/m1/retreat")
                .contentType(MediaType.APPLICATION_JSON)
                .content("\"bench-poke-1\""))
                .andExpect(status().isBadRequest());
    }

    @Test
    void resolveEffect_error_retorna400() throws Exception {
        when(battleEngine.resolverAccionPendiente(any(), any(), any()))
                .thenThrow(new RuntimeException("error"));

        mockMvc.perform(post("/api/battle/m1/resolve-effect")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"selectedIds\":[\"c1\"]}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void debugSetHp_error_retorna400() throws Exception {
        when(battleEngine.debugSetHp(any(), any(), anyInt(), any()))
                .thenThrow(new RuntimeException("error"));

        mockMvc.perform(post("/api/battle/m1/debug/hp")
                .header("X-Username", "ash")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"objetivo\":\"active\",\"hp\":100}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void debugForzarEstado_error_retorna400() throws Exception {
        when(battleEngine.debugForzarEstado(any(), any(), any(), any()))
                .thenThrow(new RuntimeException("error"));

        mockMvc.perform(post("/api/battle/m1/debug/status")
                .header("X-Username", "ash")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"objetivo\":\"active\",\"estado\":\"ASLEEP\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void debugDrawCard_error_retorna400() throws Exception {
        when(battleEngine.debugRobarCarta(any(), any(), any()))
                .thenThrow(new RuntimeException("error"));

        mockMvc.perform(post("/api/battle/m1/debug/draw")
                .header("X-Username", "ash")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"cardId\":\"xy1-1\"}"))
                .andExpect(status().isBadRequest());
    }
}
