package com.pokemon.tcg.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pokemon.tcg.model.Card;
import com.pokemon.tcg.model.CardTranslation;
import com.pokemon.tcg.model.battle.AttackTranslation;
import com.pokemon.tcg.model.battle.Ataque;
import com.pokemon.tcg.repository.AttackTranslationRepository;
import com.pokemon.tcg.repository.CardRepository;
import com.pokemon.tcg.repository.CardTranslationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CardCatalogServiceTest {

    private CardRepository cardRepo;
    private CardTranslationRepository cardTranslationRepo;
    private AttackTranslationRepository attackTranslationRepo;
    private CardCatalogService service;

    @BeforeEach
    void setUp() {
        cardRepo = mock(CardRepository.class);
        cardTranslationRepo = mock(CardTranslationRepository.class);
        attackTranslationRepo = mock(AttackTranslationRepository.class);
        service = new CardCatalogService(cardRepo, new ObjectMapper(), cardTranslationRepo, attackTranslationRepo);
    }

    @Test
    void testGetCatalogoWithTranslation() {
        Ataque ataque = new Ataque();
        ataque.setId(1L);
        ataque.setNombre("Tackle");
        ataque.setTexto("Original text");
        ataque.setTiposEnergia(new ArrayList<>());

        Card targetCard = new Card();
        targetCard.setId("xy1-1");
        targetCard.setNombre("Venusaur-EX");
        targetCard.setHp("180");
        targetCard.setSupertype("Pokemon");
        targetCard.setSubtypes(new ArrayList<>());
        targetCard.reemplazarAtaques(List.of(ataque));

        List<Card> baseCards = new ArrayList<>();
        baseCards.add(targetCard);
        for (int i = 2; i <= 146; i++) {
            Card c = new Card();
            c.setId("xy1-" + i);
            c.setNombre("Card " + i);
            c.setHp("60");
            c.setSupertype("Pokemon");
            c.setSubtypes(new ArrayList<>());
            c.reemplazarAtaques(new ArrayList<>());
            baseCards.add(c);
        }

        when(cardRepo.findAll()).thenReturn(baseCards);

        CardTranslation cardTr = new CardTranslation("xy1-1", "xyz", "NombreTraducido", List.of("ReglaTraducida"));
        when(cardTranslationRepo.findByLang("xyz")).thenReturn(List.of(cardTr));

        AttackTranslation attackTr = new AttackTranslation(1L, "xyz", "AtaqueTraducido", "TextoTraducido");
        when(attackTranslationRepo.findByLang("xyz")).thenReturn(List.of(attackTr));

        List<Card> translatedCatalog = service.getCatalogo("xyz");

        Card translatedCard = translatedCatalog.stream()
                .filter(c -> c.getId().equals("xy1-1"))
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
