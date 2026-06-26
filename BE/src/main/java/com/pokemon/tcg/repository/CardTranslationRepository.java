package com.pokemon.tcg.repository;

import com.pokemon.tcg.model.CardTranslation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CardTranslationRepository extends JpaRepository<CardTranslation, Long> {
    List<CardTranslation> findByLang(String lang);
    boolean existsByLang(String lang);
}
