package com.pokemon.tcg.model.battle.command;

import com.pokemon.tcg.model.Card;
import com.pokemon.tcg.model.battle.Partida;
import com.pokemon.tcg.model.battle.TableroJugador;

public class DiscardOpponentDeckPerDamageCounterCommand implements BattleCommand {
    private final int requiredHeads;

    public DiscardOpponentDeckPerDamageCounterCommand(int requiredHeads) {
        this.requiredHeads = requiredHeads;
    }

    @Override
    public void execute(Partida partida, TableroJugador atacante, TableroJugador defensor) {
        int heads = 0;
        for (Boolean b : partida.getUltimasMonedasLanzadas()) {
            if (b) heads++;
        }

        if (heads >= requiredHeads && atacante.getActivo() != null) {
            int maxHp = 0;
            try {
                maxHp = Integer.parseInt(atacante.getActivo().getCard().getHp());
            } catch (NumberFormatException e) {
                maxHp = 0;
            }
            
            int missingHp = maxHp - atacante.getActivo().getHpActual();
            int damageCounters = Math.max(0, missingHp / 10);

            for (int i = 0; i < damageCounters; i++) {
                if (defensor.getMazo() != null && !defensor.getMazo().isEmpty()) {
                    Card discarded = defensor.getMazo().remove(0);
                    defensor.getPilaDescarte().add(discarded);
                }
            }
        }
    }
}
