package com.pokemon.tcg.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "jugadores")
public class Jugador {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String username;

    @JsonIgnore
    @Column(length = 128)
    private String passwordHash;

    @Column(unique = true)
    private String email;

    @JsonIgnore
    @Column(length = 128)
    private String passwordResetTokenHash;

    @JsonIgnore
    private Long passwordResetTokenExpiresAt;
    
    private int sobresDisponibles;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "jugador_card",
        joinColumns = @JoinColumn(name = "jugador_id"),
        inverseJoinColumns = @JoinColumn(name = "card_id")
    )
    private List<Card> coleccion = new ArrayList<>();

    private String characterId;
    private String skinColor;
    private String hairColor;
    private String eyeColor;
    private double height = 1.0;
    private boolean pikachuCompanion = true;
    private boolean santoroGiftClaimed = false;
    private boolean santoroQuestTracking = false;
    private String santoroQuestState = "AVAILABLE";

    // valores base de los juegadores
    public Jugador() {}
    public Jugador(String username) {
        this.username = username;
        this.sobresDisponibles = 10;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPasswordResetTokenHash() { return passwordResetTokenHash; }
    public void setPasswordResetTokenHash(String passwordResetTokenHash) { this.passwordResetTokenHash = passwordResetTokenHash; }
    public Long getPasswordResetTokenExpiresAt() { return passwordResetTokenExpiresAt; }
    public void setPasswordResetTokenExpiresAt(Long passwordResetTokenExpiresAt) { this.passwordResetTokenExpiresAt = passwordResetTokenExpiresAt; }
    public int getSobresDisponibles() { return sobresDisponibles; }
    public void setSobresDisponibles(int sobresDisponibles) { this.sobresDisponibles = sobresDisponibles; }
    public List<Card> getColeccion() { return coleccion; }
    public void setColeccion(List<Card> coleccion) { this.coleccion = coleccion; }

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

    public boolean isSantoroGiftClaimed() { return santoroGiftClaimed; }
    public void setSantoroGiftClaimed(boolean santoroGiftClaimed) { this.santoroGiftClaimed = santoroGiftClaimed; }

    public boolean isSantoroQuestTracking() { return santoroQuestTracking; }
    public void setSantoroQuestTracking(boolean santoroQuestTracking) { this.santoroQuestTracking = santoroQuestTracking; }

    public String getSantoroQuestState() { return santoroQuestState; }
    public void setSantoroQuestState(String santoroQuestState) { this.santoroQuestState = santoroQuestState; }
}
