package com.pokemon.tcg.model.battle.command;

import com.pokemon.tcg.model.Card;
import com.pokemon.tcg.model.battle.CartaEnJuego;
import com.pokemon.tcg.model.battle.Partida;
import com.pokemon.tcg.model.battle.PendingBattleAction;
import com.pokemon.tcg.model.battle.TableroJugador;
import com.pokemon.tcg.model.battle.state.EstadoEsperandoInteraccion;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GeomancyCommand implements BattleCommand {

    @Override
    public void execute(Partida partida, TableroJugador atacante, TableroJugador defensor) {
        if (atacante.getBanca().isEmpty()) {
            return;
        }

        int maxSelections = Math.min(2, atacante.getBanca().size());

        if (atacante == partida.getBot()) {
            List<Card> fairyEnergies = atacante.getMazo().stream()
                    .filter(c -> "Energy".equalsIgnoreCase(c.getSupertype()) 
                            && "Fairy".equalsIgnoreCase(c.getTipo()))
                    .toList();

            int attachedCount = 0;
            for (int i = 0; i < maxSelections; i++) {
                if (i < fairyEnergies.size()) {
                    Card energy = fairyEnergies.get(i);
                    CartaEnJuego targetPokemon = atacante.getBanca().get(i);
                    atacante.getMazo().remove(energy);
                    targetPokemon.getEnergiasUnidas().add(energy);
                    attachedCount++;
                    partida.getTurnLogs().add("ENERGY_ATTACHED:BOT:" + targetPokemon.getCard().getNombre().replace(':', '-'));
                }
            }
            if (attachedCount > 0) {
                Collections.shuffle(atacante.getMazo());
                partida.getTurnLogs().add("DECK_SEARCHED:BOT");
            }
            return;
        }

        PendingBattleAction action = new PendingBattleAction();
        action.setActor(partida.getJugadorUsername());
        action.setType("SELECT_BENCHED_POKEMON_FOR_GEOMANCY");
        action.setPrompt("Geomancy: Seleccioná hasta 2 Pokémon de tu Banca");
        action.setMinSelections(1);
        action.setMaxSelections(maxSelections);
        action.setEndsTurn(true);
        action.setOptions(atacante.getBanca().stream()
                .map(carta -> {
                    String id = carta.getCard().getId();
                    String set = id.contains("-") ? id.split("-")[0] : "base1";
                    String numero = id.contains("-") ? id.split("-")[1] : "1";
                    return new PendingBattleAction.Option(
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
