package com.pokemon.tcg.model.battle.state;

import com.pokemon.tcg.model.battle.Partida;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EstadosPartidaTest {

    @Test
    void estadoInicio_noPermiteAcciones() {
        EstadoInicio e = new EstadoInicio();
        assertFalse(e.permiteAccionesDeJuego());
    }

    @Test
    void estadoInicio_faseCorrecta() {
        assertEquals(Partida.Fase.INICIO, new EstadoInicio().getFase());
    }

    @Test
    void estadoTurnoNormal_permiteAcciones() {
        assertTrue(new EstadoTurnoNormal().permiteAccionesDeJuego());
    }

    @Test
    void estadoTurnoNormal_faseCorrecta() {
        assertEquals(Partida.Fase.TURNO_NORMAL, new EstadoTurnoNormal().getFase());
    }

    @Test
    void estadoFinPartida_noPermiteAcciones() {
        assertFalse(new EstadoFinPartida().permiteAccionesDeJuego());
    }

    @Test
    void estadoFinPartida_faseCorrecta() {
        assertEquals(Partida.Fase.FIN_PARTIDA, new EstadoFinPartida().getFase());
    }

    @Test
    void estadoLanzamientoMoneda_noPermiteAcciones() {
        assertFalse(new EstadoLanzamientoMoneda().permiteAccionesDeJuego());
    }

    @Test
    void estadoLanzamientoMoneda_faseCorrecta() {
        assertEquals(Partida.Fase.LANZAMIENTO_MONEDA, new EstadoLanzamientoMoneda().getFase());
    }

    @Test
    void estadoEsperandoInteraccion_noPermiteAcciones() {
        assertFalse(new EstadoEsperandoInteraccion().permiteAccionesDeJuego());
    }

    @Test
    void estadoEsperandoInteraccion_faseCorrecta() {
        assertEquals(Partida.Fase.ESPERANDO_INTERACCION, new EstadoEsperandoInteraccion().getFase());
    }

    @Test
    void estadoSetupInitialDraw_noPermiteAcciones() {
        assertFalse(new EstadoSetupInitialDraw().permiteAccionesDeJuego());
    }

    @Test
    void estadoSetupInitialDraw_faseCorrecta() {
        assertEquals(Partida.Fase.SETUP_INITIAL_DRAW, new EstadoSetupInitialDraw().getFase());
    }

    @Test
    void estadoSetupMulliganEvaluation_noPermiteAcciones() {
        assertFalse(new EstadoSetupMulliganEvaluation().permiteAccionesDeJuego());
    }

    @Test
    void estadoSetupMulliganReveal_faseCorrecta() {
        assertEquals(Partida.Fase.SETUP_MULLIGAN_REVEAL, new EstadoSetupMulliganReveal().getFase());
    }

    @Test
    void estadoSetupMulliganExtraDraw_noPermiteAcciones() {
        assertFalse(new EstadoSetupMulliganExtraDraw().permiteAccionesDeJuego());
    }

    @Test
    void estadoSetupPlaceActive_noPermiteAcciones() {
        assertFalse(new EstadoSetupPlaceActive().permiteAccionesDeJuego());
    }

    @Test
    void estadoSetupPlaceActive_faseCorrecta() {
        assertEquals(Partida.Fase.SETUP_PLACE_ACTIVE, new EstadoSetupPlaceActive().getFase());
    }

    @Test
    void estadoSetupPlaceBench_noPermiteAcciones() {
        assertFalse(new EstadoSetupPlaceBench().permiteAccionesDeJuego());
    }

    @Test
    void estadoSetupPlaceBenchExtra_noPermiteAcciones() {
        assertFalse(new EstadoSetupPlaceBenchExtra().permiteAccionesDeJuego());
    }

    @Test
    void estadoSetupPrizePlacement_noPermiteAcciones() {
        assertFalse(new EstadoSetupPrizePlacement().permiteAccionesDeJuego());
    }

    @Test
    void estadoSetupPrizePlacement_faseCorrecta() {
        assertEquals(Partida.Fase.SETUP_PRIZE_PLACEMENT, new EstadoSetupPrizePlacement().getFase());
    }

    @Test
    void estadoSetupReveal_noPermiteAcciones() {
        assertFalse(new EstadoSetupReveal().permiteAccionesDeJuego());
    }

    @Test
    void estadoSetupReveal_faseCorrecta() {
        assertEquals(Partida.Fase.SETUP_REVEAL, new EstadoSetupReveal().getFase());
    }
}
