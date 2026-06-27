package com.pokemon.tcg.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Embeddable
@JsonIgnoreProperties(ignoreUnknown = true)
public class Habilidad {
    private String nombre;

    @Column(length = 2000)
    private String texto;

    private String type;

    public Habilidad() {}

    public Habilidad(String nombre, String texto, String type) {
        this.nombre = nombre;
        this.texto = texto;
        this.type = type;
    }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getTexto() { return texto; }
    public void setTexto(String texto) { this.texto = texto; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
}
