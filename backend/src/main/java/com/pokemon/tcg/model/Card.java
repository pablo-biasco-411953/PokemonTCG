package com.pokemon.tcg.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.pokemon.tcg.model.converter.StringListConverter;
import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "cards")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "supertype", discriminatorType = DiscriminatorType.STRING)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "supertype",
    visible = true
)
@JsonSubTypes({
    @JsonSubTypes.Type(value = PokemonCard.class, name = "Pokémon"),
    @JsonSubTypes.Type(value = PokemonCard.class, name = "Pokemon"),
    @JsonSubTypes.Type(value = TrainerCard.class, name = "Trainer"),
    @JsonSubTypes.Type(value = EnergyCard.class, name = "Energy")
})
public abstract class Card {
    @Id
    private String id;
    private String nombre;
    private String tipo;
    private String imagen;

    @Column(name = "supertype", insertable = false, updatable = false)
    @JsonProperty("supertype")
    private String supertype;

    @Convert(converter = StringListConverter.class)
    @Column(name = "subtypes", length = 500)
    @JsonProperty("subtypes")
    private List<String> subtypes = new ArrayList<>();

    @Convert(converter = StringListConverter.class)
    @Column(name = "rules", length = 2000)
    @JsonProperty("reglas")
    private List<String> reglas = new ArrayList<>();

    public Card() {}

    @JsonProperty("supertype")
    public void setSupertype(String supertype) {
        this.supertype = supertype;
    }

    @JsonProperty("supertype")
    public String getSupertype() {
        return supertype;
    }

    @JsonProperty("subtypes")
    public void setSubtypes(List<String> lista) {
        this.subtypes = (lista != null) ? lista : new ArrayList<>();
    }

    @JsonProperty("reglas")
    public void setReglas(List<String> lista) {
        this.reglas = (lista != null) ? lista : new ArrayList<>();
    }

    // Getters y Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }
    public String getImagen() { return imagen; }
    public void setImagen(String imagen) { this.imagen = imagen; }

    public List<String> getSubtypes() { return subtypes; }
    public List<String> getReglas() { return reglas; }

    // Métodos dummy para mantener firmas de compatibilidad polimórfica (si aplica)
    public String getHp() { return "0"; }
    public int getCostoRetirada() { return 0; }
    public String getEvolvesFrom() { return null; }
    public List<com.pokemon.tcg.model.battle.Ataque> getAtaques() { return new ArrayList<>(); }
    public List<java.util.Map<String, String>> getDebilidades() { return new ArrayList<>(); }
    public List<java.util.Map<String, String>> getResistencias() { return new ArrayList<>(); }
}