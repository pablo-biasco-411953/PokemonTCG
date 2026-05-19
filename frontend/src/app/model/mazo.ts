import { Card } from './card';

export interface MazoJugadorResumen {
  id: number;
  username: string;
  sobresDisponibles: number;
}

export interface Mazo {
  id: number;
  nombre: string;
  cartas: Card[];
  jugador?: MazoJugadorResumen;
}
