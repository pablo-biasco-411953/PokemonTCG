package com.pokemon.tcg.dto.lobby;

public class LobbyRoomStartResponse {
    private LobbyRoomSnapshot room;
    private String matchId;

    public LobbyRoomStartResponse() {}

    public LobbyRoomStartResponse(LobbyRoomSnapshot room, String matchId) {
        this.room = room;
        this.matchId = matchId;
    }

    public LobbyRoomSnapshot getRoom() { return room; }
    public void setRoom(LobbyRoomSnapshot room) { this.room = room; }
    public String getMatchId() { return matchId; }
    public void setMatchId(String matchId) { this.matchId = matchId; }
}
