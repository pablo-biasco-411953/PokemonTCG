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
            KoResolver koResolver
    ) {
        // Clear previous execution queue and coin flips
        partida.getExecutionQueue().clear();
        partida.getUltimasMonedasLanzadas().clear();

        int damageToDeal = ataque.getDanio();
        
        // 1. Base damage command
        if (damageToDeal > 0) {
            partida.getExecutionQueue().add(new com.pokemon.tcg.model.battle.command.DamageCommand(damageToDeal));
        }

        // 2. Parse text effects into commands
        if (ataque.getTexto() != null && !ataque.getTexto().isEmpty()) {
            List<com.pokemon.tcg.model.battle.command.BattleCommand> effectCommands = effectParserService.parseEffects(ataque.getTexto());
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

        int totalDamage = 0;

        // 3. Process queue iteratively
        while (!partida.getExecutionQueue().isEmpty()) {
            com.pokemon.tcg.model.battle.command.BattleCommand command = partida.getExecutionQueue().poll();
            command.execute(partida, atacanteTablero, defensorTablero);

            if (command instanceof com.pokemon.tcg.model.battle.command.DamageCommand) {
                totalDamage += ((com.pokemon.tcg.model.battle.command.DamageCommand) command).getAmount();
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

        int caras = 0;
        for (Boolean b : partida.getUltimasMonedasLanzadas()) {
            if (b) caras++;
        }

        return new AttackResolution(
                new ResultadoAtaque(totalDamage, caras),
                new java.util.ArrayList<>(partida.getUltimasMonedasLanzadas())
        );
    }
}
