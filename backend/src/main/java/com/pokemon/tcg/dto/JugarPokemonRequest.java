package com.pokemon.tcg.dto;

public class JugarPokemonRequest {
    private String cartaId;
    private int posicion;
    public String getCartaId() { return cartaId; }
    public void setCartaId(String cartaId) { this.cartaId = cartaId; }
    public int getPosicion() { return posicion; }
    public void setPosicion(int posicion) { this.posicion = posicion; }
}
