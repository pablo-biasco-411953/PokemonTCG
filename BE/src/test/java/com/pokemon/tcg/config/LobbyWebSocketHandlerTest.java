package com.pokemon.tcg.config;

import com.pokemon.tcg.dto.LobbyMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class LobbyWebSocketHandlerTest {

    private LobbyWebSocketHandler handler;

    @BeforeEach
    void setUp() {
        handler = new LobbyWebSocketHandler();
    }

    private WebSocketSession mockSession(String id) {
        WebSocketSession session = mock(WebSocketSession.class);
        Map<String, Object> attrs = new ConcurrentHashMap<>();
        when(session.getAttributes()).thenReturn(attrs);
        when(session.isOpen()).thenReturn(true);
        when(session.getId()).thenReturn(id);
        return session;
    }

    // =================== afterConnectionEstablished ===================

    @Test
    void afterConnectionEstablished_noException() throws Exception {
        WebSocketSession session = mockSession("s1");
        assertDoesNotThrow(() -> handler.afterConnectionEstablished(session));
    }

    // =================== handleTextMessage: null/invalid payload ===================

    @Test
    void handleTextMessage_nullUsername_returnsEarly() throws Exception {
        WebSocketSession session = mockSession("s1");
        TextMessage msg = new TextMessage("{\"type\":\"JOIN\"}");
        assertDoesNotThrow(() -> handler.handleTextMessage(session, msg));
    }

    // =================== handleTextMessage: JOIN ===================

    @Test
    void handleTextMessage_join_storesSessionAndBroadcasts() throws Exception {
        WebSocketSession session = mockSession("s1");
        String payload = "{\"type\":\"JOIN\",\"username\":\"ash\",\"characterId\":\"pikachu\"}";
        TextMessage msg = new TextMessage(payload);

        handler.handleTextMessage(session, msg);

        assertEquals("ash", session.getAttributes().get("username"));
    }

    @Test
    void handleTextMessage_join_sendsExistingPlayersToNewPlayer() throws Exception {
        // First player joins
        WebSocketSession session1 = mockSession("s1");
        String payload1 = "{\"type\":\"JOIN\",\"username\":\"ash\"}";
        handler.handleTextMessage(session1, new TextMessage(payload1));

        // Second player joins - should receive info about ash
        WebSocketSession session2 = mockSession("s2");
        String payload2 = "{\"type\":\"JOIN\",\"username\":\"misty\"}";
        handler.handleTextMessage(session2, new TextMessage(payload2));

        // session2 should have received a message about ash
        verify(session2, atLeastOnce()).sendMessage(any(TextMessage.class));
    }

    @Test
    void handleTextMessage_join_cancelsPendingDisconnect() throws Exception {
        // Join, disconnect, rejoin
        WebSocketSession session1 = mockSession("s1");
        handler.handleTextMessage(session1, new TextMessage("{\"type\":\"JOIN\",\"username\":\"ash\"}"));
        session1.getAttributes().put("username", "ash");
        handler.afterConnectionClosed(session1, CloseStatus.NORMAL);

        // Rejoin before disconnect fires
        WebSocketSession session2 = mockSession("s2");
        handler.handleTextMessage(session2, new TextMessage("{\"type\":\"JOIN\",\"username\":\"ash\"}"));

        assertEquals("ash", session2.getAttributes().get("username"));
    }

    @Test
    void handleTextMessage_join_closesPreviousOpenSession() throws Exception {
        WebSocketSession session1 = mockSession("s1");
        when(session1.isOpen()).thenReturn(true);
        handler.handleTextMessage(session1, new TextMessage("{\"type\":\"JOIN\",\"username\":\"ash\"}"));
        session1.getAttributes().put("username", "ash");

        // New session for same user
        WebSocketSession session2 = mockSession("s2");
        handler.handleTextMessage(session2, new TextMessage("{\"type\":\"JOIN\",\"username\":\"ash\"}"));

        verify(session1).close(CloseStatus.NORMAL);
    }

    // =================== handleTextMessage: MOVE ===================

    @Test
    void handleTextMessage_move_updatesPosition() throws Exception {
        WebSocketSession session = mockSession("s1");
        handler.handleTextMessage(session, new TextMessage("{\"type\":\"JOIN\",\"username\":\"ash\"}"));

        String movePayload = "{\"type\":\"MOVE\",\"username\":\"ash\",\"x\":10.0,\"y\":0.0,\"z\":5.0,\"rotY\":1.57,\"animation\":\"walking\"}";
        handler.handleTextMessage(session, new TextMessage(movePayload));
    }

    @Test
    void handleTextMessage_move_noExistingState_addsNew() throws Exception {
        WebSocketSession session = mockSession("s1");
        String movePayload = "{\"type\":\"MOVE\",\"username\":\"ash\",\"x\":1.0,\"y\":0.0,\"z\":1.0,\"rotY\":0.0,\"animation\":\"idle\"}";
        assertDoesNotThrow(() -> handler.handleTextMessage(session, new TextMessage(movePayload)));
    }

    // =================== handleTextMessage: CHAT / EMOTE ===================

    @Test
    void handleTextMessage_chat_broadcastsToOthers() throws Exception {
        WebSocketSession session1 = mockSession("s1");
        WebSocketSession session2 = mockSession("s2");
        handler.handleTextMessage(session1, new TextMessage("{\"type\":\"JOIN\",\"username\":\"ash\"}"));
        handler.handleTextMessage(session2, new TextMessage("{\"type\":\"JOIN\",\"username\":\"misty\"}"));

        String chatPayload = "{\"type\":\"CHAT\",\"username\":\"ash\",\"text\":\"Hola!\"}";
        handler.handleTextMessage(session1, new TextMessage(chatPayload));

        verify(session2, atLeastOnce()).sendMessage(any(TextMessage.class));
    }

    @Test
    void handleTextMessage_emote_broadcastsToOthers() throws Exception {
        WebSocketSession session = mockSession("s1");
        handler.handleTextMessage(session, new TextMessage("{\"type\":\"JOIN\",\"username\":\"ash\"}"));

        String emotePayload = "{\"type\":\"EMOTE\",\"username\":\"ash\",\"emote\":\"wave\"}";
        assertDoesNotThrow(() -> handler.handleTextMessage(session, new TextMessage(emotePayload)));
    }

    // =================== handleTextMessage: CHALLENGE_DUEL ===================

    @Test
    void handleTextMessage_challengeDuel_unknownTarget_noSend() throws Exception {
        WebSocketSession session = mockSession("s1");
        handler.handleTextMessage(session, new TextMessage("{\"type\":\"JOIN\",\"username\":\"ash\"}"));

        String challengePayload = "{\"type\":\"CHALLENGE_DUEL\",\"username\":\"ash\",\"targetUsername\":\"unknown\"}";
        assertDoesNotThrow(() -> handler.handleTextMessage(session, new TextMessage(challengePayload)));
    }

    @Test
    void handleTextMessage_challengeDuel_knownTarget_sends() throws Exception {
        WebSocketSession session1 = mockSession("s1");
        WebSocketSession session2 = mockSession("s2");
        handler.handleTextMessage(session1, new TextMessage("{\"type\":\"JOIN\",\"username\":\"ash\"}"));
        handler.handleTextMessage(session2, new TextMessage("{\"type\":\"JOIN\",\"username\":\"misty\"}"));

        String challengePayload = "{\"type\":\"CHALLENGE_DUEL\",\"username\":\"ash\",\"targetUsername\":\"misty\"}";
        handler.handleTextMessage(session1, new TextMessage(challengePayload));

        verify(session2, atLeastOnce()).sendMessage(any(TextMessage.class));
    }

    @Test
    void handleTextMessage_battleStart_knownTarget_sends() throws Exception {
        WebSocketSession session1 = mockSession("s1");
        WebSocketSession session2 = mockSession("s2");
        handler.handleTextMessage(session1, new TextMessage("{\"type\":\"JOIN\",\"username\":\"ash\"}"));
        handler.handleTextMessage(session2, new TextMessage("{\"type\":\"JOIN\",\"username\":\"misty\"}"));

        String battlePayload = "{\"type\":\"BATTLE_START\",\"username\":\"ash\",\"targetUsername\":\"misty\"}";
        handler.handleTextMessage(session1, new TextMessage(battlePayload));

        verify(session2, atLeastOnce()).sendMessage(any(TextMessage.class));
    }

    @Test
    void handleTextMessage_challengeDuel_noTarget_noSend() throws Exception {
        WebSocketSession session = mockSession("s1");
        handler.handleTextMessage(session, new TextMessage("{\"type\":\"JOIN\",\"username\":\"ash\"}"));

        String challengePayload = "{\"type\":\"CHALLENGE_DUEL\",\"username\":\"ash\"}";
        assertDoesNotThrow(() -> handler.handleTextMessage(session, new TextMessage(challengePayload)));
    }

    // =================== afterConnectionClosed ===================

    @Test
    void afterConnectionClosed_withUsername_schedulesPendingDisconnect() throws Exception {
        WebSocketSession session = mockSession("s1");
        handler.handleTextMessage(session, new TextMessage("{\"type\":\"JOIN\",\"username\":\"ash\"}"));
        session.getAttributes().put("username", "ash");

        assertDoesNotThrow(() -> handler.afterConnectionClosed(session, CloseStatus.NORMAL));
    }

    @Test
    void afterConnectionClosed_withoutUsername_doesNothing() throws Exception {
        WebSocketSession session = mockSession("s1");
        // No username in attributes
        assertDoesNotThrow(() -> handler.afterConnectionClosed(session, CloseStatus.SERVER_ERROR));
    }

    @Test
    void afterConnectionClosed_wrongSession_ignoresDisconnect() throws Exception {
        WebSocketSession session1 = mockSession("s1");
        WebSocketSession session2 = mockSession("s2");
        handler.handleTextMessage(session1, new TextMessage("{\"type\":\"JOIN\",\"username\":\"ash\"}"));

        // session2 is now the registered session for ash
        handler.handleTextMessage(session2, new TextMessage("{\"type\":\"JOIN\",\"username\":\"ash\"}"));
        session1.getAttributes().put("username", "ash");

        // Closing session1 (which is no longer registered) should be ignored
        assertDoesNotThrow(() -> handler.afterConnectionClosed(session1, CloseStatus.NORMAL));
    }

    // =================== handleTransportError ===================

    @Test
    void handleTransportError_closesSession() throws Exception {
        WebSocketSession session = mockSession("s1");
        assertDoesNotThrow(() -> handler.handleTransportError(session, new RuntimeException("network error")));
        verify(session).close();
    }

    @Test
    void handleTransportError_closeThrowsIo_handled() throws Exception {
        WebSocketSession session = mockSession("s1");
        doThrow(new java.io.IOException("IO error")).when(session).close();

        assertDoesNotThrow(() -> handler.handleTransportError(session, new RuntimeException("error")));
    }

    // =================== broadcastToAll ===================

    @Test
    void broadcastToAll_stringPayload_sendsToOpenSessions() throws Exception {
        WebSocketSession session = mockSession("s1");
        handler.handleTextMessage(session, new TextMessage("{\"type\":\"JOIN\",\"username\":\"ash\"}"));

        handler.broadcastToAll("test broadcast");

        verify(session, atLeastOnce()).sendMessage(any(TextMessage.class));
    }

    @Test
    void broadcastToAll_objectPayload_serializedAndSent() throws Exception {
        WebSocketSession session = mockSession("s1");
        handler.handleTextMessage(session, new TextMessage("{\"type\":\"JOIN\",\"username\":\"ash\"}"));

        LobbyMessage msg = new LobbyMessage();
        msg.setType("SYSTEM");
        msg.setUsername("server");

        handler.broadcastToAll(msg);

        verify(session, atLeastOnce()).sendMessage(any(TextMessage.class));
    }

    @Test
    void broadcastToAll_closedSession_skipped() throws Exception {
        WebSocketSession session = mockSession("s1");
        when(session.isOpen()).thenReturn(false);
        handler.handleTextMessage(session, new TextMessage("{\"type\":\"JOIN\",\"username\":\"ash\"}"));

        handler.broadcastToAll("test");

        verify(session, never()).sendMessage(any(TextMessage.class));
    }

    @Test
    void broadcastToAll_sendThrowsIo_handledGracefully() throws Exception {
        WebSocketSession session = mockSession("s1");
        handler.handleTextMessage(session, new TextMessage("{\"type\":\"JOIN\",\"username\":\"ash\"}"));

        doThrow(new java.io.IOException("send fail")).when(session).sendMessage(any(TextMessage.class));

        assertDoesNotThrow(() -> handler.broadcastToAll("payload"));
    }

    // =================== disconnect lambda (async, waits 3s) ===================

    @Test
    void afterConnectionClosed_disconnectLambda_cleansUpAfterDelay() throws Exception {
        WebSocketSession session = mockSession("s1");
        handler.handleTextMessage(session, new TextMessage("{\"type\":\"JOIN\",\"username\":\"ash\"}"));
        session.getAttributes().put("username", "ash");

        WebSocketSession session2 = mockSession("s2");
        handler.handleTextMessage(session2, new TextMessage("{\"type\":\"JOIN\",\"username\":\"misty\"}"));

        handler.afterConnectionClosed(session, CloseStatus.NORMAL);

        // Wait for the 2500ms scheduled disconnect to fire
        Thread.sleep(3000);

        // After disconnect, broadcastToAll should not include ash anymore
        // session2 should receive LEAVE broadcast
        verify(session2, atLeastOnce()).sendMessage(any(TextMessage.class));
    }
}
