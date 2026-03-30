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

    @Column(length = 2000, name = "attacks")
    private String attacksJson;

    @Transient
    private List<Ataque> ataques = new ArrayList<>();

    private static final ObjectMapper mapper = new ObjectMapper();

    public Card() {}

    // â”€â”€â”€ LECTURA DESDE TU ARCHIVO CARDS.JSON â”€â”€â”€
    // â”€â”€â”€ LECTURA DESDE TU ARCHIVO CARDS.JSON â”€â”€â”€
    @JsonProperty("ataques")
    public void cargarAtaquesDesdeJson(List<Map<String, Object>> jsonAtaques) {
        List<Ataque> nuevaLista = new ArrayList<>();

        if (jsonAtaques != null) {
            for (Map<String, Object> map : jsonAtaques) {
                Ataque atk = new Ataque();
                atk.setNombre((String) map.get("nombre"));

                // --- LECTOR DE DAÃ‘O A PRUEBA DE BALAS ---
                Object dmgObj = map.get("dano");
                if (dmgObj == null) dmgObj = map.get("damage");
                if (dmgObj == null) dmgObj = map.get("danio");
                if (dmgObj == null) dmgObj = map.get("daÃ±o");

                if (dmgObj != null) {
                    try {
                        // Lo pasamos a texto y le borramos cualquier letra o sÃ­mbolo (+, x, etc)
                        String dmgStr = String.valueOf(dmgObj).replaceAll("[^0-9]", "");
                        if (!dmgStr.isEmpty()) {
                            atk.setDanio(Integer.parseInt(dmgStr));
                        } else {
                            atk.setDanio(0); // Ataques que no hacen daÃ±o (ej: curar)
                        }
                    } catch (Exception e) {
                        atk.setDanio(20); // Fallback por si hay un error rarÃ­simo
                    }
                } else {
                    atk.setDanio(20); // Si directamente no existe el campo
                }
                // ------------------------------------------

                // Extraemos el costo
                List<String> costo = (List<String>) map.get("costo");
                atk.setTiposEnergia(costo != null ? costo : new ArrayList<>());

                nuevaLista.add(atk);
            }
        }

        this.ataques = nuevaLista;

        try {
            this.attacksJson = mapper.writeValueAsString(this.ataques);
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    // â”€â”€â”€ LECTURA DESDE LA BASE DE DATOS (Cuando jugÃ¡s la partida) â”€â”€â”€
    @PostLoad
    private void onLoad() {
        try {
            if (this.attacksJson != null && !this.attacksJson.isEmpty() && !this.attacksJson.equals("[]")) {
                this.ataques = mapper.readValue(this.attacksJson, new TypeReference<List<Ataque>>() {});
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // â”€â”€â”€ GETTERS Y SETTERS â”€â”€â”€
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

    // Angular consume esto
    public List<Ataque> getAtaques() { return ataques; }
    public void setAtaques(List<Ataque> ataques) { this.ataques = ataques; }

    @JsonIgnore // Que Jackson no toque esto
    public String getAttacksJson() { return attacksJson; }
    public void setAttacksJson(String attacksJson) { this.attacksJson = attacksJson; }
}
