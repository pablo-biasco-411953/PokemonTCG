package com.pokemon.tcg.service.battle.chain;

// Duerme al defensor, opcionalmente condicionado a una moneda.
// Texto: "is now asleep"
public class EfectoSueno extends ManejadorEfecto {

    @Override
    protected void manejar(ContextoAtaque ctx) {
        if (!ctx.textoAtaque().contains("is now asleep")) return;

        if (ctx.textoAtaque().contains("flip a coin")) {
            boolean esCara = ctx.random.nextBoolean();
            ctx.historialMonedas.add(esCara);
            if (esCara) {
                ctx.defensor.agregarCondicion("Asleep");
                System.out.println("💤 ¡Salió CARA! " + ctx.defensor.getCard().getNombre() + " se quedó Dormido.");
            }
        } else {
            ctx.defensor.agregarCondicion("Asleep");
            System.out.println("💤 " + ctx.defensor.getCard().getNombre() + " se quedó Dormido.");
        }
    }
}
