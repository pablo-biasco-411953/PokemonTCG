package com.pokemon.tcg.model.battle.command;

import com.pokemon.tcg.model.battle.CartaEnJuego;
import com.pokemon.tcg.model.battle.Partida;
import com.pokemon.tcg.model.battle.PendingBattleAction;
import com.pokemon.tcg.model.battle.TableroJugador;
import com.pokemon.tcg.model.battle.state.EstadoEsperandoInteraccion;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class FroakieBounceCommand implements BattleCommand {
    private final Random random = new Random();

    @Override
    public void execute(Partida partida, TableroJugador atacante, TableroJugador defensor) {
        if (atacante.getBanca().isEmpty()) {
            return;
        }

        boolean isHeads = random.nextBoolean();
        List<Boolean> flips = new ArrayList<>();
        flips.add(isHeads);
        partida.setUltimasMonedasLanzadas(flips);

        String actor = (atacante == partida.getJugador()) ? "JUGADOR" : "BOT";
        partida.getTurnLogs().add("COIN_FLIP:" + actor + ":" + List.of(isHeads ? "CARA" : "CRUZ"));

        if (!isHeads) {
            System.out.println("🪙 Bounce: salio CRUZ, no se realiza el cambio.");
            return;
        }

        System.out.println("🪙 Bounce: salio CARA, iniciando cambio...");

        if (atacante == partida.getBot()) {
            // For the bot, select a benched pokemon (simply the first one) and swap
            CartaEnJuego suplente = atacante.getBanca().get(0);
            CartaEnJuego activoViejo = atacante.getActivo();
            if (activoViejo != null) {
                activoViejo.limpiarCondiciones();
                atacante.getBanca().remove(suplente);
                atacante.getBanca().add(activoViejo);
            }
            atacante.setActivo(suplente);
            partida.getTurnLogs().add("ACTIVE_SWITCHED:BOT:" + suplente.getCard().getNombre());
            System.out.println("🤖 Bot activo cambiado a: " + suplente.getCard().getNombre());
            return;
        }

        // For the player, configure PendingBattleAction
        PendingBattleAction action = new PendingBattleAction();
        action.setActor(partida.getJugadorUsername());
        action.setType("SWITCH_ACTIVE");
        action.setPrompt("Bounce: Salió CARA. Seleccioná un Pokémon de tu banca para cambiarlo por tu activo.");
        action.setMinSelections(1);
        action.setMaxSelections(1);
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
