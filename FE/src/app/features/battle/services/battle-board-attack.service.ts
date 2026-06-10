import { Injectable } from '@angular/core';

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
  detectarCoinFlipAtaque(ataque: any, traducirDescripcion: (texto: string, cantidadMonedas: number, danioExtraPorCara: number, esMultiplicador: boolean, esFalloCruz: boolean, esSoloEstado: boolean) => string): CoinFlipConfig | null {
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
    const cantidadMonedas = (numMap[numStr] ?? parseInt(numStr, 10)) || 1;

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

    if (texto.includes('does nothing')) {
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
    } else if (texto.includes("can't attack") || texto.includes("can't play any supporter")) {
      tipoEfecto = 'restriction';
      esSoloEstado = true;
    }

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

    const misEnergias = (activo.energiasUnidas || []).map((e: any) => {
      const texto = (e.tipo === 'Energy' || !e.tipo) ? e.nombre : e.tipo;
      return this.normalizarTipo(texto);
    });

    const costoRequerido = [...(ataque.costo || [])].map((t: string) => this.normalizarTipo(t));

    for (let i = costoRequerido.length - 1; i >= 0; i--) {
      const tipoReq = costoRequerido[i];
      if (tipoReq !== 'Colorless') {
        const index = misEnergias.indexOf(tipoReq);
        if (index !== -1) {
          misEnergias.splice(index, 1);
          costoRequerido.splice(i, 1);
        } else {
          return false;
        }
      }
    }

    return misEnergias.length >= costoRequerido.length;
  }

  // Marca energia por energia cuales requisitos ya estan cubiertos.
  getCheckEnergiasAtaque(ataque: any, activo: any): any[] {
    if (!activo?.energiasUnidas) return [];
    const poseidas = [...activo.energiasUnidas];
    const resultado: any[] = [];

    (ataque.costo || []).forEach((tipoRequerido: string) => {
      const index = poseidas.findIndex((e: any) => e.tipo === tipoRequerido || tipoRequerido === 'Colorless');
      if (index !== -1) {
        resultado.push({ tipo: tipoRequerido, cumplido: true });
        poseidas.splice(index, 1);
      } else {
        resultado.push({ tipo: tipoRequerido, cumplido: false });
      }
    });

    return resultado;
  }

  // Resume las energias que todavia faltan para atacar.
  getFaltantesAtaque(ataque: any, activo: any): any[] {
    if (!activo?.energiasUnidas) return [];

    const poseidas = [...activo.energiasUnidas];
    const costoRestante = [...(ataque.costo || [])];
    const faltantesMap: { [key: string]: number } = {};

    for (let i = costoRestante.length - 1; i >= 0; i--) {
      const tipoReq = costoRestante[i];
      if (tipoReq !== 'Colorless') {
        const index = poseidas.findIndex((p: any) => p.tipo === tipoReq);
        if (index !== -1) {
          poseidas.splice(index, 1);
          costoRestante.splice(i, 1);
        }
      }
    }

    for (let i = costoRestante.length - 1; i >= 0; i--) {
      if (costoRestante[i] === 'Colorless' && poseidas.length > 0) {
        poseidas.splice(0, 1);
        costoRestante.splice(i, 1);
      }
    }

    costoRestante.forEach((tipo: string) => {
      faltantesMap[tipo] = (faltantesMap[tipo] || 0) + 1;
    });

    return Object.keys(faltantesMap).map(tipo => ({ tipo, cantidad: faltantesMap[tipo] }));
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
}
