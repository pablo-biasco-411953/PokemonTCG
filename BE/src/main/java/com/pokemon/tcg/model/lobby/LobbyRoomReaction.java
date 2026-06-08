package com.pokemon.tcg.model.lobby;

public class LobbyRoomReaction {
    private String id;
    private String sender;
    private String reaction;
    private long sentAt;

    public LobbyRoomReaction() {}

    public LobbyRoomReaction(String id, String sender, String reaction, long sentAt) {
        this.id = id;
        this.sender = sender;
        this.reaction = reaction;
        this.sentAt = sentAt;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getSender() { return sender; }
    public void setSender(String sender) { this.sender = sender; }
    public String getReaction() { return reaction; }
    public void setReaction(String reaction) { this.reaction = reaction; }
    public long getSentAt() { return sentAt; }
    public void setSentAt(long sentAt) { this.sentAt = sentAt; }
}
