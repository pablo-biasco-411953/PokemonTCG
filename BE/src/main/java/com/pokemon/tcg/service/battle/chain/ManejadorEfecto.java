package com.pokemon.tcg.service.battle.chain;

public abstract class ManejadorEfecto {

    private ManejadorEfecto siguiente;

    public ManejadorEfecto encadenar(ManejadorEfecto siguiente) {
        this.siguiente = siguiente;
        return siguiente;
    }

    public final void procesar(ContextoAtaque ctx) {
        if (ctx.ataqueAnulado) return;
        manejar(ctx);
        if (siguiente != null) siguiente.procesar(ctx);
    }

    protected abstract void manejar(ContextoAtaque ctx);
}
