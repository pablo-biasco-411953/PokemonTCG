package com.pokemon.tcg.service.battle.chain;

// Envenena al defensor.
// Texto: "is now poisoned"
public class EfectoVeneno extends ManejadorEfecto {

    @Override
    protected void manejar(ContextoAtaque ctx) {
        if (!ctx.textoAtaque().contains("is now poisoned")) return;

        ctx.defensor.agregarCondicion("Poisoned");
        System.out.println("☠️ " + ctx.defensor.getCard().getNombre() + " fue Envenenado.");
    }
}
