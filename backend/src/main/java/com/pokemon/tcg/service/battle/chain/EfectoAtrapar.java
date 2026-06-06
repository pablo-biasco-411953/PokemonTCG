package com.pokemon.tcg.service.battle.chain;

// Impide que el defensor se retire el próximo turno.
// Texto: "can't retreat during your opponent's next turn" / "cannot retreat"
public class EfectoAtrapar extends ManejadorEfecto {

    @Override
    protected void manejar(ContextoAtaque ctx) {
        String texto = ctx.textoAtaque();
        if (!texto.contains("can't retreat during your opponent's next turn") && !texto.contains("cannot retreat")) return;

        ctx.defensor.agregarCondicion("CantRetreat");
        System.out.println("🪤 ¡" + ctx.defensor.getCard().getNombre() + " quedó atrapado! No podrá huir el próximo turno.");
    }
}
