package com.pokemon.tcg.controller;

import com.pokemon.tcg.model.Jugador;
import com.pokemon.tcg.repository.JugadorRepository;
import com.pokemon.tcg.dto.JugadorDatosResponse;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;

@RestController
@RequestMapping("/api/jugadores")
@CrossOrigin(origins = "http://localhost:4200")
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

            int cantidadCartas = (j.getColeccion() != null) ? j.getColeccion().size() : 0;
            
            // Usamos el nuevo DTO en vez de un HashMap genÃ©rico
            JugadorDatosResponse response = new JugadorDatosResponse(
                j.getUsername(), 
                j.getSobresDisponibles(), 
                cantidadCartas
            );

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
