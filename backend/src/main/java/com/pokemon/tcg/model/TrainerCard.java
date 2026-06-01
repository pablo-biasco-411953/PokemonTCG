package com.pokemon.tcg.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import java.util.List;

@Entity
@DiscriminatorValue("Trainer")
@JsonTypeName("Trainer")
public class TrainerCard extends Card {

    public enum TrainerType {
        OBJETO,
        PARTIDARIO,
        ESTADIO,
        HERRAMIENTA
    }

    @Enumerated(EnumType.STRING)
    private TrainerType trainerType;

    public TrainerCard() {}

    public TrainerType getTrainerType() { return trainerType; }
    public void setTrainerType(TrainerType trainerType) { this.trainerType = trainerType; }

    @Override
    @JsonProperty("subtypes")
    public void setSubtypes(List<String> list) {
        super.setSubtypes(list);
        if (list != null) {
            if (list.contains("Supporter")) {
                this.trainerType = TrainerType.PARTIDARIO;
            } else if (list.contains("Stadium")) {
                this.trainerType = TrainerType.ESTADIO;
            } else if (list.contains("Pokémon Tool") || list.contains("Tool")) {
                this.trainerType = TrainerType.HERRAMIENTA;
            } else {
                this.trainerType = TrainerType.OBJETO;
            }
        }
    }
}
