package com.pokemon.tcg.model;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@Entity
@Table(name = "mazos")
public class Mazo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nombre;

    @ManyToOne
    @JoinColumn(name = "jugador_id")
    @JsonIgnoreProperties({"coleccion", "mazos"})
    private Jugador jugador;

    @ManyToMany
    @JoinTable(
        name = "mazo_card",
        joinColumns = @JoinColumn(name = "mazo_id"),
        inverseJoinColumns = @JoinColumn(name = "card_id")
    )
    private List<Card> cartas;

    // Constructor vacío para JPA
    public Mazo() {}

    // ESTE ES EL QUE FALTABA PARA EL SERVICE
    public Mazo(String nombre, Jugador jugador) {
        this.nombre = nombre;
        this.jugador = jugador;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public Jugador getJugador() { return jugador; }
    public void setJugador(Jugador jugador) { this.jugador = jugador; }
    public List<Card> getCartas() { return cartas; }
    public void setCartas(List<Card> cartas) { this.cartas = cartas; }
}
