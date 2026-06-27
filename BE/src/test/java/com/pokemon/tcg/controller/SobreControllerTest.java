package com.pokemon.tcg.controller;

import com.pokemon.tcg.model.Card;
import com.pokemon.tcg.service.SobreService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class SobreControllerTest {

    private MockMvc mockMvc;
    private SobreService sobreService;

    @BeforeEach
    void setUp() {
        sobreService = mock(SobreService.class);
        SobreController controller = new SobreController(sobreService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void abrirSobre_exitoso_retorna200ConCartas() throws Exception {
        Card c1 = new Card(); c1.setId("xy1-1"); c1.setNombre("Bulbasaur");
        Card c2 = new Card(); c2.setId("xy1-2"); c2.setNombre("Ivysaur");
        when(sobreService.abrirSobre("ash")).thenReturn(List.of(c1, c2));

        mockMvc.perform(post("/api/sobres/abrir/ash"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value("xy1-1"));
    }

    @Test
    void abrirSobre_jugadorNoEncontrado_retorna400() throws Exception {
        when(sobreService.abrirSobre("noexiste"))
                .thenThrow(new IllegalArgumentException("Jugador no encontrado: noexiste"));

        mockMvc.perform(post("/api/sobres/abrir/noexiste"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Error al abrir el sobre: Jugador no encontrado: noexiste"));
    }

    @Test
    void abrirSobre_sinSobres_retorna400() throws Exception {
        when(sobreService.abrirSobre("ash"))
                .thenThrow(new IllegalStateException("No hay sobres disponibles para el jugador: ash"));

        mockMvc.perform(post("/api/sobres/abrir/ash"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Error al abrir el sobre: No hay sobres disponibles para el jugador: ash"));
    }

    @Test
    void abrirSobre_retorna10Cartas_normalmente() throws Exception {
        List<Card> cartas = new java.util.ArrayList<>();
        for (int i = 0; i < 10; i++) {
            Card c = new Card();
            c.setId("xy1-" + (i + 1));
            cartas.add(c);
        }
        when(sobreService.abrirSobre("ash")).thenReturn(cartas);

        mockMvc.perform(post("/api/sobres/abrir/ash"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(10));
    }

    @Test
    void abrirSobre_sinBDConCartas_retorna400() throws Exception {
        when(sobreService.abrirSobre("ash"))
                .thenThrow(new IllegalStateException("La base de datos no tiene suficientes cartas cargadas."));

        mockMvc.perform(post("/api/sobres/abrir/ash"))
                .andExpect(status().isBadRequest());
    }
}
