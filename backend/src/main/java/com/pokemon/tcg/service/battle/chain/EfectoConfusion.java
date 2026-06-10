package com.pokemon.tcg.service.battle.chain;

// Confunde al defensor, opcionalmente condicionado a una moneda.
// Texto: "is now confused"
public class EfectoConfusion extends ManejadorEfecto {

    @Override
    protected void manejar(ContextoAtaque ctx) {
        if (!ctx.textoAtaque().contains("is now confused")) return;

        if (ctx.textoAtaque().contains("flip a coin")) {
            boolean esCara = ctx.random.nextBoolean();
            ctx.historialMonedas.add(esCara);
            if (esCara) {
                ctx.defensor.agregarCondicion("Confused");
                System.out.println("🌀 ¡Salió CARA! " + ctx.defensor.getCard().getNombre() + " se Confundió.");
            }
        } else {
            ctx.defensor.agregarCondicion("Confused");
            System.out.println("🌀 " + ctx.defensor.getCard().getNombre() + " se Confundió.");
        }
    }
}
