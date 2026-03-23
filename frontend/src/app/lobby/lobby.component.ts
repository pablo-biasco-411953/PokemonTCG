import { Component, OnInit } from '@angular/core';
import { SobreService } from '../services/sobre.service';
import { BattleService } from '../services/battle.service';
import { Router } from '@angular/router';
import { Jugador } from '../model/jugador';
import { MazoService } from '../services/mazo.service';
import { Mazo } from '../model/mazo';

@Component({
  selector: 'app-lobby',
  templateUrl: './lobby.component.html',
  styleUrls: ['./lobby.component.scss']
})
export class LobbyComponent implements OnInit {
  jugador: Jugador | null = null;
  sobresDisponibles = 6;
  cartasRecibidas: any[] = [];
  errorMsg: string | null = null;
  mazos: Mazo[] = [];

  constructor(
    private sobreService: SobreService,
    private battleService: BattleService,
    private mazoService: MazoService,
    private router: Router
  ) {}

  ngOnInit(): void {
    // Recuperar información del jugador desde localStorage
    const jugadorData = localStorage.getItem('jugador');
    if (jugadorData) {
      this.jugador = JSON.parse(jugadorData);
      this.sobresDisponibles = this.jugador.sobresDisponibles;

      // Cargar los mazos del jugador
      this.cargarMazos();
    }
  }

  private cargarMazos(): void {
    if (this.jugador?.username) {
      this.mazoService.listarMazos(this.jugador.username).subscribe({
        next: (mazos) => {
          this.mazos = mazos;
        },
        error: (err) => {
          console.error('Error al cargar mazos:', err);
        }
      });
    }
  }

  abrirSobres(): void {
    if (!this.jugador?.username) return;

    this.sobreService.abrirSobre(this.jugador.username).subscribe({
      next: (cartas) => {
        this.cartasRecibidas = cartas;
        if (this.sobresDisponibles > 0) {
          this.sobresDisponibles -= 1;
        }
        this.errorMsg = null;
      },
      error: (err) => {
        console.error(err);
        this.errorMsg = err?.error?.message || 'Error al abrir sobre';
        alert(this.errorMsg);
      }
    });
  }

  buscarPartida(mazoId: number): void {
    if (!this.jugador?.username) return;

    this.battleService.startBattle(this.jugador.username, mazoId).subscribe({
      next: (partida) => {
        console.log('Partida iniciada:', partida);
        // Redirigir a la pantalla de batalla
        localStorage.setItem('matchId', partida.id);
        this.router.navigate(['/battle']);
      },
      error: (err) => {
        console.error(err);
        this.errorMsg = err?.error?.message || 'Error al iniciar partida';
        alert(this.errorMsg);
      }
    });
  }
}
