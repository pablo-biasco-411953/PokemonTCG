package com.pokemon.tcg.model.battle.command;

import com.pokemon.tcg.model.battle.Partida;
import com.pokemon.tcg.model.battle.TableroJugador;
import com.pokemon.tcg.model.battle.CartaEnJuego;

public class GogoatChargeDashCommand implements BattleCommand {
    private final String choice;

    public GogoatChargeDashCommand(String choice) {
        this.choice = choice;
    }

    @Override
    public void execute(Partida partida, TableroJugador atacante, TableroJugador defensor) {
        CartaEnJuego activeAtacante = atacante.getActivo();
        if (activeAtacante == null) return;

        boolean chooseYes = false;
        if (atacante == partida.getBot()) {
            // Bot AI choice: yes if it won't KO itself
            chooseYes = activeAtacante.getHpActual() > 20;
        } else {
            chooseYes = "yes".equalsIgnoreCase(choice);
        }

        if (chooseYes) {
            partida.getExecutionQueue().addFirst(new DamageCommand(20));
            partida.getExecutionQueue().addFirst(new SelfDamageCommand(20));
            String actor = (atacante == partida.getJugador()) ? "JUGADOR" : "BOT";
            partida.getTurnLogs().add("CHARGE_DASH_BOOST:" + actor);
            System.out.println("🐏 Gogoat Charge Dash choice: YES. Added 20 damage and 20 self damage.");
        }
    }
}
