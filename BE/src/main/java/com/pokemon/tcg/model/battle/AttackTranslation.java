package com.pokemon.tcg.model.battle;

import jakarta.persistence.*;

@Entity
@Table(name = "attack_translations")
public class AttackTranslation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ataque_id", nullable = false)
    private Long ataqueId;

    @Column(nullable = false)
    private String lang;

    @Column(nullable = false)
    private String nombre;

    @Column(length = 2000)
    private String texto;

    public AttackTranslation() {}

    public AttackTranslation(Long ataqueId, String lang, String nombre, String texto) {
        this.ataqueId = ataqueId;
        this.lang = lang;
        this.nombre = nombre;
        this.texto = texto;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getAtaqueId() { return ataqueId; }
    public void setAtaqueId(Long ataqueId) { this.ataqueId = ataqueId; }

    public String getLang() { return lang; }
    public void setLang(String lang) { this.lang = lang; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getTexto() { return texto; }
    public void setTexto(String texto) { this.texto = texto; }
}
