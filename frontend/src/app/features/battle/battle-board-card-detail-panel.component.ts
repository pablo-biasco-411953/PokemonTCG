import { CommonModule } from '@angular/common';
import { Component, Input } from '@angular/core';
import { SafeHtml } from '@angular/platform-browser';
import { BattleActionCard, CartaEnJuego } from '../../shared/models/battle';
import { Card } from '../../shared/models/card';
import { BattleBoardAttack, CardGlossaryEntry } from './battle-board.types';

@Component({
  selector: 'app-battle-board-card-detail-panel',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './battle-board-card-detail-panel.component.html',
  styleUrls: ['./battle-board-card-detail-panel.component.scss'],
})
export class BattleBoardCardDetailPanelComponent {
  @Input() hoveredCard: Card | BattleActionCard | null = null;
  @Input() hoveredInPlayCard: CartaEnJuego | null = null;
  @Input() hoveredCardStatuses: CardGlossaryEntry[] = [];
  @Input() mostrarHintMano = false;
  @Input() getImagenCarta: ((id: string) => string) | null = null;
  @Input() getEnergyColor: ((tipo: string) => string) | null = null;
  @Input() formatTextoAtaque: ((texto: string) => SafeHtml) | null = null;

  asAttackList(ataques?: BattleBoardAttack[]): BattleBoardAttack[] {
    return ataques ?? [];
  }

  resolveImage(cardId: string): string {
    return this.getImagenCarta ? this.getImagenCarta(cardId) : '';
  }

  resolveEnergyColor(tipo: string): string {
    return this.getEnergyColor ? this.getEnergyColor(tipo) : '#cccccc';
  }

  resolveAttackText(texto: string): SafeHtml | string {
    return this.formatTextoAtaque ? this.formatTextoAtaque(texto) : texto;
  }

  getDisplayedHp(card: Card | BattleActionCard): string {
    return card.hp ?? '';
  }

  getCostoRetirada(card: Card | BattleActionCard): number | null {
    return 'costoRetirada' in card && typeof card.costoRetirada === 'number'
      ? card.costoRetirada
      : null;
  }

  getAttachedEnergies(): Card[] {
    return this.hoveredInPlayCard?.energiasUnidas ?? [];
  }

  getAttachedStatuses(): string[] {
    const raw = this.hoveredInPlayCard?.condicionesEspeciales ?? [];
    return Array.isArray(raw) ? raw : Array.from(raw as any);
  }

  getEvolutionInfo(card: Card | BattleActionCard): string | null {
    const evolvesFrom = 'evolvesFrom' in card ? card.evolvesFrom : null;
    return evolvesFrom ? `Evoluciona de ${evolvesFrom}` : null;
  }

  getCurrentHp(): number | null {
    return typeof this.hoveredInPlayCard?.hpActual === 'number' ? this.hoveredInPlayCard.hpActual : null;
  }
}
