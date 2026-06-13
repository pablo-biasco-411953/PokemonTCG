import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, Output } from '@angular/core';
import { BattleBoardAttack } from './battle-board.types';

@Component({
  selector: 'app-battle-board-abilities-panel',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './battle-board-abilities-panel.component.html',
  styleUrls: ['./battle-board-abilities-panel.component.scss'],
})
export class BattleBoardAbilitiesPanelComponent {
  @Input() visible = false;
  @Input() ataques: BattleBoardAttack[] = [];
  @Input() retiroCoste = 0;
  @Input() retiroBloqueado = false;
  @Input() validarEnergiaAtaque: ((habilidad: BattleBoardAttack) => boolean) | null = null;
  @Input() getCheckEnergiasAtaque:
    | ((habilidad: BattleBoardAttack) => { tipo: string; cumplido?: boolean }[])
    | null = null;
  @Input() getEnergyColor: ((tipo: string) => string) | null = null;

  @Output() close = new EventEmitter<void>();
  @Output() attackSelected = new EventEmitter<BattleBoardAttack>();
  @Output() retreatSelected = new EventEmitter<void>();

  puedeUsarAtaque(habilidad: BattleBoardAttack): boolean {
    return this.validarEnergiaAtaque ? this.validarEnergiaAtaque(habilidad) : false;
  }

  getChecks(habilidad: BattleBoardAttack): { tipo: string; cumplido?: boolean }[] {
    return this.getCheckEnergiasAtaque ? this.getCheckEnergiasAtaque(habilidad) : [];
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

  cerrar(event: MouseEvent): void {
    event.stopPropagation();
    this.close.emit();
  }

  seleccionarAtaque(habilidad: BattleBoardAttack, event: MouseEvent): void {
    event.stopPropagation();
    this.attackSelected.emit(habilidad);
  }

  seleccionarRetirada(event: MouseEvent): void {
    event.stopPropagation();
    this.retreatSelected.emit();
  }
}
