package com.pokemon.tcg.dto.lobby;

public class LobbyRoomRequest {
    private String username;
    private String roomName;
    private Long mazoId;
    private String deckName;
    private String password;
    private boolean ready;
    private String text;
    private String botDifficulty;
    private Integer turnTimeSeconds;

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getRoomName() { return roomName; }
    public void setRoomName(String roomName) { this.roomName = roomName; }
    public Long getMazoId() { return mazoId; }
    public void setMazoId(Long mazoId) { this.mazoId = mazoId; }
    public String getDeckName() { return deckName; }
    public void setDeckName(String deckName) { this.deckName = deckName; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public boolean isReady() { return ready; }
    public void setReady(boolean ready) { this.ready = ready; }
    public String getText() { return text; }
    public void setText(String text) { this.text = text; }
    public String getBotDifficulty() { return botDifficulty; }
    public void setBotDifficulty(String botDifficulty) { this.botDifficulty = botDifficulty; }
    public Integer getTurnTimeSeconds() { return turnTimeSeconds; }
    public void setTurnTimeSeconds(Integer turnTimeSeconds) { this.turnTimeSeconds = turnTimeSeconds; }
}
