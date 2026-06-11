package com.pokemon.tcg.service.battle;

import com.pokemon.tcg.model.Card;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EnergyCostCalculatorTest {

    @Test
    void doubleColorlessPaysTwoColorlessRequirements() {
        assertTrue(EnergyCostCalculator.canPay(
                List.of(energy("Double Colorless Energy", "")),
                List.of("Colorless", "Colorless")
        ));
    }

    @Test
    void rainbowPaysOneSpecificRequirement() {
        assertTrue(EnergyCostCalculator.canPay(
                List.of(energy("Rainbow Energy", "")),
                List.of("Metal")
        ));
        assertFalse(EnergyCostCalculator.canPay(
                List.of(energy("Rainbow Energy", "")),
                List.of("Metal", "Colorless")
        ));
    }

    @Test
    void exactEnergyIsUsedBeforeRainbow() {
        assertTrue(EnergyCostCalculator.canPay(
                List.of(energy("Metal Energy", "Energy"), energy("Rainbow Energy", "")),
                List.of("Metal", "Fire")
        ));
    }

    private Card energy(String name, String type) {
        Card card = new Card();
        card.setNombre(name);
        card.setTipo(type);
        card.setSupertype("Energy");
        return card;
    }
}
