package com.pokemon.tcg.dto;

public class UseAbilityRequest {
    private String sourcePokemonId;
    private String abilityName;
    private String targetPokemonId;
    private String extraParams;

    public String getSourcePokemonId() { return sourcePokemonId; }
    public void setSourcePokemonId(String sourcePokemonId) { this.sourcePokemonId = sourcePokemonId; }

    public String getAbilityName() { return abilityName; }
    public void setAbilityName(String abilityName) { this.abilityName = abilityName; }

    public String getTargetPokemonId() { return targetPokemonId; }
    public void setTargetPokemonId(String targetPokemonId) { this.targetPokemonId = targetPokemonId; }

    public String getExtraParams() { return extraParams; }
    public void setExtraParams(String extraParams) { this.extraParams = extraParams; }
}
