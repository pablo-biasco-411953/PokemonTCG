package com.pokemon.tcg.model.battle.command;

import com.pokemon.tcg.model.Card;
import com.pokemon.tcg.model.battle.PendingBattleAction;
import com.pokemon.tcg.model.battle.Partida;
import com.pokemon.tcg.model.battle.TableroJugador;
import com.pokemon.tcg.model.battle.state.EstadoEsperandoInteraccion;

import java.util.List;

/**
 * PeekTopDeckCommand: Shows the player the top N cards of their deck
 * and lets them put them back in any order (Clairvoyant Eye, etc.).
 *
 * The bot simply keeps the cards in the same order.
 */
public class PeekTopDeckCommand implements BattleCommand {

    private final int count;
    private final String prompt;

    public PeekTopDeckCommand(int count, String prompt) {
        this.count = count;
        this.prompt = prompt;
    }

    @Override
    public void execute(Partida partida, TableroJugador atacante, TableroJugador defensor) {
        List<Card> mazo = atacante.getMazo();
        int available = Math.min(count, mazo.size());
        if (available == 0) return;

        // The bot simply leaves the cards in place (no advantage gained).
        if (atacante == partida.getBot()) {
            partida.getTurnLogs().add("DECK_PEEKED:BOT:" + available);
            return;
        }

        // Build options from the top N cards (visible to player, stay in deck for now).
        List<Card> topCards = mazo.subList(0, available);

        PendingBattleAction action = new PendingBattleAction();
        action.setActor(partida.getJugadorUsername());
        action.setType("REORDER_TOP_DECK");
        action.setPrompt(prompt);
        action.setDestination("TOP_DECK");
        // Player must reorder exactly as many cards as were peeked.
        action.setMinSelections(available);
        action.setMaxSelections(available);
        
        List<PendingBattleAction.Option> optionsList = new java.util.ArrayList<>();
        for (int i = 0; i < available; i++) {
            Card card = topCards.get(i);
            optionsList.add(new PendingBattleAction.Option(String.valueOf(i), card.getNombre(), card.getImagen()));
        }
        action.setOptions(optionsList);

        partida.setPendingAction(action);
        partida.transicionarA(new EstadoEsperandoInteraccion());
    }
}
