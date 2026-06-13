package com.pokemon.tcg.model.battle.command;

import com.pokemon.tcg.model.Card;
import com.pokemon.tcg.model.battle.CartaEnJuego;
import com.pokemon.tcg.model.battle.Partida;
import com.pokemon.tcg.model.battle.TableroJugador;

import java.util.Iterator;
import java.util.Random;

public class AttachEnergyFromDiscardToBenchByCoinsCommand implements BattleCommand {
    private static final Random RANDOM = new Random();

    private final int flips;
    private final String energyType;

    public AttachEnergyFromDiscardToBenchByCoinsCommand(int flips, String energyType) {
        this.flips = Math.max(1, flips);
        this.energyType = energyType;
    }

    @Override
    public void execute(Partida partida, TableroJugador atacante, TableroJugador defensor) {
        int heads = 0;
        for (int i = 0; i < flips; i++) {
            boolean isHeads = RANDOM.nextBoolean();
            partida.getUltimasMonedasLanzadas().add(isHeads);
            if (isHeads) heads++;
        }
        if (heads <= 0 || atacante.getBanca().isEmpty()) return;

        CartaEnJuego target = atacante.getBanca().get(0);
        int attached = 0;
        Iterator<Card> iterator = atacante.getPilaDescarte().iterator();
        while (iterator.hasNext() && attached < heads) {
            Card card = iterator.next();
            if (!matches(card)) continue;
            iterator.remove();
            target.getEnergiasUnidas().add(card);
            attached++;
        }

        if (attached > 0) {
            String actor = atacante == partida.getJugador() ? "JUGADOR" : "BOT";
            partida.getTurnLogs().add("ENERGY_ATTACHED_FROM_DISCARD:" + actor + ":" + attached);
        }
    }

    private boolean matches(Card card) {
        if (card == null || card.getSupertype() == null || !card.getSupertype().equalsIgnoreCase("Energy")) {
            return false;
        }
        if (energyType == null) return true;
        String value = card.getTipo();
        if (value == null || value.isBlank() || "Energy".equalsIgnoreCase(value)) {
            value = card.getNombre();
        }
        return value != null && value.toLowerCase().contains(energyType.toLowerCase());
    }
}
