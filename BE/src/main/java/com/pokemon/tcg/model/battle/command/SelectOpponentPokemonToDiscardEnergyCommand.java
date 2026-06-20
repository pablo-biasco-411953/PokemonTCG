package com.pokemon.tcg.model.battle.command;

import com.pokemon.tcg.model.battle.CartaEnJuego;
import com.pokemon.tcg.model.battle.Partida;
import com.pokemon.tcg.model.battle.PendingBattleAction;
import com.pokemon.tcg.model.battle.TableroJugador;
import com.pokemon.tcg.model.battle.state.EstadoEsperandoInteraccion;

import java.util.ArrayList;
import java.util.List;

public class SelectOpponentPokemonToDiscardEnergyCommand implements BattleCommand {

    @Override
    public void execute(Partida partida, TableroJugador atacante, TableroJugador defensor) {
        List<CartaEnJuego> targetOptions = new ArrayList<>();
        if (defensor.getActivo() != null && !defensor.getActivo().getEnergiasUnidas().isEmpty()) {
            targetOptions.add(defensor.getActivo());
        }
        for (CartaEnJuego b : defensor.getBanca()) {
            if (!b.getEnergiasUnidas().isEmpty()) {
                targetOptions.add(b);
            }
        }

        if (targetOptions.isEmpty()) {
            return;
        }

        if (atacante == partida.getBot()) {
            // Bot automatically picks one target: prefer active, else first benched
            CartaEnJuego target = null;
            if (defensor.getActivo() != null && !defensor.getActivo().getEnergiasUnidas().isEmpty()) {
                target = defensor.getActivo();
            } else {
                target = targetOptions.get(0);
            }

            if (!target.getEnergiasUnidas().isEmpty()) {
                com.pokemon.tcg.model.Card energy = target.getEnergiasUnidas().remove(0);
                defensor.getPilaDescarte().add(energy);
                partida.getTurnLogs().add("ENERGY_DISCARDED:BOT:" + energy.getNombre());
            }
            return;
        }

        PendingBattleAction action = new PendingBattleAction();
        action.setActor(partida.getJugadorUsername());
        action.setType("DISCARD_OPPONENT_ENERGY");
        action.setPrompt("Elegí 1 de los Pokémon de tu rival para descartarle una Energía.");
        action.setMinSelections(1);
        action.setMaxSelections(1);
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
