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
  // Datos principales del mazo en construccion.
  coleccion: Card[] = [];
  mazoEnProceso: Card[] = [];
  cantidadesPoseidas: { [key: string]: number } = {};
  nombreMazo: string = 'Mi Nuevo Mazo';
  username: string = '';
  idMazoAEditar: number | null = null;

  // Filtros del catalogo.
  filtroNombre: string = '';
  filtroTipo: string = 'Todos';
  tipos: string[] = ['Todos', 'Grass', 'Fire', 'Water', 'Lightning', 'Psychic', 'Fighting', 'Darkness', 'Metal', 'Dragon', 'Colorless'];

  // Estado del zoom de inspeccion.
  showInspeccion: boolean = false;
  cardFocus: Card | null = null;
  hoverTimer: any;

  // Mapeado temporal entre ids viejos y assets reales.
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

  // Carga la coleccion y, si aplica, un mazo para editar.
  ngOnInit(): void {
    const data = localStorage.getItem('jugador');
    if (data) {
      this.username = JSON.parse(data).username;
      this.cargarColeccion();

      const mazoParaEditar = history.state.mazo;
      if (mazoParaEditar) {
        this.idMazoAEditar = mazoParaEditar.id;
        this.nombreMazo = mazoParaEditar.nombre;
        this.mazoEnProceso = mazoParaEditar.cartas;
      }
    }
  }

  // Recupera la coleccion completa del jugador.
  cargarColeccion() {
    this.jugadorService.getColeccion(this.username).subscribe((res: Card[]) => {
      this.coleccion = res;
      this.actualizarCantidadesPoseidas();
      this.coleccion = this.obtenerCartasUnicas(res);
      this.cdr.detectChanges();
    });
  }

  // Quita duplicados para mostrar una Pokedex mas limpia.
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

  // Cuenta cuantas copias reales posee el jugador de cada carta.
  actualizarCantidadesPoseidas() {
    this.cantidadesPoseidas = {};
    this.coleccion.forEach(c => {
      this.cantidadesPoseidas[c.id] = (this.cantidadesPoseidas[c.id] || 0) + 1;
    });
  }

  // Coleccion filtrada por nombre y tipo.
  get coleccionFiltrada() {
    return this.coleccion.filter(c => {
      const matchNombre = c.nombre.toLowerCase().includes(this.filtroNombre.toLowerCase());
      const matchTipo = this.filtroTipo === 'Todos' || c.tipo === this.filtroTipo;
      return matchNombre && matchTipo;
    });
  }

  // Resuelve la imagen publica asociada a una carta.
  getImagenReal(id: string): string {
    const archivo = this.mapaFotos[id] || id;
    return `/images/cards/${archivo}.png`;
  }

  // Activa la inspeccion con una breve espera.
  startHover(card: Card) {
    this.hoverTimer = setTimeout(() => {
      this.cardFocus = card;
      this.showInspeccion = true;
      this.cdr.detectChanges();
    }, 1500);
  }

  // Cancela el zoom de inspeccion.
  stopHover() {
    clearTimeout(this.hoverTimer);
    this.showInspeccion = false;
    this.cardFocus = null;
  }

  // Agrega una carta al mazo si cumple reglas de cantidad.
  agregarAlMazo(carta: Card) {
    const copiasEnMazo = this.mazoEnProceso.filter(c => c.id === carta.id).length;
    const totalPoseidas = this.cantidadesPoseidas[carta.id] || 0;

    if (copiasEnMazo < totalPoseidas && copiasEnMazo < 4 && this.mazoEnProceso.length < 60) {
      this.mazoEnProceso.push(carta);
    } else if (copiasEnMazo >= totalPoseidas) {
      alert(`Â¡No tenÃ©s mÃ¡s copias de ${carta.nombre}! AbrÃ­ mÃ¡s sobres.`);
    }
  }

  // Cuenta cuantas copias de una carta ya hay en el mazo.
  getCantidadEnMazo(id: string): number {
    return this.mazoEnProceso.filter(c => c.id === id).length;
  }

  // Elimina una carta del mazo segun su posicion.
  quitarDelMazo(index: number) {
    this.mazoEnProceso.splice(index, 1);
  }

  // Guarda un mazo nuevo o actualiza uno existente.
  guardar() {
    const ids = this.mazoEnProceso.map(c => c.id);

    if (this.idMazoAEditar) {
      this.mazoService.actualizarMazo(this.idMazoAEditar, this.nombreMazo, ids).subscribe(() => {
        alert('Â¡Mazo actualizado!');
        this.router.navigate(['/lobby']);
      });
    } else {
      this.mazoService.guardarMazo(this.nombreMazo, this.username, ids).subscribe(() => {
        alert('Â¡Mazo creado!');
        this.router.navigate(['/lobby']);
      });
    }
  }

  // Vuelve al lobby.
  volver() { this.router.navigate(['/lobby']); }
}
