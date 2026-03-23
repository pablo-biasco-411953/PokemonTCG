package com.pokemon.tcg.controller;

import com.pokemon.tcg.model.Card;
import com.pokemon.tcg.repository.CardRepository;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/cards")
public class CardController {
    private final CardRepository cardRepo;

    public CardController(CardRepository cardRepo) {
        this.cardRepo = cardRepo;
    }

    @GetMapping
    public List<Card> getAll() {
        return cardRepo.findAll();
    }
}
