package com.pokemon.tcg.dto;

public class LobbyMessage {
    private String type; // "JOIN" | "MOVE" | "LEAVE"
    private String username;
    private String characterId;
    private String skinColor;
    private String hairColor;
    private String eyeColor;
    private double height;
    private boolean pikachuCompanion;
    private double x;
    private double y;
    private double z;
    private double rotY;
    private String animation; // "idle" | "walking" | "running"

    // Default constructor for Jackson JSON deserialization
    public LobbyMessage() {}

    // Getters and Setters
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

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

    public double getX() { return x; }
    public void setX(double x) { this.x = x; }

    public double getY() { return y; }
    public void setY(double y) { this.y = y; }

    public double getZ() { return z; }
    public void setZ(double z) { this.z = z; }

    public double getRotY() { return rotY; }
    public void setRotY(double rotY) { this.rotY = rotY; }

    public String getAnimation() { return animation; }
    public void setAnimation(String animation) { this.animation = animation; }

    private String text;
    private String emote;

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    public String getEmote() { return emote; }
    public void setEmote(String emote) { this.emote = emote; }
}
