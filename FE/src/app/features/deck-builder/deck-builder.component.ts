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
import { CardService } from '../../core/services/card.service';
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
  catalogoCartas: Card[] = [];
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
  tipos: string[] = ['Todos', 'Grass', 'Fire', 'Water', 'Lightning', 'Psychic', 'Fighting', 'Darkness', 'Metal', 'Dragon', 'Colorless', 'Trainer', 'Energy'];

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
    private cardService: CardService,
    private notificationService: BattleNotificationService
  ) {}

  idMazoAEditar: number | null = null;

  ngOnInit(): void {
    const data = localStorage.getItem('jugador');
    if (data) {
      this.username = JSON.parse(data).username;
      this.cargarCatalogo();
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
      this.nombreMazo = '';
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

  private cargarCatalogo() {
    this.cardService.getAll().subscribe({
      next: (cards) => {
        this.catalogoCartas = cards;
        this.cdr.detectChanges();
      },
      error: (err) => console.error('Error cargando catalogo para evoluciones:', err),
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
        }, 300);
      },
      error: (err) => {
        console.error('Error cargando imagenes:', err);
        this.isLoadingImages = false;
        this.cdr.detectChanges();
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
      
      let matchTipo = false;
      if (this.filtroTipo === 'Todos') {
        matchTipo = true;
      } else if (this.filtroTipo === 'Trainer') {
        matchTipo = c.supertype === 'Trainer';
      } else if (this.filtroTipo === 'Energy') {
        matchTipo = c.supertype === 'Energy';
      } else {
        matchTipo = c.tipo === this.filtroTipo;
      }
      
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
    return this.cardService.getImagenCarta(archivo);
  }

  onCardImageError(event: Event): void {
    const img = event.target as HTMLImageElement;
    this.cardService.handleCardImageError(img);
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
    return this.formatearAtributos(card?.debilidades);
  }

  getResistenciaDetalle(card: Card | null): string {
    return this.formatearAtributos(card?.resistencias);
  }

  getEtapaEvolutiva(card: Card | null): string {
    if (!card || !this.esPokemon(card)) return card?.supertype || 'Carta';
    const subtypes = (card.subtypes || []).map(subtype => this.normalizar(subtype));
    if (subtypes.includes('basic')) return 'Pokemon Basico';
    if (subtypes.includes('stage 1')) return 'Fase 1';
    if (subtypes.includes('stage 2')) return 'Fase 2';
    if (subtypes.includes('mega')) return 'Mega Evolucion';
    if (card.evolvesFrom) return 'Evolucion';
    return 'Pokemon';
  }

  getEvolucionaDe(card: Card | null): string {
    if (!card || !this.esPokemon(card)) return 'No aplica';
    return card.evolvesFrom || 'No evoluciona de otro Pokemon';
  }

  getEvolucionaA(card: Card | null): string {
    if (!card || !this.esPokemon(card)) return 'No aplica';
    const fuente = this.catalogoCartas.length ? this.catalogoCartas : this.coleccion;
    const evoluciones = fuente.filter(
      candidate => this.normalizar(candidate.evolvesFrom) === this.normalizar(card.nombre),
    );
    const nombres = Array.from(new Set(evoluciones.map(candidate => candidate.nombre)));
    return nombres.length ? nombres.join(', ') : 'Sin evolucion en la coleccion';
  }

  mostrarInfoEvolucion(card: Card | null): boolean {
    return !!card && this.esPokemon(card);
  }

  private formatearAtributos(atributos: Card['debilidades']): string {
    if (!atributos?.length) return 'Ninguna';
    return atributos
      .map(atributo => {
        const tipo = atributo.tipo || atributo.type;
        const valor = atributo.valor || atributo.value;
        return tipo ? `${tipo}${valor ? ' ' + valor : ''}` : null;
      })
      .filter((texto): texto is string => !!texto)
      .join(', ') || 'Ninguna';
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
    return this.esPokemon(carta)
      && (carta.subtypes || []).some(subtype => this.normalizar(subtype) === 'basic');
  }

  public esPokemon(carta: Card): boolean {
    return this.normalizar(carta.supertype) === 'pokemon';
  }

  public esEX(carta: Card): boolean {
    return !!carta.subtypes && carta.subtypes.includes('EX');
  }

  private normalizar(texto: string | null | undefined): string {
    return (texto || '')
      .normalize('NFD')
      .replace(/[\u0300-\u036f]/g, '')
      .trim()
      .toLowerCase();
  }

  private notificar(message: string, type: 'info' | 'success' | 'warning' | 'error' = 'info'): void {
    this.notificationService.show(message, type, 3200);
  }
}
