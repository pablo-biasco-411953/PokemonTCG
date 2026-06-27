package com.pokemon.tcg.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pokemon.tcg.dto.ActualizarMazoRequest;
import com.pokemon.tcg.dto.DebugInjectCardRequest;
import com.pokemon.tcg.dto.GuardarMazoRequest;
import com.pokemon.tcg.model.Jugador;
import com.pokemon.tcg.model.Mazo;
import com.pokemon.tcg.service.MazoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Collections;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class MazoControllerTest {

    private MockMvc mockMvc;
    private MazoService mazoService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mazoService = mock(MazoService.class);
        MazoController controller = new MazoController(mazoService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    private Mazo mazoBase() {
        Jugador jugador = new Jugador("ash");
        Mazo mazo = new Mazo("Bosque", jugador);
        mazo.setCartas(Collections.emptyList());
        return mazo;
    }

    @Test
    void guardarMazo_exitoso_retorna200() throws Exception {
        Mazo mazo = mazoBase();
        when(mazoService.guardarMazo(eq("Bosque"), eq("ash"), anyList())).thenReturn(mazo);

        GuardarMazoRequest req = new GuardarMazoRequest();
        req.setNombre("Bosque");
        req.setUsername("ash");
        req.setCartas(Collections.nCopies(60, "xy1-1"));

        mockMvc.perform(post("/api/mazos/guardar")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    @Test
    void guardarMazo_error_retorna400() throws Exception {
        when(mazoService.guardarMazo(any(), any(), any()))
                .thenThrow(new IllegalArgumentException("Un mazo debe contener exactamente 60 cartas."));

        GuardarMazoRequest req = new GuardarMazoRequest();
        req.setNombre("Bosque");
        req.setUsername("ash");
        req.setCartas(Collections.nCopies(10, "xy1-1"));

        mockMvc.perform(post("/api/mazos/guardar")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Un mazo debe contener exactamente 60 cartas."));
    }

    @Test
    void guardarMazo_jugadorNoEncontrado_retorna400() throws Exception {
        when(mazoService.guardarMazo(any(), eq("noexiste"), any()))
                .thenThrow(new IllegalArgumentException("Jugador no encontrado: noexiste"));

        GuardarMazoRequest req = new GuardarMazoRequest();
        req.setNombre("Bosque");
        req.setUsername("noexiste");
        req.setCartas(Collections.nCopies(60, "xy1-1"));

        mockMvc.perform(post("/api/mazos/guardar")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void listarMazos_retorna200ConLista() throws Exception {
        Mazo m1 = mazoBase();
        Mazo m2 = new Mazo("Fuego", new Jugador("ash"));
        when(mazoService.listarMazos("ash")).thenReturn(List.of(m1, m2));

        mockMvc.perform(get("/api/mazos/listar/ash"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void listarMazos_listaVacia_retorna200() throws Exception {
        when(mazoService.listarMazos("ash")).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/mazos/listar/ash"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void actualizarMazo_exitoso_retorna200() throws Exception {
        Mazo mazo = mazoBase();
        when(mazoService.actualizarMazo(eq(1L), eq("NuevoNombre"), anyList())).thenReturn(mazo);

        ActualizarMazoRequest req = new ActualizarMazoRequest();
        req.setNombre("NuevoNombre");
        req.setCartas(Collections.nCopies(60, "xy1-1"));

        mockMvc.perform(put("/api/mazos/actualizar/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    @Test
    void actualizarMazo_noEncontrado_retorna400() throws Exception {
        when(mazoService.actualizarMazo(eq(99L), any(), any()))
                .thenThrow(new RuntimeException("Mazo no encontrado con ID: 99"));

        ActualizarMazoRequest req = new ActualizarMazoRequest();
        req.setNombre("X");
        req.setCartas(Collections.nCopies(60, "xy1-1"));

        mockMvc.perform(put("/api/mazos/actualizar/99")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Error al actualizar el mazo: Mazo no encontrado con ID: 99"));
    }

    @Test
    void eliminarMazo_exitoso_retorna200() throws Exception {
        doNothing().when(mazoService).eliminarMazo(1L);

        mockMvc.perform(delete("/api/mazos/eliminar/1"))
                .andExpect(status().isOk());
    }

    @Test
    void eliminarMazo_noEncontrado_retorna400() throws Exception {
        doThrow(new IllegalArgumentException("Mazo no encontrado con ID: 99"))
                .when(mazoService).eliminarMazo(99L);

        mockMvc.perform(delete("/api/mazos/eliminar/99"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Error al eliminar el mazo: Mazo no encontrado con ID: 99"));
    }

    @Test
    void debugInyectarCarta_exitoso_retorna200() throws Exception {
        Mazo mazo = mazoBase();
        when(mazoService.debugInyectarCarta(1L, "xy1-5", "xy1-1")).thenReturn(mazo);

        DebugInjectCardRequest req = new DebugInjectCardRequest();
        req.setCartaId("xy1-5");
        req.setCartaAReemplazarId("xy1-1");

        mockMvc.perform(post("/api/mazos/1/debug/inject-card")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    @Test
    void debugInyectarCarta_noAdmin_retorna400() throws Exception {
        when(mazoService.debugInyectarCarta(1L, "xy1-5", null))
                .thenThrow(new SecurityException("Solo los administradores pueden usar God Mode."));

        DebugInjectCardRequest req = new DebugInjectCardRequest();
        req.setCartaId("xy1-5");

        mockMvc.perform(post("/api/mazos/1/debug/inject-card")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }
}
