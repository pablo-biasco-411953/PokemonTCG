export interface CardWeakness {
  tipo: string;
  valor?: string;
}

export interface Ataque {
  nombre: string;
  danio: number;
  texto: string;
  costo: string[];
}

export interface Card {
  id: string;
  nombre: string;
  tipo: string;
  hp: string;
  imagen: string;
  supertype?: string;
  evolvesFrom?: string | null;
  costoRetirada?: number;
  pokemonId?: number;
  attacks?: string;
  ataques?: Ataque[];
  subtypes?: string[];
  debilidades?: CardWeakness[];
  resistencias?: CardWeakness[];
}
