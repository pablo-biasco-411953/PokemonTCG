package com.pokemon.tcg.service.battle;

import com.pokemon.tcg.model.Card;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;

public final class EnergyCostCalculator {
    private EnergyCostCalculator() {}

    public static boolean canPay(List<Card> attached, List<String> cost) {
        if (cost == null || cost.isEmpty()) return true;

        List<EnergyUnit> units = new ArrayList<>();
        if (attached != null) {
            for (Card energy : attached) units.addAll(unitsFrom(energy));
        }

        List<String> requirements = cost.stream().map(EnergyCostCalculator::normalizeType).toList();
        for (String requirement : requirements) {
            if ("Colorless".equals(requirement)) continue;
            int index = findUnit(units, requirement);
            if (index < 0) return false;
            units.remove(index);
        }

        long colorless = requirements.stream().filter("Colorless"::equals).count();
        return units.size() >= colorless;
    }

    private static int findUnit(List<EnergyUnit> units, String requirement) {
        for (int i = 0; i < units.size(); i++) {
            EnergyUnit unit = units.get(i);
            if (!unit.wildcard && unit.type.equals(requirement)) return i;
        }
        for (int i = 0; i < units.size(); i++) {
            if (units.get(i).wildcard) return i;
        }
        return -1;
    }

    public static int colorlessValue(Card energy) {
        return unitsFrom(energy).size();
    }

    private static List<EnergyUnit> unitsFrom(Card energy) {
        String name = energy.getNombre() == null ? "" : energy.getNombre();
        String normalizedName = normalizeText(name);
        if (normalizedName.contains("double colorless") || normalizedName.contains("incolora doble") || normalizedName.contains("doble incolora")) {
            return List.of(new EnergyUnit("Colorless", false), new EnergyUnit("Colorless", false));
        }
        if (normalizedName.contains("rainbow")) {
            String source = energy.getTipo();
            if (source != null && !source.isBlank() && !source.equalsIgnoreCase("Energy")) {
                return List.of(new EnergyUnit(normalizeType(source), false));
            } else {
                return List.of(new EnergyUnit("Colorless", true));
            }
        }

        String source = energy.getTipo();
        if (source == null || source.isBlank() || "Energy".equalsIgnoreCase(source)) source = name;
        return List.of(new EnergyUnit(normalizeType(source), false));
    }

    private static String normalizeType(String value) {
        String text = normalizeText(value);
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

    private static String normalizeText(String value) {
        return Normalizer.normalize(value == null ? "" : value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase();
    }

    private record EnergyUnit(String type, boolean wildcard) {}
}
