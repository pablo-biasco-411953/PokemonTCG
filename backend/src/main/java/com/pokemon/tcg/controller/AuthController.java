package com.pokemon.tcg.controller;

import com.pokemon.tcg.model.Jugador;
import com.pokemon.tcg.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<Jugador> login(@RequestBody LoginRequest request) {
        Jugador jugador = authService.login(request.getUsername());
        return ResponseEntity.ok(jugador);
    }

    public static class LoginRequest {
        private String username;

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }
    }
}