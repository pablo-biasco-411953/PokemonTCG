package com.pokemon.tcg.service;

import com.pokemon.tcg.model.Card;
import com.pokemon.tcg.model.CardTranslation;
import com.pokemon.tcg.model.battle.AttackTranslation;
import com.pokemon.tcg.model.battle.Ataque;
import com.pokemon.tcg.repository.CardTranslationRepository;
import com.pokemon.tcg.repository.AttackTranslationRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;MODE=MySQL;NON_KEYWORDS=VALUE",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "app.mail.enabled=false"
})
@Transactional
public class CardCatalogServiceTest {

    @Autowired
    private CardCatalogService cardCatalogService;

    @Autowired
    private CardTranslationRepository cardTranslationRepo;

    @Autowired
    private AttackTranslationRepository attackTranslationRepo;

    @Test
    public void testGetCatalogoWithTranslation() {
        // 1. Get base catalog (which makes sure base cards are loaded)
        List<Card> baseCatalog = cardCatalogService.getCatalogo();
        assertFalse(baseCatalog.isEmpty(), "El catálogo base no debería estar vacío");

        // Find a card with an attack to test full translation
        Card targetCard = baseCatalog.stream()
                .filter(c -> c.getAtaques() != null && !c.getAtaques().isEmpty())
                .findFirst()
                .orElse(null);

        assertNotNull(targetCard, "Debería haber al menos una carta con ataques en el catálogo");
        String cardId = targetCard.getId();
        Ataque targetAttack = targetCard.getAtaques().get(0);

        // 2. Create translations in DB
        String testLang = "xyz"; // Idioma de prueba personalizado
        CardTranslation cardTr = new CardTranslation(cardId, testLang, "NombreTraducido", Collections.singletonList("ReglaTraducida"));
        cardTranslationRepo.save(cardTr);

        AttackTranslation attackTr = new AttackTranslation(targetAttack.getId(), testLang, "AtaqueTraducido", "TextoTraducido");
        attackTranslationRepo.save(attackTr);

        // 3. Retrieve catalog for custom lang
        List<Card> translatedCatalog = cardCatalogService.getCatalogo(testLang);
        Card translatedCard = translatedCatalog.stream()
                .filter(c -> c.getId().equals(cardId))
                .findFirst()
                .orElse(null);

        assertNotNull(translatedCard, "La carta traducida debería estar presente en el catálogo");
        assertEquals("NombreTraducido", translatedCard.getNombre(), "El nombre de la carta debería estar traducido");
        assertFalse(translatedCard.getReglas().isEmpty(), "Debería tener reglas traducidas");
        assertEquals("ReglaTraducida", translatedCard.getReglas().get(0), "La regla de la carta debería estar traducida");

        assertFalse(translatedCard.getAtaques().isEmpty(), "La carta traducida debería tener ataques");
        Ataque translatedAttack = translatedCard.getAtaques().get(0);
        assertEquals("AtaqueTraducido", translatedAttack.getNombre(), "El nombre del ataque debería estar traducido");
        assertEquals("TextoTraducido", translatedAttack.getTexto(), "El texto del ataque debería estar traducido");
    }
}
