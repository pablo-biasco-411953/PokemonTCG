import { Injectable } from '@angular/core';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';

@Injectable({ providedIn: 'root' })
export class BattleBoardUiService {
  // Diccionario para resolver sprites animados por nombre.
  private readonly pokedexNum: Record<string, number> = {
    bulbasaur: 1, ivysaur: 2, venusaur: 3,
    charmander: 4, charmeleon: 5, charizard: 6,
    squirtle: 7, wartortle: 8, blastoise: 9,
    caterpie: 10, metapod: 11, butterfree: 12,
    weedle: 13, kakuna: 14, beedrill: 15,
    pidgey: 16, pidgeotto: 17, pidgeot: 18,
    rattata: 19, raticate: 20,
    spearow: 21, fearow: 22,
    ekans: 23, arbok: 24,
    pikachu: 25, raichu: 26,
    sandshrew: 27, sandslash: 28,
    'nidoran♀': 29, nidorina: 30, nidoqueen: 31,
    'nidoran♂': 32, nidorino: 33, nidoking: 34,
    clefairy: 35, clefable: 36,
    vulpix: 37, ninetales: 38,
    jigglypuff: 39, wigglytuff: 40,
    zubat: 41, golbat: 42,
    oddish: 43, gloom: 44, vileplume: 45,
    paras: 46, parasect: 47,
    venonat: 48, venomoth: 49,
    diglett: 50, dugtrio: 51,
    meowth: 52, persian: 53,
    psyduck: 54, golduck: 55,
    mankey: 56, primeape: 57,
    growlithe: 58, arcanine: 59,
    poliwag: 60, poliwhirl: 61, poliwrath: 62,
    abra: 63, kadabra: 64, alakazam: 65,
    machop: 66, machoke: 67, machamp: 68,
    bellsprout: 69, weepinbell: 70, victreebel: 71,
    tentacool: 72, tentacruel: 73,
    geodude: 74, graveler: 75, golem: 76,
    ponyta: 77, rapidash: 78,
    slowpoke: 79, slowbro: 80,
    magnemite: 81, magneton: 82,
    farfetchd: 83, "farfetch'd": 83,
    doduo: 84, dodrio: 85,
    seel: 86, dewgong: 87,
    grimer: 88, muk: 89,
    shellder: 90, cloyster: 91,
    gastly: 92, haunter: 93, gengar: 94,
    onix: 95, drowzee: 96, hypno: 97,
    krabby: 98, kingler: 99,
    voltorb: 100, electrode: 101,
    exeggcute: 102, exeggutor: 103,
    cubone: 104, marowak: 105,
    hitmonlee: 106, hitmonchan: 107,
    lickitung: 108, koffing: 109, weezing: 110,
    rhyhorn: 111, rhydon: 112,
    chansey: 113, tangela: 114, kangaskhan: 115,
    tangrowth: 465,
    horsea: 116, seadra: 117,
    goldeen: 118, seaking: 119,
    staryu: 120, starmie: 121,
    'mr. mime': 122, 'mr mime': 122,
    scyther: 123, jynx: 124,
    electabuzz: 125, magmar: 126, pinsir: 127, tauros: 128,
    magikarp: 129, gyarados: 130, lapras: 131, ditto: 132,
    eevee: 133, vaporeon: 134, jolteon: 135, flareon: 136,
    porygon: 137, omanyte: 138, omastar: 139,
    kabuto: 140, kabutops: 141, aerodactyl: 142, snorlax: 143,
    articuno: 144, zapdos: 145, moltres: 146,
    dratini: 147, dragonair: 148, dragonite: 149,
    mewtwo: 150, mew: 151,
    crawdaunt: 342, crobat: 169, espeon: 196, umbreon: 197,
    blaziken: 257, swampert: 260, sceptile: 254,
    torchic: 255, mudkip: 258, treecko: 252,
    ralts: 280, kirlia: 281, gardevoir: 282,
    chikorita: 152, bayleef: 153, meganium: 154,
    cyndaquil: 155, quilava: 156, typhlosion: 157,
    totodile: 158, croconaw: 159, feraligatr: 160,
    sentret: 161, furret: 162, hoothoot: 163, noctowl: 164,
    ledyba: 165, ledian: 166, spinarak: 167, ariados: 168,
    chinchou: 170, lanturn: 171, pichu: 172, cleffa: 173,
    igglybuff: 174, togepi: 175, togetic: 176, natu: 177, xatu: 178,
    mareep: 179, flaaffy: 180, ampharos: 181,
    bellossom: 182, marill: 183, azumarill: 184, sudowoodo: 185,
    politoed: 186, hoppip: 187, skiploom: 188, jumpluff: 189,
    aipom: 190, sunkern: 191, sunflora: 192, yanma: 193,
    wooper: 194, quagsire: 195, murkrow: 198, slowking: 199,
    misdreavus: 200, unown: 201, wobbuffet: 202, girafarig: 203,
    pineco: 204, forretress: 205, dunsparce: 206, gligar: 207,
    steelix: 208, snubbull: 209, granbull: 210,
    qwilfish: 211, scizor: 212, shuckle: 213, heracross: 214,
    sneasel: 215, teddiursa: 216, ursaring: 217,
    slugma: 218, magcargo: 219, swinub: 220, piloswine: 221,
    corsola: 222, remoraid: 223, octillery: 224, delibird: 225,
    mantine: 226, skarmory: 227, houndour: 228, houndoom: 229,
    kingdra: 230, phanpy: 231, donphan: 232, porygon2: 233,
    stantler: 234, smeargle: 235, tyrogue: 236, hitmontop: 237,
    smoochum: 238, elekid: 239, magby: 240, miltank: 241,
    blissey: 242, raikou: 243, entei: 244, suicune: 245,
    larvitar: 246, pupitar: 247, tyranitar: 248,
    lugia: 249, 'ho-oh': 250, celebi: 251
  };

  constructor(private sanitizer: DomSanitizer) {}

  // Extrae estados especiales mencionados en los ataques para mostrarlos en UI.
  extraerGlosario(carta: any): any[] {
    const statuses = [];
    const fullText = (carta.ataques || []).map((a: any) => a.texto || '').join(' ').toLowerCase();

    if (fullText.includes('paralyz') || fullText.includes('paraliz')) {
      statuses.push({ nombre: 'Paralizado', css: 'kw-paralyze', desc: 'El Pokemon no puede atacar ni retirarse este turno.' });
    }
    if (fullText.includes('poison') || fullText.includes('envene')) {
      statuses.push({ nombre: 'Envenenado', css: 'kw-poison', desc: 'Recibe 10 de dano entre turnos.' });
    }
    if (fullText.includes('asleep') || fullText.includes('dormi') || fullText.includes('duerm')) {
      statuses.push({ nombre: 'Dormido', css: 'kw-sleep', desc: 'No puede atacar ni retirar. Lanza moneda al final del turno para despertar.' });
    }
    if (fullText.includes('confus') || fullText.includes('confund')) {
      statuses.push({ nombre: 'Confundido', css: 'kw-confuse', desc: 'Lanza moneda al atacar. Si es cruz, falla y recibe 30 de dano.' });
    }
    if (fullText.includes('burn') || fullText.includes('quem')) {
      statuses.push({ nombre: 'Quemado', css: 'kw-burn', desc: 'Recibe 20 de dano entre turnos. Lanza moneda para curarse.' });
    }

    return statuses;
  }

  // Resalta palabras clave dentro del texto de un ataque.
  formatTextoAtaque(texto: string): SafeHtml {
    if (!texto) return '';

    let formatted = texto;
    formatted = formatted.replace(/(paralyzed|paralyzes|paraliza|paralizado)/gi, '<span class="kw-paralyze">$&</span>');
    formatted = formatted.replace(/(poisoned|poisons|envenena|envenenado)/gi, '<span class="kw-poison">$&</span>');
    formatted = formatted.replace(/(asleep|sleeps|duerme|dormido)/gi, '<span class="kw-sleep">$&</span>');
    formatted = formatted.replace(/(confused|confuses|confunde|confundido)/gi, '<span class="kw-confuse">$&</span>');
    formatted = formatted.replace(/(burned|burns|quema|quemado)/gi, '<span class="kw-burn">$&</span>');
    formatted = formatted.replace(/(does nothing|no hace nada)/gi, '<span class="kw-neutral">$&</span>');

    return this.sanitizer.bypassSecurityTrustHtml(formatted);
  }

  // Devuelve el sprite trasero animado del Pokemon.
  getSpriteBack(nombreCarta: string): string {
    const num = this.getPokemonNum(nombreCarta);
    return num ? `/sprites/pokemon/showdown/back/${num}.gif` : '';
  }

  // Devuelve el sprite frontal animado del Pokemon.
  getSpriteFront(nombreCarta: string): string {
    const num = this.getPokemonNum(nombreCarta);
    return num ? `/sprites/pokemon/showdown/${num}.gif` : '';
  }

  // Convierte los HP actuales en porcentaje para la barra de vida.
  getHpPercent(pokemon: any): number {
    if (!pokemon?.hpActual) return 0;
    return Math.max(0, Math.min(100, (pokemon.hpActual / this.getHpMax(pokemon)) * 100));
  }

  // Obtiene el HP maximo desde el estado o desde la carta base.
  getHpMax(pokemon: any): number {
    return pokemon?.hpMax || parseInt(pokemon?.card?.hp, 10) || 100;
  }

  // Arma la ruta publica de una imagen de carta.
  getImagenCarta(id: string): string {
    return `/images/cards/${id}.png`;
  }

  // Genera placeholders para completar los slots de banca.
  getEmptySlots(n: number): number[] {
    return Array(Math.max(0, 5 - n)).fill(0);
  }

  // Identifica si una carta es energia.
  esEnergia(carta: any): boolean {
    return carta?.supertype === 'Energy';
  }

  // Identifica si una carta es Pokemon.
  esPokemon(carta: any): boolean {
    return carta?.supertype === 'Pokémon' || carta?.supertype === 'Pokemon';
  }

  // Traduce el tipo de energia a una etiqueta legible.
  getEnergyName(tipo: string): string {
    const names: any = {
      grass: 'Planta',
      fire: 'Fuego',
      water: 'Agua',
      lightning: 'Electrico',
      psychic: 'Psiquico',
      fighting: 'Lucha',
      darkness: 'Siniestro',
      metal: 'Acero',
      colorless: 'Incolora',
      fairy: 'Hada',
      dragon: 'Dragon'
    };
    return names[(tipo || '').toLowerCase()] || 'Energia';
  }

  // Devuelve el color de UI asociado a cada energia.
  getEnergyColor(tipo: string): string {
    const colors: any = {
      grass: '#78C850',
      fire: '#F08030',
      water: '#6890F0',
      lightning: '#F8D030',
      psychic: '#F85888',
      fighting: '#C03028',
      darkness: '#705848',
      metal: '#B8B8D0',
      colorless: '#A8A878',
      fairy: '#EE99AC',
      dragon: '#7038F8'
    };
    return colors[(tipo || '').toLowerCase()] || '#A8A878';
  }

  // Normaliza nombres para comparaciones tolerantes a acentos.
  private normalizarNombre(nombre: string): string {
    if (!nombre) return '';
    return nombre.toLowerCase().normalize('NFD').replace(/[\u0300-\u036f]/g, '').trim();
  }

  // Busca el numero de Pokedex a partir del nombre de la carta.
  private getPokemonNum(nombreCarta: string): number {
    const norm = this.normalizarNombre(nombreCarta);
    if (this.pokedexNum[norm]) return this.pokedexNum[norm];

    const palabras = norm.split(/[\s'']+/).reverse();
    for (const palabra of palabras) {
      if (this.pokedexNum[palabra]) return this.pokedexNum[palabra];
    }

    const numMatch = norm.match(/\d+/);
    return numMatch ? parseInt(numMatch[0], 10) : 0;
  }
}
