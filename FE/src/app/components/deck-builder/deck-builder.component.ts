import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { JugadorService } from '../../services/jugador.service';
import { MazoService } from '../../services/mazo.service';
import { Card } from '../../model/card';

@Component({
  selector: 'app-deck-builder',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './deck-builder.component.html',
  styleUrls: ['./deck-builder.component.scss']
})
export class DeckBuilderComponent implements OnInit {
  coleccion: Card[] = [];
  mazoEnProceso: Card[] = [];
  cantidadesPoseidas: { [key: string]: number } = {};
  nombreMazo: string = 'Mi Nuevo Mazo';
  username: string = '';
  
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
    private cdr: ChangeDetectorRef
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

  // ESTA FUNCIÓN ES LA QUE BUSCA EL HTML
getImagenReal(id: string): string {
    const archivo = this.mapaFotos[id] || id;
    // LA RUTA CORRECTA SEGÚN TU ÁRBOL (Carpeta public):
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
      alert(`¡No tenés más copias de ${carta.nombre}! Abrí más sobres.`);
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
      alert('¡Mazo actualizado!');
      this.router.navigate(['/lobby']);
    });
  } else {
    // Lógica original de guardado
    this.mazoService.guardarMazo(this.nombreMazo, this.username, ids).subscribe(() => {
      alert('¡Mazo creado!');
      this.router.navigate(['/lobby']);
    });
  }
}

  volver() { this.router.navigate(['/lobby']); }
}
