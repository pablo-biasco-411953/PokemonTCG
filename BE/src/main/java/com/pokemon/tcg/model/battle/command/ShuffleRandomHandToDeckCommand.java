package com.pokemon.tcg.model.battle.command;

import com.pokemon.tcg.model.Card;
import com.pokemon.tcg.model.battle.Partida;
import com.pokemon.tcg.model.battle.TableroJugador;
import java.util.Collections;
import java.util.Random;

public class ShuffleRandomHandToDeckCommand implements BattleCommand {
    private final Random random = new Random();

    @Override
    public void execute(Partida partida, TableroJugador atacante, TableroJugador defensor) {
        if (defensor.getMano().isEmpty()) {
            return;
        }
        int randomIndex = random.nextInt(defensor.getMano().size());
        Card card = defensor.getMano().remove(randomIndex);
        defensor.getMazo().add(card);
        Collections.shuffle(defensor.getMazo());

        String targetName = (defensor == partida.getJugador()) ? 
            (partida.getJugadorUsername() != null ? partida.getJugadorUsername() : "JUGADOR") : 
            (partida.getBotUsername() != null ? partida.getBotUsername() : "BOT");

        partida.getTurnLogs().add("ASTONISH_REVEALED:" + targetName + ":" + card.getId() + ":" + card.getNombre().replace(':', '-'));
    }
}
