package com.pokemon.tcg.model.battle.command;

import com.pokemon.tcg.model.Card;
import com.pokemon.tcg.model.battle.Partida;
import com.pokemon.tcg.model.battle.TableroJugador;

public class MagcargoMagmaMantleCommand implements BattleCommand {

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
        boolean isFire = topCard.getNombre() != null && topCard.getNombre().toLowerCase().contains("fire");

        if (isEnergy && isFire) {
            partida.getExecutionQueue().addFirst(new DamageCommand(50));
            partida.getTurnLogs().add("MAGMA_MANTLE_BOOST:" + actor + ":" + limpiar(topCard.getNombre()));
        }
    }

    private String limpiar(String value) {
        return value == null ? "" : value.replace(':', '-').replace('\n', ' ').replace('\r', ' ').trim();
    }
}
