package com.pokemon.tcg.service.battle.chain;

// Anula el ataque si la moneda sale cruz.
// Texto: "tails, this attack does nothing" / "tails, that attack does nothing"
public class EfectoMonedaFalla extends ManejadorEfecto {

    @Override
    protected void manejar(ContextoAtaque ctx) {
        String texto = ctx.textoAtaque();
        if (!texto.contains("tails, this attack does nothing") && !texto.contains("tails, that attack does nothing")) return;

        boolean esCara = ctx.random.nextBoolean();
        ctx.historialMonedas.add(esCara);

        if (!esCara) {
            System.out.println("🪙 Salió CRUZ. El ataque falló completamente.");
            ctx.danioFinal = 0;
            ctx.ataqueAnulado = true;
        } else {
            System.out.println("🪙 Salió CARA. ¡El ataque acierta!");
            ctx.carasSacadas = 1;
        }
    }
}
