package com.pokemon.tcg.model.battle.command;

import com.pokemon.tcg.model.battle.CartaEnJuego;
import com.pokemon.tcg.model.battle.Partida;
import com.pokemon.tcg.model.battle.PendingBattleAction;
import com.pokemon.tcg.model.battle.TableroJugador;
import com.pokemon.tcg.model.battle.state.EstadoEsperandoInteraccion;

import java.util.ArrayList;
import java.util.List;

public class SelectOwnPokemonToHealCommand implements BattleCommand {
    private final int amount;
    private final boolean benchedOnly;

    public SelectOwnPokemonToHealCommand(int amount) {
        this(amount, false);
    }

    public SelectOwnPokemonToHealCommand(int amount, boolean benchedOnly) {
        this.amount = amount;
        this.benchedOnly = benchedOnly;
    }

    @Override
    public void execute(Partida partida, TableroJugador atacante, TableroJugador defensor) {
        List<CartaEnJuego> targetOptions = new ArrayList<>();
        if (!benchedOnly && atacante.getActivo() != null) {
            targetOptions.add(atacante.getActivo());
        }
        targetOptions.addAll(atacante.getBanca());

        if (targetOptions.isEmpty()) {
            return;
        }

        if (atacante == partida.getBot()) {
            // Bot automatically picks the Pokemon with the most damage
            CartaEnJuego bestTarget = null;
            int maxDamage = -1;
            for (CartaEnJuego c : targetOptions) {
                int maxHp = Integer.parseInt(c.getCard().getHp());
                int damage = maxHp - c.getHpActual();
                if (damage > maxDamage) {
                    maxDamage = damage;
                    bestTarget = c;
                }
            }

            if (bestTarget != null) {
                int maxHp = Integer.parseInt(bestTarget.getCard().getHp());
                bestTarget.setHpActual(Math.min(maxHp, bestTarget.getHpActual() + amount));
                partida.getTurnLogs().add("HEALED:BOT:" + bestTarget.getCard().getId() + ":" + amount);
            }
            return;
        }

        PendingBattleAction action = new PendingBattleAction();
        action.setActor(partida.getJugadorUsername());
        action.setType("HEAL_OWN_POKEMON");
        action.setPrompt("Elegí 1 de tus Pokémon " + (benchedOnly ? "en Banca " : "") + "para curarle " + amount + " de daño.");
        action.setMinSelections(1);
        action.setMaxSelections(1);
        action.setAmount(amount);
        action.setOptions(targetOptions.stream()
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
        partida.transicionarA(new EstadoEsperandoInteraccion());
    }
}
