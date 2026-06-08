package com.pokemon.tcg.model.lobby;

public class LobbyRoomChatMessage {
    private String sender;
    private String text;
    private long sentAt;
    private boolean system;

    public LobbyRoomChatMessage() {}

    public LobbyRoomChatMessage(String sender, String text, boolean system) {
        this.sender = sender;
        this.text = text;
        this.system = system;
        this.sentAt = System.currentTimeMillis();
    }

    public String getSender() { return sender; }
    public void setSender(String sender) { this.sender = sender; }
    public String getText() { return text; }
    public void setText(String text) { this.text = text; }
    public long getSentAt() { return sentAt; }
    public void setSentAt(long sentAt) { this.sentAt = sentAt; }
    public boolean isSystem() { return system; }
    public void setSystem(boolean system) { this.system = system; }
}
