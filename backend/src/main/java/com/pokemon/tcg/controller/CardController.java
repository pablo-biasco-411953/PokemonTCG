package com.pokemon.tcg.controller;

import com.pokemon.tcg.model.Card;
import com.pokemon.tcg.service.CardCatalogService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/cards")
public class CardController {
    private final CardCatalogService cardCatalogService;

    public CardController(CardCatalogService cardCatalogService) {
        this.cardCatalogService = cardCatalogService;
    }

    @GetMapping
    public ResponseEntity<List<Card>> getAll() {
        try {
            List<Card> cards = cardCatalogService.getCatalogo();
            return ResponseEntity.ok(cards);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
