package com.pokemon.tcg.dto;

public class PersonalizacionRequest {
    private String characterId;
    private String skinColor;
    private String hairColor;
    private String eyeColor;
    private double height;
    private boolean pikachuCompanion;

    public PersonalizacionRequest() {}

    public String getCharacterId() { return characterId; }
    public void setCharacterId(String characterId) { this.characterId = characterId; }

    public String getSkinColor() { return skinColor; }
    public void setSkinColor(String skinColor) { this.skinColor = skinColor; }

    public String getHairColor() { return hairColor; }
    public void setHairColor(String hairColor) { this.hairColor = hairColor; }

    public String getEyeColor() { return eyeColor; }
    public void setEyeColor(String eyeColor) { this.eyeColor = eyeColor; }

    public double getHeight() { return height; }
    public void setHeight(double height) { this.height = height; }

    public boolean isPikachuCompanion() { return pikachuCompanion; }
    public void setPikachuCompanion(boolean pikachuCompanion) { this.pikachuCompanion = pikachuCompanion; }
}
