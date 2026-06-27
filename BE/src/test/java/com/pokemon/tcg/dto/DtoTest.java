package com.pokemon.tcg.dto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DtoTest {

    // =================== LobbyMessage ===================

    @Test
    void lobbyMessage_settersGetters() {
        LobbyMessage msg = new LobbyMessage();
        msg.setType("JOIN");
        msg.setUsername("ash");
        msg.setCharacterId("char1");
        msg.setSkinColor("#fff");
        msg.setHairColor("#000");
        msg.setEyeColor("#blue");
        msg.setHeight(1.75);
        msg.setPikachuCompanion(true);
        msg.setX(10.0);
        msg.setY(0.0);
        msg.setZ(-5.0);
        msg.setRotY(180.0);
        msg.setAnimation("walking");

        assertEquals("JOIN", msg.getType());
        assertEquals("ash", msg.getUsername());
        assertEquals("char1", msg.getCharacterId());
        assertEquals("#fff", msg.getSkinColor());
        assertEquals("#000", msg.getHairColor());
        assertEquals("#blue", msg.getEyeColor());
        assertEquals(1.75, msg.getHeight());
        assertTrue(msg.isPikachuCompanion());
        assertEquals(10.0, msg.getX());
        assertEquals(0.0, msg.getY());
        assertEquals(-5.0, msg.getZ());
        assertEquals(180.0, msg.getRotY());
        assertEquals("walking", msg.getAnimation());
    }

    @Test
    void lobbyMessage_textYEmote() {
        LobbyMessage msg = new LobbyMessage();
        msg.setText("Hello!");
        msg.setEmote("wave");

        assertEquals("Hello!", msg.getText());
        assertEquals("wave", msg.getEmote());
    }

    @Test
    void lobbyMessage_challengeFields() {
        LobbyMessage msg = new LobbyMessage();
        msg.setTargetUsername("misty");
        msg.setChallengeId("ch-123");
        msg.setAccepted(true);
        msg.setDetails("Battle accepted");

        assertEquals("misty", msg.getTargetUsername());
        assertEquals("ch-123", msg.getChallengeId());
        assertTrue(msg.isAccepted());
        assertEquals("Battle accepted", msg.getDetails());
    }

    @Test
    void lobbyMessage_defaultConstructor() {
        LobbyMessage msg = new LobbyMessage();

        assertNull(msg.getType());
        assertNull(msg.getUsername());
        assertEquals(0.0, msg.getX());
    }

    // =================== SantoroTrackingRequest ===================

    @Test
    void santoroTrackingRequest_settersGetters() {
        SantoroTrackingRequest req = new SantoroTrackingRequest();
        req.setTracking(true);

        assertTrue(req.isTracking());
    }

    @Test
    void santoroTrackingRequest_defaultFalse() {
        SantoroTrackingRequest req = new SantoroTrackingRequest();
        assertFalse(req.isTracking());
    }

    // =================== DebugSetSobresRequest ===================

    @Test
    void debugSetSobresRequest_settersGetters() {
        DebugSetSobresRequest req = new DebugSetSobresRequest();
        req.setCantidad(10);

        assertEquals(10, req.getCantidad());
    }

    @Test
    void debugSetSobresRequest_defaultCero() {
        DebugSetSobresRequest req = new DebugSetSobresRequest();
        assertEquals(0, req.getCantidad());
    }
}
