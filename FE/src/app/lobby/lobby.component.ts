import { Component, OnInit, ChangeDetectorRef, ViewChild, ElementRef, AfterViewInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { SobreService } from '../services/sobre.service';
import { MazoService } from '../services/mazo.service';
import { JugadorService } from '../services/jugador.service';
import { BattleService } from '../services/battle.service';
import { Router } from '@angular/router';
import { AperturaSobreComponent } from '../components/apertura-sobre/apertura-sobre';
import { Card } from '../model/card';
import { Jugador, JugadorDatosResponse } from '../model/jugador';
import { Mazo } from '../model/mazo';
import { Partida } from '../model/battle';

// Datos usados por el zoom flotante de una carta.
export interface PokemonZoomUI {
  id: string;
  nombre: string;
  imagen: string;
  pokedexId: number;
  hp: number | string;
  tipo: string;
  attacks: string;
  hpIcon: string;
  typeIcon: string;
  attacksIcon: string;
}

@Component({
  selector: 'app-lobby',
  templateUrl: './lobby.component.html',
  styleUrls: ['./lobby.component.scss'],
  standalone: true,
  imports: [CommonModule, AperturaSobreComponent]
})
export class LobbyComponent implements OnInit, AfterViewInit {
  @ViewChild('bgVideo') bgVideo!: ElementRef<HTMLVideoElement>;

  // Estado principal del lobby.
  jugador: Jugador | null = null;
  mazos: Mazo[] = [];
  slotsVacios: number[] = [];
  sobresDisponibles: number = 0;
  cantidadCartas: number = 0;
  cantidadCartasUnicas: number = 0;
  mostrarAnimacionSobre: boolean = false;
  cartasNuevas: Card[] = [];

  // Scanner / Zoom
  pkmZoom: PokemonZoomUI | null = null;
  zoomX: number = 0;
  zoomY: number = 0;

  constructor(
    private sobreService: SobreService,
    private mazoService: MazoService,
    private jugadorService: JugadorService,
    private battleService: BattleService,
    private router: Router,
    private cdr: ChangeDetectorRef
  ) {}

  // Recupera al jugador guardado y carga su estado inicial.
  ngOnInit(): void {
    try {
      const data = localStorage.getItem('jugador');
      if (data) {
        this.jugador = JSON.parse(data);
        this.refrescarTodo();
      } else {
        this.router.navigate(['/login']);
      }
    } catch (error) {
      console.error('Error parseando datos del jugador:', error);
      this.router.navigate(['/login']);
    }
  }

  // Intenta reproducir el video decorativo apenas exista en el DOM.
  ngAfterViewInit(): void {
    if (this.bgVideo?.nativeElement) {
      this.bgVideo.nativeElement.muted = true;
      this.bgVideo.nativeElement.play().catch(err => console.error('Video bloqueado:', err));
    }
  }

  // Refresca resumen, sobres y mazos del jugador.
  refrescarTodo() {
    if (!this.jugador?.username) return;

    this.jugadorService.getJugador(this.jugador.username).subscribe({
      next: (res: JugadorDatosResponse) => {
        this.sobresDisponibles = res.sobresDisponibles ?? 0;

        if (res.cartasObtenidas && Array.isArray(res.cartasObtenidas)) {
          const idsUnicos = new Set(res.cartasObtenidas.map((c: Card) => c.pokemonId || c.id));
          this.cantidadCartasUnicas = idsUnicos.size;
        } else {
          this.cantidadCartasUnicas = res.cantidadCartas ?? 0;
        }

        this.jugador = { ...this.jugador!, ...res };
        this.cdr.detectChanges();
      },
      error: (err) => console.error('Error al obtener datos del jugador', err)
    });

    this.cargarMazosDeJugador();
  }

  // Carga los mazos visibles y rellena slots vacios.
  cargarMazosDeJugador() {
    if (!this.jugador?.username) return;

    this.mazoService.getMazosByJugador(this.jugador.username).subscribe({
      next: (res: Mazo[]) => {
        this.mazos = res;
        const faltantes = 2 - this.mazos.length;
        this.slotsVacios = faltantes > 0 ? Array(faltantes).fill(0) : [];
        this.cdr.detectChanges();
      }
    });
  }

  // Arma la tarjeta ampliada para inspeccion rapida.
  mostrarZoom(carta: Card, event: MouseEvent) {
    const pkm: PokemonZoomUI = {
      id: carta.id,
      nombre: carta.nombre,
      imagen: carta.imagen,
      pokedexId: carta.pokemonId || 1,
      hp: carta.hp || 70,
      tipo: carta.tipo || 'GRASS',
      attacks: carta.attacks || '',
      hpIcon: 'â™¥',
      typeIcon: 'ðŸƒ',
      attacksIcon: 'â€¢'
    };
    this.pkmZoom = pkm;
    this.actualizarPosicion(event);
  }

  // Cierra el zoom flotante.
  ocultarZoom() { this.pkmZoom = null; }

  // Reposiciona el zoom segun el mouse.
  actualizarPosicion(event: MouseEvent) {
    if (this.pkmZoom) {
      this.zoomX = event.clientX + 25;
      this.zoomY = event.clientY - 210;
      this.cdr.detectChanges();
    }
  }

  // Abre un sobre y dispara la animacion de revelado.
  abrirSobres() {
    if (!this.jugador?.username || this.sobresDisponibles <= 0) return;

    this.sobreService.abrirSobre(this.jugador.username).subscribe({
      next: (res: Card[]) => {
        this.cartasNuevas = res;
        this.mostrarAnimacionSobre = true;
        this.sobresDisponibles--;
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error('Error al abrir sobre', err);
      }
    });
  }

  // Cierra la experiencia de apertura y refresca el lobby.
  finalizarApertura() {
    console.log('Cerrando apertura de sobres...');
    this.cartasNuevas = [];
    this.mostrarAnimacionSobre = false;
    document.body.classList.remove('modal-open');
    this.cdr.detectChanges();
    this.refrescarTodo();
  }

  // Navega al editor de mazos.
  irAlDeckBuilder() {
    this.router.navigate(['/deck-builder']);
  }

  // Crea una partida usando el mazo elegido.
  buscarPartida(mazoId: number) {
    this.battleService.startBattle(this.jugador!.username, mazoId).subscribe({
      next: (partida: Partida) => {
        if (partida && partida.id) {
          this.router.navigate(['/battle', partida.id]);
        }
      },
      error: (err) => console.error('Error al iniciar batalla', err)
    });
  }
}
