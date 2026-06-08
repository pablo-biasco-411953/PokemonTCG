package com.pokemon.tcg.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pokemon.tcg.model.Card;
import com.pokemon.tcg.repository.CardRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

@Service
public class CardCatalogService {
    private final CardRepository cardRepo;
    private final ObjectMapper objectMapper;

    public CardCatalogService(CardRepository cardRepo, ObjectMapper objectMapper) {
        this.cardRepo = cardRepo;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public List<Card> getCatalogo() {
        List<Card> cartas = cardRepo.findAll();
        if (!cartas.isEmpty()) {
            return cartas;
        }

        return sincronizarDesdeJson();
    }

    @Transactional
    public List<Card> sincronizarDesdeJson() {
        List<Card> cartas = leerCardsJson();
        cardRepo.saveAll(cartas);
        cardRepo.flush();
        return cardRepo.findAll();
    }

    private List<Card> leerCardsJson() {
        try (InputStream inputStream = getClass().getResourceAsStream("/cards.json")) {
            if (inputStream == null) {
                throw new IllegalStateException("No se encontro /cards.json dentro del build.");
            }
            return objectMapper.readValue(inputStream, new TypeReference<List<Card>>() {});
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo leer cards.json: " + e.getMessage(), e);
        }
    }
}
