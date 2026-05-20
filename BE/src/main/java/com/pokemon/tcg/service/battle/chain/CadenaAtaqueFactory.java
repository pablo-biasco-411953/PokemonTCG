package com.pokemon.tcg.service.battle.chain;

public class CadenaAtaqueFactory {

    // Cadena de modificadores de daño — se ejecuta ANTES de aplicar HP.
    // Refleja exactamente la lógica de BattleAttackService.calcularDanioPorEfectos()
    public static ManejadorEfecto buildCadenaPreDanio() {
        ManejadorEfecto cabeza = new EfectoContadoresDanio();
        cabeza
            .encadenar(new EfectoInmunidad())
            .encadenar(new EfectoEscalaPorEnergias())
            .encadenar(new EfectoMonedaFalla())
            .encadenar(new EfectoMultiMoneda())
            .encadenar(new EfectoMonedaExtraDanio());
        return cabeza;
    }

    // Cadena de efectos secundarios — se ejecuta DESPUÉS de aplicar HP, solo si el defensor sobrevivió.
    // Refleja exactamente la lógica de BattleAttackService.aplicarEfectosSecundarios()
    public static ManejadorEfecto buildCadenaEfectosSecundarios() {
        ManejadorEfecto cabeza = new EfectoCuracion();
        cabeza
            .encadenar(new EfectoDanioBanca())
            .encadenar(new EfectoRobarCartas())
            .encadenar(new EfectoDanioPropio())
            .encadenar(new EfectoDescartarEnergiaPropia())
            .encadenar(new EfectoParalisis())
            .encadenar(new EfectoDescartarEnergiaRival())
            .encadenar(new EfectoAtrapar())
            .encadenar(new EfectoVeneno())
            .encadenar(new EfectoSueno())
            .encadenar(new EfectoQuemadura())
            .encadenar(new EfectoConfusion());
        return cabeza;
    }
}
