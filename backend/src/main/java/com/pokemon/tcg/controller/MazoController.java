package com.pokemon.tcg.controller;

import com.pokemon.tcg.model.Mazo;
import com.pokemon.tcg.model.Jugador;
import com.pokemon.tcg.service.MazoService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/mazos")
public class MazoController {
    private final MazoService mazoService;

    public MazoController(MazoService mazoService) {
        this.mazoService = mazoService;
    }

    @PostMapping("/guardar")
    public ResponseEntity<Mazo> guardarMazo(@RequestBody GuardarMazoRequest request) {
        Mazo mazo = mazoService.guardarMazo(request.getNombre(), request.getUsername(), request.getCartas());
        return ResponseEntity.ok(mazo);
    }

    @GetMapping("/listar/{username}")
    public ResponseEntity<List<Mazo>> listarMazos(@PathVariable String username) {
        List<Mazo> mazos = mazoService.listarMazos(username);
        return ResponseEntity.ok(mazos);
    }

    public static class GuardarMazoRequest {
        private String nombre;
        private String username;
        private List<String> cartas; // IDs de las cartas

        // getters y setters
        public String getNombre() { return nombre; }
        public void setNombre(String nombre) { this.nombre = nombre; }
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public List<String> getCartas() { return cartas; }
        public void setCartas(List<String> cartas) { this.cartas = cartas; }
    }
}