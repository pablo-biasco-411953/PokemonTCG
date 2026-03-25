import { Component, OnInit, OnDestroy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { BattleService } from '../../services/battle.service';
import { Router, ActivatedRoute } from '@angular/router';

@Component({
  selector: 'app-battle-board',
  templateUrl: './battle-board.component.html',
  styleUrls: ['./battle-board.component.scss'],
  standalone: true,
  imports: [CommonModule]
})
export class BattleBoardComponent implements OnInit, OnDestroy {
  matchId: string | null = null;
  partida: any = null;
  jugadorNombre: string = '';

  vibrarBot: boolean = false;
  cargandoAccion: boolean = false;
  private pollingPartida: any;

  constructor(
    private battleService: BattleService,
    private route: ActivatedRoute,
    private router: Router,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.matchId = this.route.snapshot.paramMap.get('id');
    const jugadorData = localStorage.getItem('jugador');

    if (!this.matchId || !jugadorData) {
      this.router.navigate(['/lobby']);
      return;
    }

    this.jugadorNombre = JSON.parse(jugadorData).username;
    this.cargarEstado();
    this.iniciarPolling();
  }

  ngOnDestroy(): void {
    if (this.pollingPartida) clearInterval(this.pollingPartida);
  }

  iniciarPolling() {
    this.pollingPartida = setInterval(() => {
      if (this.partida?.turnoActual === 'BOT') {
        this.cargarEstado();
      }
    }, 2000);
  }

  cargarEstado() {
    if (!this.matchId) return;
    this.battleService.getState(this.matchId).subscribe({
      next: (data) => {
        if (JSON.stringify(this.partida) !== JSON.stringify(data)) {
          this.partida = data;
          this.cdr.detectChanges();
        }
      }
    });
  }

  getImagenCarta(id: string) {
    return `images/cards/${id}.png`;
  }

  /** Devuelve un emoji representando el estado del pokemon */
  estadoEmoji(estado: string | undefined): string {
    const map: Record<string, string> = {
      poisoned:  '☠',
      burned:    '🔥',
      paralyzed: '⚡',
      asleep:    '💤',
      confused:  '💫',
    };
    return map[estado?.toLowerCase() ?? ''] ?? '';
  }

  // ─── Acciones de juego ────────────────────────────

  jugarPokemon(carta: any) {
    if (this.partida.turnoActual !== 'JUGADOR' || this.cargandoAccion) return;

    const tipo = carta.tipo.toLowerCase();
    const esBasico = !tipo.includes('stage') && !tipo.includes('energy');

    if (!esBasico) {
      alert('Solo podés bajar Pokémon básicos directamente.');
      return;
    }

    const posicion = this.partida.jugador.activo ? 1 : 0;
    this.cargandoAccion = true;

    this.battleService.jugarPokemon(this.matchId!, carta.id, posicion).subscribe({
      next: () => { this.cargandoAccion = false; this.cargarEstado(); },
      error: () => (this.cargandoAccion = false)
    });
  }

  atacar() {
    if (
      this.partida.turnoActual !== 'JUGADOR' ||
      !this.partida.bot.activo ||
      this.cargandoAccion
    ) return;

    this.cargandoAccion = true;
    this.battleService.atacar(this.matchId!).subscribe({
      next: () => {
        this.vibrarBot = true;
        setTimeout(() => (this.vibrarBot = false), 500);
        this.cargandoAccion = false;
        this.cargarEstado();
      },
      error: () => (this.cargandoAccion = false)
    });
  }

  pasarTurno() {
    if (this.partida.turnoActual !== 'JUGADOR' || this.cargandoAccion) return;

    this.cargandoAccion = true;
    this.battleService.pasarTurno(this.matchId!).subscribe({
      next: () => { this.cargandoAccion = false; this.cargarEstado(); },
      error: () => (this.cargandoAccion = false)
    });
  }

  seleccionarBanca(pokemon: any) {
    // Hook para futuras mecánicas (retirar, evolucionar, etc.)
    console.log('Seleccionado de banca:', pokemon);
  }

  volverAlLobby() {
    this.router.navigate(['/lobby']);
  }
}