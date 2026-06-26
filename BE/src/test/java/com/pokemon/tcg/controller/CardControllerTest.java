package com.pokemon.tcg.controller;

import com.pokemon.tcg.model.Card;
import com.pokemon.tcg.service.CardCatalogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class CardControllerTest {

    private MockMvc mockMvc;
    private CardCatalogService cardCatalogService;

    @BeforeEach
    void setUp() {
        cardCatalogService = mock(CardCatalogService.class);
        CardController controller = new CardController(cardCatalogService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void getAll_sinLang_retorna200ConCartas() throws Exception {
        Card card = new Card();
        card.setId("xy1-1");
        card.setNombre("Bulbasaur");
        when(cardCatalogService.getCatalogo(null)).thenReturn(List.of(card));

        mockMvc.perform(get("/api/cards"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("xy1-1"))
                .andExpect(jsonPath("$[0].nombre").value("Bulbasaur"));
    }

    @Test
    void getAll_conLangEs_retorna200ConTraduccion() throws Exception {
        Card card = new Card();
        card.setId("xy1-1");
        card.setNombre("Bulbasaur");
        when(cardCatalogService.getCatalogo("es")).thenReturn(List.of(card));

        mockMvc.perform(get("/api/cards").param("lang", "es"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("xy1-1"));
    }

    @Test
    void getAll_conLangEn_retorna200() throws Exception {
        when(cardCatalogService.getCatalogo("en")).thenReturn(List.of());

        mockMvc.perform(get("/api/cards").param("lang", "en"))
                .andExpect(status().isOk());
    }

    @Test
    void getAll_listaVacia_retorna200() throws Exception {
        when(cardCatalogService.getCatalogo(null)).thenReturn(List.of());

        mockMvc.perform(get("/api/cards"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void getAll_errorEnServicio_retorna500() throws Exception {
        when(cardCatalogService.getCatalogo(null)).thenThrow(new RuntimeException("Error de BD"));

        mockMvc.perform(get("/api/cards"))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void getAll_multiplesCartas_retornaTodasLasCartas() throws Exception {
        Card c1 = new Card(); c1.setId("xy1-1"); c1.setNombre("Bulbasaur");
        Card c2 = new Card(); c2.setId("xy1-2"); c2.setNombre("Ivysaur");
        Card c3 = new Card(); c3.setId("xy1-3"); c3.setNombre("Weedle");
        when(cardCatalogService.getCatalogo(null)).thenReturn(List.of(c1, c2, c3));

        mockMvc.perform(get("/api/cards"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3));
    }
}
