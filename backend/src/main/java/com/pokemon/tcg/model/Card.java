package com.pokemon.tcg.model;

import jakarta.persistence.*;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@Entity
@Table(name = "cards")
@JsonIgnoreProperties(ignoreUnknown = true)
public class Card {
    @Id
    private String id;
    private String nombre;
    private String tipo;
    private String hp;

    @Lob
    private String attacks; // JSON string

    private String imagen;

    public Card() {}

    @JsonProperty("attacks")
    public void setAttacks(Object attacks) {
        try {
            this.attacks = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(attacks);
        } catch (Exception e) {
            this.attacks = "[]";
        }
    }

    // getters y setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }
    public String getHp() { return hp; }
    public void setHp(String hp) { this.hp = hp; }
    public String getAttacks() { return attacks; }
    public void setAttacks(String attacks) { this.attacks = attacks; }
    public String getImagen() { return imagen; }
    public void setImagen(String imagen) { this.imagen = imagen; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Card)) return false;
        Card card = (Card) o;
        return Objects.equals(id, card.id);
    }

    @Override
    public int hashCode() { return Objects.hash(id); }
}
