package com.pokemon.tcg.model.battle.command;

import com.pokemon.tcg.model.battle.CartaEnJuego;
import com.pokemon.tcg.model.battle.Partida;
import com.pokemon.tcg.model.battle.TableroJugador;

public class AddDamageByDamageCountersCommand implements BattleCommand {
    private final Target target;
    private final int damagePerCounter;

    public AddDamageByDamageCountersCommand(Target target, int damagePerCounter) {
        this.target = target;
        this.damagePerCounter = damagePerCounter;
    }

    @Override
    public void execute(Partida partida, TableroJugador atacante, TableroJugador defensor) {
        CartaEnJuego pokemon = target == Target.SELF ? atacante.getActivo() : defensor.getActivo();
        int counters = countDamageCounters(pokemon);
        if (counters > 0) {
            partida.getExecutionQueue().addFirst(new DamageCommand(counters * damagePerCounter));
        }
    }

    private int countDamageCounters(CartaEnJuego pokemon) {
        if (pokemon == null || pokemon.getCard() == null) return 0;
        int maxHp;
        try {
            maxHp = Integer.parseInt(pokemon.getCard().getHp());
        } catch (NumberFormatException ignored) {
            maxHp = pokemon.getHpActual();
        }
        return Math.max(0, maxHp - pokemon.getHpActual()) / 10;
    }
}
