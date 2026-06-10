package com.pokemon.tcg.model.battle.command;

import com.pokemon.tcg.model.Card;
import com.pokemon.tcg.model.battle.CartaEnJuego;
import com.pokemon.tcg.model.battle.Partida;
import com.pokemon.tcg.model.battle.TableroJugador;
import java.util.Random;

public class RhydonMadMountainCommand implements BattleCommand {
    private static final Random RANDOM = new Random();

    @Override
    public void execute(Partida partida, TableroJugador atacante, TableroJugador defensor) {
        boolean flip1 = RANDOM.nextBoolean();
        boolean flip2 = RANDOM.nextBoolean();

        partida.getUltimasMonedasLanzadas().add(flip1);
        partida.getUltimasMonedasLanzadas().add(flip2);

        String actor = (atacante == partida.getJugador()) ? "JUGADOR" : "BOT";
        String flip1Str = flip1 ? "CARA" : "CRUZ";
        String flip2Str = flip2 ? "CARA" : "CRUZ";

        int discardedCount = 0;

        if (flip1 && flip2) {
            CartaEnJuego activeAttacker = atacante.getActivo();
            if (activeAttacker != null) {
                int maxHp;
                try {
                    maxHp = Integer.parseInt(activeAttacker.getCard().getHp());
                } catch (NumberFormatException e) {
                    maxHp = activeAttacker.getHpActual();
                }

                int counters = Math.max(0, maxHp - activeAttacker.getHpActual()) / 10;
                String targetActor = (defensor == partida.getJugador()) ? "JUGADOR" : "BOT";

                for (int i = 0; i < counters; i++) {
                    if (defensor.getMazo() == null || defensor.getMazo().isEmpty()) {
                        break;
                    }
                    Card discarded = defensor.getMazo().remove(0);
                    defensor.getPilaDescarte().add(discarded);
                    discardedCount++;
                    partida.getTurnLogs().add("DISCARD_TOP_DECK:" + targetActor + ":" + limpiar(discarded.getNombre()) + ":" + discarded.getId());
                }
            }
        }

        partida.getTurnLogs().add("MAD_MOUNTAIN_DISCARDED:" + actor + ":" + flip1Str + ":" + flip2Str + ":" + discardedCount);
    }

    private String limpiar(String value) {
        return value == null ? "" : value.replace(':', '-').replace('\n', ' ').replace('\r', ' ').trim();
    }
}
