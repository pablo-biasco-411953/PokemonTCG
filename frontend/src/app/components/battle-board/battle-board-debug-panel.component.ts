import { CommonModule } from '@angular/common';
import { CdkDrag, CdkDragHandle } from '@angular/cdk/drag-drop';
import { Component, EventEmitter, Input, Output } from '@angular/core';
import { Card } from '../../model/card';
import { Partida } from '../../model/battle';

@Component({
  selector: 'app-battle-board-debug-panel',
  standalone: true,
  imports: [CommonModule, CdkDrag, CdkDragHandle],
  templateUrl: './battle-board-debug-panel.component.html',
  styleUrls: ['./battle-board-debug-panel.component.scss'],
})
export class BattleBoardDebugPanelComponent {
  @Input() show = false;
  @Input() fps = 0;
  @Input() memoryUsage = 'N/A';
  @Input() matchId: string | null = null;
  @Input() partida: Partida | null = null;
  @Input() cargandoAccion = false;
  @Input() bloqueadoPorAnimacion = false;
  @Input() estadoCoinFlip = 'OCULTO';
  @Input() debugFilteredCatalog: Card[] = [];
  @Input() debugSelectedIndex = 0;
  @Input() debugSelectedCard: Card | null = null;
  @Input() getImagenCarta: ((id: string) => string) | null = null;

  @Output() searchTextChange = new EventEmitter<string>();
  @Output() searchSupertypeChange = new EventEmitter<string>();
  @Output() nextCard = new EventEmitter<void>();
  @Output() prevCard = new EventEmitter<void>();
  @Output() drawCard = new EventEmitter<string>();
  @Output() forceState = new EventEmitter<{ objetivo: 'JUGADOR' | 'BOT'; estado: string }>();
  @Output() setHp = new EventEmitter<{ objetivo: 'JUGADOR' | 'BOT'; hp: number }>();

  readonly estadosEspeciales = ['ASLEEP', 'PARALYZED', 'POISONED', 'CONFUSED'];
  selectedStatus = 'ASLEEP';

  emitDrawCard(): void {
    if (this.debugSelectedCard?.id) {
      this.drawCard.emit(this.debugSelectedCard.id);
    }
  }

  emitForceState(objetivo: 'JUGADOR' | 'BOT'): void {
    this.forceState.emit({ objetivo, estado: this.selectedStatus });
  }

  emitSetHp(objetivo: 'JUGADOR' | 'BOT', hp: number): void {
    this.setHp.emit({ objetivo, hp });
  }

  resolveImage(cardId: string): string {
    return this.getImagenCarta ? this.getImagenCarta(cardId) : '';
  }

  getHpActivo(quien: 'JUGADOR' | 'BOT'): number | 'KO' {
    const activo = quien === 'JUGADOR' ? this.partida?.jugador.activo : this.partida?.bot.activo;
    return activo?.hpActual ?? 'KO';
  }
}
