package com.pokemon.tcg.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pokemon.tcg.model.battle.Ataque;
import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Entity
@Table(name = "cards")
@JsonIgnoreProperties(ignoreUnknown = true)
public class Card {
    @Id
    private String id;
    private String nombre;
    private String hp;
    private String tipo;
    private String imagen;

    @JsonProperty("costoRetirada")
    private int costoRetirada;

    // 🚩 FIX: Mapeo explícito para Jackson
    @JsonProperty("supertype")
    private String supertype;

    @JsonProperty("evolvesFrom")
    private String evolvesFrom;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "card_subtypes", joinColumns = @JoinColumn(name = "card_id"))
    @Column(name = "subtype")
    private List<String> subtypes = new ArrayList<>();

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "card_rules", joinColumns = @JoinColumn(name = "card_id"))
    @Column(name = "rule", length = 2000)
    private List<String> reglas = new ArrayList<>();

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JoinColumn(name = "card_id")
    private List<Ataque> ataques = new ArrayList<>();

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "card_debilidades", joinColumns = @JoinColumn(name = "card_id"))
    private List<CardAttribute> debilidades = new ArrayList<>();

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "card_resistencias", joinColumns = @JoinColumn(name = "card_id"))
    private List<CardAttribute> resistencias = new ArrayList<>();

    public Card() {}

    // ─── PROCESAMIENTO DESDE JSON ───

    @JsonProperty("supertype")
    public void setSupertype(String supertype) {
        this.supertype = supertype;
        // 🚩 LOG DE DEBUG: Si ves esto en la consola, el fix funcionó
        if (supertype != null) {
            System.out.println("✅ [CARD LOAD] " + this.nombre + " es de tipo: " + supertype);
        }
    }

    @JsonProperty("ataques")
    public void cargarAtaquesDesdeJson(List<Map<String, Object>> jsonAtaques) {
        List<Ataque> nuevaLista = new ArrayList<>();
        if (jsonAtaques != null) {
            for (Map<String, Object> map : jsonAtaques) {
                Ataque atk = new Ataque();
                atk.setNombre((String) map.get("nombre"));

                Object dmgObj = map.get("dano") != null ? map.get("dano") : map.get("damage");
                if (dmgObj != null) {
                    String dmgStr = String.valueOf(dmgObj).replaceAll("[^0-9]", "");
                    atk.setDanio(dmgStr.isEmpty() ? 0 : Integer.parseInt(dmgStr));
                }

                List<String> costo = (List<String>) map.get("costo");
                atk.setTiposEnergia(costo != null ? costo : new ArrayList<>());

                String textoAtk = (String) map.get("texto");
                atk.setTexto(textoAtk != null ? textoAtk : "");

                nuevaLista.add(atk);
            }
        }
        this.ataques = nuevaLista;
    }

    @JsonProperty("subtypes")
    public void setSubtypes(List<String> lista) {
        this.subtypes = (lista != null) ? lista : new ArrayList<>();
    }

    @JsonProperty("reglas")
    public void setReglas(List<String> lista) {
        this.reglas = (lista != null) ? lista : new ArrayList<>();
    }

    @JsonProperty("debilidades")
    public void setDebilidades(List<CardAttribute> lista) {
        this.debilidades = (lista != null) ? lista : new ArrayList<>();
    }

    @JsonProperty("resistencias")
    public void setResistencias(List<CardAttribute> lista) {
        this.resistencias = (lista != null) ? lista : new ArrayList<>();
    }

    // ─── GETTERS Y SETTERS ───
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getHp() { return hp; }
    public void setHp(String hp) { this.hp = hp; }
    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }
    public String getImagen() { return imagen; }
    public void setImagen(String imagen) { this.imagen = imagen; }

    @JsonProperty("supertype")
    public String getSupertype() { return supertype; }

    public String getEvolvesFrom() { return evolvesFrom; }
    public void setEvolvesFrom(String evolvesFrom) { this.evolvesFrom = evolvesFrom; }

    public List<String> getSubtypes() { return subtypes; }
    public List<Ataque> getAtaques() { return ataques; }
    public List<CardAttribute> getDebilidades() { return debilidades; }
    public List<CardAttribute> getResistencias() { return resistencias; }

    public int getCostoRetirada() { return costoRetirada; }
    public void setCostoRetirada(int costoRetirada) { this.costoRetirada = costoRetirada; }
}