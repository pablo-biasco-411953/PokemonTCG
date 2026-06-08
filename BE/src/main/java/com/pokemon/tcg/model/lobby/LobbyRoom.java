package com.pokemon.tcg.model.lobby;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class LobbyRoom {
    private String id = UUID.randomUUID().toString();
    private String name;
    private String ownerUsername;
    private Long ownerMazoId;
    private String ownerDeckName;
    private boolean ownerReady;
    private String guestUsername;
    private Long guestMazoId;
    private String guestDeckName;
    private boolean guestReady;
    private boolean guestBot;
    private boolean hasPassword;
    private String passwordHash;
    private LobbyRoomStatus status = LobbyRoomStatus.OPEN;
    private String matchId;
    private long createdAt = System.currentTimeMillis();
    private long updatedAt = System.currentTimeMillis();
    private Set<String> spectators = new LinkedHashSet<>();
    private List<LobbyRoomChatMessage> chat = new ArrayList<>();
    private List<LobbyRoomReaction> reactions = new ArrayList<>();

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getOwnerUsername() { return ownerUsername; }
    public void setOwnerUsername(String ownerUsername) { this.ownerUsername = ownerUsername; }
    public Long getOwnerMazoId() { return ownerMazoId; }
    public void setOwnerMazoId(Long ownerMazoId) { this.ownerMazoId = ownerMazoId; }
    public String getOwnerDeckName() { return ownerDeckName; }
    public void setOwnerDeckName(String ownerDeckName) { this.ownerDeckName = ownerDeckName; }
    public boolean isOwnerReady() { return ownerReady; }
    public void setOwnerReady(boolean ownerReady) { this.ownerReady = ownerReady; }
    public String getGuestUsername() { return guestUsername; }
    public void setGuestUsername(String guestUsername) { this.guestUsername = guestUsername; }
    public Long getGuestMazoId() { return guestMazoId; }
    public void setGuestMazoId(Long guestMazoId) { this.guestMazoId = guestMazoId; }
    public String getGuestDeckName() { return guestDeckName; }
    public void setGuestDeckName(String guestDeckName) { this.guestDeckName = guestDeckName; }
    public boolean isGuestReady() { return guestReady; }
    public void setGuestReady(boolean guestReady) { this.guestReady = guestReady; }
    public boolean isGuestBot() { return guestBot; }
    public void setGuestBot(boolean guestBot) { this.guestBot = guestBot; }
    public boolean isHasPassword() { return hasPassword; }
    public void setHasPassword(boolean hasPassword) { this.hasPassword = hasPassword; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public LobbyRoomStatus getStatus() { return status; }
    public void setStatus(LobbyRoomStatus status) { this.status = status; }
    public String getMatchId() { return matchId; }
    public void setMatchId(String matchId) { this.matchId = matchId; }
    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }
    public Set<String> getSpectators() { return spectators; }
    public void setSpectators(Set<String> spectators) { this.spectators = spectators; }
    public List<LobbyRoomChatMessage> getChat() { return chat; }
    public void setChat(List<LobbyRoomChatMessage> chat) { this.chat = chat; }
    public List<LobbyRoomReaction> getReactions() { return reactions; }
    public void setReactions(List<LobbyRoomReaction> reactions) { this.reactions = reactions; }
}
