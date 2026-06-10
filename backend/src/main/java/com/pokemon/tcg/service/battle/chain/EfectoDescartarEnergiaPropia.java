package com.pokemon.tcg.service.battle.chain;

import com.pokemon.tcg.model.Card;
import com.pokemon.tcg.model.battle.TableroJugador;

// Descarta energías del propio atacante como costo del ataque.
// Texto: "discard an energy card attached to this" / "discard 1 energy card attached to this"
public class EfectoDescartarEnergiaPropia extends ManejadorEfecto {

    @Override
    protected void manejar(ContextoAtaque ctx) {
        String texto = ctx.textoAtaque();
        boolean mencionaDescartar = texto.contains("discard an energy card attached to")
                || texto.contains("discard 1 energy card attached to")
                || texto.contains("discard 2 energy");

        if (!mencionaDescartar) return;

        boolean esPropio = texto.contains("attached to this")
                || texto.contains("attached to " + ctx.atacante.getCard().getNombre().toLowerCase());

        if (!esPropio) return;

        int aDescartar = texto.contains("discard 2") ? 2 : 1;

        TableroJugador tableroAtacante = (ctx.partida.getJugador().getActivo() == ctx.atacante)
                ? ctx.partida.getJugador()
                : ctx.partida.getBot();

        for (int i = 0; i < aDescartar; i++) {
            if (!ctx.atacante.getEnergiasUnidas().isEmpty()) {
                Card energiaDescartada = ctx.atacante.getEnergiasUnidas().remove(0);
                tableroAtacante.getPilaDescarte().add(energiaDescartada);
                System.out.println("📉 " + ctx.atacante.getCard().getNombre() + " descartó su energía [" + energiaDescartada.getNombre() + "].");
            }
        }
    }
}
