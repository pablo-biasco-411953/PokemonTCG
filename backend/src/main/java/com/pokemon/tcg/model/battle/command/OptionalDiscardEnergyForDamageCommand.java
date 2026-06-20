package com.pokemon.tcg.model.battle.command;

import com.pokemon.tcg.model.Card;
import com.pokemon.tcg.model.battle.Partida;
import com.pokemon.tcg.model.battle.TableroJugador;

public class OptionalDiscardEnergyForDamageCommand implements BattleCommand {
    private int extraDamage;
    private String choice;

    public OptionalDiscardEnergyForDamageCommand(int extraDamage) {
        this(extraDamage, "yes");
    }

    public OptionalDiscardEnergyForDamageCommand(int extraDamage, String choice) {
        this.extraDamage = extraDamage;
        this.choice = choice;
    }

    @Override
    public void execute(Partida partida, TableroJugador atacante, TableroJugador defensor) {
        boolean isHuman = (atacante == partida.getJugador());
        boolean shouldExecute = !isHuman || "yes".equalsIgnoreCase(choice);

        if (shouldExecute && atacante.getActivo() != null && !atacante.getActivo().getEnergiasUnidas().isEmpty()) {
            // Discard the first energy found
            Card discarded = atacante.getActivo().getEnergiasUnidas().remove(0);
            atacante.getPilaDescarte().add(discarded);
            System.out.println("🔥 Se descartó una energía para hacer " + extraDamage + " de daño extra.");
            partida.getExecutionQueue().addFirst(new DamageCommand(extraDamage));
        }
    }
}
