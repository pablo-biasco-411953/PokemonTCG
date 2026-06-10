package com.pokemon.tcg.model.battle.command;

import com.pokemon.tcg.model.Card;
import com.pokemon.tcg.model.battle.Partida;
import com.pokemon.tcg.model.battle.TableroJugador;

public class DiscardTopDeckCommand implements BattleCommand {
    private final Target target;
    private final int count;

    public DiscardTopDeckCommand(Target target, int count) {
        this.target = target;
        this.count = Math.max(1, count);
    }

    public Target getTarget() {
        return target;
    }

    public int getCount() {
        return count;
    }

    @Override
    public void execute(Partida partida, TableroJugador atacante, TableroJugador defensor) {
        TableroJugador targetBoard = (target == Target.SELF) ? atacante : defensor;
        if (targetBoard.getMazo() == null || targetBoard.getMazo().isEmpty()) {
            return;
        }

        String actor = (targetBoard == partida.getJugador()) ? "JUGADOR" : "BOT";

        for (int i = 0; i < count; i++) {
            if (targetBoard.getMazo().isEmpty()) {
                break;
            }
            Card card = targetBoard.getMazo().remove(0);
            targetBoard.getPilaDescarte().add(card);
            partida.getTurnLogs().add("DISCARD_TOP_DECK:" + actor + ":" + limpiar(card.getNombre()) + ":" + card.getId());
        }
    }

    private String limpiar(String value) {
        return value == null ? "" : value.replace(':', '-').replace('\n', ' ').replace('\r', ' ').trim();
    }
}
