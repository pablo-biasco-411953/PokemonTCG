package com.pokemon.tcg.service;

import com.pokemon.tcg.model.Card;
import com.pokemon.tcg.model.Jugador;
import com.pokemon.tcg.repository.CardRepository;
import com.pokemon.tcg.repository.JugadorRepository;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class SobreService {
    private final JugadorRepository jugadorRepo;
    private final CardRepository cardRepo;

    public SobreService(JugadorRepository jugadorRepo, CardRepository cardRepo) {
        this.jugadorRepo = jugadorRepo;
        this.cardRepo = cardRepo;
    }

    /**
     * Abre un sobre para el usuario indicado.
     * Reduce los sobres disponibles en 1, selecciona 10 cartas aleatorias de la BD,
     * las añade a la colección del jugador y devuelve la lista de cartas obtenidas.
     */
    public List<Card> abrirSobre(String username) {
        Jugador jugador = jugadorRepo.findByUsername(username);
        if (jugador == null) {
            throw new IllegalArgumentException("Jugador no encontrado: " + username);
        }

        if (jugador.getSobresDisponibles() <= 0) {
            throw new IllegalStateException("No hay sobres disponibles para el jugador: " + username);
        }

        // Obtener 10 cartas aleatorias de la BD
        List<Card> randomCards = cardRepo.findTenRandomCards();
        if (randomCards.size() < 10) {
            throw new IllegalStateException("No se pudieron obtener suficientes cartas aleatorias.");
        }

        // Añadir las cartas a la colección del jugador
        jugador.getColeccion().addAll(randomCards);
        jugador.setSobresDisponibles(jugador.getSobresDisponibles() - 1);
        jugadorRepo.save(jugador);

        return randomCards;
    }
}
