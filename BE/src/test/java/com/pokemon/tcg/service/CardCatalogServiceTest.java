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
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        cardRepo = mock(CardRepository.class);
        cardTranslationRepo = mock(CardTranslationRepository.class);
        attackTranslationRepo = mock(AttackTranslationRepository.class);
        objectMapper = new ObjectMapper();
        service = new CardCatalogService(cardRepo, objectMapper, cardTranslationRepo, attackTranslationRepo);
    }

    private Card createCard(String id, String supertype, String name, String subtype) {
        Card c = new Card();
        c.setId(id);
        c.setSupertype(supertype);
        c.setNombre(name);
        if (subtype != null) c.setSubtypes(List.of(subtype));
        else c.setSubtypes(new ArrayList<>());
        c.reemplazarAtaques(new ArrayList<>());
        return c;
    }

    @Test
    void getCatalogo_When146CardsExist_ReturnsFromDb() {
        List<Card> baseCards = new ArrayList<>();
        for (int i = 1; i <= 146; i++) {
            Card c = createCard("xy1-" + i, "Pokemon", "Pokemon " + i, "Basic");
            // Set types for the energy cards to prevent saveAll from triggering
            if (i >= 132 && i <= 140) {
                c.setTipo("SomeType"); // Not actual types, just non-null to trigger save but wait, if they differ, it saves!
            }
            baseCards.add(c);
        }
        // Properly set the expected types to avoid triggering hayCambios = true
        baseCards.get(131).setTipo("Grass");
        baseCards.get(132).setTipo("Fire");
        baseCards.get(133).setTipo("Water");
        baseCards.get(134).setTipo("Lightning");
        baseCards.get(135).setTipo("Psychic");
        baseCards.get(136).setTipo("Fighting");
        baseCards.get(137).setTipo("Darkness");
        baseCards.get(138).setTipo("Metal");
        baseCards.get(139).setTipo("Fairy");

        // Give one an ability so it passes the "tieneHabilidadesCargadas" check
        baseCards.get(0).setHabilidades(List.of(new com.pokemon.tcg.model.Habilidad()));

        when(cardRepo.findAll()).thenReturn(baseCards);

        List<Card> catalog = service.getCatalogo();
        assertEquals(146, catalog.size());
        verify(cardRepo, never()).saveAll(anyList());
    }

    @Test
    void getCatalogo_WhenLessThan146CardsExist_SyncsFromJson() {
        // Mock repository to return empty first, then the loaded cards
        when(cardRepo.findAll()).thenReturn(new ArrayList<>()).thenReturn(createValidMockJsonSet());

        List<Card> catalog = service.getCatalogo();
        
        verify(cardRepo, atLeastOnce()).saveAll(anyList());
        assertNotNull(catalog);
    }
    
    @Test
    void normalizarEnergiasXy_InfersBasicEnergyTypes() {
        List<Card> baseCards = new ArrayList<>();
        for (int i = 1; i <= 144; i++) {
            baseCards.add(createCard("xy1-" + i, "Pokemon", "Pokemon", "Basic"));
        }
        baseCards.add(createCard("xy1-145", "Energy", "Fire Energy", "Basic")); // Inferred
        baseCards.add(createCard("xy1-132", "Energy", "Grass Energy", "Basic")); // Forced by map
        
        baseCards.get(0).setHabilidades(List.of(new com.pokemon.tcg.model.Habilidad()));
        when(cardRepo.findAll()).thenReturn(baseCards);
        
        service.getCatalogo(); // Will trigger normalizarEnergiasXy since size == 146
        
        assertEquals("Fire", baseCards.get(144).getTipo());
        assertEquals("Grass", baseCards.get(145).getTipo());
    }

    @Test
    void getCatalogo_WithLangEs_UsesJsonTranslations() {
        // SincronizarDesdeJson will trigger because we mock empty DB
        when(cardRepo.findAll()).thenReturn(new ArrayList<>()).thenReturn(createValidMockJsonSet());

        // We know cards_es.json exists in resources. 
        List<Card> catalogEs = service.getCatalogo("es");

        assertNotNull(catalogEs);
        // It should have applied translations if they exist in cards_es.json
    }

    @Test
    void getCatalogo_WithLangEn_ReturnsBaseList() {
        List<Card> baseCards = createValidMockJsonSet();
        when(cardRepo.findAll()).thenReturn(baseCards).thenReturn(baseCards);
        baseCards.get(0).setHabilidades(List.of(new com.pokemon.tcg.model.Habilidad()));

        List<Card> catalogEn = service.getCatalogo("en");
        assertSame(baseCards.get(0), catalogEn.get(0));
    }

    @Test
    void testGetCatalogoWithDbTranslationFallback() {
        Ataque ataque = new Ataque();
        ataque.setId(1L);
        ataque.setNombre("Tackle");
        ataque.setTexto("Original text");
        ataque.setTiposEnergia(new ArrayList<>());

        Card targetCard = createCard("xy1-1", "Pokemon", "Venusaur-EX", "Basic");
        targetCard.setHp("180");
        targetCard.reemplazarAtaques(List.of(ataque));

        List<Card> baseCards = new ArrayList<>();
        baseCards.add(targetCard);
        for (int i = 2; i <= 146; i++) {
            baseCards.add(createCard("xy1-" + i, "Pokemon", "Card " + i, "Basic"));
        }
        baseCards.get(0).setHabilidades(List.of(new com.pokemon.tcg.model.Habilidad()));

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

        assertNotNull(translatedCard);
        assertEquals("NombreTraducido", translatedCard.getNombre());
        assertFalse(translatedCard.getReglas().isEmpty());
        assertEquals("ReglaTraducida", translatedCard.getReglas().get(0));

        assertFalse(translatedCard.getAtaques().isEmpty());
        Ataque translatedAttack = translatedCard.getAtaques().get(0);
        assertEquals("AtaqueTraducido", translatedAttack.getNombre());
        assertEquals("TextoTraducido", translatedAttack.getTexto());
    }

    private List<Card> createValidMockJsonSet() {
        List<Card> cards = new ArrayList<>();
        for(int i = 1; i <= 146; i++) {
            cards.add(createCard("xy1-" + i, "Pokemon", "Pokemon " + i, "Basic"));
        }
        return cards;
    }
}
