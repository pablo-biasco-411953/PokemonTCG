package com.pokemon.tcg.model.battle;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Ataque {

    private String nombre;

    @JsonProperty("dano") // Angular necesita que se llame asÃ­
    private int danio;

    @JsonProperty("costo") // Angular necesita que se llame asÃ­
    private List<String> tiposEnergia;

    public Ataque() {}

    public Ataque(String nombre, int danio, List<String> tiposEnergia) {
        this.nombre = nombre;
        this.danio = danio;
        this.tiposEnergia = tiposEnergia;
    }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public int getDanio() { return danio; }
    public void setDanio(int danio) { this.danio = danio; }

    public List<String> getTiposEnergia() { return tiposEnergia; }
    public void setTiposEnergia(List<String> tiposEnergia) { this.tiposEnergia = tiposEnergia; }
}
