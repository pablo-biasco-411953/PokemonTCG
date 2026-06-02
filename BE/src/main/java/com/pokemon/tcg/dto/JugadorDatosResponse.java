package com.pokemon.tcg.dto;

public class JugadorDatosResponse {
    private String username;
    private int sobresDisponibles;
    private int cantidadCartas;
    
    private String characterId;
    private String skinColor;
    private String hairColor;
    private String eyeColor;
    private double height;
    private boolean pikachuCompanion;

    public JugadorDatosResponse(String username, int sobresDisponibles, int cantidadCartas) {
        this.username = username;
        this.sobresDisponibles = sobresDisponibles;
        this.cantidadCartas = cantidadCartas;
    }

    public JugadorDatosResponse(String username, int sobresDisponibles, int cantidadCartas,
                                String characterId, String skinColor, String hairColor,
                                String eyeColor, double height, boolean pikachuCompanion) {
        this.username = username;
        this.sobresDisponibles = sobresDisponibles;
        this.cantidadCartas = cantidadCartas;
        this.characterId = characterId;
        this.skinColor = skinColor;
        this.hairColor = hairColor;
        this.eyeColor = eyeColor;
        this.height = height;
        this.pikachuCompanion = pikachuCompanion;
    }

    // Getters
    public String getUsername() { return username; }
    public int getSobresDisponibles() { return sobresDisponibles; }
    public int getCantidadCartas() { return cantidadCartas; }

    public String getCharacterId() { return characterId; }
    public String getSkinColor() { return skinColor; }
    public String getHairColor() { return hairColor; }
    public String getEyeColor() { return eyeColor; }
    public double getHeight() { return height; }
    public boolean isPikachuCompanion() { return pikachuCompanion; }
}
