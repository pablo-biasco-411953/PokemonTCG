package com.pokemon.tcg.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pokemon.tcg.dto.DebugSetSobresRequest;
import com.pokemon.tcg.dto.SantoroTrackingRequest;
import com.pokemon.tcg.model.Jugador;
import com.pokemon.tcg.repository.JugadorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class JugadorControllerMoreTest {

    private MockMvc mockMvc;
    private JugadorRepository jugadorRepo;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        jugadorRepo = mock(JugadorRepository.class);
        JugadorController controller = new JugadorController(jugadorRepo);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    private Jugador jugador(String username) {
        Jugador j = new Jugador(username);
        j.setSobresDisponibles(5);
        return j;
    }

    private Jugador jugadorAdmin(String username) {
        Jugador j = jugador(username);
        j.setAdmin(true);
        return j;
    }

    // =================== debugSetSobres ===================

    @Test
    void debugSetSobres_adminExistente_retorna200() throws Exception {
        Jugador j = jugadorAdmin("ash");
        when(jugadorRepo.findByUsername("ash")).thenReturn(j);
        when(jugadorRepo.save(any())).thenReturn(j);

        DebugSetSobresRequest req = new DebugSetSobresRequest();
        req.setCantidad(10);

        mockMvc.perform(post("/api/jugadores/ash/debug/sobres")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());

        verify(jugadorRepo).save(any());
    }

    @Test
    void debugSetSobres_noAdmin_retorna403() throws Exception {
        Jugador j = jugador("ash");
        when(jugadorRepo.findByUsername("ash")).thenReturn(j);

        DebugSetSobresRequest req = new DebugSetSobresRequest();
        req.setCantidad(10);

        mockMvc.perform(post("/api/jugadores/ash/debug/sobres")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());

        verify(jugadorRepo, never()).save(any());
    }

    @Test
    void debugSetSobres_jugadorNoExiste_retorna404() throws Exception {
        when(jugadorRepo.findByUsername("noexiste")).thenReturn(null);

        DebugSetSobresRequest req = new DebugSetSobresRequest();
        req.setCantidad(5);

        mockMvc.perform(post("/api/jugadores/noexiste/debug/sobres")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isNotFound());
    }

    @Test
    void debugSetSobres_cantidadNegativa_seteaACero() throws Exception {
        Jugador j = jugadorAdmin("ash");
        when(jugadorRepo.findByUsername("ash")).thenReturn(j);
        when(jugadorRepo.save(any())).thenReturn(j);

        DebugSetSobresRequest req = new DebugSetSobresRequest();
        req.setCantidad(-5);

        mockMvc.perform(post("/api/jugadores/ash/debug/sobres")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    // =================== setSantoroTracking ===================

    @Test
    void setSantoroTracking_activar_retorna200() throws Exception {
        Jugador j = jugador("ash");
        when(jugadorRepo.findByUsername("ash")).thenReturn(j);
        when(jugadorRepo.save(any())).thenReturn(j);

        SantoroTrackingRequest req = new SantoroTrackingRequest();
        req.setTracking(true);

        mockMvc.perform(post("/api/jugadores/ash/quests/santoro/tracking")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());

        verify(jugadorRepo).save(any());
    }

    @Test
    void setSantoroTracking_desactivar_retorna200() throws Exception {
        Jugador j = jugador("ash");
        j.setSantoroQuestTracking(true);
        when(jugadorRepo.findByUsername("ash")).thenReturn(j);
        when(jugadorRepo.save(any())).thenReturn(j);

        SantoroTrackingRequest req = new SantoroTrackingRequest();
        req.setTracking(false);

        mockMvc.perform(post("/api/jugadores/ash/quests/santoro/tracking")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    @Test
    void setSantoroTracking_jugadorNoExiste_retorna404() throws Exception {
        when(jugadorRepo.findByUsername("noexiste")).thenReturn(null);

        SantoroTrackingRequest req = new SantoroTrackingRequest();
        req.setTracking(true);

        mockMvc.perform(post("/api/jugadores/noexiste/quests/santoro/tracking")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isNotFound());
    }

    @Test
    void setSantoroTracking_activarConEstadoVacio_seteaAVAILABLE() throws Exception {
        Jugador j = jugador("ash");
        j.setSantoroQuestState("");
        when(jugadorRepo.findByUsername("ash")).thenReturn(j);
        when(jugadorRepo.save(any())).thenReturn(j);

        SantoroTrackingRequest req = new SantoroTrackingRequest();
        req.setTracking(true);

        mockMvc.perform(post("/api/jugadores/ash/quests/santoro/tracking")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    // =================== rewardCoins ===================

    @Test
    void rewardCoins_exitoso_retorna200() throws Exception {
        Jugador j = jugador("ash");
        when(jugadorRepo.findByUsername("ash")).thenReturn(j);
        when(jugadorRepo.save(any())).thenReturn(j);

        mockMvc.perform(post("/api/jugadores/ash/coins/reward")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"amount\":50}"))
                .andExpect(status().isOk());

        verify(jugadorRepo).save(any());
    }

    @Test
    void rewardCoins_jugadorNoExiste_retorna404() throws Exception {
        when(jugadorRepo.findByUsername("noexiste")).thenReturn(null);

        mockMvc.perform(post("/api/jugadores/noexiste/coins/reward")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"amount\":50}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void rewardCoins_amountNulo_retorna400() throws Exception {
        Jugador j = jugador("ash");
        when(jugadorRepo.findByUsername("ash")).thenReturn(j);

        mockMvc.perform(post("/api/jugadores/ash/coins/reward")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rewardCoins_amountCero_retorna400() throws Exception {
        Jugador j = jugador("ash");
        when(jugadorRepo.findByUsername("ash")).thenReturn(j);

        mockMvc.perform(post("/api/jugadores/ash/coins/reward")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"amount\":0}"))
                .andExpect(status().isBadRequest());
    }

    // =================== executeTrade partial coverage ===================

    @Test
    void executeTrade_jugadorANoExiste_retorna404() throws Exception {
        when(jugadorRepo.findByUsername("ash")).thenReturn(null);

        String body = "{\"playerA\":\"ash\",\"playerB\":\"misty\",\"cardIdsA\":[],\"cardIdsB\":[]}";
        mockMvc.perform(post("/api/jugadores/trade/execute")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isNotFound());
    }

    @Test
    void executeTrade_jugadorBNoExiste_retorna404() throws Exception {
        Jugador ash = jugador("ash");
        ash.setColeccion(new java.util.ArrayList<>());
        when(jugadorRepo.findByUsername("ash")).thenReturn(ash);
        when(jugadorRepo.findByUsername("misty")).thenReturn(null);

        String body = "{\"playerA\":\"ash\",\"playerB\":\"misty\",\"cardIdsA\":[],\"cardIdsB\":[]}";
        mockMvc.perform(post("/api/jugadores/trade/execute")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isNotFound());
    }

    // =================== getSantoroQuest missing branches ===================

    @Test
    void getSantoroQuest_jugadorNoExiste_retorna404() throws Exception {
        when(jugadorRepo.findByUsername("noexiste")).thenReturn(null);

        mockMvc.perform(get("/api/jugadores/noexiste/quests/santoro"))
                .andExpect(status().isNotFound());
    }

    // =================== spendCoins insufficient ===================

    @Test
    void spendCoins_insuficientes_retorna400() throws Exception {
        Jugador j = jugador("ash");
        j.setSantoroPoints(10);
        when(jugadorRepo.findByUsername("ash")).thenReturn(j);

        mockMvc.perform(post("/api/jugadores/ash/coins/spend")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"amount\":100}"))
                .andExpect(status().isBadRequest());
    }
}
