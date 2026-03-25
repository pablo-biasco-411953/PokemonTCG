package com.pokemon.tcg.dto;

public class JugadorDTO {
    public String username;
    public int sobresDisponibles;
    public int cantidadCartas;

    public JugadorDTO(String username, int sobres, int cartas) {
        this.username = username;
        this.sobresDisponibles = sobres;
        this.cantidadCartas = cartas;
    }
}
