package com.pokemon.tcg.model.battle.command;

import com.pokemon.tcg.model.battle.Partida;
import com.pokemon.tcg.model.battle.TableroJugador;

public class AddDamageIfStatusConditionAndRemoveCommand implements BattleCommand {
    private final int extraDamage;

    public AddDamageIfStatusConditionAndRemoveCommand(int extraDamage) {
        this.extraDamage = extraDamage;
    }

    @Override
    public void execute(Partida partida, TableroJugador atacante, TableroJugador defensor) {
        if (defensor.getActivo() != null) {
            if (!defensor.getActivo().getCondicionesEspeciales().isEmpty()) {
                partida.getExecutionQueue().addFirst(new DamageCommand(extraDamage));
                defensor.getActivo().getCondicionesEspeciales().clear();
            }
        }
    }
}
