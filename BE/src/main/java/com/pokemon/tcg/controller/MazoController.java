package com.pokemon.tcg.controller;

import com.pokemon.tcg.model.Mazo;
import com.pokemon.tcg.service.MazoService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/mazos")
@CrossOrigin(origins = "http://localhost:4200") // <-- Clave para que Angular no de error
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
            // Llamamos al servicio para actualizar
            Mazo mazo = mazoService.actualizarMazo(id, request.getNombre(), request.getCartas());
            return ResponseEntity.ok(mazo);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error al actualizar el mazo: " + e.getMessage());
        }
    }

    // Clase DTO para la actualización
    public static class ActualizarMazoRequest {
        private String nombre;
        private List<String> cartas; // Lista de IDs de las cartas

        public String getNombre() { return nombre; }
        public void setNombre(String nombre) { this.nombre = nombre; }
        public List<String> getCartas() { return cartas; }
        public void setCartas(List<String> cartas) { this.cartas = cartas; }
    }

    public static class GuardarMazoRequest {
        private String nombre;
        private String username;
        private List<String> cartas;
        public String getNombre() { return nombre; }
        public void setNombre(String nombre) { this.nombre = nombre; }
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public List<String> getCartas() { return cartas; }
        public void setCartas(List<String> cartas) { this.cartas = cartas; }
    }
}
