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
    private int costoRetirada;
    @Column(length = 2000, name = "attacks")
    private String attacksJson;

    @Column(length = 1000, name = "weakness")
    private String weaknessJson;

    @Column(length = 1000, name = "resistance")
    private String resistanceJson;

    @Transient
    private List<Ataque> ataques = new ArrayList<>();

    @Transient
    private List<Map<String, String>> debilidades = new ArrayList<>();

    @Transient
    private List<Map<String, String>> resistencias = new ArrayList<>();

    private static final ObjectMapper mapper = new ObjectMapper();

    public Card() {}

    // ─── PROCESAMIENTO DESDE JSON (Node.js -> Java) ───

    @JsonProperty("ataques")
    public void cargarAtaquesDesdeJson(List<Map<String, Object>> jsonAtaques) {
        List<Ataque> nuevaLista = new ArrayList<>();
        if (jsonAtaques != null) {
            for (Map<String, Object> map : jsonAtaques) {
                Ataque atk = new Ataque();
                atk.setNombre((String) map.get("nombre"));

                // Lector de daño a prueba de balas
                Object dmgObj = map.get("dano");
                if (dmgObj == null) dmgObj = map.get("damage");

                if (dmgObj != null) {
                    String dmgStr = String.valueOf(dmgObj).replaceAll("[^0-9]", "");
                    atk.setDanio(dmgStr.isEmpty() ? 0 : Integer.parseInt(dmgStr));
                } else {
                    atk.setDanio(0);
                }

                List<String> costo = (List<String>) map.get("costo");
                atk.setTiposEnergia(costo != null ? costo : new ArrayList<>());
                nuevaLista.add(atk);
            }
        }
        this.ataques = nuevaLista;
        try {
            this.attacksJson = mapper.writeValueAsString(this.ataques);
        } catch(Exception e) { this.attacksJson = "[]"; }
    }

    @JsonProperty("debilidades")
    public void setDebilidades(List<Map<String, String>> lista) {
        this.debilidades = (lista != null) ? lista : new ArrayList<>();
        try {
            this.weaknessJson = mapper.writeValueAsString(this.debilidades);
        } catch (Exception e) { this.weaknessJson = "[]"; }
    }

    @JsonProperty("resistencias")
    public void setResistencias(List<Map<String, String>> lista) {
        this.resistencias = (lista != null) ? lista : new ArrayList<>();
        try {
            this.resistanceJson = mapper.writeValueAsString(this.resistencias);
        } catch (Exception e) { this.resistanceJson = "[]"; }
    }
    @JsonProperty("costoRetirada")
    public void setCostoRetirada(int costoRetirada) {
        this.costoRetirada = costoRetirada;
    }
    // ─── CARGA DESDE DB (Hidratación) ───
    @PostLoad
    private void onLoad() {
        try {
            if (this.attacksJson != null && !this.attacksJson.isEmpty()) {
                this.ataques = mapper.readValue(this.attacksJson, new TypeReference<List<Ataque>>() {});
            }
            if (this.weaknessJson != null && !this.weaknessJson.isEmpty()) {
                this.debilidades = mapper.readValue(this.weaknessJson, new TypeReference<List<Map<String, String>>>() {});
            }
            if (this.resistanceJson != null && !this.resistanceJson.isEmpty()) {
                this.resistencias = mapper.readValue(this.resistanceJson, new TypeReference<List<Map<String, String>>>() {});
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
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

    public List<Ataque> getAtaques() { return ataques; }
    public void setAtaques(List<Ataque> ataques) { this.ataques = ataques; }

    public List<Map<String, String>> getDebilidades() { return debilidades; }
    public List<Map<String, String>> getResistencias() { return resistencias; }

    @JsonIgnore
    public String getAttacksJson() { return attacksJson; }
    public void setAttacksJson(String attacksJson) { this.attacksJson = attacksJson; }
    @JsonIgnore
    public String getWeaknessJson() { return weaknessJson; }
    public void setWeaknessJson(String weaknessJson) { this.weaknessJson = weaknessJson; }
    @JsonIgnore
    public String getResistanceJson() { return resistanceJson; }
    public void setResistanceJson(String resistanceJson) { this.resistanceJson = resistanceJson; }
    public int getCostoRetirada() {
        return costoRetirada;
    }
}