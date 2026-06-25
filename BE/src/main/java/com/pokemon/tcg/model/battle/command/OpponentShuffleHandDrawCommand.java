package com.pokemon.tcg.model.battle.command;

import com.pokemon.tcg.model.Card;
import com.pokemon.tcg.model.battle.Partida;
import com.pokemon.tcg.model.battle.TableroJugador;
import java.util.Collections;

public class OpponentShuffleHandDrawCommand implements BattleCommand {
    private final int drawCount;

    public OpponentShuffleHandDrawCommand(int drawCount) {
        this.drawCount = drawCount;
    }

    public int getDrawCount() {
        return drawCount;
    }

    @Override
    public void execute(Partida partida, TableroJugador atacante, TableroJugador defensor) {
        if (!defensor.getMano().isEmpty()) {
            defensor.getMazo().addAll(defensor.getMano());
            defensor.getMano().clear();
        }
        Collections.shuffle(defensor.getMazo());
        for (int i = 0; i < drawCount; i++) {
            if (!defensor.getMazo().isEmpty()) {
                defensor.getMano().add(defensor.getMazo().remove(0));
            }
        }
        String actor = (defensor == partida.getJugador()) ? "JUGADOR" : "BOT";
        partida.getTurnLogs().add("SHUFFLED_HAND_INTO_DECK:" + actor + ":" + drawCount);
    }
}
