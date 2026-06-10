package com.pokemon.tcg.service.battle.chain;

// Duplica el daño si la moneda sale cara.
// Texto: "if heads" + "more damage" / "damage plus"
public class EfectoMonedaExtraDanio extends ManejadorEfecto {

    @Override
    protected void manejar(ContextoAtaque ctx) {
        String texto = ctx.textoAtaque();
        if (!texto.contains("if heads")) return;
        if (!texto.contains("more damage") && !texto.contains("damage plus")) return;

        boolean esCara = ctx.random.nextBoolean();
        ctx.historialMonedas.add(esCara);

        if (esCara) {
            System.out.println("🪙 ¡Salió CARA! Daño extra aplicado.");
            ctx.danioFinal += ctx.ataque.getDanio();
            ctx.carasSacadas = 1;
        } else {
            System.out.println("🪙 Salió CRUZ. Solo hace el daño base.");
        }
    }
}
