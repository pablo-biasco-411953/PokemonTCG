import { Component, OnChanges, OnInit, ChangeDetectorRef, EventEmitter, Input, Output, SimpleChanges } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { JugadorService } from '../../core/services/jugador.service';
import { MazoService } from './services/mazo.service';
import { Card } from '../../shared/models/card';
import { Mazo } from '../../shared/models/mazo';
import { I18nService } from '../../i18n/i18n.service';
import { ImagePreloaderService } from '../../core/services/image-preloader.service';

@Component({
  selector: 'app-deck-builder',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './deck-builder.component.html',
  styleUrls: ['./deck-builder.component.scss'],
  host: { '[class.embedded]': 'embedded' }
})
export class DeckBuilderComponent implements OnInit, OnChanges {
  @Input() embedded = false;
  @Input() mazoInicial: Mazo | null = null;
  @Input() mazosDisponibles: Mazo[] = [];
  @Output() closed = new EventEmitter<boolean>();

  coleccion: Card[] = [];
  mazoEnProceso: Card[] = [];
  cantidadesPoseidas: { [key: string]: number } = {};
  private cantidadesEnMazo: { [key: string]: number } = {};
  nombreMazo: string = 'Mi Nuevo Mazo';
  username: string = '';
  cartaSinStockAnimandoId: string | null = null;
  
  // PRELOADER
  isLoadingImages: boolean = true;
  loadingProgress: number = 0;
  // FILTROS
  filtroNombre: string = '';
  filtroTipo: string = 'Todos';
  tipos: string[] = ['Todos', 'Grass', 'Fire', 'Water', 'Lightning', 'Psychic', 'Fighting', 'Darkness', 'Metal', 'Dragon', 'Colorless'];

  // INSPECCIÓN (Zoom)
  showInspeccion: boolean = false;
  cardFocus: Card | null = null;
  hoverTimer: any;
  vistaDeckBuilder: 'library' | 'editor' = 'editor';
  mazoMenuSeleccionado: Mazo | null = null;

  // MAPEADO DE IDS VIEJOS A FOTOS REALES
  mapaFotos: { [key: string]: string } = {
    'p1': 'base1-1', 'p2': 'base3-1', 'p3': 'base3-2', 'p4': 'base3-3',
    'p5': 'base4-1', 'p6': 'base6-1', 'p7': 'base6-2', 'p8': 'bw1-1',
    'p9': 'bw1-2', 'p10': 'bw10-1', 'p11': 'bw10-2', 'p12': 'bw10-3'
  };

  constructor(
    private jugadorService: JugadorService,
    private mazoService: MazoService,
    private router: Router,
    private cdr: ChangeDetectorRef,
    private i18n: I18nService,
    private preloaderService: ImagePreloaderService
  ) {}
  idMazoAEditar: number | null = null; // Para saber si estamos editando
ngOnInit(): void {
  const data = localStorage.getItem('jugador');
  if (data) {
    this.username = JSON.parse(data).username;
    this.cargarColeccion();

    const mazoInicial = this.mazoInicial || history.state.mazo || null;
    this.cargarMazoParaEditar(mazoInicial);
    this.vistaDeckBuilder = this.embedded && !mazoInicial ? 'library' : 'editor';
  } else {
    this.isLoadingImages = false;
  }
}

ngOnChanges(changes: SimpleChanges): void {
  if (changes['mazoInicial'] && !changes['mazoInicial'].firstChange) {
    this.cargarMazoParaEditar(this.mazoInicial);
    this.vistaDeckBuilder = this.mazoInicial ? 'editor' : (this.embedded ? 'library' : 'editor');
  }
}

private cargarMazoParaEditar(mazo: Mazo | null) {
  if (!mazo) {
    this.idMazoAEditar = null;
    this.nombreMazo = 'Mi Nuevo Mazo';
    this.mazoEnProceso = [];
    this.cantidadesEnMazo = {};
    return;
  }

  this.idMazoAEditar = mazo.id;
  this.nombreMazo = mazo.nombre;
  this.mazoEnProceso = [...(mazo.cartas || [])];
  this.recalcularCantidadesEnMazo();
}

cargarColeccion() {
    this.jugadorService.getColeccion(this.username).subscribe({
      next: (res: Card[]) => {
      this.coleccion = res;
      this.actualizarCantidadesPoseidas(); // Contamos cuántas de cada una tiene
      
      // Eliminamos duplicados de la visualización para que la Pokédex sea limpia
      // pero sabiendo cuántas copias tenemos de cada una.
      this.coleccion = this.obtenerCartasUnicas(res);
      
      this.precargarImagenes(this.coleccion);
      },
      error: (err) => {
        console.error('Error cargando coleccion:', err);
        this.isLoadingImages = false;
        this.cdr.detectChanges();
      }
    });
  }

  precargarImagenes(cartas: Card[]) {
    // Tomamos hasta 40 imágenes únicas para no bloquear demasiado tiempo
    const urlsUnicas = Array.from(new Set(cartas.map(c => this.getImagenReal(c.id)))).slice(0, 18);
    
    this.preloaderService.preloadImages(urlsUnicas).subscribe({
      next: (progress) => {
        this.loadingProgress = progress;
        this.cdr.detectChanges();
      },
      complete: () => {
        // Un pequeño delay por estética de la animación
        setTimeout(() => {
          this.isLoadingImages = false;
          this.cdr.detectChanges();
        }, 600);
      }
    });
  }

obtenerCartasUnicas(cartas: Card[]): Card[] {
    const unicas: Card[] = [];
    const idsVistos = new Set();
    cartas.forEach(c => {
      if (!idsVistos.has(c.id)) {
        idsVistos.add(c.id);
        unicas.push(c);
      }
    });
    return unicas;
  }

actualizarCantidadesPoseidas() {
    this.cantidadesPoseidas = {};
    this.coleccion.forEach(c => {
      this.cantidadesPoseidas[c.id] = (this.cantidadesPoseidas[c.id] || 0) + 1;
    });
  }

  get coleccionFiltrada() {
    return this.coleccion.filter(c => {
      const matchNombre = c.nombre.toLowerCase().includes(this.filtroNombre.toLowerCase());
      const matchTipo = this.filtroTipo === 'Todos' || c.tipo === this.filtroTipo;
      return matchNombre && matchTipo;
    });
  }

  trackByCardId(_index: number, carta: Card): string {
    return carta.id;
  }

  trackByDeckCard(index: number, carta: Card): string {
    return `${carta.id}-${index}`;
  }

  get modoEdicion(): boolean {
    return this.idMazoAEditar !== null;
  }

  abrirNuevoMazo() {
    this.cargarMazoParaEditar(null);
    this.vistaDeckBuilder = 'editor';
    this.mazoMenuSeleccionado = null;
  }

  abrirMenuMazo(mazo: Mazo) {
    this.mazoMenuSeleccionado = mazo;
  }

  cerrarMenuMazo() {
    this.mazoMenuSeleccionado = null;
  }

  editarMazoDesdeGaleria(mazo: Mazo) {
    this.cargarMazoParaEditar(mazo);
    this.vistaDeckBuilder = 'editor';
    this.mazoMenuSeleccionado = null;
  }

  borrarMazoDesdeGaleria(mazo: Mazo) {
    const confirmar = confirm(`Eliminar el mazo "${mazo.nombre}"?`);
    if (!confirmar) return;

    this.mazoService.eliminarMazo(mazo.id).subscribe(() => {
      this.mazoMenuSeleccionado = null;
      this.closed.emit(true);
    });
  }

getImagenReal(id: string): string {
    const archivo = this.mapaFotos[id] || id;
    return `/images/cards/${archivo}.png`;
  }

  mostrarDetalleCarta(card: Card) {
    this.cardFocus = card;
  }

  get cartaDetalle(): Card | null {
    return this.cardFocus || this.coleccionFiltrada[0] || null;
  }

  getAtaquesDetalle(card: Card | null) {
    return (card?.ataques || []).slice(0, 2);
  }

  getDebilidadDetalle(card: Card | null): string {
    return card?.debilidades?.map(d => `${d.tipo}${d.valor ? ' ' + d.valor : ''}`).join(', ') || 'Sin dato';
  }

  getResistenciaDetalle(card: Card | null): string {
    return card?.resistencias?.map(r => `${r.tipo}${r.valor ? ' ' + r.valor : ''}`).join(', ') || 'Sin dato';
  }

  agregarAlMazo(carta: Card) {
    const copiasEnMazo = this.getCantidadEnMazo(carta.id);
    const totalPoseidas = this.cantidadesPoseidas[carta.id] || 0;

    // REGLA: No podés meter más de las que tenés, y máximo 4 (regla TCG)
    if (copiasEnMazo < totalPoseidas && copiasEnMazo < 4 && this.mazoEnProceso.length < 60) {
      this.mazoEnProceso.push(carta);
      this.cantidadesEnMazo[carta.id] = copiasEnMazo + 1;
    } else if (copiasEnMazo >= totalPoseidas) {
      alert(this.i18n.translate('alert.noMoreCopies', { card: carta.nombre }));
    }
  }

getCantidadEnMazo(id: string): number {
    return this.cantidadesEnMazo[id] || 0;
  }

  quitarDelMazo(index: number) {
    const [removida] = this.mazoEnProceso.splice(index, 1);
    if (!removida) return;

    const siguiente = Math.max(0, (this.cantidadesEnMazo[removida.id] || 1) - 1);
    if (siguiente === 0) {
      delete this.cantidadesEnMazo[removida.id];
    } else {
      this.cantidadesEnMazo[removida.id] = siguiente;
    }
  }

  private recalcularCantidadesEnMazo() {
    this.cantidadesEnMazo = {};
    this.mazoEnProceso.forEach(c => {
      this.cantidadesEnMazo[c.id] = (this.cantidadesEnMazo[c.id] || 0) + 1;
    });
  }
  
guardar() {
  const ids = this.mazoEnProceso.map(c => c.id);
  
  if (this.idMazoAEditar) {
    // Lógica para actualizar (necesitás este método en tu mazoService)
    this.mazoService.actualizarMazo(this.idMazoAEditar, this.nombreMazo, ids).subscribe(() => {
      alert(this.i18n.translate('alert.deckUpdated'));
      this.cerrar(true);
    });
  } else {
    // Lógica original de guardado
    this.mazoService.guardarMazo(this.nombreMazo, this.username, ids).subscribe(() => {
      alert(this.i18n.translate('alert.deckCreated'));
      this.cerrar(true);
    });
  }
}

  volver() {
    if (this.embedded && this.vistaDeckBuilder === 'editor' && !this.mazoInicial) {
      this.vistaDeckBuilder = 'library';
      this.mazoMenuSeleccionado = null;
      return;
    }
    this.cerrar(false);
  }

  private cerrar(refresh: boolean) {
    if (this.embedded) {
      this.closed.emit(refresh);
      return;
    }
    this.router.navigate(['/lobby']);
  }
}
