package com.pokemon.tcg.controller;

import com.pokemon.tcg.model.battle.*;
import com.pokemon.tcg.service.BattleEngineService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/battle/{matchId}")
public class AtaqueController {
    private final BattleEngineService battleEngine;

    public AtaqueController(BattleEngineService battleEngine) {
        this.battleEngine = battleEngine;
    }

    @PostMapping("/attack")
    public ResponseEntity<Void> realizarAtaque(
            @PathVariable String matchId,
            @RequestBody AtaqueRequest request) {
        
        // Esta implementación se completará en la Fase 5
        // Para ahora, solo devolvemos un OK
        return ResponseEntity.ok().build();
    }

    public static class AtaqueRequest {
        private String atacanteId; // ID de la carta que ataca
        private String defensorId; // ID de la carta que defiende
        private String ataqueNombre; // Nombre del ataque usado

        // getters y setters
        public String getAtacanteId() { return atacanteId; }
        public void setAtacanteId(String atacanteId) { this.atacanteId = atacanteId; }
        public String getDefensorId() { return defensorId; }
        public void setDefensorId(String defensorId) { this.defensorId = defensorId; }
        public String getAtaqueNombre() { return ataqueNombre; }
        public void setAtaqueNombre(String ataqueNombre) { this.ataqueNombre = ataqueNombre; }
    }
}