import { Ataque, Card } from './card';

// Pokemon ya puesto en juego con su estado actual.
export interface CartaEnJuego {
  card: Card;
  hpActual: number;
  energiasUnidas: Card[];
  puedeAtacar: boolean;
  condicionesEspeciales: string[];
  invulnerable?: boolean;
  reduccionDanioRecibido?: number;
  aumentoDanioCausado?: number;
  bocaAbajo?: boolean;
  debeLanzarMonedaSiAtaca?: boolean;
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
  faseActual: 'INICIO' | 'LANZAMIENTO_MONEDA' | 'SETUP_INITIAL_DRAW' | 'SETUP_MULLIGAN_EVALUATION' | 'SETUP_MULLIGAN_REVEAL' | 'SETUP_PLACE_ACTIVE' | 'SETUP_PLACE_BENCH' | 'SETUP_PRIZE_PLACEMENT' | 'SETUP_MULLIGAN_EXTRA_DRAW' | 'SETUP_PLACE_BENCH_EXTRA' | 'SETUP_REVEAL' | 'ESPERANDO_INTERACCION' | 'TURNO_NORMAL' | 'FIN_PARTIDA';
  numeroTurno?: number;
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
  lastCoinFlipEventId?: number;
  lastCoinFlipAttackName?: string;
  lastCoinFlipActor?: string;
  jugadorLoadingPercentage?: number;
  botLoadingPercentage?: number;
  setupJugadorListo?: boolean;
  setupBotListo?: boolean;
  mulligansJugador?: number;
  mulligansBot?: number;
  cartasMulliganExtraPendientesJugador?: number;
  cartasMulliganExtraPendientesBot?: number;
  setupJugadorRoboExtraMulligan?: boolean;
  setupBotRoboExtraMulligan?: boolean;
  pendingAction?: PendingBattleAction | null;
}

export interface PendingBattleAction {
  actor: string;
  type: string;
  prompt: string;
  destination: string;
  minSelections: number;
  maxSelections: number;
  options: Array<{ id: string; nombre: string; imagen?: string }>;
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
