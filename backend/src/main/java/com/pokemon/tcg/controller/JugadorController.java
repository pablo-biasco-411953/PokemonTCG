package com.pokemon.tcg.controller;

import com.pokemon.tcg.model.Jugador;
import com.pokemon.tcg.repository.JugadorRepository;
import com.pokemon.tcg.dto.JugadorDatosResponse;
import com.pokemon.tcg.dto.DebugSetSobresRequest;
import com.pokemon.tcg.dto.PersonalizacionRequest;
import com.pokemon.tcg.dto.SantoroQuestResponse;
import com.pokemon.tcg.dto.SantoroTrackingRequest;
import com.pokemon.tcg.dto.TradeExecutionRequest;
import com.pokemon.tcg.model.Card;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;

@RestController
@RequestMapping("/api/jugadores")
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
            
            JugadorDatosResponse response = new JugadorDatosResponse(
                j.getUsername(), 
                j.getSobresDisponibles(), 
                highlightCardCount(j),
                j.getSantoCoins(),
                j.getCharacterId(),
                j.getSkinColor(),
                j.getHairColor(),
                j.getEyeColor(),
                j.getHeight(),
                j.isPikachuCompanion()
            );

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error interno: " + e.getMessage());
        }
    }

    private int highlightCardCount(Jugador j) {
        return (j.getColeccion() != null) ? j.getColeccion().size() : 0;
    }

    @PostMapping("/{username}/personalizacion")
    public ResponseEntity<?> guardarPersonalizacion(@PathVariable String username,
                                                    @RequestBody PersonalizacionRequest request) {
        try {
            Jugador j = jugadorRepo.findByUsername(username);
            if (j == null) return ResponseEntity.status(404).body("Jugador no encontrado");

            j.setCharacterId(request.getCharacterId());
            j.setSkinColor(request.getSkinColor());
            j.setHairColor(request.getHairColor());
            j.setEyeColor(request.getEyeColor());
            j.setHeight(request.getHeight());
            j.setPikachuCompanion(request.isPikachuCompanion());

            jugadorRepo.save(j);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error al guardar personalización: " + e.getMessage());
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

    @PostMapping("/{username}/debug/sobres")
    public ResponseEntity<?> debugSetSobres(@PathVariable String username,
                                            @RequestBody DebugSetSobresRequest request) {
        try {
            Jugador jugador = jugadorRepo.findByUsername(username);
            if (jugador == null) return ResponseEntity.status(404).body("Jugador no encontrado");

            int cantidad = Math.max(0, request.getCantidad());
            jugador.setSobresDisponibles(cantidad);
            jugadorRepo.save(jugador);

            JugadorDatosResponse response = new JugadorDatosResponse(
                    jugador.getUsername(),
                    jugador.getSobresDisponibles(),
                    jugador.getColeccion() != null ? jugador.getColeccion().size() : 0,
                    jugador.getSantoCoins(),
                    jugador.getCharacterId(),
                    jugador.getSkinColor(),
                    jugador.getHairColor(),
                    jugador.getEyeColor(),
                    jugador.getHeight(),
                    jugador.isPikachuCompanion()
            );

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error al setear sobres: " + e.getMessage());
        }
    }

    @PostMapping("/{username}/coins/reward")
    public ResponseEntity<?> rewardCoins(@PathVariable String username,
                                         @RequestBody(required = false) Map<String, Object> payload) {
        try {
            Jugador jugador = jugadorRepo.findByUsername(username);
            if (jugador == null) return ResponseEntity.status(404).body("Jugador no encontrado");

            int amount = readPositiveAmount(payload);
            jugador.setSantoCoins(jugador.getSantoCoins() + amount);
            jugadorRepo.save(jugador);
            return ResponseEntity.ok(toDatosResponse(jugador));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error al acreditar SantoCoins: " + e.getMessage());
        }
    }

    @PostMapping("/{username}/coins/spend")
    public ResponseEntity<?> spendCoins(@PathVariable String username,
                                        @RequestBody(required = false) Map<String, Object> payload) {
        try {
            Jugador jugador = jugadorRepo.findByUsername(username);
            if (jugador == null) return ResponseEntity.status(404).body("Jugador no encontrado");

            int amount = readPositiveAmount(payload);
            if (jugador.getSantoCoins() < amount) {
                return ResponseEntity.badRequest().body("SantoCoins insuficientes");
            }
            jugador.setSantoCoins(jugador.getSantoCoins() - amount);
            jugadorRepo.save(jugador);
            return ResponseEntity.ok(toDatosResponse(jugador));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error al gastar SantoCoins: " + e.getMessage());
        }
    }

    @PostMapping("/{username}/packs/buy")
    public ResponseEntity<?> buyPacks(@PathVariable String username,
                                      @RequestBody(required = false) Map<String, Object> payload) {
        try {
            Jugador jugador = jugadorRepo.findByUsername(username);
            if (jugador == null) return ResponseEntity.status(404).body("Jugador no encontrado");

            int amount = readPositiveAmount(payload);
            int cost = switch (amount) {
                case 1 -> 80;
                case 3 -> 200;
                case 5 -> 300;
                default -> throw new IllegalArgumentException("Bundle no disponible");
            };

            if (jugador.getSantoCoins() < cost) {
                return ResponseEntity.badRequest().body("SantoCoins insuficientes");
            }

            jugador.setSantoCoins(jugador.getSantoCoins() - cost);
            jugador.setSobresDisponibles(jugador.getSobresDisponibles() + amount);
            jugadorRepo.save(jugador);
            return ResponseEntity.ok(toDatosResponse(jugador));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error comprando sobres: " + e.getMessage());
        }
    }

    private int readPositiveAmount(Map<String, Object> payload) {
        if (payload == null || !(payload.get("amount") instanceof Number number)) {
            throw new IllegalArgumentException("Amount invalido");
        }
        int amount = number.intValue();
        if (amount <= 0) {
            throw new IllegalArgumentException("Amount debe ser positivo");
        }
        return amount;
    }

    private JugadorDatosResponse toDatosResponse(Jugador jugador) {
        return new JugadorDatosResponse(
                jugador.getUsername(),
                jugador.getSobresDisponibles(),
                jugador.getColeccion() != null ? jugador.getColeccion().size() : 0,
                jugador.getSantoCoins(),
                jugador.getCharacterId(),
                jugador.getSkinColor(),
                jugador.getHairColor(),
                jugador.getEyeColor(),
                jugador.getHeight(),
                jugador.isPikachuCompanion()
        );
    }

    @GetMapping("/{username}/quests/santoro")
    public ResponseEntity<?> getSantoroQuest(@PathVariable String username) {
        try {
            Jugador jugador = jugadorRepo.findByUsername(username);
            if (jugador == null) return ResponseEntity.status(404).body("Jugador no encontrado");
            return ResponseEntity.ok(toSantoroQuestResponse(jugador));
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error al cargar mision de Santoro: " + e.getMessage());
        }
    }

    @PostMapping("/{username}/quests/santoro/tracking")
    public ResponseEntity<?> setSantoroTracking(@PathVariable String username,
                                                @RequestBody SantoroTrackingRequest request) {
        try {
            Jugador jugador = jugadorRepo.findByUsername(username);
            if (jugador == null) return ResponseEntity.status(404).body("Jugador no encontrado");

            boolean tracking = request.isTracking() && !jugador.isSantoroGiftClaimed();
            jugador.setSantoroQuestTracking(tracking);
            if (tracking && (jugador.getSantoroQuestState() == null || jugador.getSantoroQuestState().isBlank())) {
                jugador.setSantoroQuestState("AVAILABLE");
            }
            jugadorRepo.save(jugador);
            return ResponseEntity.ok(toSantoroQuestResponse(jugador));
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error al actualizar tracking de Santoro: " + e.getMessage());
        }
    }

    @PostMapping("/{username}/quests/santoro/claim")
    public ResponseEntity<?> claimSantoroGift(@PathVariable String username) {
        try {
            Jugador jugador = jugadorRepo.findByUsername(username);
            if (jugador == null) return ResponseEntity.status(404).body("Jugador no encontrado");

            if (!jugador.isSantoroGiftClaimed()) {
                jugador.setSobresDisponibles(jugador.getSobresDisponibles() + 10);
                jugador.setSantoroGiftClaimed(true);
                jugador.setSantoroQuestTracking(false);
                jugador.setSantoroQuestState("COMPLETED");
                jugadorRepo.save(jugador);
            }

            return ResponseEntity.ok(toSantoroQuestResponse(jugador));
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error al reclamar regalo de Santoro: " + e.getMessage());
        }
    }

    private SantoroQuestResponse toSantoroQuestResponse(Jugador jugador) {
        String state = jugador.getSantoroQuestState();
        if (state == null || state.isBlank()) {
            state = jugador.isSantoroGiftClaimed() ? "COMPLETED" : "AVAILABLE";
        }
        return new SantoroQuestResponse(
                jugador.isSantoroGiftClaimed(),
                jugador.isSantoroQuestTracking(),
                state,
                jugador.getSobresDisponibles()
        );
    }

    @PostMapping("/trade/execute")
    @Transactional
    public ResponseEntity<?> executeTrade(@RequestBody TradeExecutionRequest request) {
        try {
            Jugador jA = jugadorRepo.findByUsername(request.getPlayerA());
            if (jA == null) return ResponseEntity.status(404).body("Jugador A no encontrado: " + request.getPlayerA());

            Jugador jB = jugadorRepo.findByUsername(request.getPlayerB());
            if (jB == null) return ResponseEntity.status(404).body("Jugador B no encontrado: " + request.getPlayerB());

            List<Card> colA = new ArrayList<>(jA.getColeccion());
            List<Card> colB = new ArrayList<>(jB.getColeccion());

            // Swapping Player A cards -> Player B
            if (request.getPlayerACardIds() != null) {
                for (String cardId : request.getPlayerACardIds()) {
                    Card found = null;
                    for (Card c : colA) {
                        if (c.getId().equals(cardId)) {
                            found = c;
                            break;
                        }
                    }
                    if (found == null) {
                        return ResponseEntity.badRequest().body("Jugador " + request.getPlayerA() + " no posee la carta con ID: " + cardId);
                    }
                    colA.remove(found);
                    colB.add(found);
                }
            }

            // Swapping Player B cards -> Player A
            if (request.getPlayerBCardIds() != null) {
                for (String cardId : request.getPlayerBCardIds()) {
                    Card found = null;
                    for (Card c : colB) {
                        if (c.getId().equals(cardId)) {
                            found = c;
                            break;
                        }
                    }
                    if (found == null) {
                        return ResponseEntity.badRequest().body("Jugador " + request.getPlayerB() + " no posee la carta con ID: " + cardId);
                    }
                    colB.remove(found);
                    colA.add(found);
                }
            }

            jA.setColeccion(colA);
            jB.setColeccion(colB);

            jugadorRepo.save(jA);
            jugadorRepo.save(jB);

            System.out.println("✅ Trade completado con éxito entre " + request.getPlayerA() + " y " + request.getPlayerB());
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error al ejecutar trade: " + e.getMessage());
        }
    }
}
