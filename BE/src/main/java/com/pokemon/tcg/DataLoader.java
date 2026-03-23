package com.pokemon.tcg;

import com.pokemon.tcg.model.Card;
import com.pokemon.tcg.model.Jugador;
import com.pokemon.tcg.repository.CardRepository;
import com.pokemon.tcg.repository.JugadorRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.core.io.ClassPathResource;
import java.nio.charset.StandardCharsets;
import java.util.List;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;

@Component
public class DataLoader implements CommandLineRunner {

    private final CardRepository cardRepo;
    private final JugadorRepository jugadorRepo;

    public DataLoader(CardRepository cardRepo, JugadorRepository jugadorRepo) {
        this.cardRepo = cardRepo;
        this.jugadorRepo = jugadorRepo;
    }

    @Override
    public void run(String... args) throws Exception {
        // Cargar cartas si la tabla está vacía
        if (cardRepo.count() == 0) {
            ClassPathResource resource = new ClassPathResource("cards.json");
            byte[] bytes = resource.getInputStream().readAllBytes();
            String jsonContent = new String(bytes, StandardCharsets.UTF_8);
            ObjectMapper mapper = new ObjectMapper();
            List<Card> cards = mapper.readValue(jsonContent, new TypeReference<List<Card>>() {});
            cardRepo.saveAll(cards);
        }

        // Crear jugador por defecto si no existe
        if (jugadorRepo.findByUsername("Pabli") == null) {
            Jugador pablo = new Jugador("Pabli");
            jugadorRepo.save(pablo);
        }
    }
}
