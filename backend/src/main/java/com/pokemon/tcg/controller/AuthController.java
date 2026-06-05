package com.pokemon.tcg.controller;

import com.pokemon.tcg.dto.ForgotPasswordRequest;
import com.pokemon.tcg.dto.JugadorDTO;
import com.pokemon.tcg.dto.LoginRequest;
import com.pokemon.tcg.dto.RegisterRequest;
import com.pokemon.tcg.dto.ResetPasswordRequest;
import com.pokemon.tcg.model.Jugador;
import com.pokemon.tcg.service.AuthService;
import com.pokemon.tcg.service.PasswordRecoveryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final PasswordRecoveryService passwordRecoveryService;

    public AuthController(AuthService authService, PasswordRecoveryService passwordRecoveryService) {
        this.authService = authService;
        this.passwordRecoveryService = passwordRecoveryService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        try {
            Jugador jugador = authService.login(request.getUsername(), request.getPassword());
            if (jugador == null) return ResponseEntity.status(401).body("Usuario no valido");
            return ResponseEntity.ok(toAuthResponse(jugador));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error en login: " + e.getMessage());
        }
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        try {
            Jugador jugador = authService.register(request.getScreenName(), request.getEmail(), request.getPassword(), request.getConfirmPassword());
            return ResponseEntity.ok(toAuthResponse(jugador));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error en registro: " + e.getMessage());
        }
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody ForgotPasswordRequest request) {
        try {
            return ResponseEntity.ok(passwordRecoveryService.requestReset(request.getUsername(), request.getEmail()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error recuperando password: " + e.getMessage());
        }
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody ResetPasswordRequest request) {
        try {
            passwordRecoveryService.resetPassword(request.getToken(), request.getPassword(), request.getConfirmPassword());
            return ResponseEntity.ok("Password actualizado. Ya podes iniciar sesion.");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error cambiando password: " + e.getMessage());
        }
    }

    private JugadorDTO toAuthResponse(Jugador jugador) {
        return new JugadorDTO(jugador.getUsername(), jugador.getSobresDisponibles(), 0);
    }
}
