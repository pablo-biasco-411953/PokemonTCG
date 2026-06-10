package com.pokemon.tcg.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pokemon.tcg.model.Card;
import com.pokemon.tcg.repository.CardRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.text.Normalizer;
import java.util.List;
import java.util.Map;

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
        List<Card> cartas = filtrarCartasJugables(cardRepo.findAll());
        if (cartas.size() == 146) {
            normalizarEnergiasXy(cartas);
            return cartas;
        }

        return sincronizarDesdeJson();
    }

    @Transactional
    public List<Card> sincronizarDesdeJson() {
        List<Card> cartas = leerCardsJson();
        cartas = filtrarCartasJugables(cartas);
        if (cartas.isEmpty()) {
            throw new IllegalStateException("cards.json no contiene cartas XY jugables.");
        }
        normalizarEnergiasXy(cartas);
        java.util.Set<String> idsXy1 = cartas.stream()
                .map(Card::getId)
                .collect(java.util.stream.Collectors.toSet());
        cardRepo.findAll().stream()
                .filter(card -> !idsXy1.contains(card.getId()))
                .forEach(cardRepo::delete);
        cardRepo.saveAll(cartas);
        cardRepo.flush();
        List<Card> guardadas = filtrarCartasJugables(cardRepo.findAll());
        normalizarEnergiasXy(guardadas);
        return guardadas;
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

    private List<Card> filtrarCartasJugables(List<Card> cartas) {
        return cartas.stream()
                .filter(this::esSetXy1)
                .filter(card -> esPokemon(card) || esEnergia(card) || esEntrenador(card))
                .toList();
    }

    private boolean esSetXy1(Card card) {
        String id = normalizar(card.getId());
        return id.startsWith("xy1-");
    }

    private boolean esPokemon(Card card) {
        return "pokemon".equals(normalizar(card.getSupertype()));
    }

    private boolean esEnergia(Card card) {
        return "energy".equals(normalizar(card.getSupertype()));
    }

    private boolean esEntrenador(Card card) {
        return "trainer".equals(normalizar(card.getSupertype()));
    }

    private void normalizarEnergiasXy(List<Card> cartas) {
        Map<String, String> tiposPorId = Map.ofEntries(
                Map.entry("xy1-132", "Grass"),
                Map.entry("xy1-133", "Fire"),
                Map.entry("xy1-134", "Water"),
                Map.entry("xy1-135", "Lightning"),
                Map.entry("xy1-136", "Psychic"),
                Map.entry("xy1-137", "Fighting"),
                Map.entry("xy1-138", "Darkness"),
                Map.entry("xy1-139", "Metal"),
                Map.entry("xy1-140", "Fairy")
        );

        boolean hayCambios = false;
        for (Card card : cartas) {
            String tipoCorrecto = tiposPorId.get(card.getId());
            if (tipoCorrecto == null) {
                tipoCorrecto = inferirTipoEnergiaBasica(card);
            }
            if (tipoCorrecto != null && !tipoCorrecto.equalsIgnoreCase(card.getTipo())) {
                card.setTipo(tipoCorrecto);
                hayCambios = true;
            }
        }
        if (hayCambios) {
            cardRepo.saveAll(cartas);
            cardRepo.flush();
        }
    }

    private String inferirTipoEnergiaBasica(Card card) {
        if (!esEnergia(card) || card.getNombre() == null || card.getSubtypes() == null) return null;
        boolean esBasica = card.getSubtypes().stream().anyMatch(s -> "basic".equals(normalizar(s)));
        if (!esBasica) return null;

        String nombre = normalizar(card.getNombre());
        if (nombre.contains("grass energy")) return "Grass";
        if (nombre.contains("fire energy")) return "Fire";
        if (nombre.contains("water energy")) return "Water";
        if (nombre.contains("lightning energy")) return "Lightning";
        if (nombre.contains("psychic energy")) return "Psychic";
        if (nombre.contains("fighting energy")) return "Fighting";
        if (nombre.contains("darkness energy")) return "Darkness";
        if (nombre.contains("metal energy")) return "Metal";
        if (nombre.contains("fairy energy")) return "Fairy";
        return null;
    }

    private String normalizar(String value) {
        if (value == null) return "";
        return Normalizer.normalize(value.trim().toLowerCase(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
    }
}
