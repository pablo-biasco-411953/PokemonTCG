package com.pokemon.tcg.controller;

import com.pokemon.tcg.model.Mazo;
import com.pokemon.tcg.service.MazoService;
import com.pokemon.tcg.dto.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/mazos")
@CrossOrigin(origins = "http://localhost:4200")
public class MazoController {
    private final MazoService mazoService;

    public MazoController(MazoService mazoService) {
        this.mazoService = mazoService;
    }

    @PostMapping("/guardar")
    public ResponseEntity<?> guardarMazo(@RequestBody GuardarMazoRequest request) {
        try {
            Mazo mazo = mazoService.guardarMazo(request.getNombre(), request.getUsername(), request.getCartas());
            return ResponseEntity.ok(mazo);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/listar/{username}")
    public ResponseEntity<List<Mazo>> listarMazos(@PathVariable String username) {
        return ResponseEntity.ok(mazoService.listarMazos(username));
    }

    @PutMapping("/actualizar/{id}")
    public ResponseEntity<?> actualizarMazo(@PathVariable Long id, @RequestBody ActualizarMazoRequest request) {
        try {
            Mazo mazo = mazoService.actualizarMazo(id, request.getNombre(), request.getCartas());
            return ResponseEntity.ok(mazo);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error al actualizar el mazo: " + e.getMessage());
        }
    }
}
