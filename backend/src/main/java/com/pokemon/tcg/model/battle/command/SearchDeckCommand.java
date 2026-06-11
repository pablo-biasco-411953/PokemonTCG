package com.pokemon.tcg.model.battle.command;

import com.pokemon.tcg.model.Card;
import com.pokemon.tcg.model.battle.PendingBattleAction;
import com.pokemon.tcg.model.battle.Partida;
import com.pokemon.tcg.model.battle.TableroJugador;

import java.util.Collections;
import java.util.List;

public class SearchDeckCommand implements BattleCommand {
    private final String criteriaSupertype;
    private final String criteriaSubtype;
    private final String criteriaType;
    private final String destination;
    private final int maxSelections;
    private final String prompt;

    public SearchDeckCommand(
            String criteriaSupertype,
            String criteriaSubtype,
            String criteriaType,
            String destination,
            int maxSelections,
            String prompt
    ) {
        this.criteriaSupertype = criteriaSupertype;
        this.criteriaSubtype = criteriaSubtype;
        this.criteriaType = criteriaType;
        this.destination = destination;
        this.maxSelections = maxSelections;
        this.prompt = prompt;
    }

    @Override
    public void execute(Partida partida, TableroJugador atacante, TableroJugador defensor) {
        List<Card> legal = atacante.getMazo().stream().filter(this::matches).toList();
        if (legal.isEmpty()) {
            Collections.shuffle(atacante.getMazo());
            return;
        }

        if (atacante == partida.getBot()) {
            applyCards(atacante, legal.subList(0, Math.min(maxSelections, legal.size())));
            Collections.shuffle(atacante.getMazo());
            partida.getTurnLogs().add("DECK_SEARCHED:BOT");
            if ("ATTACH_ACTIVE_AND_SWITCH".equals(destination) && !atacante.getBanca().isEmpty()) {
                com.pokemon.tcg.model.battle.CartaEnJuego botActivo = atacante.getActivo();
                com.pokemon.tcg.model.battle.CartaEnJuego suplente = atacante.getBanca().remove(0);
                if (botActivo != null) {
                    botActivo.limpiarCondiciones();
                    atacante.getBanca().add(botActivo);
                }
                atacante.setActivo(suplente);
                partida.getTurnLogs().add("ACTIVE_SWITCHED:BOT:" + suplente.getCard().getNombre());
            }
            return;
        }

        PendingBattleAction action = new PendingBattleAction();
        action.setActor(partida.getJugadorUsername());
        action.setType("SEARCH_DECK");
        action.setPrompt(prompt);
        action.setDestination(destination);
        action.setMinSelections(0);
        action.setMaxSelections(Math.min(maxSelections, legal.size()));
        action.setOptions(legal.stream()
                .map(card -> new PendingBattleAction.Option(card.getId(), card.getNombre(), card.getImagen()))
                .toList());
        partida.setPendingAction(action);
        partida.transicionarA(new com.pokemon.tcg.model.battle.state.EstadoEsperandoInteraccion());
    }

    private boolean matches(Card card) {
        boolean superMatch = criteriaSupertype == null || criteriaSupertype.equalsIgnoreCase(card.getSupertype());
        boolean subtypeMatch = criteriaSubtype == null || card.getSubtypes() != null
                && card.getSubtypes().stream().anyMatch(criteriaSubtype::equalsIgnoreCase);
        boolean typeMatch = criteriaType == null || criteriaType.equalsIgnoreCase(card.getTipo())
                || card.getNombre() != null && card.getNombre().toLowerCase().contains(criteriaType.toLowerCase());
        return superMatch && subtypeMatch && typeMatch;
    }

    private void applyCards(TableroJugador board, List<Card> cards) {
        for (Card card : cards) {
            if (!board.getMazo().remove(card)) continue;
            if (("ATTACH_ACTIVE".equals(destination) || "ATTACH_ACTIVE_AND_SWITCH".equals(destination)) && board.getActivo() != null) {
                board.getActivo().getEnergiasUnidas().add(card);
            } else {
                board.getMano().add(card);
            }
        }
    }
}
