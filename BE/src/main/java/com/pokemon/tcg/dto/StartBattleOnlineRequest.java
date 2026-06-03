package com.pokemon.tcg.dto;

public class StartBattleOnlineRequest {
    private String player1;
    private Long player1MazoId;
    private String player2;
    private Long player2MazoId;

    public StartBattleOnlineRequest() {}

    public String getPlayer1() { return player1; }
    public void setPlayer1(String player1) { this.player1 = player1; }

    public Long getPlayer1MazoId() { return player1MazoId; }
    public void setPlayer1MazoId(Long player1MazoId) { this.player1MazoId = player1MazoId; }

    public String getPlayer2() { return player2; }
    public void setPlayer2(String player2) { this.player2 = player2; }

    public Long getPlayer2MazoId() { return player2MazoId; }
    public void setPlayer2MazoId(Long player2MazoId) { this.player2MazoId = player2MazoId; }
}
