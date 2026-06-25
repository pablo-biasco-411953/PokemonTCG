package com.pokemon.tcg.model.battle;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import java.util.List;

@Entity
@Table(name = "card_ataques")
@JsonIgnoreProperties(ignoreUnknown = true)
public class Ataque {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonIgnore
    private Long id;

    private String nombre;

    @JsonProperty("dano")
    private int danio;

    @ElementCollection
    @CollectionTable(name = "ataque_costo", joinColumns = @JoinColumn(name = "ataque_id"))
    @Column(name = "tipo_energia")
    @JsonProperty("costo")
    private List<String> tiposEnergia;

    @Column(length = 2000)
    private String texto;

    @Transient
    private String interactionType;

    @Transient
    private String interactionPrompt;

    public Ataque() {}

    public String getInteractionType() { return interactionType; }
    public void setInteractionType(String interactionType) { this.interactionType = interactionType; }

    public String getInteractionPrompt() { return interactionPrompt; }
    public void setInteractionPrompt(String interactionPrompt) { this.interactionPrompt = interactionPrompt; }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public int getDanio() { return danio; }
    public void setDanio(int danio) { this.danio = danio; }

    @JsonProperty("costo")
    public List<String> getCosto() { return tiposEnergia; }

    @JsonProperty("costo")
    public void setTiposEnergia(List<String> tiposEnergia) { this.tiposEnergia = tiposEnergia; }

    public String getTexto() { return texto; }
    public void setTexto(String texto) { this.texto = texto; }
}