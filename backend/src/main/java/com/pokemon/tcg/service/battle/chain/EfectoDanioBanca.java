package com.pokemon.tcg.service.battle.chain;

import com.pokemon.tcg.model.battle.CartaEnJuego;
import com.pokemon.tcg.model.battle.TableroJugador;

// Aplica daño colateral a un Pokémon aleatorio de la banca rival.
// Texto: "damage to 1 of your opponent's benched"
public class EfectoDanioBanca extends ManejadorEfecto {

    @Override
    protected void manejar(ContextoAtaque ctx) {
        String texto = ctx.textoAtaque();
        if (!texto.contains("damage to 1 of your opponent's benched")) return;

        int danioBanca = 10;
        if (texto.contains("does 20 damage")) danioBanca = 20;
        else if (texto.contains("does 30 damage")) danioBanca = 30;
        else if (texto.contains("does 40 damage")) danioBanca = 40;

        TableroJugador tableroRival = (ctx.partida.getJugador().getActivo() == ctx.defensor)
                ? ctx.partida.getJugador()
                : ctx.partida.getBot();

        if (tableroRival.getBanca().isEmpty()) return;

        CartaEnJuego victima = tableroRival.getBanca().get(ctx.random.nextInt(tableroRival.getBanca().size()));
        int hpRestante = Math.max(0, victima.getHpActual() - danioBanca);
        victima.setHpActual(hpRestante);

        System.out.println("☄️ ¡Daño colateral! " + victima.getCard().getNombre() + " (Banca) recibió " + danioBanca + " de daño.");

        if (hpRestante <= 0) {
            ctx.koResolver.resolve(ctx.partida, ctx.atacante, victima);
        }
    }
}
