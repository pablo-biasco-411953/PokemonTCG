package com.pokemon.tcg.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pokemon.tcg.model.Card;
import com.pokemon.tcg.repository.CardRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.List;

@Component
public class DataLoader implements CommandLineRunner {

    private final CardRepository cardRepo;
    private final ObjectMapper objectMapper;

    public DataLoader(CardRepository cardRepo, ObjectMapper objectMapper) {
        this.cardRepo = cardRepo;
        this.objectMapper = objectMapper;
    }

    @Override
    public void run(String... args) throws Exception {
        if (cardRepo.count() == 0) {
            try {
                InputStream inputStream = getClass().getResourceAsStream("/cards.json");
                if (inputStream == null) {
                    System.out.println("âŒ ERROR: No se encontrÃ³ el archivo /cards.json en src/main/resources");
                    return;
                }

                List<Card> cards = objectMapper.readValue(inputStream, new TypeReference<List<Card>>() {});
                cardRepo.saveAll(cards);
                System.out.println("âœ… Â¡Ã‰XITO! Se cargaron " + cards.size() + " cartas reales desde el JSON.");
            } catch (Exception e) {
                System.out.println("âŒ ERROR al procesar el JSON: " + e.getMessage());
            }
        }
    }
}
