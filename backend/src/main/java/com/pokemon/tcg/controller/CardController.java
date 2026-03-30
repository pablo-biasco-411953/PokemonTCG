package com.pokemon.tcg.controller;

import com.pokemon.tcg.model.Card;
import com.pokemon.tcg.repository.CardRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/cards")
@CrossOrigin(origins = "http://localhost:4200")
public class CardController {
    private final CardRepository cardRepo;

    public CardController(CardRepository cardRepo) {
        this.cardRepo = cardRepo;
    }

    @GetMapping
    public ResponseEntity<List<Card>> getAll() {
        try {
            List<Card> cards = cardRepo.findAll();
            return ResponseEntity.ok(cards);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
