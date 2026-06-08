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

        // Cargar desde JSON siempre para mantener base actualizada
        try (InputStream inputStream = getClass().getResourceAsStream("/cards.json")) {
            if (inputStream == null) {
                System.out.println("[DataLoader] No se encontro /cards.json");
                return;
            }
            todasLasCartas = objectMapper.readValue(inputStream, new TypeReference<List<Card>>() {});
            long dbCount = cardRepo.count();
            boolean necesitaActualizar = false;
            for (Card c : todasLasCartas) {
                if (!cardRepo.existsById(c.getId())) {
                    necesitaActualizar = true;
                    break;
                }
            }
            if (necesitaActualizar || dbCount < todasLasCartas.size()) {
                cardRepo.saveAll(todasLasCartas);
                System.out.println("[DataLoader] Base de datos actualizada con nuevas cartas. Total: " + todasLasCartas.size());
            } else {
                System.out.println("[DataLoader] Base de datos al dia. Total: " + dbCount);
            }
        } catch (Exception e) {
            System.out.println("[DataLoader] Error cargando cartas: " + e.getMessage());
            e.printStackTrace();
            return;
        }

        // Recuperar la lista definitiva de la base de datos
        todasLasCartas = cardRepo.findAll();

        Jugador pablo = jugadorRepo.findByUsername("Pablo");
        if (pablo == null) {
            crearUsuarioTest(todasLasCartas);
        } else {
            actualizarColeccionUsuario(pablo, todasLasCartas);
        }

        Jugador bot = jugadorRepo.findByUsername("BOT");
        if (bot == null) {
            crearBotUser(todasLasCartas);
        } else {
            actualizarColeccionUsuario(bot, todasLasCartas);
        }
    }

    private void crearUsuarioTest(List<Card> todasLasCartas) {
        Jugador pablo = new Jugador("Pablo");
        pablo.setPasswordHash("2ab74e1d95f6aff7947352ee0d793c366a8ab33452a87a3e39b003b42c843cf9");
        pablo.setSobresDisponibles(10);

        List<Card> coleccionCompleta = new ArrayList<>();
        for (Card card : todasLasCartas) {
            for (int i = 0; i < 4; i++) {
                coleccionCompleta.add(card);
            }
        }
        pablo.setColeccion(coleccionCompleta);
        jugadorRepo.save(pablo);

        Mazo mazoTest = new Mazo("Mazo Inicial Pablo", pablo);
        List<Card> cartasMazo = crearMazoPorDefecto(todasLasCartas);
        mazoTest.setCartas(cartasMazo);
        mazoRepo.save(mazoTest);

        System.out.println("[DataLoader] Usuario Pablo listo para pruebas con 4 copias de cada carta.");
    }

    private void crearBotUser(List<Card> todasLasCartas) {
        Jugador bot = new Jugador("BOT");
        bot.setPasswordHash("2ab74e1d95f6aff7947352ee0d793c366a8ab33452a87a3e39b003b42c843cf9");
        bot.setSobresDisponibles(10);
        bot.setCharacterId("ash");
        bot.setSkinColor("#ffe0bd");
        bot.setHairColor("#5c4033");
        bot.setEyeColor("#2563eb");
        bot.setHeight(0.82);

        List<Card> coleccionCompleta = new ArrayList<>();
        for (Card card : todasLasCartas) {
            for (int i = 0; i < 4; i++) {
                coleccionCompleta.add(card);
            }
        }
        bot.setColeccion(coleccionCompleta);
        jugadorRepo.save(bot);

        Mazo mazoTest = new Mazo("Mazo Bot", bot);
        List<Card> cartasMazo = crearMazoPorDefecto(todasLasCartas);
        mazoTest.setCartas(cartasMazo);
        mazoRepo.save(mazoTest);

        System.out.println("[DataLoader] Usuario BOT listo para pruebas con 4 copias de cada carta.");
    }

    private void actualizarColeccionUsuario(Jugador jugador, List<Card> todasLasCartas) {
        List<Card> currentCollection = jugador.getColeccion();
        List<Card> updatedCollection = new ArrayList<>(currentCollection);
        boolean modificada = false;

        for (Card card : todasLasCartas) {
            long count = currentCollection.stream().filter(c -> c.getId().equals(card.getId())).count();
            if (count < 4) {
                modificada = true;
                for (long i = count; i < 4; i++) {
                    updatedCollection.add(card);
                }
            }
        }

        if (modificada) {
            jugador.setColeccion(updatedCollection);
            jugadorRepo.save(jugador);
            System.out.println("[DataLoader] Coleccion de " + jugador.getUsername() + " actualizada con 4 copias de todas las cartas.");
        }
    }

    private List<Card> crearMazoPorDefecto(List<Card> todasLasCartas) {
        List<Card> soloPokemones = todasLasCartas.stream()
                .filter(c -> ("Pokemon".equalsIgnoreCase(c.getSupertype()) || "Pokémon".equalsIgnoreCase(c.getSupertype())) && !c.getSubtypes().contains("EX") && !c.getSubtypes().contains("MEGA"))
                .toList();
        List<Card> soloEnergias = todasLasCartas.stream()
                .filter(c -> "Energy".equalsIgnoreCase(c.getSupertype()))
                .toList();

        List<Card> cartasMazo = new ArrayList<>();
        List<Card> pokesParaMazo = new ArrayList<>(soloPokemones);
        Collections.shuffle(pokesParaMazo);
        cartasMazo.addAll(pokesParaMazo.subList(0, Math.min(40, pokesParaMazo.size())));

        List<Card> energiasParaMazo = new ArrayList<>(soloEnergias);
        Collections.shuffle(energiasParaMazo);
        for (int i = 0; i < 20 && !energiasParaMazo.isEmpty(); i++) {
            cartasMazo.add(energiasParaMazo.get(i % energiasParaMazo.size()));
        }
        return cartasMazo;
    }
}
