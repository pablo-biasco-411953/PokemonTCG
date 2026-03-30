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

    public Ataque() {}

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public int getDanio() { return danio; }
    public void setDanio(int danio) { this.danio = danio; }

    @JsonProperty("costo") // Para que Jackson sepa leerlo del JSON
    public List<String> getCosto() { return tiposEnergia; }

    @JsonProperty("costo") // Para que Jackson sepa escribirlo
    public void setTiposEnergia(List<String> tiposEnergia) { this.tiposEnergia = tiposEnergia; }
}