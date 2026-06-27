package com.pokemon.tcg.dto;

public class UnirEnergiaRequest {
    private String cartaId;
    private String energiaId;
    private String selectedType;

    public String getCartaId() { return cartaId; }
    public void setCartaId(String cartaId) { this.cartaId = cartaId; }

    public String getEnergiaId() { return energiaId; }
    public void setEnergiaId(String energiaId) { this.energiaId = energiaId; }

    public String getSelectedType() { return selectedType; }
    public void setSelectedType(String selectedType) { this.selectedType = selectedType; }
}
