import { Injectable } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { Partida } from '../../../shared/models/battle';
import { CoinFlipConfig } from './battle-board-attack.service';
import { BattleService } from './battle.service';

@Injectable({ providedIn: 'root' })
export class BattleBoardCombatService {
  constructor(private battleService: BattleService) {}

  // Devuelve la configuración visual del check de confusión del jugador.
  crearConfigConfusion(): CoinFlipConfig {
    return {
      descripcion:
        'Tu Pokémon está confundido. Lanzá una moneda. Si sale cruz, te hacés 30 de daño y el ataque falla.',
      cantidadMonedas: 1,
      danioBase: 0,
      danioExtraPorCara: 0,
      esSoloEstado: true,
    };
  }

  // Resuelve el lanzamiento local del check de confusión.
  resolverConfusion(randomFn: () => number = Math.random): number {
    return randomFn() >= 0.5 ? 1 : 0;
  }

  // Ejecuta un ataque y luego devuelve el estado refrescado.
  async atacarYRecargar(matchId: string, nombreAtaque: string): Promise<Partida> {
    await firstValueFrom(this.battleService.atacar(matchId, nombreAtaque));
    return await firstValueFrom(this.battleService.getState(matchId));
  }

  // Pasa el turno y devuelve el estado refrescado.
  async pasarTurnoYRecargar(matchId: string): Promise<Partida> {
    await firstValueFrom(this.battleService.pasarTurno(matchId));
    return await firstValueFrom(this.battleService.getState(matchId));
  }

  // Pide al backend que ejecute el turno del bot.
  async ejecutarTurnoBot(matchId: string): Promise<Partida> {
    return await firstValueFrom(this.battleService.jugarBot(matchId));
  }

  // Calcula el daño real hecho comparando HP antes/después.
  calcularDanioHecho(hpAntes: number, hpDespues: number): number {
    return hpAntes - hpDespues;
  }
}
