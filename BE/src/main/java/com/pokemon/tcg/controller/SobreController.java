package com.pokemon.tcg.controller;

import com.pokemon.tcg.model.Card;
import com.pokemon.tcg.service.SobreService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/sobres")
@CrossOrigin(origins = "http://localhost:4200")
public class SobreController {
    private final SobreService sobreService;

    public SobreController(SobreService sobreService) {
        this.sobreService = sobreService;
    }

    @PostMapping("/abrir/{username}")
    public ResponseEntity<?> abrirSobre(@PathVariable String username) {
        try {
            List<Card> cartas = sobreService.abrirSobre(username);
            return ResponseEntity.ok(cartas);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error al abrir el sobre: " + e.getMessage());
        }
    }
}
