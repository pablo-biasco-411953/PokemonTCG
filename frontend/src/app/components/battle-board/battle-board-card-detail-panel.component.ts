import { CommonModule } from '@angular/common';
import { Component, Input } from '@angular/core';
import { SafeHtml } from '@angular/platform-browser';
import { BattleActionCard } from '../../model/battle';
import { Card } from '../../model/card';
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
}
