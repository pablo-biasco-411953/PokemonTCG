package com.pokemon.tcg.model.battle.command;

import com.pokemon.tcg.model.Card;
import com.pokemon.tcg.model.battle.CartaEnJuego;
import com.pokemon.tcg.model.battle.Partida;
import com.pokemon.tcg.model.battle.PendingBattleAction;
import com.pokemon.tcg.model.battle.TableroJugador;
import com.pokemon.tcg.model.battle.state.EstadoEsperandoInteraccion;

public class MoveOpponentActiveEnergyToBenchCommand implements BattleCommand {
    private final String choice;

    public MoveOpponentActiveEnergyToBenchCommand() {
        this("yes");
    }

    public MoveOpponentActiveEnergyToBenchCommand(String choice) {
        this.choice = choice;
    }

    @Override
    public void execute(Partida partida, TableroJugador atacante, TableroJugador defensor) {
        boolean isHuman = (atacante == partida.getJugador());
        boolean shouldExecute = !isHuman || "yes".equalsIgnoreCase(choice);

        if (!shouldExecute) {
            return;
        }

        if (defensor.getActivo() == null || defensor.getActivo().getEnergiasUnidas().isEmpty() || defensor.getBanca().isEmpty()) {
            return;
        }

        if (atacante == partida.getBot()) {
            // Bot automatically moves 1 energy from active to the first benched pokemon
            Card energy = defensor.getActivo().getEnergiasUnidas().remove(0);
            CartaEnJuego target = defensor.getBanca().get(0);
            target.getEnergiasUnidas().add(energy);
            partida.getTurnLogs().add("ENERGY_MOVED:BOT:" + energy.getNombre() + ":" + target.getCard().getNombre());
            return;
        }

        PendingBattleAction action = new PendingBattleAction();
        action.setActor(partida.getJugadorUsername());
        action.setType("MOVE_ENERGY_TO_OPPONENT_BENCH");
        action.setPrompt("Elegí 1 de los Pokémon en banca de tu rival para moverle una Energía desde su Pokémon Activo.");
        action.setMinSelections(1);
        action.setMaxSelections(1);
        action.setEndsTurn(true);
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
        partida.transicionarA(new EstadoEsperandoInteraccion());
    }
}
