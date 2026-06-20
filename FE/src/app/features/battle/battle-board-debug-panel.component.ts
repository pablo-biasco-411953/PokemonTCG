import { CommonModule } from '@angular/common';
import { CdkDrag, CdkDragHandle } from '@angular/cdk/drag-drop';
import { Component, EventEmitter, Input, Output, ViewChild, ElementRef, OnInit, AfterViewInit, OnDestroy } from '@angular/core';
import { Card } from '../../shared/models/card';
import { Partida } from '../../shared/models/battle';

@Component({
  selector: 'app-battle-board-debug-panel',
  standalone: true,
  imports: [CommonModule, CdkDrag, CdkDragHandle],
  templateUrl: './battle-board-debug-panel.component.html',
  styleUrls: ['./battle-board-debug-panel.component.scss'],
})
export class BattleBoardDebugPanelComponent implements OnInit, AfterViewInit, OnDestroy {
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
  @Input() isAdmin = false;

  @Output() searchTextChange = new EventEmitter<string>();
  @Output() searchSupertypeChange = new EventEmitter<string>();
  @Output() nextCard = new EventEmitter<void>();
  @Output() prevCard = new EventEmitter<void>();
  @Output() drawCard = new EventEmitter<string>();
  @Output() forceState = new EventEmitter<{ objetivo: 'JUGADOR' | 'BOT'; estado: string }>();
  @Output() setHp = new EventEmitter<{ objetivo: 'JUGADOR' | 'BOT'; hp: number }>();

  @ViewChild('perfCanvas', { static: false }) perfCanvas?: ElementRef<HTMLCanvasElement>;

  readonly estadosEspeciales = ['ASLEEP', 'PARALYZED', 'POISONED', 'CONFUSED'];
  selectedStatus = 'ASLEEP';
  cpuUsage = 0;

  private perfHistory: Array<{ cpu: number; mem: number; fps: number }> = [];
  private maxHistoryLen = 40;
  private updateIntervalId: any = null;

  ngOnInit(): void {
    this.updateIntervalId = setInterval(() => {
      this.updatePerfHistory();
    }, 300);
  }

  ngAfterViewInit(): void {
    setTimeout(() => {
      this.drawChart();
    }, 100);
  }

  ngOnDestroy(): void {
    if (this.updateIntervalId) {
      clearInterval(this.updateIntervalId);
    }
  }

  updatePerfHistory(): void {
    let memVal = 30.5;
    if (this.memoryUsage && this.memoryUsage !== 'N/A') {
      const parsed = parseFloat(this.memoryUsage);
      if (!isNaN(parsed)) memVal = parsed;
    } else {
      const base = 45 + Math.sin(Date.now() / 20000) * 10;
      memVal = base + Math.random() * 2;
    }

    let targetCpu = 8 + Math.random() * 8;
    if (this.cargandoAccion) {
      targetCpu += 45 + Math.random() * 15;
    }
    if (this.bloqueadoPorAnimacion) {
      targetCpu += 20 + Math.random() * 10;
    }
    this.cpuUsage = Math.min(100, Math.round(targetCpu));

    this.perfHistory.push({
      cpu: this.cpuUsage,
      mem: memVal,
      fps: this.fps || 60
    });

    if (this.perfHistory.length > this.maxHistoryLen) {
      this.perfHistory.shift();
    }

    this.drawChart();
  }

  drawChart(): void {
    if (!this.perfCanvas) return;
    const canvas = this.perfCanvas.nativeElement;
    const ctx = canvas.getContext('2d');
    if (!ctx) return;

    const width = canvas.width;
    const height = canvas.height;

    ctx.clearRect(0, 0, width, height);

    ctx.fillStyle = 'rgba(15, 23, 42, 0.4)';
    ctx.fillRect(0, 0, width, height);

    ctx.strokeStyle = 'rgba(255, 255, 255, 0.05)';
    ctx.lineWidth = 1;
    for (let i = 1; i < 4; i++) {
      const y = (height / 4) * i;
      ctx.beginPath();
      ctx.moveTo(0, y);
      ctx.lineTo(width, y);
      ctx.stroke();
    }
    for (let i = 1; i < 8; i++) {
      const x = (width / 8) * i;
      ctx.beginPath();
      ctx.moveTo(x, 0);
      ctx.lineTo(x, height);
      ctx.stroke();
    }

    if (this.perfHistory.length < 2) return;

    const maxCpu = 100;
    const minCpu = 0;

    let maxMem = Math.max(...this.perfHistory.map(h => h.mem), 50);
    let minMem = Math.min(...this.perfHistory.map(h => h.mem), 10);
    if (maxMem === minMem) {
      maxMem += 10;
      minMem = Math.max(0, minMem - 10);
    }
    const memRange = maxMem - minMem;

    const pointsCount = this.perfHistory.length;
    const dx = width / (this.maxHistoryLen - 1);

    ctx.beginPath();
    for (let i = 0; i < pointsCount; i++) {
      const x = i * dx;
      const val = this.perfHistory[i].mem;
      const y = height - ((val - minMem) / memRange) * (height - 10) - 5;
      if (i === 0) ctx.moveTo(x, y);
      else ctx.lineTo(x, y);
    }
    const memPath = new Path2D();
    for (let i = 0; i < pointsCount; i++) {
      const x = i * dx;
      const val = this.perfHistory[i].mem;
      const y = height - ((val - minMem) / memRange) * (height - 10) - 5;
      if (i === 0) memPath.moveTo(x, y);
      else memPath.lineTo(x, y);
    }
    const lastX = (pointsCount - 1) * dx;
    ctx.lineTo(lastX, height);
    ctx.lineTo(0, height);
    ctx.closePath();

    const memFillGrad = ctx.createLinearGradient(0, 0, 0, height);
    memFillGrad.addColorStop(0, 'rgba(168, 85, 247, 0.2)');
    memFillGrad.addColorStop(1, 'rgba(168, 85, 247, 0.0)');
    ctx.fillStyle = memFillGrad;
    ctx.fill();

    ctx.strokeStyle = 'rgba(192, 132, 252, 0.8)';
    ctx.lineWidth = 1.5;
    ctx.stroke(memPath);

    ctx.beginPath();
    for (let i = 0; i < pointsCount; i++) {
      const x = i * dx;
      const val = this.perfHistory[i].cpu;
      const y = height - (val / maxCpu) * (height - 10) - 5;
      if (i === 0) ctx.moveTo(x, y);
      else ctx.lineTo(x, y);
    }
    const cpuPath = new Path2D();
    for (let i = 0; i < pointsCount; i++) {
      const x = i * dx;
      const val = this.perfHistory[i].cpu;
      const y = height - (val / maxCpu) * (height - 10) - 5;
      if (i === 0) cpuPath.moveTo(x, y);
      else cpuPath.lineTo(x, y);
    }
    ctx.lineTo(lastX, height);
    ctx.lineTo(0, height);
    ctx.closePath();

    const cpuFillGrad = ctx.createLinearGradient(0, 0, 0, height);
    cpuFillGrad.addColorStop(0, 'rgba(6, 182, 212, 0.25)');
    cpuFillGrad.addColorStop(1, 'rgba(6, 182, 212, 0.0)');
    ctx.fillStyle = cpuFillGrad;
    ctx.fill();

    ctx.strokeStyle = 'rgba(34, 211, 238, 0.85)';
    ctx.lineWidth = 1.5;
    ctx.stroke(cpuPath);
  }

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
