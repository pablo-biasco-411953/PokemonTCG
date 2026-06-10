package com.pokemon.tcg.model.battle.command;

import com.pokemon.tcg.model.battle.Partida;
import com.pokemon.tcg.model.battle.TableroJugador;

import java.util.List;

public class SequenceCommand implements BattleCommand {
    private final List<BattleCommand> commands;

    public SequenceCommand(BattleCommand... commands) {
        this.commands = List.of(commands);
    }

    @Override
    public void execute(Partida partida, TableroJugador atacante, TableroJugador defensor) {
        for (int i = commands.size() - 1; i >= 0; i--) {
            partida.getExecutionQueue().addFirst(commands.get(i));
        }
    }
}
