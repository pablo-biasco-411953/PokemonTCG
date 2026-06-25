import { CommonModule } from '@angular/common';
import { Component, Input, inject } from '@angular/core';
import { SafeHtml } from '@angular/platform-browser';
import { BattleActionCard, CartaEnJuego } from '../../shared/models/battle';
import { Card } from '../../shared/models/card';
import { BattleBoardAttack, CardGlossaryEntry } from './battle-board.types';
import { CardService } from '../../core/services/card.service';

@Component({
  selector: 'app-battle-board-card-detail-panel',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './battle-board-card-detail-panel.component.html',
  styleUrls: ['./battle-board-card-detail-panel.component.scss'],
})
export class BattleBoardCardDetailPanelComponent {
  private cardService = inject(CardService);

  @Input() hoveredCard: Card | BattleActionCard | null = null;
  @Input() hoveredInPlayCard: CartaEnJuego | null = null;
  @Input() hoveredCardStatuses: CardGlossaryEntry[] = [];
  @Input() mostrarHintMano = false;
  @Input() getImagenCarta: ((id: string) => string) | null = null;
  @Input() getEnergyColor: ((tipo: string) => string) | null = null;
  @Input() formatTextoAtaque: ((texto: string) => SafeHtml) | null = null;

  onCardImageError(event: Event): void {
    const img = event.target as HTMLImageElement;
    this.cardService.handleCardImageError(img);
  }

  asAttackList(ataques?: BattleBoardAttack[]): BattleBoardAttack[] {
    return ataques ?? [];
  }

  resolveImage(cardId: string): string {
    return this.getImagenCarta ? this.getImagenCarta(cardId) : '';
  }

  resolveEnergyColor(tipo: string): string {
    return this.getEnergyColor ? this.getEnergyColor(tipo) : '#cccccc';
  }

  resolveEnergyLabel(tipo: string): string {
    const labels: Record<string, string> = {
      Grass: 'PLA', Fire: 'FUE', Water: 'AGU', Lightning: 'ELE', Psychic: 'PSI',
      Fighting: 'LUC', Darkness: 'SIN', Metal: 'MET', Fairy: 'HAD',
      Dragon: 'DRA', Colorless: 'INC'
    };
    return labels[tipo] || tipo.slice(0, 2).toUpperCase();
  }

  resolveEnergyName(tipo: string): string {
    const names: Record<string, string> = {
      Grass: 'Energia Planta', Fire: 'Energia Fuego', Water: 'Energia Agua',
      Lightning: 'Energia Electrica', Psychic: 'Energia Psiquica', Fighting: 'Energia Lucha',
      Darkness: 'Energia Siniestra', Metal: 'Energia Metal', Fairy: 'Energia Hada',
      Dragon: 'Energia Dragon', Colorless: 'Energia Incolora: se paga con cualquier tipo'
    };
    return names[tipo] || tipo;
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
