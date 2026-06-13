package com.pokemon.tcg.model.battle.command;

import com.pokemon.tcg.model.Card;
import com.pokemon.tcg.model.battle.CartaEnJuego;
import com.pokemon.tcg.model.battle.Partida;
import com.pokemon.tcg.model.battle.TableroJugador;

public class DiscardTopDeckAttachEnergyCommand implements BattleCommand {
    private final String energyType;

    public DiscardTopDeckAttachEnergyCommand(String energyType) {
        this.energyType = energyType;
    }

    public String getEnergyType() {
        return energyType;
    }

    @Override
    public void execute(Partida partida, TableroJugador atacante, TableroJugador defensor) {
        if (atacante.getMazo() == null || atacante.getMazo().isEmpty()) {
            return;
        }

        Card topCard = atacante.getMazo().remove(0);
        atacante.getPilaDescarte().add(topCard);

        String actor = (atacante == partida.getJugador()) ? "JUGADOR" : "BOT";
        partida.getTurnLogs().add("DISCARD_TOP_DECK:" + actor + ":" + limpiar(topCard.getNombre()) + ":" + topCard.getId());

        boolean isEnergy = topCard.getSupertype() != null && topCard.getSupertype().equalsIgnoreCase("Energy");
        boolean matchesType = topCard.getNombre() != null && topCard.getNombre().toLowerCase().contains(energyType.toLowerCase());

        if (isEnergy && matchesType) {
            CartaEnJuego active = atacante.getActivo();
            if (active != null) {
                active.getEnergiasUnidas().add(topCard);
                partida.getTurnLogs().add("ATTACHED_FROM_DISCARD:" + actor + ":" + limpiar(topCard.getNombre()) + ":" + limpiar(active.getCard().getNombre()));
            }
        }
    }

    private String limpiar(String value) {
        return value == null ? "" : value.replace(':', '-').replace('\n', ' ').replace('\r', ' ').trim();
    }
}
