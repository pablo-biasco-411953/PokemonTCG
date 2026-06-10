package com.pokemon.tcg.model.battle.command;

import com.pokemon.tcg.model.battle.CartaEnJuego;
import com.pokemon.tcg.model.battle.Partida;
import com.pokemon.tcg.model.battle.TableroJugador;

public class SelfBenchDamageCommand implements BattleCommand {
    private final int amount;

    public SelfBenchDamageCommand(int amount) {
        this.amount = amount;
    }

    public int getAmount() {
        return amount;
    }

    @Override
    public void execute(Partida partida, TableroJugador atacante, TableroJugador defensor) {
        for (CartaEnJuego pokemon : atacante.getBanca()) {
            pokemon.setHpActual(Math.max(0, pokemon.getHpActual() - amount));
            System.out.println("☄️ Daño propio en banca: " + pokemon.getCard().getNombre() + " recibe " + amount + " de daño.");
        }
    }
}
