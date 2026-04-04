package com.pokemon.tcg.model.battle;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Ataque {
    private String nombre;

    @JsonProperty("dano")
    private int danio;

    @JsonProperty("costo")
    private List<String> tiposEnergia;

    // 🚩 NUEVO: El texto del ataque (para los estados alterados)
    private String texto;

    public Ataque() {}

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public int getDanio() { return danio; }
    public void setDanio(int danio) { this.danio = danio; }

    @JsonProperty("costo")
    public List<String> getCosto() { return tiposEnergia; }

    @JsonProperty("costo")
    public void setTiposEnergia(List<String> tiposEnergia) { this.tiposEnergia = tiposEnergia; }

    // 🚩 GETTER Y SETTER NUEVO
    public String getTexto() { return texto; }
    public void setTexto(String texto) { this.texto = texto; }
}