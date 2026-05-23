import { Card } from './card';

// Resumen minimo del dueño de un mazo.
export interface MazoJugadorResumen {
  id: number;
  username: string;
  sobresDisponibles: number;
}

// Modelo de mazo usado por lobby y deck builder.
export interface Mazo {
  id: number;
  nombre: string;
  cartas: Card[];
  jugador?: MazoJugadorResumen;
}
