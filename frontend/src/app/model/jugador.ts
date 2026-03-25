// En jugador.ts
export interface Jugador {
  id?: number;         // El ? lo hace opcional
  username: string;
  sobresDisponibles: number;
  coleccion?: any[];   // También opcional por las dudas
}