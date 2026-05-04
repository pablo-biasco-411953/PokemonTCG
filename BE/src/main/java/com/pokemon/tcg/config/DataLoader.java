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
        List<Card> todasLasCartas;

        if (cardRepo.count() == 0) {
            try (InputStream inputStream = getClass().getResourceAsStream("/cards.json")) {
                if (inputStream == null) {
                    System.out.println("[DataLoader] No se encontro /cards.json");
                    return;
                }

                todasLasCartas = objectMapper.readValue(inputStream, new TypeReference<List<Card>>() {});
                cardRepo.saveAll(todasLasCartas);
                System.out.println("[DataLoader] Cartas cargadas: " + todasLasCartas.size());
            } catch (Exception e) {
                System.out.println("[DataLoader] Error cargando cartas: " + e.getMessage());
                e.printStackTrace();
                return;
            }
        } else {
            todasLasCartas = cardRepo.findAll();
        }

        if (jugadorRepo.findByUsername("Pablo") == null) {
            crearUsuarioTest(todasLasCartas);
        }

        if (jugadorRepo.findByUsername("BOT") == null) {
            crearBotUser(todasLasCartas);
        }
    }

    private void crearUsuarioTest(List<Card> todasLasCartas) {
        Jugador pablo = new Jugador("Pablo");
        pablo.setPasswordHash("2ab74e1d95f6aff7947352ee0d793c366a8ab33452a87a3e39b003b42c843cf9");
        pablo.setSobresDisponibles(10);

        List<Card> soloPokemones = todasLasCartas.stream()
                .filter(c -> "Pokemon".equalsIgnoreCase(c.getSupertype()) || "Pokémon".equalsIgnoreCase(c.getSupertype()))
                .toList();
        List<Card> soloEnergias = todasLasCartas.stream()
                .filter(c -> "Energy".equalsIgnoreCase(c.getSupertype()))
                .toList();

        List<Card> coleccionRandom = new ArrayList<>(todasLasCartas);
        Collections.shuffle(coleccionRandom);
        pablo.setColeccion(new ArrayList<>(coleccionRandom.subList(0, Math.min(120, coleccionRandom.size()))));

        jugadorRepo.save(pablo);

        Mazo mazoTest = new Mazo("Mazo Inicial Pablo", pablo);
        List<Card> cartasMazo = new ArrayList<>();

        List<Card> pokesParaMazo = new ArrayList<>(soloPokemones);
        Collections.shuffle(pokesParaMazo);
        cartasMazo.addAll(pokesParaMazo.subList(0, Math.min(40, pokesParaMazo.size())));

        List<Card> energiasParaMazo = new ArrayList<>(soloEnergias);
        Collections.shuffle(energiasParaMazo);
        for (int i = 0; i < 20 && !energiasParaMazo.isEmpty(); i++) {
            cartasMazo.add(energiasParaMazo.get(i % energiasParaMazo.size()));
        }

        mazoTest.setCartas(cartasMazo);
        mazoRepo.save(mazoTest);

        System.out.println("[DataLoader] Usuario Pablo listo para pruebas.");
    }

    private void crearBotUser(List<Card> todasLasCartas) {
        Jugador bot = new Jugador("BOT");
        bot.setPasswordHash("2ab74e1d95f6aff7947352ee0d793c366a8ab33452a87a3e39b003b42c843cf9");
        bot.setSobresDisponibles(10);
        bot.setCharacterId("ash"); // default bot character
        bot.setSkinColor("#ffe0bd");
        bot.setHairColor("#5c4033");
        bot.setEyeColor("#2563eb");
        bot.setHeight(0.82); // Ash scale

        List<Card> coleccionRandom = new ArrayList<>(todasLasCartas);
        Collections.shuffle(coleccionRandom);
        bot.setColeccion(new ArrayList<>(coleccionRandom.subList(0, Math.min(120, coleccionRandom.size()))));

        jugadorRepo.save(bot);

        Mazo mazoTest = new Mazo("Mazo Bot", bot);
        List<Card> cartasMazo = new ArrayList<>();

        List<Card> soloPokemones = todasLasCartas.stream()
                .filter(c -> "Pokemon".equalsIgnoreCase(c.getSupertype()) || "Pokémon".equalsIgnoreCase(c.getSupertype()))
                .toList();
        List<Card> soloEnergias = todasLasCartas.stream()
                .filter(c -> "Energy".equalsIgnoreCase(c.getSupertype()))
                .toList();

        List<Card> pokesParaMazo = new ArrayList<>(soloPokemones);
        Collections.shuffle(pokesParaMazo);
        cartasMazo.addAll(pokesParaMazo.subList(0, Math.min(40, pokesParaMazo.size())));

        List<Card> energiasParaMazo = new ArrayList<>(soloEnergias);
        Collections.shuffle(energiasParaMazo);
        for (int i = 0; i < 20 && !energiasParaMazo.isEmpty(); i++) {
            cartasMazo.add(energiasParaMazo.get(i % energiasParaMazo.size()));
        }

        mazoTest.setCartas(cartasMazo);
        mazoRepo.save(mazoTest);

        System.out.println("[DataLoader] Usuario BOT listo para pruebas.");
    }
}
