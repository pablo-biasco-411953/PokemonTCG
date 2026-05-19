package com.pokemon.tcg.dto;

public class DebugInjectCardRequest {
    private String cartaId;
    private String cartaAReemplazarId;

    public String getCartaId() {
        return cartaId;
    }

    public void setCartaId(String cartaId) {
        this.cartaId = cartaId;
    }

    public String getCartaAReemplazarId() {
        return cartaAReemplazarId;
    }

    public void setCartaAReemplazarId(String cartaAReemplazarId) {
        this.cartaAReemplazarId = cartaAReemplazarId;
    }
}
