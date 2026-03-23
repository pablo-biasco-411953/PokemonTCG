export interface Mazo {
  id: number;
  nombre: string;
  jugador: any; // Referencia al jugador
  cartas: any[]; // Referencia a las cartas del mazo
}
