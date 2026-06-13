package com.pokemon.tcg.model.battle.command;

import com.pokemon.tcg.model.Card;
import com.pokemon.tcg.model.battle.Partida;
import com.pokemon.tcg.model.battle.TableroJugador;

import java.util.Random;

public class DiscardRandomHandCardsByCoinTailsCommand implements BattleCommand {
    private static final Random RANDOM = new Random();

    private final int flips;
    private final Target target;

    public DiscardRandomHandCardsByCoinTailsCommand(int flips, Target target) {
        this.flips = Math.max(1, flips);
        this.target = target;
    }

    @Override
    public void execute(Partida partida, TableroJugador atacante, TableroJugador defensor) {
        TableroJugador targetBoard = target == Target.SELF ? atacante : defensor;
        String actor = targetBoard == partida.getJugador() ? "JUGADOR" : "BOT";

        for (int i = 0; i < flips; i++) {
            boolean isHeads = RANDOM.nextBoolean();
            partida.getUltimasMonedasLanzadas().add(isHeads);
            if (isHeads || targetBoard.getMano().isEmpty()) continue;

            int index = RANDOM.nextInt(targetBoard.getMano().size());
            Card discarded = targetBoard.getMano().remove(index);
            targetBoard.getPilaDescarte().add(discarded);
            partida.getTurnLogs().add("RANDOM_HAND_DISCARDED:" + actor + ":" + clean(discarded.getNombre()) + ":" + discarded.getId());
        }
    }

    private String clean(String value) {
        return value == null ? "" : value.replace(':', '-').replace('\n', ' ').replace('\r', ' ').trim();
    }
}
