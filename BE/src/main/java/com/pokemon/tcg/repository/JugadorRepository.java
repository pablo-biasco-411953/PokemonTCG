package com.pokemon.tcg.repository;

import com.pokemon.tcg.model.Jugador;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface JugadorRepository extends JpaRepository<Jugador, Long> {
    
    @Query("SELECT j FROM Jugador j LEFT JOIN FETCH j.coleccion WHERE j.username = :username")
    Jugador findByUsername(@Param("username") String username);
}
