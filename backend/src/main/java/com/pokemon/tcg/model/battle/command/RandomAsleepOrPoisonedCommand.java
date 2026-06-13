package com.pokemon.tcg.model.battle.command;

import com.pokemon.tcg.model.battle.Partida;
import com.pokemon.tcg.model.battle.TableroJugador;
import java.util.Random;

public class RandomAsleepOrPoisonedCommand implements BattleCommand {
    
    private final Target target;

    public RandomAsleepOrPoisonedCommand(Target target) {
        this.target = target;
    }

    @Override
    public void execute(Partida partida, TableroJugador atacante, TableroJugador defensor) {
        TableroJugador targetTablero = target == Target.SELF ? atacante : defensor;
        if (targetTablero.getActivo() != null) {
            boolean isAsleep = new Random().nextBoolean();
            String status = isAsleep ? "Asleep" : "Poisoned";
            targetTablero.getActivo().getCondicionesEspeciales().add(status);
            System.out.println("✨ Efecto aleatorio aplicado: " + targetTablero.getActivo().getCard().getNombre() + " ahora está " + status);
        }
    }
}
