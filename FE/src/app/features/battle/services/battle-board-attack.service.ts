import { Injectable } from '@angular/core';
import { CartaEnJuego } from '../../../shared/models/battle';

// Configuracion visual de un ataque que depende de monedas.
export interface CoinFlipConfig {
  cantidadMonedas: number;
  danioBase: number;
  danioExtraPorCara: number;
  descripcion: string;
  esSoloEstado: boolean;
  tipoEfecto?: 'damage' | 'status' | 'protection' | 'self-damage' | 'discard' | 'switch' | 'search' | 'restriction' | 'other';
}

@Injectable({ providedIn: 'root' })
export class BattleBoardAttackService {
  // Detecta si un ataque necesita una animacion de moneda y resume su efecto.
  detectarCoinFlipAtaque(
    ataque: any,
    traducirDescripcion: (
      texto: string,
      cantidadMonedas: number,
      danioExtraPorCara: number,
      esMultiplicador: boolean,
      esFalloCruz: boolean,
      esSoloEstado: boolean,
    ) => string,
    activo?: CartaEnJuego | null,
  ): CoinFlipConfig | null {
    if (!ataque?.texto && !ataque?.descripcion && !ataque?.efecto) return null;

    const texto: string = (ataque.texto || ataque.descripcion || ataque.efecto || '').toLowerCase();
    const flipMatch = texto.match(/flip\s+(\d+|a|an|one|two|three|four|five)\s+coin|lanz[aá]\s+(\d+|una?)\s+moneda/i);
    if (!flipMatch) return null;

    const numStr = (flipMatch[1] || flipMatch[2] || 'a').toLowerCase();
    const numMap: Record<string, number> = {
      a: 1, an: 1, one: 1, una: 1, un: 1, '1': 1,
      two: 2, dos: 2, '2': 2,
      three: 3, tres: 3, '3': 3,
      four: 4, cuatro: 4, '4': 4,
      five: 5, cinco: 5, '5': 5
    };
    let cantidadMonedas = (numMap[numStr] ?? parseInt(numStr, 10)) || 1;

    let danioBase = parseInt(ataque.danio || ataque.dano || '0', 10) || 0;
    let danioExtraPorCara = 0;
    let esMultiplicador = false;
    let esFalloCruz = false;
    let esSoloEstado = false;
    let tipoEfecto: CoinFlipConfig['tipoEfecto'] = 'other';

    if (texto.includes('paralyzed') || texto.includes('asleep') || texto.includes('confused') || texto.includes('poisoned')) {
      if (!texto.includes('more damage') && !texto.includes('damage times')) {
        esSoloEstado = true;
        tipoEfecto = 'status';
      }
    }

    const esRestriccionPorAtaque = texto.includes("tries to attack during your opponent's next turn")
      && texto.includes('that attack does nothing');

    if (esRestriccionPorAtaque) {
      tipoEfecto = 'restriction';
      esSoloEstado = true;
      esFalloCruz = true;
      danioBase = 0;
      danioExtraPorCara = 0;
    } else if (texto.includes('does nothing')) {
      tipoEfecto = 'damage';
      esFalloCruz = true;
      danioExtraPorCara = danioBase;
      danioBase = 0;
    } else if (texto.includes('times the number of heads') || texto.includes('x the number of heads') || texto.includes('for each heads')) {
      tipoEfecto = 'damage';
      esMultiplicador = true;
      const multiMatch = texto.match(/does (\d+) damage times/i);
      danioExtraPorCara = multiMatch ? parseInt(multiMatch[1], 10) : (danioBase > 0 ? danioBase : 10);
      danioBase = 0;
    } else if (texto.includes('more damage') || texto.includes('additional damage')) {
      tipoEfecto = 'damage';
      const damageMatch = texto.match(/(\d+)\s*(?:more|extra|additional)/i);
      danioExtraPorCara = damageMatch ? parseInt(damageMatch[1], 10) : 10;
    }

    if (texto.includes('prevent all effects of attacks') || texto.includes('prevent all damage done')) {
      tipoEfecto = 'protection';
      esSoloEstado = true;
    } else if (texto.includes('damage to itself')) {
      tipoEfecto = 'self-damage';
      esSoloEstado = true;
    } else if (texto.includes('discard') && texto.includes('energy')) {
      tipoEfecto = 'discard';
      esSoloEstado = true;
    } else if (texto.includes('switch this pok')) {
      tipoEfecto = 'switch';
      esSoloEstado = true;
    } else if (texto.includes('search your deck')) {
      tipoEfecto = 'search';
      esSoloEstado = true;
    } else if (esRestriccionPorAtaque || texto.includes("can't attack") || texto.includes("can't play any supporter")) {
      tipoEfecto = 'restriction';
      esSoloEstado = true;
    }

    cantidadMonedas = this.resolverCantidadMonedasDinamica(texto, cantidadMonedas, activo);

    const descripcion = traducirDescripcion(
      texto,
      cantidadMonedas,
      danioExtraPorCara,
      esMultiplicador,
      esFalloCruz,
      esSoloEstado
    );

    return { cantidadMonedas, danioBase, danioExtraPorCara, descripcion, esSoloEstado, tipoEfecto };
  }

  // Comprueba si el activo puede pagar el costo de un ataque.
  validarEnergiaAtaque(ataque: any, activo: any): boolean {
    if (!ataque || !activo) return false;
    return this.resolverCosto(ataque, activo).every(Boolean);
  }

  // Marca energia por energia cuales requisitos ya estan cubiertos.
  getCheckEnergiasAtaque(ataque: any, activo: any): any[] {
    const resueltos = this.resolverCosto(ataque, activo);
    return (ataque?.costo || []).map((tipo: string, index: number) => ({
      tipo,
      cumplido: resueltos[index],
    }));
  }

  // Resume las energias que todavia faltan para atacar.
  getFaltantesAtaque(ataque: any, activo: any): any[] {
    const costo = [...(ataque?.costo || [])];
    const resueltos = this.resolverCosto(ataque, activo);
    const faltantesMap: { [key: string]: number } = {};
    costo.forEach((tipo: string, index: number) => {
      if (!resueltos[index]) {
        faltantesMap[tipo] = (faltantesMap[tipo] || 0) + 1;
      }
    });

    return Object.keys(faltantesMap).map(tipo => ({ tipo, cantidad: faltantesMap[tipo] }));
  }

  private resolverCosto(ataque: any, activo: any): boolean[] {
    const costo = [...(ataque?.costo || [])];
    const resultado = costo.map(() => false);
    const unidades: Array<{ tipo: string; wildcard: boolean }> = (activo?.energiasUnidas || []).flatMap((energia: any) =>
      this.obtenerUnidadesEnergia(energia),
    );

    costo.forEach((tipo: string, index: number) => {
      const requerido = this.normalizarTipo(tipo);
      if (requerido === 'Colorless') return;
      let unidad = unidades.findIndex((actual) => !actual.wildcard && actual.tipo === requerido);
      if (unidad < 0) unidad = unidades.findIndex((actual) => actual.wildcard);
      if (unidad >= 0) {
        unidades.splice(unidad, 1);
        resultado[index] = true;
      }
    });

    costo.forEach((tipo: string, index: number) => {
      if (this.normalizarTipo(tipo) !== 'Colorless' || resultado[index]) return;
      if (unidades.length > 0) {
        unidades.splice(0, 1);
        resultado[index] = true;
      }
    });
    return resultado;
  }

  private obtenerUnidadesEnergia(energia: any): Array<{ tipo: string; wildcard: boolean }> {
    const nombre = (energia?.nombre || '').toLowerCase();
    if (nombre.includes('double colorless') || nombre.includes('doble incolora')) {
      return [
        { tipo: 'Colorless', wildcard: false },
        { tipo: 'Colorless', wildcard: false },
      ];
    }
    if (nombre.includes('rainbow') || nombre.includes('arcoiris')) {
      return [{ tipo: 'Colorless', wildcard: true }];
    }
    const texto = energia?.tipo === 'Energy' || !energia?.tipo ? energia?.nombre : energia?.tipo;
    return [{ tipo: this.normalizarTipo(texto), wildcard: false }];
  }

  // Lleva nombres mezclados a un tipo canonico del juego.
  private normalizarTipo(tipo: string): string {
    const t = (tipo || '').toLowerCase();
    if (t.includes('grass') || t.includes('planta')) return 'Grass';
    if (t.includes('fire') || t.includes('fuego')) return 'Fire';
    if (t.includes('water') || t.includes('agua')) return 'Water';
    if (t.includes('lightning') || t.includes('eléctrica') || t.includes('electrica')) return 'Lightning';
    if (t.includes('psychic') || t.includes('psíquica') || t.includes('psiquica')) return 'Psychic';
    if (t.includes('fighting') || t.includes('lucha')) return 'Fighting';
    if (t.includes('darkness') || t.includes('siniestra') || t.includes('oscuridad')) return 'Darkness';
    if (t.includes('metal') || t.includes('acero')) return 'Metal';
    if (t.includes('dragon') || t.includes('dragón') || t.includes('dragon')) return 'Dragon';
    if (t.includes('fairy') || t.includes('hada')) return 'Fairy';
    if (t.includes('colorless') || t.includes('incolora')) return 'Colorless';
    return tipo;
  }

  private resolverCantidadMonedasDinamica(
    texto: string,
    cantidadBase: number,
    activo?: CartaEnJuego | null,
  ): number {
    if (!activo?.card) {
      return cantidadBase;
    }

    if (texto.includes('for each damage counter on this pok')) {
      const maxHp = parseInt(activo.card.hp || '0', 10) || activo.hpActual || 0;
      return Math.max(0, Math.floor((maxHp - (activo.hpActual ?? maxHp)) / 10));
    }

    if (texto.includes('for each energy attached to this pok')) {
      const specificEnergyMatch = texto.match(/for each ([a-z]+) energy attached to this pok/i);
      if (specificEnergyMatch?.[1]) {
        const expected = this.normalizarTipo(specificEnergyMatch[1]);
        return (activo.energiasUnidas || []).filter((energia) => {
          if (!energia) return false;
          const source = energia.tipo === 'Energy' || !energia.tipo ? energia.nombre : energia.tipo;
          return this.normalizarTipo(source).toLowerCase() === expected.toLowerCase();
        }).length;
      }
      return (activo.energiasUnidas || []).length;
    }

    return cantidadBase;
  }
}
