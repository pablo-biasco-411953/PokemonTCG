package com.pokemon.tcg.service;

import com.pokemon.tcg.model.Card;
import com.pokemon.tcg.model.Jugador;
import com.pokemon.tcg.repository.JugadorRepository;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

@Service
/**
 * Genera sobres aleatorios y los agrega a la coleccion del jugador.
 */
public class SobreService {
    private final JugadorRepository jugadorRepo;
    private final CardCatalogService cardCatalogService;
    private final Random random = new Random();

    public SobreService(JugadorRepository jugadorRepo, CardCatalogService cardCatalogService) {
        this.jugadorRepo = jugadorRepo;
        this.cardCatalogService = cardCatalogService;
    }

    public List<Card> abrirSobre(String username) {
        Jugador jugador = jugadorRepo.findByUsername(username);
        if (jugador == null) {
            throw new IllegalArgumentException("Jugador no encontrado: " + username);
        }

        if (jugador.getSobresDisponibles() <= 0) {
            throw new IllegalStateException("No hay sobres disponibles para el jugador: " + username);
        }

        List<Card> todasLasCartas = cardCatalogService.getCatalogo();

        List<Card> energias = todasLasCartas.stream()
                .filter(this::esEnergia)
                .toList();

        List<Card> pokemones = todasLasCartas.stream()
                .filter(this::esPokemon)
                .toList();

        if (energias.isEmpty() || pokemones.isEmpty()) {
            todasLasCartas = cardCatalogService.sincronizarDesdeJson();
            energias = todasLasCartas.stream()
                    .filter(this::esEnergia)
                    .toList();
            pokemones = todasLasCartas.stream()
                    .filter(this::esPokemon)
                    .toList();

            if (energias.isEmpty() || pokemones.isEmpty()) {
                throw new IllegalStateException("La base de datos no tiene suficientes cartas cargadas.");
            }
        }

        int cantEnergias = random.nextInt(4) + 2;
        int cantPokemones = 10 - cantEnergias;

        List<Card> sobreGenerado = new ArrayList<>();

        List<Card> energiasMezcladas = new ArrayList<>(energias);
        Collections.shuffle(energiasMezcladas);
        sobreGenerado.addAll(energiasMezcladas.subList(0, Math.min(cantEnergias, energiasMezcladas.size())));

        List<Card> pokemonesMezclados = new ArrayList<>(pokemones);
        Collections.shuffle(pokemonesMezclados);
        sobreGenerado.addAll(pokemonesMezclados.subList(0, Math.min(cantPokemones, pokemonesMezclados.size())));

        Collections.shuffle(sobreGenerado);

        jugador.getColeccion().addAll(sobreGenerado);
        jugador.setSobresDisponibles(jugador.getSobresDisponibles() - 1);
        jugadorRepo.save(jugador);

        return sobreGenerado;
    }

    private boolean esEnergia(Card card) {
        String supertype = normalizar(card.getSupertype());
        String nombre = normalizar(card.getNombre());
        String hp = card.getHp();

        return "energy".equals(supertype)
                || (supertype.isBlank() && nombre.contains("energy"))
                || (supertype.isBlank() && "0".equals(hp) && nombre.contains("energia"));
    }

    private boolean esPokemon(Card card) {
        String supertype = normalizar(card.getSupertype());
        String hp = card.getHp();

        return "pokemon".equals(supertype)
                || (supertype.isBlank() && hp != null && !"0".equals(hp));
    }

    private String normalizar(String value) {
        if (value == null) return "";
        return Normalizer.normalize(value.trim().toLowerCase(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
    }
}
