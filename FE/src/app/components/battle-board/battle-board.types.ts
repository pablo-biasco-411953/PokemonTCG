import { Ataque, Card } from '../../model/card';
import { BattleActionCard, CartaEnJuego } from '../../model/battle';

export type CoinSide = '' | 'CARA' | 'CRUZ';
export type BattleBoardSide = 'jugador' | 'bot';
export type OverlaySide = BattleBoardSide | 'neutral';
export type CoinFaceState = 'girando' | 'cara' | 'cruz';

export interface DamageNumberState {
  valor: number;
  esCuracion: boolean;
}

export interface CoinVisualState {
  estado: CoinFaceState;
}

export interface AttackCoinFlipState {
  nombreAtaque: string;
  descripcion: string;
  cantidadMonedas: number;
  danioBase: number;
  danioExtraPorCara: number;
  monedas: CoinVisualState[];
  danioTotal: number;
  terminado: boolean;
  progreso: number;
  esSoloEstado: boolean;
}

export interface InterTurnOverlayState {
  titulo: string;
  subtitulo?: string;
  fase: string;
  tipo: OverlaySide;
  duracion: number;
}

export interface ParticleVisualState {
  color: string;
  tx: number;
  ty: number;
  size: number;
  duracion: number;
}

export type HoveredBattleCard = Card | BattleActionCard | CartaEnJuego;

export interface CardGlossaryEntry {
  nombre: string;
  desc: string;
  css: string;
}

export type BattleBoardAttack = Ataque & {
  dano?: number | string;
};
