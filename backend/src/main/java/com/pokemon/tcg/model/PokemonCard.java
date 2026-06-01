package com.pokemon.tcg.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.pokemon.tcg.model.battle.Ataque;
import com.pokemon.tcg.model.converter.AtaqueListConverter;
import com.pokemon.tcg.model.converter.MapListConverter;
import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Entity
@DiscriminatorValue("Pokémon")
@JsonTypeName("Pokémon")
public class PokemonCard extends Card {
    private String hp;
    private String evolvesFrom;
    private int costoRetirada;

    @Convert(converter = AtaqueListConverter.class)
    @Column(name = "attacks", length = 2000)
    @JsonProperty("ataques")
    private List<Ataque> ataques = new ArrayList<>();

    @Convert(converter = MapListConverter.class)
    @Column(name = "weakness", length = 1000)
    @JsonProperty("debilidades")
    private List<Map<String, String>> debilidades = new ArrayList<>();

    @Convert(converter = MapListConverter.class)
    @Column(name = "resistance", length = 1000)
    @JsonProperty("resistencias")
    private List<Map<String, String>> resistencias = new ArrayList<>();

    public PokemonCard() {}

    @JsonProperty("ataques")
    public void cargarAtaquesDesdeJson(List<Map<String, Object>> jsonAtaques) {
        List<Ataque> nuevaLista = new ArrayList<>();
        if (jsonAtaques != null) {
            for (Map<String, Object> map : jsonAtaques) {
                Ataque atk = new Ataque();
                atk.setNombre((String) map.get("nombre"));

                Object dmgObj = map.get("dano") != null ? map.get("dano") : map.get("damage");
                if (dmgObj != null) {
                    String dmgStr = String.valueOf(dmgObj).replaceAll("[^0-9]", "");
                    atk.setDanio(dmgStr.isEmpty() ? 0 : Integer.parseInt(dmgStr));
                }

                List<String> costo = (List<String>) map.get("costo");
                atk.setTiposEnergia(costo != null ? costo : new ArrayList<>());

                String textoAtk = (String) map.get("texto");
                atk.setTexto(textoAtk != null ? textoAtk : "");

                nuevaLista.add(atk);
            }
        }
        this.ataques = nuevaLista;
    }

    @JsonProperty("debilidades")
    public void setDebilidades(List<Map<String, String>> lista) {
        this.debilidades = (lista != null) ? lista : new ArrayList<>();
    }

    @JsonProperty("resistencias")
    public void setResistencias(List<Map<String, String>> lista) {
        this.resistencias = (lista != null) ? lista : new ArrayList<>();
    }

    // Getters y Setters
    @Override
    public String getHp() { return hp; }
    public void setHp(String hp) { this.hp = hp; }

    @Override
    public String getEvolvesFrom() { return evolvesFrom; }
    public void setEvolvesFrom(String evolvesFrom) { this.evolvesFrom = evolvesFrom; }

    @Override
    public int getCostoRetirada() { return costoRetirada; }
    public void setCostoRetirada(int costoRetirada) { this.costoRetirada = costoRetirada; }

    @Override
    public List<Ataque> getAtaques() { return ataques; }
    public void setAtaques(List<Ataque> ataques) { this.ataques = ataques; }

    @Override
    public List<Map<String, String>> getDebilidades() { return debilidades; }
    public void setDebilidadesEx(List<Map<String, String>> debilidades) { this.debilidades = debilidades; }

    @Override
    public List<Map<String, String>> getResistencias() { return resistencias; }
    public void setResistenciasEx(List<Map<String, String>> resistencias) { this.resistencias = resistencias; }
}
