import { Injectable } from '@angular/core';
import { Card } from '../model/card';
import { BattleActionCard, CartaEnJuego, Partida } from '../model/battle';

@Injectable({ providedIn: 'root' })
export class BattleBoardStateService {
  // Clona un snapshot simple del estado para comparaciones visuales.
  clonarPartida(partida: Partida | null): Partida | null {
    return partida ? JSON.parse(JSON.stringify(partida)) : null;
  }

  // Comprueba condiciones especiales sin importar mayúsculas/minúsculas.
  tieneCondicion(pokemon: CartaEnJuego | null | undefined, condicion: string): boolean {
    return (pokemon?.condicionesEspeciales || []).some(
      (actual) => actual.toUpperCase() === condicion.toUpperCase(),
    );
  }

  // Busca a qué Pokémon del tablero puede evolucionar una carta de la mano.
  buscarObjetivoEvolucion(
    partida: Partida | null,
    cartaMano: BattleActionCard | Card | null | undefined,
  ): CartaEnJuego | null {
    if (!partida || !cartaMano?.evolvesFrom) return null;

    if (partida.jugador?.activo?.card?.nombre === cartaMano.evolvesFrom) {
      return partida.jugador.activo;
    }

    return (
      partida.jugador?.banca?.find((pokemon) => pokemon.card.nombre === cartaMano.evolvesFrom) ||
      null
    );
  }

  // Indica si una carta de la mano puede evolucionar algo en mesa.
  puedeEvolucionar(
    partida: Partida | null,
    cartaMano: BattleActionCard | Card | null | undefined,
  ): boolean {
    return !!this.buscarObjetivoEvolucion(partida, cartaMano);
  }

  // Devuelve una carta puntual de la mano.
  buscarCartaEnMano(partida: Partida | null, cartaId: string): Card | undefined {
    return partida?.jugador?.mano.find((carta) => carta.id === cartaId);
  }
}
