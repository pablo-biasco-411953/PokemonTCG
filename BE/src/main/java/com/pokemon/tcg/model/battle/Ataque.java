package com.pokemon.tcg.model.battle;

import java.util.List;

/**
 * Representa un ataque de una carta en el juego.
 */
public class Ataque {
    private String nombre;
    private int danio;
    private List<String> tiposEnergia; // Tipos de energía requeridos para ejecutar este ataque
    
    public Ataque() {}
    
    public Ataque(String nombre, int danio, List<String> tiposEnergia) {
        this.nombre = nombre;
        this.danio = danio;
        this.tiposEnergia = tiposEnergia;
    }
    
    // getters y setters
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public int getDanio() { return danio; }
    public void setDanio(int danio) { this.danio = danio; }
    public List<String> getTiposEnergia() { return tiposEnergia; }
    public void setTiposEnergia(List<String> tiposEnergia) { this.tiposEnergia = tiposEnergia; }
}