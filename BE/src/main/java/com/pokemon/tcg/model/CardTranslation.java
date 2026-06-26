package com.pokemon.tcg.model;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "card_translations")
public class CardTranslation {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "card_id", nullable = false)
    private String cardId;

    @Column(nullable = false)
    private String lang;

    @Column(nullable = false)
    private String nombre;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "card_translation_rules", joinColumns = @JoinColumn(name = "card_translation_id"))
    @Column(name = "rule", length = 2000)
    private List<String> reglas = new ArrayList<>();

    public CardTranslation() {}

    public CardTranslation(String cardId, String lang, String nombre, List<String> reglas) {
        this.cardId = cardId;
        this.lang = lang;
        this.nombre = nombre;
        this.reglas = reglas != null ? reglas : new ArrayList<>();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCardId() { return cardId; }
    public void setCardId(String cardId) { this.cardId = cardId; }

    public String getLang() { return lang; }
    public void setLang(String lang) { this.lang = lang; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public List<String> getReglas() { return reglas; }
    public void setReglas(List<String> reglas) { this.reglas = reglas; }
}
