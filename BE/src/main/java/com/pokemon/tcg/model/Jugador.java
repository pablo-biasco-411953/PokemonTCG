package com.pokemon.tcg.model;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "jugadores")
public class Jugador {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String username;
    
    private int sobresDisponibles;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "jugador_card",
        joinColumns = @JoinColumn(name = "jugador_id"),
        inverseJoinColumns = @JoinColumn(name = "card_id")
    )
    private List<Card> coleccion = new ArrayList<>();

    public Jugador() {}
    public Jugador(String username) {
        this.username = username;
        this.sobresDisponibles = 5;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public int getSobresDisponibles() { return sobresDisponibles; }
    public void setSobresDisponibles(int sobresDisponibles) { this.sobresDisponibles = sobresDisponibles; }
    public List<Card> getColeccion() { return coleccion; }
    public void setColeccion(List<Card> coleccion) { this.coleccion = coleccion; }
}
