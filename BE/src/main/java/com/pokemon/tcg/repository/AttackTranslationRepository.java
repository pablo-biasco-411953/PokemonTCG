package com.pokemon.tcg.repository;

import com.pokemon.tcg.model.battle.AttackTranslation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface AttackTranslationRepository extends JpaRepository<AttackTranslation, Long> {
    List<AttackTranslation> findByLang(String lang);
}
