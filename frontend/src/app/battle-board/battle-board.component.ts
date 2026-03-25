import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { BattleService } from '../services/battle.service';
import { Router } from '@angular/router';

@Component({
  selector: 'app-battle-board',
  templateUrl: './battle-board.component.html',
  styleUrls: ['./battle-board.component.scss'],
  standalone: true,
  imports: [CommonModule]
})
export class BattleBoardComponent implements OnInit {
  matchId: string | null = null;
  partida: any = null; 
  jugadorNombre: string = '';

  constructor(
    private battleService: BattleService,
    private router: Router,
    private cdr: ChangeDetectorRef 
  ) {}

  ngOnInit(): void {
    console.log("⚔️ Tablero de Batalla iniciado");

    this.matchId = localStorage.getItem('matchId');
    const jugadorData = localStorage.getItem('jugador');

    if (!this.matchId || !jugadorData) {
      console.warn("⚠️ No hay partida activa o usuario. Volviendo al lobby.");
      this.router.navigate(['/lobby']);
      return;
    }

    this.jugadorNombre = JSON.parse(jugadorData).username;
    this.cargarEstado();
  }

  cargarEstado() {
    if (!this.matchId) return;

    this.battleService.getState(this.matchId).subscribe({
      next: (data: any) => {
        console.log("✅ Estado de la partida cargado:", data);
        // Creamos una nueva referencia para que Angular detecte el cambio
        this.partida = { ...data };
        // Forzamos la actualización de la vista
        this.cdr.detectChanges(); 
      },
      error: (err: any) => {
        console.error("❌ Error al obtener el estado:", err);
        alert("No se pudo recuperar la partida.");
        this.router.navigate(['/lobby']);
      }
    });
  }

  getImagenCarta(id: string): string {
    // Si tus imágenes están en src/assets/images/cards/
    return `assets/images/cards/${id}.png`;
  }

  lanzarMoneda() {
    if (!this.matchId) return;
    
    console.log("🪙 Lanzando moneda...");
    this.battleService.lanzarMoneda(this.matchId).subscribe({
      next: (ganaJugador: boolean) => {
        alert(ganaJugador ? "¡Ganaste el sorteo! Empezás vos." : "El Bot ganó el sorteo y empieza.");
        this.cargarEstado();
      },
      error: (err: any) => {
        console.error("❌ Error al lanzar moneda:", err);
      }
    });
  }
}