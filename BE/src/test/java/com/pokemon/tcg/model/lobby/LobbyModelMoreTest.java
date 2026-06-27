package com.pokemon.tcg.model.lobby;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class LobbyModelMoreTest {

    // =================== LobbyRoomChatMessage ===================

    @Test
    void chatMessage_3argConstructor() {
        LobbyRoomChatMessage msg = new LobbyRoomChatMessage("ash", "Hola!", false);

        assertEquals("ash", msg.getSender());
        assertEquals("Hola!", msg.getText());
        assertFalse(msg.isSystem());
        assertTrue(msg.getSentAt() > 0);
    }

    @Test
    void chatMessage_defaultConstructorAndSetters() {
        LobbyRoomChatMessage msg = new LobbyRoomChatMessage();
        msg.setSender("misty");
        msg.setText("¡Buen juego!");
        msg.setSentAt(123456789L);
        msg.setSystem(true);

        assertEquals("misty", msg.getSender());
        assertEquals("¡Buen juego!", msg.getText());
        assertEquals(123456789L, msg.getSentAt());
        assertTrue(msg.isSystem());
    }

    // =================== LobbyRoomReaction ===================

    @Test
    void reaction_4argConstructor() {
        LobbyRoomReaction r = new LobbyRoomReaction("r1", "brock", "👍", 999L);

        assertEquals("r1", r.getId());
        assertEquals("brock", r.getSender());
        assertEquals("👍", r.getReaction());
        assertEquals(999L, r.getSentAt());
    }

    @Test
    void reaction_defaultConstructorAndSetters() {
        LobbyRoomReaction r = new LobbyRoomReaction();
        r.setId("r2");
        r.setSender("ash");
        r.setReaction("❤️");
        r.setSentAt(111L);

        assertEquals("r2", r.getId());
        assertEquals("ash", r.getSender());
        assertEquals("❤️", r.getReaction());
        assertEquals(111L, r.getSentAt());
    }

    // =================== LobbyRoom remaining getters ===================

    @Test
    void lobbyRoom_remainingGettersSetters() {
        LobbyRoom room = new LobbyRoom();
        room.setPasswordHash("hash123");
        room.setMatchId("match-abc");
        room.setCreatedAt(1000L);
        room.setUpdatedAt(2000L);
        room.setSpectators(Set.of("spectator1"));
        room.setChat(List.of(new LobbyRoomChatMessage("a", "b", false)));
        room.setReactions(List.of(new LobbyRoomReaction("r1", "a", "👍", 1L)));

        assertEquals("hash123", room.getPasswordHash());
        assertEquals("match-abc", room.getMatchId());
        assertEquals(1000L, room.getCreatedAt());
        assertEquals(2000L, room.getUpdatedAt());
        assertEquals(1, room.getSpectators().size());
        assertEquals(1, room.getChat().size());
        assertEquals(1, room.getReactions().size());
    }

    @Test
    void lobbyRoom_turnTimeSecondsClamped() {
        LobbyRoom room = new LobbyRoom();
        room.setTurnTimeSeconds(-5);
        assertEquals(0, room.getTurnTimeSeconds());

        room.setTurnTimeSeconds(30);
        assertEquals(30, room.getTurnTimeSeconds());
    }
}
