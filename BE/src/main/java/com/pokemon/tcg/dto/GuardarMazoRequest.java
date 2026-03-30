package com.pokemon.tcg.dto;

import java.util.List;

public class GuardarMazoRequest {
    private String nombre;
    private String username;
    private List<String> cartas;

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public List<String> getCartas() { return cartas; }
    public void setCartas(List<String> cartas) { this.cartas = cartas; }
}
