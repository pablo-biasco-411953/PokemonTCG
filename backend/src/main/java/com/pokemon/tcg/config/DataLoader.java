package com.pokemon.tcg.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pokemon.tcg.model.Card;
import com.pokemon.tcg.model.Jugador;
import com.pokemon.tcg.model.Mazo;
import com.pokemon.tcg.repository.CardRepository;
import com.pokemon.tcg.repository.JugadorRepository;
import com.pokemon.tcg.repository.MazoRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Component
public class DataLoader implements CommandLineRunner {

    private final CardRepository cardRepo;
    private final JugadorRepository jugadorRepo;
    private final MazoRepository mazoRepo;
    private final ObjectMapper objectMapper;

    public DataLoader(CardRepository cardRepo,
                      JugadorRepository jugadorRepo,
                      MazoRepository mazoRepo,
                      ObjectMapper objectMapper) {
        this.cardRepo = cardRepo;
        this.jugadorRepo = jugadorRepo;
        this.mazoRepo = mazoRepo;
        this.objectMapper = objectMapper;
    }

    @Override
    public void run(String... args) throws Exception {
        if (cardRepo.count() == 0) {
            try {
                InputStream inputStream = getClass().getResourceAsStream("/cards.json");
                if (inputStream == null) {
                    System.out.println("❌ ERROR: No se encontró el archivo /cards.json");
                    return;
                }

                List<Card> todasLasCartas = objectMapper.readValue(inputStream, new TypeReference<List<Card>>() {});
                cardRepo.saveAll(todasLasCartas);
                System.out.println("✅ Cartas cargadas: " + todasLasCartas.size());

                // --- CREACIÓN DE USUARIO TEST: PABLO ---
                crearUsuarioTest(todasLasCartas);

            } catch (Exception e) {
                System.out.println("❌ ERROR en DataLoader: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    private void crearUsuarioTest(List<Card> todasLasCartas) {
        // 1. Crear el Jugador
        Jugador pablo = new Jugador("Pablo");
        pablo.setSobresDisponibles(10);

        // 2. Separar cartas
        List<Card> soloPokemones = todasLasCartas.stream()
                .filter(c -> "Pokémon".equalsIgnoreCase(c.getSupertype()))
                .toList();
        List<Card> soloEnergias = todasLasCartas.stream()
                .filter(c -> "Energy".equalsIgnoreCase(c.getSupertype()))
                .toList();

        // 3. Colección de 120 cartas al azar
        List<Card> coleccionRandom = new ArrayList<>(todasLasCartas);
        Collections.shuffle(coleccionRandom);
        pablo.setColeccion(new ArrayList<>(coleccionRandom.subList(0, Math.min(120, coleccionRandom.size()))));

        jugadorRepo.save(pablo);

        // 4. Mazo de 60 cartas
        Mazo mazoTest = new Mazo("Mazo Inicial Pablo", pablo);
        List<Card> cartasMazo = new ArrayList<>();

        List<Card> pokesParaMazo = new ArrayList<>(soloPokemones);
        Collections.shuffle(pokesParaMazo);
        cartasMazo.addAll(pokesParaMazo.subList(0, Math.min(40, pokesParaMazo.size())));

        List<Card> energiasParaMazo = new ArrayList<>(soloEnergias);
        Collections.shuffle(energiasParaMazo);
        for(int i = 0; i < 20; i++) {
            cartasMazo.add(energiasParaMazo.get(i % energiasParaMazo.size()));
        }

        mazoTest.setCartas(cartasMazo);
        mazoRepo.save(mazoTest);

        System.out.println("🚀 [TEST DATA] Usuario 'Pablo' listo para la batalla.");
    }
}