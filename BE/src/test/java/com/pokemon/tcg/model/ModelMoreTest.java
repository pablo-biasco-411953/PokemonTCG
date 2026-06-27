package com.pokemon.tcg.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ModelMoreTest {

    // =================== Habilidad ===================

    @Test
    void habilidad_defaultConstructorAndSetters() {
        Habilidad h = new Habilidad();
        h.setNombre("Overgrow");
        h.setTexto("This Pokémon's Grass-type attacks deal 30 more damage.");
        h.setType("Ability");

        assertEquals("Overgrow", h.getNombre());
        assertEquals("This Pokémon's Grass-type attacks deal 30 more damage.", h.getTexto());
        assertEquals("Ability", h.getType());
    }

    @Test
    void habilidad_3argConstructor() {
        Habilidad h = new Habilidad("Blaze", "Fire attacks do 30 more damage.", "Ability");

        assertEquals("Blaze", h.getNombre());
        assertEquals("Fire attacks do 30 more damage.", h.getTexto());
        assertEquals("Ability", h.getType());
    }

    // =================== CardTranslation ===================

    @Test
    void cardTranslation_defaultConstructorAndSetters() {
        CardTranslation ct = new CardTranslation();
        ct.setId(1L);
        ct.setCardId("xy1-1");
        ct.setLang("es");
        ct.setNombre("Bulbasaurio");
        ct.setReglas(List.of("Regla 1", "Regla 2"));

        assertEquals(1L, ct.getId());
        assertEquals("xy1-1", ct.getCardId());
        assertEquals("es", ct.getLang());
        assertEquals("Bulbasaurio", ct.getNombre());
        assertEquals(2, ct.getReglas().size());
    }

    @Test
    void cardTranslation_4argConstructor() {
        CardTranslation ct = new CardTranslation("xy1-2", "pt", "Ivysaur", List.of("Evolucion"));

        assertEquals("xy1-2", ct.getCardId());
        assertEquals("pt", ct.getLang());
        assertEquals("Ivysaur", ct.getNombre());
        assertEquals(1, ct.getReglas().size());
        assertEquals("Evolucion", ct.getReglas().get(0));
    }

    @Test
    void cardTranslation_4argConstructor_nullReglas() {
        CardTranslation ct = new CardTranslation("xy1-3", "fr", "Bulbizarre", null);

        assertNotNull(ct.getReglas());
        assertTrue(ct.getReglas().isEmpty());
    }

    // =================== CardAttribute ===================

    @Test
    void cardAttribute_defaultConstructorAndSetters() {
        CardAttribute ca = new CardAttribute();
        ca.setType("Fire");
        ca.setValue("×2");

        assertEquals("Fire", ca.getType());
        assertEquals("×2", ca.getValue());
    }
}
