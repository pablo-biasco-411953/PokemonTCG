package com.pokemon.tcg.model.battle.command;

import com.pokemon.tcg.model.Card;
import com.pokemon.tcg.model.battle.Partida;
import com.pokemon.tcg.model.battle.TableroJugador;
import java.util.HashSet;
import java.util.Set;

public class AddDamageByDifferentBasicEnergyTypesCommand implements BattleCommand {
    private int damagePerType;

    public AddDamageByDifferentBasicEnergyTypesCommand(int damagePerType) {
        this.damagePerType = damagePerType;
    }

    @Override
    public void execute(Partida partida, TableroJugador atacante, TableroJugador defensor) {
        if (atacante.getActivo() == null) return;
        
        Set<String> uniqueTypes = new HashSet<>();
        for (Card energy : atacante.getActivo().getEnergiasUnidas()) {
            if (energy.getNombre() != null && energy.getNombre().toLowerCase().contains("double")) {
                continue; // Not a basic energy
            }
            if (energy.getTipo() != null && !energy.getTipo().isEmpty() && !"Energy".equalsIgnoreCase(energy.getTipo())) {
                uniqueTypes.add(energy.getTipo());
            } else if (energy.getNombre() != null) {
                // Infer type from name for basic energies
                String lowerName = energy.getNombre().toLowerCase();
                if (lowerName.contains("grass")) uniqueTypes.add("Grass");
                else if (lowerName.contains("fire")) uniqueTypes.add("Fire");
                else if (lowerName.contains("water")) uniqueTypes.add("Water");
                else if (lowerName.contains("lightning") || lowerName.contains("electric")) uniqueTypes.add("Lightning");
                else if (lowerName.contains("psychic")) uniqueTypes.add("Psychic");
                else if (lowerName.contains("fighting")) uniqueTypes.add("Fighting");
                else if (lowerName.contains("darkness")) uniqueTypes.add("Darkness");
                else if (lowerName.contains("metal")) uniqueTypes.add("Metal");
                else if (lowerName.contains("fairy")) uniqueTypes.add("Fairy");
            }
        }

        int extraDamage = uniqueTypes.size() * damagePerType;
        if (extraDamage > 0) {
            System.out.println("🌈 Diferentes tipos de energía básica: " + uniqueTypes.size() + ". Añadiendo " + extraDamage + " daño.");
            partida.getExecutionQueue().addFirst(new DamageCommand(extraDamage));
        }
    }
}
