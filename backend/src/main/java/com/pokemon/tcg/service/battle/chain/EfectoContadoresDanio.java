package com.pokemon.tcg.service.battle.chain;

// Escala el daño según contadores de daño que tiene el propio atacante.
// Texto: "damage counter on this" / "damage counter on it"
public class EfectoContadoresDanio extends ManejadorEfecto {

    @Override
    protected void manejar(ContextoAtaque ctx) {
        String texto = ctx.textoAtaque();
        if (!texto.contains("damage counter on this") && !texto.contains("damage counter on it")) return;

        int hpMaximo = Integer.parseInt(ctx.atacante.getCard().getHp());
        int hpFaltante = hpMaximo - ctx.atacante.getHpActual();
        int contadores = hpFaltante / 10;

        int multiplicador = texto.contains("20 more damage") ? 20 : 10;
        int danioExtra = contadores * multiplicador;

        System.out.println("💢 " + ctx.atacante.getCard().getNombre() + " está herido (" + contadores + " contadores). ¡Hace " + danioExtra + " de daño extra!");
        ctx.danioFinal += danioExtra;
    }
}
