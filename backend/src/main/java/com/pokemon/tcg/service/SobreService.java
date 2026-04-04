package com.pokemon.tcg.service;

import com.pokemon.tcg.model.Card;
import com.pokemon.tcg.model.Jugador;
import com.pokemon.tcg.repository.CardRepository;
import com.pokemon.tcg.repository.JugadorRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

@Service
public class SobreService {
    private final JugadorRepository jugadorRepo;
    private final CardRepository cardRepo;
    private final Random random = new Random();

    public SobreService(JugadorRepository jugadorRepo, CardRepository cardRepo) {
        this.jugadorRepo = jugadorRepo;
        this.cardRepo = cardRepo;
    }

    public List<Card> abrirSobre(String username) {
        Jugador jugador = jugadorRepo.findByUsername(username);
        if (jugador == null) {
            throw new IllegalArgumentException("Jugador no encontrado: " + username);
        }

        if (jugador.getSobresDisponibles() <= 0) {
            throw new IllegalStateException("No hay sobres disponibles para el jugador: " + username);
        }

        List<Card> todasLasCartas = cardRepo.findAll();

        // 🚩 FIX: Ahora filtramos por Supertype en lugar de Tipo
        List<Card> energias = todasLasCartas.stream()
                .filter(c -> "Energy".equalsIgnoreCase(c.getSupertype()))
                .toList();

        List<Card> pokemones = todasLasCartas.stream()
                .filter(c -> "Pokémon".equalsIgnoreCase(c.getSupertype()))
                .toList();

        if (energias.isEmpty() || pokemones.isEmpty()) {
            throw new IllegalStateException("La base de datos no tiene suficientes cartas cargadas.");
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
}