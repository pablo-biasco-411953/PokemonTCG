package com.pokemon.tcg.dto;

public class JugarEntrenadorRequest {
    private String cartaId;
    private String objetivoId;

    public JugarEntrenadorRequest() {}

    public JugarEntrenadorRequest(String cartaId, String objetivoId) {
        this.cartaId = cartaId;
        this.objetivoId = objetivoId;
    }

    public String getCartaId() { return cartaId; }
    public void setCartaId(String cartaId) { this.cartaId = cartaId; }
    public String getObjetivoId() { return objetivoId; }
    public void setObjetivoId(String objetivoId) { this.objetivoId = objetivoId; }
}
