import { Injectable } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { BattleService } from './battle.service';
import { BattleBoardStateService } from './battle-board-state.service';
import { BattleActionCard, CartaEnJuego, Partida } from '../../../shared/models/battle';
import { Card } from '../../../shared/models/card';

export type CardActionType =
  | 'unir-energia'
  | 'evolucionar'
  | 'requiere-promocion'
  | 'bajar-pokemon'
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
  ) {}

  // Decide qué acción debería ejecutar el jugador al intentar usar una carta desde la mano.
  resolverAccionCarta(
    partida: Partida | null,
    carta: BattleActionCard | Card | null | undefined,
  ): CardActionDecision {
    if (!partida || !carta) return { tipo: 'sin-accion' };

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
          mensaje: 'Primero tenés que subir un Pokémon de tu banca al puesto activo!',
        };
      }

      return { tipo: 'bajar-pokemon' };
    }

    return { tipo: 'sin-accion' };
  }

  // Indica si el activo actual tiene energía suficiente para retirarse.
  puedePagarRetiro(activo: CartaEnJuego | null | undefined): boolean {
    if (!activo) return false;
    return activo.energiasUnidas.length >= (activo.card.costoRetirada ?? 0);
  }

  // Construye el texto de confirmación al retirar un Pokémon activo.
  construirMensajeRetirada(activo: CartaEnJuego): string {
    const costo = activo.card.costoRetirada ?? 0;
    return `Querés retirar a ${activo.card.nombre}? Costará ${costo} energía(s).`;
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

  // Une una energía al activo indicado y devuelve el nuevo estado.
  async unirEnergiaYRecargar(
    matchId: string,
    activoId: string,
    energiaId: string,
  ): Promise<Partida> {
    await firstValueFrom(this.battleService.unirEnergia(matchId, activoId, energiaId));
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
