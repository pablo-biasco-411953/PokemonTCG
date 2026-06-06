package com.pokemon.tcg.service.battle.chain;

// Cura HP del atacante después del ataque.
// Texto: "heal X from this pokémon" / "from 1 of your pokémon"
public class EfectoCuracion extends ManejadorEfecto {

    @Override
    protected void manejar(ContextoAtaque ctx) {
        String texto = ctx.textoAtaque();
        boolean esPropio = texto.contains("from this pok") || texto.contains("from 1 of your pok");
        if (!texto.contains("heal") || !esPropio) return;

        int cantidadCura = 20;
        if (texto.contains("10 damage")) cantidadCura = 10;
        else if (texto.contains("30 damage")) cantidadCura = 30;
        else if (texto.contains("40 damage")) cantidadCura = 40;
        else if (texto.contains("50 damage")) cantidadCura = 50;

        int hpMaximo = Integer.parseInt(ctx.atacante.getCard().getHp());
        int hpAntes = ctx.atacante.getHpActual();
        int nuevoHp = Math.min(hpMaximo, hpAntes + cantidadCura);
        ctx.atacante.setHpActual(nuevoHp);

        System.out.println("💖 " + ctx.atacante.getCard().getNombre() + " se curó " + (nuevoHp - hpAntes) + " HP.");
    }
}
