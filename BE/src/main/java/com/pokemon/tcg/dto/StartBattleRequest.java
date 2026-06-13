package com.pokemon.tcg.dto;

public class StartBattleRequest {
    private Long mazoId;
    private String botDifficulty;
    public Long getMazoId() { return mazoId; }
    public void setMazoId(Long mazoId) { this.mazoId = mazoId; }
    public String getBotDifficulty() { return botDifficulty; }
    public void setBotDifficulty(String botDifficulty) { this.botDifficulty = botDifficulty; }
}
