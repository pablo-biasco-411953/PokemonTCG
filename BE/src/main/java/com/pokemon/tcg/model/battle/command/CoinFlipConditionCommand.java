package com.pokemon.tcg.model.battle.command;

import com.pokemon.tcg.model.battle.Partida;
import com.pokemon.tcg.model.battle.TableroJugador;

public class CoinFlipConditionCommand implements BattleCommand {
    private final int coinsToFlip;
    private final int requiredHeads;
    private final BattleCommand onSuccess;

    public CoinFlipConditionCommand(int coinsToFlip, int requiredHeads, BattleCommand onSuccess) {
        this.coinsToFlip = coinsToFlip;
        this.requiredHeads = requiredHeads;
        this.onSuccess = onSuccess;
    }

    @Override
    public void execute(Partida partida, TableroJugador atacante, TableroJugador defensor) {
        // En BattleAttackService, las tiradas ya deberían estar en partida.getUltimasMonedasLanzadas() 
        // porque el motor lanza monedas antes, O bien este comando debería generarlas.
        // Pero BattleEngineService lanza monedas si hay un comando que implementa tiradas.
        // Para simplificar, este comando fuerza el resultado del comando hijo
        // pero requiere que las monedas hayan sido tiradas. Como el framework actual 
        // usa MultiCoinDamageCommand o CoinFlipCommand para generar tiradas en BattleAttackService,
        // tenemos que registrar las monedas aquí si no hay sufucientes.
        
        while (partida.getUltimasMonedasLanzadas().size() < coinsToFlip) {
            partida.getUltimasMonedasLanzadas().add(Math.random() >= 0.5);
        }

        int heads = 0;
        for (int i = 0; i < coinsToFlip; i++) {
            if (partida.getUltimasMonedasLanzadas().get(i)) heads++;
        }

        if (heads >= requiredHeads && onSuccess != null) {
            partida.getExecutionQueue().addFirst(onSuccess);
        }
    }
}
