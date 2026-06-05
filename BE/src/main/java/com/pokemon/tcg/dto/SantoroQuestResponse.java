package com.pokemon.tcg.dto;

public class SantoroQuestResponse {
    private boolean giftClaimed;
    private boolean tracking;
    private String state;
    private int sobresDisponibles;

    public SantoroQuestResponse(boolean giftClaimed, boolean tracking, String state, int sobresDisponibles) {
        this.giftClaimed = giftClaimed;
        this.tracking = tracking;
        this.state = state;
        this.sobresDisponibles = sobresDisponibles;
    }

    public boolean isGiftClaimed() { return giftClaimed; }
    public boolean isTracking() { return tracking; }
    public String getState() { return state; }
    public int getSobresDisponibles() { return sobresDisponibles; }
}
