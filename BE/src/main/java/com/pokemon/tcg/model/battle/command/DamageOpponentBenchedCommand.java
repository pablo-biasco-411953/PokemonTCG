package com.pokemon.tcg.model.battle.command;

import com.pokemon.tcg.model.battle.CartaEnJuego;
import com.pokemon.tcg.model.battle.Partida;
import com.pokemon.tcg.model.battle.TableroJugador;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DamageOpponentBenchedCommand implements BattleCommand {
    private final int amount;
    private final int count;

    public DamageOpponentBenchedCommand(int amount, int count) {
        this.amount = amount;
        this.count = count;
    }

    public int getAmount() {
        return amount;
    }

    public int getCount() {
        return count;
    }

    @Override
    public void execute(Partida partida, TableroJugador atacante, TableroJugador defensor) {
        if (defensor.getBanca().isEmpty()) return;
        
        List<CartaEnJuego> bancaCopy = new ArrayList<>(defensor.getBanca());
        Collections.shuffle(bancaCopy);
        
        int targets = Math.min(count, bancaCopy.size());
        for (int i = 0; i < targets; i++) {
            CartaEnJuego targetPokemon = bancaCopy.get(i);
            targetPokemon.setHpActual(Math.max(0, targetPokemon.getHpActual() - amount));
            System.out.println("☄️ Daño oponente en banca: " + targetPokemon.getCard().getNombre() + " recibe " + amount + " de daño.");
            
            String defenderOwner = (defensor == partida.getJugador()) ? "JUGADOR" : "BOT";
            partida.getTurnLogs().add("BENCH_DAMAGE:" + defenderOwner + ":" + targetPokemon.getCard().getId() + ":" + targetPokemon.getCard().getNombre().replace(':', '-') + ":" + amount);
        }
    }
}
