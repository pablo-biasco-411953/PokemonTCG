package com.pokemon.tcg.repository;

import com.pokemon.tcg.model.Jugador;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JugadorRepository extends JpaRepository<Jugador, Long> {
    Jugador findByUsername(String username);
}
