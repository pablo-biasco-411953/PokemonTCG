package com.pokemon.tcg.service.battle.chain;

import com.pokemon.tcg.model.battle.Ataque;
import com.pokemon.tcg.model.battle.CartaEnJuego;
import com.pokemon.tcg.model.battle.Partida;
import com.pokemon.tcg.service.BattleAttackService;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class ContextoAtaque {

    public final Partida partida;
    public final Ataque ataque;
    public final CartaEnJuego atacante;
    public final CartaEnJuego defensor;
    public final BattleAttackService.KoResolver koResolver;
    public final Random random;

    public int danioFinal;
    public int carasSacadas;
    public boolean ataqueAnulado;
    public final List<Boolean> historialMonedas = new ArrayList<>();

    public ContextoAtaque(
            Partida partida,
            Ataque ataque,
            CartaEnJuego atacante,
            CartaEnJuego defensor,
            BattleAttackService.KoResolver koResolver,
            Random random
    ) {
        this.partida = partida;
        this.ataque = ataque;
        this.atacante = atacante;
        this.defensor = defensor;
        this.koResolver = koResolver;
        this.random = random;
        this.danioFinal = ataque.getDanio();
        this.carasSacadas = 0;
        this.ataqueAnulado = false;
    }

    public String textoAtaque() {
        return ataque.getTexto() != null ? ataque.getTexto().toLowerCase() : "";
    }
}
