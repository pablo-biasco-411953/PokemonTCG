package com.pokemon.tcg.model.battle.command;

import com.pokemon.tcg.model.battle.CartaEnJuego;
import com.pokemon.tcg.model.battle.Partida;
import com.pokemon.tcg.model.battle.TableroJugador;

import java.util.Random;

public class MultiCoinDamageCommand implements BattleCommand {
    public enum CountMode {
        FIXED,
        UNTIL_TAILS,
        ATTACHED_ENERGY,
        ATTACHED_ENERGY_TYPE,
        DAMAGE_COUNTERS
    }

    private static final int MAX_UNTIL_TAILS_FLIPS = 100;
    private static final Random RANDOM = new Random();

    private final CountMode countMode;
    private final int fixedCount;
    private final int damagePerHead;
    private final String energyType;
    private final boolean protectIfAllHeads;

    public MultiCoinDamageCommand(
            CountMode countMode,
            int fixedCount,
            int damagePerHead,
            String energyType,
            boolean protectIfAllHeads
    ) {
        this.countMode = countMode;
        this.fixedCount = fixedCount;
        this.damagePerHead = damagePerHead;
        this.energyType = energyType;
        this.protectIfAllHeads = protectIfAllHeads;
    }

    @Override
    public void execute(Partida partida, TableroJugador atacante, TableroJugador defensor) {
        int flipCount = resolveFlipCount(atacante.getActivo());
        int heads = 0;
        int flips = 0;

        if (countMode == CountMode.UNTIL_TAILS) {
            while (flips < MAX_UNTIL_TAILS_FLIPS) {
                boolean isHeads = RANDOM.nextBoolean();
                partida.getUltimasMonedasLanzadas().add(isHeads);
                flips++;
                if (!isHeads) break;
                heads++;
            }
        } else {
            for (int i = 0; i < flipCount; i++) {
                boolean isHeads = RANDOM.nextBoolean();
                partida.getUltimasMonedasLanzadas().add(isHeads);
                flips++;
                if (isHeads) heads++;
            }
        }

        if (heads > 0 && damagePerHead > 0) {
            partida.getExecutionQueue().addFirst(new DamageCommand(heads * damagePerHead));
        }
        if (protectIfAllHeads && flips > 0 && heads == flips && atacante.getActivo() != null) {
            atacante.getActivo().setInvulnerable(true);
        }
    }

    private int resolveFlipCount(CartaEnJuego active) {
        if (active == null) return 0;
        return switch (countMode) {
            case FIXED -> fixedCount;
            case UNTIL_TAILS -> 0;
            case ATTACHED_ENERGY -> active.getEnergiasUnidas().size();
            case ATTACHED_ENERGY_TYPE -> (int) active.getEnergiasUnidas().stream()
                    .filter(card -> card != null && normalizeEnergyType(card).equalsIgnoreCase(energyType))
                    .count();
            case DAMAGE_COUNTERS -> {
                int maxHp;
                try {
                    maxHp = Integer.parseInt(active.getCard().getHp());
                } catch (NumberFormatException ignored) {
                    maxHp = active.getHpActual();
                }
                yield Math.max(0, maxHp - active.getHpActual()) / 10;
            }
        };
    }

    private String normalizeEnergyType(com.pokemon.tcg.model.Card card) {
        String value = card.getTipo();
        if (value == null || value.equalsIgnoreCase("Energy")) value = card.getNombre();
        return value == null ? "" : value.replace(" Energy", "").trim();
    }
}
