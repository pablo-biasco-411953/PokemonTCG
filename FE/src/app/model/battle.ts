import { Ataque, Card } from './card';

export interface CartaEnJuego {
  card: Card;
  hpActual: number;
  energiasUnidas: Card[];
  puedeAtacar: boolean;
  condicionesEspeciales: string[];
  invulnerable?: boolean;
}

export interface TableroJugador {
  mazo: Card[];
  mano: Card[];
  premios: Card[];
  activo: CartaEnJuego | null;
  banca: CartaEnJuego[];
  pilaDescarte: Card[];
}

export interface Partida {
  id: string;
  jugador: TableroJugador;
  bot: TableroJugador;
  turnoActual: 'JUGADOR' | 'BOT';
  faseActual: 'INICIO' | 'LANZAMIENTO_MONEDA' | 'TURNO_NORMAL' | 'FIN_PARTIDA';
  yaSeRetiroEsteTurno: boolean;
  ultimasMonedasLanzadas: boolean[];
}

export interface StartBattleResponse extends Partida {}

export interface BattleActionCard {
  id: string;
  nombre: string;
  tipo?: string;
  hp?: string;
  ataques?: Ataque[];
  supertype?: string;
  evolvesFrom?: string | null;
}
