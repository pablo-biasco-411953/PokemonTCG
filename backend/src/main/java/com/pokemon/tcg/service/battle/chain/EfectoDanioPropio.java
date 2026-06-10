package com.pokemon.tcg.service.battle.chain;

// El atacante se hace daño a sí mismo por retroceso.
// Texto: "damage to itself"
public class EfectoDanioPropio extends ManejadorEfecto {

    @Override
    protected void manejar(ContextoAtaque ctx) {
        String texto = ctx.textoAtaque();
        if (!texto.contains("damage to itself")) return;

        int autoDanio = 10;
        if (texto.contains("20 damage")) autoDanio = 20;
        else if (texto.contains("30 damage")) autoDanio = 30;
        else if (texto.contains("40 damage")) autoDanio = 40;

        ctx.atacante.setHpActual(Math.max(0, ctx.atacante.getHpActual() - autoDanio));
        System.out.println("💥 ¡Ouch! " + ctx.atacante.getCard().getNombre() + " se hizo " + autoDanio + " de daño a sí mismo por el retroceso.");

        if (ctx.atacante.getHpActual() <= 0) {
            System.out.println("💀 " + ctx.atacante.getCard().getNombre() + " se debilitó por su propio ataque.");
            ctx.koResolver.resolve(ctx.partida, ctx.defensor, ctx.atacante);
        }
    }
}
