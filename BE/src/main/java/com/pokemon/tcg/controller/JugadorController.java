package com.pokemon.tcg.controller;

import com.pokemon.tcg.model.Jugador;
import com.pokemon.tcg.repository.JugadorRepository;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import java.util.*;

@RestController
@RequestMapping("/api/jugadores")
// CAMBIO CLAVE: Especificamos el origen exacto de Angular
public class JugadorController {

    private final JugadorRepository jugadorRepo;

    public JugadorController(JugadorRepository jugadorRepo) {
        this.jugadorRepo = jugadorRepo;
    }

    @GetMapping("/{username}/datos")
    public ResponseEntity<?> obtenerDatos(@PathVariable String username) {
        try {
            Jugador j = jugadorRepo.findByUsername(username);
            if (j == null) return ResponseEntity.status(404).body("Jugador no encontrado");

            Map<String, Object> response = new HashMap<>();
            response.put("username", j.getUsername());
            response.put("sobresDisponibles", j.getSobresDisponibles());
            int size = (j.getColeccion() != null) ? j.getColeccion().size() : 0;
            response.put("cantidadCartas", size);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error interno: " + e.getMessage());
        }
    }

    @GetMapping("/{username}/coleccion")
    public ResponseEntity<?> obtenerColeccion(@PathVariable String username) {
        try {
            Jugador j = jugadorRepo.findByUsername(username);
            if (j == null) return ResponseEntity.notFound().build();
            return ResponseEntity.ok(j.getColeccion());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error en coleccion: " + e.getMessage());
        }
    }
}
