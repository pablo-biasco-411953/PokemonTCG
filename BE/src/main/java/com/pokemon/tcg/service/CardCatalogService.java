package com.pokemon.tcg.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pokemon.tcg.model.Card;
import com.pokemon.tcg.model.Habilidad;
import com.pokemon.tcg.model.battle.Ataque;
import com.pokemon.tcg.repository.CardRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.text.Normalizer;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class CardCatalogService {
    private final CardRepository cardRepo;
    private final ObjectMapper objectMapper;
    private final Map<String, List<TranslatedCard>> translations = new ConcurrentHashMap<>();

    public CardCatalogService(CardRepository cardRepo, ObjectMapper objectMapper) {
        this.cardRepo = cardRepo;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public List<Card> getCatalogo() {
        List<Card> cartas = filtrarCartasJugables(cardRepo.findAll());
        boolean tieneHabilidadesCargadas = cartas.stream().anyMatch(c -> c.getHabilidades() != null && !c.getHabilidades().isEmpty());
        if (cartas.size() == 146 && tieneHabilidadesCargadas) {
            normalizarEnergiasXy(cartas);
            eagerlyLoadCards(cartas);
            return cartas;
        }

        List<Card> sync = sincronizarDesdeJson();
        eagerlyLoadCards(sync);
        return sync;
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
        eagerlyLoadCards(guardadas);
        return guardadas;
    }

    private void eagerlyLoadCards(List<Card> cards) {
        if (cards != null) {
            for (Card card : cards) {
                if (card.getSubtypes() != null) card.getSubtypes().size();
                if (card.getReglas() != null) card.getReglas().size();
                if (card.getAtaques() != null) card.getAtaques().size();
                if (card.getDebilidades() != null) card.getDebilidades().size();
                if (card.getResistencias() != null) card.getResistencias().size();
                if (card.getHabilidades() != null) card.getHabilidades().size();
            }
        }
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

    @Transactional
    public List<Card> getCatalogo(String lang) {
        List<Card> baseList = getCatalogo();
        if (lang == null || lang.trim().isEmpty() || "en".equalsIgnoreCase(lang)) {
            return baseList;
        }
        return baseList.stream()
                .map(card -> localizarCarta(card, lang))
                .toList();
    }

    private Card localizarCarta(Card card, String lang) {
        if (lang == null || lang.equalsIgnoreCase("en")) {
            return card;
        }
        List<TranslatedCard> list = getTranslationsFor(lang);
        if (list == null || list.isEmpty()) return card;
        TranslatedCard tr = list.stream().filter(t -> t.getId().equals(card.getId())).findFirst().orElse(null);
        if (tr == null) return card;

        Card clone = new Card();
        clone.setId(card.getId());
        clone.setNombre(tr.getNombre());
        clone.setHp(card.getHp());
        clone.setTipo(card.getTipo());
        clone.setImagen(card.getImagen());
        clone.setCostoRetirada(card.getCostoRetirada());
        clone.setSupertype(card.getSupertype());
        clone.setEvolvesFrom(card.getEvolvesFrom());
        clone.setSubtypes(card.getSubtypes() != null ? new ArrayList<>(card.getSubtypes()) : new ArrayList<>());
        clone.setReglas(tr.getReglas() != null ? new ArrayList<>(tr.getReglas()) : (card.getReglas() != null ? new ArrayList<>(card.getReglas()) : new ArrayList<>()));
        clone.setDebilidades(card.getDebilidades() != null ? new ArrayList<>(card.getDebilidades()) : new ArrayList<>());
        clone.setResistencias(card.getResistencias() != null ? new ArrayList<>(card.getResistencias()) : new ArrayList<>());
        clone.setHabilidades(card.getHabilidades() != null ? new ArrayList<>(card.getHabilidades()) : new ArrayList<>());

        List<Ataque> attacks = new ArrayList<>();
        if (card.getAtaques() != null) {
            for (int i = 0; i < card.getAtaques().size(); i++) {
                Ataque baseAtk = card.getAtaques().get(i);
                Ataque cloneAtk = new Ataque();
                cloneAtk.setId(baseAtk.getId());
                cloneAtk.setDanio(baseAtk.getDanio());
                cloneAtk.setTiposEnergia(baseAtk.getCosto() != null ? new ArrayList<>(baseAtk.getCosto()) : new ArrayList<>());
                cloneAtk.setInteractionType(baseAtk.getInteractionType());
                cloneAtk.setInteractionPrompt(baseAtk.getInteractionPrompt());

                if (tr.getAtaques() != null && i < tr.getAtaques().size()) {
                    TranslatedAttack trAtk = tr.getAtaques().get(i);
                    cloneAtk.setNombre(trAtk.getNombre());
                    cloneAtk.setTexto(trAtk.getTexto());
                } else {
                    cloneAtk.setNombre(baseAtk.getNombre());
                    cloneAtk.setTexto(baseAtk.getTexto());
                }
                attacks.add(cloneAtk);
            }
        }
        clone.reemplazarAtaques(attacks);
        return clone;
    }

    private List<TranslatedCard> getTranslationsFor(String lang) {
        return translations.computeIfAbsent(lang.toLowerCase(), l -> {
            try (InputStream inputStream = getClass().getResourceAsStream("/cards_" + l + ".json")) {
                if (inputStream == null) {
                    System.out.println("⚠️ Translation file /cards_" + l + ".json not found in resources.");
                    return new ArrayList<>();
                }
                return objectMapper.readValue(inputStream, new TypeReference<List<TranslatedCard>>() {});
            } catch (IOException e) {
                System.err.println("❌ Error reading translations for " + l + ": " + e.getMessage());
                return new ArrayList<>();
            }
        });
    }

    public static class TranslatedAttack {
        private String nombre;
        private String texto;

        public TranslatedAttack() {}

        public String getNombre() { return nombre; }
        public void setNombre(String nombre) { this.nombre = nombre; }
        public String getTexto() { return texto; }
        public void setTexto(String texto) { this.texto = texto; }
    }

    public static class TranslatedCard {
        private String id;
        private String nombre;
        private List<String> reglas;
        private List<TranslatedAttack> ataques;

        public TranslatedCard() {}

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getNombre() { return nombre; }
        public void setNombre(String nombre) { this.nombre = nombre; }
        public List<String> getReglas() { return reglas; }
        public void setReglas(List<String> reglas) { this.reglas = reglas; }
        public List<TranslatedAttack> getAtaques() { return ataques; }
        public void setAtaques(List<TranslatedAttack> ataques) { this.ataques = ataques; }
    }
}
