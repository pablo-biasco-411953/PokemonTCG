package com.pokemon.tcg.model.battle.command;

import com.pokemon.tcg.model.battle.Partida;
import com.pokemon.tcg.model.battle.TableroJugador;
import com.pokemon.tcg.model.Card;

import java.util.Iterator;
import java.util.List;

public class DiscardEnergyCommand implements BattleCommand {
    private int amount;
    private Target target;

    public DiscardEnergyCommand(int amount, Target target) {
        this.amount = amount;
        this.target = target;
    }

    @Override
    public void execute(Partida partida, TableroJugador atacante, TableroJugador defensor) {
        TableroJugador targetPlayer = (target == Target.SELF) ? atacante : defensor;
        String actor = (atacante == partida.getJugador()) ? "JUGADOR" : "BOT";
        
        if (targetPlayer.getActivo() != null) {
            List<Card> energias = targetPlayer.getActivo().getEnergiasUnidas();
            int discarded = 0;
            Iterator<Card> iterator = energias.iterator();
            while (iterator.hasNext() && discarded < amount) {
                Card energia = iterator.next();
                iterator.remove();
                targetPlayer.getPilaDescarte().add(energia);
                discarded++;
                
                partida.getTurnLogs().add("ENERGY_DISCARDED:" + actor);
            }
        }
    }
}
