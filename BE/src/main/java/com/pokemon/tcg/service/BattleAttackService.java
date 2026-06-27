package com.pokemon.tcg.service;

import com.pokemon.tcg.model.battle.Ataque;
import com.pokemon.tcg.model.battle.CartaEnJuego;
import com.pokemon.tcg.model.battle.Partida;
import com.pokemon.tcg.model.battle.ResultadoAtaque;
import org.springframework.stereotype.Service;

import java.util.List;


@Service
public class BattleAttackService {

    @FunctionalInterface
    public interface KoResolver {
        void resolve(Partida partida, CartaEnJuego atacante, CartaEnJuego defensor);
    }

    public record AttackResolution(ResultadoAtaque resultado, List<Boolean> historialMonedas) {}

    private final com.pokemon.tcg.service.battle.command.AttackEffectParserService effectParserService;

    @org.springframework.beans.factory.annotation.Autowired
    public BattleAttackService(com.pokemon.tcg.service.battle.command.AttackEffectParserService effectParserService) {
        this.effectParserService = effectParserService;
    }

    public BattleAttackService() {
        // Instantiate a simple parser that returns an empty command list
        this.effectParserService = new com.pokemon.tcg.service.battle.command.AttackEffectParserService();
    }

    public AttackResolution resolveAttack(
            Partida partida,
            Ataque ataque,
            CartaEnJuego atacante,
            CartaEnJuego defensor,
            KoResolver koResolver,
            String extraParams
    ) {
        // Clear previous execution queue and coin flips
        partida.getExecutionQueue().clear();
        partida.getUltimasMonedasLanzadas().clear();

        String effectText = ataque.getTexto() == null ? "" : ataque.getTexto().toLowerCase();
        boolean damageComesOnlyFromHeads =
                effectText.contains("damage times the number of heads")
                || (effectText.contains("damage for each heads") && !effectText.contains("more damage"));
        boolean attackFailsOnTails = effectText.contains("if tails, this attack does nothing");
        boolean damageComesOnlyFromBenched = effectText.contains("damage times the number of your benched");
        int damageToDeal = damageComesOnlyFromHeads || attackFailsOnTails || damageComesOnlyFromBenched ? 0 : ataque.getDanio();
        
        // 1. Base damage command
        if (attackFailsOnTails && ataque.getDanio() > 0) {
            partida.getExecutionQueue().add(new com.pokemon.tcg.model.battle.command.CoinFlipCommand(
                    new com.pokemon.tcg.model.battle.command.DamageCommand(ataque.getDanio())
            ));
        } else if (damageToDeal > 0) {
            int finalDamageToDeal = damageToDeal;
            if (atacante.getAtaquePotenciadoSiguienteTurno() != null
                && atacante.getAtaquePotenciadoSiguienteTurno().equalsIgnoreCase(ataque.getNombre())
                && atacante.getDanioExtraSiguienteTurno() > 0) {
                finalDamageToDeal += atacante.getDanioExtraSiguienteTurno();
            }
            if (atacante.getReduccionDanioCausadoSiguienteTurno() > 0) {
                finalDamageToDeal = Math.max(0, finalDamageToDeal - atacante.getReduccionDanioCausadoSiguienteTurno());
            }
            partida.getExecutionQueue().add(new com.pokemon.tcg.model.battle.command.DamageCommand(finalDamageToDeal));
        }

        // 2. Parse text effects into commands
        if (ataque.getTexto() != null && !ataque.getTexto().isEmpty()) {
            List<com.pokemon.tcg.model.battle.command.BattleCommand> effectCommands = effectParserService.parseEffects(ataque.getTexto(), extraParams);
            partida.getExecutionQueue().addAll(effectCommands);
        }

        // Identify boards
        com.pokemon.tcg.model.battle.TableroJugador atacanteTablero;
        com.pokemon.tcg.model.battle.TableroJugador defensorTablero;

        if (partida.getJugador().getActivo() == atacante) {
            atacanteTablero = partida.getJugador();
            defensorTablero = partida.getBot();
        } else {
            atacanteTablero = partida.getBot();
            defensorTablero = partida.getJugador();
        }

        int rawDamage = 0;

        // 3. Process queue iteratively
        while (!partida.getExecutionQueue().isEmpty()) {
            com.pokemon.tcg.model.battle.command.BattleCommand command = partida.getExecutionQueue().poll();
            if (command instanceof com.pokemon.tcg.model.battle.command.DamageCommand) {
                rawDamage += ((com.pokemon.tcg.model.battle.command.DamageCommand) command).getAmount();
            } else {
                command.execute(partida, atacanteTablero, defensorTablero);
            }
        }
        int totalDamage = defensor.isInvulnerable() ? 0 : calcularDanioConTipo(partida, rawDamage, atacante, defensor);
        if (totalDamage > 0) {
            if (defensor.getPreventDamageThreshold() > 0 && totalDamage <= defensor.getPreventDamageThreshold()) {
                System.out.println("🛡️ [BATTLE] Harden previno " + totalDamage + " de daño a " + defensor.getCard().getNombre());
                totalDamage = 0;
            } else {
                defensor.setHpActual(Math.max(0, defensor.getHpActual() - totalDamage));
            }

            // Apply Chesnaught's Spiky Shield Ability
            boolean hasSpikyShield = defensor.getCard().getHabilidades() != null && defensor.getCard().getHabilidades().stream()
                    .anyMatch(h -> "Spiky Shield".equalsIgnoreCase(h.getNombre()));
            if (hasSpikyShield) {
                atacante.setHpActual(Math.max(0, atacante.getHpActual() - 30));
                System.out.println("[ABILITY] Spiky Shield coloca 3 contadores de daño en " + atacante.getCard().getNombre());
            }

            // Apply Voltorb's Destiny Burst Ability
            boolean hasDestinyBurst = defensor.getCard().getHabilidades() != null && defensor.getCard().getHabilidades().stream()
                    .anyMatch(h -> "Destiny Burst".equalsIgnoreCase(h.getNombre()));
            if (hasDestinyBurst && defensor.getHpActual() <= 0) {
                boolean coinHeads = new java.util.Random().nextBoolean();
                partida.getUltimasMonedasLanzadas().add(coinHeads);
                if (coinHeads) {
                    atacante.setHpActual(Math.max(0, atacante.getHpActual() - 50));
                    System.out.println("[ABILITY] Destiny Burst salió CARA: coloca 5 contadores de daño en " + atacante.getCard().getNombre());
                } else {
                    System.out.println("[ABILITY] Destiny Burst salió SECA: no hace nada.");
                }
            }
        }

        System.out.println("⚔️ [BATTLE] " + atacante.getCard().getNombre()
                + " usó [" + ataque.getNombre() + "] y atacó a "
                + defensor.getCard().getNombre() + " por un total de " + totalDamage);

        // 4. Check for KOs
        if (defensor.getHpActual() <= 0) {
            koResolver.resolve(partida, atacante, defensor);
        }
        if (atacante.getHpActual() <= 0) {
            koResolver.resolve(partida, defensor, atacante);
        }

        revalidarSeleccionDeBancaDespuesDeKo(partida, defensorTablero);

        int caras = 0;
        for (Boolean b : partida.getUltimasMonedasLanzadas()) {
            if (b) caras++;
        }

        return new AttackResolution(
                new ResultadoAtaque(totalDamage, caras),
                new java.util.ArrayList<>(partida.getUltimasMonedasLanzadas())
        );
    }

    private void revalidarSeleccionDeBancaDespuesDeKo(
            Partida partida,
            com.pokemon.tcg.model.battle.TableroJugador tableroDefensor
    ) {
        com.pokemon.tcg.model.battle.PendingBattleAction pending = partida.getPendingAction();
        if (pending == null || !"CHOOSE_OPPONENT_BENCH_TO_DAMAGE".equals(pending.getType())) {
            return;
        }

        java.util.Set<String> idsEnBanca = tableroDefensor.getBanca().stream()
                .map(carta -> carta.getCard().getId())
                .collect(java.util.stream.Collectors.toSet());
        List<com.pokemon.tcg.model.battle.PendingBattleAction.Option> opcionesValidas = pending.getOptions().stream()
                .filter(option -> idsEnBanca.contains(option.getId()))
                .toList();

        if (opcionesValidas.isEmpty()) {
            partida.setPendingAction(null);
            if (partida.getFaseActual() == Partida.Fase.ESPERANDO_INTERACCION) {
                partida.transicionarA(new com.pokemon.tcg.model.battle.state.EstadoTurnoNormal());
            }
            return;
        }

        int seleccionesDisponibles = Math.min(pending.getMaxSelections(), opcionesValidas.size());
        pending.setOptions(opcionesValidas);
        pending.setMinSelections(Math.min(pending.getMinSelections(), seleccionesDisponibles));
        pending.setMaxSelections(seleccionesDisponibles);
    }

    public void registrarEventoMoneda(Partida partida, String actor, String attackName) {
        partida.setLastCoinFlipEventId(partida.getLastCoinFlipEventId() + 1);
        partida.setLastCoinFlipActor(actor);
        partida.setLastCoinFlipAttackName(attackName);
    }

    private int calcularDanioConTipo(Partida partida, int damage, CartaEnJuego atacante, CartaEnJuego defensor) {
        if (damage <= 0 || atacante == null || defensor == null) return Math.max(0, damage);
        String attackType = atacante.getCard().getTipo();
        int result = damage;

        // Apply Muscle Band (+20 damage before Weakness/Resistance)
        boolean hasMuscleBand = atacante.getAttachedTools() != null && atacante.getAttachedTools().stream()
                .anyMatch(t -> "xy1-121".equals(t.getId()) || "Muscle Band".equalsIgnoreCase(t.getNombre()));
        if (hasMuscleBand) {
            result += 20;
        }

        // Apply Shadow Circle (No Weakness if Darkness energy is attached)
        boolean isShadowCircleActive = partida.getActiveStadium() != null && ("xy1-126".equals(partida.getActiveStadium().getId()) || "Shadow Circle".equalsIgnoreCase(partida.getActiveStadium().getNombre()));
        boolean hasDarknessEnergy = defensor.getEnergiasUnidas() != null && defensor.getEnergiasUnidas().stream()
                .anyMatch(e -> "Darkness".equalsIgnoreCase(e.getTipo()) || (e.getNombre() != null && e.getNombre().toLowerCase().contains("darkness energy")));
        boolean applyWeakness = !(isShadowCircleActive && hasDarknessEnergy);

        if (applyWeakness && matchesType(defensor.getCard().getDebilidades(), attackType)) {
            result *= 2;
        }
        if (matchesType(defensor.getCard().getResistencias(), attackType)) {
            result = Math.max(0, result - 20);
        }

        // Apply Hard Charm (-20 damage after Weakness/Resistance)
        boolean hasHardCharm = defensor.getAttachedTools() != null && defensor.getAttachedTools().stream()
                .anyMatch(t -> "xy1-119".equals(t.getId()) || "Hard Charm".equalsIgnoreCase(t.getNombre()));
        if (hasHardCharm) {
            result = Math.max(0, result - 20);
        }

        // Apply Furfrou's Fur Coat Ability (-20 damage after Weakness/Resistance)
        boolean hasFurCoat = defensor.getCard().getHabilidades() != null && defensor.getCard().getHabilidades().stream()
                .anyMatch(h -> "Fur Coat".equalsIgnoreCase(h.getNombre()));
        if (hasFurCoat) {
            result = Math.max(0, result - 20);
        }

        return result;
    }

    private boolean matchesType(List<com.pokemon.tcg.model.CardAttribute> attributes, String attackType) {
        if (attributes == null || attackType == null) return false;
        return attributes.stream()
                .filter(java.util.Objects::nonNull)
                .map(com.pokemon.tcg.model.CardAttribute::getType)
                .filter(java.util.Objects::nonNull)
                .anyMatch(type -> type.equalsIgnoreCase(attackType));
    }
}
