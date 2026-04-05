package com.pokemon.tcg.dto;

public class EvolveRequest {
    private String cartaManoId;
    private String cartaTableroId;

    public String getCartaManoId() { return cartaManoId; }
    public void setCartaManoId(String cartaManoId) { this.cartaManoId = cartaManoId; }

    public String getCartaTableroId() { return cartaTableroId; }
    public void setCartaTableroId(String cartaTableroId) { this.cartaTableroId = cartaTableroId; }
}