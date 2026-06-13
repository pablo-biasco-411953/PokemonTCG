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
        
        if (atacante == partida.getBot()) {
            List<CartaEnJuego> bancaCopy = new ArrayList<>(defensor.getBanca());
            Collections.shuffle(bancaCopy);
            
            int targets = Math.min(count, bancaCopy.size());
            for (int i = 0; i < targets; i++) {
                CartaEnJuego targetPokemon = bancaCopy.get(i);
                targetPokemon.setHpActual(Math.max(0, targetPokemon.getHpActual() - amount));
                System.out.println("☄️ Daño oponente en banca: " + targetPokemon.getCard().getNombre() + " recibe " + amount + " de daño.");
                
                partida.getTurnLogs().add("BENCH_DAMAGE:JUGADOR:" + targetPokemon.getCard().getId() + ":" + targetPokemon.getCard().getNombre().replace(':', '-') + ":" + amount);
            }
        } else {
            com.pokemon.tcg.model.battle.PendingBattleAction action = new com.pokemon.tcg.model.battle.PendingBattleAction();
            action.setActor(partida.getJugadorUsername());
            action.setType("CHOOSE_OPPONENT_BENCH_TO_DAMAGE");
            action.setPrompt("Elegí " + count + " Pokémon de la banca de tu rival para hacerle " + amount + " de daño.");
            action.setDestination("DAMAGE_OPPONENT_BENCH");
            action.setMinSelections(count);
            action.setMaxSelections(count);
            action.setAmount(amount);
            action.setOptions(defensor.getBanca().stream()
                    .map(carta -> {
                        String id = carta.getCard().getId();
                        String set = id.contains("-") ? id.split("-")[0] : "base1";
                        String numero = id.contains("-") ? id.split("-")[1] : "1";
                        return new com.pokemon.tcg.model.battle.PendingBattleAction.Option(
                            id,
                            carta.getCard().getNombre(),
                            carta.getCard().getImagen(),
                            carta.getHpActual(),
                            Integer.parseInt(carta.getCard().getHp()),
                            numero,
                            set
                        );
                    })
                    .toList());
            
            partida.setPendingAction(action);
            partida.transicionarA(new com.pokemon.tcg.model.battle.state.EstadoEsperandoInteraccion());
        }
    }
}
