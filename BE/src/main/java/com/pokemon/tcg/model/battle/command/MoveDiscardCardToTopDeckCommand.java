package com.pokemon.tcg.model.battle.command;

import com.pokemon.tcg.model.Card;
import com.pokemon.tcg.model.battle.PendingBattleAction;
import com.pokemon.tcg.model.battle.Partida;
import com.pokemon.tcg.model.battle.TableroJugador;

import java.util.List;

public class MoveDiscardCardToTopDeckCommand implements BattleCommand {
    private final Target target;
    private final int maxSelections;
    private final String prompt;

    public MoveDiscardCardToTopDeckCommand(Target target, int maxSelections, String prompt) {
        this.target = target;
        this.maxSelections = Math.max(1, maxSelections);
        this.prompt = prompt;
    }

    @Override
    public void execute(Partida partida, TableroJugador atacante, TableroJugador defensor) {
        TableroJugador sourceBoard = target == Target.SELF ? atacante : defensor;
        List<Card> discard = sourceBoard.getPilaDescarte();
        if (discard == null || discard.isEmpty()) {
            return;
        }

        if (sourceBoard == partida.getBot()) {
            Card selected = discard.remove(discard.size() - 1);
            sourceBoard.getMazo().add(0, selected);
            partida.getTurnLogs().add("DISCARD_TO_TOP_DECK:BOT:" + selected.getNombre());
            return;
        }

        PendingBattleAction action = new PendingBattleAction();
        action.setActor(partida.getJugadorUsername());
        action.setType("DISCARD_TO_TOP_DECK");
        action.setPrompt(prompt);
        action.setDestination("DECK_TOP");
        action.setMinSelections(1);
        action.setMaxSelections(Math.min(maxSelections, discard.size()));
        action.setOptions(discard.stream()
                .map(card -> new PendingBattleAction.Option(card.getId(), card.getNombre(), card.getImagen()))
                .toList());
        partida.setPendingAction(action);
        partida.transicionarA(new com.pokemon.tcg.model.battle.state.EstadoEsperandoInteraccion());
    }
}
