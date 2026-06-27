package com.pokemon.tcg.model.battle.command;

import com.pokemon.tcg.model.battle.CartaEnJuego;
import com.pokemon.tcg.model.battle.Partida;
import com.pokemon.tcg.model.battle.TableroJugador;

public class AddDamageIfPokemonOnBenchCommand implements BattleCommand {
    private final String pokemonName;
    private final int extraDamage;

    public AddDamageIfPokemonOnBenchCommand(String pokemonName, int extraDamage) {
        this.pokemonName = pokemonName;
        this.extraDamage = extraDamage;
    }

    @Override
    public void execute(Partida partida, TableroJugador atacante, TableroJugador defensor) {
        boolean isOnBench = false;
        for (CartaEnJuego benched : atacante.getBanca()) {
            if (benched != null && benched.getCard() != null && benched.getCard().getNombre().toLowerCase().contains(pokemonName.toLowerCase())) {
                isOnBench = true;
                break;
            }
        }

        if (isOnBench) {
            partida.getExecutionQueue().addFirst(new DamageCommand(extraDamage));
        }
    }
}
