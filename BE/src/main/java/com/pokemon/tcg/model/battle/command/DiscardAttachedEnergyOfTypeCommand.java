package com.pokemon.tcg.model.battle.command;

import com.pokemon.tcg.model.battle.Partida;
import com.pokemon.tcg.model.battle.TableroJugador;
import com.pokemon.tcg.model.Card;

import java.util.Iterator;
import java.util.List;

public class DiscardAttachedEnergyOfTypeCommand implements BattleCommand {
    private final String energyType;
    private final Target target;
    private final boolean discardAll;

    public DiscardAttachedEnergyOfTypeCommand(String energyType, Target target) {
        this(energyType, target, false);
    }

    public DiscardAttachedEnergyOfTypeCommand(String energyType, Target target, boolean discardAll) {
        this.energyType = energyType;
        this.target = target;
        this.discardAll = discardAll;
    }

    @Override
    public void execute(Partida partida, TableroJugador atacante, TableroJugador defensor) {
        TableroJugador targetPlayer = (target == Target.SELF) ? atacante : defensor;
        String actor = (atacante == partida.getJugador()) ? "JUGADOR" : "BOT";
        
        if (targetPlayer.getActivo() != null) {
            List<Card> energias = targetPlayer.getActivo().getEnergiasUnidas();
            java.util.Iterator<Card> iterator = energias.iterator();
            while (iterator.hasNext()) {
                Card energy = iterator.next();
                if ("Any".equalsIgnoreCase(energyType) || isEnergyOfType(energy, energyType)) {
                    iterator.remove();
                    targetPlayer.getPilaDescarte().add(energy);
                    partida.getTurnLogs().add("ENERGY_DISCARDED:" + actor + ":" + energy.getNombre());
                    System.out.println("🔥 Se descartó una energía unida a " + targetPlayer.getActivo().getCard().getNombre());
                    if (!discardAll) {
                        break;
                    }
                }
            }
        }
    }

    private boolean isEnergyOfType(Card card, String targetType) {
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
}
