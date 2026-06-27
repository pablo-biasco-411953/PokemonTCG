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

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class LobbyRoomControllerMoreTest {

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

    private LobbyRoomSnapshot snapshot(String id) {
        LobbyRoomSnapshot s = new LobbyRoomSnapshot();
        s.setId(id);
        s.setOwnerUsername("ash");
        return s;
    }

    private LobbyRoomRequest req(String username) {
        LobbyRoomRequest r = new LobbyRoomRequest();
        r.setUsername(username);
        r.setText("test-text");
        return r;
    }

    // =================== spectateRoom ===================

    @Test
    void spectateRoom_exitoso_retorna200() throws Exception {
        LobbyRoomSnapshot room = snapshot("room-1");
        LobbyRoomStartResponse response = new LobbyRoomStartResponse(room, "match-1");
        when(lobbyRoomService.spectateRoom(eq("room-1"), any(), any())).thenReturn(response);

        mockMvc.perform(post("/api/lobby-rooms/room-1/spectate")
                .header("X-Username", "ash")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req("ash"))))
                .andExpect(status().isOk());

        verify(lobbyWebSocketHandler, atLeastOnce()).broadcastToAll(any());
    }

    @Test
    void spectateRoom_error_retorna400() throws Exception {
        when(lobbyRoomService.spectateRoom(any(), any(), any()))
                .thenThrow(new IllegalArgumentException("Sala llena"));

        mockMvc.perform(post("/api/lobby-rooms/room-1/spectate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req("ash"))))
                .andExpect(status().isBadRequest());
    }

    // =================== addChat ===================

    @Test
    void addChat_exitoso_retorna200() throws Exception {
        LobbyRoomSnapshot room = snapshot("room-1");
        when(lobbyRoomService.addChat(eq("room-1"), any(), any())).thenReturn(room);

        mockMvc.perform(post("/api/lobby-rooms/room-1/chat")
                .header("X-Username", "ash")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req("ash"))))
                .andExpect(status().isOk());

        verify(lobbyWebSocketHandler, atLeastOnce()).broadcastToAll(any());
    }

    @Test
    void addChat_error_retorna400() throws Exception {
        when(lobbyRoomService.addChat(any(), any(), any()))
                .thenThrow(new IllegalArgumentException("error"));

        mockMvc.perform(post("/api/lobby-rooms/room-1/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req("ash"))))
                .andExpect(status().isBadRequest());
    }

    // =================== addReaction ===================

    @Test
    void addReaction_exitoso_retorna200() throws Exception {
        LobbyRoomSnapshot room = snapshot("room-1");
        when(lobbyRoomService.addReaction(eq("room-1"), any(), any())).thenReturn(room);

        mockMvc.perform(post("/api/lobby-rooms/room-1/reaction")
                .header("X-Username", "ash")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req("ash"))))
                .andExpect(status().isOk());

        verify(lobbyWebSocketHandler, atLeastOnce()).broadcastToAll(any());
    }

    @Test
    void addReaction_error_retorna400() throws Exception {
        when(lobbyRoomService.addReaction(any(), any(), any()))
                .thenThrow(new RuntimeException("error"));

        mockMvc.perform(post("/api/lobby-rooms/room-1/reaction")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req("ash"))))
                .andExpect(status().isBadRequest());
    }

    // =================== addMatchReaction ===================

    @Test
    void addMatchReaction_exitoso_retorna200() throws Exception {
        LobbyRoomSnapshot room = snapshot("room-1");
        when(lobbyRoomService.addReactionByMatchId(eq("match-1"), any(), any())).thenReturn(room);

        mockMvc.perform(post("/api/lobby-rooms/match/match-1/reaction")
                .header("X-Username", "ash")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req("ash"))))
                .andExpect(status().isOk());

        verify(lobbyWebSocketHandler, atLeastOnce()).broadcastToAll(any());
    }

    @Test
    void addMatchReaction_error_retorna400() throws Exception {
        when(lobbyRoomService.addReactionByMatchId(any(), any(), any()))
                .thenThrow(new RuntimeException("error"));

        mockMvc.perform(post("/api/lobby-rooms/match/match-1/reaction")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req("ash"))))
                .andExpect(status().isBadRequest());
    }

    // =================== updateSettings ===================

    @Test
    void updateSettings_exitoso_retorna200() throws Exception {
        LobbyRoomSnapshot room = snapshot("room-1");
        when(lobbyRoomService.updateSettings(eq("room-1"), any(), any(), any())).thenReturn(room);

        LobbyRoomRequest r = req("ash");
        r.setTurnTimeSeconds(60);
        r.setBotDifficulty("HARD");

        mockMvc.perform(post("/api/lobby-rooms/room-1/settings")
                .header("X-Username", "ash")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(r)))
                .andExpect(status().isOk());

        verify(lobbyWebSocketHandler, atLeastOnce()).broadcastToAll(any());
    }

    @Test
    void updateSettings_error_retorna400() throws Exception {
        when(lobbyRoomService.updateSettings(any(), any(), any(), any()))
                .thenThrow(new IllegalArgumentException("error"));

        mockMvc.perform(post("/api/lobby-rooms/room-1/settings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req("ash"))))
                .andExpect(status().isBadRequest());
    }

    // =================== Error paths for partially-covered methods ===================

    @Test
    void leaveRoom_error_retorna400() throws Exception {
        when(lobbyRoomService.leaveRoom(any(), any()))
                .thenThrow(new RuntimeException("error"));

        mockMvc.perform(post("/api/lobby-rooms/room-1/leave")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req("ash"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void kickGuest_error_retorna400() throws Exception {
        when(lobbyRoomService.kickGuest(any(), any()))
                .thenThrow(new RuntimeException("error"));

        mockMvc.perform(post("/api/lobby-rooms/room-1/kick")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req("ash"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void addBot_error_retorna400() throws Exception {
        when(lobbyRoomService.addBot(any(), any(), any()))
                .thenThrow(new RuntimeException("error"));

        mockMvc.perform(post("/api/lobby-rooms/room-1/bot")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req("ash"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void setReady_error_retorna400() throws Exception {
        when(lobbyRoomService.setReady(any(), any(), anyBoolean(), any()))
                .thenThrow(new RuntimeException("error"));

        mockMvc.perform(post("/api/lobby-rooms/room-1/ready")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req("ash"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getRoomByMatch_error_retorna400() throws Exception {
        when(lobbyRoomService.getRoomByMatchId(any(), any()))
                .thenThrow(new RuntimeException("error"));

        mockMvc.perform(get("/api/lobby-rooms/match/match-1")
                .header("X-Username", "ash"))
                .andExpect(status().isBadRequest());
    }
}
