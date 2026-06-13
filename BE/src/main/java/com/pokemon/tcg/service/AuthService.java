package com.pokemon.tcg.service;

import com.pokemon.tcg.model.Jugador;
import com.pokemon.tcg.repository.JugadorRepository;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@Service
public class AuthService {

    private final JugadorRepository jugadorRepo;

    public AuthService(JugadorRepository jugadorRepo) {
        this.jugadorRepo = jugadorRepo;
    }

    public Jugador login(String username, String password) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("El usuario es obligatorio.");
        }
        if (password == null || password.length() < 4) {
            throw new IllegalArgumentException("La contrasena debe tener al menos 4 caracteres.");
        }

        String cleanUsername = username.trim();
        Jugador jugador = jugadorRepo.findAuthByUsername(cleanUsername);

        if (jugador == null) {
            throw new IllegalArgumentException("Usuario o contrasena incorrectos.");
        }

        String passwordHash = hashPassword(password);
        if (jugador.getPasswordHash() == null
                || jugador.getPasswordHash().isBlank()
                || !jugador.getPasswordHash().equals(passwordHash)) {
            throw new IllegalArgumentException("Usuario o contrasena incorrectos.");
        }

        return jugador;
    }

    public Jugador register(String screenName, String email, String password, String confirmPassword) {
        if (screenName == null || screenName.isBlank()) {
            throw new IllegalArgumentException("El nombre de usuario es obligatorio.");
        }
        if (email == null || email.isBlank() || !email.contains("@")) {
            throw new IllegalArgumentException("El email es obligatorio para recuperar la cuenta.");
        }
        if (password == null || password.length() < 4) {
            throw new IllegalArgumentException("La contrasena debe tener al menos 4 caracteres.");
        }
        if (!password.equals(confirmPassword)) {
            throw new IllegalArgumentException("Las contrasenas no coinciden.");
        }

        String cleanScreenName = screenName.trim();
        Jugador existente = jugadorRepo.findAuthByUsername(cleanScreenName);
        if (existente != null) {
            throw new IllegalArgumentException("El nombre de usuario ya esta en uso.");
        }

        String cleanEmail = email.trim().toLowerCase();
        if (jugadorRepo.findByEmail(cleanEmail) != null) {
            throw new IllegalArgumentException("Ese email ya esta asociado a otro entrenador.");
        }

        Jugador jugador = new Jugador(cleanScreenName);
        jugador.setEmail(cleanEmail);
        jugador.setPasswordHash(hashPassword(password));
        jugadorRepo.save(jugador);
        return jugador;
    }

    public String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(("pokemon-tcg:" + password).getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("No se pudo preparar el hash de contrasena.", e);
        }
    }
}
