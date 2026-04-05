import { Component, OnInit, OnDestroy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { BattleService } from '../../services/battle.service';
import { Router, ActivatedRoute } from '@angular/router';
import { DragDropModule, CdkDragDrop } from '@angular/cdk/drag-drop';
import { firstValueFrom } from 'rxjs';


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

  // Tracking de cartas nuevas (jugador)
  public cartasNuevas = new Set<string>();
  private manoAnteriorIds = new Set<string>();

  // Tracking de cartas nuevas (bot)
  public cartasNuevasBot = new Set<string>();
  private manoAnteriorIdsBot = new Set<string>();

  // Estado visual
  vibrarBot         = false;
  cargandoAccion    = false;
  boardVisible      = false;
  showIntro         = true;
  introFadingOut    = false;
  showTurnOverlay   = false;
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

  // Variables internas
  public activoVisualJugador: any = null;
  public hpRenderJugador: number = 0;
  public bloqueadoPorAnimacion: boolean = false;
  public anguloFinal: number = 0;

  private intentosBotSinAccion = 0;
  private ultimaCantidadCartasBot = -1;
  private ciclosSinCambio = 0;
  private hpVisualInterno: number = 0;

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
    private cdr: ChangeDetectorRef
  ) {}

  // ═══════════════════════════════════════════════
  // LIFECYCLE
  // ═══════════════════════════════════════════════

  ngOnInit(): void {
    this.matchId = this.route.snapshot.paramMap.get('id');
    if (!this.matchId) return;

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
    if (!this.matchId || this.bloqueadoPorAnimacion || this.botEstaAtacando) return;

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
        // PRIMERO detectamos (antes de asignar this.partida),
        // luego forzamos detectChanges para que Angular aplique la clase,
        // DESPUÉS asignamos los datos nuevos.
        if (data.jugador?.mano) {
          this.detectarCartasNuevas(data.jugador.mano);
        }
        if (data.bot?.mano) {
          this.detectarCartasNuevasBot(data.bot.mano);
        }

        this.cdr.detectChanges(); // tick intermedio → Angular registra las clases nueva-carta

        this.partida = data;
        if (!this.datosPendientesBot) {
          this.hpRenderJugador = hpServidorJugador;
        }
        this.cdr.detectChanges();
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
  const activoBotDespues = estadoFinal?.bot?.activo;
  const hpJugadorAntes = this.hpRenderJugador;
  const hpJugadorDespues = estadoFinal?.jugador?.activo?.hpActual || 0;
  const danioHecho = hpJugadorAntes - hpJugadorDespues;
  
  let botAtaco = false;

  // 1. Detección de ataque
  if (danioHecho > 0) {
    botAtaco = true; 
  } else if (activoBotDespues && activoBotDespues.card?.ataques?.length > 0) {
    const ataqueBot = activoBotDespues.card.ataques[0];
    const costoReq = ataqueBot.costo?.length || 0;
    const energiasBot = activoBotDespues.energiasUnidas?.length || 0;
    if (energiasBot >= costoReq) botAtaco = true;
  }

  if (!botAtaco) {
    this.partida = estadoFinal;
    this.hpRenderJugador = hpJugadorDespues;
    this.cargandoAccion = false;
    this.bloqueadoPorAnimacion = false;
    this.cdr.detectChanges();
    return;
  }

  // 2. Inicio de secuencia
  this.botEstaAtacando = true;
  this.animandoBotAtaque = true;
  this.cdr.detectChanges();

  if (activoBotDespues && activoBotDespues.card?.ataques?.length > 0) {
    const habilidadBot = activoBotDespues.card.ataques[0];
    const coinConfig = this.detectarCoinFlipAtaque(habilidadBot);

    if (coinConfig) {
      let carasReales = 0;
      
      if (coinConfig.esSoloEstado) {
        // 🚩 CHEQUEO DIRECTO AL JSON DEL BACKEND
        const condicionesActuales = estadoFinal.jugador?.activo?.condicionesEspeciales || [];
        const estaParalizado = condicionesActuales.includes('Paralyzed');
        carasReales = estaParalizado ? 1 : 0;
        
        // FORZAMOS el resultado para que el modal no use basura anterior
        (this as any).resultadoMoneda = estaParalizado ? 'CARA' : 'CRUZ';
      } else {
        // Lógica de daño normal
        const txt = (habilidadBot.texto || '').toLowerCase();
        if (txt.includes('does nothing')) {
          carasReales = danioHecho > 0 ? 1 : 0;
        } else if (txt.includes('heads')) {
          if (coinConfig.danioExtraPorCara > 0) {
             const base = txt.includes('more') || txt.includes('plus') ? coinConfig.danioBase : 0;
             carasReales = Math.min(coinConfig.cantidadMonedas, Math.round((danioHecho - base) / coinConfig.danioExtraPorCara));
          }
        }
        (this as any).resultadoMoneda = carasReales > 0 ? 'CARA' : 'CRUZ';
      }

      // Esperamos a que la moneda termine de girar
      await this.animarMonedasSincronizadas(habilidadBot.nombre, coinConfig, carasReales, coinConfig.esSoloEstado);
    }
  }

  // 3. EL MOMENTO DEL IMPACTO
  // Solo acá actualizamos la data para que el HP baje y los estados aparezcan
  this.showImpactFlash = true;
  this.partida = JSON.parse(JSON.stringify(estadoFinal)); // Clonamos para forzar el refresh de Angular
  this.hpRenderJugador = hpJugadorDespues;
  this.cdr.detectChanges();

  await this.delay(150);
  this.showImpactFlash = false;
  this.cdr.detectChanges();

  await this.delay(450);
  this.animandoBotAtaque     = false;
  this.botEstaAtacando       = false;
  this.cargandoAccion        = false;
  this.bloqueadoPorAnimacion = false;
  this.ataqueRealizado       = false;
  
  // Limpieza final de la moneda para que no quede el cartel viejo en el próximo turno
  (this as any).resultadoMoneda = ''; 
  
  this.cdr.detectChanges();
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
      // 1. Mandamos la orden de ataque al backend
      await firstValueFrom(this.battleService.atacar(this.matchId!, nombreAtaque));

      // 2. Pedimos la foto actualizada
      const estadoFinal: any = await firstValueFrom(this.battleService.getState(this.matchId!));

      // 3. Magia de monedas (RECARGADA)
      const coinConfig = this.detectarCoinFlipAtaque(habilidad);
      if (coinConfig) {
        const hpBotAntes = this.partida.bot?.activo?.hpActual || 0;
        const hpBotDespues = estadoFinal.bot?.activo?.hpActual || 0;
        const danioHecho = hpBotAntes - hpBotDespues;

        let carasReales = 0;

        // 🚩 SOLUCIÓN AL "SIEMPRE CRUZ":
        if (coinConfig.esSoloEstado) {
            // Si el ataque es de estado, miramos si el activo del BOT tiene condiciones ahora
            const condicionesBot = estadoFinal.bot?.activo?.condicionesEspeciales || [];
            const tieneEstado = ['Paralyzed', 'Asleep', 'Confused', 'Poisoned'].some(c => condicionesBot.includes(c));
            
            carasReales = tieneEstado ? 1 : 0;
            // Forzamos la variable global para el modal
            (this as any).resultadoMoneda = tieneEstado ? 'CARA' : 'CRUZ';
        } else {
            // Lógica normal para ataques de DAÑO (Kricketot, Tangela, etc.)
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

        // Llamamos a la animación con el valor REAL
        await this.animarMonedasSincronizadas(habilidad.nombre, coinConfig, carasReales, coinConfig.esSoloEstado);
      }

      // 4. Animación de impacto
      this.animandoAtaque = true;
      this.cdr.detectChanges();

      await this.delay(400);
      this.dispararParticulas('bot', tipoEnergia);
      this.showImpactFlash = true;
      
      // 🚩 ACTUALIZACIÓN FÍSICA: Usamos un clon para que Angular refresque bien los estados (iconos)
      this.partida = JSON.parse(JSON.stringify(estadoFinal));
      this.hpRenderJugador = estadoFinal.jugador?.activo?.hpActual || 0;
      this.cdr.detectChanges();

      await this.delay(200);
      this.showImpactFlash = false;
      this.cdr.detectChanges();

      await this.delay(400);
      this.animandoAtaque = false;
      this.cdr.detectChanges();

      // 5. Turno del Bot
      if (estadoFinal.turnoActual === 'BOT') {
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

  async pasarTurno(): Promise<void> {
    console.log("👉 Botón 'Pasar Turno' presionado.");
    console.log("Turno actual del tablero:", this.partida?.turnoActual);
    console.log("¿Está cargando acción (cargandoAccion)?:", this.cargandoAccion);
    console.log("¿Bloqueado por animación?:", this.bloqueadoPorAnimacion);

    if (this.partida?.turnoActual !== 'JUGADOR') {
        console.warn("⛔ Bloqueado: Angular piensa que no es el turno del jugador.");
        return;
    }
    if (this.cargandoAccion) {
        console.warn("⛔ Bloqueado: 'cargandoAccion' se quedó trabado en TRUE en alguna acción anterior.");
        return; // ¡Acá seguro está el fantasma!
    }

    this.cargandoAccion        = true;
    this.bloqueadoPorAnimacion = true;

    try {
      console.log("⏳ Enviando petición al backend para pasar turno...");
      await firstValueFrom(this.battleService.pasarTurno(this.matchId!));
      console.log("✅ El Backend aceptó el fin de turno.");

      const estadoFinal: any = await firstValueFrom(this.battleService.getState(this.matchId!));
      console.log("📸 Foto de la mesa recibida. El turno ahora es de:", estadoFinal.turnoActual);

      if (estadoFinal.turnoActual === 'BOT') {
        this.turnoOverlayTipo = 'bot';
        this.showTurnOverlay  = true;
        this.cdr.detectChanges();
        
        await this.delay(2000);
        this.showTurnOverlay = false;
        this.cdr.detectChanges();
        await this.delay(400);

        try {
           console.log("🤖 Disparando endpoint para despertar al bot...");
           const estadoPostBot: any = await firstValueFrom(this.battleService.jugarBot(this.matchId!));
           console.log("✅ El bot terminó de pensar. Animando impacto...");
           this.ejecutarIAEnemigaConData(estadoPostBot);
        } catch (err: any) {
           console.error("❌ Error CRÍTICO del bot en Java:", err);
           this.bloqueadoPorAnimacion = false;
           this.cargandoAccion = false;
        }

      } else {
        console.log("⚠️ Qué raro, se pasó turno pero sigue siendo de JUGADOR.");
        this.partida = estadoFinal;
        this.cargandoAccion        = false;
        this.bloqueadoPorAnimacion = false;
        this.cdr.detectChanges();
      }
    } catch (error: any) {
      this.cargandoAccion        = false;
      this.bloqueadoPorAnimacion = false;
      console.error("❌ Error del servidor al pasar turno:", error);
      alert('Error al pasar turno: ' + (error?.error ?? 'No se pudo comunicar con el servidor'));
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

  jugarCarta(carta: any): void {
    if (this.partida.turnoActual !== 'JUGADOR' || this.cargandoAccion) return;
    if (this.esEnergia(carta)) { this.gestionarUnionEnergia(carta); return; }
    if (this.esPokemon(carta)) {
      if (!this.partida.jugador.activo && this.partida.jugador.banca.length > 0) {
        alert('¡Primero tenés que subir un Pokémon de tu banca al puesto activo!');
        return;
      }
      this.gestionarBajadaPokemon(carta);
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
    return `https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/back/${num}.png`;
  }

  getSpriteFront(nombreCarta: string): string {
    const num = this.getPokemonNum(nombreCarta);
    if (!num) return '';
    return `https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/${num}.png`;
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