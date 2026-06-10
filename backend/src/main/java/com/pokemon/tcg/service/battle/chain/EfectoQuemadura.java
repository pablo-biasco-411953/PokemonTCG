package com.pokemon.tcg.service.battle.chain;

// Quema al defensor.
// Texto: "is now burned"
public class EfectoQuemadura extends ManejadorEfecto {

    @Override
    protected void manejar(ContextoAtaque ctx) {
        if (!ctx.textoAtaque().contains("is now burned")) return;

        ctx.defensor.agregarCondicion("Burned");
        System.out.println("🔥 " + ctx.defensor.getCard().getNombre() + " se Quemó.");
    }
}
