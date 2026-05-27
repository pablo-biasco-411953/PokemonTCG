import { ChangeDetectorRef, Component, HostListener, OnDestroy, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { BattleBoardAbilitiesPanelComponent } from './battle-board-abilities-panel.component';
import { BattleBoardCardDetailPanelComponent } from './battle-board-card-detail-panel.component';
import { BattleBoardDebugPanelComponent } from './battle-board-debug-panel.component';
import { BattleBoardDiscardModalComponent } from './battle-board-discard-modal.component';
import { BattleService } from './services/battle.service';
import { BattleBoardUiService } from './services/battle-board-ui.service';
import {
  BattleBoardAttackService,
  CoinFlipConfig,
} from './services/battle-board-attack.service';
import { Router, ActivatedRoute } from '@angular/router';
import { DragDropModule, CdkDragDrop } from '@angular/cdk/drag-drop';
import { firstValueFrom } from 'rxjs';
import { SafeHtml } from '@angular/platform-browser';
import { Card } from '../../shared/models/card';
import { BattleActionCard, CartaEnJuego, Partida } from '../../shared/models/battle';
import { BattleBoardActionService } from './services/battle-board-action.service';
import { BattleBoardCombatService } from './services/battle-board-combat.service';
import { BattleBoardStateService } from './services/battle-board-state.service';
import { BattleBoardTurnService } from './services/battle-board-turn.service';
import {
  AttackCoinFlipState,
  BattleBoardAttack,
  CardGlossaryEntry,
  CoinSide,
  DamageNumberState,
  HoveredBattleCard,
  InterTurnOverlayState,
  ParticleVisualState,
} from './battle-board.types';

@Component({
  selector: 'app-battle-board',
  templateUrl: './battle-board.component.html',
  styleUrls: ['./battle-board.component.scss'],
  standalone: true,
  imports: [
    CommonModule,
    DragDropModule,
    BattleBoardAbilitiesPanelComponent,
    BattleBoardCardDetailPanelComponent,
    BattleBoardDebugPanelComponent,
    BattleBoardDiscardModalComponent,
  ],
})
export class BattleBoardComponent implements OnInit, OnDestroy {
  // Pantalla principal de batalla: mezcla estado remoto, animaciones y acciones del jugador.
  public Math = Math;
  readonly handDropListId = 'player-hand-dropzone';
  readonly activeDropListId = 'player-active-dropzone';
  readonly benchDropListId = 'player-bench-dropzone';
  readonly playableDropListIds = [this.handDropListId, this.activeDropListId, this.benchDropListId];
  matchId: string | null = null;
  partida: Partida | null = null;
  jugadorNombre = '';

  // Gesto de moneda
  private yStart = 0;
  private yEnd = 0;
  public lanzada = false;
  hoveredCard: Card | BattleActionCard | null = null;
  hoveredCardStatuses: CardGlossaryEntry[] = [];
  hoveredCardList: HoveredBattleCard[] = []; // Lista de cartas actual (por ejemplo, la mano).
  hoveredCardIndex = -1; // Índice de la carta resaltada dentro de la lista actual.
  // Tracking de cartas nuevas (jugador)
  public cartasNuevas = new Set<string>();
  private manoAnteriorIds = new Set<string>();

  // Tracking de cartas nuevas (bot)
  public cartasNuevasBot = new Set<string>();
  private manoAnteriorIdsBot = new Set<string>();
  healedTextPlayer: string | null = null;
  curingParalysisPlayer = false;
  curingSleepPlayer = false;

  healedTextBot: string | null = null;
  curingParalysisBot = false;
  curingSleepBot = false;
  // Estado visual
  vibrarBot = false;
  cargandoAccion = false;
  boardVisible = false;
  showIntro = true;
  introFadingOut = false;
  showTurnOverlay = false;
  tiempoTurnoMaximo = 60;
  tiempoRestante = 60;
  porcentajeTimer = 100;
  timerInterval: ReturnType<typeof setInterval> | null = null;
  botPensando = false;
  esperandoMiNuevoTurno = false;
  turnoOverlayTipo: 'jugador' | 'bot' = 'jugador';
  public modoSeleccionRetirada = false;

  // Animaciones y Panel
  animandoAtaque = false;
  public animandoBotAtaque = false;
  showImpactFlash = false;
  showHabilidadesPanel = false;

  isDraggingEnergy = false;
  originPos = { x: 0, y: 0 };
  mousePos = { x: 0, y: 0 };
  selectedEnergyId: string | null = null;

  private ataqueRealizado = false;
  private pollingPartida: ReturnType<typeof setInterval> | null = null;
  public botEstaAtacando = false;
  private datosPendientesBot: Partida | null = null;
  animandoEvolucionId: string | null = null;
  // Variables internas
  public activoVisualJugador: CartaEnJuego | null = null;
  public hpRenderJugador = 0;
  public bloqueadoPorAnimacion = false;
  public anguloFinal = 0;
  isScrollingMode = false;
  scrollTimeout: ReturnType<typeof setTimeout> | null = null;
  lastScrollTime = 0;
  private intentosBotSinAccion = 0;
  private ultimaCantidadCartasBot = -1;
  private ciclosSinCambio = 0;
  private hpVisualInterno: number = 0;
  mostrarModalDescarte = false;
  cartasParaVerEnDescarte: Card[] = [];
  tituloDescarteActual = '';

  // Verifica si una evolución de la mano tiene un objetivo válido en mesa.
  puedeEvolucionar(cartaMano: BattleActionCard | Card): boolean {
    return this.battleBoardState.puedeEvolucionar(this.partida, cartaMano);
  }

  // CoinFlip
  public estadoCoinFlip:
    | 'ELEGIR_LADO'
    | 'ESPERANDO_TIRO'
    | 'GIRANDO'
    | 'ELEGIR_TURNO'
    | 'RESULTADO_BOT'
    | 'OCULTO' = 'OCULTO';
  eleccionJugador: 'CARA' | 'CRUZ' = 'CARA';
  resultadoMoneda: CoinSide = 'CARA';
  public girando = false;

  // -- Estado de efectos --
  mostrarAuraCuracionBot = false;
  mostrarAuraCuracionPlayer = false;
  mostrarKO = false;

  // Números de daño flotantes.
  damageNumberBot: DamageNumberState | null = null;
  damageNumberPlayer: DamageNumberState | null = null;

  // Partículas y efectos visuales.
  mostrarEfectoBot = false;
  mostrarEfectoJugador = false;
  particulasBot: ParticleVisualState[] = [];
  particulasJugador: ParticleVisualState[] = [];
  animandoBotDanio = false;
  animandoJugadorDanio = false;

  constructor(
    private battleService: BattleService,
    private route: ActivatedRoute,
    private router: Router,
    private cdr: ChangeDetectorRef,
    private battleBoardUi: BattleBoardUiService,
    private battleBoardAttack: BattleBoardAttackService,
    private battleBoardAction: BattleBoardActionService,
    private battleBoardCombat: BattleBoardCombatService,
    private battleBoardState: BattleBoardStateService,
    private battleBoardTurn: BattleBoardTurnService,
  ) {}

  // -----------------------------------------------
  // LIFECYCLE
  // -----------------------------------------------

  ngOnInit(): void {
    this.matchId = this.route.snapshot.paramMap.get('id');
    if (!this.matchId) return;
    this.cargarCatalogoGodMode();
    this.showIntro = true;
    setTimeout(() => (this.introFadingOut = true), 2000);
    setTimeout(() => (this.showIntro = false), 3000);

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
          this.manoAnteriorIds = new Set((data.jugador?.mano || []).map((c: any) => c.id));
          this.manoAnteriorIdsBot = new Set((data.bot?.mano || []).map((c: any) => c.id));
          this.finalizarCoinFlip();
        }
      },
    });
  }

  // Limpia polling al salir de la partida.
  ngOnDestroy(): void {
    if (this.pollingPartida) clearInterval(this.pollingPartida);
  }

  // -----------------------------------------------
  // COIN FLIP
  // -----------------------------------------------

  // Guarda la eleccion del jugador y habilita el gesto de lanzamiento.
  iniciarSorteo(eleccion: 'CARA' | 'CRUZ') {
    this.eleccionJugador = eleccion;
    this.estadoCoinFlip = 'ESPERANDO_TIRO';
    this.lanzada = false;
    this.cdr.detectChanges();
  }

  // Confirma en backend quien toma el primer turno.
  async seleccionarTurno(yoVoyPrimero: boolean) {
    try {
      await firstValueFrom(this.battleService.elegirTurno(this.matchId!, yoVoyPrimero));
      this.finalizarCoinFlip();
    } catch (error) {
      console.error('Error al elegir turno:', error);
      this.finalizarCoinFlip();
    }
  }

  // Inicia el temporizador visual del turno del jugador.
  iniciarRelojTurno() {
    this.detenerRelojTurno();
    this.tiempoRestante = this.tiempoTurnoMaximo;
    this.porcentajeTimer = 100;

    this.timerInterval = setInterval(() => {
      if (
        this.partida?.turnoActual !== 'JUGADOR' ||
        this.cargandoAccion ||
        this.bloqueadoPorAnimacion
      ) {
        return;
      }

      this.tiempoRestante--;
      this.porcentajeTimer = (this.tiempoRestante / this.tiempoTurnoMaximo) * 100;

      // Forzamos a Angular a refrescar barra y texto del timer.
      this.cdr.detectChanges();

      if (this.tiempoRestante <= 0) {
        this.ejecutarTimeOut();
      }
    }, 1000);
  }

  // Detiene el temporizador cuando cambia el turno o termina una accion.
  detenerRelojTurno() {
    if (this.timerInterval) {
      clearInterval(this.timerInterval);
      this.timerInterval = null;
    }
  }

  // Pasa el turno automáticamente si el jugador se queda sin tiempo.
  async ejecutarTimeOut() {
    this.detenerRelojTurno();
    console.log('Tiempo agotado. Pasando turno automáticamente...');
    await this.pasarTurno();
  }

  // Oculta la interfaz del sorteo y muestra el tablero principal.
  finalizarCoinFlip() {
    this.estadoCoinFlip = 'OCULTO';
    this.lanzada = false;
    this.girando = false;
    this.boardVisible = true;
    this.cargarEstado();
    this.cdr.detectChanges();
  }

  // Guarda el punto inicial del gesto de lanzamiento.
  onMouseDown(event: MouseEvent) {
    this.yStart = event.clientY;
  }

  interTurnOverlay: InterTurnOverlayState | null = null;

  // -- Coin flip de ataque --
  coinFlipAtaque: AttackCoinFlipState | null = null;

  async mostrarInterTurn(
    tipo: 'jugador' | 'bot' | 'neutral',
    titulo: string,
    subtitulo = '',
    duracion = 2000,
  ): Promise<void> {
    const fase =
      tipo === 'jugador' ? 'INICIO DE TURNO' : tipo === 'bot' ? 'TURNO DEL RIVAL' : 'ENTRE TURNOS';

    this.interTurnOverlay = { titulo, subtitulo, fase, tipo, duracion };
    this.cdr.detectChanges();

    await this.delay(duracion);

    // Fade out
    this.interTurnOverlay = null;
    this.cdr.detectChanges();

    // Pequeña pausa de limpieza.
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
    if (objetivo === 'bot') this.mostrarAuraCuracionBot = true;
    else this.mostrarAuraCuracionPlayer = true;
    this.cdr.detectChanges();

    await this.delay(duracion);

    if (objetivo === 'bot') this.mostrarAuraCuracionBot = false;
    else this.mostrarAuraCuracionPlayer = false;
    this.cdr.detectChanges();
  }

  mostrarDamageNumber(objetivo: 'bot' | 'jugador', valor: number, esCuracion = false): void {
    const num: DamageNumberState = { valor, esCuracion };
    if (objetivo === 'bot') {
      this.damageNumberBot = num;
      setTimeout(() => {
        this.damageNumberBot = null;
        this.cdr.detectChanges();
      }, 1000);
    } else {
      this.damageNumberPlayer = num;
      setTimeout(() => {
        this.damageNumberPlayer = null;
        this.cdr.detectChanges();
      }, 1000);
    }
    this.cdr.detectChanges();
  }

  async onMouseUp(event: MouseEvent) {
    if (this.lanzada || this.estadoCoinFlip !== 'ESPERANDO_TIRO') return;

    this.yEnd = event.clientY;
    const diferencia = this.yStart - this.yEnd;
    const fuerza = Math.min(Math.max(diferencia, 50), 400);

    if (fuerza > 50) {
      this.lanzada = true;
      this.girando = true;
      this.estadoCoinFlip = 'GIRANDO';

      const duracionVuelo = 1.8;
      const vueltasBase = 5 + Math.floor(fuerza / 50);

      document.documentElement.style.setProperty('--altura-vuelo', `-${fuerza * 1.3}px`);
      document.documentElement.style.setProperty('--duracion-vuelo', `${duracionVuelo}s`);

      this.cdr.detectChanges();

      try {
        const salioCara = await firstValueFrom(this.battleService.lanzarMoneda(this.matchId!));
        this.resultadoMoneda = salioCara ? 'CARA' : 'CRUZ';

        this.anguloFinal =
          this.resultadoMoneda === 'CARA' ? vueltasBase * 360 : vueltasBase * 360 + 180;

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

  // Navegación con la rueda del mouse dentro de la mano.
  @HostListener('window:wheel', ['$event'])
  onScrollCard(event: WheelEvent) {
    if (
      this.hoveredCard &&
      this.hoveredCardList === this.partida?.jugador?.mano &&
      this.hoveredCardList.length > 1
    ) {
      event.preventDefault();

      const now = Date.now();

      // Bloqueamos temporalmente el hover físico del mouse.
      this.isScrollingMode = true;
      if (this.scrollTimeout) clearTimeout(this.scrollTimeout);
      this.scrollTimeout = setTimeout(() => {
        this.isScrollingMode = false; // Se desbloquea 200 ms después de dejar de girar.
      }, 200);

      // Freno de velocidad: solo permite 1 salto de carta cada 120 ms.
      if (now - this.lastScrollTime < 120) return;
      this.lastScrollTime = now;

      // Avanza o retrocede el índice de la carta mostrada.
      if (event.deltaY > 0) {
        this.hoveredCardIndex = (this.hoveredCardIndex + 1) % this.hoveredCardList.length;
      } else if (event.deltaY < 0) {
        this.hoveredCardIndex =
          (this.hoveredCardIndex - 1 + this.hoveredCardList.length) % this.hoveredCardList.length;
      }

      // Actualizamos el panel de detalle.
      const nextItem = this.hoveredCardList[this.hoveredCardIndex];
      const cartaReal: Card | BattleActionCard = 'card' in nextItem ? nextItem.card : nextItem;

      this.hoveredCard = cartaReal;
      this.hoveredCardStatuses = this.extraerGlosario(cartaReal);
    }
  }

  // Click central para jugar la carta actualmente enfocada.
  @HostListener('window:mousedown', ['$event'])
  onMiddleClick(event: MouseEvent) {
    // event.button === 1 indica el botón central.
    if (
      event.button === 1 &&
      this.hoveredCard &&
      this.hoveredCardList === this.partida?.jugador?.mano
    ) {
      event.preventDefault();

      console.log('Click central: jugando carta', this.hoveredCard.nombre);
      this.jugarCarta(this.hoveredCard); // Juega la carta que se ve en el panel grande.
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

      // Prendemos o apagamos el medidor para no gastar recursos si está cerrado
      if (this.showDebugPanel) {
        this.iniciarMedidorRendimiento();
      } else {
        this.detenerMedidorRendimiento();
      }
    }
  }

  // ?? LÓGICA DE RENDIMIENTO (FPS Y MEMORIA)
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
    const wakeCheck = this.battleBoardTurn.evaluarDespertar(
      this.partida?.jugador?.activo,
      estadoFinal?.jugador?.activo,
    );

    // Caso: el jugador estaba dormido y el server resolvió la moneda.
    if (wakeCheck) {
      console.log('Lanzando moneda de despertar...');

      // Reutilizamos el overlay de monedas con un evento ficticio.
      const configFicticia = {
        descripcion: '¿Se despierta?',
        cantidadMonedas: 1,
        esSoloEstado: true,
        danioBase: 0,
        danioExtraPorCara: 0,
      };

      await this.animarMonedasSincronizadas(
        '¿Se despierta?',
        configFicticia,
        wakeCheck.seDesperto ? 1 : 0,
        true,
      );

      if (wakeCheck.seDesperto) {
        console.log('Se despertó.');
      } else {
        console.log('Sigue dormido.');
      }
    }
  }

  extraerGlosario(carta: Card | BattleActionCard): CardGlossaryEntry[] {
    return this.battleBoardUi.extraerGlosario(carta);
  }

  formatTextoAtaque(texto: string): SafeHtml {
    return this.battleBoardUi.formatTextoAtaque(texto);
  }

  setHoveredCard(item: any, list: any[] = [], index: number = -1) {
    // ?? FIX: Si estamos girando la ruedita, ignoramos los choques físicos del mouse
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
    esSoloEstado: boolean,
  ): Promise<number> {
    // Inicializamos el estado del coin flip.
    this.coinFlipAtaque = {
      nombreAtaque,
      descripcion,
      cantidadMonedas,
      danioBase,
      danioExtraPorCara,
      monedas: Array(cantidadMonedas)
        .fill(null)
        .map(() => ({ estado: 'girando' as const })),
      danioTotal: 0,
      terminado: false,
      progreso: 0,
      esSoloEstado,
    };

    this.cdr.detectChanges();
    // Pequeña pausa para que el overlay aparezca.
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
    const danioTotal = danioBase + caras * danioExtraPorCara;
    this.coinFlipAtaque!.danioTotal = danioTotal;
    this.coinFlipAtaque!.terminado = true;
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
    esSoloEstado: boolean;
  } | null {
    return this.battleBoardAttack.detectarCoinFlipAtaque(
      ataque,
      (texto, cantidadMonedas, danioExtraPorCara, esMultiplicador, esFalloCruz, esSoloEstado) =>
        this.traducirEfectoCoinFlip(
          texto,
          cantidadMonedas,
          danioExtraPorCara,
          esMultiplicador,
          esFalloCruz,
          esSoloEstado,
        ),
    );
  }

  async procesarEventosPostEstado(estadoAnterior: any, estadoNuevo: any): Promise<void> {
    // -- KO del Pokémon del bot --
    const botActivoAntes = estadoAnterior?.bot?.activo;
    const botActivoAhora = estadoNuevo?.bot?.activo;
    if (botActivoAntes && !botActivoAhora) {
      await this.mostrarKOAnim();
    }

    // -- KO del Pokémon del jugador --
    const playerActivoAntes = estadoAnterior?.jugador?.activo;
    const playerActivoAhora = estadoNuevo?.jugador?.activo;
    if (playerActivoAntes && !playerActivoAhora) {
      await this.mostrarKOAnim();
    }

    // -- Daño al bot --
    const hpBotAntes = botActivoAntes?.hpActual ?? 0;
    const hpBotAhora = botActivoAhora?.hpActual ?? 0;
    if (botActivoAntes && botActivoAhora && hpBotAhora < hpBotAntes) {
      this.mostrarDamageNumber('bot', hpBotAntes - hpBotAhora);
    }

    // -- Curación del bot --
    if (botActivoAntes && botActivoAhora && hpBotAhora > hpBotAntes) {
      const curado = hpBotAhora - hpBotAntes;
      this.mostrarDamageNumber('bot', curado, true);
      this.mostrarCuracion('bot');
    }

    // -- Daño al jugador --
    const hpPlayerAntes = playerActivoAntes?.hpActual ?? 0;
    const hpPlayerAhora = playerActivoAhora?.hpActual ?? 0;
    if (playerActivoAntes && playerActivoAhora && hpPlayerAhora < hpPlayerAntes) {
      this.mostrarDamageNumber('jugador', hpPlayerAntes - hpPlayerAhora);
    }

    // -- Curación del jugador --
    if (playerActivoAntes && playerActivoAhora && hpPlayerAhora > hpPlayerAntes) {
      const curado = hpPlayerAhora - hpPlayerAntes;
      this.mostrarDamageNumber('jugador', curado, true);
      this.mostrarCuracion('jugador');
    }
  }

  debugFullCatalog: Card[] = [];
  debugFilteredCatalog: Card[] = [];
  debugSelectedIndex = 0;

  // Criterios de búsqueda del panel debug.
  debugSearchText = '';
  debugSearchSupertype = '';

  // Carga el catálogo debug usado por las herramientas de prueba.
  cargarCatalogoGodMode() {
    this.battleService.getCardCatalogDebug().subscribe({
      next: (cartas) => {
        this.debugFullCatalog = cartas;

        this.aplicarFiltrosDebug();
        console.log(`God Mode: catálogo cargado con ${cartas.length} cartas.`);
      },
      error: (err) => console.error('Error cargando catálogo de God Mode:', err),
    });
  }

  // Actualiza el filtro textual del panel debug.
  actualizarFiltroTexto(value: string | Event) {
    const rawValue =
      typeof value === 'string' ? value : ((value.target as HTMLInputElement | null)?.value ?? '');
    this.debugSearchText = rawValue.toLowerCase();
    this.aplicarFiltrosDebug();
  }

  // Actualiza el filtro por supertipo del panel debug.
  actualizarFiltroTipo(value: string | Event) {
    this.debugSearchSupertype =
      typeof value === 'string' ? value : ((value.target as HTMLSelectElement | null)?.value ?? '');
    this.aplicarFiltrosDebug();
  }

  // Aplica filtros y reinicia el carrusel del catalogo debug.
  aplicarFiltrosDebug() {
    this.debugFilteredCatalog = this.debugFullCatalog.filter((c) => {
      // 1. Filtro por tipo (Pokémon, Trainer, Energy).
      const matchTipo = !this.debugSearchSupertype || c.supertype === this.debugSearchSupertype;

      // 2. Filtro por texto en nombre, ataques o efectos.
      let matchTexto = true;
      if (this.debugSearchText) {
        const nombreMatch = c.nombre?.toLowerCase().includes(this.debugSearchText);

        // Buscamos dentro de los ataques por palabras clave.
        const ataquesMatch = c.ataques?.some(
          (atk: any) =>
            atk.nombre?.toLowerCase().includes(this.debugSearchText) ||
            atk.texto?.toLowerCase().includes(this.debugSearchText),
        );

        matchTexto = nombreMatch || !!ataquesMatch;
      }

      return matchTipo && matchTexto;
    });

    this.debugSelectedIndex = 0; // Reseteamos el carrusel al primer resultado.
  }

  // Devuelve la carta debug actualmente enfocada.
  get debugSelectedCard(): Card | null {
    if (!this.debugFilteredCatalog || this.debugFilteredCatalog.length === 0) return null;
    return this.debugFilteredCatalog[this.debugSelectedIndex];
  }

  // Avanza a la siguiente carta del carrusel debug.
  nextDebugCard() {
    if (this.debugFilteredCatalog.length === 0) return;
    this.debugSelectedIndex = (this.debugSelectedIndex + 1) % this.debugFilteredCatalog.length;
  }

  // Retrocede a la carta anterior del carrusel debug.
  prevDebugCard() {
    if (this.debugFilteredCatalog.length === 0) return;
    this.debugSelectedIndex =
      (this.debugSelectedIndex - 1 + this.debugFilteredCatalog.length) %
      this.debugFilteredCatalog.length;
  }
  // Traduce el texto técnico del coin flip a una descripción visible.
  private traducirEfectoCoinFlip(
    textoOriginal: string,
    monedas: number,
    danio: number,
    esMultiplicador: boolean,
    esFalloCruz: boolean,
    esSoloEstado: boolean,
  ): string {
    const numStr = monedas === 1 ? 'una moneda' : `${monedas} monedas`;

    if (esSoloEstado) {
      if (textoOriginal.includes('paralyzed'))
        return `Lanzá ${numStr}. Si sale CARA, el rival queda paralizado.`;
      if (textoOriginal.includes('asleep'))
        return `Lanzá ${numStr}. Si sale CARA, el rival queda dormido.`;
      return `Lanzá ${numStr} para aplicar un efecto especial.`;
    }

    if (esFalloCruz) return `Lanzá ${numStr}. Si sale CRUZ, el ataque falla.`;
    if (esMultiplicador) return `Lanzá ${numStr}. Hace ${danio} de daño por cada CARA.`;

    return `Lanzá ${numStr}. Hace ${danio} de daño extra por cada CARA.`;
  }
  // -----------------------------------------------
  // CARGA DE ESTADO
  // -----------------------------------------------

  // Sincroniza el estado de batalla con el backend.
  cargarEstado(): void {
    if (!this.matchId || this.bloqueadoPorAnimacion || this.botEstaAtacando || this.botPensando)
      return;
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

        // --- DETECCIÓN DE CARTAS NUEVAS ---
        if (data.jugador?.mano) {
          this.detectarCartasNuevas(data.jugador.mano);
        }
        if (data.bot?.mano) {
          this.detectarCartasNuevasBot(data.bot.mano);
        }

        this.cdr.detectChanges(); // tick intermedio

        // ?? 1. GUARDAMOS DE QUIÉN ERA EL TURNO ANTES DE ACTUALIZAR
        const turnoAnterior = this.partida?.turnoActual;

        // --- ACTUALIZACIÓN DEL ESTADO ---
        this.partida = data;

        if (this.esperandoMiNuevoTurno && this.partida.turnoActual === 'JUGADOR') {
          this.esperandoMiNuevoTurno = false;
          this.iniciarRelojTurno(); // ¡Prende la mecha de 60 segundos!
        } else if (this.partida.turnoActual === 'BOT') {
          this.detenerRelojTurno();
        }

        // ?? 2. CHEQUEAMOS SI ARRANCÓ TU TURNO PARA PRENDER LA MECHA
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
      },
    });
  }

  // -----------------------------------------------
  // DETECCIÓN DE CARTAS NUEVAS
  // -----------------------------------------------

  private detectarCartasNuevas(nuevaMano: any[]): void {
    // Marca visualmente cartas nuevas en la mano del jugador.
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
    // Marca visualmente cartas nuevas en la mano del bot.
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
    const maxAngle = Math.min(4 * total, 35);
    const angleStep = total > 1 ? (maxAngle * 2) / (total - 1) : 0;
    const angle = total > 1 ? -maxAngle + angleStep * index : 0;
    const cardSpacing = Math.min(80, 480 / Math.max(total, 1));
    const totalWidth = cardSpacing * (total - 1);
    const offsetX = -totalWidth / 2 + cardSpacing * index;
    const normalizedPos = total > 1 ? (index / (total - 1)) * 2 - 1 : 0;
    const offsetY = normalizedPos * normalizedPos * 20;
    return `translateX(calc(-50% + ${offsetX}px)) translateY(${offsetY}px) rotate(${angle}deg)`;
  }

  // -----------------------------------------------
  // POLLING
  // -----------------------------------------------

  iniciarPolling(): void {
    // Inicia el refresco periodico cuando el bot esta jugando.
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
    // Muestra la transicion visual entre turnos.
    this.turnoOverlayTipo = turno;
    this.showTurnOverlay = true;
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
    // Orquesta la transicion dramatica hacia el turno del bot.
    this.bloqueadoPorAnimacion = true;
    this.partida = dataServidor;
    setTimeout(() => {
      this.turnoOverlayTipo = 'bot';
      this.showTurnOverlay = true;
      this.cdr.detectChanges();
      setTimeout(() => {
        this.showTurnOverlay = false;
        this.bloqueadoPorAnimacion = false;
        this.cdr.detectChanges();
      }, 2000);
    }, 1000);
  }

  iniciarTransicionTurnoBot(nuevoEstado: any) {
    this.bloqueadoPorAnimacion = true;
    this.turnoOverlayTipo = 'bot';
    this.showTurnOverlay = true;
    this.cdr.detectChanges();
    setTimeout(() => {
      this.showTurnOverlay = false;
      this.partida = nuevoEstado;
      this.cdr.detectChanges();
      setTimeout(() => {
        this.bloqueadoPorAnimacion = false;
      }, 500);
    }, 2000);
  }

  actualizarSeguridadEstado(data: any) {
    if (data.turnoActual === 'JUGADOR') {
      this.botEstaAtacando = false;
      this.bloqueadoPorAnimacion = false;
      this.cargandoAccion = false;
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
    // ?? 1. GUARDAMOS LA FOTO ANTES DE QUE EL BOT ACTÚE
    const estadoAntiguo = this.battleBoardState.clonarPartida(this.partida);
    const analisisBot = this.battleBoardTurn.analizarTurnoBot(
      estadoAntiguo,
      estadoFinal,
      this.hpRenderJugador,
    );
    const wakeCheckBot = this.battleBoardTurn.evaluarDespertar(
      estadoAntiguo?.bot?.activo,
      estadoFinal?.bot?.activo,
    );

    // ?? 2. FASE DE MANTENIMIENTO DEL BOT (MONEDA DE DESPERTAR)
    // Lo hacemos ANTES de procesar el ataque
    if (wakeCheckBot) {
      console.log('Checkup del bot. ¿Se despierta?', wakeCheckBot.seDesperto);
      await this.reproducirChequeoDespertar('¿Se despierta el rival?', wakeCheckBot.seDesperto);
    }

    // ?? 3. LÓGICA DE DECISIÓN (¿El bot ataca realmente?)
    const { botAtaco, hpJugadorDespues, danioHecho } = analisisBot;
    const activoBotDespues = estadoFinal?.bot?.activo;

    // ?? 4. ESCANEAMOS CURACIONES/DAÑOS PASIVOS
    await this.verificarEstadosCurados(estadoAntiguo, estadoFinal);

    // ? 5. SALIDA SIN ATAQUE (si falló la moneda o estaba paralizado)
    if (!botAtaco) {
      this.aplicarEstadoRefrescado(estadoFinal);

      this.limpiarBanderasBot(); // Método auxiliar para no repetir código

      if (this.partida?.turnoActual === 'JUGADOR') {
        this.iniciarRelojTurno();
      }
      this.cdr.detectChanges();
      return;
    }

    // ?? 6. SI ATACÓ, ANIMAMOS EL IMPACTO Y CERRAMOS LA SECUENCIA
    this.botEstaAtacando = true;
    this.animandoBotAtaque = true;
    this.cdr.detectChanges();

    if (activoBotDespues && activoBotDespues.card?.ataques?.length > 0) {
      const habilidadBot = activoBotDespues.card.ataques[0];
      const coinConfig = this.detectarCoinFlipAtaque(habilidadBot);

      if (coinConfig) {
        const carasReales = this.battleBoardTurn.resolverCarasBot(
          coinConfig,
          estadoFinal,
          danioHecho,
        );
        this.resultadoMoneda = this.battleBoardTurn.obtenerResultadoMoneda(
          coinConfig.cantidadMonedas,
          carasReales,
        );
        await this.animarMonedasSincronizadas(
          habilidadBot.nombre,
          coinConfig,
          carasReales,
          coinConfig.esSoloEstado,
        );
      }
    }

    // 7. EL MOMENTO DEL IMPACTO
    this.showImpactFlash = true;
    this.aplicarEstadoRefrescado(estadoFinal);
    this.cdr.detectChanges();

    await this.delay(150);
    this.showImpactFlash = false;
    this.cdr.detectChanges();

    // ?? 8. FINAL DE LA SECUENCIA
    await this.delay(600);
    this.limpiarBanderasBot();

    if (this.partida?.turnoActual === 'JUGADOR') {
      console.log('? Turno del bot finalizado. Iniciando reloj de jugador.');
      this.iniciarRelojTurno();
    }
    this.cdr.detectChanges();
  }

  // Método auxiliar para limpiar estados
  private limpiarBanderasBot() {
    this.animandoBotAtaque = false;
    this.botEstaAtacando = false;
    this.cargandoAccion = false;
    this.bloqueadoPorAnimacion = false;
    this.ataqueRealizado = false;
    this.botPensando = false;
    this.esperandoMiNuevoTurno = false;
    this.resultadoMoneda = '';
  }

  async refrescarTableroDebug() {
    try {
      const nuevoEstado: any = await firstValueFrom(this.battleService.getState(this.matchId!));
      this.partida = this.battleBoardState.clonarPartida(nuevoEstado);
      this.cdr.detectChanges();
      console.log('Tablero recargado manualmente.');
    } catch (error) {
      console.error('Error recargando tablero en modo debug:', error);
    }
  }

  // Herramienta 1: robar carta manualmente.
  async debugRobarCarta(cardId: string) {
    if (!cardId) return;
    try {
      console.log(`GOD MODE: inyectando carta ${cardId} a la mano...`);
      await firstValueFrom(this.battleService.debugDrawCard(this.matchId!, cardId));
      await this.refrescarTableroDebug();
    } catch (err) {
      console.error('Error en God Mode (Robar Carta):', err);
      alert('Falla en God Mode. ¿Ya creaste el endpoint en Java?');
    }
  }

  // Herramienta 2: Forzar Estado (Dormir, Paralizar, etc.)
  async debugForzarEstado(objetivo: 'JUGADOR' | 'BOT', estado: string) {
    try {
      console.log(`GOD MODE: forzando estado ${estado} a ${objetivo}...`);
      await firstValueFrom(this.battleService.debugForzarEstado(this.matchId!, objetivo, estado));
      await this.refrescarTableroDebug();
    } catch (err) {
      console.error('Error en God Mode (Forzar Estado):', err);
    }
  }

  // Herramienta 3: setear HP.
  async debugSetHp(objetivo: 'JUGADOR' | 'BOT', hp: number) {
    try {
      console.log(`GOD MODE: seteando HP de ${objetivo} a ${hp}...`);
      await firstValueFrom(this.battleService.debugSetHp(this.matchId!, objetivo, hp));
      await this.refrescarTableroDebug();
    } catch (err) {
      console.error('Error en God Mode (Set HP):', err);
    }
  }

  abrirModalDescarte(quien: 'JUGADOR' | 'BOT') {
    console.log('Descarte abierto:', quien);
    const pila =
      quien === 'JUGADOR' ? this.partida?.jugador?.pilaDescarte : this.partida?.bot?.pilaDescarte;

    if (pila) {
      this.tituloDescarteActual = quien === 'JUGADOR' ? 'TU DESCARTE' : 'DESCARTE RIVAL';
      this.cartasParaVerEnDescarte = [...pila].reverse();
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
      console.log('⚡ Jugador curado de parálisis!');
      await this.animarCuraEstado('jugador', 'Paralyzed', '¡Parálisis curada!');
    }
    if (condViejoJugador.includes('Asleep') && !condNuevoJugador.includes('Asleep')) {
      console.log('😴 Jugador despertó!');
      await this.animarCuraEstado('jugador', 'Asleep', '¡Se despertó!');
    }

    // --- REVISAMOS AL BOT ---
    const condViejoBot = estadoAnterior.bot?.activo?.condicionesEspeciales || [];
    const condNuevoBot = estadoNuevo.bot?.activo?.condicionesEspeciales || [];

    if (condViejoBot.includes('Paralyzed') && !condNuevoBot.includes('Paralyzed')) {
      console.log('⚡ Bot curado de parálisis!');
      await this.animarCuraEstado('bot', 'Paralyzed', '¡Parálisis curada!');
    }
    if (condViejoBot.includes('Asleep') && !condNuevoBot.includes('Asleep')) {
      console.log('😴 Bot despertó!');
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
          this.partida = data;
          this.hpRenderJugador = miHp;
          this.cdr.detectChanges();
          await new Promise((f) => setTimeout(f, 1200));
          this.turnoOverlayTipo = 'bot';
          this.showTurnOverlay = true;
          this.cdr.detectChanges();
          await new Promise((f) => setTimeout(f, 2000));
          this.showTurnOverlay = false;
          this.cdr.detectChanges();
          this.ejecutarIAEnemigaConData(data);
          return;
        }
        this.partida = data;
        this.hpRenderJugador = data.jugador?.activo?.hpActual || 0;
        this.cdr.detectChanges();
      },
    });
  }

  // -----------------------------------------------
  // ACCIONES DE JUEGO
  // -----------------------------------------------

  async ejecutarAtaqueSecuencia(nombreAtaque: string) {
    if (this.cargandoAccion || !nombreAtaque) return;

    const activoJugador = this.partida?.jugador?.activo;
    if (!activoJugador) return;

    this.bloqueadoPorAnimacion = true;
    this.cargandoAccion = true;
    this.ataqueRealizado = true;

    const habilidad = (activoJugador.card.ataques ?? []).find(
      (a: any) => a.nombre === nombreAtaque,
    );
    if (!habilidad) {
      this.bloqueadoPorAnimacion = false;
      this.cargandoAccion = false;
      this.ataqueRealizado = false;
      alert('No se encontró el ataque seleccionado.');
      return;
    }

    const tipoEnergia = habilidad.costo?.[0] || 'Colorless';

    try {
      const ataqueBloqueadoPorConfusion = await this.resolverConfusionPreAtaque(activoJugador);
      if (ataqueBloqueadoPorConfusion) {
        return;
      }

      const estadoFinal = await this.battleBoardCombat.atacarYRecargar(this.matchId!, nombreAtaque);
      await this.reproducirCoinFlipAtaqueJugador(habilidad, estadoFinal);
      await this.reproducirImpactoAtaqueJugador(tipoEnergia, estadoFinal);
      await this.finalizarSecuenciaAtaque(estadoFinal);
    } catch (error: any) {
      this.cargandoAccion = false;
      this.ataqueRealizado = false;
      this.bloqueadoPorAnimacion = false;
      console.error('Error en ataque:', error);
    }
  }
  async iniciarTurnoBot(estadoFinal: any) {
    await this.delay(1000);
    this.cargandoAccion = false;
    await this.mostrarOverlayTurnoBot();

    try {
      const estadoPostBot = await this.battleBoardCombat.ejecutarTurnoBot(this.matchId!);
      this.ejecutarIAEnemigaConData(estadoPostBot);
    } catch (err: any) {
      console.error('Error al ejecutar bot:', err);
      this.bloqueadoPorAnimacion = false;
    }
  }
  async pasarTurno(): Promise<void> {
    console.log("?? Botón 'Pasar Turno' presionado.");

    // ??? 1. VALIDACIONES DE SEGURIDAD
    if (this.partida?.turnoActual !== 'JUGADOR') {
      console.warn('? Bloqueado: No es tu turno.');
      return;
    }
    if (this.cargandoAccion) {
      console.warn('? Bloqueado: Hay una acción en curso.');
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
    const estadoAntiguo = this.battleBoardState.clonarPartida(this.partida);

    try {
      console.log('? Enviando fin de turno al servidor (Java)...');
      // Avisamos al backend que termine nuestro turno
      const estadoFinal = await this.battleBoardCombat.pasarTurnoYRecargar(this.matchId!);
      console.log('?? Datos de Checkup recibidos del server.');
      await this.procesarPostPassTurn(estadoAntiguo, estadoFinal);
    } catch (error: any) {
      // En caso de error, liberamos la UI para que no quede trabada
      this.cargandoAccion = false;
      this.bloqueadoPorAnimacion = false;
      console.error('? Error del servidor al pasar turno:', error);
      alert('Error de conexión: ' + (error?.error ?? 'El servidor no responde'));
    }
  }
  realizarAccion(habilidad: BattleBoardAttack): void {
    this.showHabilidadesPanel = false;
    if (!this.validarEnergiaAtaque(habilidad)) {
      alert('No tenés suficiente energía para usar ' + habilidad.nombre + '!');
      return;
    }
    // IMPORTANTE: Ahora pasamos por acá siempre
    this.ejecutarAtaqueSecuencia(habilidad.nombre);
  }
  async animarMonedasSincronizadas(
    nombreAtaque: string,
    config: CoinFlipConfig,
    carasForzadas: number,
    esSoloEstado: boolean,
  ): Promise<void> {
    // 1. Reiniciamos el estado visual
    this.resultadoMoneda = '';

    this.coinFlipAtaque = {
      nombreAtaque,
      descripcion: config.descripcion,
      cantidadMonedas: config.cantidadMonedas,
      danioBase: config.danioBase,
      danioExtraPorCara: config.danioExtraPorCara,
      monedas: Array(config.cantidadMonedas)
        .fill(null)
        .map(() => ({ estado: 'girando' as const })),
      danioTotal: 0,
      terminado: false,
      progreso: 0,
      esSoloEstado: esSoloEstado,
    };
    this.cdr.detectChanges();

    let carasAsignadas = 0;
    for (let i = 0; i < config.cantidadMonedas; i++) {
      // Progreso visual de la barra
      this.coinFlipAtaque!.progreso = ((i + 1) / config.cantidadMonedas) * 100;

      await this.delay(600 + Math.random() * 200);

      const esCara = this.battleBoardTurn.resolverSiguienteMoneda(
        carasForzadas,
        carasAsignadas,
        config.cantidadMonedas,
        i,
      );

      if (esCara) carasAsignadas++;

      // Seteamos el estado visual de la moneda actual
      this.coinFlipAtaque!.monedas[i].estado = esCara ? 'cara' : 'cruz';

      // ?? Actualizamos el flag que usa el HTML para el cartel "LOGRADO/FALLÓ"
      if (config.cantidadMonedas === 1) {
        this.resultadoMoneda = esCara ? 'CARA' : 'CRUZ';
      }

      this.cdr.detectChanges();
      await this.delay(400);
    }

    // Si son varias monedas, definimos el resultado global
    if (config.cantidadMonedas > 1) {
      this.resultadoMoneda = this.battleBoardTurn.obtenerResultadoMoneda(
        config.cantidadMonedas,
        carasAsignadas,
      );
    }

    // ?? CÁLCULO FINAL: Aseguramos que el daño extra mostrado sea el correcto
    this.coinFlipAtaque!.danioTotal = this.battleBoardTurn.calcularDanioMonedas(
      config,
      carasAsignadas,
    );
    this.coinFlipAtaque!.terminado = true;
    this.cdr.detectChanges();

    // Pausa para que el usuario festeje (o llore) el resultado
    await this.delay(2000);

    // Limpieza
    this.coinFlipAtaque = null;
    // (this as any).resultadoMoneda = ""; // Opcional: limpiar para el próximo ataque
    this.cdr.detectChanges();
  }

  intentarAbrirHabilidades(event?: MouseEvent): void {
    if (event) {
      event.stopPropagation();
      event.preventDefault();
    }
    if (this.partida?.turnoActual === 'JUGADOR') {
      this.botEstaAtacando = false;
      this.bloqueadoPorAnimacion = false;
    }
    if (this.cargandoAccion) {
      console.warn('?? Bloqueado por cargandoAccion');
      return;
    }
    this.showHabilidadesPanel = !this.showHabilidadesPanel;
    this.cdr.detectChanges();
  }

  async jugarCarta(carta: any): Promise<void> {
    if (!this.partida || this.partida.turnoActual !== 'JUGADOR' || this.cargandoAccion) return;

    const decision = this.battleBoardAction.resolverAccionCarta(this.partida, carta);

    switch (decision.tipo) {
      case 'unir-energia':
        this.gestionarUnionEnergia(carta);
        return;
      case 'evolucionar':
        if (decision.target) {
          await this.ejecutarEvolucionVisual(carta, decision.target);
        }
        return;
      case 'requiere-promocion':
        if (decision.mensaje) alert(decision.mensaje);
        return;
      case 'bajar-pokemon':
        this.gestionarBajadaPokemon(carta);
        return;
      default:
        return;
    }
  }

  async ejecutarEvolucionVisual(cartaEvolucion: any, target: any) {
    this.bloqueadoPorAnimacion = true;
    this.cargandoAccion = true;

    try {
      // 1. Avisamos al backend
      const estadoFinal = await this.battleBoardAction.evolucionarYRecargar(
        this.matchId!,
        cartaEvolucion.id,
        target.card.id,
      );

      // 2. Disparamos la luz blanca sobre el Pokémon objetivo
      this.animandoEvolucionId = target.card.id;
      this.cdr.detectChanges();

      // Dejamos que el brillo suba (600ms)
      await this.delay(600);

      // 3. Justo en el pico de la luz, pedimos la foto nueva con el Pokémon ya evolucionado
      this.aplicarEstadoRefrescado(estadoFinal);
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
      alert('Error al evolucionar: ' + (error.error || error.message));
    }
  }

  async seleccionarBanca(p: any) {
    if (this.modoSeleccionRetirada) {
      this.cargandoAccion = true;
      try {
        const nuevoEstado = await this.battleBoardAction.retirarPokemonYRecargar(
          this.matchId!,
          p.card.id,
        );
        this.aplicarEstadoRefrescado(nuevoEstado);
        this.modoSeleccionRetirada = false;
        this.cargandoAccion = false;
        this.cdr.detectChanges();
      } catch (err: any) {
        this.modoSeleccionRetirada = false;
        this.cargandoAccion = false;
        alert(err.error || 'No se pudo realizar la retirada.');
      }
    } else if (!this.partida?.jugador?.activo) {
      this.cargandoAccion = true;
      try {
        const nuevoEstado = await this.battleBoardAction.subirAActivoYRecargar(
          this.matchId!,
          p.card.id,
        );
        this.aplicarEstadoRefrescado(nuevoEstado);
        this.cargandoAccion = false;
        this.cdr.detectChanges();
      } catch (err) {
        this.cargandoAccion = false;
        console.error(err);
        alert('No se pudo subir el Pokémon al puesto activo.');
      }
    }
  }

  iniciarModoRetirada() {
    this.showHabilidadesPanel = false;
    this.modoSeleccionRetirada = true;
  }

  async retirarPokemon(suplente: any) {
    const activoJugador = this.partida?.jugador?.activo;
    if (this.cargandoAccion || this.partida?.turnoActual !== 'JUGADOR' || !activoJugador) return;
    if (confirm(this.battleBoardAction.construirMensajeRetirada(activoJugador))) {
      this.cargandoAccion = true;
      try {
        const nuevoEstado = await this.battleBoardAction.retirarPokemonYRecargar(
          this.matchId!,
          suplente.card.id,
        );
        this.aplicarEstadoRefrescado(nuevoEstado);
        this.cargandoAccion = false;
        this.cdr.detectChanges();
      } catch (err: any) {
        this.cargandoAccion = false;
        alert(err.error || 'No tenés suficiente energía para retirarte.');
      }
    }
  }

  soltarCarta(event: CdkDragDrop<any[]>, zona: 'activo' | 'banca'): void {
    if (event.previousContainer.id !== this.handDropListId) return;
    if (event.previousContainer === event.container) return;
    const cartaArrastrada = event.item.data;
    if (this.esEnergia(cartaArrastrada)) {
      if (zona !== 'activo') {
        alert('Las energias se unen al Pokemon activo.');
        return;
      }
      this.gestionarUnionEnergia(cartaArrastrada);
    } else if (this.esPokemon(cartaArrastrada)) {
      this.jugarCarta(cartaArrastrada);
    }
  }

  private gestionarBajadaPokemon(carta: any): void {
    if (this.cargandoAccion) return;
    this.cargandoAccion = true;
    this.battleBoardAction
      .jugarPokemonYRecargar(this.matchId!, carta.id)
      .then((nuevoEstado) => {
        this.aplicarEstadoRefrescado(nuevoEstado);
        this.cargandoAccion = false;
        this.cdr.detectChanges();
      })
      .catch((err) => {
        this.cargandoAccion = false;
        console.error(err);
        alert(err.error || 'No se pudo bajar el Pokémon.');
      });
  }

  private gestionarUnionEnergia(cartaEnergia: any): void {
    const activoJugador = this.partida?.jugador?.activo;
    if (!activoJugador) {
      alert('Necesitás un Pokémon activo!');
      return;
    }
    this.cargandoAccion = true;
    this.battleBoardAction
      .unirEnergiaYRecargar(this.matchId!, activoJugador.card.id, cartaEnergia.id)
      .then((nuevoEstado) => {
        this.aplicarEstadoRefrescado(nuevoEstado);
        this.cargandoAccion = false;
        this.cdr.detectChanges();
      })
      .catch((err: any) => {
        this.cargandoAccion = false;
        console.error(err);
        alert('No se pudo unir la energía.');
      });
  }

  puedePagarRetiro(): boolean {
    return this.battleBoardAction.puedePagarRetiro(this.partida?.jugador?.activo);
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
  // ENERGÍA DRAG
  // -----------------------------------------------

  onEnergyMouseMove = (event: MouseEvent) => {
    this.mousePos = { x: event.clientX, y: event.clientY };
  };

  onEnergyMouseUp = (event: MouseEvent) => {
    this.isDraggingEnergy = false;
    window.removeEventListener('mousemove', this.onEnergyMouseMove);
    window.removeEventListener('mouseup', this.onEnergyMouseUp);
    this.checkDropTarget(event);
  };

  startEnergyDrag(event: MouseEvent, cartaId: string) {
    const card = this.partida?.jugador.mano.find((c: any) => c.id === cartaId);
    if (card?.supertype !== 'Energy') return;
    this.isDraggingEnergy = true;
    this.selectedEnergyId = cartaId;
    this.originPos = { x: event.clientX, y: event.clientY };
    this.mousePos = { x: event.clientX, y: event.clientY };
    window.addEventListener('mousemove', this.onEnergyMouseMove);
    window.addEventListener('mouseup', this.onEnergyMouseUp);
  }

  checkDropTarget(event: MouseEvent) {
    const element = document.elementFromPoint(event.clientX, event.clientY);
    const pokemonElement = element?.closest('.pokemon-card-container');
    if (pokemonElement) {
      const targetId = pokemonElement.getAttribute('data-card-id');
      if (targetId && this.selectedEnergyId) {
        console.log(`Uniendo energía ${this.selectedEnergyId} a Pokémon ${targetId}`);
      }
    }
  }

  // -----------------------------------------------
  // SPRITES Y UTILIDADES
  // -----------------------------------------------

  getSpriteBack(nombreCarta: string): string {
    return this.battleBoardUi.getSpriteBack(nombreCarta);
  }
  getSpriteFront(nombreCarta: string): string {
    return this.battleBoardUi.getSpriteFront(nombreCarta);
  }

  onSpriteError(event: Event): void {
    (event.target as HTMLImageElement).style.display = 'none';
  }

  tieneCondicionEspecial(quien: 'JUGADOR' | 'BOT', condicion: string): boolean {
    const activo = quien === 'JUGADOR' ? this.partida?.jugador?.activo : this.partida?.bot?.activo;
    return activo?.condicionesEspeciales.includes(condicion) ?? false;
  }

  getAtaquesActivoJugador(): BattleBoardAttack[] {
    return (this.partida?.jugador?.activo?.card.ataques as BattleBoardAttack[] | undefined) ?? [];
  }

  getCostoRetiradaActivoJugador(): number {
    return this.partida?.jugador?.activo?.card.costoRetirada ?? 0;
  }

  esHoverSobreManoJugador(): boolean {
    return this.hoveredCardList === (this.partida?.jugador?.mano ?? []);
  }

  getUltimaCartaDescarte(quien: 'JUGADOR' | 'BOT'): Card | null {
    const pila =
      quien === 'JUGADOR' ? this.partida?.jugador?.pilaDescarte : this.partida?.bot?.pilaDescarte;
    return pila && pila.length > 0 ? pila[pila.length - 1] : null;
  }

  getEnergiasActivas(quien: 'JUGADOR' | 'BOT'): Card[] {
    const activo = quien === 'JUGADOR' ? this.partida?.jugador?.activo : this.partida?.bot?.activo;
    return activo?.energiasUnidas ?? [];
  }

  estaPokemonDerrotado(quien: 'JUGADOR' | 'BOT'): boolean {
    const activo = quien === 'JUGADOR' ? this.partida?.jugador?.activo : this.partida?.bot?.activo;
    return (activo?.hpActual ?? 1) <= 0;
  }

  tieneCartasDescarte(quien: 'JUGADOR' | 'BOT'): boolean {
    return this.getCantidadDescarte(quien) > 0;
  }

  getCantidadDescarte(quien: 'JUGADOR' | 'BOT'): number {
    const pila =
      quien === 'JUGADOR' ? this.partida?.jugador?.pilaDescarte : this.partida?.bot?.pilaDescarte;
    return pila?.length ?? 0;
  }

  getTextoEstadoTurno(): string {
    return this.partida?.turnoActual === 'JUGADOR'
      ? `TU TURNO (${this.tiempoRestante}s)`
      : 'TURNO RIVAL';
  }

  getTextoBotonTurno(): string {
    return this.partida?.turnoActual === 'JUGADOR' && !this.cargandoAccion
      ? 'TERMINAR TURNO'
      : 'ESPERANDO...';
  }

  getIconoBotonTurno(): string {
    return this.partida?.turnoActual === 'JUGADOR' && !this.cargandoAccion ? '▶' : '⏳';
  }

  getHpPercent(pokemon: any): number {
    return this.battleBoardUi.getHpPercent(pokemon);
  }
  getHpMax(pokemon: any): number {
    return this.battleBoardUi.getHpMax(pokemon);
  }
  getImagenCarta(id: string): string {
    return this.battleBoardUi.getImagenCarta(id);
  }
  getEmptySlots(n: number): number[] {
    return this.battleBoardUi.getEmptySlots(n);
  }

  esEnergia(carta: any): boolean {
    return this.battleBoardUi.esEnergia(carta);
  }
  esPokemon(carta: any): boolean {
    return this.battleBoardUi.esPokemon(carta);
  }

  get manoAgrupada(): any[][] {
    const tamanoStack = 4;
    const mano = this.partida?.jugador?.mano ?? [];
    const stacks = [];
    for (let i = 0; i < mano.length; i += tamanoStack) stacks.push(mano.slice(i, i + tamanoStack));
    return stacks;
  }

  getEnergyName(tipo: string): string {
    return this.battleBoardUi.getEnergyName(tipo);
  }
  getEnergyColor(tipo: string): string {
    return this.battleBoardUi.getEnergyColor(tipo);
  }

  // -----------------------------------------------
  // VALIDACION DE ENERGIAS
  // -----------------------------------------------

  validarEnergiaAtaque(ataque: any): boolean {
    return this.battleBoardAttack.validarEnergiaAtaque(ataque, this.partida?.jugador?.activo);
  }

  getCheckEnergiasAtaque(ataque: any): any[] {
    return this.battleBoardAttack.getCheckEnergiasAtaque(ataque, this.partida?.jugador?.activo);
  }

  getFaltantesAtaque(ataque: any): any[] {
    return this.battleBoardAttack.getFaltantesAtaque(ataque, this.partida?.jugador?.activo);
  }

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
        duracion: 0.4 + Math.random() * 0.5,
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

  private aplicarEstadoRefrescado(estado: Partida | null | undefined): void {
    if (!estado) return;
    this.partida = this.battleBoardState.clonarPartida(estado);
    this.hpRenderJugador = estado.jugador?.activo?.hpActual || 0;
  }

  private async reproducirChequeoDespertar(
    descripcion: string,
    seDesperto: boolean,
  ): Promise<void> {
    this.coinFlipAtaque = this.battleBoardTurn.crearEstadoCoinFlip(descripcion);
    this.cdr.detectChanges();

    await this.delay(1500);

    if (this.coinFlipAtaque?.monedas[0]) {
      this.coinFlipAtaque.monedas[0].estado = seDesperto ? 'cara' : 'cruz';
      this.coinFlipAtaque.terminado = true;
    }

    this.resultadoMoneda = seDesperto ? 'CARA' : 'CRUZ';
    this.cdr.detectChanges();

    await this.delay(2000);

    this.coinFlipAtaque = null;
    this.cdr.detectChanges();
  }

  private async mostrarOverlayTurnoBot(): Promise<void> {
    this.turnoOverlayTipo = 'bot';
    this.showTurnOverlay = true;
    this.cdr.detectChanges();

    await this.delay(2000);
    this.showTurnOverlay = false;
    this.cdr.detectChanges();
    await this.delay(400);
  }

  private async ejecutarTurnoBotConPausa(): Promise<Partida> {
    this.botPensando = true;
    this.cdr.detectChanges();

    const tiempoPensamiento = this.battleBoardTurn.calcularTiempoPensamientoBot();
    await this.delay(tiempoPensamiento);

    const estadoPostBot = await this.battleBoardCombat.ejecutarTurnoBot(this.matchId!);
    this.botPensando = false;
    this.cdr.detectChanges();
    return estadoPostBot;
  }

  private async resolverConfusionPreAtaque(activoJugador: CartaEnJuego): Promise<boolean> {
    if (!activoJugador.condicionesEspeciales.includes('Confused')) {
      return false;
    }

    const configConfusion = this.battleBoardCombat.crearConfigConfusion();
    const exitoConfusion = this.battleBoardCombat.resolverConfusion();

    await this.animarMonedasSincronizadas(
      'Check de Confusión',
      configConfusion,
      exitoConfusion,
      true,
    );

    if (this.resultadoMoneda !== 'CRUZ') {
      return false;
    }

    console.log('💫 Confusión! Te pegaste a vos mismo.');
    const estadoFallo = await this.battleBoardCombat.pasarTurnoYRecargar(this.matchId!);

    this.animandoJugadorDanio = true;
    this.aplicarEstadoRefrescado(estadoFallo);
    this.cdr.detectChanges();

    await this.delay(600);
    this.animandoJugadorDanio = false;

    await this.iniciarTurnoBot(estadoFallo);
    return true;
  }

  private async reproducirCoinFlipAtaqueJugador(
    habilidad: any,
    estadoFinal: Partida,
  ): Promise<void> {
    const coinConfig = this.detectarCoinFlipAtaque(habilidad);
    if (!coinConfig) return;

    const hpBotAntes = this.partida?.bot?.activo?.hpActual || 0;
    const hpBotDespues = estadoFinal.bot?.activo?.hpActual || 0;
    const danioHecho = this.battleBoardCombat.calcularDanioHecho(hpBotAntes, hpBotDespues);

    const carasReales = this.battleBoardTurn.resolverCarasJugador(
      coinConfig,
      habilidad,
      estadoFinal,
      danioHecho,
    );
    this.resultadoMoneda = this.battleBoardTurn.obtenerResultadoMoneda(
      coinConfig.cantidadMonedas,
      carasReales,
    );

    await this.animarMonedasSincronizadas(
      habilidad.nombre,
      coinConfig,
      carasReales,
      coinConfig.esSoloEstado,
    );
  }

  private async reproducirImpactoAtaqueJugador(
    tipoEnergia: string,
    estadoFinal: Partida,
  ): Promise<void> {
    this.animandoAtaque = true;
    this.cdr.detectChanges();

    await this.delay(400);
    this.dispararParticulas('bot', tipoEnergia);
    this.showImpactFlash = true;

    this.aplicarEstadoRefrescado(estadoFinal);
    this.cdr.detectChanges();

    await this.delay(200);
    this.showImpactFlash = false;
    this.cdr.detectChanges();

    await this.delay(400);
    this.animandoAtaque = false;
    this.cdr.detectChanges();
  }

  private async finalizarSecuenciaAtaque(estadoFinal: Partida): Promise<void> {
    if (estadoFinal.turnoActual === 'BOT') {
      await this.iniciarTurnoBot(estadoFinal);
      return;
    }

    this.cargandoAccion = false;
    this.bloqueadoPorAnimacion = false;
    this.ataqueRealizado = false;
    this.cdr.detectChanges();
  }

  private async procesarPostPassTurn(
    estadoAntiguo: Partida | null,
    estadoFinal: Partida,
  ): Promise<void> {
    const wakeCheckJugador = this.battleBoardTurn.evaluarDespertar(
      estadoAntiguo?.jugador?.activo,
      estadoFinal?.jugador?.activo,
    );

    if (wakeCheckJugador) {
      console.log('😴 Te detecté durmiendo! Activando moneda...');
      await this.reproducirChequeoDespertar(
        '¿Se despierta tu Pokémon?',
        wakeCheckJugador.seDesperto,
      );
      console.log(
        wakeCheckJugador.seDesperto ? '🍀 Suerte! El Pokémon se despertó.' : '?? Sigue dormido.',
      );
    }

    await this.verificarEstadosCurados(estadoAntiguo, estadoFinal);
    this.aplicarEstadoRefrescado(estadoFinal);
    this.cdr.detectChanges();

    if (estadoFinal.turnoActual !== 'BOT') {
      console.log('?? Turno devuelto al jugador inmediatamente.');
      this.cargandoAccion = false;
      this.bloqueadoPorAnimacion = false;
      this.iniciarRelojTurno();
      this.cdr.detectChanges();
      return;
    }

    await this.mostrarOverlayTurnoBot();

    try {
      console.log('?? Disparando IA en el backend...');
      const estadoPostBot = await this.ejecutarTurnoBotConPausa();
      console.log('? IA finalizada. Animando ataques del bot...');
      this.ejecutarIAEnemigaConData(estadoPostBot);
    } catch (err: any) {
      console.error('? Error crítico en la IA del Bot:', err);
      this.botPensando = false;
      this.bloqueadoPorAnimacion = false;
      this.cargandoAccion = false;
      this.cdr.detectChanges();
    }
  }

  // -----------------------------------------------
  // NAVEGACION
  // -----------------------------------------------

  volverAlLobby(): void {
    this.router.navigate(['/lobby']);
  }

  private delay(ms: number): Promise<void> {
    return new Promise((resolve) => setTimeout(resolve, ms));
  }
}
