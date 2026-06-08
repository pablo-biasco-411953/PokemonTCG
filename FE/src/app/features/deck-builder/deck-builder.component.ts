import { Component, OnInit, ChangeDetectorRef, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { JugadorService } from '../../core/services/jugador.service';
import { MazoService } from './services/mazo.service';
import { Card } from '../../shared/models/card';
import { TranslatePipe } from '../../i18n/translate.pipe';
import { I18nService } from '../../i18n/i18n.service';

@Component({
  selector: 'app-deck-builder',
  standalone: true,
  imports: [CommonModule, FormsModule, TranslatePipe],
  templateUrl: './deck-builder.component.html',
  styleUrls: ['./deck-builder.component.scss'],
  host: { '[class.embedded]': 'embedded' }
})
export class DeckBuilderComponent implements OnInit {
  @Input() embedded = false;
  @Output() closed = new EventEmitter<boolean>();

  coleccion: Card[] = [];
  mazoEnProceso: Card[] = [];
  cantidadesPoseidas: { [key: string]: number } = {};
  nombreMazo: string = 'Mi Nuevo Mazo';
  username: string = '';
  cartaSinStockAnimandoId: string | null = null;
  
  // FILTROS
  filtroNombre: string = '';
  filtroTipo: string = 'Todos';
  tipos: string[] = ['Todos', 'Grass', 'Fire', 'Water', 'Lightning', 'Psychic', 'Fighting', 'Darkness', 'Metal', 'Dragon', 'Colorless'];

  // INSPECCIÓN (Zoom)
  showInspeccion: boolean = false;
  cardFocus: Card | null = null;
  hoverTimer: any;

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
    private i18n: I18nService
  ) {}
  idMazoAEditar: number | null = null; // Para saber si estamos editando
ngOnInit(): void {
  const data = localStorage.getItem('jugador');
  if (data) {
    this.username = JSON.parse(data).username;
    this.cargarColeccion();
    
    // VERIFICAR SI HAY UN MAZO PARA EDITAR (podes pasarlo por estado de ruta o params)
    const mazoParaEditar = history.state.mazo; 
    if (mazoParaEditar) {
      this.idMazoAEditar = mazoParaEditar.id;
      this.nombreMazo = mazoParaEditar.nombre;
      this.mazoEnProceso = mazoParaEditar.cartas; // Cargamos las cartas guardadas
    }
  }
}

  

cargarColeccion() {
    this.jugadorService.getColeccion(this.username).subscribe((res: Card[]) => {
      this.coleccion = res;
      this.actualizarCantidadesPoseidas(); // Contamos cuántas de cada una tiene
      
      // Eliminamos duplicados de la visualización para que la Pokédex sea limpia
      // pero sabiendo cuántas copias tenemos de cada una.
      this.coleccion = this.obtenerCartasUnicas(res);
      this.cdr.detectChanges();
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

getImagenReal(id: string): string {
    if (/^xy/i.test(id)) {
      return `images/cards/${id}.png`;
    }

    const energyMap: Record<string, string> = {
      'col1-88': 'grass', 'g1-75': 'grass', 'xy12-91': 'grass', 'base1-99': 'grass',
      'col1-89': 'fire', 'g1-76': 'fire', 'xy12-92': 'fire', 'base1-98': 'fire',
      'col1-90': 'water', 'g1-77': 'water', 'xy12-93': 'water', 'base1-102': 'water',
      'col1-91': 'lightning', 'g1-78': 'lightning', 'xy12-94': 'lightning', 'base1-100': 'lightning',
      'col1-92': 'psychic', 'g1-79': 'psychic', 'xy12-95': 'psychic', 'base1-101': 'psychic',
      'col1-93': 'fighting', 'g1-80': 'fighting', 'xy12-96': 'fighting', 'base1-97': 'fighting',
      'col1-94': 'darkness', 'g1-81': 'darkness', 'xy12-97': 'darkness',
      'col1-95': 'metal', 'g1-82': 'metal',
      'g1-83': 'fairy'
    };
    const archivo = this.mapaFotos[id] || id;
    if (energyMap[archivo]) {
      return `images/cards/energy-${energyMap[archivo]}.png`;
    }
    return `images/cards/${archivo}.png`;
}
  startHover(card: Card) {
    this.hoverTimer = setTimeout(() => {
      this.cardFocus = card;
      this.showInspeccion = true;
      this.cdr.detectChanges();
    }, 1500);
  }

  stopHover() {
    clearTimeout(this.hoverTimer);
    this.showInspeccion = false;
    this.cardFocus = null;
  }

  agregarAlMazo(carta: Card) {
    const copiasEnMazo = this.mazoEnProceso.filter(c => c.id === carta.id).length;
    const totalPoseidas = this.cantidadesPoseidas[carta.id] || 0;

    // REGLA: No podés meter más de las que tenés, y máximo 4 (regla TCG)
    if (copiasEnMazo < totalPoseidas && copiasEnMazo < 4 && this.mazoEnProceso.length < 60) {
      this.mazoEnProceso.push(carta);
    } else if (copiasEnMazo >= totalPoseidas) {
      alert(this.i18n.translate('alert.noMoreCopies', { card: carta.nombre }));
    }
  }

getCantidadEnMazo(id: string): number {
    return this.mazoEnProceso.filter(c => c.id === id).length;
  }

  quitarDelMazo(index: number) {
    this.mazoEnProceso.splice(index, 1);
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

  volver() { this.cerrar(false); }

  private cerrar(refresh: boolean) {
    if (this.embedded) {
      this.closed.emit(refresh);
      return;
    }
    this.router.navigate(['/lobby']);
  }
}
