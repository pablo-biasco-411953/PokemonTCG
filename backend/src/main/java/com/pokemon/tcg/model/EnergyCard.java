package com.pokemon.tcg.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import java.util.List;

@Entity
@DiscriminatorValue("Energy")
@JsonTypeName("Energy")
public class EnergyCard extends Card {
    private boolean especial; // true = Especial, false = Basica

    public EnergyCard() {}

    public boolean isEspecial() { return especial; }
    public void setEspecial(boolean especial) { this.especial = especial; }

    @Override
    @JsonProperty("subtypes")
    public void setSubtypes(List<String> list) {
        super.setSubtypes(list);
        if (list != null && list.contains("Special")) {
            this.especial = true;
        } else {
            this.especial = false;
        }
    }
}
