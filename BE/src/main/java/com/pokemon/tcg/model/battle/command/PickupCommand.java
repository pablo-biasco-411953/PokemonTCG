package com.pokemon.tcg.model.battle.command;

import com.pokemon.tcg.model.Card;
import com.pokemon.tcg.model.battle.Partida;
import com.pokemon.tcg.model.battle.PendingBattleAction;
import com.pokemon.tcg.model.battle.TableroJugador;
import com.pokemon.tcg.model.battle.state.EstadoEsperandoInteraccion;

import java.util.ArrayList;
import java.util.List;

public class PickupCommand implements BattleCommand {
    private final int maxAmount;

    public PickupCommand(int maxAmount) {
        this.maxAmount = maxAmount;
    }

    @Override
    public void execute(Partida partida, TableroJugador atacante, TableroJugador defensor) {
        List<Card> itemsInDiscard = atacante.getPilaDescarte().stream()
                .filter(c -> "Trainer".equalsIgnoreCase(c.getSupertype()) && c.getSubtypes() != null && c.getSubtypes().contains("Item"))
                .toList();

        if (itemsInDiscard.isEmpty()) {
            System.out.println("⚠️ Pickup: No hay cartas de tipo Item en la pila de descarte.");
            partida.getTurnLogs().add("PICKUP_NO_ITEMS_FOUND");
            return;
        }

        if (atacante == partida.getBot()) {
            // IA del Bot: Autoselecciona hasta 2 items del descarte
            List<String> names = new ArrayList<>();
            int retrieved = 0;
            for (Card card : itemsInDiscard) {
                if (retrieved >= maxAmount) break;
                atacante.getPilaDescarte().remove(card);
                atacante.getMano().add(card);
                names.add(card.getNombre());
                retrieved++;
            }
            partida.getTurnLogs().add("PICKUP_RESOLVED:BOT:" + String.join(",", names));
            return;
        }

        // Jugador: Lanzamos la acción interactiva de descarte a la mano
        PendingBattleAction action = new PendingBattleAction();
        action.setActor(partida.getJugadorUsername());
        action.setType("SELECT_DISCARD_ITEMS_FOR_PICKUP");
        action.setPrompt("Pickup: Elegí hasta " + maxAmount + " cartas de tipo Item de tu descarte para poner en tu mano.");
        action.setDestination("HAND");
        action.setMinSelections(0);
        action.setMaxSelections(Math.min(maxAmount, itemsInDiscard.size()));
        action.setEndsTurn(true);
        action.setOptions(itemsInDiscard.stream()
                .map(card -> new PendingBattleAction.Option(card.getId(), card.getNombre(), card.getImagen()))
                .toList());

        partida.setPendingAction(action);
        partida.transicionarA(new EstadoEsperandoInteraccion());
    }
}
