// Debilidad o resistencia expresada por tipo.
export interface CardWeakness {
  tipo?: string;
  valor?: string;
  type?: string;
  value?: string;
}

// Forma simplificada de un ataque consumida por el frontend.
export interface Ataque {
  nombre: string;
  danio: number;
  texto: string;
  costo: string[];
  interactionType?: 'CHOOSE_STATUS' | 'CHOOSE_OPPONENT_ATTACK' | 'YES_NO_PROMPT' | 'CHOOSE_BENCHED_ENERGY_TARGETS' | null;
  interactionPrompt?: string | null;
}

// Modelo base de una carta recibido desde backend.
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
  rarity?: string;
}
