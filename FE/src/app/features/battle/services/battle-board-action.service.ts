import { Injectable } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { BattleService } from './battle.service';
import { BattleBoardStateService } from './battle-board-state.service';
import { BattleActionCard, CartaEnJuego, Partida } from '../../../shared/models/battle';
import { Card } from '../../../shared/models/card';
import { I18nService } from '../../../i18n/i18n.service';

export type CardActionType =
  | 'unir-energia'
  | 'evolucionar'
  | 'requiere-promocion'
  | 'bajar-pokemon'
  | 'jugar-trainer'
  | 'sin-accion';

export interface CardActionDecision {
  tipo: CardActionType;
  target?: CartaEnJuego | null;
  mensaje?: string;
}

@Injectable({ providedIn: 'root' })
export class BattleBoardActionService {
  constructor(
    private battleService: BattleService,
    private battleBoardState: BattleBoardStateService,
    private i18n: I18nService,
  ) {}

  // Decide qué acción debería ejecutar el jugador al intentar usar una carta desde la mano.
  resolverAccionCarta(
    partida: Partida | null,
    carta: BattleActionCard | Card | null | undefined,
  ): CardActionDecision {
    if (!partida || !carta) return { tipo: 'sin-accion' };

    if (carta.supertype === 'Trainer') {
      return { tipo: 'jugar-trainer' };
    }

    if (this.esEnergia(carta)) {
      return { tipo: 'unir-energia' };
    }

    if (this.esPokemon(carta)) {
      if (carta.evolvesFrom) {
        const target = this.battleBoardState.buscarObjetivoEvolucion(partida, carta);
        if (target) {
          return { tipo: 'evolucionar', target };
        }
      }

      if (!partida.jugador.activo && partida.jugador.banca.length > 0) {
        return {
          tipo: 'requiere-promocion',
          mensaje: this.i18n.translate('alert.promoteFirst'),
        };
      }

      return { tipo: 'bajar-pokemon' };
    }

    return { tipo: 'sin-accion' };
  }

  // Indica si el activo actual tiene energía suficiente para retirarse.
  puedePagarRetiro(activo: CartaEnJuego | null | undefined, activeStadium?: any): boolean {
    if (!activo) return false;
    const disponible = activo.energiasUnidas.reduce((total, energia) => {
      const nombre = (energia.nombre || '').toLowerCase();
      return total + (nombre.includes('double colorless') || nombre.includes('incolora doble') || nombre.includes('doble incolora') ? 2 : 1);
    }, 0);
    
    // Apply Fairy Garden
    const isFairyGardenActive = activeStadium?.id === 'xy1-117' || activeStadium?.nombre?.toLowerCase() === 'fairy garden';
    const hasFairyEnergy = activo.energiasUnidas?.some(e => e.tipo?.toLowerCase() === 'fairy' || e.nombre?.toLowerCase().includes('fairy energy'));
    if (isFairyGardenActive && hasFairyEnergy) {
      return true;
    }
    
    return disponible >= (activo.card.costoRetirada ?? 0);
  }

  // Construye el texto de confirmación al retirar un Pokémon activo.
  construirMensajeRetirada(activo: CartaEnJuego, activeStadium?: any): string {
    let costo = activo.card.costoRetirada ?? 0;
    const isFairyGardenActive = activeStadium?.id === 'xy1-117' || activeStadium?.nombre?.toLowerCase() === 'fairy garden';
    const hasFairyEnergy = activo.energiasUnidas?.some(e => e.tipo?.toLowerCase() === 'fairy' || e.nombre?.toLowerCase().includes('fairy energy'));
    if (isFairyGardenActive && hasFairyEnergy) {
      costo = 0;
    }
    return this.i18n.translate('confirm.retreat', { name: activo.card.nombre, cost: costo.toString() });
  }

  // Ejecuta la evolución y devuelve un estado fresco de backend.
  async evolucionarYRecargar(
    matchId: string,
    cartaEvolucionId: string,
    targetId: string,
  ): Promise<Partida> {
    await firstValueFrom(this.battleService.evolucionar(matchId, cartaEvolucionId, targetId));
    return await firstValueFrom(this.battleService.getState(matchId));
  }

  // Juega un Pokémon desde la mano y devuelve el nuevo estado.
  async jugarPokemonYRecargar(matchId: string, cartaId: string): Promise<Partida> {
    await firstValueFrom(this.battleService.jugarPokemon(matchId, cartaId));
    return await firstValueFrom(this.battleService.getState(matchId));
  }

  // Juega una carta de Entrenador y devuelve el nuevo estado.
  async jugarTrainerYRecargar(matchId: string, cartaId: string): Promise<Partida> {
    await firstValueFrom(this.battleService.jugarTrainer(matchId, cartaId));
    return await firstValueFrom(this.battleService.getState(matchId));
  }

  // Une una energía al activo indicado y devuelve el nuevo estado.
  async unirEnergiaYRecargar(
    matchId: string,
    activoId: string,
    energiaId: string,
    selectedType?: string
  ): Promise<Partida> {
    await firstValueFrom(this.battleService.unirEnergia(matchId, activoId, energiaId, selectedType));
    return await firstValueFrom(this.battleService.getState(matchId));
  }

  // Promueve un Pokémon de la banca y devuelve el nuevo estado.
  async subirAActivoYRecargar(matchId: string, cartaId: string): Promise<Partida> {
    await firstValueFrom(this.battleService.subirAActivo(matchId, cartaId));
    return await firstValueFrom(this.battleService.getState(matchId));
  }

  // Realiza la retirada y devuelve el nuevo estado.
  async retirarPokemonYRecargar(matchId: string, nuevoActivoId: string): Promise<Partida> {
    await firstValueFrom(this.battleService.retirarPokemon(matchId, nuevoActivoId));
    return await firstValueFrom(this.battleService.getState(matchId));
  }

  // Detecta si una carta se comporta como energía en el battle board.
  private esEnergia(carta: BattleActionCard | Card): boolean {
    return carta?.supertype === 'Energy';
  }

  // Detecta si una carta se comporta como Pokémon en el battle board.
  private esPokemon(carta: BattleActionCard | Card): boolean {
    return carta?.supertype === 'Pokémon' || carta?.supertype === 'Pokemon';
  }
}
