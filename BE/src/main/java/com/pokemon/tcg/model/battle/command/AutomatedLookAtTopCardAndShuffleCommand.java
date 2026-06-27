package com.pokemon.tcg.model.battle.command;

import com.pokemon.tcg.model.Card;
import com.pokemon.tcg.model.battle.Partida;
import com.pokemon.tcg.model.battle.TableroJugador;

import java.util.Collections;

public class AutomatedLookAtTopCardAndShuffleCommand implements BattleCommand {

    @Override
    public void execute(Partida partida, TableroJugador atacante, TableroJugador defensor) {
        if (defensor.getMazo() != null && !defensor.getMazo().isEmpty()) {
            Card topCard = defensor.getMazo().get(0);
            
            // Simple AI to decide whether to shuffle:
            // If it's an Energy, a Supporter/Item, or a Stage 1/2 Pokemon, shuffle to deny them the good card.
            // If it's a Basic Pokemon, don't shuffle (let them draw it).
            boolean isGoodCard = false;
            String supertype = topCard.getSupertype();
            String subtype = topCard.getSubtypes() != null && !topCard.getSubtypes().isEmpty() ? topCard.getSubtypes().get(0) : "";
            
            if ("Energy".equalsIgnoreCase(supertype) || "Trainer".equalsIgnoreCase(supertype)) {
                isGoodCard = true;
            } else if ("Pokémon".equalsIgnoreCase(supertype) && ("Stage 1".equalsIgnoreCase(subtype) || "Stage 2".equalsIgnoreCase(subtype))) {
                isGoodCard = true;
            }

            if (isGoodCard) {
                Collections.shuffle(defensor.getMazo());
                System.out.println("🤖 [AI] Diglett forzó la mezcla del mazo rival porque la carta en el tope era buena (" + topCard.getNombre() + ")");
            } else {
                System.out.println("🤖 [AI] Diglett NO forzó la mezcla del mazo rival porque la carta en el tope no era peligrosa (" + topCard.getNombre() + ")");
            }
        }
    }
}
