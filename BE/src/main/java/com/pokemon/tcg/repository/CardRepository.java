package com.pokemon.tcg.repository;

import com.pokemon.tcg.model.Card;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CardRepository extends JpaRepository<Card, String> {
    @Query(value = "SELECT * FROM cards ORDER BY RAND() LIMIT 10", nativeQuery = true)
    List<Card> findTenRandomCards();
}
