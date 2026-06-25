import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, Output, inject } from '@angular/core';
import { Card } from '../../shared/models/card';
import { CardService } from '../../core/services/card.service';

@Component({
  selector: 'app-battle-board-discard-modal',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './battle-board-discard-modal.component.html',
  styleUrls: ['./battle-board-discard-modal.component.scss'],
})
export class BattleBoardDiscardModalComponent {
  private cardService = inject(CardService);

  @Input() visible = false;
  @Input() titulo = '';
  @Input() cartas: Card[] = [];
  @Input() getImagenCarta: ((id: string) => string) | null = null;

  @Output() close = new EventEmitter<void>();

  onCardImageError(event: Event): void {
    const img = event.target as HTMLImageElement;
    this.cardService.handleCardImageError(img);
  }

  resolveImage(cardId: string): string {
    return this.getImagenCarta ? this.getImagenCarta(cardId) : '';
  }
}
