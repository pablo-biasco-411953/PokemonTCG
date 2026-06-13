package com.pokemon.tcg.model.battle.command;

import com.pokemon.tcg.model.Card;
import com.pokemon.tcg.model.battle.CartaEnJuego;
import com.pokemon.tcg.model.battle.Partida;
import com.pokemon.tcg.model.battle.TableroJugador;

public class AddDamageByAttachedEnergyCommand implements BattleCommand {
    private final String energyType;
    private final int damagePerEnergy;
    private final boolean includeOpponentActive;

    public AddDamageByAttachedEnergyCommand(String energyType, int damagePerEnergy, boolean includeOpponentActive) {
        this.energyType = energyType;
        this.damagePerEnergy = damagePerEnergy;
        this.includeOpponentActive = includeOpponentActive;
    }

    @Override
    public void execute(Partida partida, TableroJugador atacante, TableroJugador defensor) {
        int energies = countMatching(atacante.getActivo());
        if (includeOpponentActive) {
            energies += countMatching(defensor.getActivo());
        }
        if (energies > 0) {
            partida.getExecutionQueue().addFirst(new DamageCommand(energies * damagePerEnergy));
        }
    }

    private int countMatching(CartaEnJuego pokemon) {
        if (pokemon == null || pokemon.getEnergiasUnidas() == null) return 0;
        int count = 0;
        for (Card card : pokemon.getEnergiasUnidas()) {
            if (energyType == null || normalizeEnergyType(card).equalsIgnoreCase(energyType)) {
                count++;
            }
        }
        return count;
    }

    private String normalizeEnergyType(Card card) {
        if (card == null) return "";
        String value = card.getTipo();
        if (value == null || value.isBlank() || "Energy".equalsIgnoreCase(value)) {
            value = card.getNombre();
        }
        return value == null ? "" : value.replace(" Energy", "").trim();
    }
}
