import { Card } from './card';

export interface Jugador {
  id?: number;
  username: string;
  sobresDisponibles: number;
  cantidadCartas?: number;
  nivel?: number;
  coleccion?: Card[];
  cartasObtenidas?: Card[];
}

export interface JugadorDatosResponse {
  username: string;
  sobresDisponibles: number;
  cantidadCartas: number;
  cartasObtenidas?: Card[];
}
