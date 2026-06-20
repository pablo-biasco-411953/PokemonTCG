package com.pokemon.tcg.model.battle.command;

import com.pokemon.tcg.model.Card;
import com.pokemon.tcg.model.battle.Partida;
import com.pokemon.tcg.model.battle.PendingBattleAction;
import com.pokemon.tcg.model.battle.TableroJugador;
import com.pokemon.tcg.model.battle.state.EstadoEsperandoInteraccion;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class LaprasSeafaringCommand implements BattleCommand {
    private final Random random = new Random();

    public LaprasSeafaringCommand() {}

    public LaprasSeafaringCommand(String ignoredExtraParams) {}

    @Override
    public void execute(Partida partida, TableroJugador atacante, TableroJugador defensor) {
        int heads = 0;
        List<Boolean> flips = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            boolean isHeads = random.nextBoolean();
            flips.add(isHeads);
            if (isHeads) heads++;
        }
        partida.setUltimasMonedasLanzadas(flips);

        String actor = atacante == partida.getJugador() ? "JUGADOR" : "BOT";
        partida.getTurnLogs().add("COIN_FLIP:" + actor + ":" + flips.stream().map(f -> f ? "CARA" : "CRUZ").toList());

        int availableWater = (int) atacante.getPilaDescarte().stream().filter(this::isWaterEnergy).count();
        int selectable = Math.min(heads, availableWater);

        if (selectable == 0 || atacante.getBanca().isEmpty()) {
            return;
        }

        if (atacante == partida.getBot()) {
            new AttachEnergyFromDiscardToBenchCommand("Water", selectable).execute(partida, atacante, defensor);
            return;
        }

        PendingBattleAction action = new PendingBattleAction();
        action.setActor(partida.getJugadorUsername());
        action.setType("ATTACH_DISCARD_ENERGY_TO_BENCH");
        action.setPrompt(buildPrompt(heads, availableWater, atacante.getBanca().size(), selectable));
        action.setDestination("ATTACH_DISCARD_TO_BENCH");
        action.setMinSelections(selectable);
        action.setMaxSelections(selectable);
        action.setAmount(selectable);
        action.setEndsTurn(true);
        action.setOptions(selectable == 0 ? List.of() : atacante.getBanca().stream()
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

    private String buildPrompt(int heads, int availableWater, int benchSize, int selectable) {
        if (heads == 0) {
            return "Seafaring: no salio ninguna cara, asi que no se asigna Energia Agua.";
        }
        if (benchSize == 0) {
            return "Seafaring: salio " + heads + " cara" + (heads == 1 ? "" : "s") + ", pero no tenes Pokemon en Banca.";
        }
        if (availableWater == 0) {
            return "Seafaring: salio " + heads + " cara" + (heads == 1 ? "" : "s") + ", pero no hay Energia Agua en tu descarte.";
        }
        return "Seafaring: asigna " + selectable + " Energia Agua desde el descarte a tus Pokemon en Banca.";
    }

    private boolean isWaterEnergy(Card card) {
        if (card == null || card.getSupertype() == null || !card.getSupertype().equalsIgnoreCase("Energy")) {
            return false;
        }
        String value = card.getTipo();
        if (value == null || value.isBlank() || "Energy".equalsIgnoreCase(value)) {
            value = card.getNombre();
        }
        return value != null && value.toLowerCase().contains("water");
    }
}
