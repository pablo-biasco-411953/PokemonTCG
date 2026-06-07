package com.pokemon.tcg.model.battle.command;

import com.pokemon.tcg.model.battle.Partida;
import com.pokemon.tcg.model.battle.TableroJugador;
import java.util.Random;

public class CoinFlipCommand implements BattleCommand {
    private BattleCommand onHeads;
    private BattleCommand onTails;
    private static final Random random = new Random();

    public CoinFlipCommand(BattleCommand onHeads) {
        this.onHeads = onHeads;
        this.onTails = null;
    }

    public CoinFlipCommand(BattleCommand onHeads, BattleCommand onTails) {
        this.onHeads = onHeads;
        this.onTails = onTails;
    }

    public BattleCommand getOnHeads() {
        return onHeads;
    }

    public BattleCommand getOnTails() {
        return onTails;
    }

    @Override
    public void execute(Partida partida, TableroJugador atacante, TableroJugador defensor) {
        boolean isHeads = random.nextBoolean();
        partida.getUltimasMonedasLanzadas().add(isHeads); // record it in Partida
        
        if (isHeads) {
            if (onHeads != null) {
                partida.getExecutionQueue().addFirst(onHeads);
            }
        } else {
            if (onTails != null) {
                partida.getExecutionQueue().addFirst(onTails);
            }
        }
    }
}
