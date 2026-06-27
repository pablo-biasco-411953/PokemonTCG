package com.pokemon.tcg.service.battle.command;

import com.pokemon.tcg.model.Card;
import com.pokemon.tcg.model.battle.Partida;
import com.pokemon.tcg.model.battle.TableroJugador;

public class ComandoJugarTrainer implements ComandoTurno {

    private final Card card;
    private final TableroJugador tablero;
    private final java.util.function.Consumer<Partida> effectRunner;

    public ComandoJugarTrainer(Card card, TableroJugador tablero, java.util.function.Consumer<Partida> effectRunner) {
        this.card = card;
        this.tablero = tablero;
        this.effectRunner = effectRunner;
    }

    @Override
    public boolean puedeEjecutar(Partida partida) {
        if (!tablero.getMano().contains(card)) return false;
        
        boolean isSupporter = card.getSubtypes() != null && card.getSubtypes().stream().anyMatch(s -> s.equalsIgnoreCase("Supporter"));
        boolean isStadium = card.getSubtypes() != null && card.getSubtypes().stream().anyMatch(s -> s.equalsIgnoreCase("Stadium"));
        
        if (isSupporter && partida.isPlayedSupporterThisTurn()) {
            return false;
        }
        if (isStadium && partida.isPlayedStadiumThisTurn()) {
            return false;
        }

        // Apply Trevenant's Forest's Curse Ability
        boolean isItem = !isSupporter && !isStadium;
        if (isItem) {
            TableroJugador oponente = (partida.getJugador() == tablero) ? partida.getBot() : partida.getJugador();
            boolean hasForestsCurse = oponente.getActivo() != null 
                    && oponente.getActivo().getCard().getHabilidades() != null
                    && oponente.getActivo().getCard().getHabilidades().stream()
                            .anyMatch(h -> "Forest's Curse".equalsIgnoreCase(h.getNombre()));
            if (hasForestsCurse) {
                return false;
            }
        }

        return true;
    }

    @Override
    public void ejecutar(Partida partida) {
        if (!puedeEjecutar(partida)) {
            throw new IllegalStateException("No podés jugar esta carta de Entrenador en este momento.");
        }

        boolean isSupporter = card.getSubtypes() != null && card.getSubtypes().stream().anyMatch(s -> s.equalsIgnoreCase("Supporter"));
        boolean isStadium = card.getSubtypes() != null && card.getSubtypes().stream().anyMatch(s -> s.equalsIgnoreCase("Stadium"));
        boolean isTool = card.getSubtypes() != null && card.getSubtypes().stream().anyMatch(s -> s.equalsIgnoreCase("Pokémon Tool") || s.equalsIgnoreCase("Pokemon Tool"));

        // Enforce turn limits
        if (isSupporter) {
            partida.setPlayedSupporterThisTurn(true);
        }
        if (isStadium) {
            partida.setPlayedStadiumThisTurn(true);
        }

        // Handle cards movement
        if (isStadium) {
            tablero.getMano().remove(card);
            // If there is an active stadium, discard it
            if (partida.getActiveStadium() != null) {
                // Determine whose discard pile the old stadium goes to (usually the owner's discard pile, but since we don't track stadium ownership, we can discard to player's discard pile for simplicity)
                tablero.getPilaDescarte().add(partida.getActiveStadium());
            }
            partida.setActiveStadium(card);
        } else if (isTool) {
            // Pokémon tools are removed from hand and attached to a Pokémon.
            // Do not discard now. The effect runner will remove it and attach it.
        } else {
            // Normal Items and Supporters go to the discard pile
            tablero.getMano().remove(card);
            tablero.getPilaDescarte().add(card);
        }

        // Execute the card-specific effect
        effectRunner.accept(partida);
    }

    @Override
    public String getNombre() {
        return "JugarTrainer[" + card.getNombre() + "]";
    }
}
