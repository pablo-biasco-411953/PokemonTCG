package com.pokemon.tcg.dto;

public class JugadorDatosResponse {
    private String username;
    private int sobresDisponibles;
    private int cantidadCartas;

    public JugadorDatosResponse(String username, int sobresDisponibles, int cantidadCartas) {
        this.username = username;
        this.sobresDisponibles = sobresDisponibles;
        this.cantidadCartas = cantidadCartas;
    }

    // Getters
    public String getUsername() { return username; }
    public int getSobresDisponibles() { return sobresDisponibles; }
    public int getCantidadCartas() { return cantidadCartas; }
}
