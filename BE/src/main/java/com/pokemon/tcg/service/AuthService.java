package com.pokemon.tcg.service;

import com.pokemon.tcg.model.Jugador;
import com.pokemon.tcg.repository.JugadorRepository;
import org.springframework.stereotype.Service;

@Service
/**
 * Resuelve login simple por username y crea usuarios nuevos si no existen.
 */
public class AuthService {

    private final JugadorRepository jugadorRepo;

    public AuthService(JugadorRepository jugadorRepo) {
        this.jugadorRepo = jugadorRepo;
    }

    public Jugador login(String username) {
        // Si el jugador no existe todavía, se persiste uno nuevo.
        Jugador jugador = jugadorRepo.findByUsername(username);
        if (jugador == null) {
            // Crear nuevo jugador
            jugador = new Jugador(username);
            jugadorRepo.save(jugador);
        }
        return jugador;
    }
}
