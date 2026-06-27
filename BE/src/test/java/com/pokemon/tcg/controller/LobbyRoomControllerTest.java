package com.pokemon.tcg.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pokemon.tcg.config.LobbyWebSocketHandler;
import com.pokemon.tcg.dto.lobby.LobbyRoomRequest;
import com.pokemon.tcg.dto.lobby.LobbyRoomSnapshot;
import com.pokemon.tcg.dto.lobby.LobbyRoomStartResponse;
import com.pokemon.tcg.service.LobbyRoomService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class LobbyRoomControllerTest {

    private MockMvc mockMvc;
    private LobbyRoomService lobbyRoomService;
    private LobbyWebSocketHandler lobbyWebSocketHandler;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        lobbyRoomService = mock(LobbyRoomService.class);
        lobbyWebSocketHandler = mock(LobbyWebSocketHandler.class);
        LobbyRoomController controller = new LobbyRoomController(lobbyRoomService, lobbyWebSocketHandler);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    private LobbyRoomSnapshot snapshotBase() {
        LobbyRoomSnapshot s = new LobbyRoomSnapshot();
        s.setId("room-123");
        s.setOwnerUsername("ash");
        return s;
    }

    @Test
    void listRooms_retorna200ConListaVacia() throws Exception {
        when(lobbyRoomService.listRooms(any())).thenReturn(List.of());

        mockMvc.perform(get("/api/lobby-rooms"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void listRooms_conHeader_retorna200() throws Exception {
        LobbyRoomSnapshot s = snapshotBase();
        when(lobbyRoomService.listRooms("ash")).thenReturn(List.of(s));

        mockMvc.perform(get("/api/lobby-rooms")
                .header("X-Username", "ash"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("room-123"));
    }

    @Test
    void getRoom_exitoso_retorna200() throws Exception {
        LobbyRoomSnapshot s = snapshotBase();
        when(lobbyRoomService.getRoom("room-123", "ash")).thenReturn(s);

        mockMvc.perform(get("/api/lobby-rooms/room-123")
                .header("X-Username", "ash"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("room-123"));
    }

    @Test
    void getRoom_noEncontrado_retorna400() throws Exception {
        when(lobbyRoomService.getRoom(eq("inexistente"), any()))
                .thenThrow(new IllegalArgumentException("Sala no encontrada."));

        mockMvc.perform(get("/api/lobby-rooms/inexistente"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Sala no encontrada."));
    }

    @Test
    void createRoom_exitoso_retorna200() throws Exception {
        LobbyRoomSnapshot s = snapshotBase();
        when(lobbyRoomService.createRoom(eq("ash"), any())).thenReturn(s);
        doNothing().when(lobbyWebSocketHandler).broadcastToAll(any());

        LobbyRoomRequest req = new LobbyRoomRequest();
        req.setUsername("ash");
        req.setRoomName("Sala de Ash");
        req.setMazoId(1L);

        mockMvc.perform(post("/api/lobby-rooms")
                .header("X-Username", "ash")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("room-123"));
    }

    @Test
    void createRoom_error_retorna400() throws Exception {
        when(lobbyRoomService.createRoom(any(), any()))
                .thenThrow(new IllegalArgumentException("Username requerido."));

        LobbyRoomRequest req = new LobbyRoomRequest();

        mockMvc.perform(post("/api/lobby-rooms")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void joinRoom_exitoso_retorna200() throws Exception {
        LobbyRoomSnapshot s = snapshotBase();
        when(lobbyRoomService.joinRoom(eq("room-123"), eq("misty"), any())).thenReturn(s);
        doNothing().when(lobbyWebSocketHandler).broadcastToAll(any());

        LobbyRoomRequest req = new LobbyRoomRequest();
        req.setUsername("misty");
        req.setMazoId(2L);

        mockMvc.perform(post("/api/lobby-rooms/room-123/join")
                .header("X-Username", "misty")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    @Test
    void joinRoom_salaLlena_retorna400() throws Exception {
        when(lobbyRoomService.joinRoom(any(), any(), any()))
                .thenThrow(new IllegalArgumentException("La sala ya esta llena."));

        LobbyRoomRequest req = new LobbyRoomRequest();
        req.setUsername("brock");

        mockMvc.perform(post("/api/lobby-rooms/room-123/join")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void leaveRoom_exitoso_retorna200() throws Exception {
        LobbyRoomSnapshot s = snapshotBase();
        when(lobbyRoomService.leaveRoom(eq("room-123"), eq("ash"))).thenReturn(s);
        doNothing().when(lobbyWebSocketHandler).broadcastToAll(any());

        LobbyRoomRequest req = new LobbyRoomRequest();
        req.setUsername("ash");

        mockMvc.perform(post("/api/lobby-rooms/room-123/leave")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    @Test
    void addBot_exitoso_retorna200() throws Exception {
        LobbyRoomSnapshot s = snapshotBase();
        when(lobbyRoomService.addBot(eq("room-123"), eq("ash"), any())).thenReturn(s);
        doNothing().when(lobbyWebSocketHandler).broadcastToAll(any());

        LobbyRoomRequest req = new LobbyRoomRequest();
        req.setUsername("ash");
        req.setBotDifficulty("NORMAL");

        mockMvc.perform(post("/api/lobby-rooms/room-123/bot")
                .header("X-Username", "ash")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    @Test
    void setReady_exitoso_retorna200() throws Exception {
        LobbyRoomSnapshot s = snapshotBase();
        when(lobbyRoomService.setReady(eq("room-123"), eq("ash"), anyBoolean(), any())).thenReturn(s);
        doNothing().when(lobbyWebSocketHandler).broadcastToAll(any());

        LobbyRoomRequest req = new LobbyRoomRequest();
        req.setUsername("ash");
        req.setReady(true);
        req.setMazoId(1L);

        mockMvc.perform(post("/api/lobby-rooms/room-123/ready")
                .header("X-Username", "ash")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    @Test
    void startRoom_exitoso_retorna200() throws Exception {
        LobbyRoomSnapshot s = snapshotBase();
        LobbyRoomStartResponse response = new LobbyRoomStartResponse();
        response.setRoom(s);
        when(lobbyRoomService.startRoom(eq("room-123"), eq("ash"))).thenReturn(response);
        doNothing().when(lobbyWebSocketHandler).broadcastToAll(any());

        LobbyRoomRequest req = new LobbyRoomRequest();
        req.setUsername("ash");

        mockMvc.perform(post("/api/lobby-rooms/room-123/start")
                .header("X-Username", "ash")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    @Test
    void startRoom_noListo_retorna400() throws Exception {
        when(lobbyRoomService.startRoom(any(), any()))
                .thenThrow(new IllegalArgumentException("No todos los jugadores estan listos."));

        LobbyRoomRequest req = new LobbyRoomRequest();
        req.setUsername("ash");

        mockMvc.perform(post("/api/lobby-rooms/room-123/start")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getRoomByMatch_exitoso_retorna200() throws Exception {
        LobbyRoomSnapshot s = snapshotBase();
        s.setMatchId("match-456");
        when(lobbyRoomService.getRoomByMatchId("match-456", null)).thenReturn(s);

        mockMvc.perform(get("/api/lobby-rooms/match/match-456"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.matchId").value("match-456"));
    }

    @Test
    void kickGuest_exitoso_retorna200() throws Exception {
        LobbyRoomSnapshot s = snapshotBase();
        when(lobbyRoomService.kickGuest(eq("room-123"), eq("ash"))).thenReturn(s);
        doNothing().when(lobbyWebSocketHandler).broadcastToAll(any());

        LobbyRoomRequest req = new LobbyRoomRequest();
        req.setUsername("ash");

        mockMvc.perform(post("/api/lobby-rooms/room-123/kick")
                .header("X-Username", "ash")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());
    }
}
