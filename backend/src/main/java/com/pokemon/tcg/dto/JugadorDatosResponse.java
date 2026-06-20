package com.pokemon.tcg.dto;

public class JugadorDatosResponse {
    private String username;
    private int sobresDisponibles;
    private int santoroPoints;
    private int cantidadCartas;
    
    private String characterId;
    private String skinColor;
    private String hairColor;
    private String eyeColor;
    private double height;
    private boolean pikachuCompanion;
    private boolean admin;

    public JugadorDatosResponse(String username, int sobresDisponibles, int cantidadCartas, boolean admin) {
        this.username = username;
        this.sobresDisponibles = sobresDisponibles;
        this.santoroPoints = 200;
        this.cantidadCartas = cantidadCartas;
        this.admin = admin;
    }

    public JugadorDatosResponse(String username, int sobresDisponibles, int cantidadCartas,
                                int santoroPoints,
                                String characterId, String skinColor, String hairColor,
                                String eyeColor, double height, boolean pikachuCompanion,
                                boolean admin) {
        this.username = username;
        this.sobresDisponibles = sobresDisponibles;
        this.santoroPoints = santoroPoints;
        this.cantidadCartas = cantidadCartas;
        this.characterId = characterId;
        this.skinColor = skinColor;
        this.hairColor = hairColor;
        this.eyeColor = eyeColor;
        this.height = height;
        this.pikachuCompanion = pikachuCompanion;
        this.admin = admin;
    }

    // Getters
    public String getUsername() { return username; }
    public int getSobresDisponibles() { return sobresDisponibles; }
    public int getSantoroPoints() { return santoroPoints; }
    public int getCantidadCartas() { return cantidadCartas; }

    public String getCharacterId() { return characterId; }
    public String getSkinColor() { return skinColor; }
    public String getHairColor() { return hairColor; }
    public String getEyeColor() { return eyeColor; }
    public double getHeight() { return height; }
    public boolean isPikachuCompanion() { return pikachuCompanion; }
    public boolean isAdmin() { return admin; }
}
