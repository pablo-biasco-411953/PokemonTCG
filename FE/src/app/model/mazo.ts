import { Card } from './card';

export interface Mazo {
  id: number;
  nombre: string;
cartas: any[]; // Cambiá pokemonIds: number[] por esto  // El objeto jugador que viene en tu JSON
  jugador?: {
    id: number;
    username: string;
    sobresDisponibles: number;
  };
}