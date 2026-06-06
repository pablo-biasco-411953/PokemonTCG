package com.pokemon.tcg.service.battle.chain;

import com.pokemon.tcg.model.Card;

// Destruye energías del defensor — puede escalar según caras obtenidas.
// Texto: "discard an energy" / "discard 1 energy" (sin "attached to this")
public class EfectoDescartarEnergiaRival extends ManejadorEfecto {

    @Override
    protected void manejar(ContextoAtaque ctx) {
        String texto = ctx.textoAtaque();
        if (!texto.contains("discard an energy") && !texto.contains("discard 1 energy")) return;

        // Si es energía propia, EfectoDescartarEnergiaPropia ya lo maneja
        boolean esPropio = texto.contains("attached to this")
                || texto.contains("attached to " + ctx.atacante.getCard().getNombre().toLowerCase());
        if (esPropio) return;

        int aRomper = 1;
        if (texto.contains("for each heads")) aRomper = ctx.carasSacadas;

        for (int i = 0; i < aRomper; i++) {
            if (!ctx.defensor.getEnergiasUnidas().isEmpty()) {
                Card energiaRota = ctx.defensor.getEnergiasUnidas().remove(0);
                System.out.println("💥 ¡CRÍTICO! " + ctx.atacante.getCard().getNombre()
                        + " le destrozó la energía [" + energiaRota.getNombre() + "] a " + ctx.defensor.getCard().getNombre());
            }
        }
    }
}
