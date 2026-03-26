import { Component, OnInit, OnDestroy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { BattleService } from '../../services/battle.service';
import { Router, ActivatedRoute } from '@angular/router';

@Component({
  selector: 'app-battle-board',
  templateUrl: './battle-board.component.html',
  styleUrls: ['./battle-board.component.scss'],
  standalone: true,
  imports: [CommonModule]
})
export class BattleBoardComponent implements OnInit, OnDestroy {

  matchId: string | null = null;
  partida: any = null;
  jugadorNombre = '';

  vibrarBot      = false;
  cargandoAccion = false;
  boardVisible   = false;
  showIntro      = true;
  introFadingOut = false;
  showTurnOverlay  = false;
  turnoOverlayTipo: 'jugador' | 'bot' = 'jugador';

  private ataqueRealizado = false;
  private pollingPartida: any;

  /**
   * FIX: el mapa ahora usa el NOMBRE del Pokémon (card.nombre) en minúsculas
   * como clave, no el ID de la carta. Así el sprite siempre coincide con
   * el Pokémon que se está jugando, independientemente del set.
   *
   * Completá esta lista con todos los pokémon de tu base de datos.
   * La clave es el nombre normalizado (sin acentos, en minúsculas).
   * El valor es el número nacional en la Pokédex.
   */
  private readonly pokedexNum: Record<string, number> = {
    // Gen 1
    'bulbasaur': 1,   'ivysaur': 2,     'venusaur': 3,
    'charmander': 4,  'charmeleon': 5,  'charizard': 6,
    'squirtle': 7,    'wartortle': 8,   'blastoise': 9,
    'caterpie': 10,   'metapod': 11,    'butterfree': 12,
    'weedle': 13,     'kakuna': 14,     'beedrill': 15,
    'pidgey': 16,     'pidgeotto': 17,  'pidgeot': 18,
    'rattata': 19,    'raticate': 20,
    'spearow': 21,    'fearow': 22,
    'ekans': 23,      'arbok': 24,
    'pikachu': 25,    'raichu': 26,
    'sandshrew': 27,  'sandslash': 28,
    'nidoran♀': 29,   'nidorina': 30,   'nidoqueen': 31,
    'nidoran♂': 32,   'nidorino': 33,   'nidoking': 34,
    'clefairy': 35,   'clefable': 36,
    'vulpix': 37,     'ninetales': 38,
    'jigglypuff': 39, 'wigglytuff': 40,
    'zubat': 41,      'golbat': 42,
    'oddish': 43,     'gloom': 44,      'vileplume': 45,
    'paras': 46,      'parasect': 47,
    'venonat': 48,    'venomoth': 49,
    'diglett': 50,    'dugtrio': 51,
    'meowth': 52,     'persian': 53,
    'psyduck': 54,    'golduck': 55,
    'mankey': 56,     'primeape': 57,
    'growlithe': 58,  'arcanine': 59,
    'poliwag': 60,    'poliwhirl': 61,  'poliwrath': 62,
    'abra': 63,       'kadabra': 64,    'alakazam': 65,
    'machop': 66,     'machoke': 67,    'machamp': 68,
    'bellsprout': 69, 'weepinbell': 70, 'victreebel': 71,
    'tentacool': 72,  'tentacruel': 73,
    'geodude': 74,    'graveler': 75,   'golem': 76,
    'ponyta': 77,     'rapidash': 78,
    'slowpoke': 79,   'slowbro': 80,
    'magnemite': 81,  'magneton': 82,
    'farfetchd': 83,  "farfetch'd": 83,
    'doduo': 84,      'dodrio': 85,
    'seel': 86,       'dewgong': 87,
    'grimer': 88,     'muk': 89,
    'shellder': 90,   'cloyster': 91,
    'gastly': 92,     'haunter': 93,    'gengar': 94,
    'onix': 95,
    'drowzee': 96,    'hypno': 97,
    'krabby': 98,     'kingler': 99,
    'voltorb': 100,   'electrode': 101,
    'exeggcute': 102, 'exeggutor': 103,
    'cubone': 104,    'marowak': 105,
    'hitmonlee': 106, 'hitmonchan': 107,
    'lickitung': 108,
    'koffing': 109,   'weezing': 110,
    'rhyhorn': 111,   'rhydon': 112,
    'chansey': 113,
    'tangela': 114,
    'kangaskhan': 115,
    'horsea': 116,    'seadra': 117,
    'goldeen': 118,   'seaking': 119,
    'staryu': 120,    'starmie': 121,
    'mr. mime': 122,  'mr mime': 122,
    'scyther': 123,
    'jynx': 124,
    'electabuzz': 125,
    'magmar': 126,
    'pinsir': 127,
    'tauros': 128,
    'magikarp': 129,  'gyarados': 130,
    'lapras': 131,
    'ditto': 132,
    'eevee': 133,     'vaporeon': 134,  'jolteon': 135,
    'flareon': 136,
    'porygon': 137,
    'omanyte': 138,   'omastar': 139,
    'kabuto': 140,    'kabutops': 141,
    'aerodactyl': 142,
    'snorlax': 143,
    'articuno': 144,  'zapdos': 145,    'moltres': 146,
    'dratini': 147,   'dragonair': 148, 'dragonite': 149,
    'mewtwo': 150,    'mew': 151,
    // Pokémon TCG especiales
    'crawdaunt': 342,
    'team aqua crawdaunt': 342,
    'team magma blaziken': 257,
    // Agregá más según tu colección...
  };

  constructor(
    private battleService: BattleService,
    private route: ActivatedRoute,
    private router: Router,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.matchId = this.route.snapshot.paramMap.get('id');
    const jugadorData = localStorage.getItem('jugador');
    if (!this.matchId || !jugadorData) { this.router.navigate(['/lobby']); return; }
    this.jugadorNombre = JSON.parse(jugadorData).username;
    this.cargarEstado();
    this.iniciarPolling();
    this.reproducirIntro();
  }

  ngOnDestroy(): void {
    if (this.pollingPartida) clearInterval(this.pollingPartida);
  }

  // ─── Intro ────────────────────────────────────────────────
  private reproducirIntro(): void {
    setTimeout(() => { this.introFadingOut = true;  this.cdr.detectChanges(); }, 2500);
    setTimeout(() => { this.boardVisible   = true;   this.cdr.detectChanges(); }, 2700);
    setTimeout(() => { this.showIntro      = false;  this.cdr.detectChanges(); }, 3300);
  }

  // ─── Turn overlay ─────────────────────────────────────────
  private mostrarTurnOverlay(turno: 'jugador' | 'bot'): void {
    this.turnoOverlayTipo = turno;
    this.showTurnOverlay  = true;
    this.cdr.detectChanges();
    setTimeout(() => { this.showTurnOverlay = false; this.cdr.detectChanges(); }, 2000);
  }

  // ─── Polling ──────────────────────────────────────────────
  iniciarPolling(): void {
    this.pollingPartida = setInterval(() => {
      if (this.partida?.turnoActual === 'BOT') this.cargarEstado();
    }, 2000);
  }

  cargarEstado(): void {
    if (!this.matchId) return;
    this.battleService.getState(this.matchId).subscribe({
      next: (data) => {
        if (JSON.stringify(this.partida) !== JSON.stringify(data)) {
          const turnoAntes = this.partida?.turnoActual;
          this.partida = data;
          if (turnoAntes && turnoAntes !== data.turnoActual) {
            if (data.turnoActual === 'JUGADOR') this.ataqueRealizado = false;
            this.mostrarTurnOverlay(data.turnoActual === 'JUGADOR' ? 'jugador' : 'bot');
          }
          this.cdr.detectChanges();
        }
      }
    });
  }

  // ─── Sprites ─────────────────────────────────────────────
  /**
   * FIX: busca el sprite por NOMBRE del Pokémon, no por ID de carta.
   * Normaliza el nombre: minúsculas, sin tildes, sin prefijos de set
   * (ej: "Team Aqua's Crawdaunt" → "crawdaunt" → sprite 342).
   */
  private normalizarNombre(nombre: string): string {
    if (!nombre) return '';
    return nombre
      .toLowerCase()
      .normalize('NFD').replace(/[\u0300-\u036f]/g, '') // quita tildes
      .trim();
  }

  private getPokemonNum(nombreCarta: string): number {
    const norm = this.normalizarNombre(nombreCarta);

    // Intento 1: nombre completo normalizado
    if (this.pokedexNum[norm]) return this.pokedexNum[norm];

    // Intento 2: buscar la primera palabra que matchee (para "Team X's Pokémon")
    const palabras = norm.split(/[\s']+/);
    for (const p of palabras.reverse()) { // de última a primera (el nombre suele estar al final)
      if (this.pokedexNum[p]) return this.pokedexNum[p];
    }

    // Intento 3: extraer número si el nombre tiene formato con número
    const numMatch = norm.match(/\d+/);
    if (numMatch) return parseInt(numMatch[0], 10);

    return 0;
  }

  getSpriteBack(nombreCarta: string): string {
    const num = this.getPokemonNum(nombreCarta);
    if (!num) return '';
    return `https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/back/${num}.png`;
  }

  getSpriteFront(nombreCarta: string): string {
    const num = this.getPokemonNum(nombreCarta);
    if (!num) return '';
    return `https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/${num}.png`;
  }

  onSpriteError(event: Event): void {
    (event.target as HTMLImageElement).style.display = 'none';
  }

  // ─── HP ───────────────────────────────────────────────────
  getHpPercent(pokemon: any): number {
    if (!pokemon?.hpActual) return 0;
    // hpMax puede venir como número o como string desde la carta
    const max = pokemon.hpMax
      || parseInt(pokemon.card?.hp, 10)
      || 100;
    return Math.max(0, Math.min(100, (pokemon.hpActual / max) * 100));
  }

  // ─── Helpers ──────────────────────────────────────────────
  getImagenCarta(id: string): string { return `images/cards/${id}.png`; }
  getEmptySlots(n: number): number[] { return Array(Math.max(0, 5 - n)).fill(0); }

  esEnergia(carta: any): boolean {
    const t = carta?.tipo?.toLowerCase() ?? '';
    return t.includes('energy') || t.includes('energía');
  }
  esPokemon(carta: any): boolean {
    return !this.esEnergia(carta) && !(carta?.tipo?.toLowerCase() ?? '').includes('stage');
  }
  puedeAtacar(): boolean {
    return !!(
      this.partida?.jugador?.activo &&
      this.partida?.bot?.activo &&
      this.partida?.turnoActual === 'JUGADOR' &&
      !this.ataqueRealizado &&
      (this.partida?.jugador?.activo?.energiasUnidas?.length ?? 0) > 0
    );
  }

  // ─── Acciones ─────────────────────────────────────────────
  jugarCarta(carta: any): void {
    if (this.partida.turnoActual !== 'JUGADOR' || this.cargandoAccion) return;
    if (this.esEnergia(carta)) this.gestionarUnionEnergia(carta);
    else if (!(carta?.tipo?.toLowerCase() ?? '').includes('stage')) this.gestionarBajadaPokemon(carta);
    else alert('Las evoluciones todavía no están implementadas.');
  }

  private gestionarBajadaPokemon(carta: any): void {
    const posicion = this.partida.jugador.activo ? 1 : 0;
    this.cargandoAccion = true;
    this.battleService.jugarPokemon(this.matchId!, carta.id, posicion).subscribe({
      next: () => { this.cargandoAccion = false; this.cargarEstado(); },
      error: () => (this.cargandoAccion = false)
    });
  }

  private gestionarUnionEnergia(cartaEnergia: any): void {
    if (!this.partida.jugador.activo) { alert('¡Necesitás un Pokémon activo!'); return; }
    this.cargandoAccion = true;
    this.battleService.unirEnergia(this.matchId!, this.partida.jugador.activo.card.id, cartaEnergia.id).subscribe({
      next: () => { this.cargandoAccion = false; this.cargarEstado(); },
      error: (err) => { this.cargandoAccion = false; console.error(err); alert('No se pudo unir la energía.'); }
    });
  }

  atacar(): void {
    if (!this.puedeAtacar() || this.cargandoAccion) return;
    this.cargandoAccion = true;
    this.battleService.atacar(this.matchId!).subscribe({
      next: () => {
        this.ataqueRealizado = true;
        this.vibrarBot = true;
        setTimeout(() => (this.vibrarBot = false), 500);
        this.cargandoAccion = false;
        this.cargarEstado();
      },
      error: (err) => {
        this.cargandoAccion = false;
        alert('El ataque falló: ' + (err?.error ?? 'error desconocido'));
      }
    });
  }

  pasarTurno(): void {
    if (this.partida.turnoActual !== 'JUGADOR' || this.cargandoAccion) return;
    this.cargandoAccion = true;
    this.mostrarTurnOverlay('bot');
    this.battleService.pasarTurno(this.matchId!).subscribe({
      next: () => { this.cargandoAccion = false; this.cargarEstado(); },
      error: () => (this.cargandoAccion = false)
    });
  }

  seleccionarBanca(pokemon: any): void {
    if (!this.partida.jugador.activo && this.partida.turnoActual === 'JUGADOR') {
      this.cargandoAccion = true;
      this.battleService.jugarPokemon(this.matchId!, pokemon.card.id, 0).subscribe({
        next: () => { this.cargandoAccion = false; this.cargarEstado(); },
        error: () => (this.cargandoAccion = false)
      });
    }
  }

  volverAlLobby(): void { this.router.navigate(['/lobby']); }
}