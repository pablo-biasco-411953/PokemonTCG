import { Component, OnInit, OnDestroy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { BattleService } from '../../services/battle.service';
import { Router, ActivatedRoute } from '@angular/router';
import { DragDropModule, CdkDragDrop } from '@angular/cdk/drag-drop';
@Component({
  selector: 'app-battle-board',
  templateUrl: './battle-board.component.html',
  styleUrls: ['./battle-board.component.scss'],
  standalone: true,
  imports: [CommonModule, DragDropModule] // <-- ¡Agregalo acá!
})
export class BattleBoardComponent implements OnInit, OnDestroy {

  matchId: string | null = null;
  partida: any = null;
  jugadorNombre = '';

  // Estado visual
  vibrarBot         = false;
  cargandoAccion    = false;
  boardVisible      = false;
  showIntro         = true;
  introFadingOut    = false;
  showTurnOverlay   = false;
  turnoOverlayTipo: 'jugador' | 'bot' = 'jugador';

  // Animaciones de ataque
  animandoAtaque    = false;   // sprite jugador se lanza
  animandoBotAtaque = false;   // sprite bot recibe el golpe
  showImpactFlash   = false;   // flash blanco de impacto

  private ataqueRealizado = false;
  private pollingPartida: any;

  /**
   * Mapa nombre→número de Pokédex.
   * La clave es el nombre normalizado (minúsculas, sin tildes).
   * Completá con todos los pokémon de tu colección.
   */
  private readonly pokedexNum: Record<string, number> = {
    'bulbasaur':1,'ivysaur':2,'venusaur':3,
    'charmander':4,'charmeleon':5,'charizard':6,
    'squirtle':7,'wartortle':8,'blastoise':9,
    'caterpie':10,'metapod':11,'butterfree':12,
    'weedle':13,'kakuna':14,'beedrill':15,
    'pidgey':16,'pidgeotto':17,'pidgeot':18,
    'rattata':19,'raticate':20,
    'spearow':21,'fearow':22,
    'ekans':23,'arbok':24,
    'pikachu':25,'raichu':26,
    'sandshrew':27,'sandslash':28,
    'nidoran♀':29,'nidorina':30,'nidoqueen':31,
    'nidoran♂':32,'nidorino':33,'nidoking':34,
    'clefairy':35,'clefable':36,
    'vulpix':37,'ninetales':38,
    'jigglypuff':39,'wigglytuff':40,
    'zubat':41,'golbat':42,
    'oddish':43,'gloom':44,'vileplume':45,
    'paras':46,'parasect':47,
    'venonat':48,'venomoth':49,
    'diglett':50,'dugtrio':51,
    'meowth':52,'persian':53,
    'psyduck':54,'golduck':55,
    'mankey':56,'primeape':57,
    'growlithe':58,'arcanine':59,
    'poliwag':60,'poliwhirl':61,'poliwrath':62,
    'abra':63,'kadabra':64,'alakazam':65,
    'machop':66,'machoke':67,'machamp':68,
    'bellsprout':69,'weepinbell':70,'victreebel':71,
    'tentacool':72,'tentacruel':73,
    'geodude':74,'graveler':75,'golem':76,
    'ponyta':77,'rapidash':78,
    'slowpoke':79,'slowbro':80,
    'magnemite':81,'magneton':82,
    'farfetchd':83,"farfetch'd":83,
    'doduo':84,'dodrio':85,
    'seel':86,'dewgong':87,
    'grimer':88,'muk':89,
    'shellder':90,'cloyster':91,
    'gastly':92,'haunter':93,'gengar':94,
    'onix':95,'drowzee':96,'hypno':97,
    'krabby':98,'kingler':99,
    'voltorb':100,'electrode':101,
    'exeggcute':102,'exeggutor':103,
    'cubone':104,'marowak':105,
    'hitmonlee':106,'hitmonchan':107,
    'lickitung':108,'koffing':109,'weezing':110,
    'rhyhorn':111,'rhydon':112,
    'chansey':113,'tangela':114,'kangaskhan':115,
    'tangrowth': 465,
    'horsea':116,'seadra':117,
    'goldeen':118,'seaking':119,
    'staryu':120,'starmie':121,
    'mr. mime':122,'mr mime':122,
    'scyther':123,'jynx':124,
    'electabuzz':125,'magmar':126,'pinsir':127,'tauros':128,
    'magikarp':129,'gyarados':130,'lapras':131,'ditto':132,
    'eevee':133,'vaporeon':134,'jolteon':135,'flareon':136,
    'porygon':137,'omanyte':138,'omastar':139,
    'kabuto':140,'kabutops':141,'aerodactyl':142,'snorlax':143,
    'articuno':144,'zapdos':145,'moltres':146,
    'dratini':147,'dragonair':148,'dragonite':149,
    'mewtwo':150,'mew':151,
    // Especiales de TCG
    'crawdaunt':342,'crobat':169,'espeon':196,'umbreon':197,
    'blaziken':257,'swampert':260,'sceptile':254,
    'torchic':255,'mudkip':258,'treecko':252,
    'ralts':280,'kirlia':281,'gardevoir':282,
    // Johto (Gen 2)
  'chikorita':152,'bayleef':153,'meganium':154,
  'cyndaquil':155,'quilava':156,'typhlosion':157,
  'totodile':158,'croconaw':159,'feraligatr':160,
  'sentret':161,'furret':162,'hoothoot':163,'noctowl':164,
  'ledyba':165,'ledian':166,'spinarak':167,'ariados':168,
  'chinchou':170,'lanturn':171,'pichu':172,'cleffa':173,
  'igglybuff':174,'togepi':175,'togetic':176,'natu':177,'xatu':178,
  'mareep':179,'flaaffy':180,'ampharos':181,
  'bellossom':182,'marill':183,'azumarill':184,'sudowoodo':185,
  'politoed':186,'hoppip':187,'skiploom':188,'jumpluff':189,
  'aipom':190,'sunkern':191,'sunflora':192,'yanma':193,
  'wooper':194,'quagsire':195,'murkrow':198,'slowking':199,
  'misdreavus':200,'unown':201,'wobbuffet':202,'girafarig':203,
  'pineco':204,'forretress':205,'dunsparce':206,'gligar':207,
  'steelix':208,'snubbull':209,'granbull':210,
  'qwilfish':211,'scizor':212,'shuckle':213,'heracross':214,
  'sneasel':215,'teddiursa':216,'ursaring':217,
  'slugma':218,'magcargo':219,'swinub':220,'piloswine':221,
  'corsola':222,'remoraid':223,'octillery':224,'delibird':225,
  'mantine':226,'skarmory':227,'houndour':228,'houndoom':229,
  'kingdra':230,'phanpy':231,'donphan':232,'porygon2':233,
  'stantler':234,'smeargle':235,'tyrogue':236,'hitmontop':237,
  'smoochum':238,'elekid':239,'magby':240,'miltank':241,
  'blissey':242,'raikou':243,'entei':244,'suicune':245,
  'larvitar':246,'pupitar':247,'tyranitar':248,
  'lugia':249,'ho-oh':250,'celebi':251,

  // Hoenn (Gen 3) - Vi varios de estos en tu banca
  'poochyena':261,'mightyena':262,'zigzagoon':263,'linoone':264,
  'wurmple':265,'silcoon':266,'beautifly':267,'cascoon':268,'dustox':269,
  'lotad':270,'lombre':271,'ludicolo':272,'seedot':273,'nuzleaf':274,'shiftry':275,
  'taillow':276,'swellow':277,'wingull':278,'pelipper':279,
  'slakoth':287,'vigoroth':288,'slaking':289,
  'nincada':290,'ninjask':291,'shedinja':292,
  'whismur':293,'loudred':294,'exploud':295,
  'makuhita':296,'hariyama':297,'azurill':298,'nosepass':299,
  'skitty':300,'delcatty':301,'sableye':302,'mawile':303,
  'aron':304,'lairon':305,'aggron':306,'meditite':307,'medicham':308,
  'electrike':309,'manectric':310,'plusle':311,'minun':312,
  'volbeat':313,'illumise':314,'roselia':315,'gulpin':316,'swalot':317,
  'carvanha':318,'sharpedo':319,'wailmer':320,'wailord':321,
  'numel':322,'camerupt':323,'torkoal':324,'spoink':325,'grumpig':326,
  'spinda':327,'trapinch':328,'vibrava':329,'flygon':330,
  'cacnea':331,'cacturne':332,'swablu':333,'altaria':334,'zangoose':335,'seviper':336,
  'lunatone':337,'solrock':338,'barboach':339,'whiscash':340,'corphish':341,
  'lileep':345,'cradily':346,'anorith':347,'armaldo':348,
  'feebas':349,'milotic':350,'castform':351,'kecleon':352,
  'shuppet':353,'banette':354,'duskull':355,'dusclops':356,
  'tropius':357,'chimecho':358,'absol':359,'wynaut':360,
  'snorunt':361,'glalie':362,'spheal':363,'sealeo':364,'walrein':365,
  'clamperl':366,'huntail':367,'gorebyss':368,'relicanth':369,'luvdisc':370,
  'bagon':371,'shelgon':372,'salamence':373,'beldum':374,'metang':375,'metagross':376,
  'regirock':377,'regice':378,'registeel':379,'latias':380,'latios':381,
  'kyogre':382,'groudon':383,'rayquaza':384,'jirachi':385,'deoxys':386,

  // Sinnoh (Gen 4) - ¡Para que Dialga no se rompa!
  'turtwig':387,'grotle':388,'torterra':389,
  'chimchar':390,'monferno':391,'infernape':392,
  'piplup':393,'prinplup':394,'empoleon':395,
  'starly':396,'staravia':397,'staraptor':398,
  'bidoof':399,'bibarel':400,'kricketot':401,'kricketune':402,
  'shinx':403,'luxio':404,'luxray':405,'cranidos':408,'rampardos':409,
  'shieldon':410,'bastiodon':411,'combee':415,'vespiquen':416,
  'pachirisu':417,'buizel':418,'floatzel':419,'cherubi':420,'cherrim':421,
  'drifloon':425,'drifblim':426,'buneary':427,'lopunny':428,
  'honchkrow':430,'glameow':431,'purugly':432,'stunky':434,'skuntank':435,
  'bronzor':436,'bronzong':437,'gible':443,'gabite':444,'garchomp':445,
  'lucario':448,'riolu':447,'croagunk':453,'toxicroak':454,
  'weavile':461,'magnezone':462,'electivire':466,'magmortar':467,
  'leafeon':470,'glaceon':471,'gliscor':472,'mamoswine':473,
  'porygon-z':474,'gallade':475,'probopass':476,'dusknoir':477,'froslass':478,
  'rotom':479,'uxie':480,'mesprit':481,'azelf':482,
  'dialga':483,'palkia':484,'heatran':485,'regigigas':486,'giratina':487,
  'cresselia':488,'phione':489,'manaphy':490,'darkrai':491,'shaymin':492,'arceus':493,
  // Unova (Gen 5) - Muy comunes en TCG
  'victini':494,'snivy':495,'servine':496,'serperior':497,
  'tepig':498,'pignite':499,'emboar':500,
  'oshawott':501,'dewott':502,'samurott':503,
  'purrloin':509,'liepard':510,'munna':517,'musharna':518,
  'pidove':519,'tranquill':520,'unfezant':521,'blitzle':522,'zebstrika':523,
  'drilbur':529,'excadrill':530,'audino':531,'timburr':532,'gurdurr':533,'conkeldurr':534,
  'tympole':535,'palpitoad':536,'seismitoad':537,'throh':538,'sawk':539,
  'venipede':543,'whirlipede':544,'scolipede':545,'cottonee':546,'whimsicott':547,
  'petilil':548,'lilligant':549,'basculin':550,'sandile':551,'krokorok':552,'krookodile':553,
  'darumaka':554,'darmanitan':555,'maractus':556,'dwebble':557,'crustle':558,'scraggy':559,'scrafty':560,
  'sigilyph':561,'yamask':562,'cofagrigus':563,'tirtouga':564,'carracosta':565,'archen':566,'archeops':567,
  'trubbish':568,'garbodor':569,'zorua':570,'zoroark':571,'minccino':572,'cinccino':573,
  'gothita':574,'gothorita':575,'gothitelle':576,'solosis':577,'duosion':578,'reuniclus':579,
  'ducklett':580,'swanna':581,'vanillite':582,'vanillish':583,'vanilluxe':584,
  'deerling':585,'sawsbuck':586,'emolga':587,'karrablast':588,'escavalier':589,'foongus':590,'amoonguss':591,
  'frillish':592,'jellicent':593,'alomomola':594,'joltik':595,'galvantula':596,
  'ferroseed':597,'ferrothorn':598,'klink':599,'klang':600,'klinklang':601,
  'tynamo':602,'eelektrik':603,'eelektross':604,'elgyem':605,'beheeyem':606,
  'litwick':607,'lampent':608,'chandelure':609,'axew':610,'fraxure':611,'haxorus':612,
  'cubchoo':613,'beartic':614,'cryogonal':615,'shelmet':616,'accelgor':617,'stunfisk':618,
  'mienshao':620,'druddigon':621,'golett':622,'golurk':623,'pawniard':624,'bisharp':625,'bouffalant':626,
  'rufflet':627,'braviary':628,'vullaby':629,'mandibuzz':630,'heatmor':631,'durant':632,
  'deino':633,'zweilous':634,'hydreigon':635,'larvesta':636,'volcarona':637,
  'cobalion':638,'terrakion':639,'virizion':640,'tornadus':641,'thundurus':642,'reshiram':643,'zekrom':644,
  'landorus':645,'kyurem':646,'keldeo':647,'meloetta':648,'genesect':649,

  // Kalos (Gen 6)
  'chespin':650,'quilladin':651,'chesnaught':652,
  'fennekin':653,'braixen':654,'delphox':655,
  'froakie':656,'frogadier':657,'greninja':658,
  'fletchling':661,'fletchinder':662,'talonflame':663,
  'scatterbug':664,'spewpa':665,'vivillon':666,'litleo':667,'pyroar':668,
  'flabebe':669,'floette':670,'florges':671,'skiddo':672,'gogoat':673,
  'pancham':674,'pangoro':675,'furfrou':676,'espurr':677,'meowstic':678,
  'honedge':679,'doublade':680,'aegislash':681,'spritzee':682,'aromatisse':683,
  'swirlix':684,'slurpuff':685,'inkay':686,'malamar':687,'binacle':688,'barbaracle':689,
  'skrelp':690,'dragalge':691,'clauncher':692,'clawitzer':693,'helioptile':694,'heliolisk':695,
  'tyrunt':696,'tyrantrum':697,'amaura':698,'aurorus':699,'sylveon':700,
  'hawlucha':701,'dedenne':702,'carbink':703,'goomy':704,'sliggoo':705,'goodra':706,
  'klefki':707,'phantump':708,'trevenant':709,'pumpkaboo':710,'gourgeist':711,
  'bergmite':712,'avalugg':713,'noibat':714,'noivern':715,
  'xerneas':716,'yveltal':717,'zygarde':718,'diancie':719,'hoopa':720,'volcanion':721,

  // Alola (Gen 7) - Por si aparecen los GX
  'rowlet':722,'dartrix':723,'decidueye':724,
  'litten':725,'torracat':726,'incineroar':727,
  'popplio':728,'brionne':729,'primarina':730,
  'rockruff':744,'lycanroc':745,'mimikyu':778,'tapu koko':785,'tapu lele':786,'solgaleo':791,'lunala':792,
  'zeraora':807,'meltan':808,'melmetal':809,

  // Galar (Gen 8) - Por si tenés V o VMAX
  'grookey':810,'thackey':811,'rillaboom':812,
  'scorbunny':813,'raboot':814,'cinderace':815,
  'sobble':816,'drizzile':817,'inteleon':818,
  'zacian':888,'zamazenta':889,'eternatus':890,
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
    setTimeout(() => { this.boardVisible   = true;  this.cdr.detectChanges(); }, 2700);
    setTimeout(() => { this.showIntro      = false; this.cdr.detectChanges(); }, 3300);
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
  // Solo pedimos estado si es el turno del BOT y no estamos animando nada
  this.pollingPartida = setInterval(() => {
    if (this.partida?.turnoActual === 'BOT' && !this.cargandoAccion && !this.animandoAtaque) {
      this.cargarEstado();
    }
  }, 2000);
}

cargarEstado(): void {
  // Si no hay ID o estamos en medio de un ataque/carga, no pisamos el estado
  if (!this.matchId || this.cargandoAccion || this.animandoAtaque) return;

  this.battleService.getState(this.matchId).subscribe({
    next: (data) => {
      // Solo disparamos el detector de cambios si la data es distinta
      if (JSON.stringify(this.partida) !== JSON.stringify(data)) {
        const turnoAntes = this.partida?.turnoActual;
        this.partida = data;

        // Si el turno cambió, mostramos el cartelito
        if (turnoAntes && turnoAntes !== data.turnoActual) {
          if (data.turnoActual === 'JUGADOR') this.ataqueRealizado = false;
          this.mostrarTurnOverlay(data.turnoActual === 'JUGADOR' ? 'jugador' : 'bot');
        }
        this.cdr.detectChanges();
      }
    },
    error: (err) => console.error("Error cargando estado:", err)
  });
}

  // ─── Sprites ─────────────────────────────────────────────
  private normalizarNombre(nombre: string): string {
    if (!nombre) return '';
    return nombre.toLowerCase()
      .normalize('NFD').replace(/[\u0300-\u036f]/g, '')
      .trim();
  }

  private getPokemonNum(nombreCarta: string): number {
    const norm = this.normalizarNombre(nombreCarta);
    if (this.pokedexNum[norm]) return this.pokedexNum[norm];

    // Busca palabra por palabra (maneja "Team Aqua's Crawdaunt" → "crawdaunt")
    const palabras = norm.split(/[\s'']+/).reverse();
    for (const p of palabras) {
      if (this.pokedexNum[p]) return this.pokedexNum[p];
    }

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
    const max = this.getHpMax(pokemon);
    return Math.max(0, Math.min(100, (pokemon.hpActual / max) * 100));
  }

  getHpMax(pokemon: any): number {
    return pokemon?.hpMax || parseInt(pokemon?.card?.hp, 10) || 100;
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

  soltarCarta(event: CdkDragDrop<any[]>, zona: 'activo' | 'banca'): void {
    // Si la soltó en la misma mano, no hacemos nada
    if (event.previousContainer === event.container) return;

    // Obtenemos la carta que el jugador arrastró
    const cartaArrastrada = event.item.data;

    // Reutilizamos la lógica que ya tenés armada
    if (this.esEnergia(cartaArrastrada)) {
      this.gestionarUnionEnergia(cartaArrastrada);
    } else if (this.esPokemon(cartaArrastrada)) {
      // Acá podés afinar si la baja a la banca o al activo dependiendo de la 'zona'
      this.jugarCarta(cartaArrastrada); 
    }
  }

  /**
   * Secuencia de ataque coordinada:
   * 1. Sprite jugador se lanza hacia arriba (350ms)
   * 2. Flash de impacto blanco (400ms)
   * 3. Sprite bot vibra + se ilumina de rojo (400-900ms)
   * 4. Todo vuelve a normal, se actualiza el estado (900ms)
   */
 atacar(): void {
  if (!this.puedeAtacar() || this.cargandoAccion) return;

  this.cargandoAccion = true;
  this.ataqueRealizado = true; // Bloqueamos el botón de ataque inmediatamente

  this.battleService.atacar(this.matchId!).subscribe({
    next: () => {
      // FASE 1: El sprite salta
      this.animandoAtaque = true;
      this.cdr.detectChanges();

      // FASE 2: Flash de impacto (en el milisegundo 380)
      setTimeout(() => {
        this.showImpactFlash = true;
        this.cdr.detectChanges();
        setTimeout(() => { this.showImpactFlash = false; this.cdr.detectChanges(); }, 200);
      }, 380);

      // FASE 3: El Bot vibra y se pone rojo
      setTimeout(() => {
        this.animandoBotAtaque = true;
        this.vibrarBot = true;
        this.cdr.detectChanges();
      }, 430);

      // FASE 4: Limpieza y actualización REAL
      setTimeout(() => {
        this.animandoAtaque = false;
        this.animandoBotAtaque = false;
        this.vibrarBot = false;
        this.cargandoAccion = false;
        
        // Pedimos el estado final después de que la animación terminó
        this.cargarEstado(); 
      }, 950);
    },
    error: (err) => {
      this.cargandoAccion = false;
      this.ataqueRealizado = false;
      alert('Error al atacar: ' + (err?.error ?? 'No hay conexión'));
    }
  });
}

  // En tu playmat.component.ts
get manoAgrupada(): any[][] {
  const tamañoStack = 4; // Agrupamos de a 4 cartas
  const mano = this.partida.jugador.mano;
  const stacks = [];

  for (let i = 0; i < mano.length; i += tamañoStack) {
    stacks.push(mano.slice(i, i + tamañoStack));
  }
  
  return stacks;
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