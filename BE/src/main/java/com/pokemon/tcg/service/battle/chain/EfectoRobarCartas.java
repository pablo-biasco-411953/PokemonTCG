package com.pokemon.tcg.service.battle.chain;

import com.pokemon.tcg.model.Card;
import com.pokemon.tcg.model.battle.TableroJugador;

// El atacante (o su entrenador) roba cartas del mazo.
// Texto: "draw a card" / "draw 1 card" / "draw 2 cards" / "draw 3 cards"
public class EfectoRobarCartas extends ManejadorEfecto {

    @Override
    protected void manejar(ContextoAtaque ctx) {
        String texto = ctx.textoAtaque();
        if (!texto.contains("draw a card") && !texto.contains("draw 1 card")
                && !texto.contains("draw 2 cards") && !texto.contains("draw 3 cards")) return;

        int aRobar = 1;
        if (texto.contains("2 cards")) aRobar = 2;
        else if (texto.contains("3 cards")) aRobar = 3;

        TableroJugador tableroAtacante = (ctx.partida.getJugador().getActivo() == ctx.atacante)
                ? ctx.partida.getJugador()
                : ctx.partida.getBot();

        for (int i = 0; i < aRobar; i++) {
            if (!tableroAtacante.getMazo().isEmpty()) {
                Card carta = tableroAtacante.getMazo().remove(0);
                tableroAtacante.getMano().add(carta);
            }
        }
        System.out.println("🃏 " + ctx.atacante.getCard().getNombre() + " hizo que su entrenador robe " + aRobar + " carta(s).");
    }
}
