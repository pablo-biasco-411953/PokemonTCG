package com.pokemon.tcg.controller;

import com.pokemon.tcg.dto.ForgotPasswordRequest;
import com.pokemon.tcg.dto.JugadorDTO;
import com.pokemon.tcg.dto.LoginRequest;
import com.pokemon.tcg.dto.RegisterRequest;
import com.pokemon.tcg.dto.ResetPasswordRequest;
import com.pokemon.tcg.model.Jugador;
import com.pokemon.tcg.service.AuthService;
import com.pokemon.tcg.service.PasswordRecoveryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Autenticacion", description = "Endpoints para registro, login y recuperacion de contrasena")
public class AuthController {

    private final AuthService authService;
    private final PasswordRecoveryService passwordRecoveryService;

    public AuthController(AuthService authService, PasswordRecoveryService passwordRecoveryService) {
        this.authService = authService;
        this.passwordRecoveryService = passwordRecoveryService;
    }

    @PostMapping("/login")
    @Operation(summary = "Iniciar sesion", description = "Autentica un jugador con sus credenciales")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        try {
            Jugador jugador = authService.login(request.getUsername(), request.getPassword());
            return ResponseEntity.ok(toAuthResponse(jugador));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(401).body(e.getMessage());
        }
    }

    @PostMapping("/register")
    @Operation(summary = "Registrar nuevo usuario", description = "Crea un nuevo jugador en el sistema")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        Jugador jugador = authService.register(
                request.getScreenName(),
                request.getEmail(),
                request.getPassword(),
                request.getConfirmPassword()
        );
        return ResponseEntity.ok(toAuthResponse(jugador));
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Solicitar recuperacion de contrasena", description = "Envia un token de recuperacion al email")
    public ResponseEntity<?> forgotPassword(@RequestBody ForgotPasswordRequest request) {
        return ResponseEntity.ok(passwordRecoveryService.requestReset(request.getUsername(), request.getEmail()));
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Restablecer contrasena", description = "Cambia la contrasena utilizando el token recibido")
    public ResponseEntity<?> resetPassword(@RequestBody ResetPasswordRequest request) {
        passwordRecoveryService.resetPassword(request.getToken(), request.getPassword(), request.getConfirmPassword());
        return ResponseEntity.ok("Password actualizado. Ya podes iniciar sesion.");
    }

    private JugadorDTO toAuthResponse(Jugador jugador) {
        return new JugadorDTO(jugador.getUsername(), jugador.getSobresDisponibles(), 0, jugador.isAdmin());
    }
}
