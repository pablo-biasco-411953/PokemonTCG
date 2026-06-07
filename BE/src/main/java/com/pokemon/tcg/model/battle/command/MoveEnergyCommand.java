package com.pokemon.tcg.model.battle.command;

import com.pokemon.tcg.model.battle.Partida;
import com.pokemon.tcg.model.battle.TableroJugador;
import com.pokemon.tcg.model.Card;

import java.util.Iterator;

public class MoveEnergyCommand implements BattleCommand {
    private String energyType;
    private int amount;

    public MoveEnergyCommand(String energyType, int amount) {
        this.energyType = energyType;
        this.amount = amount;
    }

    @Override
    public void execute(Partida partida, TableroJugador atacante, TableroJugador defensor) {
        String actor = (atacante == partida.getJugador()) ? "JUGADOR" : "BOT";
        
        // Simple logic: move from active to first benched pokemon
        if (atacante.getActivo() != null && !atacante.getBanca().isEmpty()) {
            int moved = 0;
            Iterator<Card> it = atacante.getActivo().getEnergiasUnidas().iterator();
            while (it.hasNext() && moved < amount) {
                Card energia = it.next();
                if (energyType == null || energyType.equals(energia.getTipo())) {
                    it.remove();
                    atacante.getBanca().get(0).getEnergiasUnidas().add(energia);
                    moved++;
                    partida.getTurnLogs().add("ENERGY_MOVED:" + actor);
                }
            }
        }
    }
}
