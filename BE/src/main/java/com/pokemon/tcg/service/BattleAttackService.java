package com.pokemon.tcg.service;

import com.pokemon.tcg.model.battle.Ataque;
import com.pokemon.tcg.model.battle.CartaEnJuego;
import com.pokemon.tcg.model.battle.Partida;
import com.pokemon.tcg.model.battle.ResultadoAtaque;
import com.pokemon.tcg.service.battle.chain.CadenaAtaqueFactory;
import com.pokemon.tcg.service.battle.chain.ContextoAtaque;
import com.pokemon.tcg.service.battle.chain.ManejadorEfecto;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Random;

@Service
public class BattleAttackService {

    @FunctionalInterface
    public interface KoResolver {
        void resolve(Partida partida, CartaEnJuego atacante, CartaEnJuego defensor);
    }

    public record AttackResolution(ResultadoAtaque resultado, List<Boolean> historialMonedas) {}

    private final Random random = new Random();

    public AttackResolution resolveAttack(
            Partida partida,
            Ataque ataque,
            CartaEnJuego atacante,
            CartaEnJuego defensor,
            KoResolver koResolver
    ) {
        ContextoAtaque ctx = new ContextoAtaque(partida, ataque, atacante, defensor, koResolver, random);

        ManejadorEfecto cadenaPreDanio = CadenaAtaqueFactory.buildCadenaPreDanio();
        cadenaPreDanio.procesar(ctx);

        if (!ctx.ataqueAnulado) {
            defensor.setHpActual(Math.max(0, defensor.getHpActual() - ctx.danioFinal));
            System.out.println("⚔️ [BATTLE] " + atacante.getCard().getNombre()
                    + " usó [" + ataque.getNombre() + "] y atacó a "
                    + defensor.getCard().getNombre() + " por " + ctx.danioFinal);

            boolean defensorVivo = defensor.getHpActual() > 0;
            boolean huboContacto = ctx.danioFinal > 0 || ataque.getDanio() == 0;

            if (defensorVivo && huboContacto) {
                ManejadorEfecto cadenaEfectos = CadenaAtaqueFactory.buildCadenaEfectosSecundarios();
                cadenaEfectos.procesar(ctx);
            }

            if (defensor.getHpActual() <= 0) {
                koResolver.resolve(partida, atacante, defensor);
            }
        }

        return new AttackResolution(
                new ResultadoAtaque(ctx.danioFinal, ctx.carasSacadas),
                ctx.historialMonedas
        );
    }
}
