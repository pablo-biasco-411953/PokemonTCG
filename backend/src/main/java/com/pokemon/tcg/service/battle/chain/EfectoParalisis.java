package com.pokemon.tcg.service.battle.chain;

// Paraliza al defensor, opcionalmente condicionado a una moneda.
// Texto: "is now paralyzed"
public class EfectoParalisis extends ManejadorEfecto {

    @Override
    protected void manejar(ContextoAtaque ctx) {
        if (!ctx.textoAtaque().contains("is now paralyzed")) return;

        if (ctx.textoAtaque().contains("flip a coin")) {
            boolean esCara = ctx.random.nextBoolean();
            ctx.historialMonedas.add(esCara);
            if (esCara) {
                ctx.defensor.agregarCondicion("Paralyzed");
                System.out.println("⚡ ¡Salió CARA! " + ctx.defensor.getCard().getNombre() + " fue Paralizado.");
            } else {
                System.out.println("💨 Salió CRUZ. Se salvó de la Parálisis.");
            }
        } else {
            ctx.defensor.agregarCondicion("Paralyzed");
            System.out.println("⚡ " + ctx.defensor.getCard().getNombre() + " fue Paralizado (100% de chance).");
        }
    }
}
