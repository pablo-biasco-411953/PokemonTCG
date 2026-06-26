package com.pokemon.tcg.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pokemon.tcg.dto.PersonalizacionRequest;
import com.pokemon.tcg.dto.SantoroTrackingRequest;
import com.pokemon.tcg.dto.TradeExecutionRequest;
import com.pokemon.tcg.model.Card;
import com.pokemon.tcg.model.Jugador;
import com.pokemon.tcg.repository.JugadorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class JugadorControllerTest {

    private MockMvc mockMvc;
    private JugadorRepository jugadorRepo;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        jugadorRepo = mock(JugadorRepository.class);
        JugadorController controller = new JugadorController(jugadorRepo);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    private Jugador jugadorBase(String username) {
        Jugador j = new Jugador(username);
        j.setEmail(username + "@test.com");
        return j;
    }

    @Test
    void obtenerDatos_jugadorExiste_retorna200() throws Exception {
        Jugador j = jugadorBase("ash");
        when(jugadorRepo.findByUsername("ash")).thenReturn(j);

        mockMvc.perform(get("/api/jugadores/ash/datos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("ash"));
    }

    @Test
    void obtenerDatos_jugadorNoExiste_retorna404() throws Exception {
        when(jugadorRepo.findByUsername("noexiste")).thenReturn(null);

        mockMvc.perform(get("/api/jugadores/noexiste/datos"))
                .andExpect(status().isNotFound());
    }

    @Test
    void obtenerDatos_retornaSobresDisponibles() throws Exception {
        Jugador j = jugadorBase("ash");
        when(jugadorRepo.findByUsername("ash")).thenReturn(j);

        mockMvc.perform(get("/api/jugadores/ash/datos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sobresDisponibles").value(10));
    }

    @Test
    void guardarPersonalizacion_exitoso_retorna200() throws Exception {
        Jugador j = jugadorBase("ash");
        when(jugadorRepo.findByUsername("ash")).thenReturn(j);
        when(jugadorRepo.save(any())).thenReturn(j);

        PersonalizacionRequest req = new PersonalizacionRequest();
        req.setCharacterId("char1");
        req.setSkinColor("light");
        req.setHairColor("black");
        req.setEyeColor("brown");
        req.setHeight(1.75);
        req.setPikachuCompanion(true);

        mockMvc.perform(post("/api/jugadores/ash/personalizacion")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());
        verify(jugadorRepo).save(j);
    }

    @Test
    void guardarPersonalizacion_jugadorNoExiste_retorna404() throws Exception {
        when(jugadorRepo.findByUsername("noexiste")).thenReturn(null);

        PersonalizacionRequest req = new PersonalizacionRequest();

        mockMvc.perform(post("/api/jugadores/noexiste/personalizacion")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isNotFound());
    }

    @Test
    void obtenerColeccion_jugadorExiste_retorna200() throws Exception {
        Jugador j = jugadorBase("ash");
        Card c = new Card();
        c.setId("xy1-1");
        c.setNombre("Bulbasaur");
        j.getColeccion().add(c);
        when(jugadorRepo.findByUsername("ash")).thenReturn(j);

        mockMvc.perform(get("/api/jugadores/ash/coleccion"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("xy1-1"));
    }

    @Test
    void obtenerColeccion_jugadorNoExiste_retorna404() throws Exception {
        when(jugadorRepo.findByUsername("noexiste")).thenReturn(null);

        mockMvc.perform(get("/api/jugadores/noexiste/coleccion"))
                .andExpect(status().isNotFound());
    }

    @Test
    void spendCoins_suficientesPuntos_retorna200() throws Exception {
        Jugador j = jugadorBase("ash");
        when(jugadorRepo.findByUsername("ash")).thenReturn(j);
        when(jugadorRepo.save(any())).thenReturn(j);

        mockMvc.perform(post("/api/jugadores/ash/coins/spend")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("amount", 50))))
                .andExpect(status().isOk());
    }

    @Test
    void spendCoins_puntosInsuficientes_retorna400() throws Exception {
        Jugador j = jugadorBase("ash");
        when(jugadorRepo.findByUsername("ash")).thenReturn(j);

        mockMvc.perform(post("/api/jugadores/ash/coins/spend")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("amount", 9999))))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Santoropoints insuficientes"));
    }

    @Test
    void spendCoins_jugadorNoExiste_retorna404() throws Exception {
        when(jugadorRepo.findByUsername("noexiste")).thenReturn(null);

        mockMvc.perform(post("/api/jugadores/noexiste/coins/spend")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("amount", 10))))
                .andExpect(status().isNotFound());
    }

    @Test
    void buyPacks_bundle1_retorna200() throws Exception {
        Jugador j = jugadorBase("ash");
        when(jugadorRepo.findByUsername("ash")).thenReturn(j);
        when(jugadorRepo.save(any())).thenReturn(j);

        mockMvc.perform(post("/api/jugadores/ash/packs/buy")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("amount", 1))))
                .andExpect(status().isOk());
    }

    @Test
    void buyPacks_bundleNoDisponible_retorna400() throws Exception {
        Jugador j = jugadorBase("ash");
        when(jugadorRepo.findByUsername("ash")).thenReturn(j);

        mockMvc.perform(post("/api/jugadores/ash/packs/buy")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("amount", 4))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getSantoroQuest_retorna200() throws Exception {
        Jugador j = jugadorBase("ash");
        when(jugadorRepo.findByUsername("ash")).thenReturn(j);

        mockMvc.perform(get("/api/jugadores/ash/quests/santoro"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.giftClaimed").value(false));
    }

    @Test
    void claimSantoroGift_primeraClamada_agregaSobres() throws Exception {
        Jugador j = jugadorBase("ash");
        int sobresAntes = j.getSobresDisponibles();
        when(jugadorRepo.findByUsername("ash")).thenReturn(j);
        when(jugadorRepo.save(any())).thenReturn(j);

        mockMvc.perform(post("/api/jugadores/ash/quests/santoro/claim"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.giftClaimed").value(true));
    }

    @Test
    void claimSantoroGift_jugadorNoExiste_retorna404() throws Exception {
        when(jugadorRepo.findByUsername("noexiste")).thenReturn(null);

        mockMvc.perform(post("/api/jugadores/noexiste/quests/santoro/claim"))
                .andExpect(status().isNotFound());
    }

    @Test
    void executeTrade_exitoso_retorna200() throws Exception {
        Jugador jA = jugadorBase("ash");
        Jugador jB = jugadorBase("misty");
        Card carta = new Card();
        carta.setId("xy1-1");
        jA.getColeccion().add(carta);

        when(jugadorRepo.findByUsername("ash")).thenReturn(jA);
        when(jugadorRepo.findByUsername("misty")).thenReturn(jB);
        when(jugadorRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        TradeExecutionRequest req = new TradeExecutionRequest();
        req.setPlayerA("ash");
        req.setPlayerB("misty");
        req.setPlayerACardIds(List.of("xy1-1"));
        req.setPlayerBCardIds(List.of());

        mockMvc.perform(post("/api/jugadores/trade/execute")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    @Test
    void executeTrade_jugadorANoExiste_retorna404() throws Exception {
        when(jugadorRepo.findByUsername("noexiste")).thenReturn(null);

        TradeExecutionRequest req = new TradeExecutionRequest();
        req.setPlayerA("noexiste");
        req.setPlayerB("misty");

        mockMvc.perform(post("/api/jugadores/trade/execute")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isNotFound());
    }

    @Test
    void executeTrade_cartaNoEnColeccion_retorna400() throws Exception {
        Jugador jA = jugadorBase("ash");
        Jugador jB = jugadorBase("misty");

        when(jugadorRepo.findByUsername("ash")).thenReturn(jA);
        when(jugadorRepo.findByUsername("misty")).thenReturn(jB);

        TradeExecutionRequest req = new TradeExecutionRequest();
        req.setPlayerA("ash");
        req.setPlayerB("misty");
        req.setPlayerACardIds(List.of("noExisteId"));
        req.setPlayerBCardIds(List.of());

        mockMvc.perform(post("/api/jugadores/trade/execute")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }
}
