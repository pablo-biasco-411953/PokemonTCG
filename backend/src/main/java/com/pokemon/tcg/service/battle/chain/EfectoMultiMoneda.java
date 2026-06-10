package com.pokemon.tcg.service.battle.chain;

// Multiplica el daño base por la cantidad de caras obtenidas en múltiples monedas.
// Texto: "times the number of heads" / "x the number of heads" / "for each heads"
public class EfectoMultiMoneda extends ManejadorEfecto {

    @Override
    protected void manejar(ContextoAtaque ctx) {
        String texto = ctx.textoAtaque();
        if (!texto.contains("times the number of heads")
                && !texto.contains("x the number of heads")
                && !texto.contains("for each heads")) return;

        int monedas = 1;
        if (texto.contains("2 coins")) monedas = 2;
        else if (texto.contains("3 coins")) monedas = 3;
        else if (texto.contains("4 coins")) monedas = 4;
        else if (texto.contains("5 coins")) monedas = 5;

        int caras = 0;
        for (int i = 0; i < monedas; i++) {
            boolean esCara = ctx.random.nextBoolean();
            ctx.historialMonedas.add(esCara);
            if (esCara) caras++;
        }

        System.out.println("🪙 Se tiraron " + monedas + " monedas. Caras: " + caras + ". Daño calculado: " + (ctx.ataque.getDanio() * caras));
        ctx.danioFinal = ctx.ataque.getDanio() * caras;
        ctx.carasSacadas = caras;
    }
}
