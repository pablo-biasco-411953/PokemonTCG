import { Component, OnInit, ChangeDetectorRef, ViewChild, ElementRef, AfterViewInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { SobreService } from '../services/sobre.service';
import { MazoService } from '../services/mazo.service';
import { JugadorService } from '../services/jugador.service';
import { BattleService } from '../services/battle.service';
import { Router } from '@angular/router';
import { AperturaSobreComponent } from '../components/apertura-sobre/apertura-sobre';
import { Mazo } from '../model/mazo'; 

// --- INTERFACES ---
export interface MazoUI extends Mazo {
  pokemonIds: number[];
}

export interface Jugador {
  username: string;
  nivel?: number;
  sobresDisponibles?: number;
  cantidadCartas?: number;
}

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
  
  jugador: Jugador | null = null;
  mazos: MazoUI[] = []; 
  slotsVacios: number[] = []; 
  sobresDisponibles: number = 0;
  cantidadCartas: number = 0;
  mostrarAnimacionSobre: boolean = false;
  cartasNuevas: any[] = []; 

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

  ngAfterViewInit(): void {
    if (this.bgVideo?.nativeElement) {
      this.bgVideo.nativeElement.muted = true;
      this.bgVideo.nativeElement.play().catch(err => console.error("Video bloqueado:", err));
    }
  }
cantidadCartasUnicas: number = 0;
 refrescarTodo() {
  if (!this.jugador?.username) return;

  this.jugadorService.getJugador(this.jugador.username).subscribe({
    next: (res: any) => {
      // 1. Actualizamos datos básicos
      this.sobresDisponibles = res.sobresDisponibles ?? 0;
      
      // 2. Lógica de Cartas Únicas:
      // Si el backend te devuelve la lista de cartas obtenidas (res.cartasObtenidas):
      if (res.cartasObtenidas && Array.isArray(res.cartasObtenidas)) {
        const idsUnicos = new Set(res.cartasObtenidas.map((c: any) => c.pokemonId || c.id));
        this.cantidadCartasUnicas = idsUnicos.size;
      } else {
        // Si el backend ya hace el "COUNT DISTINCT", usamos el valor directo
        this.cantidadCartasUnicas = res.cantidadCartas ?? 0;
      }

      this.jugador = { ...this.jugador!, ...res };
      this.cdr.detectChanges();
    },
    error: (err) => console.error('Error al obtener datos del jugador', err)
  });
  
  this.cargarMazosDeJugador();
}

  cargarMazosDeJugador() {
    if (!this.jugador?.username) return;
    
    this.mazoService.getMazosByJugador(this.jugador.username).subscribe({
      next: (res: any[]) => { 
        this.mazos = res; 
        const faltantes = 2 - this.mazos.length;
        this.slotsVacios = faltantes > 0 ? Array(faltantes).fill(0) : [];
        this.cdr.detectChanges(); 
      }
    });
  }

  mostrarZoom(carta: any, event: MouseEvent) {
    const pkm: PokemonZoomUI = {
      id: carta.id,
      nombre: carta.nombre,
      imagen: carta.imagen,
      pokedexId: carta.pokemonId || 1,
      hp: carta.hp || 70,
      tipo: carta.tipo || 'GRASS',
      attacks: carta.attacks || '',
      hpIcon: '♥',
      typeIcon: '🍃',
      attacksIcon: '•'
    };
    this.pkmZoom = pkm;
    this.actualizarPosicion(event);
  }

  ocultarZoom() { this.pkmZoom = null; }

  actualizarPosicion(event: MouseEvent) {
    if (this.pkmZoom) {
      this.zoomX = event.clientX + 25;
      this.zoomY = event.clientY - 210;
      this.cdr.detectChanges();
    }
  }

  abrirSobres() {
    if (!this.jugador?.username || this.sobresDisponibles <= 0) return;

    // 1. Llamada al servicio para obtener las cartas del backend
    this.sobreService.abrirSobre(this.jugador.username).subscribe({
      next: (res: any[]) => {
        this.cartasNuevas = res;
        
        // 2. DISPARAMOS LA CEREMONIA
        // Seteamos esto a true para que el @if del HTML renderice el componente de animación
        this.mostrarAnimacionSobre = true; 
        
        // 3. Bajamos el contador visual (opcional, podés hacerlo al terminar la animación)
        this.sobresDisponibles--; 
        
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error('Error al abrir sobre', err);
        // Podrías meter un sonido de error o un glitch visual acá
      }
    });
  }

  // Este método lo llama el componente de apertura mediante un (output) cuando el usuario termina de ver las cartas
finalizarApertura() { 
  console.log("Cerrando apertura de sobres...");
  this.cartasNuevas = []; 
  this.mostrarAnimacionSobre = false; 
  document.body.classList.remove('modal-open'); 
  this.cdr.detectChanges(); 
  this.refrescarTodo(); 
}
  irAlDeckBuilder() { 
    this.router.navigate(['/deck-builder']); 
  }
  
buscarPartida(mazoId: number) { 
// Agregamos el "!" después de this.jugador
this.battleService.startBattle(this.jugador!.username, mazoId).subscribe({
  next: (partida: any) => {
    if (partida && partida.id) {
       this.router.navigate(['/battle', partida.id]);
    }
  },
  error: (err) => console.error('Error al iniciar batalla', err)
});
}
}