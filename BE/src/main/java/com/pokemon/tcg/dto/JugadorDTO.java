package com.pokemon.tcg.dto;

public class JugadorDTO {
    public String username;
    public int sobresDisponibles;
    public int cantidadCartas;
    public boolean admin;

    public JugadorDTO(String username, int sobres, int cartas, boolean admin) {
        this.username = username;
        this.sobresDisponibles = sobres;
        this.cantidadCartas = cartas;
        this.admin = admin;
    }
}
