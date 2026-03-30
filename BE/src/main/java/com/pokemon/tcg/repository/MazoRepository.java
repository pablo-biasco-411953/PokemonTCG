package com.pokemon.tcg.repository;

import com.pokemon.tcg.model.Mazo;
import com.pokemon.tcg.model.Jugador;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface MazoRepository extends JpaRepository<Mazo, Long> {
    List<Mazo> findByJugador(Jugador jugador);
}
