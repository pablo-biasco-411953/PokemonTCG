package com.pokemon.tcg.service.battle.chain;

// Activa el escudo de invulnerabilidad del atacante para turnos siguientes.
// Texto: "prevent all effects of attacks, including damage"
public class EfectoInmunidad extends ManejadorEfecto {

    @Override
    protected void manejar(ContextoAtaque ctx) {
        String texto = ctx.textoAtaque();
        if (!texto.contains("prevent all effects of attacks") || !texto.contains("including damage")) return;

        if (texto.contains("flip a coin")) {
            System.out.println("🪙 [MONEDA] Tirando para Inmunidad...");
            boolean esCara = ctx.random.nextBoolean();
            ctx.historialMonedas.add(esCara);
            ctx.atacante.setInvulnerable(esCara);
            if (esCara) {
                System.out.println("🛡️ ¡Salió CARA! " + ctx.atacante.getCard().getNombre() + " activó su escudo de inmunidad.");
            } else {
                System.out.println("💨 Salió CRUZ. " + ctx.atacante.getCard().getNombre() + " no logró protegerse.");
            }
        } else {
            ctx.atacante.setInvulnerable(true);
        }
    }
}
