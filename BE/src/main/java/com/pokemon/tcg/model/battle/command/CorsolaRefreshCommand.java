package com.pokemon.tcg.model.battle.command;

import com.pokemon.tcg.model.battle.CartaEnJuego;
import com.pokemon.tcg.model.battle.Partida;
import com.pokemon.tcg.model.battle.TableroJugador;

public class CorsolaRefreshCommand implements BattleCommand {

    @Override
    public void execute(Partida partida, TableroJugador atacante, TableroJugador defensor) {
        CartaEnJuego active = atacante.getActivo();
        if (active == null) return;

        // Heal 30 damage
        int currentHp = active.getHpActual();
        int maxHp = Integer.parseInt(active.getCard().getHp());
        active.setHpActual(Math.min(maxHp, currentHp + 30));

        // Remove all Special Conditions
        active.getCondicionesEspeciales().clear();
        
        String actor = (atacante == partida.getJugador()) ? "JUGADOR" : "BOT";
        partida.getTurnLogs().add("HEALED:" + actor + ":" + active.getCard().getNombre() + ":30");
        partida.getTurnLogs().add("STATUS_CLEARED:" + actor + ":" + active.getCard().getNombre());
        System.out.println("🌸 Corsola Refresh healed 30 and removed special conditions.");
    }
}
