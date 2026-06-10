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
import { BattleNotificationComponent } from '../battle/components/battle-notification/battle-notification';
import { BattleNotificationService } from '../battle/services/battle-notification';

@Component({
  selector: 'app-deck-builder',
  standalone: true,
  imports: [CommonModule, FormsModule, BattleNotificationComponent],
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
  nombreMazo = 'Mi Nuevo Mazo';
  username = '';
  cartaSinStockAnimandoId: string | null = null;

  isLoadingImages = true;
  loadingProgress = 0;
  filtroNombre = '';
  filtroTipo = 'Todos';
  tipos: string[] = ['Todos', 'Grass', 'Fire', 'Water', 'Lightning', 'Psychic', 'Fighting', 'Darkness', 'Metal', 'Dragon', 'Colorless'];

  showInspeccion = false;
  cardFocus: Card | null = null;
  hoverTimer: any;
  vistaDeckBuilder: 'library' | 'editor' = 'editor';
  mazoMenuSeleccionado: Mazo | null = null;

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
    private preloaderService: ImagePreloaderService,
    private notificationService: BattleNotificationService
  ) {}

  idMazoAEditar: number | null = null;

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
        this.actualizarCantidadesPoseidas();
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
    const urlsUnicas = Array.from(new Set(cartas.map(c => this.getImagenReal(c.id)))).slice(0, 18);

    this.preloaderService.preloadImages(urlsUnicas).subscribe({
      next: (progress) => {
        this.loadingProgress = progress;
        this.cdr.detectChanges();
      },
      complete: () => {
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

    this.mazoService.eliminarMazo(mazo.id).subscribe({
      next: () => {
        this.notificar(`Mazo "${mazo.nombre}" eliminado.`, 'success');
        this.mazoMenuSeleccionado = null;
        this.closed.emit(true);
      },
      error: (err) => {
        this.notificar(err?.error || 'No se pudo eliminar el mazo.', 'error');
      }
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

    if (copiasEnMazo < totalPoseidas && copiasEnMazo < 4 && this.mazoEnProceso.length < 60) {
      this.mazoEnProceso.push(carta);
      this.cantidadesEnMazo[carta.id] = copiasEnMazo + 1;
    } else if (copiasEnMazo >= totalPoseidas) {
      this.notificar(this.i18n.translate('alert.noMoreCopies', { card: carta.nombre }), 'warning');
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
    if (this.mazoEnProceso.length !== 60) {
      this.notificar('El mazo debe tener exactamente 60 cartas.', 'warning');
      return;
    }

    if (!this.tienePokemonBasicoEnMazo()) {
      this.notificar('No podés guardar un mazo sin al menos 1 Pokémon Básico.', 'error');
      return;
    }

    const ids = this.mazoEnProceso.map(c => c.id);

    if (this.idMazoAEditar) {
      this.mazoService.actualizarMazo(this.idMazoAEditar, this.nombreMazo, ids).subscribe({
        next: () => {
          this.notificar(this.i18n.translate('alert.deckUpdated'), 'success');
          this.cerrar(true);
        },
        error: (err) => {
          this.notificar(err?.error || 'No se pudo actualizar el mazo.', 'error');
        }
      });
    } else {
      this.mazoService.guardarMazo(this.nombreMazo, this.username, ids).subscribe({
        next: () => {
          this.notificar(this.i18n.translate('alert.deckCreated'), 'success');
          this.cerrar(true);
        },
        error: (err) => {
          this.notificar(err?.error || 'No se pudo guardar el mazo.', 'error');
        }
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

  private tienePokemonBasicoEnMazo(): boolean {
    return this.mazoEnProceso.some(carta => this.esPokemonBasico(carta));
  }

  private esPokemonBasico(carta: Card): boolean {
    const esPokemon = (carta.supertype || '').toLowerCase() === 'pokemon';
    const esBasico = (carta.subtypes || []).some(subtype => subtype.toLowerCase() === 'basic');
    return esPokemon && esBasico;
  }

  private notificar(message: string, type: 'info' | 'success' | 'warning' | 'error' = 'info'): void {
    this.notificationService.show(message, type, 3200);
  }
}
