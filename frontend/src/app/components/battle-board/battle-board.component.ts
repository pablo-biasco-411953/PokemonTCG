import { Component, OnInit, OnDestroy, ChangeDetectorRef,HostListener } from '@angular/core';
import { CommonModule } from '@angular/common';
import { BattleService } from '../../services/battle.service';
import { Router, ActivatedRoute } from '@angular/router';
import { DragDropModule, CdkDragDrop } from '@angular/cdk/drag-drop';
import { firstValueFrom } from 'rxjs';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';

@Component({
  selector: 'app-battle-board',
  templateUrl: './battle-board.component.html',
  styleUrls: ['./battle-board.component.scss'],
  standalone: true,
  imports: [CommonModule, DragDropModule]
  
})
export class BattleBoardComponent implements OnInit, OnDestroy {
  public Math = Math;
  matchId: string | null = null;
  partida: any = null;
  jugadorNombre = '';
  // Gesto moneda
  private yStart = 0;
  private yEnd = 0;
  public lanzada = false;
hoveredCard: any = null;
  hoveredCardStatuses: any[] = [];
  hoveredCardList: any[] = []; // La lista de cartas actual (ej: tu mano)
  hoveredCardIndex: number = -1; // En qué posición de la lista estás
  // Tracking de cartas nuevas (jugador)
  public cartasNuevas = new Set<string>();
  private manoAnteriorIds = new Set<string>();

  // Tracking de cartas nuevas (bot)
  public cartasNuevasBot = new Set<string>();
  private manoAnteriorIdsBot = new Set<string>();
healedTextPlayer: string | null = null;
  curingParalysisPlayer: boolean = false;
  curingSleepPlayer: boolean = false;

  healedTextBot: string | null = null;
  curingParalysisBot: boolean = false;
  curingSleepBot: boolean = false;
  // Estado visual
  vibrarBot         = false;
  cargandoAccion    = false;
  boardVisible      = false;
  showIntro         = true;
  introFadingOut    = false;
  showTurnOverlay   = false;
  tiempoTurnoMaximo: number = 60;
  tiempoRestante: number = 60;
  porcentajeTimer: number = 100;
  timerInterval: any;
  botPensando: boolean = false;
  esperandoMiNuevoTurno: boolean = false;
  turnoOverlayTipo: 'jugador' | 'bot' = 'jugador';
  public modoSeleccionRetirada = false;

  // Animaciones y Panel
  animandoAtaque       = false;
  public animandoBotAtaque = false;
  showImpactFlash      = false;
  showHabilidadesPanel = false;

  isDraggingEnergy  = false;
  originPos         = { x: 0, y: 0 };
  mousePos          = { x: 0, y: 0 };
  selectedEnergyId: string | null = null;

  private ataqueRealizado = false;
  private pollingPartida: any;
  public botEstaAtacando = false;
  private datosPendientesBot: any = null;
  animandoEvolucionId: string | null = null;
  // Variables internas
  public activoVisualJugador: any = null;
  public hpRenderJugador: number = 0;
  public bloqueadoPorAnimacion: boolean = false;
  public anguloFinal: number = 0; 
  isScrollingMode: boolean = false;
  scrollTimeout: any;
  lastScrollTime: number = 0;
  private intentosBotSinAccion = 0;
  private ultimaCantidadCartasBot = -1;
  private ciclosSinCambio = 0;
  private hpVisualInterno: number = 0;
mostrarModalDescarte: boolean = false;
  cartasParaVerEnDescarte: any[] = [];
  tituloDescarteActual: string = '';


puedeEvolucionar(cartaMano: any): boolean {
    if (!cartaMano.evolvesFrom || !this.partida) return false;
    
    // ¿Está en el activo?
    if (this.partida.jugador?.activo?.card?.nombre === cartaMano.evolvesFrom) return true;
    
    // ¿Está en la banca?
    return this.partida.jugador?.banca?.some((b: any) => b.card.nombre === cartaMano.evolvesFrom);
  }

  // CoinFlip
  public estadoCoinFlip: 'ELEGIR_LADO' | 'ESPERANDO_TIRO' | 'GIRANDO' | 'ELEGIR_TURNO' | 'RESULTADO_BOT' | 'OCULTO' = 'OCULTO';
  eleccionJugador: 'CARA' | 'CRUZ' = 'CARA';
  resultadoMoneda: 'CARA' | 'CRUZ' = 'CARA';
  public girando: boolean = false;

  // ── Estado de efectos ──
  mostrarAuraCuracionBot    = false;
  mostrarAuraCuracionPlayer = false;
  mostrarKO                 = false;
 
  // ── Números de daño flotantes ──
  damageNumberBot:    { valor: number; esCuracion: boolean } | null = null;
  damageNumberPlayer: { valor: number; esCuracion: boolean } | null = null;


  // Partículas
  mostrarEfectoBot     = false;
  mostrarEfectoJugador = false;
  particulasBot:     any[] = [];
  particulasJugador: any[] = [];
  animandoBotDanio     = false;
  animandoJugadorDanio = false;

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
    'tangrowth':465,
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
    'crawdaunt':342,'crobat':169,'espeon':196,'umbreon':197,
    'blaziken':257,'swampert':260,'sceptile':254,
    'torchic':255,'mudkip':258,'treecko':252,
    'ralts':280,'kirlia':281,'gardevoir':282,
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
    'rowlet':722,'dartrix':723,'decidueye':724,
    'litten':725,'torracat':726,'incineroar':727,
    'popplio':728,'brionne':729,'primarina':730,
    'rockruff':744,'lycanroc':745,'mimikyu':778,'tapu koko':785,'tapu lele':786,'solgaleo':791,'lunala':792,
    'zeraora':807,'meltan':808,'melmetal':809,
    'grookey':810,'thackey':811,'rillaboom':812,
    'scorbunny':813,'raboot':814,'cinderace':815,
    'sobble':816,'drizzile':817,'inteleon':818,
    'zacian':888,'zamazenta':889,'eternatus':890,
  };

  constructor(
    private battleService: BattleService,
    private route: ActivatedRoute,
    private router: Router,
    private cdr: ChangeDetectorRef,
    private sanitizer: DomSanitizer 
  ) {}

  // ═══════════════════════════════════════════════
  // LIFECYCLE
  // ═══════════════════════════════════════════════

  ngOnInit(): void {
    this.matchId = this.route.snapshot.paramMap.get('id');
    if (!this.matchId) return;
    this.cargarCatalogoGodMode();
    this.showIntro = true;
    setTimeout(() => this.introFadingOut = true, 2000);
    setTimeout(() => this.showIntro = false, 3000);

    this.battleService.getState(this.matchId).subscribe({
      next: (data) => {
        if (data.faseActual === 'LANZAMIENTO_MONEDA') {
          setTimeout(() => {
            this.estadoCoinFlip = 'ELEGIR_LADO';
            this.cdr.detectChanges();
          }, 3200);
        } else {
          this.partida = data;
          // Inicializamos los sets para que en el primer cargarEstado no marquemos todo como "nuevo"
          this.manoAnteriorIds    = new Set((data.jugador?.mano || []).map((c: any) => c.id));
          this.manoAnteriorIdsBot = new Set((data.bot?.mano    || []).map((c: any) => c.id));
          this.finalizarCoinFlip();
        }
      }
    });
  }

  ngOnDestroy(): void {
    if (this.pollingPartida) clearInterval(this.pollingPartida);
  }

  // ═══════════════════════════════════════════════
  // COIN FLIP
  // ═══════════════════════════════════════════════

  iniciarSorteo(eleccion: 'CARA' | 'CRUZ') {
    this.eleccionJugador = eleccion;
    this.estadoCoinFlip  = 'ESPERANDO_TIRO';
    this.lanzada         = false;
    this.cdr.detectChanges();
  }

  async seleccionarTurno(yoVoyPrimero: boolean) {
    try {
      await firstValueFrom(this.battleService.elegirTurno(this.matchId!, yoVoyPrimero));
      this.finalizarCoinFlip();
    } catch (error) {
      console.error('Error al elegir turno:', error);
      this.finalizarCoinFlip();
    }
  }

 iniciarRelojTurno() {
    this.detenerRelojTurno();
    this.tiempoRestante = this.tiempoTurnoMaximo;
    this.porcentajeTimer = 100;

    this.timerInterval = setInterval(() => {
      if (this.partida?.turnoActual !== 'JUGADOR' || this.cargandoAccion || this.bloqueadoPorAnimacion) {
         return;
      }

      this.tiempoRestante--;
      this.porcentajeTimer = (this.tiempoRestante / this.tiempoTurnoMaximo) * 100;

      // 🚩 CLAVE: Forzamos a Angular a actualizar la barra y el texto
      this.cdr.detectChanges();

      if (this.tiempoRestante <= 0) {
        this.ejecutarTimeOut();
      }
    }, 1000);
  }

  detenerRelojTurno() {
    if (this.timerInterval) {
      clearInterval(this.timerInterval);
      this.timerInterval = null;
    }
  }

async ejecutarTimeOut() {
    this.detenerRelojTurno();
    console.log("🔥 ¡TIEMPO AGOTADO! Pasando turno automáticamente...");
    await this.pasarTurno(); 
  }


  finalizarCoinFlip() {
    this.estadoCoinFlip = 'OCULTO';
    this.lanzada        = false;
    this.girando        = false;
    this.boardVisible   = true;
    this.cargarEstado();
    this.cdr.detectChanges();
  }

  onMouseDown(event: MouseEvent) {
    this.yStart = event.clientY;
  }

   interTurnOverlay: {
    titulo: string;
    subtitulo?: string;
    fase: string;
    tipo: 'jugador' | 'bot' | 'neutral';
    duracion: number;
  } | null = null;
 
  // ── Coin flip de ataque ──
  coinFlipAtaque: {
    nombreAtaque: string;
    descripcion: string;
    cantidadMonedas: number;
    danioBase: number;
    danioExtraPorCara: number;
    monedas: { estado: 'girando' | 'cara' | 'cruz' }[];
    danioTotal: number;
    terminado: boolean;
    progreso: number;
    esSoloEstado: boolean; // <-- ACÁ SOLO EL TIPO, SIN " = config..."
  } | null = null;

  async mostrarInterTurn(
    tipo: 'jugador' | 'bot' | 'neutral',
    titulo: string,
    subtitulo = '',
    duracion  = 2000
  ): Promise<void> {
    const fase = tipo === 'jugador' ? 'INICIO DE TURNO' :
                 tipo === 'bot'     ? 'TURNO DEL RIVAL'  : 'ENTRE TURNOS';
 
    this.interTurnOverlay = { titulo, subtitulo, fase, tipo, duracion };
    this.cdr.detectChanges();
 
    await this.delay(duracion);
 
    // Fade out
    this.interTurnOverlay = null;
    this.cdr.detectChanges();
 
    // Pequeña pausa de limpieza
    await this.delay(200);
  }


async mostrarKOAnim(): Promise<void> {
    this.mostrarKO = true;
    this.cdr.detectChanges();
    await this.delay(1200);
    this.mostrarKO = false;
    this.cdr.detectChanges();
  }
 async mostrarCuracion(objetivo: 'bot' | 'jugador', duracion = 1500): Promise<void> {
    if (objetivo === 'bot')     this.mostrarAuraCuracionBot    = true;
    else                        this.mostrarAuraCuracionPlayer = true;
    this.cdr.detectChanges();
 
    await this.delay(duracion);
 
    if (objetivo === 'bot')     this.mostrarAuraCuracionBot    = false;
    else                        this.mostrarAuraCuracionPlayer = false;
    this.cdr.detectChanges();
  }

   mostrarDamageNumber(objetivo: 'bot' | 'jugador', valor: number, esCuracion = false): void {
    const num = { valor, esCuracion };
    if (objetivo === 'bot') {
      this.damageNumberBot = num;
      setTimeout(() => { this.damageNumberBot = null; this.cdr.detectChanges(); }, 1000);
    } else {
      this.damageNumberPlayer = num;
      setTimeout(() => { this.damageNumberPlayer = null; this.cdr.detectChanges(); }, 1000);
    }
    this.cdr.detectChanges();
  }


  async onMouseUp(event: MouseEvent) {
    if (this.lanzada || this.estadoCoinFlip !== 'ESPERANDO_TIRO') return;

    this.yEnd = event.clientY;
    const diferencia = this.yStart - this.yEnd;
    const fuerza = Math.min(Math.max(diferencia, 50), 400);

    if (fuerza > 50) {
      this.lanzada         = true;
      this.girando         = true;
      this.estadoCoinFlip  = 'GIRANDO';

      const duracionVuelo = 1.8;
      const vueltasBase   = 5 + Math.floor(fuerza / 50);

      document.documentElement.style.setProperty('--altura-vuelo',   `-${fuerza * 1.3}px`);
      document.documentElement.style.setProperty('--duracion-vuelo', `${duracionVuelo}s`);

      this.cdr.detectChanges();

      try {
        const salioCara      = await firstValueFrom(this.battleService.lanzarMoneda(this.matchId!));
        this.resultadoMoneda = salioCara ? 'CARA' : 'CRUZ';

        this.anguloFinal = this.resultadoMoneda === 'CARA'
          ? vueltasBase * 360
          : vueltasBase * 360 + 180;

        await this.delay(duracionVuelo * 1000 - 300);
        this.girando = false;
        this.cdr.detectChanges();

        await this.delay(1200);

        if (this.eleccionJugador === this.resultadoMoneda) {
          this.estadoCoinFlip = 'ELEGIR_TURNO';
        } else {
          this.estadoCoinFlip = 'RESULTADO_BOT';
          this.cdr.detectChanges();
          await this.delay(2000);
          await firstValueFrom(this.battleService.elegirTurno(this.matchId!, false));
          this.finalizarCoinFlip();
        }
      } catch (e) {
        console.error('Error en sorteo:', e);
        this.finalizarCoinFlip();
      }
      this.cdr.detectChanges();
    }
  }

clearHoveredCard() {
    if (this.isScrollingMode) return; 

    this.hoveredCard = null;
    this.hoveredCardStatuses = [];
    this.hoveredCardList = [];
    this.hoveredCardIndex = -1;
  }

// 3. 🖱️ LA MAGIA DE LA RUEDITA (Solo para la mano)
  @HostListener('window:wheel', ['$event'])
  onScrollCard(event: WheelEvent) {
    if (this.hoveredCard && this.hoveredCardList === this.partida?.jugador?.mano && this.hoveredCardList.length > 1) {
      event.preventDefault();

      const now = Date.now();

      // 🚩 A. Bloqueamos el mouse físico temporalmente
      this.isScrollingMode = true;
      clearTimeout(this.scrollTimeout);
      this.scrollTimeout = setTimeout(() => {
        this.isScrollingMode = false; // Se desbloquea 200ms después de dejar de girar
      }, 200);

      // 🚩 B. Freno de velocidad: Solo permite 1 salto de carta cada 120 milisegundos
      if (now - this.lastScrollTime < 120) return;
      this.lastScrollTime = now;

      // C. Lógica de cambio de índice (Igual que antes)
      if (event.deltaY > 0) {
        this.hoveredCardIndex = (this.hoveredCardIndex + 1) % this.hoveredCardList.length;
      } else if (event.deltaY < 0) {
        this.hoveredCardIndex = (this.hoveredCardIndex - 1 + this.hoveredCardList.length) % this.hoveredCardList.length;
      }

      // D. Actualizamos el panel
      const nextItem = this.hoveredCardList[this.hoveredCardIndex];
      const cartaReal = nextItem.card ? nextItem.card : nextItem;
      
      this.hoveredCard = cartaReal;
      this.hoveredCardStatuses = this.extraerGlosario(cartaReal);
    }
  }

  // 4. 🎯 CLICK CENTRAL (Ruedita) PARA JUGAR LA CARTA
  @HostListener('window:mousedown', ['$event'])
  onMiddleClick(event: MouseEvent) {
    // event.button === 1 significa que tocaste la ruedita (el botón del medio)
    if (event.button === 1 && this.hoveredCard && this.hoveredCardList === this.partida?.jugador?.mano) {
      event.preventDefault(); // Evita que salga la crucecita molesta de Windows
      
      console.log("🖱️ ¡Click central! Jugando carta:", this.hoveredCard.nombre);
      this.jugarCarta(this.hoveredCard); // Juega la carta que estás viendo en el panel grande
    }
  }


showDebugPanel: boolean = false;
  fps: number = 0;
  memoryUsage: string = 'N/A';
  
  private frameCount = 0;
  private lastTime = performance.now();
  private animFrameId: number | null = null;

  // 🛠️ DETECTOR DE TECLA F3
  @HostListener('window:keydown', ['$event'])
  onKeyDown(event: KeyboardEvent) {
    if (event.key === 'F3') {
      event.preventDefault();
      this.showDebugPanel = !this.showDebugPanel;
      
      // Prendemos o apagamos el medidor para no gastar recursos si está cerrado
      if (this.showDebugPanel) {
        this.iniciarMedidorRendimiento();
      } else {
        this.detenerMedidorRendimiento();
      }
    }
  }

  // 🚀 LÓGICA DE RENDIMIENTO (FPS Y MEMORIA)
  iniciarMedidorRendimiento() {
    const loop = () => {
      const now = performance.now();
      this.frameCount++;
      
      // Actualizamos las métricas cada 1 segundo (1000ms)
      if (now >= this.lastTime + 1000) {
        this.fps = Math.round((this.frameCount * 1000) / (now - this.lastTime));
        this.frameCount = 0;
        this.lastTime = now;

        // Intentar leer memoria (API específica de navegadores basados en Chromium)
        const mem = (performance as any).memory;
        if (mem) {
          this.memoryUsage = (mem.usedJSHeapSize / 1048576).toFixed(2) + ' MB';
        }
        
        // Forzamos que Angular pinte estos numeritos sin afectar la partida
        this.cdr.detectChanges(); 
      }
      this.animFrameId = requestAnimationFrame(loop);
    };
    loop();
  }

  detenerMedidorRendimiento() {
    if (this.animFrameId) {
      cancelAnimationFrame(this.animFrameId);
      this.animFrameId = null;
    }
  }

async procesarCheckupEstados(estadoFinal: any) {
  const estadoAntiguo = this.partida;
  const activoJugadorAntiguo = estadoAntiguo?.jugador?.activo;
  const activoJugadorNuevo = estadoFinal?.jugador?.activo;

  // 😴 CASO: Estaba Dormido y el server lanzó moneda
  if (activoJugadorAntiguo?.condicionesEspeciales?.includes('Asleep')) {
    
    const sigueDormido = activoJugadorNuevo?.condicionesEspeciales?.includes('Asleep');
    const resultadoCara = !sigueDormido; // Si ya no está dormido, es porque salió CARA
    
    console.log("🎲 Lanzando moneda de despertar...");
    
    // Usamos tu sistema de monedas existente
    // Creamos un "ataque ficticio" para el texto del overlay
    const configFicticia = { 
        cantidadMonedas: 1, 
        esSoloEstado: true, 
        danioBase: 0, 
        danioExtraPorCara: 0 
    };

    await this.animarMonedasSincronizadas("¿Se despierta?", configFicticia, resultadoCara ? 1 : 0, true);
    
    if (resultadoCara) {
      console.log("✨ ¡Se despertó!");
      // Podés disparar un sonido de "Ding!" o una animación de chispitas
    } else {
      console.log("💤 Sigue roncando...");
    }
  }
}

extraerGlosario(carta: any): any[] {
    const statuses = [];
    // Juntamos todo el texto de la carta para analizarlo
    const fullText = (carta.ataques || []).map((a: any) => a.texto || '').join(' ').toLowerCase();

    if (fullText.includes('paralyz') || fullText.includes('paraliz')) {
      statuses.push({ nombre: 'Paralizado', css: 'kw-paralyze', desc: 'El Pokémon no puede atacar ni retirarse este turno.' });
    }
    if (fullText.includes('poison') || fullText.includes('envene')) {
      statuses.push({ nombre: 'Envenenado', css: 'kw-poison', desc: 'Recibe 10 de daño entre turnos.' });
    }
    if (fullText.includes('asleep') || fullText.includes('dormi') || fullText.includes('duerm')) {
      statuses.push({ nombre: 'Dormido', css: 'kw-sleep', desc: 'No puede atacar ni retirar. Lanza moneda al final del turno para despertar.' });
    }
    if (fullText.includes('confus') || fullText.includes('confund')) {
      statuses.push({ nombre: 'Confundido', css: 'kw-confuse', desc: 'Lanza moneda al atacar. Si es cruz, falla y recibe 30 de daño.' });
    }
    if (fullText.includes('burn') || fullText.includes('quem')) {
      statuses.push({ nombre: 'Quemado', css: 'kw-burn', desc: 'Recibe 20 de daño entre turnos. Lanza moneda para curarse.' });
    }
    
    return statuses;
  }

formatTextoAtaque(texto: string): SafeHtml {
    if (!texto) return '';
    let f = texto;
    
    // Reemplaza las palabras por un <span> con la clase del color correspondiente
    f = f.replace(/(paralyzed|paralyzes|paraliza|paralizado)/gi, '<span class="kw-paralyze">$&</span>');
    f = f.replace(/(poisoned|poisons|envenena|envenenado)/gi, '<span class="kw-poison">$&</span>');
    f = f.replace(/(asleep|sleeps|duerme|dormido)/gi, '<span class="kw-sleep">$&</span>');
    f = f.replace(/(confused|confuses|confunde|confundido)/gi, '<span class="kw-confuse">$&</span>');
    f = f.replace(/(burned|burns|quema|quemado)/gi, '<span class="kw-burn">$&</span>');
    f = f.replace(/(does nothing|no hace nada)/gi, '<span class="kw-neutral">$&</span>');

    // Le decimos a Angular que este HTML es seguro de renderizar
    return this.sanitizer.bypassSecurityTrustHtml(f);
  }

setHoveredCard(item: any, list: any[] = [], index: number = -1) {
    // 🚩 FIX: Si estamos girando la ruedita, ignoramos los choques físicos del mouse
    if (this.isScrollingMode) return; 

    if (!item) {
      this.clearHoveredCard();
      return;
    }
    const cartaReal = item.card ? item.card : item;
    this.hoveredCard = cartaReal;
    this.hoveredCardStatuses = this.extraerGlosario(cartaReal);
    this.hoveredCardList = list;
    this.hoveredCardIndex = index;
  }

async ejecutarCoinFlipAtaque(
    nombreAtaque: string,
    descripcion: string,
    cantidadMonedas: number,
    danioBase: number,
    danioExtraPorCara: number,
    esSoloEstado: boolean // 🚩 1. Agregamos el parámetro acá
  ): Promise<number> {
    // Inicializamos el estado del coin flip
    this.coinFlipAtaque = {
      nombreAtaque,
      descripcion,
      cantidadMonedas,
      danioBase,
      danioExtraPorCara,
      monedas: Array(cantidadMonedas).fill(null).map(() => ({ estado: 'girando' as const })),
      danioTotal: 0,
      terminado: false,
      progreso: 0,
      esSoloEstado: esSoloEstado // 🚩 2. Usamos el parámetro que entra, NO 'config'
    };

    this.cdr.detectChanges();
    // Pequeña pausa para que el overlay aparezca
    await this.delay(600);
 
    let caras = 0;
 
    // Lanzamos cada moneda con un delay entre ellas
    for (let i = 0; i < cantidadMonedas; i++) {
      // Progreso de la barra
      this.coinFlipAtaque!.progreso = ((i + 1) / cantidadMonedas) * 100;
      this.cdr.detectChanges();
 
      // Delay de "giro" antes de revelar
      await this.delay(600 + Math.random() * 400);
 
      // Resultado aleatorio
      const esCara = Math.random() < 0.5;
      if (esCara) caras++;
 
      this.coinFlipAtaque!.monedas[i].estado = esCara ? 'cara' : 'cruz';
      this.cdr.detectChanges();
 
      // Pausa entre monedas
      await this.delay(300);
    }
 
    // Calculamos el daño total
    const danioTotal = danioBase + (caras * danioExtraPorCara);
    this.coinFlipAtaque!.danioTotal = danioTotal;
    this.coinFlipAtaque!.terminado  = true;
    this.cdr.detectChanges();
 
    // Mostramos el resultado un momento
    await this.delay(2000);
 
    // Cerramos el overlay
    this.coinFlipAtaque = null;
    this.cdr.detectChanges();
 
    return danioTotal;
  }


detectarCoinFlipAtaque(ataque: any): {
    cantidadMonedas: number;
    danioBase: number;
    danioExtraPorCara: number;
    descripcion: string;
    esSoloEstado: boolean; // 🚩 Nueva bandera
  } | null {
    if (!ataque?.texto && !ataque?.descripcion && !ataque?.efecto) return null;

    const texto: string = (ataque.texto || ataque.descripcion || ataque.efecto || '').toLowerCase();

    const flipMatch = texto.match(/flip\s+(\d+|a|an|one|two|three|four|five)\s+coin|lanz[aá]\s+(\d+|una?)\s+moneda/i);
    if (!flipMatch) return null;

    const numStr = (flipMatch[1] || flipMatch[2] || 'a').toLowerCase();
    const numMap: Record<string, number> = { a:1, an:1, one:1, una:1, 'un':1, '1':1, two:2, dos:2, '2':2, three:3, tres:3, '3':3 };
    const cantidadMonedas = (numMap[numStr] ?? parseInt(numStr, 10)) || 1; 

    let danioBase = parseInt(ataque.danio || ataque.dano || '0', 10) || 0;
    let danioExtraPorCara = 0;
    let esMultiplicador = false;
    let esFalloCruz = false;
    let esSoloEstado = false;

    // 🕵️‍♂️ Lógica de detección mejorada
    if (texto.includes('paralyzed') || texto.includes('asleep') || texto.includes('confused') || texto.includes('poisoned')) {
        // Si el daño no depende de la moneda (como Dratini), es solo efecto de estado
        if (!texto.includes('more damage') && !texto.includes('damage times')) {
            esSoloEstado = true;
        }
    }

    if (texto.includes('does nothing')) {
        esFalloCruz = true;
        danioExtraPorCara = danioBase;
        danioBase = 0; 
    } else if (texto.includes('times the number of heads') || texto.includes('x the number of heads') || texto.includes('for each heads')) {
        esMultiplicador = true;
        const multiMatch = texto.match(/does (\d+) damage times/i);
        danioExtraPorCara = multiMatch ? parseInt(multiMatch[1], 10) : (danioBase > 0 ? danioBase : 10);
        danioBase = 0; 
    } else if (texto.includes('more damage') || texto.includes('additional damage')) {
        const damageMatch = texto.match(/(\d+)\s*(?:more|extra|additional)/i);
        danioExtraPorCara = damageMatch ? parseInt(damageMatch[1], 10) : 10;
    }

    const descripcion = this.traducirEfectoCoinFlip(texto, cantidadMonedas, danioExtraPorCara, esMultiplicador, esFalloCruz, esSoloEstado);

    return { cantidadMonedas, danioBase, danioExtraPorCara, descripcion, esSoloEstado };
  }

 async procesarEventosPostEstado(estadoAnterior: any, estadoNuevo: any): Promise<void> {
    // ── KO del Pokémon del bot ──
    const botActivoAntes  = estadoAnterior?.bot?.activo;
    const botActivoAhora  = estadoNuevo?.bot?.activo;
    if (botActivoAntes && !botActivoAhora) {
      await this.mostrarKOAnim();
    }

    // ── KO del Pokémon del jugador ──
    const playerActivoAntes = estadoAnterior?.jugador?.activo;
    const playerActivoAhora = estadoNuevo?.jugador?.activo;
    if (playerActivoAntes && !playerActivoAhora) {
      await this.mostrarKOAnim();
    }

    // ── Daño al bot ──
    const hpBotAntes  = botActivoAntes?.hpActual  ?? 0;
    const hpBotAhora  = botActivoAhora?.hpActual  ?? 0;
    if (botActivoAntes && botActivoAhora && hpBotAhora < hpBotAntes) {
      this.mostrarDamageNumber('bot', hpBotAntes - hpBotAhora);
    }

    // ── Curación del bot ──
    if (botActivoAntes && botActivoAhora && hpBotAhora > hpBotAntes) {
      const curado = hpBotAhora - hpBotAntes;
      this.mostrarDamageNumber('bot', curado, true);
      this.mostrarCuracion('bot');
    }

    // ── Daño al jugador ──
    const hpPlayerAntes = playerActivoAntes?.hpActual ?? 0;
    const hpPlayerAhora = playerActivoAhora?.hpActual ?? 0;
    if (playerActivoAntes && playerActivoAhora && hpPlayerAhora < hpPlayerAntes) {
      this.mostrarDamageNumber('jugador', hpPlayerAntes - hpPlayerAhora);
    }

    // ── Curación del jugador ──
    if (playerActivoAntes && playerActivoAhora && hpPlayerAhora > hpPlayerAntes) {
      const curado = hpPlayerAhora - hpPlayerAntes;
      this.mostrarDamageNumber('jugador', curado, true);
      this.mostrarCuracion('jugador');
    }
  }
 

debugFullCatalog: any[] = [];
  debugFilteredCatalog: any[] = [];
  debugSelectedIndex: number = 0;

  // Criterios de búsqueda
  debugSearchText: string = '';
  debugSearchSupertype: string = '';

  // 🚩 ACORDATE DE LLAMAR A ESTE MÉTODO EN TU ngOnInit()
cargarCatalogoGodMode() {
    this.battleService.getCardCatalogDebug().subscribe({
      next: (cartas) => {
        this.debugFullCatalog = cartas;
        
        // 🚩 AGREGÁ ESTA LÍNEA PARA ESPIAR EL DATO:
        console.log("🕵️‍♂️ Primera carta que llegó:", cartas[0]);

        this.aplicarFiltrosDebug();
        console.log(`🔌 God Mode: Catálogo cargado con ${cartas.length} cartas.`);
      },
      error: (err) => console.error("❌ Error cargando catálogo de God Mode:", err)
    });
  }

  actualizarFiltroTexto(event: any) {
    this.debugSearchText = event.target.value.toLowerCase();
    this.aplicarFiltrosDebug();
  }

  actualizarFiltroTipo(event: any) {
    this.debugSearchSupertype = event.target.value;
    this.aplicarFiltrosDebug();
  }

  aplicarFiltrosDebug() {
    this.debugFilteredCatalog = this.debugFullCatalog.filter(c => {
      // 1. Filtro por Tipo (Pokémon, Trainer, Energy)
      const matchTipo = !this.debugSearchSupertype || c.supertype === this.debugSearchSupertype;
      
      // 2. Filtro por Texto (Busca en el nombre y en el texto de los ataques/efectos)
      let matchTexto = true;
      if (this.debugSearchText) {
        const nombreMatch = c.nombre?.toLowerCase().includes(this.debugSearchText);
        
        // Buscamos dentro de los ataques por palabras clave (ej: "asleep", "draw")
        const ataquesMatch = c.ataques?.some((atk: any) => 
          atk.nombre?.toLowerCase().includes(this.debugSearchText) ||
          atk.texto?.toLowerCase().includes(this.debugSearchText)
        );
        
        matchTexto = nombreMatch || ataquesMatch;
      }

      return matchTipo && matchTexto;
    });
    
    this.debugSelectedIndex = 0; // Reseteamos el carrusel al primer resultado
  }

  get debugSelectedCard(): any {
    if (!this.debugFilteredCatalog || this.debugFilteredCatalog.length === 0) return null;
    return this.debugFilteredCatalog[this.debugSelectedIndex];
  }

  nextDebugCard() {
    if (this.debugFilteredCatalog.length === 0) return;
    this.debugSelectedIndex = (this.debugSelectedIndex + 1) % this.debugFilteredCatalog.length;
  }

  prevDebugCard() {
    if (this.debugFilteredCatalog.length === 0) return;
    this.debugSelectedIndex = (this.debugSelectedIndex - 1 + this.debugFilteredCatalog.length) % this.debugFilteredCatalog.length;
  }
private traducirEfectoCoinFlip(
    textoOriginal: string,
    monedas: number,
    danio: number,
    esMultiplicador: boolean,
    esFalloCruz: boolean,
    esSoloEstado: boolean
  ): string {
    const numStr = monedas === 1 ? 'una moneda' : `${monedas} monedas`;
    
    if (esSoloEstado) {
        if (textoOriginal.includes('paralyzed')) return `Lanzá ${numStr}. Si sale CARA, el rival queda Paralizado.`;
        if (textoOriginal.includes('asleep')) return `Lanzá ${numStr}. Si sale CARA, el rival queda Dormido.`;
        return `Lanzá ${numStr} para aplicar un efecto especial.`;
    }
    
    if (esFalloCruz) return `Lanzá ${numStr}. Si sale CRUZ, el ataque falla.`;
    if (esMultiplicador) return `Lanzá ${numStr}. Hace ${danio} de daño por cada CARA.`;
    
    return `Lanzá ${numStr}. Hace ${danio} de daño extra por cada CARA.`;
  }
  // ═══════════════════════════════════════════════
  // CARGA DE ESTADO
  // ═══════════════════════════════════════════════

 cargarEstado(): void {
if (!this.matchId || this.bloqueadoPorAnimacion || this.botEstaAtacando || this.botPensando) return;
    this.battleService.getState(this.matchId).subscribe({
      next: (data) => {
        if (this.bloqueadoPorAnimacion || this.botEstaAtacando) {
          console.log('🛡️ Polling interceptado.');
          return;
        }
        if (!data) return;

        const hpServidorJugador = data.jugador?.activo?.hpActual || 0;

        if (data.turnoActual === 'BOT' && hpServidorJugador < this.hpRenderJugador) {
          this.datosPendientesBot = data;
          this.ejecutarIAEnemiga();
          return;
        }

        // ═══ DETECCIÓN DE CARTAS NUEVAS ═══
        if (data.jugador?.mano) {
          this.detectarCartasNuevas(data.jugador.mano);
        }
        if (data.bot?.mano) {
          this.detectarCartasNuevasBot(data.bot.mano);
        }

        this.cdr.detectChanges(); // tick intermedio

        // 🚩 1. GUARDAMOS DE QUIÉN ERA EL TURNO ANTES DE ACTUALIZAR
        const turnoAnterior = this.partida?.turnoActual; 

        // ═══ ACTUALIZACIÓN DEL ESTADO ═══
        this.partida = data;

if (this.esperandoMiNuevoTurno && this.partida.turnoActual === 'JUGADOR') {
            this.esperandoMiNuevoTurno = false;
            this.iniciarRelojTurno(); // ¡Prende la mecha de 60 segundos!
        } 
        else if (this.partida.turnoActual === 'BOT') {
            this.detenerRelojTurno();
        }

        // 🚩 2. CHEQUEAMOS SI ARRANCÓ TU TURNO PARA PRENDER LA MECHA
        if (turnoAnterior !== 'JUGADOR' && this.partida.turnoActual === 'JUGADOR') {
            this.iniciarRelojTurno();
        } 
        // 🚩 3. SI EL TURNO ES DEL BOT, APAGAMOS TU RELOJ
        else if (this.partida.turnoActual === 'BOT') {
            this.detenerRelojTurno();
        }

        if (!this.datosPendientesBot) {
          this.hpRenderJugador = hpServidorJugador;
        }
        
        this.cdr.detectChanges(); // Actualizamos la UI, incluyendo la barra del timer
      }
    });
  }

  // ═══════════════════════════════════════════════
  // DETECCIÓN DE CARTAS NUEVAS
  // ═══════════════════════════════════════════════

  private detectarCartasNuevas(nuevaMano: any[]): void {
    nuevaMano.forEach((carta: any) => {
      if (!this.manoAnteriorIds.has(carta.id)) {
        this.cartasNuevas.add(carta.id);
        setTimeout(() => {
          this.cartasNuevas.delete(carta.id);
          this.cdr.detectChanges();
        }, 700);
      }
    });
    this.manoAnteriorIds = new Set(nuevaMano.map((c: any) => c.id));
  }

  private detectarCartasNuevasBot(nuevaMano: any[]): void {
    nuevaMano.forEach((carta: any) => {
      if (!this.manoAnteriorIdsBot.has(carta.id)) {
        this.cartasNuevasBot.add(carta.id);
        setTimeout(() => {
          this.cartasNuevasBot.delete(carta.id);
          this.cdr.detectChanges();
        }, 550);
      }
    });
    this.manoAnteriorIdsBot = new Set(nuevaMano.map((c: any) => c.id));
  }

  // ═══════════════════════════════════════════════
  // FAN DE CARTAS (posicionamiento sin bugs)
  // ═══════════════════════════════════════════════

  getCardFanTransform(index: number, total: number): string {
    const maxAngle   = Math.min(4 * total, 35);
    const angleStep  = total > 1 ? (maxAngle * 2) / (total - 1) : 0;
    const angle      = total > 1 ? -maxAngle + angleStep * index : 0;
    const cardSpacing = Math.min(80, 480 / Math.max(total, 1));
    const totalWidth  = cardSpacing * (total - 1);
    const offsetX     = -totalWidth / 2 + cardSpacing * index;
    const normalizedPos = total > 1 ? (index / (total - 1)) * 2 - 1 : 0;
    const offsetY     = normalizedPos * normalizedPos * 20;
    return `translateX(calc(-50% + ${offsetX}px)) translateY(${offsetY}px) rotate(${angle}deg)`;
  }

  // ═══════════════════════════════════════════════
  // POLLING
  // ═══════════════════════════════════════════════

  iniciarPolling(): void {
    if (this.pollingPartida) clearInterval(this.pollingPartida);
    this.pollingPartida = setInterval(() => {
      if (this.partida?.turnoActual === 'BOT' && !this.bloqueadoPorAnimacion) {
        this.cargarEstado();
      }
    }, 2000);
  }

  // ═══════════════════════════════════════════════
  // TURNOS Y OVERLAYS
  // ═══════════════════════════════════════════════

  private mostrarTurnOverlay(turno: 'jugador' | 'bot'): void {
    this.turnoOverlayTipo = turno;
    this.showTurnOverlay  = true;
    this.cdr.detectChanges();
    setTimeout(() => {
      this.showTurnOverlay = false;
      this.cdr.detectChanges();
      if (turno === 'bot') {
        setTimeout(() => this.ejecutarIAEnemiga(), 1200);
      }
    }, 2000);
  }

  procesarCambioDeTurnoDramatico(dataServidor: any) {
    this.bloqueadoPorAnimacion = true;
    this.partida = dataServidor;
    setTimeout(() => {
      this.turnoOverlayTipo = 'bot';
      this.showTurnOverlay  = true;
      this.cdr.detectChanges();
      setTimeout(() => {
        this.showTurnOverlay       = false;
        this.bloqueadoPorAnimacion = false;
        this.cdr.detectChanges();
      }, 2000);
    }, 1000);
  }

  iniciarTransicionTurnoBot(nuevoEstado: any) {
    this.bloqueadoPorAnimacion = true;
    this.turnoOverlayTipo      = 'bot';
    this.showTurnOverlay       = true;
    this.cdr.detectChanges();
    setTimeout(() => {
      this.showTurnOverlay = false;
      this.partida         = nuevoEstado;
      this.cdr.detectChanges();
      setTimeout(() => { this.bloqueadoPorAnimacion = false; }, 500);
    }, 2000);
  }

  actualizarSeguridadEstado(data: any) {
    if (data.turnoActual === 'JUGADOR') {
      this.botEstaAtacando       = false;
      this.bloqueadoPorAnimacion = false;
      this.cargandoAccion        = false;
    }
  }

  // ═══════════════════════════════════════════════
  // IA ENEMIGA
  // ═══════════════════════════════════════════════

ejecutarIAEnemiga() {
    if (this.datosPendientesBot) {
      const data = this.datosPendientesBot;
      this.datosPendientesBot = null;
      this.ejecutarIAEnemigaConData(data);
    }
  }

async ejecutarIAEnemigaConData(estadoFinal: any) {
    // 🚩 1. GUARDAMOS LA FOTO ANTES DE QUE EL BOT ACTÚE
    const estadoAntiguo = JSON.parse(JSON.stringify(this.partida));
    
    // Simplificamos la detección de estados (Case Insensitive para evitar fallos de Java)
    const condAntiguas = estadoAntiguo?.bot?.activo?.condicionesEspeciales || [];
    const botEstabaDormido = condAntiguas.some((e: string) => e.toUpperCase() === 'ASLEEP');
    const botEstabaParalizado = condAntiguas.some((e: string) => e.toUpperCase() === 'PARALYZED');

    // 🚩 2. FASE DE MANTENIMIENTO DEL BOT (MONEDA DE DESPERTAR)
    // Lo hacemos ANTES de procesar el ataque
    if (botEstabaDormido) {
        const condNuevas = estadoFinal?.bot?.activo?.condicionesEspeciales || [];
        const sigueDormido = condNuevas.some((e: string) => e.toUpperCase() === 'ASLEEP');
        const seDesperto = !sigueDormido;

        console.log("🎲 El Bot está en Checkup. ¿Se despierta?: " + seDesperto);

        // Reutilizamos tu overlay de monedas de ataque
        this.coinFlipAtaque = {
            nombreAtaque: 'FASE DE MANTENIMIENTO',
            descripcion: '¿Se despierta el rival?',
            cantidadMonedas: 1,
            danioBase: 0,
            danioExtraPorCara: 0,
            monedas: [{ estado: 'girando' }],
            terminado: false,
            progreso: 0,
            esSoloEstado: true,
            danioTotal: 0
        };
        this.cdr.detectChanges();

        await this.delay(1500); // Giro de moneda

        if (this.coinFlipAtaque && this.coinFlipAtaque.monedas[0]) {
            this.coinFlipAtaque.monedas[0].estado = seDesperto ? 'cara' : 'cruz';
            this.coinFlipAtaque.terminado = true;
        }
        (this as any).resultadoMoneda = seDesperto ? 'CARA' : 'CRUZ';
        this.cdr.detectChanges();

        await this.delay(2000); // Pausa para ver el resultado
        this.coinFlipAtaque = null;
        this.cdr.detectChanges();
    }

    // 🚩 3. LÓGICA DE DECISIÓN (¿El bot ataca realmente?)
    const hpJugadorAntes = this.hpRenderJugador;
    const hpJugadorDespues = estadoFinal?.jugador?.activo?.hpActual || 0;
    const danioHecho = hpJugadorAntes - hpJugadorDespues;
    
    let botAtaco = false;
    const activoBotDespues = estadoFinal?.bot?.activo;

    // Si no está paralizado y (si estaba dormido, ahora está despierto)
    const puedeActuar = !botEstabaParalizado && (!botEstabaDormido || (botEstabaDormido && !activoBotDespues?.condicionesEspeciales?.includes('Asleep')));

    if (puedeActuar) {
        if (danioHecho > 0) {
            botAtaco = true; 
        } else if (activoBotDespues && activoBotDespues.card?.ataques?.length > 0) {
            const ataqueBot = activoBotDespues.card.ataques[0];
            const costoReq = ataqueBot.costo?.length || 0;
            const energiasBot = activoBotDespues.energiasUnidas?.length || 0;
            if (energiasBot >= costoReq) botAtaco = true;
        }
    }

    // 🚩 4. ESCANEAMOS CURACIONES/DAÑOS PASIVOS
    await this.verificarEstadosCurados(estadoAntiguo, estadoFinal);

    // ⏩ 5. SALIDA SIN ATAQUE (Si falló la moneda o estaba paralizado)
    if (!botAtaco) {
        this.partida = JSON.parse(JSON.stringify(estadoFinal));
        this.hpRenderJugador = hpJugadorDespues;
        
        this.limpiarBanderasBot(); // Método auxiliar para no repetir código

        if (this.partida.turnoActual === 'JUGADOR') {
            this.iniciarRelojTurno();
        }
        this.cdr.detectChanges();
        return;
    }

    // 💥 6. SI ATACÓ REALMENTE
    this.botEstaAtacando = true;
    this.animandoBotAtaque = true;
    this.cdr.detectChanges();

    if (activoBotDespues && activoBotDespues.card?.ataques?.length > 0) {
        const habilidadBot = activoBotDespues.card.ataques[0];
        const coinConfig = this.detectarCoinFlipAtaque(habilidadBot);

        if (coinConfig) {
            let carasReales = 0;
            if (coinConfig.esSoloEstado) {
                const estaParalizado = estadoFinal.jugador?.activo?.condicionesEspeciales?.includes('Paralyzed');
                carasReales = estaParalizado ? 1 : 0;
            } else {
                // ... (tu lógica de cálculo de caras reales se mantiene igual) ...
                carasReales = danioHecho > 0 ? 1 : 0; // Simplificación para el ejemplo
            }
            (this as any).resultadoMoneda = carasReales > 0 ? 'CARA' : 'CRUZ';
            await this.animarMonedasSincronizadas(habilidadBot.nombre, coinConfig, carasReales, coinConfig.esSoloEstado);
        }
    }

    // 7. EL MOMENTO DEL IMPACTO
    this.showImpactFlash = true;
    this.partida = JSON.parse(JSON.stringify(estadoFinal)); 
    this.hpRenderJugador = hpJugadorDespues;
    this.cdr.detectChanges();

    await this.delay(150);
    this.showImpactFlash = false;
    this.cdr.detectChanges();

    // 🚩 8. FINAL DE LA SECUENCIA
    await this.delay(600);
    this.limpiarBanderasBot();

    if (this.partida.turnoActual === 'JUGADOR') {
        console.log("⏰ Turno del bot finalizado. Iniciando reloj de jugador.");
        this.iniciarRelojTurno();
    }
    this.cdr.detectChanges();
  }

  // Método auxiliar para limpiar estados
  private limpiarBanderasBot() {
    this.animandoBotAtaque     = false;
    this.botEstaAtacando       = false;
    this.cargandoAccion        = false;
    this.bloqueadoPorAnimacion = false;
    this.ataqueRealizado       = false;
    this.botPensando           = false;
    this.esperandoMiNuevoTurno = false;
    (this as any).resultadoMoneda = ''; 
  }

async refrescarTableroDebug() {
    try {
      const nuevoEstado: any = await firstValueFrom(this.battleService.getState(this.matchId!));
      this.partida = JSON.parse(JSON.stringify(nuevoEstado));
      this.cdr.detectChanges();
      console.log("🔄 Tablero recargado mágicamente.");
    } catch (error) {
      console.error("❌ Error recargando tablero en modo debug", error);
    }
  }

  // Herramienta 1: Robar Carta Mágica
  async debugRobarCarta(cardId: string) {
    if (!cardId) return;
    try {
      console.log(`🛠️ GOD MODE: Inyectando carta ${cardId} a la mano...`);
      await firstValueFrom(this.battleService.debugDrawCard(this.matchId!, cardId));
      await this.refrescarTableroDebug();
    } catch (err) {
      console.error("❌ Error en God Mode (Robar Carta):", err);
      alert("Falla en el God Mode. ¿Ya creaste el endpoint en Java?");
    }
  }

  // Herramienta 2: Forzar Estado (Dormir, Paralizar, etc.)
  async debugForzarEstado(objetivo: 'JUGADOR' | 'BOT', estado: string) {
    try {
      console.log(`🛠️ GOD MODE: Forzando estado ${estado} a ${objetivo}...`);
      await firstValueFrom(this.battleService.debugForzarEstado(this.matchId!, objetivo, estado));
      await this.refrescarTableroDebug();
    } catch (err) {
      console.error("❌ Error en God Mode (Forzar Estado):", err);
    }
  }

  // 🚩 ESTA ES LA QUE TE FALTABA (Herramienta 3: Setear HP)
  async debugSetHp(objetivo: 'JUGADOR' | 'BOT', hp: number) {
    try {
      console.log(`🛠️ GOD MODE: Seteando HP de ${objetivo} a ${hp}...`);
      await firstValueFrom(this.battleService.debugSetHp(this.matchId!, objetivo, hp));
      await this.refrescarTableroDebug();
    } catch (err) {
      console.error("❌ Error en God Mode (Set HP):", err);
    }
  }


abrirModalDescarte(quien: 'JUGADOR' | 'BOT') {
  console.log("Capa de descarte cliqueada:", quien); // <-- Agregá esto
  const pila = quien === 'JUGADOR' ? this.partida?.jugador?.pilaDescarte : this.partida?.bot?.pilaDescarte;
  
  if (pila) { // Quitale el .length > 0 para que abra aunque esté vacío si querés testear
    this.tituloDescarteActual = quien === 'JUGADOR' ? 'TU DESCARTE' : 'DESCARTE RIVAL';
    this.cartasParaVerEnDescarte = [...pila].reverse(); // El reverse es para que la última que cayó esté arriba
    this.mostrarModalDescarte = true;
    this.cdr.detectChanges();
  }
}

async animarCuraEstado(quien: 'jugador' | 'bot', estado: string, texto: string) {
    if (quien === 'jugador') {
      this.healedTextPlayer = texto;
      if (estado === 'Paralyzed') this.curingParalysisPlayer = true;
      if (estado === 'Asleep') this.curingSleepPlayer = true;
    } else {
      this.healedTextBot = texto;
      if (estado === 'Paralyzed') this.curingParalysisBot = true;
      if (estado === 'Asleep') this.curingSleepBot = true;
    }
    
    this.cdr.detectChanges();

    // Dejamos la animación por 2 segundos (exactamente lo que pediste)
    await this.delay(2000);

    // Apagamos todo y se disuelve
    if (quien === 'jugador') {
      this.healedTextPlayer = null;
      this.curingParalysisPlayer = false;
      this.curingSleepPlayer = false;
    } else {
      this.healedTextBot = null;
      this.curingParalysisBot = false;
      this.curingSleepBot = false;
    }
    
    this.cdr.detectChanges();
  }

  async verificarEstadosCurados(estadoAnterior: any, estadoNuevo: any) {
    if (!estadoAnterior || !estadoNuevo) return;

    // --- REVISAMOS AL JUGADOR ---
    const condViejoJugador = estadoAnterior.jugador?.activo?.condicionesEspeciales || [];
    const condNuevoJugador = estadoNuevo.jugador?.activo?.condicionesEspeciales || [];

    if (condViejoJugador.includes('Paralyzed') && !condNuevoJugador.includes('Paralyzed')) {
      console.log("⚡ ¡Jugador curado de parálisis!");
      await this.animarCuraEstado('jugador', 'Paralyzed', '¡Parálisis curada!');
    }
    if (condViejoJugador.includes('Asleep') && !condNuevoJugador.includes('Asleep')) {
      console.log("💤 ¡Jugador despertó!");
      await this.animarCuraEstado('jugador', 'Asleep', '¡Se despertó!');
    }

    // --- REVISAMOS AL BOT ---
    const condViejoBot = estadoAnterior.bot?.activo?.condicionesEspeciales || [];
    const condNuevoBot = estadoNuevo.bot?.activo?.condicionesEspeciales || [];

    if (condViejoBot.includes('Paralyzed') && !condNuevoBot.includes('Paralyzed')) {
      console.log("⚡ ¡Bot curado de parálisis!");
      await this.animarCuraEstado('bot', 'Paralyzed', '¡Parálisis curada!');
    }
    if (condViejoBot.includes('Asleep') && !condNuevoBot.includes('Asleep')) {
      console.log("💤 ¡Bot despertó!");
      await this.animarCuraEstado('bot', 'Asleep', '¡Se despertó!');
    }
  }

  forzarUpdate() {
    this.battleService.getState(this.matchId!).subscribe({
      next: async (data) => {
        if (!data) return;
        if (this.partida?.turnoActual === 'JUGADOR' && data.turnoActual === 'BOT') {
          this.bloqueadoPorAnimacion = true;
          const miHp = this.hpRenderJugador;
          this.partida         = data;
          this.hpRenderJugador = miHp;
          this.cdr.detectChanges();
          await new Promise(f => setTimeout(f, 1200));
          this.turnoOverlayTipo = 'bot';
          this.showTurnOverlay  = true;
          this.cdr.detectChanges();
          await new Promise(f => setTimeout(f, 2000));
          this.showTurnOverlay = false;
          this.cdr.detectChanges();
          this.ejecutarIAEnemigaConData(data);
          return;
        }
        this.partida         = data;
        this.hpRenderJugador = data.jugador?.activo?.hpActual || 0;
        this.cdr.detectChanges();
      }
    });
  }

  

  // ═══════════════════════════════════════════════
  // ACCIONES DE JUEGO
  // ═══════════════════════════════════════════════

async ejecutarAtaqueSecuencia(nombreAtaque: string) {
    if (this.cargandoAccion || !nombreAtaque) return;

    this.bloqueadoPorAnimacion = true;
    this.cargandoAccion        = true;
    this.ataqueRealizado       = true;

    const habilidad = this.partida.jugador.activo.card.ataques.find((a: any) => a.nombre === nombreAtaque);
    const tipoEnergia = habilidad?.costo[0] || 'Colorless';

    try {
      // 🚩 1. CHECK DE CONFUSIÓN (Antes de atacar de verdad)
      if (this.partida.jugador.activo.condicionesEspeciales.includes('Confused')) {
        const configConfusion = {
          descripcion: "Tu Pokémon está confundido. Lanzá una moneda. Si sale cruz, te hacés 30 de daño y el ataque falla.",
          cantidadMonedas: 1,
          danioBase: 0,
          danioExtraPorCara: 0,
          esSoloEstado: true
        };

        // Acá usamos una moneda "pura" (random en frontend) porque el backend
        // asume que si llega el request de ataque, ya pasaste el check.
        // Ojo: Si tu backend ya maneja la confusión y rechaza el ataque, ajustá esto.
        const exitoConfusion = Math.random() >= 0.5 ? 1 : 0; 
        
        await this.animarMonedasSincronizadas(
            "Check de Confusión", 
            configConfusion, 
            exitoConfusion, 
            true
        );

    if (this.resultadoMoneda === 'CRUZ') {
          // El ataque falló por confusión. Avisamos al backend para que pase turno.
          console.log("¡Confusión! Te pegaste a vos mismo.");
          
          // 🚩 EL FIX: Primero pasamos turno, DESPUÉS pedimos el estado
          await firstValueFrom(this.battleService.pasarTurno(this.matchId!));
          const estadoFallo: any = await firstValueFrom(this.battleService.getState(this.matchId!));
          
          // Animación de auto-golpe
          this.animandoJugadorDanio = true;
          this.partida = JSON.parse(JSON.stringify(estadoFallo));
          this.hpRenderJugador = estadoFallo.jugador?.activo?.hpActual || 0;
          this.cdr.detectChanges();
          
          await this.delay(600);
          this.animandoJugadorDanio = false;
          
          // Pasamos al bloque del bot
          return this.iniciarTurnoBot(estadoFallo); 
        }
      }

      // 2. Mandamos la orden de ataque al backend (Si no había confusión, o si salió CARA)
      await firstValueFrom(this.battleService.atacar(this.matchId!, nombreAtaque));

      // 3. Pedimos la foto actualizada
      const estadoFinal: any = await firstValueFrom(this.battleService.getState(this.matchId!));

      // 4. Magia de monedas (RECARGADA)
      const coinConfig = this.detectarCoinFlipAtaque(habilidad);
      if (coinConfig) {
        const hpBotAntes = this.partida.bot?.activo?.hpActual || 0;
        const hpBotDespues = estadoFinal.bot?.activo?.hpActual || 0;
        const danioHecho = hpBotAntes - hpBotDespues;

        let carasReales = 0;

        // 🚩 SOLUCIÓN AL "SIEMPRE CRUZ":
        if (coinConfig.esSoloEstado) {
            const condicionesBot = estadoFinal.bot?.activo?.condicionesEspeciales || [];
            const tieneEstado = ['Paralyzed', 'Asleep', 'Confused', 'Poisoned'].some(c => condicionesBot.includes(c));
            
            carasReales = tieneEstado ? 1 : 0;
            (this as any).resultadoMoneda = tieneEstado ? 'CARA' : 'CRUZ';
        } else {
            const txt = (habilidad.texto || '').toLowerCase();
            if (txt.includes('does nothing')) {
                carasReales = danioHecho > 0 ? 1 : 0;
            } else if (txt.includes('heads')) {
                if (coinConfig.danioExtraPorCara > 0) {
                    const base = (txt.includes('more') || txt.includes('plus')) ? coinConfig.danioBase : 0;
                    carasReales = Math.min(coinConfig.cantidadMonedas, Math.round(Math.max(0, danioHecho - base) / coinConfig.danioExtraPorCara));
                }
            }
            (this as any).resultadoMoneda = carasReales > 0 ? 'CARA' : 'CRUZ';
        }

        await this.animarMonedasSincronizadas(habilidad.nombre, coinConfig, carasReales, coinConfig.esSoloEstado);
      }

      // 5. Animación de impacto
      this.animandoAtaque = true;
      this.cdr.detectChanges();

      await this.delay(400);
      this.dispararParticulas('bot', tipoEnergia);
      this.showImpactFlash = true;
      
      // 🚩 ACTUALIZACIÓN FÍSICA: Clonamos para que Angular refresque bien los estados (iconos)
      this.partida = JSON.parse(JSON.stringify(estadoFinal));
      this.hpRenderJugador = estadoFinal.jugador?.activo?.hpActual || 0;
      this.cdr.detectChanges();

      await this.delay(200);
      this.showImpactFlash = false;
      this.cdr.detectChanges();

      await this.delay(400);
      this.animandoAtaque = false;
      this.cdr.detectChanges();

      // 6. Turno del Bot
      if (estadoFinal.turnoActual === 'BOT') {
          await this.iniciarTurnoBot(estadoFinal);
      } else {
        this.cargandoAccion        = false;
        this.bloqueadoPorAnimacion = false;
        this.ataqueRealizado       = false;
        this.cdr.detectChanges();
      }
    } catch (error: any) {
      this.cargandoAccion        = false;
      this.ataqueRealizado       = false;
      this.bloqueadoPorAnimacion = false;
      console.error("Error en ataque:", error);
    }
  }
async iniciarTurnoBot(estadoFinal: any) {
      await this.delay(1000);
      this.cargandoAccion   = false;
      this.turnoOverlayTipo = 'bot';
      this.showTurnOverlay  = true;
      this.cdr.detectChanges();
      
      await this.delay(2000);
      this.showTurnOverlay = false;
      this.cdr.detectChanges();
      
      await this.delay(400);

      try {
         const estadoPostBot: any = await firstValueFrom(this.battleService.jugarBot(this.matchId!));
         this.ejecutarIAEnemigaConData(estadoPostBot);
      } catch (err: any) {
         console.error("Error al ejecutar bot:", err);
         this.bloqueadoPorAnimacion = false;
      }
  }
async pasarTurno(): Promise<void> {
    console.log("👉 Botón 'Pasar Turno' presionado.");

    // 🛡️ 1. VALIDACIONES DE SEGURIDAD
    if (this.partida?.turnoActual !== 'JUGADOR') {
      console.warn("⛔ Bloqueado: No es tu turno.");
      return;
    }
    if (this.cargandoAccion) {
      console.warn("⛔ Bloqueado: Hay una acción en curso.");
      return;
    }

    // ⚡ 2. BLOQUEO INMEDIATO Y RESET DE RELOJ
    this.cargandoAccion = true;
    this.bloqueadoPorAnimacion = true;
    this.detenerRelojTurno();

    // Reseteamos visualmente la barra para que no quede a la mitad
    this.tiempoRestante = this.tiempoTurnoMaximo;
    this.porcentajeTimer = 100;
    this.esperandoMiNuevoTurno = true;
    this.cdr.detectChanges();

    // Guardamos "la foto" de la mesa antes del cambio para comparar estados
    const estadoAntiguo = JSON.parse(JSON.stringify(this.partida));

    try {
      console.log("⏳ Enviando fin de turno al servidor (Java)...");
      // Avisamos al backend que termine nuestro turno
      await firstValueFrom(this.battleService.pasarTurno(this.matchId!));

      // Pedimos el nuevo estado (donde el server ya procesó estados y moneda de Checkup)
      const estadoFinal: any = await firstValueFrom(this.battleService.getState(this.matchId!));
      console.log("📸 Datos de Checkup recibidos del server.");

      // --- 🪙 3. FASE DE CHECKUP: MONEDA DE DESPERTAR (TS-SAFE) ---
   const estadosAntiguos = estadoAntiguo?.jugador?.activo?.condicionesEspeciales || [];
const estabaDormido = estadosAntiguos.some((e: string) => e.toUpperCase() === 'ASLEEP');

const estadosNuevos = estadoFinal?.jugador?.activo?.condicionesEspeciales || [];
const sigueDormido = estadosNuevos.some((e: string) => e.toUpperCase() === 'ASLEEP');

if (estabaDormido) {
    console.log("¡Te detecté durmiendo! Activando moneda...");
        const seDesperto = !sigueDormido;

        // Seteamos el objeto para el attack-coinflip-overlay con todos los campos requeridos
        this.coinFlipAtaque = {
          nombreAtaque: 'FASE DE MANTENIMIENTO',
          descripcion: '¿Se despierta tu Pokémon?',
          cantidadMonedas: 1,
          danioBase: 0,
          danioExtraPorCara: 0,
          monedas: [{ estado: 'girando' }],
          terminado: false,
          progreso: 0,
          esSoloEstado: true,
          danioTotal: 0
        };
        this.cdr.detectChanges();

        // Tiempo de suspenso mientras gira la moneda
        await this.delay(1500);

        // Actualizamos el resultado (Validación para evitar error de objeto posiblemente nulo)
        if (this.coinFlipAtaque && this.coinFlipAtaque.monedas[0]) {
          this.coinFlipAtaque.monedas[0].estado = seDesperto ? 'cara' : 'cruz';
          this.coinFlipAtaque.terminado = true;
        }

        this.resultadoMoneda = seDesperto ? 'CARA' : 'CRUZ';
        this.cdr.detectChanges();

        // Pausa para que el usuario asimile el resultado
        await this.delay(2000);

        // Limpiamos el overlay
        this.coinFlipAtaque = null;
        this.cdr.detectChanges();

        if (seDesperto) {
          console.log("✨ ¡Suerte! El Pokémon se despertó.");
        } else {
          console.log("💤 Sigue dormido.");
        }
      }

      // --- ✨ 4. ESCANEO DE CURACIONES / DAÑOS PASIVOS ---
      // Compara vida y estados entre el antes y el después para animar rayitos o impactos
      await this.verificarEstadosCurados(estadoAntiguo, estadoFinal);

      // --- 🔄 5. ACTUALIZACIÓN VISUAL DEL TABLERO ---
      // Ahora sí, actualizamos la partida con los datos reales del server
      this.partida = JSON.parse(JSON.stringify(estadoFinal));
      this.cdr.detectChanges();

      // --- 🤖 6. LÓGICA DEL TURNO DEL RIVAL (BOT) ---
      if (estadoFinal.turnoActual === 'BOT') {
        // Mostramos el cartel de "TURNO RIVAL"
        this.turnoOverlayTipo = 'bot';
        this.showTurnOverlay = true;
        this.cdr.detectChanges();

        await this.delay(2000);
        this.showTurnOverlay = false;
        this.cdr.detectChanges();
        await this.delay(400);

        try {
          // FINGIMOS QUE EL BOT PIENSA (UX)
          this.botPensando = true;
          this.cdr.detectChanges();

          // Pausa aleatoria para humanizar la IA
          const tiempoPensamiento = Math.floor(Math.random() * 1500) + 1000;
          await this.delay(tiempoPensamiento);

          console.log("🤖 Disparando IA en el backend...");
          const estadoPostBot: any = await firstValueFrom(this.battleService.jugarBot(this.matchId!));

          // El bot ya decidió, apagamos el cartelito de "pensando"
          this.botPensando = false;
          this.cdr.detectChanges();

          console.log("✅ IA finalizada. Animando ataques del bot...");
          // Esta función debe procesar el estado final y activar el reloj de tu turno al terminar
          this.ejecutarIAEnemigaConData(estadoPostBot);

        } catch (err: any) {
          console.error("❌ Error crítico en la IA del Bot:", err);
          this.botPensando = false;
          this.bloqueadoPorAnimacion = false;
          this.cargandoAccion = false;
          this.cdr.detectChanges();
        }

      } else {
        // Caso borde: el turno volvió a vos (ej: el bot no pudo jugar)
        console.log("⚠️ Turno devuelto al jugador inmediatamente.");
        this.cargandoAccion = false;
        this.bloqueadoPorAnimacion = false;
        this.iniciarRelojTurno();
        this.cdr.detectChanges();
      }

    } catch (error: any) {
      // En caso de error, liberamos la UI para que no quede trabada
      this.cargandoAccion = false;
      this.bloqueadoPorAnimacion = false;
      console.error("❌ Error del servidor al pasar turno:", error);
      alert('Error de conexión: ' + (error?.error ?? 'El servidor no responde'));
    }
  }
realizarAccion(habilidad: any): void {
    this.showHabilidadesPanel = false;
    if (!this.validarEnergiaAtaque(habilidad)) {
      alert('¡No tenés suficiente energía para usar ' + habilidad.nombre + '!');
      return;
    }
    // IMPORTANTE: Ahora pasamos por acá siempre
    this.ejecutarAtaqueSecuencia(habilidad.nombre);
  }
async animarMonedasSincronizadas(nombreAtaque: string, config: any, carasForzadas: number, esSoloEstado: boolean): Promise<void> {   
  // 1. Reiniciamos el estado visual
  (this as any).resultadoMoneda = ''; 
  
  this.coinFlipAtaque = {
    nombreAtaque,
    descripcion: config.descripcion,
    cantidadMonedas: config.cantidadMonedas,
    danioBase: config.danioBase,
    danioExtraPorCara: config.danioExtraPorCara,
    monedas: Array(config.cantidadMonedas).fill(null).map(() => ({ estado: 'girando' as const })),
    danioTotal: 0,
    terminado: false,
    progreso: 0,
    esSoloEstado: esSoloEstado
  };
  this.cdr.detectChanges();

  let carasAsignadas = 0;
  for (let i = 0; i < config.cantidadMonedas; i++) {
    // Progreso visual de la barra
    this.coinFlipAtaque!.progreso = ((i + 1) / config.cantidadMonedas) * 100;

    await this.delay(600 + Math.random() * 200);

    let esCara = false;
    const carasRestantes = carasForzadas - carasAsignadas;
    const monedasRestantes = config.cantidadMonedas - i;

    // Lógica de "Truco": Sincronizamos con lo que el backend ya decidió
    if (carasRestantes >= monedasRestantes) {
      esCara = true; 
    } else if (carasRestantes > 0) {
      esCara = Math.random() < (carasRestantes / monedasRestantes); 
    }

    if (esCara) carasAsignadas++;

    // Seteamos el estado visual de la moneda actual
    this.coinFlipAtaque!.monedas[i].estado = esCara ? 'cara' : 'cruz';
    
    // 🚩 Actualizamos el flag que usa el HTML para el cartel "LOGRADO/FALLÓ"
    if (config.cantidadMonedas === 1) {
        (this as any).resultadoMoneda = esCara ? 'CARA' : 'CRUZ';
    }
    
    this.cdr.detectChanges();
    await this.delay(400);
  }

  // Si son varias monedas, definimos el resultado global
  if (config.cantidadMonedas > 1) {
      (this as any).resultadoMoneda = carasAsignadas > 0 ? 'CARA' : 'CRUZ';
  }

  // 🚩 CÁLCULO FINAL: Aseguramos que el daño extra mostrado sea el correcto
  this.coinFlipAtaque!.danioTotal = carasAsignadas * config.danioExtraPorCara;
  this.coinFlipAtaque!.terminado = true;
  this.cdr.detectChanges();

  // Pausa para que el usuario festeje (o llore) el resultado
  await this.delay(2000);
  
  // Limpieza
  this.coinFlipAtaque = null;
  // (this as any).resultadoMoneda = ''; // Opcional: limpiar para el próximo ataque
  this.cdr.detectChanges();
}

  intentarAbrirHabilidades(event?: MouseEvent): void {
    if (event) { event.stopPropagation(); event.preventDefault(); }
    if (this.partida?.turnoActual === 'JUGADOR') {
      this.botEstaAtacando       = false;
      this.bloqueadoPorAnimacion = false;
    }
    if (this.cargandoAccion) { console.warn('⚠️ Bloqueado por cargandoAccion'); return; }
    this.showHabilidadesPanel = !this.showHabilidadesPanel;
    this.cdr.detectChanges();
  }

 async jugarCarta(carta: any): Promise<void> {
    if (this.partida.turnoActual !== 'JUGADOR' || this.cargandoAccion) return;
    
    // 1. Manejo de Energías
    if (this.esEnergia(carta)) { 
      this.gestionarUnionEnergia(carta); 
      return; 
    }
    
    // 2. Manejo de Pokémon
    if (this.esPokemon(carta)) {
      
      // 🚩 A. CHEQUEO DE EVOLUCIÓN (Se revisa antes que la bajada normal)
      if (carta.evolvesFrom && this.puedeEvolucionar(carta)) {
        
        // Buscamos a quién evolucionar (Prioriza al Activo, sino busca en la Banca)
        let target = null;
        if (this.partida.jugador.activo?.card?.nombre === carta.evolvesFrom) {
            target = this.partida.jugador.activo;
        } else {
            target = this.partida.jugador.banca.find((b: any) => b.card.nombre === carta.evolvesFrom);
        }

        if (target) {
            await this.ejecutarEvolucionVisual(carta, target);
        }
        return; // 🛑 Cortamos la ejecución acá para que no intente bajarlo a la banca como Básico
      }

      // B. VALIDACIÓN DE ACTIVO VACÍO
      if (!this.partida.jugador.activo && this.partida.jugador.banca.length > 0) {
        alert('¡Primero tenés que subir un Pokémon de tu banca al puesto activo!');
        return;
      }
      
      // C. BAJADA DE POKÉMON BÁSICO A LA BANCA O ACTIVO
      this.gestionarBajadaPokemon(carta);
    }
  }

  async ejecutarEvolucionVisual(cartaEvolucion: any, target: any) {
    this.bloqueadoPorAnimacion = true;
    this.cargandoAccion = true;

    try {
      // 1. Avisamos al backend
      await firstValueFrom(this.battleService.evolucionar(this.matchId!, cartaEvolucion.id, target.card.id));

      // 2. Disparamos la luz blanca sobre el Pokémon objetivo
      this.animandoEvolucionId = target.card.id;
      this.cdr.detectChanges();

      // Dejamos que el brillo suba (600ms)
      await this.delay(600);

      // 3. Justo en el pico de la luz, pedimos la foto nueva con el Pokémon ya evolucionado
      const estadoFinal: any = await firstValueFrom(this.battleService.getState(this.matchId!));
      this.partida = JSON.parse(JSON.stringify(estadoFinal));
      this.hpRenderJugador = estadoFinal.jugador?.activo?.hpActual || 0;
      this.cdr.detectChanges();

      // Dejamos que el brillo baje y muestre el nuevo sprite
      await this.delay(600);

      // 4. Limpiamos
      this.animandoEvolucionId = null;
      this.bloqueadoPorAnimacion = false;
      this.cargandoAccion = false;
      this.cdr.detectChanges();

    } catch (error: any) {
      this.bloqueadoPorAnimacion = false;
      this.cargandoAccion = false;
      alert("Error al evolucionar: " + (error.error || error.message));
    }
  }

  seleccionarBanca(p: any) {
    if (this.modoSeleccionRetirada) {
      this.cargandoAccion = true;
      this.battleService.retirarPokemon(this.matchId!, p.card.id).subscribe({
        next: () => { this.modoSeleccionRetirada = false; this.cargandoAccion = false; this.cargarEstado(); },
        error: (err) => { this.modoSeleccionRetirada = false; this.cargandoAccion = false; alert(err.error || 'No se pudo realizar la retirada.'); }
      });
    } else if (!this.partida?.jugador?.activo) {
      this.cargandoAccion = true;
      this.battleService.subirAActivo(this.matchId!, p.card.id).subscribe({
        next: () => { this.cargandoAccion = false; this.cargarEstado(); },
        error: (err) => { this.cargandoAccion = false; console.error(err); alert('No se pudo subir el Pokémon al puesto activo.'); }
      });
    }
  }

  iniciarModoRetirada() {
    this.showHabilidadesPanel  = false;
    this.modoSeleccionRetirada = true;
  }

  retirarPokemon(suplente: any) {
    if (this.cargandoAccion || this.partida.turnoActual !== 'JUGADOR') return;
    const costo = this.partida.jugador.activo.card.costoRetirada;
    if (confirm(`¿Querés retirar a ${this.partida.jugador.activo.card.nombre}? Costará ${costo} energía(s).`)) {
      this.cargandoAccion = true;
      this.battleService.retirarPokemon(this.matchId!, suplente.card.id).subscribe({
        next: () => { this.cargarEstado(); this.cargandoAccion = false; },
        error: (err) => { this.cargandoAccion = false; alert(err.error || 'No tenés suficiente energía para retirarte.'); }
      });
    }
  }

  soltarCarta(event: CdkDragDrop<any[]>, zona: 'activo' | 'banca'): void {
    if (event.previousContainer === event.container) return;
    const cartaArrastrada = event.item.data;
    if (this.esEnergia(cartaArrastrada)) {
      this.gestionarUnionEnergia(cartaArrastrada);
    } else if (this.esPokemon(cartaArrastrada)) {
      this.jugarCarta(cartaArrastrada);
    }
  }

  private gestionarBajadaPokemon(carta: any): void {
    if (this.cargandoAccion) return;
    this.cargandoAccion = true;
    this.battleService.jugarPokemon(this.matchId!, carta.id).subscribe({
      next: () => { this.cargandoAccion = false; this.cargarEstado(); },
      error: (err) => { this.cargandoAccion = false; console.error(err); alert(err.error || 'No se pudo bajar el Pokémon.'); }
    });
  }

  private gestionarUnionEnergia(cartaEnergia: any): void {
    if (!this.partida.jugador.activo) { alert('¡Necesitás un Pokémon activo!'); return; }
    this.cargandoAccion = true;
    this.battleService.unirEnergia(this.matchId!, this.partida.jugador.activo.card.id, cartaEnergia.id).subscribe({
      next: () => { this.cargandoAccion = false; this.cargarEstado(); },
      error: (err: any) => { this.cargandoAccion = false; console.error(err); alert('No se pudo unir la energía.'); }
    });
  }

  puedePagarRetiro(): boolean {
    const activo = this.partida?.jugador?.activo;
    if (!activo) return false;
    return activo.energiasUnidas.length >= activo.card.costoRetirada;
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

  // ═══════════════════════════════════════════════
  // ENERGÍA DRAG
  // ═══════════════════════════════════════════════

  onEnergyMouseMove = (event: MouseEvent) => { this.mousePos = { x: event.clientX, y: event.clientY }; };

  onEnergyMouseUp = (event: MouseEvent) => {
    this.isDraggingEnergy = false;
    window.removeEventListener('mousemove', this.onEnergyMouseMove);
    window.removeEventListener('mouseup',   this.onEnergyMouseUp);
    this.checkDropTarget(event);
  };

  startEnergyDrag(event: MouseEvent, cartaId: string) {
    const card = this.partida?.jugador.mano.find((c: any) => c.id === cartaId);
    if (card?.supertype !== 'Energy') return;
    this.isDraggingEnergy  = true;
    this.selectedEnergyId  = cartaId;
    this.originPos         = { x: event.clientX, y: event.clientY };
    this.mousePos          = { x: event.clientX, y: event.clientY };
    window.addEventListener('mousemove', this.onEnergyMouseMove);
    window.addEventListener('mouseup',   this.onEnergyMouseUp);
  }

  checkDropTarget(event: MouseEvent) {
    const element       = document.elementFromPoint(event.clientX, event.clientY);
    const pokemonElement = element?.closest('.pokemon-card-container');
    if (pokemonElement) {
      const targetId = pokemonElement.getAttribute('data-card-id');
      if (targetId && this.selectedEnergyId) {
        console.log(`Uniendo energía ${this.selectedEnergyId} a Pokémon ${targetId}`);
      }
    }
  }

  // ═══════════════════════════════════════════════
  // VALIDACIÓN DE ENERGÍAS
  // ═══════════════════════════════════════════════

  validarEnergiaAtaque(ataque: any): boolean {
    if (!ataque || !this.partida?.jugador?.activo) return false;

    const normalizarTipo = (tipo: string): string => {
      const t = tipo.toLowerCase();
      if (t.includes('grass')     || t.includes('planta'))              return 'Grass';
      if (t.includes('fire')      || t.includes('fuego'))               return 'Fire';
      if (t.includes('water')     || t.includes('agua'))                return 'Water';
      if (t.includes('lightning') || t.includes('eléctrica') || t.includes('electrica')) return 'Lightning';
      if (t.includes('psychic')   || t.includes('psíquica')  || t.includes('psiquica'))  return 'Psychic';
      if (t.includes('fighting')  || t.includes('lucha'))               return 'Fighting';
      if (t.includes('darkness')  || t.includes('siniestra') || t.includes('oscuridad')) return 'Darkness';
      if (t.includes('metal')     || t.includes('acero'))               return 'Metal';
      if (t.includes('dragon')    || t.includes('dragón'))              return 'Dragon';
      if (t.includes('fairy')     || t.includes('hada'))                return 'Fairy';
      return tipo;
    };

    const misEnergias = this.partida.jugador.activo.energiasUnidas.map((e: any) => {
      const texto = (e.tipo === 'Energy' || !e.tipo) ? e.nombre : e.tipo;
      return normalizarTipo(texto);
    });

    const costoRequerido = [...ataque.costo].map((t: string) => normalizarTipo(t));

    for (let i = costoRequerido.length - 1; i >= 0; i--) {
      const tipoReq = costoRequerido[i];
      if (tipoReq !== 'Colorless') {
        const index = misEnergias.indexOf(tipoReq);
        if (index !== -1) { misEnergias.splice(index, 1); costoRequerido.splice(i, 1); }
        else { return false; }
      }
    }
    return misEnergias.length >= costoRequerido.length;
  }

  getCheckEnergiasAtaque(ataque: any): any[] {
    if (!this.partida?.jugador?.activo?.energiasUnidas) return [];
    const poseidas  = [...this.partida.jugador.activo.energiasUnidas];
    const resultado: any[] = [];
    ataque.costo.forEach((tipoRequerido: string) => {
      const index = poseidas.findIndex(e => e.tipo === tipoRequerido || tipoRequerido === 'Colorless');
      if (index !== -1) { resultado.push({ tipo: tipoRequerido, cumplido: true  }); poseidas.splice(index, 1); }
      else              { resultado.push({ tipo: tipoRequerido, cumplido: false }); }
    });
    return resultado;
  }

  getFaltantesAtaque(ataque: any): any[] {
    if (!this.partida?.jugador?.activo?.energiasUnidas) return [];
    const poseidas      = [...this.partida.jugador.activo.energiasUnidas];
    const costoRestante = [...ataque.costo];
    const faltantesMap: { [key: string]: number } = {};

    for (let i = costoRestante.length - 1; i >= 0; i--) {
      const tipoReq = costoRestante[i];
      if (tipoReq !== 'Colorless') {
        const index = poseidas.findIndex(p => p.tipo === tipoReq);
        if (index !== -1) { poseidas.splice(index, 1); costoRestante.splice(i, 1); }
      }
    }
    for (let i = costoRestante.length - 1; i >= 0; i--) {
      if (costoRestante[i] === 'Colorless' && poseidas.length > 0) {
        poseidas.splice(0, 1); costoRestante.splice(i, 1);
      }
    }
    costoRestante.forEach(tipo => { faltantesMap[tipo] = (faltantesMap[tipo] || 0) + 1; });
    return Object.keys(faltantesMap).map(tipo => ({ tipo, cantidad: faltantesMap[tipo] }));
  }

  // ═══════════════════════════════════════════════
  // SPRITES Y UTILIDADES
  // ═══════════════════════════════════════════════

  private normalizarNombre(nombre: string): string {
    if (!nombre) return '';
    return nombre.toLowerCase().normalize('NFD').replace(/[\u0300-\u036f]/g, '').trim();
  }

  private getPokemonNum(nombreCarta: string): number {
    const norm    = this.normalizarNombre(nombreCarta);
    if (this.pokedexNum[norm]) return this.pokedexNum[norm];
    const palabras = norm.split(/[\s'']+/).reverse();
    for (const p of palabras) { if (this.pokedexNum[p]) return this.pokedexNum[p]; }
    const numMatch = norm.match(/\d+/);
    if (numMatch) return parseInt(numMatch[0], 10);
    return 0;
  }

 getSpriteBack(nombreCarta: string): string {
    const num = this.getPokemonNum(nombreCarta);
    if (!num) return '';
    // Ruta a los GIFs de espalda
    return `https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/other/showdown/back/${num}.gif`;
  }

  getSpriteFront(nombreCarta: string): string {
    const num = this.getPokemonNum(nombreCarta);
    if (!num) return '';
    // Ruta a los GIFs de frente
    return `https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/other/showdown/${num}.gif`;
  }

  onSpriteError(event: Event): void { (event.target as HTMLImageElement).style.display = 'none'; }

  getHpPercent(pokemon: any): number {
    if (!pokemon?.hpActual) return 0;
    return Math.max(0, Math.min(100, (pokemon.hpActual / this.getHpMax(pokemon)) * 100));
  }

  getHpMax(pokemon: any): number {
    return pokemon?.hpMax || parseInt(pokemon?.card?.hp, 10) || 100;
  }

  getImagenCarta(id: string): string { return `images/cards/${id}.png`; }
  getEmptySlots(n: number): number[] { return Array(Math.max(0, 5 - n)).fill(0); }

  esEnergia(carta: any): boolean  { return carta?.supertype === 'Energy'; }
  esPokemon(carta: any): boolean  { return carta?.supertype === 'Pokémon' || carta?.supertype === 'Pokemon'; }

  get manoAgrupada(): any[][] {
    const tamañoStack = 4;
    const mano        = this.partida.jugador.mano;
    const stacks      = [];
    for (let i = 0; i < mano.length; i += tamañoStack) stacks.push(mano.slice(i, i + tamañoStack));
    return stacks;
  }

  getEnergyName(tipo: string): string {
    const t: any = { grass:'Planta', fire:'Fuego', water:'Agua', lightning:'Eléctrico',
                     psychic:'Psíquico', fighting:'Lucha', darkness:'Siniestro',
                     metal:'Acero', colorless:'Incolora', fairy:'Hada', dragon:'Dragón' };
    return t[(tipo || '').toLowerCase()] || 'Energía';
  }

  getEnergyColor(tipo: string): string {
    const c: any = { grass:'#78C850', fire:'#F08030', water:'#6890F0', lightning:'#F8D030',
                     psychic:'#F85888', fighting:'#C03028', darkness:'#705848',
                     metal:'#B8B8D0', colorless:'#A8A878', fairy:'#EE99AC', dragon:'#7038F8' };
    return c[(tipo || '').toLowerCase()] || '#A8A878';
  }

  // ═══════════════════════════════════════════════
  // PARTÍCULAS
  // ═══════════════════════════════════════════════

  dispararParticulas(objetivo: 'bot' | 'jugador', tipoEnergia: string) {
    const color           = this.getEnergyColor(tipoEnergia) || '#ffffff';
    const nuevasParticulas = [];
    for (let i = 0; i < 20; i++) {
      const angulo    = Math.random() * Math.PI * 2;
      const distancia = 40 + Math.random() * 80;
      nuevasParticulas.push({
        color, tx: Math.cos(angulo) * distancia, ty: Math.sin(angulo) * distancia,
        size: 5 + Math.random() * 7, duracion: 0.4 + Math.random() * 0.5
      });
    }
    if (objetivo === 'bot') {
      this.particulasBot    = nuevasParticulas;
      this.mostrarEfectoBot = true;
      this.animandoBotDanio = true;
    } else {
      this.particulasJugador    = nuevasParticulas;
      this.mostrarEfectoJugador = true;
      this.animandoJugadorDanio = true;
    }
    this.cdr.detectChanges();
    setTimeout(() => {
      if (objetivo === 'bot') { this.mostrarEfectoBot = false; this.animandoBotDanio = false; }
      else { this.mostrarEfectoJugador = false; this.animandoJugadorDanio = false; }
      this.cdr.detectChanges();
    }, 800);
  }

  // ═══════════════════════════════════════════════
  // NAVEGACIÓN
  // ═══════════════════════════════════════════════

  volverAlLobby(): void { this.router.navigate(['/lobby']); }

  private delay(ms: number): Promise<void> { return new Promise(resolve => setTimeout(resolve, ms)); }



}