package com.pokemon.tcg.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Entity
@Table(name = "cards")
@JsonIgnoreProperties(ignoreUnknown = true)
public class Card {
    @Id
    private String id;
    private String nombre;

    @Column(length = 1000)
    private String attacks;

    private String hp;
    private String tipo;
    private String imagen;

    public Card() {}

    // Este método procesa el JSON cuando llega de la API o del import
    @JsonProperty("ataques")
    public void setAtaquesFromJson(List<Map<String, Object>> ataquesJson) {
        if (ataquesJson != null && !ataquesJson.isEmpty()) {
            this.attacks = ataquesJson.stream()
                    .map(a -> (String) a.get("nombre"))
                    .collect(Collectors.joining(", "));
        } else {
            this.attacks = "Sin ataques";
        }
    }

    // --- GETTERS Y SETTERS ---

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    // Usamos este nombre para que el Service lo encuentre
    public String getAttacks() { return attacks; }
    public void setAttacks(String attacks) { this.attacks = attacks; }

    // 🚨 ALIAS PARA COMPATIBILIDAD (Soluciona el error de compilación)
    public String getAtaques() { return this.attacks; }

    public String getHp() { return hp; }
    public void setHp(String hp) { this.hp = hp; }

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }

    public String getImagen() { return imagen; }
    public void setImagen(String imagen) { this.imagen = imagen; }
}