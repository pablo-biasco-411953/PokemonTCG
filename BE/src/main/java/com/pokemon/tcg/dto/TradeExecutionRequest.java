package com.pokemon.tcg.dto;

import java.util.List;

public class TradeExecutionRequest {
    private String playerA;
    private String playerB;
    private List<String> playerACardIds;
    private List<String> playerBCardIds;

    public String getPlayerA() { return playerA; }
    public void setPlayerA(String playerA) { this.playerA = playerA; }

    public String getPlayerB() { return playerB; }
    public void setPlayerB(String playerB) { this.playerB = playerB; }

    public List<String> getPlayerACardIds() { return playerACardIds; }
    public void setPlayerACardIds(List<String> playerACardIds) { this.playerACardIds = playerACardIds; }

    public List<String> getPlayerBCardIds() { return playerBCardIds; }
    public void setPlayerBCardIds(List<String> playerBCardIds) { this.playerBCardIds = playerBCardIds; }
}
