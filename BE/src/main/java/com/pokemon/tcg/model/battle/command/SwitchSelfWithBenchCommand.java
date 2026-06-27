package com.pokemon.tcg.model.battle.command;

import com.pokemon.tcg.model.battle.CartaEnJuego;
import com.pokemon.tcg.model.battle.Partida;
import com.pokemon.tcg.model.battle.PendingBattleAction;
import com.pokemon.tcg.model.battle.TableroJugador;

import java.util.stream.Collectors;

/**
 * Permite al atacante (self) cambiar su Pokémon Activo con uno de su banca.
 * Usado por Blastoise-EX Rapid Spin: "Switch this Pokémon with 1 of your Benched Pokémon."
 */
public class SwitchSelfWithBenchCommand implements BattleCommand {

    @Override
    public void execute(Partida partida, TableroJugador atacante, TableroJugador defensor) {
        if (atacante.getBanca().isEmpty()) {
            return; // No hay Pokémon en banca para cambiar
        }

        if (atacante == partida.getBot()) {
            // El bot elige automáticamente el primer Pokémon de su banca
            CartaEnJuego suplente = atacante.getBanca().remove(0);
            CartaEnJuego oldActive = atacante.getActivo();
            if (oldActive != null) {
                oldActive.limpiarCondiciones();
                atacante.getBanca().add(oldActive);
            }
            atacante.setActivo(suplente);
            partida.getTurnLogs().add("SELF_SWITCH:BOT:" + suplente.getCard().getNombre());
            return;
        }

        // El jugador humano elige de su banca
        PendingBattleAction action = new PendingBattleAction();
        action.setActor(partida.getJugadorUsername());
        action.setType("SWITCH_ACTIVE_SELF");
        action.setPrompt("Elegí un Pokémon de tu banca para reemplazar a tu Pokémon Activo.");
        action.setDestination("SWITCH_ACTIVE_SELF");
        action.setMinSelections(1);
        action.setMaxSelections(1);
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
                .collect(Collectors.toList()));

        partida.setPendingAction(action);
        partida.transicionarA(new com.pokemon.tcg.model.battle.state.EstadoEsperandoInteraccion());
    }
}
