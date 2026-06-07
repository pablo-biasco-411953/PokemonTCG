import { Ataque, Card } from './card';

// Pokemon ya puesto en juego con su estado actual.
export interface CartaEnJuego {
  card: Card;
  hpActual: number;
  energiasUnidas: Card[];
  puedeAtacar: boolean;
  condicionesEspeciales: string[];
  invulnerable?: boolean;
}

// Zonas visibles de un jugador durante la partida.
export interface TableroJugador {
  mazo: Card[];
  mano: Card[];
  premios: Card[];
  activo: CartaEnJuego | null;
  banca: CartaEnJuego[];
  pilaDescarte: Card[];
}

// Estado completo de una partida sincronizado con backend.
export interface Partida {
  id: string;
  jugador: TableroJugador;
  bot: TableroJugador;
  turnoActual: 'JUGADOR' | 'BOT';
  faseActual: 'INICIO' | 'LANZAMIENTO_MONEDA' | 'TURNO_NORMAL' | 'FIN_PARTIDA';
  yaSeRetiroEsteTurno: boolean;
  ultimasMonedasLanzadas: boolean[];
  jugadorUsername?: string;
  botUsername?: string;
  ganador?: string;
  razonFinPartida?: string;
  coinFlipped?: boolean;
  coinFlipWinner?: string;
  coinFlipResult?: string;
  coinFlipCallerUsername?: string;
  coinHandshakeJugadorPower?: number;
  coinHandshakeBotPower?: number;
  coinHandshakeJugadorHolding?: boolean;
  coinHandshakeBotHolding?: boolean;
  coinHandshakeComplete?: boolean;
  turnLogs?: string[];
}

export interface StartBattleResponse extends Partida {}

// Vista simplificada de carta para acciones de batalla.
export interface BattleActionCard {
  id: string;
  nombre: string;
  tipo?: string;
  hp?: string;
  ataques?: Ataque[];
  supertype?: string;
  evolvesFrom?: string | null;
}
