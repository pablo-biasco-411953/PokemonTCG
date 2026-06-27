package com.pokemon.tcg.model.battle.command;

import com.pokemon.tcg.model.battle.Partida;
import com.pokemon.tcg.model.battle.TableroJugador;

public class ConditionalDamageMultiplierCommand implements BattleCommand {
    private int baseDamage;
    private int multiplier;
    private String conditionType; // e.g. "PRIZE_CARDS", "OPPONENT_TYPE"
    private String conditionValue;

    public ConditionalDamageMultiplierCommand(int baseDamage, int multiplier, String conditionType, String conditionValue) {
        this.baseDamage = baseDamage;
        this.multiplier = multiplier;
        this.conditionType = conditionType;
        this.conditionValue = conditionValue;
    }

    @Override
    public void execute(Partida partida, TableroJugador atacante, TableroJugador defensor) {
        int finalDamage = baseDamage;

        if ("PRIZE_CARDS".equals(conditionType)) {
            finalDamage += (atacante.getPremios().size() * multiplier);
        } else if ("OPPONENT_TYPE".equals(conditionType)) {
            if (defensor.getActivo() != null && defensor.getActivo().getCard().getTipo().equalsIgnoreCase(conditionValue)) {
                finalDamage += multiplier;
            }
        } else if ("BENCHED_POKEMON".equals(conditionType)) {
            finalDamage += (atacante.getBanca().size() * multiplier);
        } else if ("BOTH_ACTIVE_ENERGY".equals(conditionType)) {
            int energies = 0;
            if (atacante.getActivo() != null) {
                energies += atacante.getActivo().getEnergiasUnidas().size();
            }
            if (defensor.getActivo() != null) {
                energies += defensor.getActivo().getEnergiasUnidas().size();
            }
            finalDamage += (energies * multiplier);
        } else if ("HAS_ENERGY_TYPE".equals(conditionType)) {
            if (atacante.getActivo() != null) {
                boolean hasEnergy = atacante.getActivo().getEnergiasUnidas().stream()
                        .anyMatch(energy -> isEnergyOfType(energy, conditionValue));
                if (hasEnergy) {
                    finalDamage += multiplier;
                }
            }
        } else if ("ENERGY_COUNT_OF_TYPE".equals(conditionType)) {
            if (atacante.getActivo() != null) {
                long count = atacante.getActivo().getEnergiasUnidas().stream()
                        .filter(energy -> isEnergyOfType(energy, conditionValue))
                        .count();
                finalDamage += (count * multiplier);
            }
        } else if ("OPPONENT_HAS_DAMAGE_COUNTERS".equals(conditionType)) {
            if (defensor.getActivo() != null) {
                int maxHp = 0;
                try {
                    maxHp = Integer.parseInt(defensor.getActivo().getCard().getHp());
                } catch (NumberFormatException ignored) {
                    maxHp = defensor.getActivo().getHpActual();
                }
                if (defensor.getActivo().getHpActual() < maxHp) {
                    finalDamage += multiplier;
                }
            }
        }

        if (finalDamage > 0) {
            partida.getExecutionQueue().add(new DamageCommand(finalDamage));
        }
    }

    private boolean isEnergyOfType(com.pokemon.tcg.model.Card card, String targetType) {
        if (card.getSupertype() != null && !card.getSupertype().equalsIgnoreCase("Energy")) {
            return false;
        }
        String name = card.getNombre() == null ? "" : card.getNombre();
        String type = card.getTipo();
        if (type == null || type.isBlank() || "Energy".equalsIgnoreCase(type)) {
            type = name;
        }
        String normalized = normalizeType(type);
        return normalized.equalsIgnoreCase(targetType);
    }

    private String normalizeType(String value) {
        String text = value == null ? "" : value.toLowerCase();
        if (text.contains("grass") || text.contains("planta")) return "Grass";
        if (text.contains("fire") || text.contains("fuego")) return "Fire";
        if (text.contains("water") || text.contains("agua")) return "Water";
        if (text.contains("lightning") || text.contains("electrica") || text.contains("rayo")) return "Lightning";
        if (text.contains("psychic") || text.contains("psiquica")) return "Psychic";
        if (text.contains("fighting") || text.contains("lucha")) return "Fighting";
        if (text.contains("darkness") || text.contains("siniestra") || text.contains("oscuridad")) return "Darkness";
        if (text.contains("metal") || text.contains("acero")) return "Metal";
        if (text.contains("dragon")) return "Dragon";
        if (text.contains("fairy") || text.contains("hada")) return "Fairy";
        if (text.contains("colorless") || text.contains("incolora")) return "Colorless";
        return value == null ? "" : value;
    }

    public int getBaseDamage() {
        return baseDamage;
    }

    public int getMultiplier() {
        return multiplier;
    }

    public String getConditionType() {
        return conditionType;
    }

    public String getConditionValue() {
        return conditionValue;
    }
}
