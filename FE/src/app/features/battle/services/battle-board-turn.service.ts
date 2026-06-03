import { Injectable } from '@angular/core';
import { Ataque } from '../../../shared/models/card';
import { CartaEnJuego, Partida } from '../../../shared/models/battle';
import { AttackCoinFlipState, CoinSide } from '../battle-board.types';
import { CoinFlipConfig } from './battle-board-attack.service';

export interface SleepCheckResult {
  estabaDormido: boolean;
  sigueDormido: boolean;
  seDesperto: boolean;
}

export interface BotTurnAnalysis {
  botEstabaDormido: boolean;
  botEstabaParalizado: boolean;
  puedeActuar: boolean;
  botAtaco: boolean;
  hpJugadorDespues: number;
  danioHecho: number;
}

@Injectable({ providedIn: 'root' })
export class BattleBoardTurnService {
  // Evalúa si un Pokémon estaba dormido y si logró despertarse después del checkup.
  evaluarDespertar(
    antes: CartaEnJuego | null | undefined,
    despues: CartaEnJuego | null | undefined,
  ): SleepCheckResult | null {
    const estabaDormido = this.tieneCondicion(antes, 'ASLEEP');
    if (!estabaDormido) return null;

    const sigueDormido = this.tieneCondicion(despues, 'ASLEEP');
    return {
      estabaDormido,
      sigueDormido,
      seDesperto: !sigueDormido,
    };
  }

  // Construye el estado inicial del overlay de monedas usado en checkup o ataques.
  crearEstadoCoinFlip(
    descripcion: string,
    nombreAtaque = 'FASE DE MANTENIMIENTO',
    cantidadMonedas = 1,
  ): AttackCoinFlipState {
    return {
      nombreAtaque,
      descripcion,
      cantidadMonedas,
      danioBase: 0,
      danioExtraPorCara: 0,
      monedas: Array.from({ length: cantidadMonedas }, () => ({ estado: 'girando' as const })),
      terminado: false,
      progreso: 0,
      esSoloEstado: true,
      danioTotal: 0,
    };
  }

  // Resume si el bot puede actuar y si realmente ejecutó un ataque.
  analizarTurnoBot(
    estadoAnterior: Partida | null,
    estadoFinal: Partida,
    hpJugadorAntes: number,
  ): BotTurnAnalysis {
    const botAntes = estadoAnterior?.bot?.activo;
    const botDespues = estadoFinal?.bot?.activo;
    const botEstabaDormido = this.tieneCondicion(botAntes, 'ASLEEP');
    const botEstabaParalizado = this.tieneCondicion(botAntes, 'PARALYZED');
    const hpJugadorDespues = estadoFinal?.jugador?.activo?.hpActual || 0;
    const danioHecho = hpJugadorAntes - hpJugadorDespues;

    const puedeActuar =
      !botEstabaParalizado && (!botEstabaDormido || !this.tieneCondicion(botDespues, 'ASLEEP'));

    let botAtaco = false;
    if (puedeActuar) {
      if (danioHecho > 0) {
        botAtaco = true;
      } else if (botDespues?.card?.ataques?.length) {
        const ataqueBot = botDespues.card.ataques[0];
        const costoReq = ataqueBot.costo?.length || 0;
        const energiasBot = botDespues.energiasUnidas?.length || 0;
        botAtaco = energiasBot >= costoReq;
      }
    }

    return {
      botEstabaDormido,
      botEstabaParalizado,
      puedeActuar,
      botAtaco,
      hpJugadorDespues,
      danioHecho,
    };
  }

  // Estima cuántas caras reales produjo el ataque del bot según el daño/estado final.
  resolverCarasBot(config: CoinFlipConfig, estadoFinal: Partida, danioHecho: number): number {
    if (config.esSoloEstado) {
      const jugadorActivo = estadoFinal.jugador?.activo;
      const tieneEstado = ['Paralyzed', 'Asleep', 'Confused', 'Poisoned'].some((condicion) =>
        this.tieneCondicion(jugadorActivo, condicion),
      );
      return tieneEstado ? 1 : 0;
    }

    return danioHecho > 0 ? 1 : 0;
  }

  // Estima cuántas caras reales produjo el ataque del jugador según daño/estado final.
  resolverCarasJugador(
    config: CoinFlipConfig,
    habilidad: Ataque,
    estadoFinal: Partida,
    danioHecho: number,
  ): number {
    if (config.esSoloEstado) {
      const botActivo = estadoFinal.bot?.activo;
      const tieneEstado = ['Paralyzed', 'Asleep', 'Confused', 'Poisoned'].some((condicion) =>
        this.tieneCondicion(botActivo, condicion),
      );
      return tieneEstado ? 1 : 0;
    }

    const texto = (habilidad.texto || '').toLowerCase();
    if (texto.includes('does nothing')) {
      return danioHecho > 0 ? 1 : 0;
    }

    if (texto.includes('heads') && config.danioExtraPorCara > 0) {
      const base = texto.includes('more') || texto.includes('plus') ? config.danioBase : 0;
      return Math.min(
        config.cantidadMonedas,
        Math.round(Math.max(0, danioHecho - base) / config.danioExtraPorCara),
      );
    }

    return danioHecho > 0 ? 1 : 0;
  }

  // Decide visualmente si la siguiente moneda debe caer en cara sin romper la sincronía con backend.
  resolverSiguienteMoneda(
    carasForzadas: number,
    carasAsignadas: number,
    cantidadMonedas: number,
    indiceMoneda: number,
    randomFn: () => number = Math.random,
  ): boolean {
    const carasRestantes = carasForzadas - carasAsignadas;
    const monedasRestantes = cantidadMonedas - indiceMoneda;

    if (carasRestantes >= monedasRestantes) return true;
    if (carasRestantes <= 0) return false;

    return randomFn() < carasRestantes / monedasRestantes;
  }

  // Resume el resultado global mostrado por el overlay.
  obtenerResultadoMoneda(cantidadMonedas: number, carasAsignadas: number): CoinSide {
    if (cantidadMonedas <= 1) {
      return carasAsignadas > 0 ? 'CARA' : 'CRUZ';
    }

    return carasAsignadas > 0 ? 'CARA' : 'CRUZ';
  }

  // Calcula el daño extra mostrado en el overlay de monedas.
  calcularDanioMonedas(config: CoinFlipConfig, carasAsignadas: number): number {
    return carasAsignadas * config.danioExtraPorCara;
  }

  // Devuelve un delay corto para “humanizar” la decisión del bot.
  calcularTiempoPensamientoBot(randomFn: () => number = Math.random): number {
    return Math.floor(randomFn() * 1500) + 1000;
  }

  // Comprueba una condición especial sin depender de mayúsculas/minúsculas.
  private tieneCondicion(pokemon: CartaEnJuego | null | undefined, condicion: string): boolean {
    return (pokemon?.condicionesEspeciales || []).some(
      (actual) => actual.toUpperCase() === condicion.toUpperCase(),
    );
  }
}
