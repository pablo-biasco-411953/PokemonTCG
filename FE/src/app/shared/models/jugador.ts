import { Card } from './card';

// Estado principal del jugador dentro del frontend.
export interface Jugador {
  id?: number;
  username: string;
  sobresDisponibles: number;
  cantidadCartas?: number;
  nivel?: number;
  coleccion?: Card[];
  cartasObtenidas?: Card[];
  characterId?: string;
  skinColor?: string;
  hairColor?: string;
  eyeColor?: string;
  height?: number;
  pikachuCompanion?: boolean;
}

// Respuesta resumida para el header del lobby.
export interface JugadorDatosResponse {
  username: string;
  sobresDisponibles: number;
  cantidadCartas: number;
  cartasObtenidas?: Card[];
  characterId?: string;
  skinColor?: string;
  hairColor?: string;
  eyeColor?: string;
  height?: number;
  pikachuCompanion?: boolean;
}
