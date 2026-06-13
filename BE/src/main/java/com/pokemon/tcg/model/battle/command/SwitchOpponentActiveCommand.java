package com.pokemon.tcg.model.battle.command;

import com.pokemon.tcg.model.battle.Partida;
import com.pokemon.tcg.model.battle.TableroJugador;
import com.pokemon.tcg.model.battle.PendingBattleAction;
import com.pokemon.tcg.model.battle.CartaEnJuego;

import java.util.List;
import java.util.stream.Collectors;

public class SwitchOpponentActiveCommand implements BattleCommand {

    @Override
    public void execute(Partida partida, TableroJugador atacante, TableroJugador defensor) {
        if (defensor.getBanca().isEmpty()) {
            return;
        }

        if (atacante == partida.getBot()) {
            // Bot just picks the first one randomly or 0
            CartaEnJuego suplente = defensor.getBanca().remove(0);
            CartaEnJuego oldActive = defensor.getActivo();
            if (oldActive != null) {
                oldActive.limpiarCondiciones();
                defensor.getBanca().add(oldActive);
            }
            defensor.setActivo(suplente);
            partida.getTurnLogs().add("OPPONENT_FORCED_SWITCH:BOT:" + suplente.getCard().getNombre());
            return;
        }

        PendingBattleAction action = new PendingBattleAction();
        action.setActor(partida.getJugadorUsername());
        action.setType("CHOOSE_OPPONENT_BENCH_TO_ACTIVE");
        action.setPrompt("Elegí un Pokémon de la banca de tu rival para cambiarlo por su Pokémon Activo.");
        action.setDestination("SWITCH_OPPONENT_ACTIVE");
        action.setMinSelections(1);
        action.setMaxSelections(1);
        action.setOptions(defensor.getBanca().stream()
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
                .collect(Collectors.toList()));
        
        partida.setPendingAction(action);
        partida.transicionarA(new com.pokemon.tcg.model.battle.state.EstadoEsperandoInteraccion());
    }
}
