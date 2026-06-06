package com.pokemon.tcg.service.battle.chain;

// Escala el daño según la cantidad de energías unidas al atacante.
// Texto: "for each energy attached" / "for each extra energy"
public class EfectoEscalaPorEnergias extends ManejadorEfecto {

    @Override
    protected void manejar(ContextoAtaque ctx) {
        String texto = ctx.textoAtaque();
        if (!texto.contains("for each energy attached") && !texto.contains("for each extra energy")) return;

        int energias = ctx.atacante.getEnergiasUnidas().size();
        int multiplicador = 10;
        if (texto.contains("20 more damage") || texto.contains("20 damage")) multiplicador = 20;
        else if (texto.contains("30 more damage") || texto.contains("30 damage")) multiplicador = 30;

        int danioExtra = energias * multiplicador;
        System.out.println("🔋 " + ctx.atacante.getCard().getNombre() + " canaliza sus " + energias + " energías. ¡Hace " + danioExtra + " de daño extra!");
        ctx.danioFinal += danioExtra;
    }
}
