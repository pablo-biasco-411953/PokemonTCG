package com.pokemon.tcg.model.battle.command;

import com.pokemon.tcg.model.battle.Partida;
import com.pokemon.tcg.model.battle.TableroJugador;
import com.pokemon.tcg.model.Card;

import java.util.Collections;
import java.util.Iterator;

public class SearchDeckCommand implements BattleCommand {
    private String criteriaSupertype;
    private String criteriaSubtype;
    private Target destination; // HAND or BENCH

    public SearchDeckCommand(String criteriaSupertype, String criteriaSubtype, Target destination) {
        this.criteriaSupertype = criteriaSupertype;
        this.criteriaSubtype = criteriaSubtype;
        this.destination = destination;
    }

    @Override
    public void execute(Partida partida, TableroJugador atacante, TableroJugador defensor) {
        String actor = (atacante == partida.getJugador()) ? "JUGADOR" : "BOT";
        
        // Find first card matching criteria
        Card found = null;
        Iterator<Card> it = atacante.getMazo().iterator();
        while (it.hasNext()) {
            Card c = it.next();
            boolean superMatch = (criteriaSupertype == null || criteriaSupertype.equals(c.getSupertype()));
            boolean subMatch = (criteriaSubtype == null || (c.getSubtypes() != null && c.getSubtypes().contains(criteriaSubtype)));
            
            if (superMatch && subMatch) {
                found = c;
                it.remove();
                break;
            }
        }

        if (found != null) {
            if (destination == Target.SELF) { // Means Hand
                atacante.getMano().add(found);
            } else if (destination == Target.OPPONENT) { // Means Bench for now (hack)
                // Actually we should create a custom enum, but for now let's just add to bench if possible
                if (atacante.getBanca().size() < 5) {
                    com.pokemon.tcg.model.battle.CartaEnJuego benched = new com.pokemon.tcg.model.battle.CartaEnJuego(found);
                    atacante.getBanca().add(benched);
                } else {
                    atacante.getMano().add(found); // fallback to hand
                }
            }
            partida.getTurnLogs().add("DECK_SEARCHED:" + actor);
        }

        // Always shuffle
        Collections.shuffle(atacante.getMazo());
    }
}
