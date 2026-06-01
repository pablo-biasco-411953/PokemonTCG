package com.pokemon.tcg.service;

import com.pokemon.tcg.model.Jugador;
import com.pokemon.tcg.repository.JugadorRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Base64;

@Service
public class PasswordRecoveryService {

    private static final long TOKEN_TTL_MS = 15L * 60L * 1000L;

    private final JugadorRepository jugadorRepo;
    private final AuthService authService;
    private final JavaMailSender mailSender;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${app.mail.enabled:false}")
    private boolean mailEnabled;

    @Value("${app.mail.from:pokefetch.utn@gmail.com}")
    private String mailFrom;

    @Value("${app.mail.dev-token-response:true}")
    private boolean devTokenResponse;

    @Value("${app.frontend.reset-url:http://localhost:4200/login}")
    private String resetUrl;

    public PasswordRecoveryService(JugadorRepository jugadorRepo, AuthService authService, JavaMailSender mailSender) {
        this.jugadorRepo = jugadorRepo;
        this.authService = authService;
        this.mailSender = mailSender;
    }

    public String requestReset(String username, String email) {
        if ((username == null || username.isBlank()) && (email == null || email.isBlank())) {
            throw new IllegalArgumentException("Indica tu usuario o email.");
        }

        Jugador jugador = findJugador(username, email);
        if (jugador == null) {
            return "Si los datos existen, enviamos un codigo de recuperacion.";
        }

        if (email != null && !email.isBlank() && jugador.getEmail() != null
                && !jugador.getEmail().equalsIgnoreCase(email.trim())) {
            return "Si los datos existen, enviamos un codigo de recuperacion.";
        }

        String token = createToken();
        jugador.setPasswordResetTokenHash(authService.hashPassword(token));
        jugador.setPasswordResetTokenExpiresAt(System.currentTimeMillis() + TOKEN_TTL_MS);
        jugadorRepo.save(jugador);

        if (mailEnabled && jugador.getEmail() != null && !jugador.getEmail().isBlank()) {
            sendResetMail(jugador, token);
        }

        String baseMessage = "Si los datos existen, enviamos un codigo de recuperacion.";
        return !mailEnabled && devTokenResponse
                ? baseMessage + " Token demo: " + token
                : baseMessage;
    }

    public void resetPassword(String token, String password, String confirmPassword) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("El codigo de recuperacion es obligatorio.");
        }
        if (password == null || password.length() < 4) {
            throw new IllegalArgumentException("La contrasena debe tener al menos 4 caracteres.");
        }
        if (!password.equals(confirmPassword)) {
            throw new IllegalArgumentException("Las contrasenas no coinciden.");
        }

        Jugador jugador = jugadorRepo.findByPasswordResetTokenHash(authService.hashPassword(token.trim()));
        if (jugador == null || jugador.getPasswordResetTokenExpiresAt() == null
                || jugador.getPasswordResetTokenExpiresAt() < System.currentTimeMillis()) {
            throw new IllegalArgumentException("El codigo expiro o no es valido.");
        }

        jugador.setPasswordHash(authService.hashPassword(password));
        jugador.setPasswordResetTokenHash(null);
        jugador.setPasswordResetTokenExpiresAt(null);
        jugadorRepo.save(jugador);
    }

    private Jugador findJugador(String username, String email) {
        if (username != null && !username.isBlank()) {
            return jugadorRepo.findAuthByUsername(username.trim());
        }
        return jugadorRepo.findByEmail(email.trim().toLowerCase());
    }

    private String createToken() {
        byte[] bytes = new byte[24];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private void sendResetMail(Jugador jugador, String token) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(mailFrom);
        message.setTo(jugador.getEmail());
        message.setSubject("Pokefetch UTN - Recuperacion de password");
        message.setText("""
                Hola %s,

                Tu codigo de recuperacion es:

                %s

                Tambien podes abrir: %s

                El codigo vence en 15 minutos.
                """.formatted(jugador.getUsername(), token, resetUrl));
        mailSender.send(message);
    }
}
