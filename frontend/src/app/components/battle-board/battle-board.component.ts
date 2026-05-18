import { Component, OnInit, OnDestroy, ChangeDetectorRef,HostListener } from '@angular/core';
import { CommonModule } from '@angular/common';
import { BattleService } from '../../services/battle.service';
import { BattleBoardUiService } from '../../services/battle-board-ui.service';
import { Router, ActivatedRoute } from '@angular/router';
import { DragDropModule, CdkDragDrop } from '@angular/cdk/drag-drop';
import { firstValueFrom } from 'rxjs';
import { SafeHtml } from '@angular/platform-browser';

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
  hoveredCardIndex: number = -1; // En quï¿½ posiciï¿½n de la lista estï¿½s
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
    
    // ï¿½Estï¿½ en el activo?
    if (this.partida.jugador?.activo?.card?.nombre === cartaMano.evolvesFrom) return true;
    
    // ï¿½Estï¿½ en la banca?
    return this.partida.jugador?.banca?.some((b: any) => b.card.nombre === cartaMano.evolvesFrom);
  }

  // CoinFlip
  public estadoCoinFlip: 'ELEGIR_LADO' | 'ESPERANDO_TIRO' | 'GIRANDO' | 'ELEGIR_TURNO' | 'RESULTADO_BOT' | 'OCULTO' = 'OCULTO';
  eleccionJugador: 'CARA' | 'CRUZ' = 'CARA';
  resultadoMoneda: 'CARA' | 'CRUZ' = 'CARA';
  public girando: boolean = false;

  // -- Estado de efectos --
  mostrarAuraCuracionBot    = false;
  mostrarAuraCuracionPlayer = false;
  mostrarKO                 = false;
 
  // -- Nï¿½meros de daï¿½o flotantes --
  damageNumberBot:    { valor: number; esCuracion: boolean } | null = null;
  damageNumberPlayer: { valor: number; esCuracion: boolean } | null = null;


  // Partï¿½culas
  mostrarEfectoBot     = false;
  mostrarEfectoJugador = false;
  particulasBot:     any[] = [];
  particulasJugador: any[] = [];
  animandoBotDanio     = false;
  animandoJugadorDanio = false;

  constructor(
    private battleService: BattleService,
    private route: ActivatedRoute,
    private router: Router,
    private cdr: ChangeDetectorRef,
    private battleBoardUi: BattleBoardUiService
  ) {}

  // -----------------------------------------------
  // LIFECYCLE
  // -----------------------------------------------

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

  // -----------------------------------------------
  // COIN FLIP
  // -----------------------------------------------

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

      // ?? CLAVE: Forzamos a Angular a actualizar la barra y el texto
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
    console.log("?? ï¿½TIEMPO AGOTADO! Pasando turno automï¿½ticamente...");
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
 
  // -- Coin flip de ataque --
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
    esSoloEstado: boolean; // <-- ACï¿½ SOLO EL TIPO, SIN " = config..."
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
 
    // Pequeï¿½a pausa de limpieza
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

// 3. ??? LA MAGIA DE LA RUEDITA (Solo para la mano)
  @HostListener('window:wheel', ['$event'])
  onScrollCard(event: WheelEvent) {
    if (this.hoveredCard && this.hoveredCardList === this.partida?.jugador?.mano && this.hoveredCardList.length > 1) {
      event.preventDefault();

      const now = Date.now();

      // ?? A. Bloqueamos el mouse fï¿½sico temporalmente
      this.isScrollingMode = true;
      clearTimeout(this.scrollTimeout);
      this.scrollTimeout = setTimeout(() => {
        this.isScrollingMode = false; // Se desbloquea 200ms despuï¿½s de dejar de girar
      }, 200);

      // ?? B. Freno de velocidad: Solo permite 1 salto de carta cada 120 milisegundos
      if (now - this.lastScrollTime < 120) return;
      this.lastScrollTime = now;

      // C. Lï¿½gica de cambio de ï¿½ndice (Igual que antes)
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

  // 4. ?? CLICK CENTRAL (Ruedita) PARA JUGAR LA CARTA
  @HostListener('window:mousedown', ['$event'])
  onMiddleClick(event: MouseEvent) {
    // event.button === 1 significa que tocaste la ruedita (el botï¿½n del medio)
    if (event.button === 1 && this.hoveredCard && this.hoveredCardList === this.partida?.jugador?.mano) {
      event.preventDefault(); // Evita que salga la crucecita molesta de Windows
      
      console.log("??? ï¿½Click central! Jugando carta:", this.hoveredCard.nombre);
      this.jugarCarta(this.hoveredCard); // Juega la carta que estï¿½s viendo en el panel grande
    }
  }


showDebugPanel: boolean = false;
  fps: number = 0;
  memoryUsage: string = 'N/A';
  
  private frameCount = 0;
  private lastTime = performance.now();
  private animFrameId: number | null = null;

  // ??? DETECTOR DE TECLA F3
  @HostListener('window:keydown', ['$event'])
  onKeyDown(event: KeyboardEvent) {
    if (event.key === 'F3') {
      event.preventDefault();
      this.showDebugPanel = !this.showDebugPanel;
      
      // Prendemos o apagamos el medidor para no gastar recursos si estï¿½ cerrado
      if (this.showDebugPanel) {
        this.iniciarMedidorRendimiento();
      } else {
        this.detenerMedidorRendimiento();
      }
    }
  }

  // ?? Lï¿½GICA DE RENDIMIENTO (FPS Y MEMORIA)
  iniciarMedidorRendimiento() {
    const loop = () => {
      const now = performance.now();
      this.frameCount++;
      
      // Actualizamos las mï¿½tricas cada 1 segundo (1000ms)
      if (now >= this.lastTime + 1000) {
        this.fps = Math.round((this.frameCount * 1000) / (now - this.lastTime));
        this.frameCount = 0;
        this.lastTime = now;

        // Intentar leer memoria (API especï¿½fica de navegadores basados en Chromium)
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

  // ?? CASO: Estaba Dormido y el server lanzï¿½ moneda
  if (activoJugadorAntiguo?.condicionesEspeciales?.includes('Asleep')) {
    
    const sigueDormido = activoJugadorNuevo?.condicionesEspeciales?.includes('Asleep');
    const resultadoCara = !sigueDormido; // Si ya no estï¿½ dormido, es porque saliï¿½ CARA
    
    console.log("?? Lanzando moneda de despertar...");
    
    // Usamos tu sistema de monedas existente
    // Creamos un "ataque ficticio" para el texto del overlay
    const configFicticia = { 
        cantidadMonedas: 1, 
        esSoloEstado: true, 
        danioBase: 0, 
        danioExtraPorCara: 0 
    };

    await this.animarMonedasSincronizadas("ï¿½Se despierta?", configFicticia, resultadoCara ? 1 : 0, true);
    
    if (resultadoCara) {
      console.log("? ï¿½Se despertï¿½!");
      // Podï¿½s disparar un sonido de "Ding!" o una animaciï¿½n de chispitas
    } else {
      console.log("?? Sigue roncando...");
    }
  }
}

extraerGlosario(carta: any): any[] {
    return this.battleBoardUi.extraerGlosario(carta);
  }

formatTextoAtaque(texto: string): SafeHtml {
    return this.battleBoardUi.formatTextoAtaque(texto);
  }

setHoveredCard(item: any, list: any[] = [], index: number = -1) {
    // ?? FIX: Si estamos girando la ruedita, ignoramos los choques fï¿½sicos del mouse
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
    esSoloEstado: boolean // ?? 1. Agregamos el parï¿½metro acï¿½
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
      esSoloEstado: esSoloEstado // ?? 2. Usamos el parï¿½metro que entra, NO 'config'
    };

    this.cdr.detectChanges();
    // Pequeï¿½a pausa para que el overlay aparezca
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
 
    // Calculamos el daï¿½o total
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
    esSoloEstado: boolean; // ?? Nueva bandera
  } | null {
    if (!ataque?.texto && !ataque?.descripcion && !ataque?.efecto) return null;

    const texto: string = (ataque.texto || ataque.descripcion || ataque.efecto || '').toLowerCase();

    const flipMatch = texto.match(/flip\s+(\d+|a|an|one|two|three|four|five)\s+coin|lanz[aï¿½]\s+(\d+|una?)\s+moneda/i);
    if (!flipMatch) return null;

    const numStr = (flipMatch[1] || flipMatch[2] || 'a').toLowerCase();
    const numMap: Record<string, number> = { a:1, an:1, one:1, una:1, 'un':1, '1':1, two:2, dos:2, '2':2, three:3, tres:3, '3':3 };
    const cantidadMonedas = (numMap[numStr] ?? parseInt(numStr, 10)) || 1; 

    let danioBase = parseInt(ataque.danio || ataque.dano || '0', 10) || 0;
    let danioExtraPorCara = 0;
    let esMultiplicador = false;
    let esFalloCruz = false;
    let esSoloEstado = false;

    // ?????? Lï¿½gica de detecciï¿½n mejorada
    if (texto.includes('paralyzed') || texto.includes('asleep') || texto.includes('confused') || texto.includes('poisoned')) {
        // Si el daï¿½o no depende de la moneda (como Dratini), es solo efecto de estado
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
    // -- KO del Pokï¿½mon del bot --
    const botActivoAntes  = estadoAnterior?.bot?.activo;
    const botActivoAhora  = estadoNuevo?.bot?.activo;
    if (botActivoAntes && !botActivoAhora) {
      await this.mostrarKOAnim();
    }

    // -- KO del Pokï¿½mon del jugador --
    const playerActivoAntes = estadoAnterior?.jugador?.activo;
    const playerActivoAhora = estadoNuevo?.jugador?.activo;
    if (playerActivoAntes && !playerActivoAhora) {
      await this.mostrarKOAnim();
    }

    // -- Daï¿½o al bot --
    const hpBotAntes  = botActivoAntes?.hpActual  ?? 0;
    const hpBotAhora  = botActivoAhora?.hpActual  ?? 0;
    if (botActivoAntes && botActivoAhora && hpBotAhora < hpBotAntes) {
      this.mostrarDamageNumber('bot', hpBotAntes - hpBotAhora);
    }

    // -- Curaciï¿½n del bot --
    if (botActivoAntes && botActivoAhora && hpBotAhora > hpBotAntes) {
      const curado = hpBotAhora - hpBotAntes;
      this.mostrarDamageNumber('bot', curado, true);
      this.mostrarCuracion('bot');
    }

    // -- Daï¿½o al jugador --
    const hpPlayerAntes = playerActivoAntes?.hpActual ?? 0;
    const hpPlayerAhora = playerActivoAhora?.hpActual ?? 0;
    if (playerActivoAntes && playerActivoAhora && hpPlayerAhora < hpPlayerAntes) {
      this.mostrarDamageNumber('jugador', hpPlayerAntes - hpPlayerAhora);
    }

    // -- Curaciï¿½n del jugador --
    if (playerActivoAntes && playerActivoAhora && hpPlayerAhora > hpPlayerAntes) {
      const curado = hpPlayerAhora - hpPlayerAntes;
      this.mostrarDamageNumber('jugador', curado, true);
      this.mostrarCuracion('jugador');
    }
  }
 

debugFullCatalog: any[] = [];
  debugFilteredCatalog: any[] = [];
  debugSelectedIndex: number = 0;

  // Criterios de bï¿½squeda
  debugSearchText: string = '';
  debugSearchSupertype: string = '';

  // ?? ACORDATE DE LLAMAR A ESTE Mï¿½TODO EN TU ngOnInit()
cargarCatalogoGodMode() {
    this.battleService.getCardCatalogDebug().subscribe({
      next: (cartas) => {
        this.debugFullCatalog = cartas;
        
        // ?? AGREGï¿½ ESTA Lï¿½NEA PARA ESPIAR EL DATO:
        console.log("?????? Primera carta que llegï¿½:", cartas[0]);

        this.aplicarFiltrosDebug();
        console.log(`?? God Mode: Catï¿½logo cargado con ${cartas.length} cartas.`);
      },
      error: (err) => console.error("? Error cargando catï¿½logo de God Mode:", err)
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
      // 1. Filtro por Tipo (Pokï¿½mon, Trainer, Energy)
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
        if (textoOriginal.includes('paralyzed')) return `Lanzï¿½ ${numStr}. Si sale CARA, el rival queda Paralizado.`;
        if (textoOriginal.includes('asleep')) return `Lanzï¿½ ${numStr}. Si sale CARA, el rival queda Dormido.`;
        return `Lanzï¿½ ${numStr} para aplicar un efecto especial.`;
    }
    
    if (esFalloCruz) return `Lanzï¿½ ${numStr}. Si sale CRUZ, el ataque falla.`;
    if (esMultiplicador) return `Lanzï¿½ ${numStr}. Hace ${danio} de daï¿½o por cada CARA.`;
    
    return `Lanzï¿½ ${numStr}. Hace ${danio} de daï¿½o extra por cada CARA.`;
  }
  // -----------------------------------------------
  // CARGA DE ESTADO
  // -----------------------------------------------

 cargarEstado(): void {
if (!this.matchId || this.bloqueadoPorAnimacion || this.botEstaAtacando || this.botPensando) return;
    this.battleService.getState(this.matchId).subscribe({
      next: (data) => {
        if (this.bloqueadoPorAnimacion || this.botEstaAtacando) {
          console.log('??? Polling interceptado.');
          return;
        }
        if (!data) return;

        const hpServidorJugador = data.jugador?.activo?.hpActual || 0;

        if (data.turnoActual === 'BOT' && hpServidorJugador < this.hpRenderJugador) {
          this.datosPendientesBot = data;
          this.ejecutarIAEnemiga();
          return;
        }

        // --- DETECCIï¿½N DE CARTAS NUEVAS ---
        if (data.jugador?.mano) {
          this.detectarCartasNuevas(data.jugador.mano);
        }
        if (data.bot?.mano) {
          this.detectarCartasNuevasBot(data.bot.mano);
        }

        this.cdr.detectChanges(); // tick intermedio

        // ?? 1. GUARDAMOS DE QUIï¿½N ERA EL TURNO ANTES DE ACTUALIZAR
        const turnoAnterior = this.partida?.turnoActual; 

        // --- ACTUALIZACIï¿½N DEL ESTADO ---
        this.partida = data;

if (this.esperandoMiNuevoTurno && this.partida.turnoActual === 'JUGADOR') {
            this.esperandoMiNuevoTurno = false;
            this.iniciarRelojTurno(); // ï¿½Prende la mecha de 60 segundos!
        } 
        else if (this.partida.turnoActual === 'BOT') {
            this.detenerRelojTurno();
        }

        // ?? 2. CHEQUEAMOS SI ARRANCï¿½ TU TURNO PARA PRENDER LA MECHA
        if (turnoAnterior !== 'JUGADOR' && this.partida.turnoActual === 'JUGADOR') {
            this.iniciarRelojTurno();
        } 
        // ?? 3. SI EL TURNO ES DEL BOT, APAGAMOS TU RELOJ
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

  // -----------------------------------------------
  // DETECCIï¿½N DE CARTAS NUEVAS
  // -----------------------------------------------

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

  // -----------------------------------------------
  // FAN DE CARTAS (posicionamiento sin bugs)
  // -----------------------------------------------

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

  // -----------------------------------------------
  // POLLING
  // -----------------------------------------------

  iniciarPolling(): void {
    if (this.pollingPartida) clearInterval(this.pollingPartida);
    this.pollingPartida = setInterval(() => {
      if (this.partida?.turnoActual === 'BOT' && !this.bloqueadoPorAnimacion) {
        this.cargarEstado();
      }
    }, 2000);
  }

  // -----------------------------------------------
  // TURNOS Y OVERLAYS
  // -----------------------------------------------

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

  // -----------------------------------------------
  // IA ENEMIGA
  // -----------------------------------------------

ejecutarIAEnemiga() {
    if (this.datosPendientesBot) {
      const data = this.datosPendientesBot;
      this.datosPendientesBot = null;
      this.ejecutarIAEnemigaConData(data);
    }
  }

async ejecutarIAEnemigaConData(estadoFinal: any) {
    // ?? 1. GUARDAMOS LA FOTO ANTES DE QUE EL BOT ACTï¿½E
    const estadoAntiguo = JSON.parse(JSON.stringify(this.partida));
    
    // Simplificamos la detecciï¿½n de estados (Case Insensitive para evitar fallos de Java)
    const condAntiguas = estadoAntiguo?.bot?.activo?.condicionesEspeciales || [];
    const botEstabaDormido = condAntiguas.some((e: string) => e.toUpperCase() === 'ASLEEP');
    const botEstabaParalizado = condAntiguas.some((e: string) => e.toUpperCase() === 'PARALYZED');

    // ?? 2. FASE DE MANTENIMIENTO DEL BOT (MONEDA DE DESPERTAR)
    // Lo hacemos ANTES de procesar el ataque
    if (botEstabaDormido) {
        const condNuevas = estadoFinal?.bot?.activo?.condicionesEspeciales || [];
        const sigueDormido = condNuevas.some((e: string) => e.toUpperCase() === 'ASLEEP');
        const seDesperto = !sigueDormido;

        console.log("?? El Bot estï¿½ en Checkup. ï¿½Se despierta?: " + seDesperto);

        // Reutilizamos tu overlay de monedas de ataque
        this.coinFlipAtaque = {
            nombreAtaque: 'FASE DE MANTENIMIENTO',
            descripcion: 'ï¿½Se despierta el rival?',
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

    // ?? 3. Lï¿½GICA DE DECISIï¿½N (ï¿½El bot ataca realmente?)
    const hpJugadorAntes = this.hpRenderJugador;
    const hpJugadorDespues = estadoFinal?.jugador?.activo?.hpActual || 0;
    const danioHecho = hpJugadorAntes - hpJugadorDespues;
    
    let botAtaco = false;
    const activoBotDespues = estadoFinal?.bot?.activo;

    // Si no estï¿½ paralizado y (si estaba dormido, ahora estï¿½ despierto)
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

    // ?? 4. ESCANEAMOS CURACIONES/DAï¿½OS PASIVOS
    await this.verificarEstadosCurados(estadoAntiguo, estadoFinal);

    // ? 5. SALIDA SIN ATAQUE (Si fallï¿½ la moneda o estaba paralizado)
    if (!botAtaco) {
        this.partida = JSON.parse(JSON.stringify(estadoFinal));
        this.hpRenderJugador = hpJugadorDespues;
        
        this.limpiarBanderasBot(); // Mï¿½todo auxiliar para no repetir cï¿½digo

        if (this.partida.turnoActual === 'JUGADOR') {
            this.iniciarRelojTurno();
        }
        this.cdr.detectChanges();
        return;
    }

    // ?? 6. SI ATACï¿½ REALMENTE
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
                // ... (tu lï¿½gica de cï¿½lculo de caras reales se mantiene igual) ...
                carasReales = danioHecho > 0 ? 1 : 0; // Simplificaciï¿½n para el ejemplo
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

    // ?? 8. FINAL DE LA SECUENCIA
    await this.delay(600);
    this.limpiarBanderasBot();

    if (this.partida.turnoActual === 'JUGADOR') {
        console.log("? Turno del bot finalizado. Iniciando reloj de jugador.");
        this.iniciarRelojTurno();
    }
    this.cdr.detectChanges();
  }

  // Mï¿½todo auxiliar para limpiar estados
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
      console.log("?? Tablero recargado mï¿½gicamente.");
    } catch (error) {
      console.error("? Error recargando tablero en modo debug", error);
    }
  }

  // Herramienta 1: Robar Carta Mï¿½gica
  async debugRobarCarta(cardId: string) {
    if (!cardId) return;
    try {
      console.log(`??? GOD MODE: Inyectando carta ${cardId} a la mano...`);
      await firstValueFrom(this.battleService.debugDrawCard(this.matchId!, cardId));
      await this.refrescarTableroDebug();
    } catch (err) {
      console.error("? Error en God Mode (Robar Carta):", err);
      alert("Falla en el God Mode. ï¿½Ya creaste el endpoint en Java?");
    }
  }

  // Herramienta 2: Forzar Estado (Dormir, Paralizar, etc.)
  async debugForzarEstado(objetivo: 'JUGADOR' | 'BOT', estado: string) {
    try {
      console.log(`??? GOD MODE: Forzando estado ${estado} a ${objetivo}...`);
      await firstValueFrom(this.battleService.debugForzarEstado(this.matchId!, objetivo, estado));
      await this.refrescarTableroDebug();
    } catch (err) {
      console.error("? Error en God Mode (Forzar Estado):", err);
    }
  }

  // ?? ESTA ES LA QUE TE FALTABA (Herramienta 3: Setear HP)
  async debugSetHp(objetivo: 'JUGADOR' | 'BOT', hp: number) {
    try {
      console.log(`??? GOD MODE: Seteando HP de ${objetivo} a ${hp}...`);
      await firstValueFrom(this.battleService.debugSetHp(this.matchId!, objetivo, hp));
      await this.refrescarTableroDebug();
    } catch (err) {
      console.error("? Error en God Mode (Set HP):", err);
    }
  }


abrirModalDescarte(quien: 'JUGADOR' | 'BOT') {
  console.log("Capa de descarte cliqueada:", quien); // <-- Agregï¿½ esto
  const pila = quien === 'JUGADOR' ? this.partida?.jugador?.pilaDescarte : this.partida?.bot?.pilaDescarte;
  
  if (pila) { // Quitale el .length > 0 para que abra aunque estï¿½ vacï¿½o si querï¿½s testear
    this.tituloDescarteActual = quien === 'JUGADOR' ? 'TU DESCARTE' : 'DESCARTE RIVAL';
    this.cartasParaVerEnDescarte = [...pila].reverse(); // El reverse es para que la ï¿½ltima que cayï¿½ estï¿½ arriba
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

    // Dejamos la animaciï¿½n por 2 segundos (exactamente lo que pediste)
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
      console.log("? ï¿½Jugador curado de parï¿½lisis!");
      await this.animarCuraEstado('jugador', 'Paralyzed', 'ï¿½Parï¿½lisis curada!');
    }
    if (condViejoJugador.includes('Asleep') && !condNuevoJugador.includes('Asleep')) {
      console.log("?? ï¿½Jugador despertï¿½!");
      await this.animarCuraEstado('jugador', 'Asleep', 'ï¿½Se despertï¿½!');
    }

    // --- REVISAMOS AL BOT ---
    const condViejoBot = estadoAnterior.bot?.activo?.condicionesEspeciales || [];
    const condNuevoBot = estadoNuevo.bot?.activo?.condicionesEspeciales || [];

    if (condViejoBot.includes('Paralyzed') && !condNuevoBot.includes('Paralyzed')) {
      console.log("? ï¿½Bot curado de parï¿½lisis!");
      await this.animarCuraEstado('bot', 'Paralyzed', 'ï¿½Parï¿½lisis curada!');
    }
    if (condViejoBot.includes('Asleep') && !condNuevoBot.includes('Asleep')) {
      console.log("?? ï¿½Bot despertï¿½!");
      await this.animarCuraEstado('bot', 'Asleep', 'ï¿½Se despertï¿½!');
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

  

  // -----------------------------------------------
  // ACCIONES DE JUEGO
  // -----------------------------------------------

async ejecutarAtaqueSecuencia(nombreAtaque: string) {
    if (this.cargandoAccion || !nombreAtaque) return;

    this.bloqueadoPorAnimacion = true;
    this.cargandoAccion        = true;
    this.ataqueRealizado       = true;

    const habilidad = this.partida.jugador.activo.card.ataques.find((a: any) => a.nombre === nombreAtaque);
    const tipoEnergia = habilidad?.costo[0] || 'Colorless';

    try {
      // ?? 1. CHECK DE CONFUSIï¿½N (Antes de atacar de verdad)
      if (this.partida.jugador.activo.condicionesEspeciales.includes('Confused')) {
        const configConfusion = {
          descripcion: "Tu Pokï¿½mon estï¿½ confundido. Lanzï¿½ una moneda. Si sale cruz, te hacï¿½s 30 de daï¿½o y el ataque falla.",
          cantidadMonedas: 1,
          danioBase: 0,
          danioExtraPorCara: 0,
          esSoloEstado: true
        };

        // Acï¿½ usamos una moneda "pura" (random en frontend) porque el backend
        // asume que si llega el request de ataque, ya pasaste el check.
        // Ojo: Si tu backend ya maneja la confusiï¿½n y rechaza el ataque, ajustï¿½ esto.
        const exitoConfusion = Math.random() >= 0.5 ? 1 : 0; 
        
        await this.animarMonedasSincronizadas(
            "Check de Confusiï¿½n", 
            configConfusion, 
            exitoConfusion, 
            true
        );

    if (this.resultadoMoneda === 'CRUZ') {
          // El ataque fallï¿½ por confusiï¿½n. Avisamos al backend para que pase turno.
          console.log("ï¿½Confusiï¿½n! Te pegaste a vos mismo.");
          
          // ?? EL FIX: Primero pasamos turno, DESPUï¿½S pedimos el estado
          await firstValueFrom(this.battleService.pasarTurno(this.matchId!));
          const estadoFallo: any = await firstValueFrom(this.battleService.getState(this.matchId!));
          
          // Animaciï¿½n de auto-golpe
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

      // 2. Mandamos la orden de ataque al backend (Si no habï¿½a confusiï¿½n, o si saliï¿½ CARA)
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

        // ?? SOLUCIï¿½N AL "SIEMPRE CRUZ":
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

      // 5. Animaciï¿½n de impacto
      this.animandoAtaque = true;
      this.cdr.detectChanges();

      await this.delay(400);
      this.dispararParticulas('bot', tipoEnergia);
      this.showImpactFlash = true;
      
      // ?? ACTUALIZACIï¿½N Fï¿½SICA: Clonamos para que Angular refresque bien los estados (iconos)
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
    console.log("?? Botï¿½n 'Pasar Turno' presionado.");

    // ??? 1. VALIDACIONES DE SEGURIDAD
    if (this.partida?.turnoActual !== 'JUGADOR') {
      console.warn("? Bloqueado: No es tu turno.");
      return;
    }
    if (this.cargandoAccion) {
      console.warn("? Bloqueado: Hay una acciï¿½n en curso.");
      return;
    }

    // ? 2. BLOQUEO INMEDIATO Y RESET DE RELOJ
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
      console.log("? Enviando fin de turno al servidor (Java)...");
      // Avisamos al backend que termine nuestro turno
      await firstValueFrom(this.battleService.pasarTurno(this.matchId!));

      // Pedimos el nuevo estado (donde el server ya procesï¿½ estados y moneda de Checkup)
      const estadoFinal: any = await firstValueFrom(this.battleService.getState(this.matchId!));
      console.log("?? Datos de Checkup recibidos del server.");

      // --- ?? 3. FASE DE CHECKUP: MONEDA DE DESPERTAR (TS-SAFE) ---
   const estadosAntiguos = estadoAntiguo?.jugador?.activo?.condicionesEspeciales || [];
const estabaDormido = estadosAntiguos.some((e: string) => e.toUpperCase() === 'ASLEEP');

const estadosNuevos = estadoFinal?.jugador?.activo?.condicionesEspeciales || [];
const sigueDormido = estadosNuevos.some((e: string) => e.toUpperCase() === 'ASLEEP');

if (estabaDormido) {
    console.log("ï¿½Te detectï¿½ durmiendo! Activando moneda...");
        const seDesperto = !sigueDormido;

        // Seteamos el objeto para el attack-coinflip-overlay con todos los campos requeridos
        this.coinFlipAtaque = {
          nombreAtaque: 'FASE DE MANTENIMIENTO',
          descripcion: 'ï¿½Se despierta tu Pokï¿½mon?',
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

        // Actualizamos el resultado (Validaciï¿½n para evitar error de objeto posiblemente nulo)
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
          console.log("? ï¿½Suerte! El Pokï¿½mon se despertï¿½.");
        } else {
          console.log("?? Sigue dormido.");
        }
      }

      // --- ? 4. ESCANEO DE CURACIONES / DAï¿½OS PASIVOS ---
      // Compara vida y estados entre el antes y el despuï¿½s para animar rayitos o impactos
      await this.verificarEstadosCurados(estadoAntiguo, estadoFinal);

      // --- ?? 5. ACTUALIZACIï¿½N VISUAL DEL TABLERO ---
      // Ahora sï¿½, actualizamos la partida con los datos reales del server
      this.partida = JSON.parse(JSON.stringify(estadoFinal));
      this.cdr.detectChanges();

      // --- ?? 6. Lï¿½GICA DEL TURNO DEL RIVAL (BOT) ---
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

          console.log("?? Disparando IA en el backend...");
          const estadoPostBot: any = await firstValueFrom(this.battleService.jugarBot(this.matchId!));

          // El bot ya decidiï¿½, apagamos el cartelito de "pensando"
          this.botPensando = false;
          this.cdr.detectChanges();

          console.log("? IA finalizada. Animando ataques del bot...");
          // Esta funciï¿½n debe procesar el estado final y activar el reloj de tu turno al terminar
          this.ejecutarIAEnemigaConData(estadoPostBot);

        } catch (err: any) {
          console.error("? Error crï¿½tico en la IA del Bot:", err);
          this.botPensando = false;
          this.bloqueadoPorAnimacion = false;
          this.cargandoAccion = false;
          this.cdr.detectChanges();
        }

      } else {
        // Caso borde: el turno volviï¿½ a vos (ej: el bot no pudo jugar)
        console.log("?? Turno devuelto al jugador inmediatamente.");
        this.cargandoAccion = false;
        this.bloqueadoPorAnimacion = false;
        this.iniciarRelojTurno();
        this.cdr.detectChanges();
      }

    } catch (error: any) {
      // En caso de error, liberamos la UI para que no quede trabada
      this.cargandoAccion = false;
      this.bloqueadoPorAnimacion = false;
      console.error("? Error del servidor al pasar turno:", error);
      alert('Error de conexiï¿½n: ' + (error?.error ?? 'El servidor no responde'));
    }
  }
realizarAccion(habilidad: any): void {
    this.showHabilidadesPanel = false;
    if (!this.validarEnergiaAtaque(habilidad)) {
      alert('ï¿½No tenï¿½s suficiente energï¿½a para usar ' + habilidad.nombre + '!');
      return;
    }
    // IMPORTANTE: Ahora pasamos por acï¿½ siempre
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

    // Lï¿½gica de "Truco": Sincronizamos con lo que el backend ya decidiï¿½
    if (carasRestantes >= monedasRestantes) {
      esCara = true; 
    } else if (carasRestantes > 0) {
      esCara = Math.random() < (carasRestantes / monedasRestantes); 
    }

    if (esCara) carasAsignadas++;

    // Seteamos el estado visual de la moneda actual
    this.coinFlipAtaque!.monedas[i].estado = esCara ? 'cara' : 'cruz';
    
    // ?? Actualizamos el flag que usa el HTML para el cartel "LOGRADO/FALLï¿½"
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

  // ?? Cï¿½LCULO FINAL: Aseguramos que el daï¿½o extra mostrado sea el correcto
  this.coinFlipAtaque!.danioTotal = carasAsignadas * config.danioExtraPorCara;
  this.coinFlipAtaque!.terminado = true;
  this.cdr.detectChanges();

  // Pausa para que el usuario festeje (o llore) el resultado
  await this.delay(2000);
  
  // Limpieza
  this.coinFlipAtaque = null;
  // (this as any).resultadoMoneda = ''; // Opcional: limpiar para el prï¿½ximo ataque
  this.cdr.detectChanges();
}

  intentarAbrirHabilidades(event?: MouseEvent): void {
    if (event) { event.stopPropagation(); event.preventDefault(); }
    if (this.partida?.turnoActual === 'JUGADOR') {
      this.botEstaAtacando       = false;
      this.bloqueadoPorAnimacion = false;
    }
    if (this.cargandoAccion) { console.warn('?? Bloqueado por cargandoAccion'); return; }
    this.showHabilidadesPanel = !this.showHabilidadesPanel;
    this.cdr.detectChanges();
  }

 async jugarCarta(carta: any): Promise<void> {
    if (this.partida.turnoActual !== 'JUGADOR' || this.cargandoAccion) return;
    
    // 1. Manejo de Energï¿½as
    if (this.esEnergia(carta)) { 
      this.gestionarUnionEnergia(carta); 
      return; 
    }
    
    // 2. Manejo de Pokï¿½mon
    if (this.esPokemon(carta)) {
      
      // ?? A. CHEQUEO DE EVOLUCIï¿½N (Se revisa antes que la bajada normal)
      if (carta.evolvesFrom && this.puedeEvolucionar(carta)) {
        
        // Buscamos a quiï¿½n evolucionar (Prioriza al Activo, sino busca en la Banca)
        let target = null;
        if (this.partida.jugador.activo?.card?.nombre === carta.evolvesFrom) {
            target = this.partida.jugador.activo;
        } else {
            target = this.partida.jugador.banca.find((b: any) => b.card.nombre === carta.evolvesFrom);
        }

        if (target) {
            await this.ejecutarEvolucionVisual(carta, target);
        }
        return; // ?? Cortamos la ejecuciï¿½n acï¿½ para que no intente bajarlo a la banca como Bï¿½sico
      }

      // B. VALIDACIï¿½N DE ACTIVO VACï¿½O
      if (!this.partida.jugador.activo && this.partida.jugador.banca.length > 0) {
        alert('ï¿½Primero tenï¿½s que subir un Pokï¿½mon de tu banca al puesto activo!');
        return;
      }
      
      // C. BAJADA DE POKï¿½MON Bï¿½SICO A LA BANCA O ACTIVO
      this.gestionarBajadaPokemon(carta);
    }
  }

  async ejecutarEvolucionVisual(cartaEvolucion: any, target: any) {
    this.bloqueadoPorAnimacion = true;
    this.cargandoAccion = true;

    try {
      // 1. Avisamos al backend
      await firstValueFrom(this.battleService.evolucionar(this.matchId!, cartaEvolucion.id, target.card.id));

      // 2. Disparamos la luz blanca sobre el Pokï¿½mon objetivo
      this.animandoEvolucionId = target.card.id;
      this.cdr.detectChanges();

      // Dejamos que el brillo suba (600ms)
      await this.delay(600);

      // 3. Justo en el pico de la luz, pedimos la foto nueva con el Pokï¿½mon ya evolucionado
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
        error: (err) => { this.cargandoAccion = false; console.error(err); alert('No se pudo subir el Pokï¿½mon al puesto activo.'); }
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
    if (confirm(`ï¿½Querï¿½s retirar a ${this.partida.jugador.activo.card.nombre}? Costarï¿½ ${costo} energï¿½a(s).`)) {
      this.cargandoAccion = true;
      this.battleService.retirarPokemon(this.matchId!, suplente.card.id).subscribe({
        next: () => { this.cargarEstado(); this.cargandoAccion = false; },
        error: (err) => { this.cargandoAccion = false; alert(err.error || 'No tenï¿½s suficiente energï¿½a para retirarte.'); }
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
      error: (err) => { this.cargandoAccion = false; console.error(err); alert(err.error || 'No se pudo bajar el Pokï¿½mon.'); }
    });
  }

  private gestionarUnionEnergia(cartaEnergia: any): void {
    if (!this.partida.jugador.activo) { alert('ï¿½Necesitï¿½s un Pokï¿½mon activo!'); return; }
    this.cargandoAccion = true;
    this.battleService.unirEnergia(this.matchId!, this.partida.jugador.activo.card.id, cartaEnergia.id).subscribe({
      next: () => { this.cargandoAccion = false; this.cargarEstado(); },
      error: (err: any) => { this.cargandoAccion = false; console.error(err); alert('No se pudo unir la energï¿½a.'); }
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

  // -----------------------------------------------
  // ENERGï¿½A DRAG
  // -----------------------------------------------

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
        console.log(`Uniendo energï¿½a ${this.selectedEnergyId} a Pokï¿½mon ${targetId}`);
      }
    }
  }

  // -----------------------------------------------
  // VALIDACIï¿½N DE ENERGï¿½AS
  // -----------------------------------------------

  validarEnergiaAtaque(ataque: any): boolean {
    if (!ataque || !this.partida?.jugador?.activo) return false;

    const normalizarTipo = (tipo: string): string => {
      const t = tipo.toLowerCase();
      if (t.includes('grass')     || t.includes('planta'))              return 'Grass';
      if (t.includes('fire')      || t.includes('fuego'))               return 'Fire';
      if (t.includes('water')     || t.includes('agua'))                return 'Water';
      if (t.includes('lightning') || t.includes('elï¿½ctrica') || t.includes('electrica')) return 'Lightning';
      if (t.includes('psychic')   || t.includes('psï¿½quica')  || t.includes('psiquica'))  return 'Psychic';
      if (t.includes('fighting')  || t.includes('lucha'))               return 'Fighting';
      if (t.includes('darkness')  || t.includes('siniestra') || t.includes('oscuridad')) return 'Darkness';
      if (t.includes('metal')     || t.includes('acero'))               return 'Metal';
      if (t.includes('dragon')    || t.includes('dragï¿½n'))              return 'Dragon';
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
  // -----------------------------------------------
  // SPRITES Y UTILIDADES
  // -----------------------------------------------

  getSpriteBack(nombreCarta: string): string { return this.battleBoardUi.getSpriteBack(nombreCarta); }
  getSpriteFront(nombreCarta: string): string { return this.battleBoardUi.getSpriteFront(nombreCarta); }

  onSpriteError(event: Event): void { (event.target as HTMLImageElement).style.display = 'none'; }

  getHpPercent(pokemon: any): number { return this.battleBoardUi.getHpPercent(pokemon); }
  getHpMax(pokemon: any): number { return this.battleBoardUi.getHpMax(pokemon); }
  getImagenCarta(id: string): string { return this.battleBoardUi.getImagenCarta(id); }
  getEmptySlots(n: number): number[] { return this.battleBoardUi.getEmptySlots(n); }

  esEnergia(carta: any): boolean { return this.battleBoardUi.esEnergia(carta); }
  esPokemon(carta: any): boolean { return this.battleBoardUi.esPokemon(carta); }

  get manoAgrupada(): any[][] {
    const tamanoStack = 4;
    const mano = this.partida.jugador.mano;
    const stacks = [];
    for (let i = 0; i < mano.length; i += tamanoStack) stacks.push(mano.slice(i, i + tamanoStack));
    return stacks;
  }

  getEnergyName(tipo: string): string { return this.battleBoardUi.getEnergyName(tipo); }
  getEnergyColor(tipo: string): string { return this.battleBoardUi.getEnergyColor(tipo); }

  dispararParticulas(objetivo: 'bot' | 'jugador', tipoEnergia: string) {
    const color = this.getEnergyColor(tipoEnergia) || '#ffffff';
    const nuevasParticulas = [];

    for (let i = 0; i < 20; i++) {
      const angulo = Math.random() * Math.PI * 2;
      const distancia = 40 + Math.random() * 80;
      nuevasParticulas.push({
        color,
        tx: Math.cos(angulo) * distancia,
        ty: Math.sin(angulo) * distancia,
        size: 5 + Math.random() * 7,
        duracion: 0.4 + Math.random() * 0.5
      });
    }

    if (objetivo === 'bot') {
      this.particulasBot = nuevasParticulas;
      this.mostrarEfectoBot = true;
      this.animandoBotDanio = true;
    } else {
      this.particulasJugador = nuevasParticulas;
      this.mostrarEfectoJugador = true;
      this.animandoJugadorDanio = true;
    }

    this.cdr.detectChanges();
    setTimeout(() => {
      if (objetivo === 'bot') {
        this.mostrarEfectoBot = false;
        this.animandoBotDanio = false;
      } else {
        this.mostrarEfectoJugador = false;
        this.animandoJugadorDanio = false;
      }
      this.cdr.detectChanges();
    }, 900);
  }

  // -----------------------------------------------
  // NAVEGACION
  // -----------------------------------------------

  volverAlLobby(): void { this.router.navigate(['/lobby']); }

  private delay(ms: number): Promise<void> { return new Promise(resolve => setTimeout(resolve, ms)); }

}

