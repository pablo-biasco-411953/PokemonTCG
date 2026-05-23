import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, Output } from '@angular/core';
import { Card } from '../../shared/models/card';

@Component({
  selector: 'app-battle-board-discard-modal',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './battle-board-discard-modal.component.html',
  styleUrls: ['./battle-board-discard-modal.component.scss'],
})
export class BattleBoardDiscardModalComponent {
  @Input() visible = false;
  @Input() titulo = '';
  @Input() cartas: Card[] = [];
  @Input() getImagenCarta: ((id: string) => string) | null = null;

  @Output() close = new EventEmitter<void>();

  resolveImage(cardId: string): string {
    return this.getImagenCarta ? this.getImagenCarta(cardId) : '';
  }
}
