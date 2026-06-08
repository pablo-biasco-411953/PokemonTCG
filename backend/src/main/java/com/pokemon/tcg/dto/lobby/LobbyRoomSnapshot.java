package com.pokemon.tcg.dto.lobby;

import com.pokemon.tcg.model.lobby.LobbyRoomChatMessage;
import com.pokemon.tcg.model.lobby.LobbyRoomReaction;
import com.pokemon.tcg.model.lobby.LobbyRoomStatus;
import java.util.ArrayList;
import java.util.List;

public class LobbyRoomSnapshot {
    private String id;
    private String name;
    private LobbyRoomStatus status;
    private boolean locked;
    private String ownerUsername;
    private String ownerDeckName;
    private boolean ownerReady;
    private String guestUsername;
    private String guestDeckName;
    private boolean guestReady;
    private boolean guestBot;
    private int playerCount;
    private int spectatorCount;
    private String matchId;
    private boolean canJoin;
    private boolean canSpectate;
    private long updatedAt;
    private List<LobbyRoomChatMessage> chat = new ArrayList<>();
    private List<LobbyRoomReaction> reactions = new ArrayList<>();

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public LobbyRoomStatus getStatus() { return status; }
    public void setStatus(LobbyRoomStatus status) { this.status = status; }
    public boolean isLocked() { return locked; }
    public void setLocked(boolean locked) { this.locked = locked; }
    public String getOwnerUsername() { return ownerUsername; }
    public void setOwnerUsername(String ownerUsername) { this.ownerUsername = ownerUsername; }
    public String getOwnerDeckName() { return ownerDeckName; }
    public void setOwnerDeckName(String ownerDeckName) { this.ownerDeckName = ownerDeckName; }
    public boolean isOwnerReady() { return ownerReady; }
    public void setOwnerReady(boolean ownerReady) { this.ownerReady = ownerReady; }
    public String getGuestUsername() { return guestUsername; }
    public void setGuestUsername(String guestUsername) { this.guestUsername = guestUsername; }
    public String getGuestDeckName() { return guestDeckName; }
    public void setGuestDeckName(String guestDeckName) { this.guestDeckName = guestDeckName; }
    public boolean isGuestReady() { return guestReady; }
    public void setGuestReady(boolean guestReady) { this.guestReady = guestReady; }
    public boolean isGuestBot() { return guestBot; }
    public void setGuestBot(boolean guestBot) { this.guestBot = guestBot; }
    public int getPlayerCount() { return playerCount; }
    public void setPlayerCount(int playerCount) { this.playerCount = playerCount; }
    public int getSpectatorCount() { return spectatorCount; }
    public void setSpectatorCount(int spectatorCount) { this.spectatorCount = spectatorCount; }
    public String getMatchId() { return matchId; }
    public void setMatchId(String matchId) { this.matchId = matchId; }
    public boolean isCanJoin() { return canJoin; }
    public void setCanJoin(boolean canJoin) { this.canJoin = canJoin; }
    public boolean isCanSpectate() { return canSpectate; }
    public void setCanSpectate(boolean canSpectate) { this.canSpectate = canSpectate; }
    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }
    public List<LobbyRoomChatMessage> getChat() { return chat; }
    public void setChat(List<LobbyRoomChatMessage> chat) { this.chat = chat; }
    public List<LobbyRoomReaction> getReactions() { return reactions; }
    public void setReactions(List<LobbyRoomReaction> reactions) { this.reactions = reactions; }
}
