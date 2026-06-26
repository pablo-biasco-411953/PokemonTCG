import { ChangeDetectorRef, Component, HostListener, OnDestroy, OnInit, ViewChild, ElementRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { BattleNotificationComponent } from './components/battle-notification/battle-notification';
import { BattleBoardAbilitiesPanelComponent } from './battle-board-abilities-panel.component';
import { BattleBoardCardDetailPanelComponent } from './battle-board-card-detail-panel.component';
import { BattleBoardDebugPanelComponent } from './battle-board-debug-panel.component';
import { BattleBoardDiscardModalComponent } from './battle-board-discard-modal.component';
import { TranslatePipe } from '../../i18n/translate.pipe';
import { I18nService } from '../../i18n/i18n.service';
import { BattleService } from './services/battle.service';
import { BattleBoardUiService } from './services/battle-board-ui.service';
import {
  BattleBoardAttackService,
  CoinFlipConfig,
} from './services/battle-board-attack.service';
import { Router, ActivatedRoute } from '@angular/router';
import { DragDropModule, CdkDragDrop } from '@angular/cdk/drag-drop';
import { firstValueFrom, Observable } from 'rxjs';
import { SafeHtml } from '@angular/platform-browser';
import { Card } from '../../shared/models/card';
import { BattleActionCard, CartaEnJuego, Partida } from '../../shared/models/battle';
import { BattleBoardActionService } from './services/battle-board-action.service';
import { BattleBoardCombatService } from './services/battle-board-combat.service';
import { BattleBoardStateService } from './services/battle-board-state.service';
import { BattleBoardTurnService } from './services/battle-board-turn.service';
import { BattleNotificationService } from './services/battle-notification';
import { ImagePreloaderService } from '../../core/services/image-preloader.service';
import { MusicPlayerService } from '../../core/services/music-player.service';
import { JugadorService } from '../../core/services/jugador.service';
import { CardService } from '../../core/services/card.service';
import { LobbyRoomReaction, LobbyRoomService, LobbyRoomSnapshot } from '../lobby/services/lobby-room.service';
import * as THREE from 'three';
import { GLTFLoader } from 'three/examples/jsm/loaders/GLTFLoader.js';
import { OrbitControls } from 'three/examples/jsm/controls/OrbitControls.js';
import { KTX2Loader } from 'three/examples/jsm/loaders/KTX2Loader.js';
import { MeshoptDecoder } from 'three/examples/jsm/libs/meshopt_decoder.module.js';
import { clone as cloneSkeleton } from 'three/examples/jsm/utils/SkeletonUtils.js';
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

const CHARACTER_OPTIONS = [
  { id: 'hilda-sygna', label: 'Hilda Sygna', path: '/models-optimized/characters/hilda_sygna_10.glb', scale: 1.0, yOffset: 0, idleHints: ['idle'], walkHints: ['walk_1', 'walk'] },
  { id: 'lillie', label: 'Lillie', path: '/models-optimized/characters/lillie__anniversary_50.glb', scale: 1.0, yOffset: 0, idleHints: ['idle'], walkHints: ['walk_1', 'walk'] },
  { id: 'ash', label: 'Ash', path: '/models-optimized/characters/ash_ketchup_-_pokemon.glb', scale: 0.82, yOffset: 0, idleHints: ['house', 'talking', 'walking'], walkHints: ['walking'] },
  { id: 'robot', label: 'Robot CC0', path: '/models-optimized/player/RobotExpressive.glb', scale: 0.42, yOffset: 0, idleHints: ['idle', 'standing'], walkHints: ['walking', 'walk'] },
  // Nuevos personajes (Pibes)
  { id: 'adaman', label: 'Adaman', path: '/models-optimized/characters/adaman_regular_00.glb', scale: 1.0, yOffset: 0, idleHints: ['idle'], walkHints: ['walk_1', 'walk'] },
  { id: 'giovanni-sygna', label: 'Giovanni Sygna', path: '/models-optimized/characters/giovanni_sygna_10.glb', scale: 1.0, yOffset: 0, idleHints: ['idle'], walkHints: ['walk_1', 'walk'] },
  { id: 'hugh', label: 'Hugh', path: '/models-optimized/characters/hugh_regular_00.glb', scale: 1.0, yOffset: 0, idleHints: ['idle'], walkHints: ['walk_1', 'walk'] },
  // Nuevos personajes (Pibas)
  { id: 'courtney', label: 'Courtney', path: '/models-optimized/characters/courtney_regular_00.glb', scale: 1.0, yOffset: 0, idleHints: ['idle'], walkHints: ['walk_1', 'walk'] },
  { id: 'irida', label: 'Irida', path: '/models-optimized/characters/irida_regular_00.glb', scale: 1.0, yOffset: 0, idleHints: ['idle'], walkHints: ['walk_1', 'walk'] },
  { id: 'zinnia', label: 'Zinnia', path: '/models-optimized/characters/zinnia_regular_00.glb', scale: 1.0, yOffset: 0, idleHints: ['idle'], walkHints: ['walk_1', 'walk'] }
];

const HANDSHAKE_GLTF_CACHE = new Map<string, { scene: THREE.Object3D, animations: THREE.AnimationClip[] }>();


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
    TranslatePipe,
    BattleNotificationComponent,
  ],
})
export class BattleBoardComponent implements OnInit, OnDestroy {
  battleSongAnnouncementMinimized = false;
  get currentSong$(): Observable<any> { return this.musicPlayer.currentSong$; }
  get isPlaying$(): Observable<boolean> { return this.musicPlayer.isPlaying$; }
  toggleSongAnnouncement() {
    this.battleSongAnnouncementMinimized = !this.battleSongAnnouncementMinimized;
  }

  mostrarNotificacion(mensaje: string, tipo: 'info' | 'success' | 'warning' | 'error' = 'info') {
    if (tipo === 'error') {
      this.battleNotificationService.showModal('Error', mensaje, tipo);
    } else {
      this.battleNotificationService.show(mensaje, tipo);
    }
  }
  public Math = Math;
  readonly handDropListId = 'player-hand-dropzone';
  readonly activeDropListId = 'player-active-dropzone';
  readonly benchDropListId = 'player-bench-dropzone';
  readonly playableDropListIds = [this.handDropListId, this.activeDropListId, this.benchDropListId];
  matchId: string | null = null;
  private _partida: Partida | null = null;
  get partida(): Partida | null {
    return this._partida;
  }
  set partida(value: Partida | null) {
    this._partida = value ? this.cardService.translatePartida(value) : null;
  }
  jugadorNombre = '';
  lastProcessedCoinFlipEventId = 0;
  isSpectator = false;
  battleRoom: LobbyRoomSnapshot | null = null;
  battleReactions: Array<{ id: string; icon: string; sender: string; x: number }> = [];
  battleLogMinimized = false;
  private seenBattleReactionIds = new Set<string>();
  private battleRoomPolling: ReturnType<typeof setInterval> | null = null;
  landscapeHintDismissed = localStorage.getItem('battleLandscapeHintDismissed') === 'true';

  private yStart = 0;
  private yEnd = 0;
  private coinPointerId: number | null = null;
  public lanzada = false;
  hoveredCard: Card | BattleActionCard | null = null;
  hoveredInPlayCard: CartaEnJuego | null = null;
  hoveredCardStatuses: CardGlossaryEntry[] = [];
  hoveredCardList: HoveredBattleCard[] = [];
  hoveredCardIndex = -1;

  selectedDetailPokemon: CartaEnJuego | null = null;
  mostrarDetalleModal = false;

  // Sudden Death (Muerte Súbita)
  mostrarOverlayMuerteSubita = false;
  atmosphereHemisphereLight!: THREE.HemisphereLight;
  atmosphereRivalLight!: THREE.PointLight;
  atmospherePlayerLight!: THREE.PointLight;
  atmosphereFloor!: THREE.Mesh;
  atmosphereGrid!: THREE.GridHelper;
  private lightningFlashActive = false;
  private lightningFlashTimer = 0;

  public cartasNuevas = new Set<string>();
  private manoAnteriorIds = new Set<string>();

  public cartasNuevasBot = new Set<string>();
  private manoAnteriorIdsBot = new Set<string>();
  healedTextPlayer: string | null = null;
  curingParalysisPlayer = false;
  curingSleepPlayer = false;

  healedTextBot: string | null = null;
  curingParalysisBot = false;
  curingSleepBot = false;
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
  turnTimerEnabled = false;
  botPensando = false;
  esperandoMiNuevoTurno = false;
  turnoOverlayTipo: 'jugador' | 'bot' = 'jugador';
  public modoSeleccionRetirada = false;

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
  mostrarVictoriaModal = false;
  mostrarDerrotaModal = false;
  animandoTransicionFinJuego = false;
  
  private lastProcessedLogIndex = 0;

  private pollingSorteo: ReturnType<typeof setInterval> | null = null;
  private battlePresenceInterval: ReturnType<typeof setInterval> | null = null;
  private pendingEndGameTimeout: ReturnType<typeof setTimeout> | null = null;
  private battleMusicTimeout: ReturnType<typeof setTimeout> | null = null;
  private introAudioPlayingListener: (() => void) | null = null;
  private introAudioTimeUpdateListener: (() => void) | null = null;
  private introAudioFallbackTimeout: any = null;
  private introFadeOutTimeout: any = null;
  private introHideTimeout: any = null;
  private introRevealTimeout: any = null;
  private versusPreloadFinished = false;
  private versusSequenceTriggered = false;
  showEndGameOverlay = false;
  isVictory = false;
  coinsEarned = 0;
  endGameWinner = '';
  endGameReason = '';
  private endGameRewardSent = false;
  private lastAppliedStateSignature = '';
  public botEstaAtacando = false;
  private datosPendientesBot: Partida | null = null;
  animandoEvolucionId: string | null = null;
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
  mostrarConversionModal = false;

  // Animaciones de moneda acumulativas y temblor de pantalla
  public carasAcumuladas = 0;
  public danioAcumulado = 0;
  public screenShaking = false;

  puedeEvolucionar(cartaMano: BattleActionCard | Card): boolean {
    return this.battleBoardState.puedeEvolucionar(this.partida, cartaMano);
  }

  public estadoCoinFlip:
    | 'CARGANDO_VS'
    | 'DAR_LA_MANO'
    | 'ELEGIR_LADO'
    | 'ESPERANDO_TIRO'
    | 'GIRANDO'
    | 'ELEGIR_TURNO'
    | 'RESULTADO_BOT'
    | 'ESPERANDO_RIVAL'
    | 'OCULTO' = 'OCULTO';
  eleccionJugador: 'CARA' | 'CRUZ' = 'CARA';
  resultadoMoneda: CoinSide = 'CARA';
  public girando = false;

  jugadorLoadingPercentage = 0;
  botLoadingPercentage = 0;
  loadingPhaseFinished = false;
  isAdmin = false;

  get battleLoadingCombined(): number {
    return (this.jugadorLoadingPercentage + this.botLoadingPercentage) / 2;
  }
  private preloadPollingId: any = null;
  private preloadTimeoutId: any = null;

  mostrarAuraCuracionBot = false;
  mostrarAuraCuracionPlayer = false;
  mostrarKO = false;

  damageNumberBot: DamageNumberState | null = null;
  damageNumberPlayer: DamageNumberState | null = null;

  mostrarEfectoBot = false;
  mostrarEfectoJugador = false;
  particulasBot: ParticleVisualState[] = [];
  particulasJugador: ParticleVisualState[] = [];
  animandoBotDanio = false;
  animandoJugadorDanio = false;
  impactCalloutBot: 'effective' | 'resisted' | null = null;
  impactCalloutPlayer: 'effective' | 'resisted' | null = null;
  battlefieldDamagePulse = false;
  prizeRevealCard: Card | null = null;
  pendingEffectSelection = new Set<string>();

  handshakePower = 0;
  opponentHandshakePower = 0;
  opponentHandshakeHolding = false;
  handshakeHits = 0;
  handshakeComplete = false;
  handshakeHolding = false;
  private handshakeInterval: ReturnType<typeof setInterval> | null = null;
  private handshakePolling: ReturnType<typeof setInterval> | null = null;
  private lastHandshakeSyncAt = 0;

  localPlayerCharacterId = localStorage.getItem('lobbyCharacter') || 'hilda-sygna';
  opponentCharacterId = 'robot';
  opponentSkinColor = '#ffe0bd';
  opponentHairColor = '#5c4033';
  opponentEyeColor = '#2563eb';
  opponentHeight = 1.0;
  private opponentLoaded = false;
  isPotato = false;

  private handshakeRenderer?: THREE.WebGLRenderer;
  private handshakeScene?: THREE.Scene;
  private handshakeCamera?: THREE.PerspectiveCamera;
  private handshakeClock = new THREE.Clock();
  private handshakeAnimationId?: number;
  private playerMixer?: THREE.AnimationMixer;
  private opponentMixer?: THREE.AnimationMixer;
  private playerModel?: THREE.Object3D;
  private opponentModel?: THREE.Object3D;
  private playerRightArm?: THREE.Bone;
  private playerRightForeArm?: THREE.Bone;
  private opponentRightArm?: THREE.Bone;
  private opponentRightForeArm?: THREE.Bone;
  private defaultQuaternions = new Map<THREE.Bone, THREE.Quaternion>();
  private playerActions = new Map<string, THREE.AnimationAction>();
  private opponentActions = new Map<string, THREE.AnimationAction>();
  private currentAnims = new Map<THREE.AnimationMixer, { state: string, action: THREE.AnimationAction }>();

  // Versus Scene Cinematic Properties
  preloadCinematicStartedAt = 0;
  cinematicSceneLoadingPercentage = 0;
  cinematicAssetsReady = false;
  cinematicLoadingBarsVisible = false;

  private versusStartedAt = 0;
  private versusSequenceStartedAt = 0;
  private versusModelsLoaded = 0;
  private versusLastShotIndex = 0;
  private versusCameraTilt = 0;
  private versusPlayerClips = new Map<string, THREE.AnimationAction>();
  private versusOpponentClips = new Map<string, THREE.AnimationAction>();
  private versusPlayerCurrentClip = '';
  private versusOpponentCurrentClip = '';
  private versusPlayerModel: THREE.Object3D | undefined;
  private versusOpponentModel: THREE.Object3D | undefined;
  private versusPlayerMixer: THREE.AnimationMixer | undefined;
  private versusOpponentMixer: THREE.AnimationMixer | undefined;
  private versusAnimationId: number | undefined;
  versusCanvasInitialized = false;
  private versusScene: THREE.Scene | undefined;
  private versusCamera: THREE.PerspectiveCamera | undefined;
  private versusRenderer: THREE.WebGLRenderer | undefined;
  private versusProgressLastRender = 0;
  private serverLoadingComplete = false;
  private lastPreloadState: Partida | null = null;
  private finalizarPreloadFn: ((state?: Partida | null) => void) | null = null;

  // 3D Coin Flip Properties
  coinFlipCanvasInitialized = false;
  private coinFlipScene?: THREE.Scene;
  private coinFlipCamera?: THREE.PerspectiveCamera;
  private coinFlipRenderer?: THREE.WebGLRenderer;
  private coinFlipCoinModel?: THREE.Group;
  private coinFlipAnimationId?: number;
  private coinFlipControls?: OrbitControls;
  private coinFlipClock = new THREE.Clock();
  private coinFlipParticles: { mesh: THREE.Mesh; vx: number; vy: number; vz: number; life: number; maxLife: number; }[] = [];
  fuerzaActual = 0;
  arrastrando = false;
  private xStart = 0;
  private coinFlipVuelo = false;
  private coinFlipReleaseTime = 0;
  private coinVy = 0;
  private coinVx = 0;
  private coinVz = 0;
  private coinOmegaX = 0;
  private coinOmegaY = 0;
  private coinOmegaZ = 0;
  private coinRebotes = 0;
  private coinFlipResultadoListo = false;
  private coinFlipResultadoEsperado?: 'CARA' | 'CRUZ';
  private backendResultPromise?: Promise<Partida>;
  private loadingCoinModels = false;

  // Cinematic character state & selection variables
  eleccionTemporal: 'CARA' | 'CRUZ' | null = null;
  confirmadoLado = false;
  private playerCoinFlipModel?: THREE.Object3D;
  private opponentCoinFlipModel?: THREE.Object3D;
  private playerCoinFlipMixer?: THREE.AnimationMixer;
  private opponentCoinFlipMixer?: THREE.AnimationMixer;
  private playerCoinFlipActions = new Map<string, THREE.AnimationAction>();
  private opponentCoinFlipActions = new Map<string, THREE.AnimationAction>();
  private coinFlipPlayerRightArm?: any;
  private coinFlipPlayerRightForeArm?: any;
  private coinFlipPlayerRightHand?: any;
  private coinFlipDefaultQuaternions = new Map<any, any>();

  // Standalone Board Trainers 3D Scene Properties
  playerTrainerCanvasInitialized = false;
  opponentTrainerCanvasInitialized = false;
  private playerTrainerRenderer?: THREE.WebGLRenderer;
  private opponentTrainerRenderer?: THREE.WebGLRenderer;
  private playerTrainerScene?: THREE.Scene;
  private opponentTrainerScene?: THREE.Scene;
  private playerTrainerCamera?: THREE.PerspectiveCamera;
  private opponentTrainerCamera?: THREE.PerspectiveCamera;
  private playerTrainerModel?: THREE.Object3D;
  private opponentTrainerModel?: THREE.Object3D;
  private playerTrainerMixer?: THREE.AnimationMixer;
  private opponentTrainerMixer?: THREE.AnimationMixer;
  private playerTrainerActions = new Map<string, THREE.AnimationAction>();
  private opponentTrainerActions = new Map<string, THREE.AnimationAction>();
  private currentTrainerAnims = new Map<THREE.AnimationMixer, { state: string, action: THREE.AnimationAction }>();
  private trainersClock = new THREE.Clock();
  private battleAtmosphereRenderer?: THREE.WebGLRenderer;
  private battleAtmosphereScene?: THREE.Scene;
  private battleAtmosphereCamera?: THREE.PerspectiveCamera;
  private battleAtmosphereFrame = 0;
  private battleAtmosphereResize?: ResizeObserver;
  private battleAtmosphereClock = new THREE.Clock();
  private battleAtmosphereRings: THREE.Mesh[] = [];
  private battleAtmosphereParticles?: THREE.Points;
  private battleAtmosphereShards?: THREE.InstancedMesh;
  private battleAtmosphereBeams: THREE.Mesh[] = [];
  private trainersAnimationId?: number;
  private handshakeParticleGeometry?: THREE.SphereGeometry;
  private handshakeSpotlight?: THREE.SpotLight;
  private cameraShakeTime = 0;
  private handshakeParticles: { mesh: THREE.Mesh; velocity: THREE.Vector3; color: THREE.Color; life: number; maxLife: number; }[] = [];
  private canvasInitialized = false;
  private effectsTriggered = false;

  private playerShieldGroup?: THREE.Group;
  private botShieldGroup?: THREE.Group;

  private nextFloatingTextId = 0;
  switchFxBot = false;
  switchFxPlayer = false;
  public mostrarExplosionEnergiaPlayer = false;
  public mostrarExplosionEnergiaBot = false;
  public particulasEnergiaPlayer: Array<{ color: string; size: number; duracion: number; tx: number; ty: number }> = [];
  public particulasEnergiaBot: Array<{ color: string; size: number; duracion: number; tx: number; ty: number }> = [];

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private battleService: BattleService,
    public battleBoardUi: BattleBoardUiService,
    private cdr: ChangeDetectorRef,
    private i18nService: I18nService,
    private battleBoardAttack: BattleBoardAttackService,
    private battleBoardAction: BattleBoardActionService,
    private battleBoardCombat: BattleBoardCombatService,
    private battleBoardState: BattleBoardStateService,
    private battleBoardTurn: BattleBoardTurnService,
    private jugadorService: JugadorService,
    private imagePreloader: ImagePreloaderService,
    private battleNotificationService: BattleNotificationService,
    private lobbyRoomService: LobbyRoomService,
    public i18n: I18nService,
    private musicPlayer: MusicPlayerService,
    private cardService: CardService,
  ) {}

  ngOnInit(): void {
    try {
      const jugadorData = localStorage.getItem('jugador');
      if (jugadorData) {
        this.isAdmin = JSON.parse(jugadorData).admin || false;
      }
    } catch (e) {
      this.isAdmin = false;
    }
    this.isPotato = (navigator as any).hardwareConcurrency <= 4 || /Mobi|Android|iPhone/i.test(navigator.userAgent);
    this.requestLandscapeOrientation();
    this.matchId = this.route.snapshot.paramMap.get('id');
    if (!this.matchId) return;
    this.isSpectator = this.route.snapshot.queryParamMap.get('spectate') === '1';
    this.jugadorNombre = this.obtenerNombreJugadorLocal();
    if (!this.isSpectator) {
      this.iniciarPresenciaBatalla();
    }
    this.cargarCatalogoGodMode();
    this.cardService.getAll().subscribe({
      next: () => {
        if (this.partida) {
          this.partida = { ...this.partida };
        }
      }
    });
    this.battleMusicTimeout = setTimeout(() => {
      this.musicPlayer.playSong('battle');
    }, 0);

    let triggered = false;
    const triggerReveal = () => {
      if (triggered) return;
      triggered = true;

      if (this.musicPlayer?.audioElement) {
        if (this.introAudioPlayingListener) {
          this.musicPlayer.audioElement.removeEventListener('playing', this.introAudioPlayingListener);
        }
        if (this.introAudioTimeUpdateListener) {
          this.musicPlayer.audioElement.removeEventListener('timeupdate', this.introAudioTimeUpdateListener);
        }
      }
      if (this.introAudioFallbackTimeout) clearTimeout(this.introAudioFallbackTimeout);

      this.introFadeOutTimeout = setTimeout(() => {
        this.introFadingOut = true;
        this.cdr.detectChanges();
      }, 2200);
      this.introHideTimeout = setTimeout(() => {
        this.showIntro = false;
        this.cdr.detectChanges();
      }, 3000);
      this.introRevealTimeout = setTimeout(() => {
        this.versusSequenceTriggered = true;
        this.tryStartVersusSequence();
      }, 3550);
    };

    this.introAudioPlayingListener = () => {
      triggerReveal();
    };

    this.introAudioTimeUpdateListener = () => {
      if (this.musicPlayer?.audioElement && this.musicPlayer.audioElement.currentTime > 0) {
        triggerReveal();
      }
    };

    if (this.musicPlayer?.audioElement) {
      this.musicPlayer.audioElement.addEventListener('playing', this.introAudioPlayingListener);
      this.musicPlayer.audioElement.addEventListener('timeupdate', this.introAudioTimeUpdateListener);
    }

    this.introAudioFallbackTimeout = setTimeout(() => {
      triggerReveal();
    }, 3000);

    this.battleService.getState(this.matchId).subscribe({
      next: (data) => this.hidratarEstadoInicial(data),
      error: (err) => {
        console.error('No se pudo recuperar la partida al recargar', err);
        this.mostrarNotificacion(this.i18n.translate('alert.battleNotAvailable'), 'error');
        this.router.navigate(['/lobby']);
      }
    });
  }

  ngOnDestroy(): void {
    if (this.pollingPartida) clearInterval(this.pollingPartida);
    if (this.pollingSorteo) clearInterval(this.pollingSorteo);
    if (this.battlePresenceInterval) clearInterval(this.battlePresenceInterval);
    if (this.battleRoomPolling) clearInterval(this.battleRoomPolling);
    if (this.pendingEndGameTimeout) clearTimeout(this.pendingEndGameTimeout);
    if (this.battleMusicTimeout) clearTimeout(this.battleMusicTimeout);
    if (this.musicPlayer?.audioElement) {
      if (this.introAudioPlayingListener) {
        this.musicPlayer.audioElement.removeEventListener('playing', this.introAudioPlayingListener);
      }
      if (this.introAudioTimeUpdateListener) {
        this.musicPlayer.audioElement.removeEventListener('timeupdate', this.introAudioTimeUpdateListener);
      }
    }
    if (this.introAudioFallbackTimeout) clearTimeout(this.introAudioFallbackTimeout);
    if (this.introFadeOutTimeout) clearTimeout(this.introFadeOutTimeout);
    if (this.introHideTimeout) clearTimeout(this.introHideTimeout);
    if (this.introRevealTimeout) clearTimeout(this.introRevealTimeout);
    this.cleanupBattleAtmosphere();
    this.detenerPollingHandshake();
    if (this.handshakeInterval) clearInterval(this.handshakeInterval);
    this.cleanupHandshakeScene();
  }

  private requestLandscapeOrientation(): void {}

  private iniciarPresenciaBatalla(): void {
    if (!this.matchId) return;
    if (this.battlePresenceInterval) clearInterval(this.battlePresenceInterval);

    this.battleService.heartbeat(this.matchId).subscribe({
      next: (data) => {
        if (data?.faseActual === 'FIN_PARTIDA') {
          this.aplicarEstadoRefrescado(data);
        }
      },
      error: (err) => console.warn('No se pudo enviar heartbeat inicial de batalla', err)
    });

    this.battlePresenceInterval = setInterval(() => {
      if (!this.matchId || this.showEndGameOverlay) return;
      this.battleService.heartbeat(this.matchId).subscribe({
        next: (data) => {
          if (data?.faseActual === 'FIN_PARTIDA') {
            this.aplicarEstadoRefrescado(data);
          }
        },
        error: (err) => console.warn('No se pudo enviar heartbeat de batalla', err)
      });
    }, 10000);
  }

  private hidratarEstadoInicial(data: Partida): void {
    if (!data) return;
    this.partida = data;
    this.lastProcessedCoinFlipEventId = data.lastCoinFlipEventId || 0;
    if (this.esPartidaOnline(data) && !this.battleRoomPolling) {
      this.iniciarPollingSalaBatalla();
    }
    this.lastAppliedStateSignature = this.crearFirmaPartida(data);
    this.manoAnteriorIds = new Set((data.jugador?.mano || []).map((c: any) => c.id));
    this.manoAnteriorIdsBot = new Set((data.bot?.mano || []).map((c: any) => c.id));
    if (!this.opponentLoaded && this.nombreRival) {
      this.cargarDatosOponente();
    }

    if (data.faseActual === 'FIN_PARTIDA') {
      this.handleGameEnd(data);
      return;
    }

    if (data.faseActual === 'LANZAMIENTO_MONEDA') {
      this.boardVisible = false;
      if (!this.loadingPhaseFinished) {
        this.startPreloadSequence(data);
      } else {
        this.hidratarPreparativosIniciales(data);
      }
      this.cdr.detectChanges();
      return;
    }

    this.finalizarCoinFlip();
  }

  private reingresarAFaseLanzamientoMoneda(data: Partida): void {
    if (this.pollingPartida) {
      clearInterval(this.pollingPartida);
      this.pollingPartida = null;
    }

    this.detenerRelojTurno();
    this.detenerPollingHandshake();
    this.showEndGameOverlay = false;
    this.cargandoAccion = false;
    this.bloqueadoPorAnimacion = false;
    this.botPensando = false;
    this.botEstaAtacando = false;
    this.esperandoMiNuevoTurno = false;
    this.initialBotTurnInFlight = false;
    this.estadoSetupMulligan = null;
    this.setupAccionEnCurso = false;
    this.setupAutoActionKey = '';
    this.setupMulliganRevealKey = '';
    this.setupShuffleVisible = false;
    this.setupMulliganJugadorCartas = [];
    this.setupMulliganRivalCartas = [];
    this.setupMulliganJugadorDebeMostrar = false;
    this.setupMulliganRivalDebeMostrar = false;
    this.cartasExtraPermitidas = 0;
    this.mulliganJugadorCount = 0;
    this.mulliganOponenteCount = 0;
    this.lanzada = false;
    this.girando = false;
    this.boardVisible = false;
    this.estadoCoinFlip = this.loadingPhaseFinished ? 'DAR_LA_MANO' : 'CARGANDO_VS';
    this.coinFlipAtaque = null;
    this.resultadoMoneda = 'CARA';
    this.anguloFinal = 0;

    this.partida = data;
    this.lastAppliedStateSignature = this.crearFirmaPartida(data);

    if (this.loadingPhaseFinished) {
      this.hidratarPreparativosIniciales(data);
    } else {
      this.startPreloadSequence(data);
    }

    if (!this.isSpectator) {
      this.procesarFasesSetup(data);
    }

    this.procesarTurnLogs();
    this.cdr.detectChanges();
  }

  private hidratarPreparativosIniciales(data: Partida): void {
    this.handshakePower = data.coinHandshakeJugadorPower ?? 0;
    this.opponentHandshakePower = data.coinHandshakeBotPower ?? 0;
    this.opponentHandshakeHolding = !!data.coinHandshakeBotHolding;
    this.handshakeComplete = !!data.coinHandshakeComplete;

    if (data.coinFlipped) {
      this.detenerPollingHandshake();
      if (this.isSpectator) {
        this.estadoCoinFlip = 'ESPERANDO_RIVAL';
        this.iniciarPollingSorteo();
        return;
      }
      this.estadoCoinFlip = data.coinFlipWinner === this.jugadorNombre ? 'ELEGIR_TURNO' : 'RESULTADO_BOT';
      if (this.estadoCoinFlip === 'RESULTADO_BOT') {
        this.iniciarPollingSorteo();
      }
      return;
    }

    if (data.coinHandshakeComplete) {
      if (this.isSpectator) {
        this.estadoCoinFlip = 'ESPERANDO_RIVAL';
        this.iniciarPollingSorteo();
        return;
      }
      this.estadoCoinFlip = this.puedeCantarSorteo(data) ? 'ELEGIR_LADO' : 'ESPERANDO_RIVAL';
      if (this.estadoCoinFlip === 'ESPERANDO_RIVAL') {
        this.iniciarPollingSorteo();
      }
      return;
    }

    if (this.isSpectator) {
      this.estadoCoinFlip = 'ESPERANDO_RIVAL';
      this.iniciarPollingSorteo();
      return;
    }

    this.estadoCoinFlip = 'DAR_LA_MANO';
    this.iniciarPollingHandshake();
    setTimeout(() => {
      if (this.estadoCoinFlip === 'DAR_LA_MANO') {
        this.syncHandshakeWithServer(true);
      }
    }, 120);
  }

  private startPreloadSequence(data: Partida): void {
    if (this.loadingPhaseFinished) {
      this.hidratarPreparativosIniciales(data);
      return;
    }
    if (this.estadoCoinFlip === 'CARGANDO_VS' && (this.preloadPollingId || this.preloadTimeoutId)) {
      return;
    }
    this.estadoCoinFlip = 'CARGANDO_VS';

    this.preloadCinematicStartedAt = performance.now();
    this.cinematicSceneLoadingPercentage = 4;
    this.cinematicAssetsReady = false;
    this.cinematicLoadingBarsVisible = false;

    this.jugadorLoadingPercentage = data.jugadorLoadingPercentage ?? 0;
    this.botLoadingPercentage = data.botLoadingPercentage ?? 0;

    // Recopilar imagenes a precargar
    const imageUrls = new Set<string>();
    
    const extractImages = (tablero: any) => {
      if (!tablero) return;
      [...(tablero.mazo || []), ...(tablero.mano || []), ...(tablero.premios || []), ...(tablero.banca || []), ...(tablero.descarte || [])]
        .forEach(c => {
          if (c?.imagen) imageUrls.add(`/images/cards/${c.id}.png`);
        });
      if (tablero.activo?.imagen) imageUrls.add(`/images/cards/${tablero.activo.id}.png`);
    };

    extractImages(data.jugador);
    extractImages(data.bot);

    // Texturas de tablero base
    imageUrls.add('/images/cards/back.png');

    const totalItems = imageUrls.size;
    let loadedItems = 0;

    if (totalItems === 0) {
      this.updateLoadingPercentage(100);
      return;
    }

    const finalizarPreload = (state: Partida | null = null) => {
      if (this.loadingPhaseFinished) return;
      this.finalizarPreloadFn = null;
      if (this.preloadPollingId) {
        clearInterval(this.preloadPollingId);
        this.preloadPollingId = null;
      }
      if (this.preloadTimeoutId) {
        clearTimeout(this.preloadTimeoutId);
        this.preloadTimeoutId = null;
      }
      this.jugadorLoadingPercentage = 100;
      this.botLoadingPercentage = 100;
      this.loadingPhaseFinished = true;
      this.updateLoadingPercentage(100);
      this.hidratarPreparativosIniciales(state || data);
      this.cdr.detectChanges();
    };

    this.finalizarPreloadFn = finalizarPreload;
    this.serverLoadingComplete = false;
    this.lastPreloadState = data;

    this.preloadTimeoutId = setTimeout(() => {
      this.serverLoadingComplete = true;
      if (!this.versusCanvasInitialized && this.finalizarPreloadFn) {
        this.finalizarPreloadFn(this.lastPreloadState);
      }
    }, 20000);

    this.preloadPollingId = setInterval(() => {
      this.battleService.getState(this.matchId!).subscribe(state => {
        if (state) {
          this.lastPreloadState = state;
          this.botLoadingPercentage = state.botLoadingPercentage ?? 0;
          if (this.jugadorLoadingPercentage >= 99 && this.botLoadingPercentage >= 98) {
            this.serverLoadingComplete = true;
            if (!this.versusCanvasInitialized && this.finalizarPreloadFn) {
              this.finalizarPreloadFn(state);
            }
          } else if (!this.esPartidaOnline(state) && this.botLoadingPercentage < 100) {
             this.updateLoadingPercentage(Math.min(100, this.botLoadingPercentage + 18));
          }
        }
      });
    }, 500);

    Array.from(imageUrls).forEach(url => {
      const img = new Image();
      img.onload = img.onerror = () => {
        loadedItems++;
        const pct = loadedItems >= totalItems ? 100 : Math.min(99, Math.floor((loadedItems / totalItems) * 100));
        this.jugadorLoadingPercentage = pct;
        this.updateLoadingPercentage(pct);
        if (pct >= 100 && (!this.esPartidaOnline(data) || this.botLoadingPercentage >= 98)) {
          this.serverLoadingComplete = true;
          if (!this.versusCanvasInitialized && this.finalizarPreloadFn) {
            this.finalizarPreloadFn(this.lastPreloadState);
          }
        }
      };
      img.src = url;
    });
  }

  private iniciarPollingSalaBatalla(): void {
    if (!this.matchId) return;
    if (this.battleRoomPolling) clearInterval(this.battleRoomPolling);
    this.refrescarSalaBatalla();
    this.battleRoomPolling = setInterval(() => this.refrescarSalaBatalla(), 2200);
  }

  private refrescarSalaBatalla(): void {
    if (!this.matchId) return;
    this.lobbyRoomService.getRoomByMatch(this.matchId).subscribe({
      next: (room) => {
        this.battleRoom = room;
        this.registrarReaccionesBatalla(room.reactions || []);
        this.cdr.detectChanges();
      },
      error: () => {
        this.battleRoom = null;
      }
    });
  }

  sendBattleReaction(reaction: string): void {
    if (!this.matchId) return;
    this.lobbyRoomService.sendMatchReaction(this.matchId, reaction).subscribe({
      next: (room) => {
        this.battleRoom = room;
        this.registrarReaccionesBatalla(room.reactions || []);
      },
      error: () => {}
    });
  }

  private registrarReaccionesBatalla(reactions: LobbyRoomReaction[]): void {
    for (const reaction of reactions) {
      if (!reaction?.id || this.seenBattleReactionIds.has(reaction.id)) continue;
      this.seenBattleReactionIds.add(reaction.id);
      const item = {
        id: reaction.id,
        icon: this.getReactionIcon(reaction.reaction),
        sender: reaction.sender || 'Trainer',
        x: 18 + Math.round(Math.random() * 64)
      };
      this.battleReactions = [...this.battleReactions, item].slice(-16);
      setTimeout(() => {
        this.battleReactions = this.battleReactions.filter(r => r.id !== item.id);
        this.cdr.detectChanges();
      }, 1700);
    }
  }

  floatingTextsPlayer: { id: number; text: string; type: string; targetId?: string }[] = [];
  floatingTextsBot: { id: number; text: string; type: string; targetId?: string }[] = [];
  cartasCurandose = new Set<string>();

  getFloatingTextsPlayer(targetId?: string): any[] {
    if (!targetId) return this.floatingTextsPlayer.filter(t => !t.targetId);
    return this.floatingTextsPlayer.filter(t => t.targetId === targetId || (!t.targetId && targetId === this.partida?.jugador?.activo?.card?.id));
  }

  getFloatingTextsBot(targetId?: string): any[] {
    if (!targetId) return this.floatingTextsBot.filter(t => !t.targetId);
    return this.floatingTextsBot.filter(t => t.targetId === targetId || (!t.targetId && targetId === this.partida?.bot?.activo?.card?.id));
  }

  mostrarTextoFlotante(objetivo: 'jugador' | 'bot', texto: string, tipo = 'info', targetId?: string): void {
    const id = this.nextFloatingTextId++;
    const item = { id, text: texto, type: tipo, targetId };
    if (objetivo === 'jugador') {
      this.floatingTextsPlayer.push(item);
      this.cdr.detectChanges();
      setTimeout(() => {
        this.floatingTextsPlayer = this.floatingTextsPlayer.filter(t => t.id !== id);
        this.cdr.detectChanges();
      }, 2000);
    } else {
      this.floatingTextsBot.push(item);
      this.cdr.detectChanges();
      setTimeout(() => {
        this.floatingTextsBot = this.floatingTextsBot.filter(t => t.id !== id);
        this.cdr.detectChanges();
      }, 2000);
    }
  }

  generarExplosionEnergia(objetivo: 'jugador' | 'bot'): void {
    const colores = ['#ff4444', '#3b82f6', '#10b981', '#fbbf24', '#a855f7', '#f97316', '#6b7280'];
    const particles = [];
    for (let i = 0; i < 20; i++) {
      const angulo = Math.random() * Math.PI * 2;
      const distancia = 50 + Math.random() * 90;
      particles.push({
        color: colores[Math.floor(Math.random() * colores.length)],
        size: 8 + Math.random() * 10,
        duracion: 0.5 + Math.random() * 0.4,
        tx: Math.cos(angulo) * distancia,
        ty: Math.sin(angulo) * distancia
      });
    }
    if (objetivo === 'jugador') {
      this.particulasEnergiaPlayer = particles;
      this.mostrarExplosionEnergiaPlayer = true;
      this.cdr.detectChanges();
      setTimeout(() => {
        this.mostrarExplosionEnergiaPlayer = false;
        this.cdr.detectChanges();
      }, 800);
    } else {
      this.particulasEnergiaBot = particles;
      this.mostrarExplosionEnergiaBot = true;
      this.cdr.detectChanges();
      setTimeout(() => {
        this.mostrarExplosionEnergiaBot = false;
        this.cdr.detectChanges();
      }, 800);
    }
  }

  private getReactionIcon(reaction: string): string {
    const iconMap: Record<string, string> = {
      heart: '♥',
      fire: '🔥',
      spark: '✦',
      coin: '●'
    };
    return iconMap[reaction] || reaction || '✦';
  }

  private updateLoadingPercentage(pct: number): void {
    if (!this.matchId) return;
    if (this.isSpectator) return;
    pct = Math.min(100, Math.max(0, pct));
    this.battleService.actualizarLoading(this.matchId, pct).subscribe(state => {
      if (state) {
        this.botLoadingPercentage = this.loadingPhaseFinished ? 100 : (state.botLoadingPercentage ?? 0);
        if (pct >= 100 && this.botLoadingPercentage >= 100) {
          this.loadingPhaseFinished = true;
          this.jugadorLoadingPercentage = 100;
          this.botLoadingPercentage = 100;
          this.hidratarPreparativosIniciales(state);
        }
        this.cdr.detectChanges();
      }
    });
  }

  dismissLandscapeHint(): void {
    this.landscapeHintDismissed = true;
    localStorage.setItem('battleLandscapeHintDismissed', 'true');
  }

  comenzarApretonMano(event: PointerEvent): void {
    if (this.isSpectator) return;
    if (this.estadoCoinFlip !== 'DAR_LA_MANO' || this.handshakeComplete) return;
    event.preventDefault();
    this.handshakeHolding = true;
    (event.currentTarget as HTMLElement | null)?.setPointerCapture?.(event.pointerId);
    this.syncHandshakeWithServer(true);
    this.startHandshakeLoop(9);
  }

  soltarApretonMano(event?: PointerEvent): void {
    if (this.isSpectator) return;
    if (event) {
      try {
        (event.currentTarget as HTMLElement | null)?.releasePointerCapture?.(event.pointerId);
      } catch {}
    }
    if (this.handshakeComplete) return;
    this.handshakeHolding = false;
    this.syncHandshakeWithServer(true);
    this.startHandshakeLoop(-4.5);
  }

  private startHandshakeLoop(delta: number): void {
    if (this.handshakeInterval) clearInterval(this.handshakeInterval);
    this.handshakeInterval = setInterval(() => this.tickHandshake(delta), 55);
  }

  private tickHandshake(delta: number): void {
    if (this.estadoCoinFlip !== 'DAR_LA_MANO' || this.handshakeComplete) return;
    this.handshakePower = Math.max(0, Math.min(100, this.handshakePower + delta));
    this.handshakeHits = Math.ceil(this.handshakePower / 16.7);
    let forceSync = false;

    if (this.handshakePower >= 100) {
      this.handshakePower = 100;
      forceSync = true;
      if (this.handshakeInterval) {
        clearInterval(this.handshakeInterval);
        this.handshakeInterval = null;
      }
    } else if (!this.handshakeHolding && this.handshakePower <= 0 && this.handshakeInterval) {
      forceSync = true;
      clearInterval(this.handshakeInterval);
      this.handshakeInterval = null;
    }
    this.syncHandshakeWithServer(forceSync);
    this.cdr.detectChanges();
  }

  private iniciarPollingHandshake(): void {
    if (this.handshakePolling) clearInterval(this.handshakePolling);
    this.handshakePolling = setInterval(() => {
      if (!this.matchId || this.estadoCoinFlip !== 'DAR_LA_MANO') return;
      this.battleService.getState(this.matchId).subscribe({
        next: (data) => this.applyHandshakeState(data)
      });
    }, 250);
  }

  private detenerPollingHandshake(): void {
    if (this.handshakePolling) {
      clearInterval(this.handshakePolling);
      this.handshakePolling = null;
    }
  }

  private syncHandshakeWithServer(force = false): void {
    if (!this.matchId) return;
    if (this.isSpectator) return;
    const now = Date.now();
    if (!force && now - this.lastHandshakeSyncAt < 120) return;
    this.lastHandshakeSyncAt = now;

    this.battleService.actualizarHandshakeMoneda(
      this.matchId,
      this.handshakeHolding,
      Math.round(this.handshakePower),
    ).subscribe({
      next: (data) => this.applyHandshakeState(data),
      error: (err) => console.warn('No se pudo sincronizar saludo de moneda', err)
    });
  }

  private applyHandshakeState(data: Partida): void {
    this.partida = data;
    this.handshakePower = data.coinHandshakeJugadorPower ?? this.handshakePower;
    this.opponentHandshakePower = data.coinHandshakeBotPower ?? 0;
    this.opponentHandshakeHolding = !!data.coinHandshakeBotHolding;
    this.handshakeComplete = !!data.coinHandshakeComplete;
    
    this.cargarDatosOponente();

    if (this.handshakeComplete && this.estadoCoinFlip === 'DAR_LA_MANO') {
      this.detenerPollingHandshake();
      if (this.handshakeInterval) {
        clearInterval(this.handshakeInterval);
        this.handshakeInterval = null;
      }
      setTimeout(() => this.finalizarSaludoRival(), 600);
    }
    this.cdr.detectChanges();
  }

  private finalizarSaludoRival(): void {
    if (this.estadoCoinFlip !== 'DAR_LA_MANO') return;
    this.detenerPollingHandshake();
    this.estadoCoinFlip = this.puedeCantarSorteo(this.partida) ? 'ELEGIR_LADO' : 'ESPERANDO_RIVAL';
    if (this.estadoCoinFlip === 'ESPERANDO_RIVAL') {
      this.iniciarPollingSorteo();
    }
    this.cdr.detectChanges();
  }

  seleccionarCaraCruzTemporal(eleccion: 'CARA' | 'CRUZ') {
    if (this.isSpectator || this.confirmadoLado) return;
    this.eleccionTemporal = eleccion;
    this.cdr.detectChanges();
  }

  confirmarSeleccionLado() {
    if (!this.eleccionTemporal) return;
    this.confirmadoLado = true;
    this.eleccionJugador = this.eleccionTemporal;
    
    // Cambiar estado a esperando tiro
    this.estadoCoinFlip = 'ESPERANDO_TIRO';
    this.lanzada = false;
    this.cdr.detectChanges();
  }

  cancelarSeleccionLado() {
    this.confirmadoLado = false;
    this.eleccionTemporal = null;
    this.cdr.detectChanges();
  }

  iniciarSorteo(eleccion: 'CARA' | 'CRUZ') {
    if (this.isSpectator) return;
    if (!this.puedeCantarSorteo(this.partida)) {
      this.estadoCoinFlip = 'ESPERANDO_RIVAL';
      this.iniciarPollingSorteo();
      return;
    }
    this.eleccionTemporal = eleccion;
    this.confirmarSeleccionLado();
  }

  async seleccionarTurno(yoVoyPrimero: boolean) {
    if (this.isSpectator) return;
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
    if (!this.turnTimerEnabled) return;

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
    if (!this.turnTimerEnabled) return;
    this.detenerRelojTurno();
    console.log('Tiempo agotado. Pasando turno automáticamente...');
    await this.pasarTurno();
  }

  finalizarCoinFlip() {
    if (this.pollingSorteo) {
      clearInterval(this.pollingSorteo);
      this.pollingSorteo = null;
    }
    this.estadoCoinFlip = 'OCULTO';
    this.lanzada = false;
    this.girando = false;
    this.boardVisible = true;
    this.cargarEstado();
    this.iniciarPolling();
    this.cdr.detectChanges();
  }

  private obtenerNombreJugadorLocal(): string {
    try {
      const data = localStorage.getItem('jugador');
      return data ? JSON.parse(data).username || '' : '';
    } catch {
      return '';
    }
  }

  get nombreRival(): string {
    if (!this.partida) return 'BOT';
    if (this.partida.botUsername === this.jugadorNombre) {
      return this.partida.jugadorUsername || 'BOT';
    }
    return this.partida.botUsername || 'BOT';
  }

  get nombreGanadorSorteo(): string {
    return this.partida?.coinFlipWinner || this.nombreRival;
  }

  private esPartidaOnline(estado: Partida | null | undefined = this.partida): boolean {
    return !!estado?.botUsername && estado.botUsername !== 'BOT';
  }

  trackByCardId(index: number, item: any): string {
    return item?.card?.id || item?.id || String(index);
  }

  private crearFirmaPartida(estado: Partida | null | undefined): string {
    return estado ? JSON.stringify(estado) : '';
  }

  private async procesarTurnLogs(): Promise<void> {
    if (!this.partida?.turnLogs) return;

    for (let i = this.lastProcessedLogIndex; i < this.partida.turnLogs.length; i++) {
      const log = this.partida.turnLogs[i];
      const parts = log.split(':');
      const event = parts[0];
      const actor = parts[1];
      
      const esMiAccion = actor === this.jugadorNombre || actor === 'JUGADOR';

      switch (event) {
        case 'SUPER_EFFECTIVE':
          this.mostrarFeedbackImpacto(actor, 'effective');
          break;
        case 'RESISTED':
          this.mostrarFeedbackImpacto(actor, 'resisted');
          break;
        case 'PRIZE_TAKEN': {
          const objetivo = esMiAccion ? 'jugador' : 'bot';
          this.mostrarTextoFlotante(objetivo, '¡PREMIO OBTENIDO!', 'prize');
          this.mostrarNotificacion(
            `${esMiAccion ? 'Tomaste' : 'El rival tomo'} ${parts[2] || '1'} carta(s) de Premio.`,
            'info',
          );
          break;
        }
        case 'DISCARD_TOP_DECK': {
          const targetIsMe = esMiAccion;
          const cardNombre = parts[2] || '';
          this.mostrarNotificacion(
            targetIsMe ?
              this.i18nService.translate('battle.discardTopDeck.selfDetail', { card: cardNombre }) :
              this.i18nService.translate('battle.discardTopDeck.oppDetail', { card: cardNombre }),
            'warning'
          );
          break;
        }
        case 'ATTACHED_FROM_DISCARD': {
          const targetIsMe = esMiAccion;
          const cardNombre = parts[2] || '';
          const activePokemon = parts[3] || 'Active';
          const target = targetIsMe ? 'jugador' : 'bot';
          this.mostrarTextoFlotante(target, '+1 ENERGÍA', 'energy-add');
          this.mostrarNotificacion(
            targetIsMe ?
              this.i18nService.translate('battle.attachedFromDiscard.selfDetail', { card: cardNombre, active: activePokemon }) :
              this.i18nService.translate('battle.attachedFromDiscard.oppDetail', { card: cardNombre, active: activePokemon }),
            'info'
          );
          break;
        }
        case 'MAGMA_MANTLE_BOOST': {
          const targetIsMe = esMiAccion;
          const cardNombre = parts[2] || '';
          const target = targetIsMe ? 'jugador' : 'bot';
          this.mostrarTextoFlotante(target, '+50 DAÑO', 'attack');
          this.mostrarNotificacion(
            targetIsMe ?
              this.i18nService.translate('battle.magmaMantle.selfBoost', { card: cardNombre }) :
              this.i18nService.translate('battle.magmaMantle.oppBoost', { card: cardNombre }),
            'info'
          );
          break;
        }
        case 'ENERGY_DISCARDED': {
          const objetivo = esMiAccion ? 'jugador' : 'bot';
          this.mostrarTextoFlotante(objetivo, '-1 ENERGÍA', 'energy-remove');
          this.generarExplosionEnergia(objetivo);
          this.mostrarNotificacion(`${esMiAccion ? 'Descartaste' : 'El rival descartó'} una energía al ataque.`, 'warning');
          break;
        }
        case 'DECK_SEARCHED':
          this.mostrarNotificacion(`${esMiAccion ? 'Buscaste' : 'El rival buscó'} cartas en el mazo.`, 'info');
          break;
        case 'ENERGY_MOVED':
          this.mostrarNotificacion(`${esMiAccion ? 'Moviste' : 'El rival movió'} una energía.`, 'info');
          break;
        case 'STATUS_APPLIED': {
          const condicion = parts[3];
          const objetivo = esMiAccion ? 'bot' : 'jugador';
          let textoCondicion = condicion ? condicion.toUpperCase() : 'ESTADO';
          if (condicion === 'Poisoned') textoCondicion = 'ENVENENADO';
          else if (condicion === 'Asleep') textoCondicion = 'DORMIDO';
          else if (condicion === 'Burned') textoCondicion = 'QUEMADO';
          else if (condicion === 'Paralyzed') textoCondicion = 'PARALIZADO';
          else if (condicion === 'Confused') textoCondicion = 'CONFUNDIDO';
          else if (condicion === 'CantRetreat') textoCondicion = 'RETIRADA BLOQUEADA';
          this.mostrarTextoFlotante(objetivo, `¡${textoCondicion}!`, 'status');
          if (condicion && condicion !== 'CantRetreat') {
            this.mostrarEfectoStatusVisual(objetivo, condicion);
          }
          break;
        }
        case 'ATTACK_USED': {
          const atacante = esMiAccion ? 'jugador' : 'bot';
          const attackName = parts[2];
          this.mostrarTextoFlotante(atacante, `¡${attackName.toUpperCase()}!`, 'attack');
          if (attackName) this.triggerAttackBanner(attackName, !esMiAccion);
          break;
        }
        case 'ENERGY_ATTACHED': {
          const objetivo = esMiAccion ? 'jugador' : 'bot';
          const cardName = parts[3];
          this.mostrarTextoFlotante(objetivo, '+1 ENERGÍA', 'energy-add');
          if (cardName) this.triggerCardSplash(cardName, !esMiAccion);
          break;
        }
        case 'POKEMON_PLAYED': {
          const cardName = parts[2];
          if (cardName) this.triggerCardSplash(cardName, !esMiAccion);
          break;
        }
        case 'TRAINER_PLAYED': {
          const cardName = parts[2];
          this.mostrarNotificacion(
            this.i18nService.translate(esMiAccion ? 'battle.playedTrainerSelf' : 'battle.playedTrainerOpponent', { card: cardName }),
            'info'
          );
          if (cardName) this.triggerCardSplash(cardName, !esMiAccion);
          break;
        }
        case 'KNOCK_OUT': {
          const objetivo = esMiAccion ? 'bot' : 'jugador';
          this.mostrarTextoFlotante(objetivo, '¡FUERA DE COMBATE!', 'ko');
          break;
        }
        case 'MUERTE_SUBITA': {
          this.battleNotificationService.showModal('Muerte Súbita', 'Empate total. El próximo premio define la partida.', 'warning');
          break;
        }
        case 'POISON_DAMAGE': {
          const targetOwner = parts[1];
          const damageAmount = parseInt(parts[3], 10) || 10;
          const objetivo =
            targetOwner === this.jugadorNombre || targetOwner === 'JUGADOR' ? 'jugador' : 'bot';
          this.mostrarTextoFlotante(
            objetivo,
            `${this.getLocalizedStatusLabel('Poisoned')} -${damageAmount}`,
            'status-poisoned',
          );
          this.mostrarEfectoStatusVisual(objetivo, 'Poisoned');
          break;
        }
        case 'BURN_DAMAGE': {
          const targetOwner = parts[1];
          const damageAmount = parseInt(parts[3], 10) || 20;
          const objetivo =
            targetOwner === this.jugadorNombre || targetOwner === 'JUGADOR' ? 'jugador' : 'bot';
          this.mostrarTextoFlotante(
            objetivo,
            `${this.getLocalizedStatusLabel('Burned')} -${damageAmount}`,
            'status-burned',
          );
          this.mostrarEfectoStatusVisual(objetivo, 'Burned');
          break;
        }
        case 'HEALED': {
          const targetOwner = parts[1];
          const targetId = parts[2];
          const amount = parseInt(parts[3], 10) || 0;
          const objetivo =
            targetOwner === this.jugadorNombre || targetOwner === 'JUGADOR' ? 'jugador' : 'bot';
          this.mostrarTextoFlotante(objetivo, `+${amount} HP`, 'status-healed-text', targetId);
          this.mostrarCuracion(objetivo, targetId);
          await this.delay(1200); // Dar tiempo para que el jugador vea la animación de curación
          break;
        }
        case 'AWAKE_FLIP_HEADS': {
          const targetOwner = parts[1];
          const objetivo =
            targetOwner === this.jugadorNombre || targetOwner === 'JUGADOR' ? 'jugador' : 'bot';
          
          while (this.showTurnOverlay) {
            await this.delay(100);
          }
          await this.delay(500);

          this.mostrarTextoFlotante(objetivo, '¡Despierta! (CARA)', 'status-healed-text');
          await this.animarMonedasSincronizadas('Chequeo de Sueño', { cantidadMonedas: 1, esSoloEstado: true, danioBase: 0, danioExtraPorCara: 0, descripcion: '' }, 1, true);
          break;
        }
        case 'AWAKE_FLIP_TAILS': {
          const targetOwner = parts[1];
          const objetivo =
            targetOwner === this.jugadorNombre || targetOwner === 'JUGADOR' ? 'jugador' : 'bot';

          while (this.showTurnOverlay) {
            await this.delay(100);
          }
          await this.delay(500);

          this.mostrarTextoFlotante(objetivo, 'Sigue Dormido (CRUZ)', 'status-asleep');
          await this.animarMonedasSincronizadas('Chequeo de Sueño', { cantidadMonedas: 1, esSoloEstado: true, danioBase: 0, danioExtraPorCara: 0, descripcion: '' }, 0, true);
          break;
        }
        case 'ASTONISH_REVEALED': {
          const cardId = (parts[2] || '').trim();
          const cardNombre = parts[3];
          const targetIsMe = esMiAccion;

          this.astonishReveal = {
            visible: true,
            cardId,
            cardNombre,
            esMiAccion: !targetIsMe,
            isFlipped: false
          };
          this.cdr.detectChanges();

          setTimeout(() => {
            if (this.astonishReveal) {
              this.astonishReveal.isFlipped = true;
              this.cdr.detectChanges();
            }
          }, 800);

          setTimeout(() => {
            this.cerrarAstonishReveal();
          }, 6500);

          const objetivo = targetIsMe ? 'jugador' : 'bot';
          this.mostrarTextoFlotante(objetivo, '¡ASTONISH!', 'attack');

          const detailMsg = targetIsMe ?
            this.i18nService.translate('battle.astonish.oppDetail', { card: cardNombre }) :
            this.i18nService.translate('battle.astonish.selfDetail', { card: cardNombre });

          this.mostrarNotificacion(detailMsg, 'warning');
          break;
        }
        case 'BENCH_DAMAGE': {
          const targetOwner = parts[1];
          const cardId = (parts[2] || '').trim();
          const cardNombre = parts[3];
          const damageAmount = parseInt(parts[4], 10);
          const isMyBench = targetOwner === 'JUGADOR' || targetOwner === this.jugadorNombre;
          const targetSide = isMyBench ? 'jugador' : 'bot';
          
          this.mostrarTextoFlotante(targetSide, `-${damageAmount}`, 'damage', cardId);
          
          const detailMsg = isMyBench ?
            this.i18nService.translate('battle.benchDamage.selfDetail', { name: cardNombre, damage: damageAmount.toString() }) :
            this.i18nService.translate('battle.benchDamage.oppDetail', { name: cardNombre, damage: damageAmount.toString() });
          this.mostrarNotificacion(detailMsg, 'warning');
          await this.delay(1000);
          break;
        }
      }
    }
    this.lastProcessedLogIndex = this.partida.turnLogs.length;
  }

  getOpponentHandStep(total: number): number {
    if (total <= 1) return 0;
    return Math.max(7, Math.min(21, 610 / (total - 1)));
  }

  get pendingAction() {
    const pending = this.partida?.pendingAction;
    return pending?.actor === this.jugadorNombre ? pending : null;
  }

  get visualBattleLog(): Array<{ kind: string; title: string; detail: string }> {
    const logs = (this.partida?.turnLogs || []).slice().reverse().map(log => this.formatBattleLog(log));
    if (logs.length) return logs;
    if (!this.partida) return [];
    return [
      {
        kind: 'phase',
        title: this.partida.faseActual === 'TURNO_NORMAL' ? 'Turno en curso' : 'Preparativos',
        detail: `Fase ${this.partida.faseActual} · Turno ${this.partida.numeroTurno || 1}`
      }
    ];
  }

  private formatBattleLog(log: string): { kind: string; title: string; detail: string } {
    const parts = (log || '').split(':');
    const event = parts[0] || 'EVENT';
    const actor = parts[1] || 'Sistema';
    const esMiAccion = actor === this.jugadorNombre || actor === 'JUGADOR';
    const subject = esMiAccion ? 'Vos' : (actor === 'BOT' ? this.nombreRival : actor);
    const first = parts[2] || '';
    const second = parts[3] || '';
    const third = parts[4] || '';
    const map: Record<string, { title: string; detail: string; kind: string }> = {
      ASTONISH_REVEALED: { 
        title: this.i18nService.translate('battle.astonish.title'), 
        detail: esMiAccion ? 
          this.i18nService.translate('battle.astonish.oppDetail', { card: second }) : 
          this.i18nService.translate('battle.astonish.selfDetail', { card: second }), 
        kind: 'search' 
      },
      BENCH_DAMAGE: {
        title: this.i18nService.translate('battle.benchDamage.title'),
        detail: esMiAccion ?
          this.i18nService.translate('battle.benchDamage.selfDetail', { name: second, damage: third }) :
          this.i18nService.translate('battle.benchDamage.oppDetail', { name: second, damage: third }),
        kind: 'damage'
      },
      ENERGY_DISCARDED: { title: 'Energia descartada', detail: esMiAccion ? 'Descartaste energia para resolver un ataque.' : `${subject} descarto energia para resolver un ataque.`, kind: 'energy' },
      DECK_SEARCHED: { title: 'Busqueda en mazo', detail: esMiAccion ? 'Buscaste cartas en el mazo.' : `${subject} busco cartas en el mazo.`, kind: 'search' },
      ENERGY_MOVED: { title: 'Energia movida', detail: esMiAccion ? 'Moviste una energia en el campo.' : `${subject} movio una energia en el campo.`, kind: 'energy' },
      POKEMON_PLAYED: { title: 'Pokemon en banca', detail: esMiAccion ? `Bajaste a ${first || 'un Pokemon'} a la banca.` : `${subject} bajo a ${first || 'un Pokemon'} a la banca.`, kind: 'bench' },
      ACTIVE_PLACED: { title: 'Activo preparado', detail: esMiAccion ? 'Preparaste tu Pokemon activo.' : `${subject} preparo su Pokemon activo.`, kind: 'setup' },
      BENCH_PLACED: { title: 'Banca preparada', detail: esMiAccion ? 'Preparaste un Pokemon en banca.' : `${subject} preparo un Pokemon en banca.`, kind: 'setup' },
      PRIZES_PLACED: { title: 'Premios colocados', detail: esMiAccion ? 'Colocaste tus premios.' : `${subject} coloco sus premios.`, kind: 'setup' },
      PRIZE_TAKEN: { title: 'Premio tomado', detail: esMiAccion ? `Tomaste ${first || '1'} carta(s) de Premio.` : `${subject} tomo ${first || '1'} carta(s) de Premio.`, kind: 'prize' },
      MUERTE_SUBITA: { title: 'Muerte subita', detail: 'Empate total. El siguiente premio define la batalla.', kind: 'system' },
      KNOCK_OUT: { title: 'Fuera de combate', detail: `${first || 'Un Pokemon'} quedo fuera de combate.`, kind: 'ko' },
      ENERGY_ATTACHED: { title: 'Energia unida', detail: esMiAccion ? `Uniste energia a ${first || 'tu Pokemon'}.` : `${subject} unio energia a ${first || 'su Pokemon'}.`, kind: 'energy' },
      CARDS_DRAWN: { title: 'Cartas robadas', detail: esMiAccion ? `Robaste ${first || '1'} carta(s).` : `${subject} robo ${first || '1'} carta(s).`, kind: 'draw' },
      ATTACK_USED: { title: 'Ataque', detail: esMiAccion ? `Usaste ${first || 'un ataque'} contra ${second || 'el rival'}${third ? ` por ${third} de dano` : ''}.` : `${subject} uso ${first || 'un ataque'} contra ${second || 'el rival'}${third ? ` por ${third} de dano` : ''}.`, kind: 'attack' },
      SUPER_EFFECTIVE: { title: 'Super efectivo', detail: `El ataque de ${subject} aprovecho la debilidad de ${first || 'su objetivo'}.`, kind: 'effective' },
      RESISTED: { title: 'Ataque resistido', detail: `${first || 'El objetivo'} resistio parte del ataque de ${subject}.`, kind: 'resisted' },
      STATUS_APPLIED: { title: 'Estado aplicado', detail: `${subject} dejo a ${first || 'un Pokemon'} ${this.traducirCondicionLog(second)}.`, kind: 'status' },
      TURN_PASSED: { title: 'Turno terminado', detail: esMiAccion ? 'Terminaste tu turno.' : `${subject} termino su turno.`, kind: 'phase' },
      TURN_STARTED: { title: 'Nuevo turno', detail: esMiAccion ? 'Comienza tu turno.' : `Comienza el turno de ${subject}.`, kind: 'phase' },
      SURRENDERED: { title: 'Rendicion', detail: esMiAccion ? 'Te rendiste.' : `${subject} se rindio.`, kind: 'system' },
      DISCONNECTED: { title: 'Desconexion', detail: esMiAccion ? 'Te desconectaste.' : `${subject} se desconecto.`, kind: 'system' },
      DISCARD_TOP_DECK: {
        title: this.i18nService.translate('battle.discardTopDeck.title'),
        detail: esMiAccion ?
          this.i18nService.translate('battle.discardTopDeck.selfDetail', { card: first }) :
          this.i18nService.translate('battle.discardTopDeck.oppDetail', { card: first }),
        kind: 'discard'
      },
      ATTACHED_FROM_DISCARD: {
        title: this.i18nService.translate('battle.attachedFromDiscard.title'),
        detail: esMiAccion ?
          this.i18nService.translate('battle.attachedFromDiscard.selfDetail', { card: first, active: second }) :
          this.i18nService.translate('battle.attachedFromDiscard.oppDetail', { card: first, active: second }),
        kind: 'energy'
      },
      MAD_MOUNTAIN_DISCARDED: {
        title: this.i18nService.translate('battle.madMountain.title'),
        detail: this.i18nService.translate('battle.madMountain.detail', { subject, flip1: first, flip2: second, count: third }),
        kind: 'attack'
      },
      MAGMA_MANTLE_BOOST: {
        title: this.i18nService.translate('battle.magmaMantle.title'),
        detail: esMiAccion ?
          this.i18nService.translate('battle.magmaMantle.selfBoost', { card: first }) :
          this.i18nService.translate('battle.magmaMantle.oppBoost', { card: first }),
        kind: 'attack'
      },
      DECK_PEEKED: {
        title: this.i18nService.translate('battle.deckPeeked.title'),
        detail: esMiAccion ?
          this.i18nService.translate('battle.deckPeeked.selfDetail', { count: first }) :
          this.i18nService.translate('battle.deckPeeked.oppDetail', { count: first }),
        kind: 'search'
      }
    };
    return map[event] || {
      title: event.replaceAll('_', ' ').toLowerCase(),
      detail: log,
      kind: 'system'
    };
  }

  private traducirCondicionLog(condition: string): string {
    const map: Record<string, string> = {
      Paralyzed: 'paralizado',
      Asleep: 'dormido',
      Confused: 'confundido',
      Poisoned: 'envenenado',
      Burned: 'quemado',
      CantRetreat: 'sin retirada'
    };
    return map[condition] || condition || 'afectado';
  }

  togglePendingEffectCard(cardId: string): void {
    const pending = this.partida?.pendingAction;
    if (!pending) return;
    if (this.pendingEffectSelection.has(cardId)) {
      this.pendingEffectSelection.delete(cardId);
      return;
    }
    if (this.pendingEffectSelection.size < pending.maxSelections) {
      this.pendingEffectSelection.add(cardId);
    }
  }

  isPendingEffectCardSelected(cardId: string): boolean {
    return this.pendingEffectSelection.has(cardId);
  }

  /** Returns the 1-based selection order for REORDER_TOP_DECK mode, or 0 if not selected. */
  getPendingEffectOrderIndex(cardId: string): number {
    const arr = [...this.pendingEffectSelection];
    const idx = arr.indexOf(cardId);
    return idx === -1 ? 0 : idx + 1;
  }

  get isReorderMode(): boolean {
    return this.partida?.pendingAction?.type === 'REORDER_TOP_DECK';
  }

  canConfirmPendingEffect(): boolean {
    const pending = this.partida?.pendingAction;
    if (!pending) return false;
    return this.pendingEffectSelection.size >= pending.minSelections
      && this.pendingEffectSelection.size <= pending.maxSelections;
  }

  ejecutarResolverEfecto(estadoAntiguo: any, selectedIds: string[]): void {
    this.cargandoAccion = true;
    this.battleService.resolverEfecto(this.matchId!, selectedIds).subscribe({
      next: async (estado) => {
        this.pendingEffectSelection.clear();
        if (estado.turnoActual === 'BOT' && estadoAntiguo?.turnoActual === 'JUGADOR') {
          await this.procesarPostPassTurn(estadoAntiguo, estado);
        } else {
          await this.aplicarEstadoRefrescado(estado);
          this.cargandoAccion = false;
          this.cdr.detectChanges();
        }
      },
      error: error => {
        this.cargandoAccion = false;
        this.mostrarNotificacion(error?.error || 'No se pudo resolver el efecto.', 'warning');
      },
    });
  }

  confirmPendingEffect(): void {
    if (!this.matchId || !this.canConfirmPendingEffect()) return;
    this.cargandoAccion = true;
    const estadoAntiguo = this.battleBoardState.clonarPartida(this.partida);
    const selectedIds = [...this.pendingEffectSelection];

    const pending = this.partida?.pendingAction;
    if (pending && pending.type === 'DISCARD_RECOVERY' && selectedIds.length > 0) {
      const targetCardId = selectedIds[0];
      const card = this.partida?.jugador?.pilaDescarte?.find((c: any) => c.id === targetCardId);
      if (card) {
        this.bloqueadoPorAnimacion = true;
        const spriteUrl = this.getSpriteFront(card);
        this.reviveAnimationActive = {
          visible: true,
          cardId: card.id,
          cardNombre: card.nombre,
          spriteUrl: spriteUrl || '',
          hpProgress: 0,
          card: card
        };
        this.cdr.detectChanges();
        
        setTimeout(() => {
          if (!this.reviveAnimationActive) return;
          const interval = setInterval(() => {
            if (!this.reviveAnimationActive) {
              clearInterval(interval);
              return;
            }
            if (this.reviveAnimationActive.hpProgress < 100) {
              this.reviveAnimationActive.hpProgress += 5;
              this.cdr.detectChanges();
            } else {
              clearInterval(interval);
            }
          }, 70);
          
          setTimeout(() => {
            this.reviveAnimationActive = null;
            this.bloqueadoPorAnimacion = false;
            this.cdr.detectChanges();
            this.ejecutarResolverEfecto(estadoAntiguo, selectedIds);
          }, 2200);
          
        }, 1000);
        return;
      }
    }

    this.ejecutarResolverEfecto(estadoAntiguo, selectedIds);
  }

  private mostrarFeedbackImpacto(actor: string, kind: 'effective' | 'resisted'): void {
    const atacanteEsJugador = actor === this.jugadorNombre || actor === 'JUGADOR';
    const objetivo = atacanteEsJugador ? 'bot' : 'jugador';
    if (objetivo === 'bot') {
      this.impactCalloutBot = kind;
      this.dispararParticulas('bot', kind === 'effective' ? 'Fire' : 'Metal');
    } else {
      this.impactCalloutPlayer = kind;
      this.dispararParticulas('jugador', kind === 'effective' ? 'Fire' : 'Metal');
    }
    this.battlefieldDamagePulse = kind === 'effective';
    this.cdr.detectChanges();
    window.setTimeout(() => {
      if (objetivo === 'bot') this.impactCalloutBot = null;
      else this.impactCalloutPlayer = null;
      this.battlefieldDamagePulse = false;
      this.cdr.detectChanges();
    }, 1100);
  }

  private puedeCantarSorteo(estado: Partida | null | undefined = this.partida): boolean {
    return !this.esPartidaOnline(estado) || estado?.coinFlipCallerUsername === this.jugadorNombre;
  }

  private iniciarPollingSorteo(): void {
    if (this.pollingSorteo) clearInterval(this.pollingSorteo);

    this.pollingSorteo = setInterval(() => {
      if (!this.matchId || this.estadoCoinFlip === 'OCULTO') return;

      this.battleService.getState(this.matchId).subscribe({
        next: async (data) => {
          this.partida = data;
          this.lastAppliedStateSignature = this.crearFirmaPartida(data);

          if (data.faseActual === 'FIN_PARTIDA') {
            this.handleGameEnd(data);
            return;
          }

          if (data.faseActual !== 'LANZAMIENTO_MONEDA') {
            this.finalizarCoinFlip();
            return;
          }

          if (!data.coinFlipped) {
            return;
          }

          if (this.estadoCoinFlip === 'ESPERANDO_RIVAL') {
            this.reproducirAnimacionMonedaRival(data);
            return;
          }

          this.resultadoMoneda = data.coinFlipResult === 'CRUZ' ? 'CRUZ' : 'CARA';
          console.log('[Polling Sorteo] Moneda lanzada detectada en backend. Winner:', data.coinFlipWinner, 'Jugador:', this.jugadorNombre);
          if (data.coinFlipWinner === this.jugadorNombre) {
            console.log('[Polling Sorteo] El jugador ganó. Pasando a ELEGIR_TURNO');
            this.estadoCoinFlip = 'ELEGIR_TURNO';
          } else {
            console.log('[Polling Sorteo] El bot ganó. Pasando a RESULTADO_BOT');
            this.estadoCoinFlip = 'RESULTADO_BOT';
            this.cdr.detectChanges();
            const online = this.esPartidaOnline(data);
            console.log('[Polling Sorteo] ¿Es partida online?', online);
            if (!online) {
              console.log('[Polling Sorteo] Deteniendo polling del sorteo para proceder...');
              if (this.pollingSorteo) {
                clearInterval(this.pollingSorteo);
                this.pollingSorteo = null;
              }
              console.log('[Polling Sorteo] Esperando delay de 2s...');
              await this.delay(2000);
              console.log('[Polling Sorteo] Enviando elegirTurno para bot:', this.nombreRival);
              try {
                await firstValueFrom(this.battleService.elegirTurno(this.matchId!, false, this.nombreRival));
                console.log('[Polling Sorteo] elegirTurno bot exitoso!');
              } catch (err) {
                console.error('[Polling Sorteo] Error al elegir turno del bot en polling:', err);
              }
              console.log('[Polling Sorteo] Finalizando coin flip');
              this.finalizarCoinFlip();
            }
          }
          this.cdr.detectChanges();
        },
      });
    }, 1000);
  }

  private async reproducirAnimacionMonedaRival(data: Partida) {
    if (this.pollingSorteo) {
      clearInterval(this.pollingSorteo);
      this.pollingSorteo = null;
    }
    
    this.estadoCoinFlip = 'GIRANDO';
    this.lanzada = true;
    this.girando = true;
    this.cdr.detectChanges();

    // Esperar a que la escena se monte y cargue
    while (this.loadingCoinModels && this.coinFlipCanvasInitialized) {
      await this.delay(50);
    }
    await this.delay(400); // Pausa dramática

    if (this.coinFlipControls) this.coinFlipControls.enabled = false;

    // Posicionar la moneda inicialmente en la mano derecha del bot
    if (this.coinFlipCoinModel) {
      this.coinFlipCoinModel.position.set(-0.2, 0.0, -2.1);
    }

    // Disparar físicas automatizadas en 3D
    this.coinFlipVuelo = true;
    this.coinRebotes = 0;
    
    const fuerzaSimulada = 200 + Math.random() * 150;
    this.coinVy = 7.0 + (fuerzaSimulada / 400) * 11.5; 
    this.coinVx = (Math.random() - 0.5) * 1.5;
    // Lanzar hacia adelante en Z positivo (hacia el jugador)
    this.coinVz = 1.8 + (fuerzaSimulada / 400) * 2.5; 
    this.coinOmegaX = 25.0 + (fuerzaSimulada / 400) * 30.0 + Math.random() * 10;
    this.coinOmegaY = (Math.random() - 0.5) * 8;
    this.coinOmegaZ = (Math.random() - 0.5) * 8;

    this.resultadoMoneda = data.coinFlipResult === 'CRUZ' ? 'CRUZ' : 'CARA';
    this.coinFlipResultadoEsperado = this.resultadoMoneda;
    this.coinFlipResultadoListo = true;
    this.cdr.detectChanges();

    // Esperar fin de parábola
    while (this.coinFlipVuelo && this.coinFlipCanvasInitialized) {
      await this.delay(50);
    }

    if (this.coinFlipControls && this.coinFlipCoinModel) {
      this.coinFlipControls.enabled = true;
      this.coinFlipControls.target.copy(this.coinFlipCoinModel.position);
    }

    // Baile de celebración/derrota justo después de caer
    if (data.coinFlipWinner === this.jugadorNombre) {
      if (this.playerCoinFlipMixer) this.setCelebrationAnimation(this.playerCoinFlipMixer, this.playerCoinFlipActions);
      if (this.opponentCoinFlipMixer) this.setDefeatAnimation(this.opponentCoinFlipMixer, this.opponentCoinFlipActions);
    } else {
      if (this.opponentCoinFlipMixer) this.setCelebrationAnimation(this.opponentCoinFlipMixer, this.opponentCoinFlipActions);
      if (this.playerCoinFlipMixer) this.setDefeatAnimation(this.playerCoinFlipMixer, this.playerCoinFlipActions);
    }
    this.cdr.detectChanges();

    // Enfocar y mostrar el resultado en el piso por 5 segundos enteros (5000 ms) antes de avanzar
    await this.delay(5000);

    console.log('[Sorteo Rival] Fin animación. Winner:', data.coinFlipWinner, 'Jugador:', this.jugadorNombre);
    if (data.coinFlipWinner === this.jugadorNombre) {
      console.log('[Sorteo Rival] El jugador ganó. Pasando a ELEGIR_TURNO');
      this.estadoCoinFlip = 'ELEGIR_TURNO';
    } else {
      console.log('[Sorteo Rival] El bot ganó. Pasando a RESULTADO_BOT');
      this.estadoCoinFlip = 'RESULTADO_BOT';
      this.cdr.detectChanges();
      const online = this.esPartidaOnline(data);
      console.log('[Sorteo Rival] ¿Es partida online?', online);
      if (online) {
        console.log('[Sorteo Rival] Iniciando polling del sorteo...');
        this.iniciarPollingSorteo();
      } else {
        console.log('[Sorteo Rival] Partida offline. Iniciando delay de 2s para elección del bot...');
        await this.delay(2000);
        console.log('[Sorteo Rival] Enviando elegirTurno para bot:', this.nombreRival);
        try {
          await firstValueFrom(this.battleService.elegirTurno(this.matchId!, false, this.nombreRival));
          console.log('[Sorteo Rival] elegirTurno bot exitoso!');
        } catch (err) {
          console.error('[Sorteo Rival] Error al elegir turno del bot en animacion:', err);
        }
        console.log('[Sorteo Rival] Finalizando coin flip');
        this.finalizarCoinFlip();
      }
    }
    this.cdr.detectChanges();
  }

  onCoinPointerDown(event: PointerEvent) {
    if (this.lanzada || this.estadoCoinFlip !== 'ESPERANDO_TIRO' || this.loadingCoinModels) return;
    if (!this.coinFlipCanvasInitialized || !this.coinFlipCamera || !this.coinFlipCoinModel || !this.coinFlipRenderer) return;

    // Permitir arrastrar desde cualquier parte del canvas para mayor accesibilidad
    const canvas = this.coinFlipRenderer.domElement;
    if (this.coinFlipControls) this.coinFlipControls.enabled = false;

    event.preventDefault();
    this.coinPointerId = event.pointerId;
    this.yStart = event.clientY;
    this.xStart = event.clientX;
    this.arrastrando = true;
    this.fuerzaActual = 0;
    canvas.setPointerCapture?.(event.pointerId);
  }

  onCoinPointerMove(event: PointerEvent) {
    if (!this.arrastrando || event.pointerId !== this.coinPointerId) return;
    event.preventDefault();
    
    const deltaY = event.clientY - this.yStart;
    const deltaX = event.clientX - this.xStart;
    const dist = Math.sqrt(deltaX * deltaX + deltaY * deltaY);
    this.fuerzaActual = Math.min(Math.max(dist, 0), 400);
    this.cdr.detectChanges();
  }

  onCoinPointerCancel(event: PointerEvent) {
    if (event.pointerId !== this.coinPointerId) return;
    this.coinPointerId = null;
    this.arrastrando = false;
    this.fuerzaActual = 0;
    if (this.coinFlipControls) this.coinFlipControls.enabled = true;
    this.cdr.detectChanges();
  }

  interTurnOverlay: InterTurnOverlayState | null = null;
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

    this.interTurnOverlay = null;
    this.cdr.detectChanges();

    await this.delay(200);
  }

  async mostrarKOAnim(): Promise<void> {
    this.mostrarKO = true;
    this.cdr.detectChanges();
    await this.delay(1200);
    this.mostrarKO = false;
    this.cdr.detectChanges();
  }

  async mostrarCuracion(objetivo: 'bot' | 'jugador', targetId?: string, duracion = 1500): Promise<void> {
    if (targetId) {
      this.cartasCurandose.add(targetId);
    } else {
      if (objetivo === 'bot') this.mostrarAuraCuracionBot = true;
      else this.mostrarAuraCuracionPlayer = true;
    }
    this.cdr.detectChanges();

    await this.delay(duracion);

    if (targetId) {
      this.cartasCurandose.delete(targetId);
    } else {
      if (objetivo === 'bot') this.mostrarAuraCuracionBot = false;
      else this.mostrarAuraCuracionPlayer = false;
    }
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

  async onCoinPointerUp(event: PointerEvent) {
    if (this.coinPointerId !== null && event.pointerId !== this.coinPointerId) return;

    if (this.arrastrando) {
      event.preventDefault();
      this.coinPointerId = null;
      this.arrastrando = false;

      const deltaY = event.clientY - this.yStart;
      const deltaX = event.clientX - this.xStart;
      const dist = Math.sqrt(deltaX * deltaX + deltaY * deltaY);
      const fuerzaFinal = Math.min(Math.max(dist, 0), 400);

      if (fuerzaFinal >= 50) {
        this.lanzada = true;
        this.fuerzaActual = fuerzaFinal;
        this.cdr.detectChanges();
        if (this.coinFlipControls) this.coinFlipControls.enabled = true;
        await this.lanzarMoneda3DConAngulos(fuerzaFinal, deltaX, deltaY);
      } else {
        this.fuerzaActual = 0;
        if (this.coinFlipControls) this.coinFlipControls.enabled = true;
        this.cdr.detectChanges();
      }
    } else {
      this.coinPointerId = null;
    }
  }

  getFuerzaColor(): string {
    const ratio = this.fuerzaActual / 400;
    if (ratio < 0.4) {
      return '#10b981'; // verde
    } else if (ratio < 0.75) {
      return '#fbbf24'; // amarillo/naranja
    } else {
      return '#ef4444'; // rojo brillante
    }
  }

  private async lanzarMoneda3DConAngulos(fuerza: number, deltaX: number, deltaY: number): Promise<void> {
    try {
      this.coinFlipVuelo = true;
      this.coinFlipReleaseTime = 0;
      this.coinRebotes = 0;
      this.girando = true;
      this.estadoCoinFlip = 'GIRANDO';

      if (this.playerCoinFlipMixer) {
        const throwHints = ['attack', 'slash', 'shoot', 'throw', 'run'];
        let throwAction: THREE.AnimationAction | undefined;
        for (const hint of throwHints) {
          throwAction = this.playerCoinFlipActions.get(hint);
          if (throwAction) break;
        }
        if (throwAction) {
          this.playerCoinFlipActions.get('idle')?.fadeOut(0.15);
          throwAction.reset().fadeIn(0.1).setLoop(THREE.LoopOnce, 1).play();
          throwAction.clampWhenFinished = true;
        }
      }

      if (this.coinFlipControls) this.coinFlipControls.enabled = false;

      // Impulsos físicos basados en swipe vector (dirección opuesta con resortera/flick)
      this.coinVy = 7.0 + (fuerza / 400) * 11.5; 
      this.coinVx = -deltaX * 0.02;
      // Lanzar hacia adelante en Z negativo (hacia el bot)
      this.coinVz = -Math.abs(deltaY) * 0.02 - 1.5;

      this.coinOmegaX = 25.0 + (fuerza / 400) * 35.0;
      this.coinOmegaY = -deltaX * 0.08;
      this.coinOmegaZ = deltaY * 0.08;
      
      this.cdr.detectChanges();

      const estadoSorteo = await firstValueFrom(
        this.battleService.lanzarMoneda(this.matchId!, this.eleccionJugador),
      );
      this.partida = estadoSorteo;
      this.resultadoMoneda = estadoSorteo.coinFlipResult === 'CRUZ' ? 'CRUZ' : 'CARA';

      this.coinFlipResultadoEsperado = this.resultadoMoneda;
      this.coinFlipResultadoListo = true;

      while (this.coinFlipVuelo && this.coinFlipCanvasInitialized) {
        await this.delay(50);
      }

      if (this.coinFlipControls && this.coinFlipCoinModel) {
        this.coinFlipControls.enabled = true;
        this.coinFlipControls.target.copy(this.coinFlipCoinModel.position);
      }

      // Baile de celebración/derrota justo después de caer
      if (estadoSorteo.coinFlipWinner === this.jugadorNombre) {
        if (this.playerCoinFlipMixer) this.setCelebrationAnimation(this.playerCoinFlipMixer, this.playerCoinFlipActions);
        if (this.opponentCoinFlipMixer) this.setDefeatAnimation(this.opponentCoinFlipMixer, this.opponentCoinFlipActions);
      } else {
        if (this.opponentCoinFlipMixer) this.setCelebrationAnimation(this.opponentCoinFlipMixer, this.opponentCoinFlipActions);
        if (this.playerCoinFlipMixer) this.setDefeatAnimation(this.playerCoinFlipMixer, this.playerCoinFlipActions);
      }
      this.cdr.detectChanges();

      // Enfocar y mostrar el resultado en el piso por 5 segundos enteros (5000 ms) antes de avanzar
      await this.delay(5000);

      console.log('[Sorteo Manual] Tiro realizado. Winner:', estadoSorteo.coinFlipWinner, 'Jugador:', this.jugadorNombre);
      if (estadoSorteo.coinFlipWinner === this.jugadorNombre) {
        console.log('[Sorteo Manual] El jugador ganó. Pasando a ELEGIR_TURNO');
        this.estadoCoinFlip = 'ELEGIR_TURNO';
      } else {
        console.log('[Sorteo Manual] El bot ganó. Pasando a RESULTADO_BOT');
        this.estadoCoinFlip = 'RESULTADO_BOT';
        this.cdr.detectChanges();
        const online = this.esPartidaOnline(estadoSorteo);
        console.log('[Sorteo Manual] ¿Es partida online?', online);
        if (online) {
          console.log('[Sorteo Manual] Iniciando polling del sorteo...');
          this.iniciarPollingSorteo();
        } else {
          console.log('[Sorteo Manual] Partida offline. Iniciando delay de 2s...');
          await this.delay(2000);
          console.log('[Sorteo Manual] Enviando elegirTurno para bot:', this.nombreRival);
          try {
            await firstValueFrom(this.battleService.elegirTurno(this.matchId!, false, this.nombreRival));
            console.log('[Sorteo Manual] elegirTurno bot exitoso!');
          } catch (err) {
            console.error('[Sorteo Manual] Error al elegir turno del bot en manual:', err);
          }
          console.log('[Sorteo Manual] Finalizando coin flip');
          this.finalizarCoinFlip();
        }
      }
    } catch (e) {
      console.error('Error en sorteo 3D:', e);
      this.finalizarCoinFlip();
    }
    this.cdr.detectChanges();
  }

  clearHoveredCard() {
    if (this.isScrollingMode) return;

    this.hoveredCard = null;
    this.hoveredInPlayCard = null;
    this.hoveredCardStatuses = [];
    this.hoveredCardList = [];
    this.hoveredCardIndex = -1;
  }

  @HostListener('window:wheel', ['$event'])
  onScrollCard(event: WheelEvent) {
    if (
      this.hoveredCard &&
      this.hoveredCardList === this.partida?.jugador?.mano &&
      this.hoveredCardList.length > 1
    ) {
      event.preventDefault();

      const now = Date.now();

      this.isScrollingMode = true;
      if (this.scrollTimeout) clearTimeout(this.scrollTimeout);
      this.scrollTimeout = setTimeout(() => {
        this.isScrollingMode = false;
      }, 200);

      if (now - this.lastScrollTime < 120) return;
      this.lastScrollTime = now;

      if (event.deltaY > 0) {
        this.hoveredCardIndex = (this.hoveredCardIndex + 1) % this.hoveredCardList.length;
      } else if (event.deltaY < 0) {
        this.hoveredCardIndex =
          (this.hoveredCardIndex - 1 + this.hoveredCardList.length) % this.hoveredCardList.length;
      }

      const nextItem = this.hoveredCardList[this.hoveredCardIndex];
      const cartaReal: Card | BattleActionCard = 'card' in nextItem ? nextItem.card : nextItem;

      this.hoveredCard = cartaReal;
      this.hoveredInPlayCard = 'card' in nextItem ? nextItem as CartaEnJuego : null;
      this.hoveredCardStatuses = this.extraerGlosario(cartaReal);
    }
  }

  @HostListener('window:mousedown', ['$event'])
  onMiddleClick(event: MouseEvent) {
    if (
      event.button === 1 &&
      this.hoveredCard &&
      this.hoveredCardList === this.partida?.jugador?.mano
    ) {
      event.preventDefault();
      console.log('Click central: jugando carta', this.hoveredCard.nombre);
      this.jugarCarta(this.hoveredCard);
    }
  }

  showDebugPanel: boolean = false;
  fps: number = 0;
  memoryUsage: string = 'N/A';

  private frameCount = 0;
  private lastTime = performance.now();
  private animFrameId: number | null = null;

  @HostListener('window:keydown', ['$event'])
  onKeyDown(event: KeyboardEvent) {
    if (event.key === 'F3') {
      if (this.isSpectator) return;
      event.preventDefault();
      this.showDebugPanel = !this.showDebugPanel;

      if (this.showDebugPanel) {
        this.turnTimerEnabled = true;
        if (this.partida?.turnoActual === 'JUGADOR') {
          this.iniciarRelojTurno();
        }
        this.iniciarMedidorRendimiento();
      } else {
        this.turnTimerEnabled = false;
        this.detenerRelojTurno();
        this.tiempoRestante = this.tiempoTurnoMaximo;
        this.porcentajeTimer = 100;
        this.detenerMedidorRendimiento();
      }
      this.cdr.detectChanges();
    }
  }

  iniciarMedidorRendimiento() {
    const loop = () => {
      const now = performance.now();
      this.frameCount++;

      if (now >= this.lastTime + 1000) {
        this.fps = Math.round((this.frameCount * 1000) / (now - this.lastTime));
        this.frameCount = 0;
        this.lastTime = now;

        const mem = (performance as any).memory;
        if (mem) {
          this.memoryUsage = (mem.usedJSHeapSize / 1048576).toFixed(2) + ' MB';
        }

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

    if (wakeCheck) {
      console.log('Lanzando moneda de despertar...');

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
    if (this.isScrollingMode) return;

    if (!item) {
      this.clearHoveredCard();
      return;
    }
    const cartaReal = item.card ? item.card : item;
    this.hoveredCard = cartaReal;
    this.hoveredInPlayCard = item.card ? item as CartaEnJuego : null;
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
      tipoEfecto: 'damage',
    };

    this.cdr.detectChanges();
    await this.delay(600);

    let caras = 0;

    for (let i = 0; i < cantidadMonedas; i++) {
      this.coinFlipAtaque!.progreso = ((i + 1) / cantidadMonedas) * 100;
      this.cdr.detectChanges();

      await this.delay(600 + Math.random() * 400);

      const esCara = Math.random() < 0.5;
      if (esCara) caras++;

      this.coinFlipAtaque!.monedas[i].estado = esCara ? 'cara' : 'cruz';
      this.cdr.detectChanges();

      await this.delay(300);
    }

    const danioTotal = danioBase + caras * danioExtraPorCara;
    this.coinFlipAtaque!.danioTotal = danioTotal;
    this.coinFlipAtaque!.terminado = true;
    this.cdr.detectChanges();

    await this.delay(3000);

    this.coinFlipAtaque = null;
    this.cdr.detectChanges();

    return danioTotal;
  }

  detectarCoinFlipAtaque(ataque: any, activo?: CartaEnJuego | null): CoinFlipConfig | null {
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
      activo,
    );
  }

  async procesarEventosPostEstado(estadoAnterior: any, estadoNuevo: any): Promise<void> {
    const newEventId = estadoNuevo?.lastCoinFlipEventId || 0;
    if (newEventId > this.lastProcessedCoinFlipEventId) {
      this.lastProcessedCoinFlipEventId = newEventId;
      const actor = estadoNuevo.lastCoinFlipActor;
      const esMiAccion = actor === this.jugadorNombre || actor === 'JUGADOR';
      if (!esMiAccion || this.isSpectator) {
        const nombreAtaque = estadoNuevo.lastCoinFlipAttackName;
        if (nombreAtaque) {
          await this.reproducirCoinFlipAtaqueRemoto(nombreAtaque, actor, estadoNuevo);
        }
      }
    }

    const botActivoAntes = estadoAnterior?.bot?.activo;
    const botActivoAhora = estadoNuevo?.bot?.activo;
    if (botActivoAntes && !botActivoAhora) {
      await this.mostrarKOAnim();
    }

    const playerActivoAntes = estadoAnterior?.jugador?.activo;
    const playerActivoAhora = estadoNuevo?.jugador?.activo;
    if (playerActivoAntes && !playerActivoAhora) {
      await this.mostrarKOAnim();
    }

    const botCambioActivo =
      botActivoAntes &&
      botActivoAhora &&
      this.obtenerFirmaActivo(botActivoAntes) !== this.obtenerFirmaActivo(botActivoAhora);
    if (botCambioActivo) {
      this.activarEfectoCambio('bot');
    }

    const playerCambioActivo =
      playerActivoAntes &&
      playerActivoAhora &&
      this.obtenerFirmaActivo(playerActivoAntes) !== this.obtenerFirmaActivo(playerActivoAhora);
    if (playerCambioActivo) {
      this.activarEfectoCambio('jugador');
    }

    // Delay damage if attack or ability was used to allow banner to show first
    const logsDeTurno = estadoNuevo?.turnLogs || [];
    const huboAtaque = logsDeTurno.some((log: string) => log.startsWith('ATTACK_USED') || log.startsWith('ABILITY_USED'));
    if (huboAtaque) {
      await this.delay(1000);
    }

    const hpBotAntes = botActivoAntes?.hpActual ?? 0;
    const hpBotAhora = botActivoAhora?.hpActual ?? 0;
    if (botActivoAntes && botActivoAhora && hpBotAhora < hpBotAntes) {
      this.mostrarDamageNumber('bot', hpBotAntes - hpBotAhora);
    }

    if (botActivoAntes && botActivoAhora && hpBotAhora > hpBotAntes) {
      const curado = hpBotAhora - hpBotAntes;
      this.mostrarDamageNumber('bot', curado, true);
      this.mostrarCuracion('bot');
    }

    const hpPlayerAntes = playerActivoAntes?.hpActual ?? 0;
    const hpPlayerAhora = playerActivoAhora?.hpActual ?? 0;
    if (playerActivoAntes && playerActivoAhora && hpPlayerAhora < hpPlayerAntes) {
      this.mostrarDamageNumber('jugador', hpPlayerAntes - hpPlayerAhora);
    }

    if (playerActivoAntes && playerActivoAhora && hpPlayerAhora > hpPlayerAntes) {
      const curado = hpPlayerAhora - hpPlayerAntes;
      this.mostrarDamageNumber('jugador', curado, true);
      this.mostrarCuracion('jugador');
    }
  }

  private obtenerActivoPorActor(actor: string, estado: Partida): any {
    if (!actor || !estado) return null;
    if (actor === this.jugadorNombre || actor === 'JUGADOR') {
      return estado.jugador?.activo;
    }
    if (actor === 'BOT' || actor === this.nombreRival || actor === estado.botUsername) {
      return estado.bot?.activo;
    }
    if (actor === estado.jugadorUsername) {
      return estado.jugador?.activo;
    }
    if (actor === estado.botUsername) {
      return estado.bot?.activo;
    }
    return null;
  }

  private buscarAtaqueEnActivo(activo: any, nombreAtaque: string): any {
    if (!activo || !activo.card?.ataques?.length) return null;
    const normalized = (nombreAtaque || '').replace(':', '-').toLowerCase();
    let habilidad = activo.card.ataques.find(
      (a: any) =>
        (a.nombre || '').replace(':', '-').toLowerCase() === normalized ||
        (a.nombreOriginal || '').replace(':', '-').toLowerCase() === normalized
    );
    if (!habilidad) {
      habilidad = activo.card.ataques[0];
    }
    return habilidad;
  }

  private async reproducirCoinFlipAtaqueRemoto(
    nombreAtaque: string,
    actor: string,
    estadoFinal: Partida,
  ): Promise<void> {
    const activo = this.obtenerActivoPorActor(actor, estadoFinal);
    if (!activo) return;

    const habilidad = this.buscarAtaqueEnActivo(activo, nombreAtaque);
    if (!habilidad) return;

    const coinConfig = this.detectarCoinFlipAtaque(habilidad, activo);
    if (!coinConfig) return;

    const monedasServidor = estadoFinal.ultimasMonedasLanzadas?.length || 0;
    if (monedasServidor > 0) coinConfig.cantidadMonedas = monedasServidor;

    let carasReales = this.contarCarasServidor(estadoFinal);
    if (carasReales === null) {
      carasReales = 0;
    }

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

  debugFullCatalog: Card[] = [];
  debugFilteredCatalog: Card[] = [];
  debugSelectedIndex = 0;
  debugSearchText = '';
  debugSearchSupertype = '';

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

  actualizarFiltroTexto(value: string | Event) {
    const rawValue =
      typeof value === 'string' ? value : ((value.target as HTMLInputElement | null)?.value ?? '');
    this.debugSearchText = rawValue.toLowerCase();
    this.aplicarFiltrosDebug();
  }

  actualizarFiltroTipo(value: string | Event) {
    this.debugSearchSupertype =
      typeof value === 'string' ? value : ((value.target as HTMLSelectElement | null)?.value ?? '');
    this.aplicarFiltrosDebug();
  }

  aplicarFiltrosDebug() {
    this.debugFilteredCatalog = this.debugFullCatalog.filter((c) => {
      const matchTipo = !this.debugSearchSupertype || c.supertype === this.debugSearchSupertype;

      let matchTexto = true;
      if (this.debugSearchText) {
        const nombreMatch = c.nombre?.toLowerCase().includes(this.debugSearchText);

        const ataquesMatch = c.ataques?.some(
          (atk: any) =>
            atk.nombre?.toLowerCase().includes(this.debugSearchText) ||
            atk.texto?.toLowerCase().includes(this.debugSearchText),
        );

        matchTexto = nombreMatch || !!ataquesMatch;
      }

      return matchTipo && matchTexto;
    });

    this.debugSelectedIndex = 0;
  }

  get debugSelectedCard(): Card | null {
    if (!this.debugFilteredCatalog || this.debugFilteredCatalog.length === 0) return null;
    return this.debugFilteredCatalog[this.debugSelectedIndex];
  }

  nextDebugCard() {
    if (this.debugFilteredCatalog.length === 0) return;
    this.debugSelectedIndex = (this.debugSelectedIndex + 1) % this.debugFilteredCatalog.length;
  }

  prevDebugCard() {
    if (this.debugFilteredCatalog.length === 0) return;
    this.debugSelectedIndex =
      (this.debugSelectedIndex - 1 + this.debugFilteredCatalog.length) %
      this.debugFilteredCatalog.length;
  }

  private traducirEfectoCoinFlip(
    textoOriginal: string,
    monedas: number,
    danio: number,
    esMultiplicador: boolean,
    esFalloCruz: boolean,
    esSoloEstado: boolean,
  ): string {
    const numStr = monedas === 1 ? 'una moneda' : `${monedas} monedas`;

    if (textoOriginal.includes('prevent all effects of attacks')) {
      return `Lanza ${numStr}. Si sale CARA, este Pokemon queda protegido de todo efecto y dano de ataques durante el proximo turno rival.`;
    }
    if (textoOriginal.includes('prevent all damage done')) {
      return `Lanza ${numStr}. Si sale CARA, este Pokemon no recibe dano de ataques durante el proximo turno rival.`;
    }
    if (textoOriginal.includes('if tails') && textoOriginal.includes('damage to itself')) {
      const recoil = textoOriginal.match(/does (\d+) damage to itself/)?.[1] || '';
      return `Lanza ${numStr}. El ataque hace su dano normal. Si sale CRUZ, este Pokemon recibe ${recoil} de dano.`;
    }
    if (textoOriginal.includes('if heads') && textoOriginal.includes('discard') && textoOriginal.includes('energy')) {
      return `Lanza ${numStr}. Si sale CARA, descarta una Energia del Pokemon indicado.`;
    }
    if (textoOriginal.includes('if heads') && textoOriginal.includes('switch this pok')) {
      return `Lanza ${numStr}. Si sale CARA, cambia este Pokemon por uno de tu banca.`;
    }
    if (textoOriginal.includes('if heads') && textoOriginal.includes('search your deck')) {
      return `Lanza ${numStr}. Si sale CARA, busca la carta indicada en tu mazo.`;
    }
    if (textoOriginal.includes('if heads') && textoOriginal.includes('asleep') && textoOriginal.includes('if tails') && textoOriginal.includes('confused')) {
      return `Lanza ${numStr}. CARA duerme al rival; CRUZ lo confunde.`;
    }
    if (esSoloEstado) {
      if (textoOriginal.includes('paralyzed'))
        return `Lanzá ${numStr}. Si sale CARA, el rival queda paralizado.`;
      if (textoOriginal.includes('asleep'))
        return `Lanzá ${numStr}. Si sale CARA, el rival queda dormido.`;
      return `Lanza ${numStr} para resolver el efecto indicado en la carta.`;
    }

    if (esFalloCruz) return `Lanzá ${numStr}. Si sale CRUZ, el ataque falla.`;
    if (esMultiplicador) return `Lanzá ${numStr}. Hace ${danio} de daño por cada CARA.`;

    if (danio > 0) return `Lanza ${numStr}. Hace ${danio} de dano extra por cada CARA.`;
    return `Lanza ${numStr} para resolver el efecto indicado en la carta.`;
  }

  cargarEstado(): void {
    const online = this.esPartidaOnline();
    if (!this.matchId || this.cargandoAccion || (!online && (this.bloqueadoPorAnimacion || this.botEstaAtacando || this.botPensando))) return;
    this.battleService.getState(this.matchId).subscribe({
      next: (data) => {
        const dataOnline = this.esPartidaOnline(data);
        if (data?.faseActual === 'FIN_PARTIDA') {
          this.aplicarEstadoRefrescado(data);
          return;
        }
        if (data?.faseActual === 'LANZAMIENTO_MONEDA') {
          const yaReingresoAlSetup =
            this.partida?.faseActual === 'LANZAMIENTO_MONEDA' &&
            this.estadoCoinFlip !== 'OCULTO' &&
            !this.boardVisible;
          if (!yaReingresoAlSetup) {
            this.reingresarAFaseLanzamientoMoneda(data);
            return;
          }
        }
        if (!dataOnline && (this.bloqueadoPorAnimacion || this.botEstaAtacando)) {
          console.log('??? Polling interceptado.');
          return;
        }
        if (!data) return;
        const firmaNueva = this.crearFirmaPartida(data);
        if (firmaNueva === this.lastAppliedStateSignature) return;

        const hpServidorJugador = data.jugador?.activo?.hpActual || 0;

        if (!dataOnline && hpServidorJugador < this.hpRenderJugador) {
          this.datosPendientesBot = data;
          this.ejecutarIAEnemiga();
          return;
        }

        if (data.jugador?.mano) {
          this.detectarCartasNuevas(data.jugador.mano);
        }
        if (data.bot?.mano) {
          this.detectarCartasNuevasBot(data.bot.mano);
        }

        this.cdr.detectChanges();

        const turnoAnterior = this.partida?.turnoActual;
        const estadoAnterior = this.partida ? this.battleBoardState.clonarPartida(this.partida) : null;
        this.partida = data;
        this.lastAppliedStateSignature = firmaNueva;

        if (estadoAnterior) {
          this.procesarEventosPostEstado(estadoAnterior, data);
        }

        if (!this.isSpectator) {
          this.procesarFasesSetup(data);
        } else {
          this.estadoSetupMulligan = null;
        }

        this.procesarTurnLogs();

        if (!this.opponentLoaded && this.nombreRival) {
          this.cargarDatosOponente();
        }

        if (this.esperandoMiNuevoTurno && this.partida.turnoActual === 'JUGADOR') {
          this.esperandoMiNuevoTurno = false;
          this.iniciarRelojTurno();
        } else if (this.partida.turnoActual === 'BOT') {
          this.detenerRelojTurno();
        }

        if (turnoAnterior !== 'JUGADOR' && this.partida.turnoActual === 'JUGADOR') {
          this.iniciarRelojTurno();
        }
        else if (this.partida.turnoActual === 'BOT') {
          this.detenerRelojTurno();
        }

        if (!this.datosPendientesBot) {
          this.hpRenderJugador = hpServidorJugador;
        }

        this.cdr.detectChanges();
      },
    });
  }


  estadoSetupMulligan: 'REVEAL' | 'EXTRA_DRAW' | 'PLACE_ACTIVE' | 'PLACE_BENCH' | 'PLACE_BENCH_EXTRA' | 'PRIZES' | 'FINAL_REVEAL' | null = null;
  astonishReveal: {
    visible: boolean;
    cardId: string;
    cardNombre: string;
    esMiAccion: boolean;
    isFlipped: boolean;
  } | null = null;

  cardSplashActive: {
    visible: boolean;
    cardId: string;
    cardNombre: string;
    esMiAccion: boolean;
  } | null = null;

  attackBannerActive: {
    visible: boolean;
    attackName: string;
    esMiAccion: boolean;
  } | null = null;

  reviveAnimationActive: {
    visible: boolean;
    cardId: string;
    cardNombre: string;
    spriteUrl: string;
    hpProgress: number;
    card?: any;
  } | null = null;

  triggerAttackBanner(attackName: string, isOpponent: boolean) {
    this.attackBannerActive = {
      visible: true,
      attackName,
      esMiAccion: !isOpponent
    };
    this.cdr.detectChanges();
    setTimeout(() => {
      this.attackBannerActive = null;
      this.cdr.detectChanges();
    }, 900);
  }

  triggerCardSplash(cardName: string, isOpponent: boolean) {
    const term = cardName.trim().toLowerCase();
    const card = this.debugFullCatalog.find((c: any) => 
      (c.nombre && c.nombre.trim().toLowerCase() === term) || 
      (c.name && c.name.trim().toLowerCase() === term) ||
      (c.id && c.id.trim().toLowerCase() === term)
    ) || this.debugFilteredCatalog.find((c: any) => 
      (c.nombre && c.nombre.trim().toLowerCase() === term) || 
      (c.name && c.name.trim().toLowerCase() === term) ||
      (c.id && c.id.trim().toLowerCase() === term)
    );
    const cardId = card ? card.id : '';
    this.cardSplashActive = {
      visible: true,
      cardId,
      cardNombre: cardName,
      esMiAccion: !isOpponent
    };
    this.cdr.detectChanges();
    setTimeout(() => {
      this.cardSplashActive = null;
      this.cdr.detectChanges();
    }, 900);
  }

  cerrarAstonishReveal(): void {
    this.astonishReveal = null;
    this.cdr.detectChanges();
  }

  modoSeleccionUnionEnergia = false;
  energiaAUnir: any = null;

  iniciarModoUnionEnergia(cartaEnergia: any): void {
    if (this.cargandoAccion || this.partida?.turnoActual !== 'JUGADOR') return;
    if (this.modoSeleccionUnionEnergia && this.energiaAUnir?.id === cartaEnergia.id) {
      this.cancelarModoUnionEnergia();
      return;
    }
    this.modoSeleccionUnionEnergia = true;
    this.energiaAUnir = cartaEnergia;
    const msg = this.i18nService.translate('battle.selectTargetForEnergy');
    this.mostrarNotificacion(msg, 'info');
    this.cdr.detectChanges();
  }

  cancelarModoUnionEnergia(): void {
    this.modoSeleccionUnionEnergia = false;
    this.energiaAUnir = null;
    this.cdr.detectChanges();
  }

  async completarUnionEnergia(targetPokemon: any) {
    if (!targetPokemon || !this.energiaAUnir) return;
    this.cargandoAccion = true;
    this.modoSeleccionUnionEnergia = false;
    const energia = this.energiaAUnir;
    this.energiaAUnir = null;
    
    try {
      const nuevoEstado = await this.battleBoardAction.unirEnergiaYRecargar(
        this.matchId!,
        targetPokemon.card.id,
        energia.id
      );
      this.aplicarEstadoRefrescado(nuevoEstado);
      this.cargandoAccion = false;
      this.cdr.detectChanges();
    } catch (err: any) {
      this.cargandoAccion = false;
      console.error(err);
      this.mostrarNotificacion(err.error || this.i18n.translate('alert.cannotAttachEnergy'), 'error');
    }
  }

  clickEnActivo(): void {
    if (this.modoSeleccionUnionEnergia && this.energiaAUnir) {
      this.completarUnionEnergia(this.partida?.jugador?.activo);
    } else {
      this.intentarAbrirHabilidades();
    }
  }

  mulliganCartasRival: any[] = [];
  mulliganOponenteCount: number = 0;
  mulliganJugadorCount: number = 0;
  cartasExtraPermitidas: number = 0;
  setupAccionEnCurso = false;
  private setupAutoActionKey = '';
  private initialBotTurnInFlight = false;
  setupMulliganJugadorCartas: any[] = [];
  setupMulliganRivalCartas: any[] = [];
  setupMulliganJugadorDebeMostrar = false;
  setupMulliganRivalDebeMostrar = false;
  setupShuffleVisible = false;
  private setupMulliganRevealKey = '';

  async procesarFasesSetup(data: Partida) {
    if (data.faseActual === 'SETUP_INITIAL_DRAW') {
      if (!data.setupJugadorListo) {
        // Enviar evaluación
        this.ejecutarSetupUnaVez('evaluate', async () => {
          const estado = await firstValueFrom(this.battleService.evaluateSetup(this.matchId!));
          this.aplicarEstadoSetup(estado);
        });
      }
    } else if (data.faseActual === 'SETUP_MULLIGAN_REVEAL') {
      this.setEstadoSetup('REVEAL');
      const jugadorDebeMostrar = !this.tienePokemonBasicoEnLista(data.jugador?.mano || []);
      const rivalDebeMostrar = !this.tienePokemonBasicoEnLista(data.bot?.mano || []);
      const jugadorIds = (data.jugador?.mano || []).map((c: any) => c.id).join(',');
      const rivalIds = (data.bot?.mano || []).map((c: any) => c.id).join(',');
      const revealKey = `${data.mulligansJugador || 0}:${data.mulligansBot || 0}:${jugadorDebeMostrar}:${rivalDebeMostrar}:${jugadorIds}:${rivalIds}`;

      if (this.setupMulliganRevealKey !== revealKey) {
        this.setupMulliganRevealKey = revealKey;
        this.setupMulliganJugadorDebeMostrar = jugadorDebeMostrar;
        this.setupMulliganRivalDebeMostrar = rivalDebeMostrar;
        this.setupMulliganJugadorCartas = jugadorDebeMostrar ? (data.jugador?.mano || []) : [];
        this.setupMulliganRivalCartas = rivalDebeMostrar ? (data.bot?.mano || []) : [];
        this.mulliganCartasRival = this.setupMulliganRivalCartas;
        this.setupShuffleVisible = false;
      }
      this.cdr.detectChanges();

      if (!data.setupJugadorListo) {
        this.ejecutarSetupUnaVez(`mulligan-${data.mulligansJugador || 0}-${data.mulligansBot || 0}`, async () => {
          await this.delay(2300);
          this.setupShuffleVisible = true;
          this.cdr.detectChanges();
          await this.delay(1000);
          const estado = await firstValueFrom(this.battleService.executeMulligan(this.matchId!));
          this.estadoSetupMulligan = null;
          this.aplicarEstadoSetup(estado);
        });
      }
    } else if (data.faseActual === 'SETUP_PLACE_ACTIVE') {
      this.setEstadoSetup('PLACE_ACTIVE');
      this.cdr.detectChanges();
      this.ejecutarSetupBotSiHaceFalta(data);
    } else if (data.faseActual === 'SETUP_PLACE_BENCH') {
      this.setEstadoSetup('PLACE_BENCH');
      this.cdr.detectChanges();
      this.ejecutarSetupBotSiHaceFalta(data);
    } else if (data.faseActual === 'SETUP_MULLIGAN_EXTRA_DRAW') {
      this.mulliganJugadorCount = data.mulligansJugador || 0;
      this.mulliganOponenteCount = data.mulligansBot || 0;
      this.cartasExtraPermitidas = data.cartasMulliganExtraPendientesJugador || 0;

      if (!data.setupJugadorListo && this.cartasExtraPermitidas === 0) {
        this.estadoSetupMulligan = null;
        this.enviarCartasExtra(0);
        return;
      }
      if (this.cartasExtraPermitidas <= 0) {
        this.estadoSetupMulligan = null;
        this.cdr.detectChanges();
        return;
      }

      this.setEstadoSetup('EXTRA_DRAW');
      this.cdr.detectChanges();
    } else if (data.faseActual === 'SETUP_PLACE_BENCH_EXTRA') {
      const jugadorPuedeBancaExtra = !!data.setupJugadorRoboExtraMulligan;
      if (!jugadorPuedeBancaExtra) {
        this.estadoSetupMulligan = null;
        if (!data.setupJugadorListo) {
          this.confirmarBancaSetup();
        } else {
          this.ejecutarSetupBotSiHaceFalta(data);
        }
        return;
      }
      this.setEstadoSetup('PLACE_BENCH_EXTRA');
      this.cdr.detectChanges();
      this.ejecutarSetupBotSiHaceFalta(data);
      if (!data.setupJugadorListo && !this.tieneBasicosSetupDisponibles()) {
        this.confirmarBancaSetup();
      }
    } else if (data.faseActual === 'SETUP_PRIZE_PLACEMENT') {
      this.setEstadoSetup('PRIZES');
      this.ejecutarSetupBotSiHaceFalta(data);
      if (!data.setupJugadorListo) {
        this.ejecutarSetupUnaVez('prizes', async () => {
          await this.delay(1200);
          const estado = await firstValueFrom(this.battleService.placePrizes(this.matchId!));
          this.aplicarEstadoSetup(estado);
        });
      }
    } else if (data.faseActual === 'SETUP_REVEAL') {
      this.setEstadoSetup('FINAL_REVEAL');
      this.ejecutarSetupUnaVez('final-reveal', async () => {
        await this.delay(1400);
        const estado = await firstValueFrom(this.battleService.revealSetup(this.matchId!));
        this.estadoSetupMulligan = null;
        this.aplicarEstadoSetup(estado);
      });
    } else if (data.faseActual === 'TURNO_NORMAL') {
      this.estadoSetupMulligan = null;
      this.setupMulliganRevealKey = '';
      this.setupAutoActionKey = '';
      this.setupAccionEnCurso = false;
      
      // Mostrar overlay de Muerte Súbita justo al terminar el setup
      if (data.muerteSubita && !this.mostrarOverlayMuerteSubita && data.numeroTurno === 1) {
        this.mostrarOverlayMuerteSubita = true;
        this.cdr.detectChanges();
        setTimeout(() => {
          this.mostrarOverlayMuerteSubita = false;
          this.cdr.detectChanges();
        }, 4200);
      }

      this.iniciarPrimerTurnoBotSiHaceFalta(data);
    }
  }

  private iniciarPrimerTurnoBotSiHaceFalta(data: Partida): void {
    if (
      this.esPartidaOnline(data) ||
      data.turnoActual !== 'BOT' ||
      data.numeroTurno !== 1 ||
      this.initialBotTurnInFlight ||
      this.isSpectator ||
      !this.matchId
    ) {
      return;
    }

    this.initialBotTurnInFlight = true;
    this.bloqueadoPorAnimacion = true;
    this.cargandoAccion = true;
    this.cdr.detectChanges();

    this.iniciarTurnoBot(data)
      .catch((err) => console.error('No se pudo iniciar el primer turno del bot:', err))
      .finally(() => {
        this.initialBotTurnInFlight = false;
        this.cargandoAccion = false;
        this.bloqueadoPorAnimacion = false;
        this.cdr.detectChanges();
      });
  }

  enviarCartasExtra(cantidad: number) {
    if (this.setupAccionEnCurso) return;
    this.setupAccionEnCurso = true;
    this.estadoSetupMulligan = null;
    this.battleService.extraDraw(this.matchId!, cantidad).subscribe({
      next: (estado) => this.aplicarEstadoSetup(estado),
      error: (err) => {
        this.setupAccionEnCurso = false;
        this.mostrarNotificacion(err.error || 'No se pudieron resolver las cartas extra.', 'error');
      }
    });
  }

  get opcionesCartasExtra(): number[] {
    return Array.from({ length: this.cartasExtraPermitidas }, (_, i) => i + 1);
  }

  cartasBasicasSetupDisponibles(): any[] {
    return (this.partida?.jugador?.mano || []).filter(c => this.esPokemonBasicoSetup(c));
  }

  tieneBasicosSetupDisponibles(): boolean {
    return this.cartasBasicasSetupDisponibles().length > 0 && ((this.partida?.jugador?.banca?.length || 0) < 5);
  }

  puedeConfirmarBancaSetup(): boolean {
    return !!this.partida?.jugador?.activo;
  }

  async colocarActivoSetup(carta: any): Promise<void> {
    if (!this.matchId || this.setupAccionEnCurso) return;
    this.setupAccionEnCurso = true;
    try {
      const estado = await firstValueFrom(this.battleService.placeActiveSetup(this.matchId, carta.id));
      this.aplicarEstadoSetup(estado);
    } catch (err: any) {
      this.setupAccionEnCurso = false;
      this.mostrarNotificacion(err?.error || 'No se pudo colocar el Pokemon activo.', 'error');
    }
  }

  async colocarBancaSetup(carta: any): Promise<void> {
    if (!this.matchId || this.setupAccionEnCurso || !this.tieneBasicosSetupDisponibles()) return;
    this.setupAccionEnCurso = true;
    try {
      const estado = await firstValueFrom(this.battleService.placeBenchSetup(this.matchId, carta.id));
      this.aplicarEstadoSetup(estado);
    } catch (err: any) {
      this.setupAccionEnCurso = false;
      this.mostrarNotificacion(err?.error || 'No se pudo colocar ese Pokemon en banca.', 'error');
    }
  }

  async confirmarBancaSetup(): Promise<void> {
    if (!this.matchId || this.setupAccionEnCurso || !this.puedeConfirmarBancaSetup()) return;
    this.setupAccionEnCurso = true;
    try {
      const estado = await firstValueFrom(this.battleService.confirmBenchSetup(this.matchId));
      this.aplicarEstadoSetup(estado);
    } catch (err: any) {
      this.setupAccionEnCurso = false;
      this.mostrarNotificacion(err?.error || 'No se pudo confirmar la banca.', 'error');
    }
  }

  private aplicarEstadoSetup(estado: Partida): void {
    this.partida = estado;
    this.lastAppliedStateSignature = this.crearFirmaPartida(estado);
    this.setupAccionEnCurso = false;
    this.cdr.detectChanges();
    this.procesarFasesSetup(estado);
  }

  private ejecutarSetupUnaVez(key: string, accion: () => Promise<void>): void {
    if (this.setupAccionEnCurso || this.setupAutoActionKey === key) return;
    this.setupAutoActionKey = key;
    this.setupAccionEnCurso = true;
    accion().catch(err => {
      this.setupAccionEnCurso = false;
      console.error('Error en setup', err);
    });
  }

  private ejecutarSetupBotSiHaceFalta(data: Partida): void {
    if (this.esPartidaOnline(data) || data.setupBotListo || !this.matchId) return;
    const key = `bot-${data.faseActual}-${data.setupJugadorListo}-${data.bot?.mano?.length || 0}-${data.bot?.banca?.length || 0}`;
    this.ejecutarSetupUnaVez(key, async () => {
      await this.delay(500);
      const estado = await firstValueFrom(this.battleService.jugarBotSetup(this.matchId!));
      this.aplicarEstadoSetup(estado);
    });
  }

  private esPokemonBasicoSetup(carta: any): boolean {
    if (!this.esPokemon(carta)) return false;
    return (carta?.subtypes || []).some((s: string) => String(s).toLowerCase() === 'basic');
  }

  private tienePokemonBasicoEnLista(cartas: any[]): boolean {
    return (cartas || []).some(c => this.esPokemonBasicoSetup(c));
  }

  private setEstadoSetup(estado: typeof this.estadoSetupMulligan): void {
    if (this.estadoSetupMulligan !== estado) {
      this.estadoSetupMulligan = estado;
      if (estado !== 'REVEAL') {
        this.setupMulliganRevealKey = '';
      }
    }
  }

  private handleGameEnd(partida: Partida): void {
    if (this.showEndGameOverlay) return;
    if (this.pendingEndGameTimeout) {
      clearTimeout(this.pendingEndGameTimeout);
      this.pendingEndGameTimeout = null;
    }

    this.partida = partida;
    this.lastAppliedStateSignature = this.crearFirmaPartida(partida);
    this.endGameWinner = partida.ganador || 'BOT';
    this.isVictory = this.endGameWinner === this.jugadorNombre;
    this.endGameReason = partida.razonFinPartida || this.buildFallbackEndGameReason();
    const online = this.esPartidaOnline(partida);
    this.coinsEarned = this.isVictory ? (online ? 100 : 50) : (online ? 20 : 10);
    this.showEndGameOverlay = true;
    this.boardVisible = true;
    this.estadoCoinFlip = 'OCULTO';
    this.cargandoAccion = false;
    this.bloqueadoPorAnimacion = false;
    this.botPensando = false;
    this.botEstaAtacando = false;

    if (this.pollingPartida) {
      clearInterval(this.pollingPartida);
      this.pollingPartida = null;
    }
    this.detenerRelojTurno();

    this.awardEndGameCoins();
    this.cdr.detectChanges();
  }

  surrenderBattle(): void {
    if (!this.matchId || this.showEndGameOverlay || this.cargandoAccion) return;

    const confirmar = window.confirm(this.i18n.translate('confirm.surrender'));
    if (!confirmar) return;

    this.cargandoAccion = true;
    this.battleService.surrender(this.matchId).subscribe({
      next: (partida) => {
        if (partida?.faseActual === 'FIN_PARTIDA') {
          this.handleGameEnd(partida);
          return;
        }
        this.partida = partida;
        this.cargandoAccion = false;
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error('Error al rendirse', err);
        this.cargandoAccion = false;
        this.mostrarNotificacion(err?.error || this.i18n.translate('alert.cannotSurrender'), 'error');
        this.cdr.detectChanges();
      }
    });
  }

  private buildFallbackEndGameReason(): string {
    return this.isVictory
      ? 'Ganaste por condicion de victoria del tablero.'
      : 'Perdiste por condicion de victoria del rival.';
  }

  private awardEndGameCoins(): void {
    if (this.endGameRewardSent || !this.jugadorNombre || this.coinsEarned <= 0) return;
    this.endGameRewardSent = true;
    this.jugadorService.rewardCoins(this.jugadorNombre, this.coinsEarned).subscribe({
      error: (err) => console.error('Error acreditando SantoroPoints', err)
    });
  }

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

  getCardFanTransform(index: number, total: number): string {
    const maxAngle = Math.min(2.8 * total, 26);
    const angleStep = total > 1 ? (maxAngle * 2) / (total - 1) : 0;
    const angle = total > 1 ? -maxAngle + angleStep * index : 0;
    const cardSpacing = Math.max(30, Math.min(72, 440 / Math.max(total - 1, 1)));
    const totalWidth = cardSpacing * (total - 1);
    const offsetX = -totalWidth / 2 + cardSpacing * index;
    const normalizedPos = total > 1 ? (index / (total - 1)) * 2 - 1 : 0;
    const curveDepth = total <= 7 ? 18 : total <= 10 ? 11 : 7;
    const baseLift = total <= 7 ? 0 : total <= 10 ? -6 : -10;
    const offsetY = normalizedPos * normalizedPos * curveDepth + baseLift;
    return `translateX(calc(-50% + ${offsetX}px)) translateY(${offsetY}px) rotate(${angle}deg)`;
  }

  iniciarPolling(): void {
    if (this.pollingPartida) clearInterval(this.pollingPartida);
    this.pollingPartida = setInterval(() => {
      if (this.esPartidaOnline()) {
        this.cargarEstado();
        return;
      }

      if (this.partida?.turnoActual === 'BOT' && !this.bloqueadoPorAnimacion) {
        this.cargarEstado();
      }
    }, 1200);
  }

  private mostrarTurnOverlay(turno: 'jugador' | 'bot'): void {
    this.turnoOverlayTipo = turno;
    this.showTurnOverlay = true;
    this.cdr.detectChanges();
    setTimeout(() => {
      this.showTurnOverlay = false;
      this.cdr.detectChanges();
      if (turno === 'bot' && !this.esPartidaOnline()) {
        setTimeout(() => this.ejecutarIAEnemiga(), 1200);
      }
    }, 2000);
  }

  procesarCambioDeTurnoDramatico(dataServidor: any) {
    this.bloqueadoPorAnimacion = true;
    this.partida = dataServidor;
    this.lastAppliedStateSignature = this.crearFirmaPartida(dataServidor);
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

  ejecutarIAEnemiga() {
    if (this.datosPendientesBot) {
      const data = this.datosPendientesBot;
      this.datosPendientesBot = null;
      this.ejecutarIAEnemigaConData(data);
    }
  }

  async ejecutarIAEnemigaConData(estadoFinal: any) {
    this.botPensando = true;
    this.cdr.detectChanges();
    await this.delay(2500); // Artificial delay to simulate thinking
    this.botPensando = false;
    this.cdr.detectChanges();

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

    if (wakeCheckBot) {
      console.log('Checkup del bot. ¿Se despierta?', wakeCheckBot.seDesperto);
      await this.reproducirChequeoDespertar('¿Se despierta el rival?', wakeCheckBot.seDesperto);
    }

    const { botAtaco, hpJugadorDespues, danioHecho } = analisisBot;

    // Detectar si el bot atacó realmente leyendo los logs nuevos de este turno
    const startLogIdx = estadoAntiguo?.turnLogs?.length || 0;
    let botAtacoReal = false;
    let nombreAtaqueBot = '';
    if (estadoFinal.turnLogs) {
      for (let i = startLogIdx; i < estadoFinal.turnLogs.length; i++) {
        const log = estadoFinal.turnLogs[i];
        if (log.startsWith('ATTACK_USED:BOT:')) {
          botAtacoReal = true;
          const parts = log.split(':');
          if (parts.length >= 3) {
            nombreAtaqueBot = parts[2];
          }
        }
      }
    }

    const activoBotDespues = estadoFinal?.bot?.activo;

    await this.verificarEstadosCurados(estadoAntiguo, estadoFinal);

    if (!botAtacoReal) {
      this.mostrarNotificacion('El rival pasó su turno.', 'info');
      this.aplicarEstadoRefrescado(estadoFinal);
      if (this.showEndGameOverlay) return;

      this.limpiarBanderasBot();

      if (this.partida?.turnoActual === 'JUGADOR') {
        this.iniciarRelojTurno();
      }
      this.cdr.detectChanges();
      return;
    }

    this.botEstaAtacando = true;
    this.animandoBotAtaque = true;
    this.triggerTrainerAttack('opponent');
    this.cdr.detectChanges();

    if (activoBotDespues && activoBotDespues.card?.ataques?.length > 0) {
      let habilidadBot = null;
      if (nombreAtaqueBot) {
        const normalizedLogName = nombreAtaqueBot.replace(':', '-').toLowerCase();
        habilidadBot = activoBotDespues.card.ataques.find(
          (a: any) => 
            (a.nombre || '').replace(':', '-').toLowerCase() === normalizedLogName ||
            (a.nombreOriginal || '').replace(':', '-').toLowerCase() === normalizedLogName
        );
      }
      if (!habilidadBot) {
        habilidadBot = activoBotDespues.card.ataques[0];
      }

      const coinConfig = this.detectarCoinFlipAtaque(habilidadBot, activoBotDespues);

      if (coinConfig) {
        const monedasServidor = estadoFinal.ultimasMonedasLanzadas?.length || 0;
        if (monedasServidor > 0) coinConfig.cantidadMonedas = monedasServidor;
        let carasReales = this.contarCarasServidor(estadoFinal);
        if (carasReales === null) {
          if (coinConfig.tipoEfecto === 'self-damage') {
            const hpPropioAntes = this.partida?.bot?.activo?.hpActual || 0;
            const hpPropioDespues = estadoFinal.bot?.activo?.hpActual || 0;
            const autodanioHecho = hpPropioAntes - hpPropioDespues;
            carasReales = autodanioHecho > 0 ? 0 : 1;
          } else {
            carasReales = this.battleBoardTurn.resolverCarasBot(
              coinConfig,
              habilidadBot,
              estadoFinal,
              danioHecho,
            );
          }
        }
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

    this.showImpactFlash = true;
    this.aplicarEstadoRefrescado(estadoFinal);
    if (this.showEndGameOverlay) return;
    this.cdr.detectChanges();

    await this.delay(150);
    this.showImpactFlash = false;
    this.cdr.detectChanges();

    await this.delay(600);
    this.limpiarBanderasBot();

    if (this.partida?.turnoActual === 'JUGADOR') {
      console.log('? Turno del bot finalizado. Iniciando reloj de jugador.');
      this.iniciarRelojTurno();
    }
    this.cdr.detectChanges();
  }

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
      this.lastAppliedStateSignature = this.crearFirmaPartida(this.partida);
      this.cdr.detectChanges();
      console.log('Tablero recargado manualmente.');
    } catch (error) {
      console.error('Error recargando tablero en modo debug:', error);
    }
  }

  async debugRobarCarta(cardId: string) {
    if (!cardId) return;
    try {
      console.log(`GOD MODE: inyectando carta ${cardId} a la mano...`);
      await firstValueFrom(this.battleService.debugDrawCard(this.matchId!, cardId));
      await this.refrescarTableroDebug();
    } catch (err) {
      console.error('Error en God Mode (Robar Carta):', err);
      this.mostrarNotificacion(this.i18n.translate('alert.godModeFail'), 'error');
    }
  }

  async debugForzarEstado(objetivo: 'JUGADOR' | 'BOT', estado: string) {
    try {
      console.log(`GOD MODE: forzando estado ${estado} a ${objetivo}...`);
      await firstValueFrom(this.battleService.debugForzarEstado(this.matchId!, objetivo, estado));
      await this.refrescarTableroDebug();
    } catch (err) {
      console.error('Error en God Mode (Forzar Estado):', err);
    }
  }

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

    await this.delay(2000);

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

    const condViejoJugador = estadoAnterior.jugador?.activo?.condicionesEspeciales || [];
    const condNuevoJugador = estadoNuevo.jugador?.activo?.condicionesEspeciales || [];

    // CURADOS JUGADOR
    if (condViejoJugador.includes('Paralyzed') && !condNuevoJugador.includes('Paralyzed')) {
      this.mostrarNotificacion('¡Tu Pokémon ya no está paralizado!', 'success');
      await this.animarCuraEstado('jugador', 'Paralyzed', '¡Parálisis curada!');
    }
    if (condViejoJugador.includes('Asleep') && !condNuevoJugador.includes('Asleep')) {
      this.mostrarNotificacion('¡Tu Pokémon se despertó!', 'success');
      await this.animarCuraEstado('jugador', 'Asleep', '¡Se despertó!');
    }
    if (condViejoJugador.includes('Poisoned') && !condNuevoJugador.includes('Poisoned')) {
      this.mostrarNotificacion('¡Tu Pokémon se curó del veneno!', 'success');
    }

    // NUEVOS ESTADOS JUGADOR
    if (!condViejoJugador.includes('Paralyzed') && condNuevoJugador.includes('Paralyzed')) {
      this.mostrarNotificacion('¡Tu Pokémon fue paralizado!', 'error');
    }
    if (!condViejoJugador.includes('Asleep') && condNuevoJugador.includes('Asleep')) {
      this.mostrarNotificacion('¡Tu Pokémon se quedó dormido!', 'error');
    }
    if (!condViejoJugador.includes('Poisoned') && condNuevoJugador.includes('Poisoned')) {
      this.mostrarNotificacion('¡Tu Pokémon fue envenenado!', 'error');
    }
    if (!condViejoJugador.includes('Confused') && condNuevoJugador.includes('Confused')) {
      this.mostrarNotificacion('¡Tu Pokémon está confundido!', 'error');
    }

    const condViejoBot = estadoAnterior.bot?.activo?.condicionesEspeciales || [];
    const condNuevoBot = estadoNuevo.bot?.activo?.condicionesEspeciales || [];

    // CURADOS BOT
    if (condViejoBot.includes('Paralyzed') && !condNuevoBot.includes('Paralyzed')) {
      this.mostrarNotificacion('El Pokémon rival se curó de la parálisis.', 'info');
      await this.animarCuraEstado('bot', 'Paralyzed', '¡Parálisis curada!');
    }
    if (condViejoBot.includes('Asleep') && !condNuevoBot.includes('Asleep')) {
      this.mostrarNotificacion('El Pokémon rival se despertó.', 'info');
      await this.animarCuraEstado('bot', 'Asleep', '¡Se despertó!');
    }

    // NUEVOS ESTADOS BOT
    if (!condViejoBot.includes('Paralyzed') && condNuevoBot.includes('Paralyzed')) {
      this.mostrarNotificacion('¡El Pokémon rival fue paralizado!', 'success');
    }
    if (!condViejoBot.includes('Asleep') && condNuevoBot.includes('Asleep')) {
      this.mostrarNotificacion('¡El Pokémon rival se durmió!', 'success');
    }
    if (!condViejoBot.includes('Poisoned') && condNuevoBot.includes('Poisoned')) {
      this.mostrarNotificacion('¡El Pokémon rival fue envenenado!', 'success');
    }
    if (!condViejoBot.includes('Confused') && condNuevoBot.includes('Confused')) {
      this.mostrarNotificacion('¡El Pokémon rival está confundido!', 'success');
    }
  }

  forzarUpdate() {
    this.battleService.getState(this.matchId!).subscribe({
      next: async (data) => {
        if (!data) return;
        if (data.faseActual === 'FIN_PARTIDA') {
          this.handleGameEnd(data);
          return;
        }
        if (this.partida?.turnoActual === 'JUGADOR' && data.turnoActual === 'BOT') {
          this.bloqueadoPorAnimacion = true;
          const miHp = this.hpRenderJugador;
          this.partida = data;
          this.lastAppliedStateSignature = this.crearFirmaPartida(data);
          this.hpRenderJugador = miHp;
          this.cdr.detectChanges();
          await new Promise((f) => setTimeout(f, 1200));
          this.turnoOverlayTipo = 'bot';
          this.showTurnOverlay = true;
          this.cdr.detectChanges();
          await new Promise((f) => setTimeout(f, 2000));
          this.showTurnOverlay = false;
          this.cdr.detectChanges();
          if (this.esPartidaOnline(data)) {
            this.partida = data;
            this.lastAppliedStateSignature = this.crearFirmaPartida(data);
            this.hpRenderJugador = data.jugador?.activo?.hpActual || 0;
            this.bloqueadoPorAnimacion = false;
            this.cdr.detectChanges();
          } else {
            this.ejecutarIAEnemigaConData(data);
          }
          return;
        }
        this.partida = data;
        this.lastAppliedStateSignature = this.crearFirmaPartida(data);
        this.hpRenderJugador = data.jugador?.activo?.hpActual || 0;
        this.cdr.detectChanges();
      },
    });
  }

  async ejecutarAtaqueSecuencia(nombreAtaque: string) {
    if (this.cargandoAccion || !nombreAtaque) return;

    const activoJugador = this.partida?.jugador?.activo;
    if (!activoJugador) return;

    // Buscar por nombre visible (puede estar traducido) o por nombreOriginal
    const habilidad = (activoJugador.card.ataques ?? []).find(
      (a: any) => a.nombre === nombreAtaque || a.nombreOriginal === nombreAtaque,
    );

    // Usar el nombre original (inglés) para comunicarse con el backend
    const nombreParaBackend = habilidad?.nombreOriginal || nombreAtaque;

    if (nombreParaBackend === 'Conversion Powder') {
      this.mostrarConversionModal = true;
      return;
    }

    this.bloqueadoPorAnimacion = true;
    this.cargandoAccion = true;
    this.ataqueRealizado = true;

    if (!habilidad) {
      this.bloqueadoPorAnimacion = false;
      this.cargandoAccion = false;
      this.ataqueRealizado = false;
      this.mostrarNotificacion(this.i18n.translate('alert.attackNotFound'), 'error');
      return;
    }

    const tipoEnergia = habilidad.costo?.[0] || 'Colorless';

    try {
      const ataqueBloqueadoPorConfusion = await this.resolverConfusionPreAtaque(activoJugador);
      if (ataqueBloqueadoPorConfusion) {
        return;
      }

      const estadoFinal = await this.battleBoardCombat.atacarYRecargar(this.matchId!, nombreParaBackend);
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

  async seleccionarEstadoConversion(estado: string): Promise<void> {
    this.mostrarConversionModal = false;
    if (!this.matchId) return;
    
    this.bloqueadoPorAnimacion = true;
    this.cargandoAccion = true;
    this.ataqueRealizado = true;
    
    try {
      // Usar la función de ataque de battle.service directamente pasando el param, 
      // y luego recargar el estado como en atacarYRecargar.
      // Se usa el nombre en inglés para el backend.
      await firstValueFrom(this.battleService.atacar(this.matchId, 'Conversion Powder', estado));
      const estadoFinal = await firstValueFrom(this.battleService.getState(this.matchId));
      
      const activoJugador = this.partida?.jugador?.activo;
      const habilidad = (activoJugador?.card.ataques ?? []).find(
        (a: any) => a.nombreOriginal === 'Conversion Powder' || a.nombre === 'Conversion Powder'
      );
      const tipoEnergia = habilidad?.costo?.[0] || 'Grass';
      
      await this.reproducirCoinFlipAtaqueJugador(habilidad, estadoFinal);
      await this.reproducirImpactoAtaqueJugador(tipoEnergia, estadoFinal);
      await this.finalizarSecuenciaAtaque(estadoFinal);
    } catch (error: any) {
      this.cargandoAccion = false;
      this.ataqueRealizado = false;
      this.bloqueadoPorAnimacion = false;
      console.error('Error al atacar (Conversion):', error);
      this.mostrarNotificacion(error.error?.message || error.message || 'Error al atacar', 'error');
    }
  }

  cerrarConversionModal(): void {
    this.mostrarConversionModal = false;
  }

  async iniciarTurnoBot(estadoFinal: any) {
    await this.delay(1000);
    this.cargandoAccion = false;
    await this.mostrarOverlayTurnoBot();

    if (this.partida?.botUsername) {
      this.bloqueadoPorAnimacion = false;
      this.cdr.detectChanges();
      return;
    }

    try {
      // Simula que el bot está pensando
      await this.delay(2000);
      const estadoPostBot = await this.battleBoardCombat.ejecutarTurnoBot(this.matchId!);
      this.ejecutarIAEnemigaConData(estadoPostBot);
    } catch (err: any) {
      console.error('Error al ejecutar bot:', err);
      this.bloqueadoPorAnimacion = false;
    }
  }

  async pasarTurno(): Promise<void> {
    if (this.isSpectator) return;
    console.log("?? Botón 'Pasar Turno' presionado.");

    if (this.partida?.turnoActual !== 'JUGADOR') {
      console.warn('? Bloqueado: No es tu turno.');
      return;
    }
    if (this.cargandoAccion) {
      console.warn('? Bloqueado: Hay una acción en curso.');
      return;
    }

    this.cargandoAccion = true;
    this.bloqueadoPorAnimacion = true;
    this.detenerRelojTurno();

    this.tiempoRestante = this.tiempoTurnoMaximo;
    this.porcentajeTimer = 100;
    this.esperandoMiNuevoTurno = true;
    this.cdr.detectChanges();

    const estadoAntiguo = this.battleBoardState.clonarPartida(this.partida);

    try {
      console.log('? Enviando fin de turno al servidor (Java)...');
      const estadoFinal = await this.battleBoardCombat.pasarTurnoYRecargar(this.matchId!);
      console.log('?? Datos de Checkup recibidos del server.');
      await this.procesarPostPassTurn(estadoAntiguo, estadoFinal);
    } catch (error: any) {
      this.cargandoAccion = false;
      this.bloqueadoPorAnimacion = false;
      console.error('? Error del servidor al pasar turno:', error);
      this.mostrarNotificacion(this.i18n.translate('alert.connectionError', { error: error?.error ?? (this.i18n.currentLanguage() === 'es' ? 'El servidor no responde' : 'The server does not respond') }), 'error');
    }
  }

  realizarAccion(habilidad: BattleBoardAttack): void {
    this.showHabilidadesPanel = false;
    if ((this.partida?.numeroTurno || 1) <= 1) {
      this.mostrarNotificacion('No se puede atacar en el primer turno.', 'warning');
      return;
    }
    if (!this.validarEnergiaAtaque(habilidad)) {
      this.mostrarNotificacion(this.i18n.translate('alert.notEnoughEnergyForAttack', { attack: habilidad.nombre }), 'warning');
      return;
    }
    // Pasar el nombre (puede ser traducido); ejecutarAtaqueSecuencia resolverá el nombreOriginal
    this.ejecutarAtaqueSecuencia(habilidad.nombre);
  }

  async animarMonedasSincronizadas(
    nombreAtaque: string,
    config: CoinFlipConfig,
    carasForzadas: number,
    esSoloEstado: boolean,
  ): Promise<void> {
    this.resultadoMoneda = '';
    this.carasAcumuladas = 0;
    this.danioAcumulado = config.danioBase;
    this.screenShaking = false;

    this.coinFlipAtaque = {
      nombreAtaque,
      descripcion: config.descripcion,
      parentesisDetalle: (config as any).parentesisDetalle,
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
      tipoEfecto: config.tipoEfecto,
    };
    this.cdr.detectChanges();
    await this.delay(120);

    let carasAsignadas = 0;
    for (let i = 0; i < config.cantidadMonedas; i++) {
      this.coinFlipAtaque!.progreso = ((i + 1) / config.cantidadMonedas) * 100;

      await this.delay(600 + Math.random() * 200);

      const esCara = this.battleBoardTurn.resolverSiguienteMoneda(
        carasForzadas,
        carasAsignadas,
        config.cantidadMonedas,
        i,
      );

      if (esCara) {
        carasAsignadas++;
        this.carasAcumuladas = carasAsignadas;
        this.danioAcumulado = config.danioBase + (carasAsignadas * config.danioExtraPorCara);
      }

      this.coinFlipAtaque!.monedas[i].estado = esCara ? 'cara' : 'cruz';

      if (config.cantidadMonedas === 1) {
        this.resultadoMoneda = esCara ? 'CARA' : 'CRUZ';
      }

      this.cdr.detectChanges();
      await this.delay(400);
    }

    if (config.cantidadMonedas > 1) {
      this.resultadoMoneda = this.battleBoardTurn.obtenerResultadoMoneda(
        config.cantidadMonedas,
        carasAsignadas,
      );
    }

    this.coinFlipAtaque!.danioTotal = this.battleBoardTurn.calcularDanioMonedas(
      config,
      carasAsignadas,
    );
    this.coinFlipAtaque!.terminado = true;

    // Vibrar pantalla si el daño total es alto (>= 80)
    if (this.coinFlipAtaque!.danioTotal >= 80) {
      this.screenShaking = true;
      window.setTimeout(() => {
        this.screenShaking = false;
        this.cdr.detectChanges();
      }, 850);
    }

    this.cdr.detectChanges();

    await this.delay(3000);

    this.coinFlipAtaque = null;
    this.carasAcumuladas = 0;
    this.danioAcumulado = 0;
    this.cdr.detectChanges();
  }

  get coinFlipAttacker(): CartaEnJuego | null {
    if (!this.partida) return null;
    return this.partida.turnoActual === 'BOT' ? this.partida.bot?.activo : this.partida.jugador?.activo;
  }

  getCoinFlipThemeIcon(): string {
    const effect = this.coinFlipAtaque?.tipoEfecto;
    if (effect === 'protection') return '🛡️';
    if (effect === 'discard') return '⚡';
    if (effect === 'status') return '✨';
    if (effect === 'search') return '🔎';
    if (effect === 'self-damage') return '💥';
    if (effect === 'switch') return '↔';
    if ((this.coinFlipAtaque?.nombreAtaque || '').toLowerCase().includes('seafaring')) return '🌊';
    return '🪙';
  }

  getCoinFlipThemeClass(): string {
    return `acf-theme-${this.coinFlipAtaque?.tipoEfecto || 'other'}`;
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
    if (this.isSpectator) return;
    if (!this.partida || this.partida.turnoActual !== 'JUGADOR' || this.partida.faseActual !== 'TURNO_NORMAL' || this.cargandoAccion) return;

    const decision = this.battleBoardAction.resolverAccionCarta(this.partida, carta);

    switch (decision.tipo) {
      case 'unir-energia':
        this.iniciarModoUnionEnergia(carta);
        return;
      case 'evolucionar':
        if (decision.target) {
          await this.ejecutarEvolucionVisual(carta, decision.target);
        }
        return;
      case 'requiere-promocion':
        if (decision.mensaje) this.mostrarNotificacion(decision.mensaje, 'info');
        return;
      case 'bajar-pokemon':
        this.gestionarBajadaPokemon(carta);
        return;
      case 'jugar-trainer':
        await this.ejecutarJugarTrainer(carta);
        return;
      default:
        return;
    }
  }

  async ejecutarEvolucionVisual(cartaEvolucion: any, target: any) {
    this.bloqueadoPorAnimacion = true;
    this.cargandoAccion = true;

    try {
      const estadoFinal = await this.battleBoardAction.evolucionarYRecargar(
        this.matchId!,
        cartaEvolucion.id,
        target.card.id,
      );

      this.animandoEvolucionId = target.card.id;
      this.cdr.detectChanges();

      await this.delay(600);

      this.aplicarEstadoRefrescado(estadoFinal);
      this.cdr.detectChanges();

      await this.delay(600);

      this.animandoEvolucionId = null;
      this.bloqueadoPorAnimacion = false;
      this.cargandoAccion = false;
      this.cdr.detectChanges();
    } catch (error: any) {
      this.bloqueadoPorAnimacion = false;
      this.cargandoAccion = false;
      this.mostrarNotificacion(this.i18n.translate('alert.evolutionError', { error: error.error || error.message }), 'error');
    }
  }

  async seleccionarBanca(p: any) {
    if (this.modoSeleccionUnionEnergia && this.energiaAUnir) {
      this.completarUnionEnergia(p);
      return;
    }
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
        this.mostrarNotificacion(err.error || this.i18n.translate('alert.retreatError'), 'error');
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
      } catch (err: any) {
        this.cargandoAccion = false;
        console.error(err);
        this.mostrarNotificacion(this.i18n.translate('alert.activeError'), 'error');
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
    if (confirm(this.battleBoardAction.construirMensajeRetirada(activoJugador, this.partida?.activeStadium))) {
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
        this.mostrarNotificacion(err.error || this.i18n.translate('alert.notEnoughEnergyForRetreat'), 'error');
      }
    }
  }

  soltarCarta(event: CdkDragDrop<any[]>, zona: 'activo' | 'banca'): void {
    if (event.previousContainer.id !== this.handDropListId) return;
    if (event.previousContainer === event.container) return;
    const cartaArrastrada = event.item.data;
    if (this.esEnergia(cartaArrastrada)) {
      if (zona !== 'activo') {
        this.mostrarNotificacion(this.i18n.translate('alert.energyAttachActiveOnly'), 'warning');
        return;
      }
      this.gestionarUnionEnergia(cartaArrastrada);
    } else if (this.esPokemon(cartaArrastrada)) {
      this.jugarCarta(cartaArrastrada);
    } else if (this.esTrainer(cartaArrastrada)) {
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
        this.mostrarNotificacion(err.error || this.i18n.translate('alert.cannotPlayPokemon'), 'error');
      });
  }

  private async ejecutarJugarTrainer(carta: any): Promise<void> {
    if (this.cargandoAccion) return;
    this.cargandoAccion = true;
    try {
      const nuevoEstado = await this.battleBoardAction.jugarTrainerYRecargar(this.matchId!, carta.id);
      this.aplicarEstadoRefrescado(nuevoEstado);
      this.cargandoAccion = false;
      this.cdr.detectChanges();
    } catch (err: any) {
      this.cargandoAccion = false;
      console.error(err);
      this.mostrarNotificacion(err.error || 'No se pudo jugar la carta de Entrenador.', 'error');
    }
  }

  private gestionarUnionEnergia(cartaEnergia: any): void {
    const activoJugador = this.partida?.jugador?.activo;
    if (!activoJugador) {
      this.mostrarNotificacion(this.i18n.translate('alert.needActivePokemon'), 'warning');
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
        this.mostrarNotificacion(this.i18n.translate('alert.cannotAttachEnergy'), 'error');
      });
  }

  puedePagarRetiro(): boolean {
    return this.battleBoardAction.puedePagarRetiro(this.partida?.jugador?.activo, this.partida?.activeStadium);
  }

  puedeAtacar(): boolean {
    return !!(
      this.partida?.jugador?.activo &&
      this.partida?.bot?.activo &&
      this.partida?.turnoActual === 'JUGADOR' &&
      (this.partida?.numeroTurno || 1) > 1 &&
      !this.ataqueRealizado &&
      !this.partida?.jugador?.activo?.bocaAbajo &&
      !this.partida?.bot?.activo?.bocaAbajo &&
      (this.partida?.jugador?.activo?.energiasUnidas?.length ?? 0) > 0
    );
  }

  getImagenCartaEnJuego(carta: any): string {
    if (carta?.bocaAbajo) return '/images/cards/back.png';
    return this.getImagenCarta(carta?.card?.id || carta?.id);
  }

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

  getSpriteBack(carta: any): string {
    return this.battleBoardUi.getSpriteBack(carta);
  }
  getSpriteFront(carta: any): string {
    return this.battleBoardUi.getSpriteFront(carta);
  }

  onSpriteError(event: Event, carta: any): void {
    const img = event.target as HTMLImageElement;
    if (!carta) {
      img.style.display = 'none';
      return;
    }

    const num = (this.battleBoardUi as any).getPokemonNum(carta);
    const triedOnline = img.getAttribute('data-tried-online') === 'true';

    if (num > 0 && !triedOnline) {
      img.setAttribute('data-tried-online', 'true');
      const esTrasero = img.classList.contains('sprite-back');
      img.src = esTrasero
        ? `https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/other/showdown/back/${num}.gif`
        : `https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/other/showdown/${num}.gif`;
    } else {
      const fallbackSrc = this.battleBoardUi.getImagenCarta(carta.id);
      if (!img.src.includes(fallbackSrc)) {
        img.src = fallbackSrc;
        img.classList.add('fallback-sprite');
      } else {
        img.style.display = 'none';
      }
    }
  }

  onCardImageError(event: Event): void {
    const img = event.target as HTMLImageElement;
    this.cardService.handleCardImageError(img);
  }

  tieneCondicionEspecial(quien: 'JUGADOR' | 'BOT', condicion: string): boolean {
    const activo = quien === 'JUGADOR' ? this.partida?.jugador?.activo : this.partida?.bot?.activo;
    return activo?.condicionesEspeciales.includes(condicion) ?? false;
  }

  getAtaquesActivoJugador(): BattleBoardAttack[] {
    return (this.partida?.jugador?.activo?.card.ataques as BattleBoardAttack[] | undefined) ?? [];
  }

  tieneEnergiaHAD(activo: any): boolean {
    if (!activo || !activo.energiasUnidas) return false;
    return activo.energiasUnidas.some((e: any) => e.tipo === 'HAD' || e.tipo === 'Fairy' || e.tipo?.toLowerCase() === 'fairy');
  }

  getCostoRetiradaActivoJugador(): number {
    const cost = this.partida?.jugador?.activo?.card.costoRetirada ?? 0;
    if (this.partida?.activeStadium?.id === 'xy1-125' && this.tieneEnergiaHAD(this.partida?.jugador?.activo)) {
      return 0;
    }
    return cost;
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

  getPrizeTarget(): number {
    return this.partida?.muerteSubita ? 1 : 6;
  }

  getPrizeSlots(): number[] {
    return Array.from({ length: this.getPrizeTarget() }, (_, index) => index);
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
    if (this.partida?.turnoActual !== 'JUGADOR') {
      return this.i18n.translate('battle.rivalTurn');
    }
    return this.turnTimerEnabled
      ? this.i18n.translate('battle.turnTime', { time: this.tiempoRestante.toString() })
      : this.i18n.translate('battle.turnNoLimit');
  }

  getTextoBotonTurno(): string {
    return this.partida?.turnoActual === 'JUGADOR' && !this.cargandoAccion
      ? this.i18n.translate('battle.endTurn')
      : this.i18n.translate('battle.waiting');
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
  getDamageCounters(pokemon: any): number {
    if (!pokemon) return 0;
    const maxHp = this.getHpMax(pokemon);
    const currentHp = pokemon.hpActual ?? maxHp;
    return Math.max(0, maxHp - currentHp) / 10;
  }
  getImagenCarta(id: string): string {
    return this.battleBoardUi.getImagenCarta(id);
  }
  abrirDetallePokemon(event: MouseEvent, pokemon: CartaEnJuego | null | undefined): void {
    if (!pokemon || pokemon.bocaAbajo) return;
    event.preventDefault();
    this.selectedDetailPokemon = pokemon;
    this.mostrarDetalleModal = true;
    this.cdr.detectChanges();
  }
  cerrarDetallePokemon(): void {
    this.selectedDetailPokemon = null;
    this.mostrarDetalleModal = false;
    this.cdr.detectChanges();
  }

  esCartaEX(carta: Card | undefined | null): boolean {
    return !!carta?.subtypes?.includes('EX');
  }

  getPokemonFase(carta: Card | undefined | null): string {
    if (!carta) return '';
    const subtypes = carta.subtypes || [];
    const lower = subtypes.map(s => String(s).toLowerCase());
    if (lower.includes('stage 1')) return 'Fase 1';
    if (lower.includes('stage 2')) return 'Fase 2';
    if (lower.includes('basic')) return 'Básico';
    return subtypes.join(', ') || 'Básico';
  }

  resolveEnergyLabel(tipo: string): string {
    const labels: Record<string, string> = {
      Grass: 'PLA', Fire: 'FUE', Water: 'AGU', Lightning: 'ELE', Psychic: 'PSI',
      Fighting: 'LUC', Darkness: 'SIN', Metal: 'MET', Fairy: 'HAD',
      Dragon: 'DRA', Colorless: 'INC'
    };
    return labels[tipo] || tipo.slice(0, 2).toUpperCase();
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
  esTrainer(carta: any): boolean {
    return this.battleBoardUi.esTrainer(carta);
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

  mostrarEfectoStatusVisual(objetivo: 'jugador' | 'bot', condicion: string) {
    let color = '#ffffff';
    if (condicion === 'Poisoned') color = '#a855f7';
    else if (condicion === 'Burned') color = '#f97316';
    else if (condicion === 'Paralyzed') color = '#eab308';
    else if (condicion === 'Asleep') color = '#93c5fd';
    else if (condicion === 'Confused') color = '#d946ef';

    const nuevasParticulas = [];
    for (let i = 0; i < 15; i++) {
      const angulo = Math.random() * Math.PI * 2;
      const distancia = 30 + Math.random() * 60;
      nuevasParticulas.push({
        color,
        tx: Math.cos(angulo) * distancia,
        ty: Math.sin(angulo) * distancia,
        size: 6 + Math.random() * 8,
        duracion: 0.8 + Math.random() * 0.4,
      });
    }

    if (objetivo === 'bot') {
      this.particulasBot = nuevasParticulas;
      this.mostrarEfectoBot = true;
    } else {
      this.particulasJugador = nuevasParticulas;
      this.mostrarEfectoJugador = true;
    }

    this.cdr.detectChanges();
    setTimeout(() => {
      if (objetivo === 'bot') {
        this.mostrarEfectoBot = false;
      } else {
        this.mostrarEfectoJugador = false;
      }
      this.cdr.detectChanges();
    }, 1200);
  }

  private async aplicarEstadoRefrescado(estado: Partida | null | undefined): Promise<void> {
    if (!estado) return;
    const debeMostrarFinal = estado.faseActual === 'FIN_PARTIDA';
    const estadoAnterior = this.partida ? this.battleBoardState.clonarPartida(this.partida) : null;

    if (estado.faseActual === 'LANZAMIENTO_MONEDA') {
      const yaReingresoAlSetup =
        this.partida?.faseActual === 'LANZAMIENTO_MONEDA' &&
        this.estadoCoinFlip !== 'OCULTO' &&
        !this.boardVisible;
      if (!yaReingresoAlSetup) {
        this.reingresarAFaseLanzamientoMoneda(estado);
        return;
      }
    }

    // El overlay de muerte súbita se mostrará al iniciar TURNO_NORMAL

    // Check if cards were drawn
    if (this.partida) {
      const previousPrizes = this.partida.jugador?.premios?.length || 0;
      const nextPrizes = estado.jugador?.premios?.length || 0;
      const prevHandSizeJugador = this.partida.jugador?.mano?.length || 0;
      const newHandSizeJugador = estado.jugador?.mano?.length || 0;
      if (newHandSizeJugador > prevHandSizeJugador) {
        const drawn = newHandSizeJugador - prevHandSizeJugador;
        this.mostrarNotificacion(`Has robado ${drawn} carta(s).`, 'info');
        if (nextPrizes < previousPrizes) {
          const revealed = estado.jugador.mano[newHandSizeJugador - 1];
          if (revealed) {
            this.prizeRevealCard = revealed;
            window.setTimeout(() => {
              this.prizeRevealCard = null;
              this.cdr.detectChanges();
            }, 1900);
          }
        }
      }

      const prevHandSizeBot = this.partida.bot?.mano?.length || 0;
      const newHandSizeBot = estado.bot?.mano?.length || 0;
      if (newHandSizeBot > prevHandSizeBot) {
        const drawn = newHandSizeBot - prevHandSizeBot;
        this.mostrarNotificacion(`El rival robó ${drawn} carta(s).`, 'info');
      }
    }

    this.partida = this.battleBoardState.clonarPartida(estado);
    this.lastAppliedStateSignature = this.crearFirmaPartida(this.partida);
    this.hpRenderJugador = estado.jugador?.activo?.hpActual || 0;
    this.lastProcessedCoinFlipEventId = estado.lastCoinFlipEventId || 0;

    if (estadoAnterior) {
      void this.procesarEventosPostEstado(estadoAnterior, estado).then(() => {
        if (debeMostrarFinal) {
          this.programarFinDePartida(estado, 550);
        }
      });
    } else if (debeMostrarFinal) {
      this.programarFinDePartida(estado);
    }
    if (estado.jugador?.mano) {
      this.detectarCartasNuevas(estado.jugador.mano);
    }
    if (estado.bot?.mano) {
      this.detectarCartasNuevasBot(estado.bot.mano);
    }
    await this.procesarTurnLogs();
  }

  private async reproducirChequeoDespertar(
    descripcion: string,
    seDesperto: boolean,
  ): Promise<void> {
    while (this.showTurnOverlay) {
      await this.delay(100);
    }
    await this.delay(500);

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
    const coinConfig = this.detectarCoinFlipAtaque(habilidad, this.partida?.jugador?.activo);
    if (!coinConfig) return;
    const monedasServidor = estadoFinal.ultimasMonedasLanzadas?.length || 0;
    if (monedasServidor > 0) coinConfig.cantidadMonedas = monedasServidor;

    const hpBotAntes = this.partida?.bot?.activo?.hpActual || 0;
    const hpBotDespues = estadoFinal.bot?.activo?.hpActual || 0;
    const danioHecho = this.battleBoardCombat.calcularDanioHecho(hpBotAntes, hpBotDespues);

    let carasReales = this.contarCarasServidor(estadoFinal);
    if (carasReales === null) {
      if (coinConfig.tipoEfecto === 'self-damage') {
        const hpPropioAntes = this.partida?.jugador?.activo?.hpActual || 0;
        const hpPropioDespues = estadoFinal.jugador?.activo?.hpActual || 0;
        const autodanioHecho = hpPropioAntes - hpPropioDespues;
        carasReales = autodanioHecho > 0 ? 0 : 1;
      } else {
        carasReales = this.battleBoardTurn.resolverCarasJugador(
          coinConfig,
          habilidad,
          estadoFinal,
          danioHecho,
        );
      }
    }
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

  private contarCarasServidor(estado: Partida | null | undefined): number | null {
    const monedas = estado?.ultimasMonedasLanzadas;
    if (!monedas?.length) return null;
    return monedas.filter(Boolean).length;
  }

  private async reproducirImpactoAtaqueJugador(
    tipoEnergia: string,
    estadoFinal: Partida,
  ): Promise<void> {
    this.animandoAtaque = true;
    this.triggerTrainerAttack('player');
    this.cdr.detectChanges();

    await this.delay(400);
    this.dispararParticulas('bot', tipoEnergia);
    this.showImpactFlash = true;

    this.aplicarEstadoRefrescado(estadoFinal);
    if (this.showEndGameOverlay) return;
    this.cdr.detectChanges();

    await this.delay(200);
    this.showImpactFlash = false;
    this.cdr.detectChanges();

    await this.delay(400);
    this.animandoAtaque = false;
    this.cdr.detectChanges();
  }

  private async finalizarSecuenciaAtaque(estadoFinal: Partida): Promise<void> {
    if (estadoFinal.faseActual === 'FIN_PARTIDA') {
      this.aplicarEstadoRefrescado(estadoFinal);
      return;
    }
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
    await this.aplicarEstadoRefrescado(estadoFinal);
    if (this.showEndGameOverlay) return;
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

    if (this.partida?.botUsername) {
      this.bloqueadoPorAnimacion = false;
      this.cargandoAccion = false;
      this.cdr.detectChanges();
      return;
    }

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

  volverAlLobby(): void {
    this.router.navigate(['/lobby']);
  }

  getCharacterLabel(id: string): string {
    const option = CHARACTER_OPTIONS.find(o => o.id === id);
    return option ? option.label : 'Trainer';
  }

  cargarDatosOponente(): void {
    if (this.opponentLoaded || !this.nombreRival) {
      return;
    }
    this.opponentLoaded = true;

    if (!this.esPartidaOnline()) {
      this.randomizeBotCustomization();
      this.cdr.detectChanges();
      return;
    }

    this.jugadorService.getJugador(this.nombreRival).subscribe({
      next: (res) => {
        if (res) {
          const oldCharId = this.opponentCharacterId;
          this.opponentCharacterId = res.characterId || 'robot';
          this.opponentSkinColor = res.skinColor || '#ffe0bd';
          this.opponentHairColor = res.hairColor || '#5c4033';
          this.opponentEyeColor = res.eyeColor || '#2563eb';
          this.opponentHeight = res.height || 1.0;
          
          if (this.opponentCharacterId !== oldCharId) {
            if (this.handshakeScene) {
              if (this.opponentModel) {
                this.disposeModelMaterials(this.opponentModel);
                this.handshakeScene.remove(this.opponentModel);
                this.opponentModel = undefined;
              }
              this.opponentMixer = undefined;
              this.opponentActions.clear();
              this.loadOpponentModelInScene();
            }
            if (this.opponentTrainerScene) {
              this.reloadOpponentTrainerModel();
            }
            if (this.versusScene) {
              this.reloadVersusOpponentModel();
            }
          } else {
            this.loadOpponent3DModel();
            if (this.opponentTrainerModel) {
              this.applyModelCustomization(
                this.opponentTrainerModel,
                this.opponentCharacterId,
                this.opponentSkinColor,
                this.opponentHairColor,
                this.opponentEyeColor,
                this.opponentHeight
              );
            }
            if (this.versusOpponentModel) {
              this.applyModelCustomization(
                this.versusOpponentModel,
                this.opponentCharacterId,
                this.opponentSkinColor,
                this.opponentHairColor,
                this.opponentEyeColor,
                this.opponentHeight
              );
            }
          }
          this.cdr.detectChanges();
        }
      },
      error: (err) => {
        console.warn('No se pudo cargar datos del rival, usando bot por defecto', err);
        this.randomizeBotCustomization();
        this.cdr.detectChanges();
      }
    });
  }

  private randomizeBotCustomization(): void {
    const characters = ['ash', 'lillie', 'hilda-sygna', 'robot', 'adaman', 'giovanni-sygna', 'hugh', 'courtney', 'irida', 'zinnia'];
    const skins = ['#ffe0bd', '#f1c27d', '#e0ac69', '#c68642', '#8d5524', '#ffdbac'];
    const hairs = ['#5c4033', '#2c1e18', '#b55229', '#e6c875', '#ffffff', '#2563eb', '#16a34a', '#db2777', '#7c3aed'];
    const eyes = ['#2563eb', '#16a34a', '#7c3aed', '#b45309', '#475569', '#db2777'];
    
    const oldCharId = this.opponentCharacterId;
    
    this.opponentCharacterId = characters[Math.floor(Math.random() * characters.length)];
    this.opponentSkinColor = skins[Math.floor(Math.random() * skins.length)];
    this.opponentHairColor = hairs[Math.floor(Math.random() * hairs.length)];
    this.opponentEyeColor = eyes[Math.floor(Math.random() * eyes.length)];
    this.opponentHeight = parseFloat((0.92 + Math.random() * 0.16).toFixed(3)); // 0.92 to 1.08
    
    console.log('Bot customization randomized:', {
      id: this.opponentCharacterId,
      skin: this.opponentSkinColor,
      hair: this.opponentHairColor,
      eye: this.opponentEyeColor,
      height: this.opponentHeight
    });
    
    if (this.opponentCharacterId !== oldCharId) {
      if (this.handshakeScene) {
        if (this.opponentModel) {
          this.disposeModelMaterials(this.opponentModel);
          this.handshakeScene.remove(this.opponentModel);
          this.opponentModel = undefined;
        }
        this.opponentMixer = undefined;
        this.opponentActions.clear();
        this.loadOpponentModelInScene();
      }
      if (this.opponentTrainerScene) {
        this.reloadOpponentTrainerModel();
      }
      if (this.versusScene) {
        this.reloadVersusOpponentModel();
      }
    } else {
      this.loadOpponent3DModel();
      if (this.opponentTrainerModel) {
        this.applyModelCustomization(
          this.opponentTrainerModel,
          this.opponentCharacterId,
          this.opponentSkinColor,
          this.opponentHairColor,
          this.opponentEyeColor,
          this.opponentHeight
        );
      }
      if (this.versusOpponentModel) {
        this.applyModelCustomization(
          this.versusOpponentModel,
          this.opponentCharacterId,
          this.opponentSkinColor,
          this.opponentHairColor,
          this.opponentEyeColor,
          this.opponentHeight
        );
      }
    }
  }

  loadOpponent3DModel(): void {
    if (this.opponentModel) {
      this.applyModelCustomization(
        this.opponentModel,
        this.opponentCharacterId,
        this.opponentSkinColor,
        this.opponentHairColor,
        this.opponentEyeColor,
        this.opponentHeight
      );
    }
  }

  reloadOpponentTrainerModel(): void {
    if (!this.opponentTrainerScene || !this.opponentTrainerRenderer) return;

    if (this.opponentTrainerModel) {
      this.disposeModelMaterials(this.opponentTrainerModel);
      this.opponentTrainerScene.remove(this.opponentTrainerModel);
      this.opponentTrainerModel = undefined;
    }
    this.opponentTrainerMixer = undefined;
    this.opponentTrainerActions.clear();

    const oppSkin = this.opponentSkinColor || undefined;
    const oppHair = this.opponentHairColor || undefined;
    const oppEye = this.opponentEyeColor || undefined;
    const oppHeight = this.opponentHeight || 1.0;

    const loadingId = this.opponentCharacterId;
    this.loadHandshakeCharacter(loadingId, (model, animations) => {
      if (loadingId !== this.opponentCharacterId) {
        this.disposeModelMaterials(model);
        return;
      }
      this.opponentTrainerModel = model;
      this.opponentTrainerMixer = new THREE.AnimationMixer(model);
      animations.forEach(clip => {
        this.opponentTrainerActions.set(clip.name.toLowerCase(), this.opponentTrainerMixer!.clipAction(clip));
      });

      this.applyModelCustomization(
        model,
        this.opponentCharacterId,
        oppSkin,
        oppHair,
        oppEye,
        oppHeight
      );

      model.rotation.y = -Math.PI / 5;
      this.opponentTrainerScene!.add(model);

      this.playTrainerIdle('opponent');
    }, this.opponentTrainerRenderer);
  }

  @ViewChild('playerTrainerCanvas') set playerTrainerCanvas(element: ElementRef<HTMLCanvasElement> | undefined) {
    if (element && !this.playerTrainerCanvasInitialized) {
      this.playerTrainerCanvasInitialized = true;
      setTimeout(() => this.initPlayerTrainerScene(element.nativeElement), 0);
    }
  }

  @ViewChild('opponentTrainerCanvas') set opponentTrainerCanvas(element: ElementRef<HTMLCanvasElement> | undefined) {
    if (element && !this.opponentTrainerCanvasInitialized) {
      this.opponentTrainerCanvasInitialized = true;
      setTimeout(() => this.initOpponentTrainerScene(element.nativeElement), 0);
    }
  }

  @ViewChild('battleAtmosphereCanvas') set battleAtmosphereCanvas(element: ElementRef<HTMLCanvasElement> | undefined) {
    if (element && !this.battleAtmosphereRenderer) {
      setTimeout(() => this.initBattleAtmosphere(element.nativeElement), 0);
    }
  }

  @ViewChild('coinFlipCanvas') set coinFlipCanvas(element: ElementRef<HTMLCanvasElement> | undefined) {
    if (element) {
      if (!this.coinFlipCanvasInitialized) {
        this.coinFlipCanvasInitialized = true;
        setTimeout(() => this.initCoinFlipScene(element.nativeElement), 0);
      }
    } else {
      if (this.coinFlipCanvasInitialized) {
        this.cleanupCoinFlipScene();
      }
    }
  }

  @ViewChild('handshakeCanvas') set handshakeCanvas(element: ElementRef<HTMLCanvasElement> | undefined) {
    if (element && !this.canvasInitialized) {
      this.canvasInitialized = true;
      setTimeout(() => this.initHandshakeScene(element.nativeElement), 0);
    }
  }

  @ViewChild('versusCanvas') set versusCanvas(element: ElementRef<HTMLCanvasElement> | undefined) {
    if (!element) {
      this.cleanupVersusScene(true);
    } else if (!this.versusCanvasInitialized) {
      this.versusCanvasInitialized = true;
      setTimeout(() => this.initVersusScene(element.nativeElement), 0);
    }
  }

  private initBattleAtmosphere(canvas: HTMLCanvasElement): void {
    const width = Math.max(1, canvas.clientWidth);
    const height = Math.max(1, canvas.clientHeight);
    const renderer = new THREE.WebGLRenderer({
      canvas,
      alpha: false,
      antialias: !this.isPotato,
      powerPreference: this.isPotato ? 'low-power' : 'high-performance'
    });
    renderer.setPixelRatio(this.isPotato ? 1 : Math.min(window.devicePixelRatio, 1.35));
    renderer.setSize(width, height, false);
    renderer.setClearColor(0x020711, 1);
    renderer.outputColorSpace = THREE.SRGBColorSpace;
    this.battleAtmosphereRenderer = renderer;

    const scene = new THREE.Scene();
    scene.fog = new THREE.FogExp2(0x020711, 0.115);
    this.battleAtmosphereScene = scene;

    const camera = new THREE.PerspectiveCamera(42, width / height, 0.1, 60);
    camera.position.set(0, 2.8, 8.8);
    camera.lookAt(0, 0, 0);
    this.battleAtmosphereCamera = camera;

    const floor = new THREE.Mesh(
      new THREE.PlaneGeometry(36, 24),
      new THREE.MeshStandardMaterial({
        color: 0x07131d,
        emissive: 0x06131d,
        emissiveIntensity: 0.5,
        roughness: 0.78,
        metalness: 0.28,
        transparent: true,
        opacity: 0.92
      })
    );
    floor.rotation.x = -Math.PI / 2;
    floor.position.y = -1.65;
    scene.add(floor);
    this.atmosphereFloor = floor;

    const grid = new THREE.GridHelper(18, 30, 0x38bdf8, 0x173b4f);
    grid.position.y = -1.62;
    const gridMaterials = Array.isArray(grid.material) ? grid.material : [grid.material];
    gridMaterials.forEach(material => {
      material.transparent = true;
      material.opacity = 0.22;
    });
    scene.add(grid);
    this.atmosphereGrid = grid;

    this.battleAtmosphereRings = [];
    const ringGeometry = new THREE.TorusGeometry(2.25, 0.026, 6, 72);
    const ringConfigs = [
      { z: -2.35, color: 0x38bdf8, radius: 1 },
      { z: -2.35, color: 0x67e8f9, radius: 0.72 },
      { z: 2.35, color: 0xfacc15, radius: 1 },
      { z: 2.35, color: 0xf59e0b, radius: 0.72 }
    ];
    ringConfigs.forEach((config, index) => {
      const material = new THREE.MeshBasicMaterial({
        color: config.color,
        transparent: true,
        opacity: index % 2 === 0 ? 0.32 : 0.18,
        blending: THREE.AdditiveBlending,
        depthWrite: false
      });
      const ring = new THREE.Mesh(ringGeometry, material);
      ring.rotation.x = Math.PI / 2;
      ring.position.set(0, -1.56 + index * 0.006, config.z);
      ring.scale.setScalar(config.radius);
      ring.userData['phase'] = index * 1.35;
      ring.userData['baseScale'] = config.radius;
      scene.add(ring);
      this.battleAtmosphereRings.push(ring);
    });

    this.battleAtmosphereBeams = [];
    const beamGeometry = new THREE.CylinderGeometry(0.016, 0.055, 4.6, 6, 1, true);
    [-6.8, 6.8].forEach((x, sideIndex) => {
      [-2.6, 0, 2.6].forEach((z, laneIndex) => {
        const material = new THREE.MeshBasicMaterial({
          color: sideIndex === 0 ? 0x38bdf8 : 0xfacc15,
          transparent: true,
          opacity: 0.07 + laneIndex * 0.018,
          blending: THREE.AdditiveBlending,
          depthWrite: false,
          side: THREE.DoubleSide
        });
        const beam = new THREE.Mesh(beamGeometry, material);
        beam.position.set(x, 0.55, z);
        beam.userData['phase'] = sideIndex * 1.7 + laneIndex * 0.8;
        scene.add(beam);
        this.battleAtmosphereBeams.push(beam);
      });
    });

    const shardCount = this.isPotato ? 8 : 22;
    const shardGeometry = new THREE.IcosahedronGeometry(0.055, 0);
    const shardMaterial = new THREE.MeshBasicMaterial({
      color: 0x9ae6ff,
      transparent: true,
      opacity: 0.32,
      blending: THREE.AdditiveBlending,
      depthWrite: false
    });
    const shards = new THREE.InstancedMesh(shardGeometry, shardMaterial, shardCount);
    const shardMatrix = new THREE.Matrix4();
    const shardPosition = new THREE.Vector3();
    const shardQuaternion = new THREE.Quaternion();
    const shardScale = new THREE.Vector3();
    for (let index = 0; index < shardCount; index++) {
      const side = index % 2 === 0 ? -1 : 1;
      shardPosition.set(
        side * (5.2 + Math.random() * 2.4),
        -0.8 + Math.random() * 4.8,
        (Math.random() - 0.5) * 8
      );
      shardQuaternion.setFromEuler(new THREE.Euler(
        Math.random() * Math.PI,
        Math.random() * Math.PI,
        Math.random() * Math.PI
      ));
      const scale = 0.55 + Math.random() * 1.25;
      shardScale.set(scale, scale * (1.2 + Math.random()), scale);
      shardMatrix.compose(shardPosition, shardQuaternion, shardScale);
      shards.setMatrixAt(index, shardMatrix);
    }
    shards.instanceMatrix.needsUpdate = true;
    this.battleAtmosphereShards = shards;
    scene.add(shards);

    const particleCount = this.isPotato ? 100 : 350;
    const positions = new Float32Array(particleCount * 3);
    for (let index = 0; index < particleCount; index++) {
      positions[index * 3] = (Math.random() - 0.5) * 16;
      positions[index * 3 + 1] = Math.random() * 7 - 1.2;
      positions[index * 3 + 2] = (Math.random() - 0.5) * 8;
    }
    const particleGeometry = new THREE.BufferGeometry();
    particleGeometry.setAttribute('position', new THREE.BufferAttribute(positions, 3));
    this.battleAtmosphereParticles = new THREE.Points(
      particleGeometry,
      new THREE.PointsMaterial({
        color: 0x8be9ff,
        size: this.isPotato ? 0.025 : 0.038,
        transparent: true,
        opacity: 0.26,
        blending: THREE.AdditiveBlending,
        depthWrite: false
      })
    );
    scene.add(this.battleAtmosphereParticles);

    // Shield groups initialization
    this.playerShieldGroup = new THREE.Group();
    this.playerShieldGroup.position.set(0, -0.5, 2.35);
    scene.add(this.playerShieldGroup);

    this.botShieldGroup = new THREE.Group();
    this.botShieldGroup.position.set(0, -0.5, -2.35);
    scene.add(this.botShieldGroup);

    const buildShield = (group: THREE.Group, colorHex: number) => {
      // 1. Transparent pulsing bubble sphere
      const sphereGeom = new THREE.SphereGeometry(1.0, 24, 24);
      const bubbleMat = new THREE.MeshBasicMaterial({
        color: colorHex,
        transparent: true,
        opacity: 0.12,
        wireframe: true,
        blending: THREE.AdditiveBlending
      });
      const bubble = new THREE.Mesh(sphereGeom, bubbleMat);
      group.add(bubble);

      const glowMat = new THREE.MeshBasicMaterial({
        color: colorHex,
        transparent: true,
        opacity: 0.04,
        blending: THREE.AdditiveBlending
      });
      const glow = new THREE.Mesh(sphereGeom, glowMat);
      group.add(glow);

      // 2. Rotating hexagonal outer shield
      const ringGeom = new THREE.RingGeometry(1.05, 1.15, 6);
      const ringMat = new THREE.MeshBasicMaterial({
        color: colorHex,
        transparent: true,
        opacity: 0.6,
        side: THREE.DoubleSide,
        blending: THREE.AdditiveBlending
      });
      const ring1 = new THREE.Mesh(ringGeom, ringMat);
      ring1.rotation.x = Math.PI / 4;
      group.add(ring1);

      const ring2 = new THREE.Mesh(ringGeom, ringMat);
      ring2.rotation.y = Math.PI / 4;
      group.add(ring2);
    };

    buildShield(this.playerShieldGroup, 0xf59e0b); // gold/amber for player
    buildShield(this.botShieldGroup, 0x3b82f6);    // blue for bot

    const hemiLight = new THREE.HemisphereLight(0x8be9ff, 0x030712, 1.45);
    scene.add(hemiLight);
    this.atmosphereHemisphereLight = hemiLight;

    const rivalLight = new THREE.PointLight(0x38bdf8, 8, 14, 2);
    rivalLight.position.set(0, 2.8, 1.8);
    scene.add(rivalLight);
    this.atmosphereRivalLight = rivalLight;

    const playerLight = new THREE.PointLight(0xfacc15, 7, 14, 2);
    playerLight.position.set(0, -1.1, 3.8);
    scene.add(playerLight);
    this.atmospherePlayerLight = playerLight;

    this.battleAtmosphereResize = new ResizeObserver(entries => {
      const rect = entries[0]?.contentRect;
      if (!rect || !this.battleAtmosphereRenderer || !this.battleAtmosphereCamera) return;
      const nextWidth = Math.max(1, rect.width);
      const nextHeight = Math.max(1, rect.height);
      this.battleAtmosphereRenderer.setSize(nextWidth, nextHeight, false);
      this.battleAtmosphereCamera.aspect = nextWidth / nextHeight;
      this.battleAtmosphereCamera.updateProjectionMatrix();
    });
    this.battleAtmosphereResize.observe(canvas);

    const animate = () => {
      if (!this.battleAtmosphereRenderer || !this.battleAtmosphereScene || !this.battleAtmosphereCamera) return;
      this.battleAtmosphereFrame = requestAnimationFrame(animate);
      const elapsed = this.battleAtmosphereClock.getElapsedTime();

      // Dynamic game state tension based on prize race + match progression.
      const playerPrizes = this.partida?.jugador?.premios?.length ?? 6;
      const botPrizes = this.partida?.bot?.premios?.length ?? 6;
      const minPrizes = Math.min(playerPrizes, botPrizes);
      const prizeDanger = Math.min(1.0, Math.max(0.0, (6 - minPrizes) / 5.0));
      const turnNumber = this.partida?.numeroTurno ?? 1;
      const tempoDanger = Math.min(1.0, Math.max(0.0, (turnNumber - 1) / 10));
      const dangerLevel = Math.min(1.0, prizeDanger * 0.78 + tempoDanger * 0.22);
      const criticalPressure = !this.partida?.muerteSubita && dangerLevel >= 0.72;
      const pressurePulse = criticalPressure
        ? (Math.sin(elapsed * (1.35 + dangerLevel * 1.1)) + 1) * 0.5
        : 0;

      // Lightning flash logic for Sudden Death
      let flashColorOverride: THREE.Color | null = null;
      let flashIntensityMultiplier = 1.0;

      if (this.partida?.muerteSubita) {
        if (this.lightningFlashActive) {
          this.lightningFlashTimer--;
          if (this.lightningFlashTimer <= 0) {
            this.lightningFlashActive = false;
          }
          // Flash spike (bright red/white thunder illumination)
          flashColorOverride = new THREE.Color(Math.random() < 0.5 ? 0xff6666 : 0xffffff);
          flashIntensityMultiplier = 4.5;
        } else {
          // 0.6% chance per frame to trigger a lightning strike
          if (Math.random() < 0.006) {
            this.lightningFlashActive = true;
            this.lightningFlashTimer = 6 + Math.floor(Math.random() * 8); // 6 to 13 frames
          }
        }
      } else if (criticalPressure) {
        if (this.lightningFlashActive) {
          this.lightningFlashTimer--;
          if (this.lightningFlashTimer <= 0) {
            this.lightningFlashActive = false;
          }
          flashColorOverride = new THREE.Color(0xffe2d2);
          flashIntensityMultiplier = 1.25 + dangerLevel * 0.55;
        } else if (Math.random() < 0.0012 + dangerLevel * 0.0018) {
          this.lightningFlashActive = true;
          this.lightningFlashTimer = 2 + Math.floor(Math.random() * 3);
        }
      }

      // 1. Dynamic background clear color and fog color interpolation
      let currentClearColor: THREE.Color;
      if (this.partida?.muerteSubita) {
        currentClearColor = flashColorOverride ? flashColorOverride : new THREE.Color(0x1a0002);
      } else {
        const baseColor = new THREE.Color(0x020711);
        const midDangerColor = new THREE.Color(0x10172c);
        const dangerClearColor = new THREE.Color(0x220406);
        currentClearColor = baseColor.clone().lerp(midDangerColor, Math.min(1, dangerLevel * 0.55));
        currentClearColor.lerp(dangerClearColor, Math.max(0, dangerLevel - 0.35) / 0.65);
        if (flashColorOverride) {
          currentClearColor.lerp(flashColorOverride, 0.28);
        }
      }
      this.battleAtmosphereRenderer.setClearColor(currentClearColor);

      if (this.battleAtmosphereScene.fog) {
        const expFog = this.battleAtmosphereScene.fog as THREE.FogExp2;
        if (this.partida?.muerteSubita) {
          expFog.color.copy(flashColorOverride ? flashColorOverride : new THREE.Color(0x220003));
        } else {
          const baseColor = new THREE.Color(0x020711);
          const dangerFogColor = new THREE.Color(0x260507);
          expFog.color.copy(baseColor).lerp(dangerFogColor, dangerLevel);
          if (flashColorOverride) {
            expFog.color.lerp(flashColorOverride, 0.22);
          }
        }
      }

      // 2. Falling particles physics (intense rain in Sudden Death, else normal space dust/atmosphere particles)
      if (this.battleAtmosphereParticles) {
        const geo = this.battleAtmosphereParticles.geometry as THREE.BufferGeometry;
        const posAttr = geo.getAttribute('position') as THREE.BufferAttribute;
        const mat = this.battleAtmosphereParticles.material as THREE.PointsMaterial;

        // Dynamic particle color and opacity
        if (this.partida?.muerteSubita) {
          mat.color.setHex(0xff3333); // vibrant red rain
          mat.opacity = 0.55;
        } else {
          const particleColor = new THREE.Color(0x8be9ff)
            .lerp(new THREE.Color(0xf59e0b), Math.min(1, dangerLevel * 0.75))
            .lerp(new THREE.Color(0xff5b5b), Math.max(0, dangerLevel - 0.58) / 0.42);
          mat.color.copy(particleColor);
          mat.opacity = 0.22 + dangerLevel * 0.24 + pressurePulse * 0.06;
          mat.size = 0.085 + dangerLevel * 0.055;
        }

        if (posAttr) {
          const positions = posAttr.array as Float32Array;
          const fallSpeed = this.partida?.muerteSubita ? 0.16 : (0.006 + dangerLevel * 0.03);
          const swaySpeed = this.partida?.muerteSubita ? 1.5 : (0.55 + dangerLevel * 0.8);
          const swayFactor = this.partida?.muerteSubita ? 0.0018 : (0.0012 + dangerLevel * 0.0016);

          for (let i = 0; i < positions.length / 3; i++) {
            positions[i * 3 + 1] -= fallSpeed; // move down Y
            positions[i * 3] += Math.sin(elapsed * swaySpeed + i) * swayFactor; // sway X
            
            // Reset if they fall below floor limit
            if (positions[i * 3 + 1] < -1.65) {
              positions[i * 3 + 1] = 5.5 + Math.random() * 2.0; // spawn above top
              positions[i * 3] = (Math.random() - 0.5) * 16.0;  // spread out X
            }
          }
          posAttr.needsUpdate = true;
        }
        this.battleAtmosphereParticles.rotation.y = elapsed * (this.partida?.muerteSubita ? 0.045 : (0.018 + dangerLevel * 0.012));
      }

      // Dynamic Floor and Grid updates
      if (this.atmosphereFloor) {
        const floorMat = this.atmosphereFloor.material as THREE.MeshStandardMaterial;
        if (this.partida?.muerteSubita) {
          floorMat.color.setHex(0x3a0000); // dark red
          floorMat.emissive.setHex(0x2a0000); // red glow
          floorMat.emissiveIntensity = 0.85 * flashIntensityMultiplier;
        } else {
          const floorColor = new THREE.Color(0x07131d)
            .lerp(new THREE.Color(0x102035), Math.min(1, dangerLevel * 0.5))
            .lerp(new THREE.Color(0x2a090a), Math.max(0, dangerLevel - 0.48) / 0.52);
          const emissiveColor = new THREE.Color(0x06131d)
            .lerp(new THREE.Color(0x122235), Math.min(1, dangerLevel * 0.45))
            .lerp(new THREE.Color(0x4a120d), Math.max(0, dangerLevel - 0.6) / 0.4);
          floorMat.color.copy(floorColor);
          floorMat.emissive.copy(emissiveColor);
          floorMat.emissiveIntensity = 0.45 + dangerLevel * 0.42 + pressurePulse * 0.16;
        }
      }

      if (this.atmosphereGrid) {
        const gridMat = (Array.isArray(this.atmosphereGrid.material) 
          ? this.atmosphereGrid.material[0] 
          : this.atmosphereGrid.material) as THREE.LineBasicMaterial;
        if (gridMat) {
          if (this.partida?.muerteSubita) {
            gridMat.color.setHex(0xff0000); // red grid
            gridMat.opacity = 0.35;
          } else {
            const gridColor = new THREE.Color(0x38bdf8)
              .lerp(new THREE.Color(0xfacc15), Math.min(1, dangerLevel * 0.58))
              .lerp(new THREE.Color(0xff4d4d), Math.max(0, dangerLevel - 0.65) / 0.35);
            gridMat.color.copy(gridColor);
            gridMat.opacity = 0.2 + dangerLevel * 0.14 + pressurePulse * 0.08;
          }
        }
      }

      // Dynamic Lights updates
      if (this.atmosphereHemisphereLight) {
        if (this.partida?.muerteSubita) {
          this.atmosphereHemisphereLight.color.setHex(0xff3333);
          this.atmosphereHemisphereLight.groundColor.setHex(0x110000);
          this.atmosphereHemisphereLight.intensity = 1.6 * flashIntensityMultiplier;
        } else {
          this.atmosphereHemisphereLight.color.copy(
            new THREE.Color(0x8be9ff)
              .lerp(new THREE.Color(0xfde68a), Math.min(1, dangerLevel * 0.6))
              .lerp(new THREE.Color(0xff8b8b), Math.max(0, dangerLevel - 0.62) / 0.38)
          );
          this.atmosphereHemisphereLight.groundColor.copy(
            new THREE.Color(0x030712).lerp(new THREE.Color(0x180406), Math.max(0, dangerLevel - 0.45) / 0.55)
          );
          this.atmosphereHemisphereLight.intensity = 1.35 + dangerLevel * 0.5 + pressurePulse * 0.2;
        }
      }

      if (this.atmosphereRivalLight) {
        if (this.partida?.muerteSubita) {
          this.atmosphereRivalLight.color.setHex(0xff0000);
          this.atmosphereRivalLight.intensity = 10 * flashIntensityMultiplier;
        } else {
          this.atmosphereRivalLight.color.copy(
            new THREE.Color(0x38bdf8).lerp(new THREE.Color(0xff6b6b), Math.max(0, dangerLevel - 0.55) / 0.45)
          );
          this.atmosphereRivalLight.intensity = 7.3 + dangerLevel * 2.4 + pressurePulse * 0.85;
        }
      }

      if (this.atmospherePlayerLight) {
        if (this.partida?.muerteSubita) {
          this.atmospherePlayerLight.color.setHex(0xff0000);
          this.atmospherePlayerLight.intensity = 9 * flashIntensityMultiplier;
        } else {
          this.atmospherePlayerLight.color.copy(
            new THREE.Color(0xfacc15).lerp(new THREE.Color(0xff8a5b), Math.max(0, dangerLevel - 0.48) / 0.52)
          );
          this.atmospherePlayerLight.intensity = 6.8 + dangerLevel * 2.1 + pressurePulse * 0.7;
        }
      }

      // 3. Dynamic atmosphere rings and beams animation
      this.battleAtmosphereRings.forEach((ring, index) => {
        const phase = ring.userData['phase'] || 0;
        const baseScale = ring.userData['baseScale'] || 1;
        const pulse = 1 + Math.sin(elapsed * (1.15 + dangerLevel * 1.5) + phase) * (0.035 + dangerLevel * 0.02);
        ring.scale.setScalar(baseScale * pulse);
        ring.rotation.z = elapsed * (index % 2 === 0 ? (0.035 + dangerLevel * 0.05) : -(0.045 + dangerLevel * 0.05));
        
        const mat = ring.material as THREE.MeshBasicMaterial;
        if (this.partida?.muerteSubita) {
          mat.color.setHex(0xff0000); // red rings
        } else {
          mat.color.copy(
            (index < 2 ? new THREE.Color(0x38bdf8) : new THREE.Color(0xfacc15))
              .lerp(new THREE.Color(0xff5f5f), Math.max(0, dangerLevel - 0.58) / 0.42)
          );
        }
        
        mat.opacity =
          (index % 2 === 0 ? 0.28 : 0.15) + Math.sin(elapsed * (1.4 + dangerLevel) + phase) * (0.055 + pressurePulse * 0.04) + dangerLevel * 0.18;
      });

      this.battleAtmosphereBeams.forEach((beam) => {
        const phase = beam.userData['phase'] || 0;
        const mat = beam.material as THREE.MeshBasicMaterial;
        if (this.partida?.muerteSubita) {
          mat.color.setHex(0xff0000); // red beams
        } else {
          const isLeft = beam.position.x < 0;
          mat.color.copy(
            (isLeft ? new THREE.Color(0x38bdf8) : new THREE.Color(0xfacc15))
              .lerp(new THREE.Color(0xff6666), Math.max(0, dangerLevel - 0.62) / 0.38)
          );
        }
        
        mat.opacity =
          0.045 + (Math.sin(elapsed * (0.85 + dangerLevel * 0.6) + phase) + 1) * 0.025 + dangerLevel * 0.055 + pressurePulse * 0.05;
      });

      // 4. Shards (asteroids) rotation, sway, and shake
      if (this.battleAtmosphereShards) {
        this.battleAtmosphereShards.rotation.y = elapsed * (0.025 + dangerLevel * 0.08);
        this.battleAtmosphereShards.position.y = Math.sin(elapsed * (0.3 + dangerLevel * 0.5)) * (0.1 + dangerLevel * 0.12);
        
        // Minor earthquake jitter to floating shards under high game tension
        if (dangerLevel > 0.6) {
          this.battleAtmosphereShards.position.x = (Math.random() - 0.5) * 0.016 * dangerLevel;
        } else {
          this.battleAtmosphereShards.position.x = 0;
        }
      }

      this.battleAtmosphereCamera.position.x = Math.sin(elapsed * (0.12 + dangerLevel * 0.08)) * (0.12 + dangerLevel * 0.07);
      this.battleAtmosphereCamera.position.y = Math.sin(elapsed * (0.16 + dangerLevel * 0.12)) * (dangerLevel > 0.45 ? 0.035 + dangerLevel * 0.015 : 0.012);
      this.battleAtmosphereCamera.lookAt(0, -0.1, 0);

      // Update visibility & animation of shields
      const playerActive = this.partida?.jugador?.activo;
      const botActive = this.partida?.bot?.activo;

      const playerHasShield = !!(playerActive && (playerActive.invulnerable || (playerActive.reduccionDanioRecibido && playerActive.reduccionDanioRecibido > 0)));
      const botHasShield = !!(botActive && (botActive.invulnerable || (botActive.reduccionDanioRecibido && botActive.reduccionDanioRecibido > 0)));

      if (this.playerShieldGroup) {
        this.playerShieldGroup.visible = playerHasShield;
        if (playerHasShield) {
          this.playerShieldGroup.rotation.y = elapsed * 0.6;
          this.playerShieldGroup.rotation.z = elapsed * 0.25;
          const scale = 1.0 + Math.sin(elapsed * 3.5) * 0.04;
          this.playerShieldGroup.scale.setScalar(scale);
        }
      }
      if (this.botShieldGroup) {
        this.botShieldGroup.visible = botHasShield;
        if (botHasShield) {
          this.botShieldGroup.rotation.y = -elapsed * 0.6;
          this.botShieldGroup.rotation.z = -elapsed * 0.25;
          const scale = 1.0 + Math.sin(elapsed * 3.5) * 0.04;
          this.botShieldGroup.scale.setScalar(scale);
        }
      }

      this.battleAtmosphereRenderer.render(this.battleAtmosphereScene, this.battleAtmosphereCamera);
    };
    animate();
  }

  private cleanupBattleAtmosphere(): void {
    if (this.battleAtmosphereFrame) cancelAnimationFrame(this.battleAtmosphereFrame);
    this.battleAtmosphereResize?.disconnect();
    this.battleAtmosphereScene?.traverse(object => {
      const mesh = object as THREE.Mesh;
      mesh.geometry?.dispose();
      const materials = mesh.material
        ? (Array.isArray(mesh.material) ? mesh.material : [mesh.material])
        : [];
      materials.forEach(material => material.dispose());
    });
    this.battleAtmosphereRenderer?.dispose();
    this.battleAtmosphereRenderer = undefined;
    this.battleAtmosphereScene = undefined;
    this.battleAtmosphereCamera = undefined;
    this.battleAtmosphereParticles = undefined;
    this.battleAtmosphereShards = undefined;
    this.battleAtmosphereBeams = [];
    this.battleAtmosphereRings = [];
    this.playerShieldGroup = undefined;
    this.botShieldGroup = undefined;
  }

  private initHandshakeScene(canvas: HTMLCanvasElement): void {
    this.effectsTriggered = false;
    this.handshakeClock.getDelta();

    const width = canvas.clientWidth || 520;
    const height = canvas.clientHeight || 156;

    const renderer = new THREE.WebGLRenderer({
      canvas: canvas,
      alpha: true,
      antialias: !this.isPotato && window.devicePixelRatio < 2,
      preserveDrawingBuffer: false,
      powerPreference: 'high-performance',
      precision: this.isPotato ? 'mediump' : 'highp',
      stencil: false,
      depth: true
    });
    renderer.setSize(width, height, false);
    renderer.setPixelRatio(this.isPotato ? 1.0 : Math.min(window.devicePixelRatio, 1.25));
    this.handshakeRenderer = renderer;

    const scene = new THREE.Scene();
    this.handshakeScene = scene;

    const camera = new THREE.PerspectiveCamera(30, width / height, 0.1, 100);
    camera.position.set(0, 0.82, 3.8);
    camera.lookAt(0, 0.82, 0);
    this.handshakeCamera = camera;

    const ambientLight = new THREE.AmbientLight(0xffffff, 0.76);
    scene.add(ambientLight);

    const spotLight = new THREE.SpotLight(0xfcd34d, 5.0, 10, Math.PI / 3, 0.5, 1);
    spotLight.position.set(0, 2.5, 0.5);
    spotLight.target.position.set(0, 0.82, 0);
    scene.add(spotLight);
    scene.add(spotLight.target);
    this.handshakeSpotlight = spotLight;

    const fillLight = new THREE.PointLight(0x06b6d4, 3.0, 8);
    fillLight.position.set(0, 0.5, -0.5);
    scene.add(fillLight);

    this.loadHandshakePlayerInScene();
    this.loadOpponentModelInScene();

    const animate = () => {
      if (!this.canvasInitialized) return;
      this.handshakeAnimationId = requestAnimationFrame(animate);

      const dt = this.handshakeClock.getDelta();

      const currWidth = canvas.clientWidth || 520;
      const currHeight = canvas.clientHeight || 156;
      if (canvas.width !== currWidth || canvas.height !== currHeight) {
        renderer.setSize(currWidth, currHeight, false);
        camera.aspect = currWidth / currHeight;
        camera.updateProjectionMatrix();
      }
      
      if (this.playerMixer) this.playerMixer.update(dt);
      if (this.opponentMixer) this.opponentMixer.update(dt);

      const playerTargetX = -2.5 + (this.handshakePower / 100) * 2.12;
      const opponentTargetX = 2.5 - (this.opponentHandshakePower / 100) * 2.12;

      if (this.playerModel) {
        const prevX = this.playerModel.position.x;
        this.playerModel.position.x += (playerTargetX - this.playerModel.position.x) * 0.12;
        const speed = Math.abs(this.playerModel.position.x - prevX) / (dt || 0.016);
        this.setHandshakeAnimation(this.playerMixer!, this.playerActions, speed > 0.4 ? 'walk' : 'idle');
      }

      if (this.opponentModel) {
        const prevX = this.opponentModel.position.x;
        this.opponentModel.position.x += (opponentTargetX - this.opponentModel.position.x) * 0.12;
        const speed = Math.abs(this.opponentModel.position.x - prevX) / (dt || 0.016);
        this.setHandshakeAnimation(this.opponentMixer!, this.opponentActions, speed > 0.4 ? 'walk' : 'idle');
      }

      if (this.playerModel && this.opponentModel) {
        const dist = this.opponentModel.position.x - this.playerModel.position.x;
        const blend = Math.max(0, Math.min(1, (2.2 - dist) / 1.44));
        
        this.applyProceduralArm(this.playerRightArm, this.playerRightForeArm, blend, 'player');
        this.applyProceduralArm(this.opponentRightArm, this.opponentRightForeArm, blend, 'opponent');
      }

      if (this.handshakeComplete && !this.effectsTriggered) {
        this.effectsTriggered = true;
        this.triggerHandshakeImpactEffects();
      }

      if (this.handshakeSpotlight && this.handshakeSpotlight.intensity > 5.0) {
        this.handshakeSpotlight.intensity -= 60.0 * dt;
        if (this.handshakeSpotlight.intensity < 5.0) {
          this.handshakeSpotlight.intensity = 5.0;
        }
      }

      this.updateHandshakeParticles(dt);
      this.updateCameraShake(dt, camera);

      if (renderer && scene && camera) {
        renderer.render(scene, camera);
      }
    };

    animate();
  }

  private loadHandshakePlayerInScene(): void {
    this.loadHandshakeCharacter(this.localPlayerCharacterId, (model, animations) => {
      this.playerModel = model;

      model.traverse((child: any) => {
        if (child.isBone) {
          this.defaultQuaternions.set(child, child.quaternion.clone());
        }
      });

      const bones = this.findBones(model);
      this.playerRightArm = bones.rightArm || undefined;
      this.playerRightForeArm = bones.rightForeArm || undefined;

      this.playerMixer = new THREE.AnimationMixer(model);
      animations.forEach(clip => {
        this.playerActions.set(clip.name.toLowerCase(), this.playerMixer!.clipAction(clip));
      });

      const localSkin = localStorage.getItem('lobbySkinColor') || '#ffe0bd';
      const localHair = localStorage.getItem('lobbyHairColor') || '#5c4033';
      const localEye = localStorage.getItem('lobbyEyeColor') || '#2563eb';
      const localHeight = parseFloat(localStorage.getItem('lobbyHeight') || '1.0');
      this.applyModelCustomization(model, this.localPlayerCharacterId, localSkin, localHair, localEye, localHeight);

      model.position.set(-2.5, 0, 0);
      model.rotation.y = Math.PI / 2;
      this.handshakeScene?.add(model);
    });
  }

  private loadOpponentModelInScene(): void {
    const loadingId = this.opponentCharacterId;
    this.loadHandshakeCharacter(loadingId, (model, animations) => {
      if (loadingId !== this.opponentCharacterId) {
        this.disposeModelMaterials(model);
        return;
      }
      
      this.opponentModel = model;

      model.traverse((child: any) => {
        if (child.isBone) {
          this.defaultQuaternions.set(child, child.quaternion.clone());
        }
      });

      const bones = this.findBones(model);
      this.opponentRightArm = bones.rightArm || undefined;
      this.opponentRightForeArm = bones.rightForeArm || undefined;

      this.opponentMixer = new THREE.AnimationMixer(model);
      animations.forEach(clip => {
        this.opponentActions.set(clip.name.toLowerCase(), this.opponentMixer!.clipAction(clip));
      });

      this.applyModelCustomization(
        model,
        this.opponentCharacterId,
        this.opponentSkinColor,
        this.opponentHairColor,
        this.opponentEyeColor,
        this.opponentHeight
      );

      model.position.set(2.5, 0, 0);
      model.rotation.y = -Math.PI / 2;
      this.handshakeScene?.add(model);
    });
  }

  private loadHandshakeCharacter(
    optionId: string,
    onSuccess: (model: THREE.Object3D, animations: THREE.AnimationClip[]) => void,
    renderer?: THREE.WebGLRenderer
  ) {
    const option = CHARACTER_OPTIONS.find(o => o.id === optionId) || CHARACTER_OPTIONS[0];

    // Check cache first to avoid reload/lag
    const cached = HANDSHAKE_GLTF_CACHE.get(option.path);
    if (cached) {
      const clonedScene = cloneSkeleton(cached.scene);
      onSuccess(clonedScene, cached.animations);
      return;
    }

    const loader = new GLTFLoader();
    loader.setMeshoptDecoder(MeshoptDecoder);
    const activeRenderer = renderer || this.handshakeRenderer;
    if (activeRenderer) {
      const ktx2 = this.getKtx2Loader(activeRenderer);
      loader.setKTX2Loader(ktx2);
    }

    loader.load(option.path, (gltf) => {
      const animations = gltf.animations || [];
      HANDSHAKE_GLTF_CACHE.set(option.path, {
        scene: gltf.scene,
        animations
      });
      const clonedScene = cloneSkeleton(gltf.scene);
      onSuccess(clonedScene, animations);
    }, undefined, (err) => {
      console.error('Error loading handshake model:', option.path, err);
    });
  }

  private getKtx2Loader(renderer: THREE.WebGLRenderer): KTX2Loader {
    const ktx2 = new KTX2Loader().setTranscoderPath('/basis/');
    ktx2.detectSupport(renderer);
    return ktx2;
  }

  private applyModelCustomization(
    model: THREE.Object3D,
    optionId: string,
    skinColor?: string,
    hairColor?: string,
    eyeColor?: string,
    heightVal?: number
  ) {
    this.tintCharacterParts(model, {
      skinHex: skinColor,
      hairHex: hairColor,
      eyeHex: eyeColor
    });

    const option = CHARACTER_OPTIONS.find(o => o.id === optionId) || CHARACTER_OPTIONS[0];
    const baseHeight = 1.38 * (option.scale || 1.0);

    model.updateMatrixWorld(true);
    const box = new THREE.Box3().setFromObject(model);
    const size = box.getSize(new THREE.Vector3());
    if (Number.isFinite(size.y) && size.y > 0.01) {
      model.scale.setScalar(baseHeight / size.y);
      this.alignModelBottom(model, 0);
    }

    // Disable frustum culling and shadows on handshake characters to boost FPS
    model.traverse((child: any) => {
      if (child.isMesh) {
        child.frustumCulled = false;
        child.castShadow = false;
        child.receiveShadow = false;
      }
    });
  }

  private tintCharacterParts(
    model: THREE.Object3D,
    colors: { skinHex?: string | null; hairHex?: string | null; eyeHex?: string | null }
  ): void {
    const skinTokens = ['skin', 'piel', 'face', 'head', 'cara', 'kao', 'hada', 'hand', 'hands', 'arm', 'leg', 'neck'];
    const hairTokens = ['hair', 'pelo', 'cabello', 'kami', 'toubu', 'bang', 'ponytail'];
    const eyeTokens = ['eye', 'eyes', 'ojo', 'iris', 'pupil', 'hitomi', 'eyeball', 'gaigan', 'mayu', 'matsuge'];
    const clothTokens = [
      'cloth', 'clothes', 'shirt', 'skirt', 'dress', 'jacket', 'pant', 'shoe', 'boot', 'bag',
      'hat', 'cap', 'ribbon', 'belt', 'sleeve', 'sock', 'uniform', 'outfit'
    ];

    model.traverse((child: any) => {
      if (!child.isMesh || !child.material) return;

      if (Array.isArray(child.material)) {
        child.material = child.material.map((mat: any) => this.getOptimizedMaterial(mat));
      } else {
        child.material = this.getOptimizedMaterial(child.material);
      }

      const ancestry = this.collectObjectNameTokens(child);
      const materials = Array.isArray(child.material) ? child.material : [child.material];

      materials.forEach((mat: any) => {
        const tokens = `${ancestry} ${this.materialTokenString(mat)}`;
        const isCloth = this.matchesAny(tokens, clothTokens);

        if (colors.eyeHex && this.matchesAny(tokens, eyeTokens)) {
          this.applyMaterialTint(mat, colors.eyeHex, 0.14);
          return;
        }

        if (colors.hairHex && this.matchesAny(tokens, hairTokens)) {
          this.applyMaterialTint(mat, colors.hairHex, 0.08);
          return;
        }

        if (colors.skinHex && this.matchesAny(tokens, skinTokens) && (!isCloth || tokens.includes('skin'))) {
          this.applyMaterialTint(mat, colors.skinHex, 0.06);
        }
      });
    });
  }

  private cloneTintableMaterial(mat: any): any {
    if (!mat || mat.userData?.customTintCloned) return mat;
    const clone = typeof mat.clone === 'function' ? mat.clone() : mat;
    if (clone?.userData) clone.userData.customTintCloned = true;
    return clone;
  }

  private getOptimizedMaterial(mat: any): any {
    if (this.isPotato) {
      return this.convertToPotatoMaterial(mat);
    }
    return this.cloneTintableMaterial(mat);
  }

  private convertToPotatoMaterial(mat: any): any {
    if (!mat) return mat;
    if (mat.isMeshBasicMaterial || mat.isMeshLambertMaterial || mat.userData?.isPotatoConverted) {
      return mat;
    }

    const lambert = new THREE.MeshLambertMaterial({
      color: mat.color ? mat.color.clone() : new THREE.Color(0xffffff),
      map: mat.map || null,
      alphaMap: mat.alphaMap || null,
      aoMap: mat.aoMap || null,
      aoMapIntensity: mat.aoMapIntensity !== undefined ? mat.aoMapIntensity : 1.0,
      opacity: mat.opacity !== undefined ? mat.opacity : 1.0,
      transparent: mat.transparent || false,
      side: mat.side !== undefined ? mat.side : THREE.DoubleSide,
      alphaTest: mat.alphaTest !== undefined ? mat.alphaTest : 0.0,
      visible: mat.visible !== undefined ? mat.visible : true,
    });

    if (mat.emissive && typeof mat.emissive.clone === 'function') {
      lambert.emissive = mat.emissive.clone();
      if (mat.emissiveIntensity !== undefined) {
        lambert.emissive.multiplyScalar(mat.emissiveIntensity);
      }
    }
    if (mat.emissiveMap) {
      lambert.emissiveMap = mat.emissiveMap;
    }

    lambert.userData = {
      ...mat.userData,
      isPotatoConverted: true,
      customTintCloned: true
    };

    if (mat.userData?.customTintCloned) {
      mat.dispose();
    }

    return lambert;
  }

  private disposeModelMaterials(model: THREE.Object3D) {
    model.traverse((child: any) => {
      if (child.isMesh && child.material) {
        const materials = Array.isArray(child.material) ? child.material : [child.material];
        materials.forEach((mat: any) => {
          if (mat.userData?.customTintCloned || mat.userData?.isPotatoConverted) {
            mat.dispose();
          }
        });
      }
    });
  }

  private collectObjectNameTokens(object: THREE.Object3D): string {
    const names: string[] = [];
    let current: THREE.Object3D | null = object;
    while (current && names.length < 6) {
      if (current.name) names.push(current.name);
      current = current.parent;
    }
    return names.join(' ').toLowerCase();
  }

  private materialTokenString(mat: any): string {
    return [
      mat?.name,
      mat?.map?.name,
      mat?.normalMap?.name,
      mat?.userData?.name,
      mat?.userData?.gltfExtensions ? JSON.stringify(mat.userData.gltfExtensions) : ''
    ].filter(Boolean).join(' ').toLowerCase();
  }

  private matchesAny(value: string, tokens: string[]): boolean {
    return tokens.some((token) => value.includes(token));
  }

  private applyMaterialTint(mat: any, hex: string, emissiveStrength: number): void {
    if (!mat?.color || typeof mat.color.set !== 'function') return;
    mat.color.set(hex);
    mat.needsUpdate = true;
    if (mat.emissive && typeof mat.emissive.set === 'function') {
      mat.emissive.set(hex).multiplyScalar(emissiveStrength);
    }
  }

  private alignModelBottom(model: THREE.Object3D, targetY: number) {
    const box = new THREE.Box3().setFromObject(model);
    model.position.y += targetY - box.min.y;
  }

  private findBones(model: THREE.Object3D) {
    let rightArm: THREE.Bone | null = null;
    let rightForeArm: THREE.Bone | null = null;
    let rightHand: THREE.Bone | null = null;
    let leftArm: THREE.Bone | null = null;
    let leftForeArm: THREE.Bone | null = null;
    let leftHand: THREE.Bone | null = null;

    model.traverse((child: any) => {
      if (child.isBone) {
        const name = child.name.toLowerCase();
        
        // --- RIGHT SIDE ---
        if (name.includes('rightarm') || name.includes('rarm') || name.includes('upperarm.r') || name.includes('upperarm_r') || (name.includes('arm.r') && !name.includes('lower'))) {
          rightArm = child;
        } else if (name.includes('rightforearm') || name.includes('rforearm') || name.includes('lowerarm.r') || name.includes('lowerarm_r') || name.includes('arm_r_03') || name.includes('forearm.r')) {
          rightForeArm = child;
        } else if (name.includes('righthand') || name.includes('rhand') || name.includes('hand.r') || name.includes('hand_r')) {
          rightHand = child;
        }
        
        // --- LEFT SIDE ---
        else if (name.includes('leftarm') || name.includes('larm') || name.includes('upperarm.l') || name.includes('upperarm_l') || (name.includes('arm.l') && !name.includes('lower'))) {
          leftArm = child;
        } else if (name.includes('leftforearm') || name.includes('lforearm') || name.includes('lowerarm.l') || name.includes('lowerarm_l') || name.includes('arm_l_03') || name.includes('forearm.l')) {
          leftForeArm = child;
        } else if (name.includes('lefthand') || name.includes('lhand') || name.includes('hand.l') || name.includes('hand_l')) {
          leftHand = child;
        }
      }
    });
    return { rightArm, rightForeArm, rightHand, leftArm, leftForeArm, leftHand };
  }

  private setHandshakeAnimation(
    mixer: THREE.AnimationMixer,
    actions: Map<string, THREE.AnimationAction>,
    state: 'idle' | 'walk'
  ) {
    const active = this.currentAnims.get(mixer);
    if (active && active.state === state) return;

    const hints = state === 'walk' ? ['walking', 'walk'] : ['idle', 'standing', 'house'];
    let chosenAction: THREE.AnimationAction | undefined;

    for (const hint of hints) {
      for (const [name, act] of actions.entries()) {
        if (name.includes(hint)) {
          chosenAction = act;
          break;
        }
      }
      if (chosenAction) break;
    }
    if (!chosenAction) {
      chosenAction = Array.from(actions.values())[0];
    }

    if (chosenAction) {
      chosenAction.reset().fadeIn(0.25).play();
      if (active?.action && active.action !== chosenAction) {
        active.action.fadeOut(0.25);
      }
      this.currentAnims.set(mixer, { state, action: chosenAction });
    }
  }

  private setCelebrationAnimation(mixer: THREE.AnimationMixer, actions: Map<string, THREE.AnimationAction>) {
    const hints = ['dance', 'victory', 'win', 'jump', 'happy', 'wave', 'run'];
    let chosenAction: THREE.AnimationAction | undefined;
    for (const hint of hints) {
      for (const [name, act] of actions.entries()) {
        if (name.includes(hint)) {
          chosenAction = act;
          break;
        }
      }
      if (chosenAction) break;
    }
    if (!chosenAction) {
      for (const [name, act] of actions.entries()) {
        if (!name.includes('idle') && !name.includes('stand')) {
          chosenAction = act;
          break;
        }
      }
    }
    if (chosenAction) {
      actions.forEach(act => act.fadeOut(0.2));
      chosenAction.reset().fadeIn(0.2).play();
    }
  }

  private setDefeatAnimation(mixer: THREE.AnimationMixer, actions: Map<string, THREE.AnimationAction>) {
    const hints = ['lose', 'defeat', 'sad', 'cry', 'idle'];
    let chosenAction: THREE.AnimationAction | undefined;
    for (const hint of hints) {
      for (const [name, act] of actions.entries()) {
        if (name.includes(hint)) {
          chosenAction = act;
          break;
        }
      }
      if (chosenAction) break;
    }
    if (chosenAction) {
      actions.forEach(act => act.fadeOut(0.2));
      chosenAction.reset().fadeIn(0.2).play();
    }
  }

  private applyProceduralArm(
    arm: THREE.Bone | undefined,
    forearm: THREE.Bone | undefined,
    blend: number,
    side: 'player' | 'opponent'
  ) {
    if (!arm) return;

    const targetPitch = 1.15;
    const targetYaw = -1.25;
    const targetForearmYaw = -0.35;

    const defaultArmQ = this.defaultQuaternions.get(arm);
    if (defaultArmQ) {
      const targetQ = defaultArmQ.clone();
      const localX = new THREE.Vector3(1, 0, 0);
      const localZ = new THREE.Vector3(0, 0, 1);

      targetQ.multiply(new THREE.Quaternion().setFromAxisAngle(localX, targetPitch));
      targetQ.multiply(new THREE.Quaternion().setFromAxisAngle(localZ, targetYaw));

      arm.quaternion.slerp(targetQ, blend);
    }

    if (forearm) {
      const defaultForearmQ = this.defaultQuaternions.get(forearm);
      if (defaultForearmQ) {
        const targetQ = defaultForearmQ.clone();
        const localZ = new THREE.Vector3(0, 0, 1);
        targetQ.multiply(new THREE.Quaternion().setFromAxisAngle(localZ, targetForearmYaw));

        forearm.quaternion.slerp(targetQ, blend);
      }
    }
  }

  private triggerHandshakeImpactEffects(): void {
    if (this.handshakeSpotlight) {
      this.handshakeSpotlight.intensity = 40.0;
    }

    this.cameraShakeTime = 0.5;

    if (this.handshakeScene) {
      if (this.handshakeParticleGeometry) {
        this.handshakeParticleGeometry.dispose();
      }
      this.handshakeParticleGeometry = new THREE.SphereGeometry(0.032, 4, 4);

      const goldMaterial = new THREE.MeshBasicMaterial({
        color: 0xfcd34d,
        transparent: true,
        opacity: 0.95
      });
      const cyanMaterial = new THREE.MeshBasicMaterial({
        color: 0x22d3ee,
        transparent: true,
        opacity: 0.95
      });

      const particleCount = this.isPotato ? 12 : 54;
      for (let i = 0; i < particleCount; i++) {
        const isGold = Math.random() > 0.55;
        const color = isGold ? new THREE.Color(0xfcd34d) : new THREE.Color(0x22d3ee);
        const material = isGold ? goldMaterial.clone() : cyanMaterial.clone();
        
        const mesh = new THREE.Mesh(this.handshakeParticleGeometry, material);
        mesh.position.set(0, 0.92, 0.05);

        this.handshakeScene.add(mesh);

        const theta = Math.random() * Math.PI * 2;
        const phi = Math.acos(Math.random() * 2 - 1);
        const speed = 0.6 + Math.random() * 1.8;
        const velocity = new THREE.Vector3(
          Math.sin(phi) * Math.cos(theta) * speed,
          Math.sin(phi) * Math.sin(theta) * speed,
          Math.cos(phi) * speed
        );

        this.handshakeParticles.push({
          mesh,
          velocity,
          color,
          life: 0,
          maxLife: 35 + Math.random() * 35
        });
      }

      goldMaterial.dispose();
      cyanMaterial.dispose();
    }
  }

  private updateHandshakeParticles(dt: number): void {
    for (let i = this.handshakeParticles.length - 1; i >= 0; i--) {
      const p = this.handshakeParticles[i];
      p.life++;
      if (p.life >= p.maxLife) {
        if (this.handshakeScene) {
          this.handshakeScene.remove(p.mesh);
        }
        if (Array.isArray(p.mesh.material)) {
          p.mesh.material.forEach(m => m.dispose());
        } else {
          p.mesh.material.dispose();
        }
        this.handshakeParticles.splice(i, 1);
      } else {
        p.mesh.position.addScaledVector(p.velocity, dt);
        p.velocity.y -= 2.0 * dt; // gravity
        const mat = p.mesh.material as THREE.MeshBasicMaterial;
        mat.opacity = 1 - (p.life / p.maxLife);
      }
    }
  }

  private updateCameraShake(dt: number, camera: THREE.PerspectiveCamera): void {
    if (this.cameraShakeTime > 0) {
      this.cameraShakeTime -= dt;
      const intensity = 0.09 * (this.cameraShakeTime / 0.5);
      camera.position.x = (Math.random() * 2 - 1) * intensity;
      camera.position.y = 0.82 + (Math.random() * 2 - 1) * intensity;
    } else {
      camera.position.set(0, 0.82, 3.8);
    }
  }

  private cleanupHandshakeScene(): void {
    this.canvasInitialized = false;
    if (this.handshakeAnimationId) {
      cancelAnimationFrame(this.handshakeAnimationId);
      this.handshakeAnimationId = undefined;
    }

    this.handshakeParticles.forEach(p => {
      this.handshakeScene?.remove(p.mesh);
      if (Array.isArray(p.mesh.material)) {
        p.mesh.material.forEach(m => m.dispose());
      } else {
        p.mesh.material.dispose();
      }
    });
    this.handshakeParticles = [];

    if (this.handshakeParticleGeometry) {
      this.handshakeParticleGeometry.dispose();
      this.handshakeParticleGeometry = undefined;
    }

    if (this.playerModel) {
      this.disposeModelMaterials(this.playerModel);
      if (this.handshakeScene) {
        this.handshakeScene.remove(this.playerModel);
      }
    }
    if (this.opponentModel) {
      this.disposeModelMaterials(this.opponentModel);
      if (this.handshakeScene) {
        this.handshakeScene.remove(this.opponentModel);
      }
    }

    this.playerModel = undefined;
    this.opponentModel = undefined;
    this.playerMixer = undefined;
    this.opponentMixer = undefined;
    this.playerRightArm = undefined;
    this.playerRightForeArm = undefined;
    this.opponentRightArm = undefined;
    this.opponentRightForeArm = undefined;
    this.defaultQuaternions.clear();
    this.playerActions.clear();
    this.opponentActions.clear();
    this.currentAnims.clear();

    if (this.trainersAnimationId) {
      cancelAnimationFrame(this.trainersAnimationId);
      this.trainersAnimationId = undefined;
    }
    if (this.playerTrainerModel) {
      this.disposeModelMaterials(this.playerTrainerModel);
      this.playerTrainerScene?.remove(this.playerTrainerModel);
      this.playerTrainerModel = undefined;
    }
    if (this.opponentTrainerModel) {
      this.disposeModelMaterials(this.opponentTrainerModel);
      this.opponentTrainerScene?.remove(this.opponentTrainerModel);
      this.opponentTrainerModel = undefined;
    }
    this.playerTrainerMixer = undefined;
    this.opponentTrainerMixer = undefined;
    this.playerTrainerActions.clear();
    this.opponentTrainerActions.clear();
    this.currentTrainerAnims.clear();
    if (this.playerTrainerRenderer) {
      this.playerTrainerRenderer.dispose();
      this.playerTrainerRenderer = undefined;
    }
    if (this.opponentTrainerRenderer) {
      this.opponentTrainerRenderer.dispose();
      this.opponentTrainerRenderer = undefined;
    }
    this.playerTrainerScene = undefined;
    this.opponentTrainerScene = undefined;
    this.playerTrainerCamera = undefined;
    this.opponentTrainerCamera = undefined;
  }

  private initCoinFlipScene(canvas: HTMLCanvasElement): void {
    if (this.loadingCoinModels) return;
    this.loadingCoinModels = true;

    this.coinRebotes = 0;
    this.coinFlipVuelo = false;
    this.coinFlipResultadoListo = false;
    this.coinFlipResultadoEsperado = undefined;
    this.coinFlipParticles = [];
    this.fuerzaActual = 0;
    this.arrastrando = false;
    this.eleccionTemporal = null;
    this.confirmadoLado = false;
    this.coinFlipClock.getDelta(); // reset clock

    const width = canvas.clientWidth || 400;
    const height = canvas.clientHeight || 380;

    const renderer = new THREE.WebGLRenderer({
      canvas: canvas,
      alpha: false, // Desactivar alpha para controlar de forma exacta el fondo de la escena 3D
      antialias: !this.isPotato && window.devicePixelRatio < 2,
      powerPreference: 'high-performance',
      precision: this.isPotato ? 'mediump' : 'highp',
      stencil: false,
      depth: true
    });
    renderer.setSize(width, height, false);
    renderer.setPixelRatio(this.isPotato ? 1.0 : Math.min(window.devicePixelRatio, 1.25));
    renderer.outputColorSpace = THREE.SRGBColorSpace;
    this.coinFlipRenderer = renderer;

    const scene = new THREE.Scene();
    // Fondo azul espacial profundo cinemático
    scene.background = new THREE.Color(0x060d1b);
    this.coinFlipScene = scene;

    // Niebla densa a tono con el fondo espacial
    scene.fog = new THREE.FogExp2(0x060d1b, 0.08);

    const camera = new THREE.PerspectiveCamera(35, width / height, 0.1, 50);
    // Posición general inicial de la cámara
    camera.position.set(2.2, 1.4, 3.8);
    camera.lookAt(0, 0.4, 0);
    this.coinFlipCamera = camera;

    // Controles de órbita interactivos para rotar la cámara libremente
    const controls = new OrbitControls(camera, renderer.domElement);
    controls.enableDamping = true;
    controls.dampingFactor = 0.08;
    controls.enableZoom = false;
    controls.minPolarAngle = Math.PI / 6;
    controls.maxPolarAngle = Math.PI / 2 - 0.05;
    this.coinFlipControls = controls;

    // Suelo de cuadrícula neón ligero
    const gridHelper = new THREE.GridHelper(60, 60, 0x00f0ff, 0x1e293b);
    gridHelper.position.y = -0.9;
    scene.add(gridHelper);

    // Piso físico oscuro
    const floorGeo = new THREE.PlaneGeometry(100, 100);
    const floorMat = new THREE.MeshStandardMaterial({
      color: 0x020408,
      roughness: 0.9,
      metalness: 0.1
    });
    const floorMesh = new THREE.Mesh(floorGeo, floorMat);
    floorMesh.rotation.x = -Math.PI / 2;
    floorMesh.position.y = -0.905;
    scene.add(floorMesh);

    // Iluminación cinemática
    const ambientLight = new THREE.AmbientLight(0xffffff, 0.7);
    scene.add(ambientLight);

    const goldLight = new THREE.DirectionalLight(0xfccd4d, 5.5);
    goldLight.position.set(3.0, 5.0, 3.0);
    scene.add(goldLight);

    const cyanLight = new THREE.DirectionalLight(0x00f0ff, 4.0);
    cyanLight.position.set(-3.0, -1.0, 2.0);
    scene.add(cyanLight);

    const rimLight = new THREE.DirectionalLight(0xff00ff, 2.5);
    rimLight.position.set(0, -2, -3);
    scene.add(rimLight);

    // Cargar modelos
    const loader = new GLTFLoader();
    loader.setMeshoptDecoder(MeshoptDecoder);
    const activeRenderer = renderer;
    if (activeRenderer) {
      const ktx2 = this.getKtx2Loader(activeRenderer);
      loader.setKTX2Loader(ktx2);
    }

    // 1. Cargar jugador local
    this.loadHandshakeCharacter(this.localPlayerCharacterId, (playerModel, playerAnims) => {
      this.playerCoinFlipModel = playerModel;
      this.playerCoinFlipMixer = new THREE.AnimationMixer(playerModel);
      playerAnims.forEach(clip => {
        this.playerCoinFlipActions.set(clip.name.toLowerCase(), this.playerCoinFlipMixer!.clipAction(clip));
      });

      // Aplicar color de piel/pelo configurados en el lobby
      const localSkin = localStorage.getItem('lobbySkinColor') || undefined;
      const localHair = localStorage.getItem('lobbyHairColor') || undefined;
      const localEye = localStorage.getItem('lobbyEyeColor') || undefined;
      let localHeight = 1.0;
      try {
        const hStr = localStorage.getItem('lobbyHeight');
        if (hStr) localHeight = parseFloat(hStr);
      } catch {}
      this.applyModelCustomization(playerModel, this.localPlayerCharacterId, localSkin, localHair, localEye, localHeight);

      // Posicionar al jugador en (0, -0.9, 1.5) mirando al rival (eje Z negativo)
      playerModel.position.set(0, -0.9, 1.5);
      playerModel.rotation.y = Math.PI;

      // Buscar huesos del brazo derecho
      const bones = this.findBones(playerModel);
      this.coinFlipPlayerRightArm = bones.rightArm || undefined;
      this.coinFlipPlayerRightForeArm = bones.rightForeArm || undefined;
      this.coinFlipPlayerRightHand = bones.rightHand || undefined;

      // Respaldar cuaterniones originales
      if (this.coinFlipPlayerRightArm) this.coinFlipDefaultQuaternions.set(this.coinFlipPlayerRightArm, this.coinFlipPlayerRightArm.quaternion.clone());
      if (this.coinFlipPlayerRightForeArm) this.coinFlipDefaultQuaternions.set(this.coinFlipPlayerRightForeArm, this.coinFlipPlayerRightForeArm.quaternion.clone());
      if (this.coinFlipPlayerRightHand) this.coinFlipDefaultQuaternions.set(this.coinFlipPlayerRightHand, this.coinFlipPlayerRightHand.quaternion.clone());

      // Iniciar animación idle
      const hints = ['idle', 'standing', 'house'];
      let idleAction: THREE.AnimationAction | undefined;
      for (const hint of hints) {
        for (const [name, act] of this.playerCoinFlipActions.entries()) {
          if (name.includes(hint)) {
            idleAction = act;
            break;
          }
        }
        if (idleAction) break;
      }
      if (idleAction) {
        idleAction.play();
        this.playerCoinFlipMixer.update(0.001);
      }

      scene.add(playerModel);

      // 2. Cargar oponente bot
      this.loadHandshakeCharacter(this.opponentCharacterId, (opponentModel, opponentAnims) => {
        this.opponentCoinFlipModel = opponentModel;
        this.opponentCoinFlipMixer = new THREE.AnimationMixer(opponentModel);
        opponentAnims.forEach(clip => {
          this.opponentCoinFlipActions.set(clip.name.toLowerCase(), this.opponentCoinFlipMixer!.clipAction(clip));
        });

        const oppSkin = this.opponentSkinColor || undefined;
        const oppHair = this.opponentHairColor || undefined;
        const oppEye = this.opponentEyeColor || undefined;
        const oppHeight = this.opponentHeight || 1.0;
        this.applyModelCustomization(opponentModel, this.opponentCharacterId, oppSkin, oppHair, oppEye, oppHeight);

        // Posicionar bot de frente al jugador
        opponentModel.position.set(0, -0.9, -2.5);
        opponentModel.rotation.y = 0;

        // Iniciar animación idle en el bot
        let botIdleAction: THREE.AnimationAction | undefined;
        for (const hint of hints) {
          for (const [name, act] of this.opponentCoinFlipActions.entries()) {
            if (name.includes(hint)) {
              botIdleAction = act;
              break;
            }
          }
          if (botIdleAction) break;
        }
        if (botIdleAction) {
          botIdleAction.play();
        }

        scene.add(opponentModel);

        // 3. Cargar moneda 3D
        loader.load('/assets/models/video_game_coin.glb', (gltfCoin) => {
          const coinGroup = new THREE.Group();
          this.coinFlipCoinModel = coinGroup;

          const coinMesh = gltfCoin.scene;

          // Centrado seguro global de la geometría de la moneda
          const coinBox = new THREE.Box3().setFromObject(coinMesh);
          const center = new THREE.Vector3();
          coinBox.getCenter(center);
          coinMesh.position.sub(center);

          const coinSize = new THREE.Vector3();
          coinBox.getSize(coinSize);
          const maxCoinDim = Math.max(coinSize.x, coinSize.y, coinSize.z);
          if (maxCoinDim > 0) {
            const coinScale = 0.22 / maxCoinDim;
            coinMesh.scale.setScalar(coinScale);
          }
          // NOT adding the original GLB coinMesh to prevent duplicate geometry (star vs Cara/Cruz)
          
          const espesor = coinSize.z * (0.22 / maxCoinDim);
          const radio = (Math.max(coinSize.x, coinSize.y) / 2) * (0.22 / maxCoinDim);

          // Crear un canto dorado brillante para el cuerpo de la moneda
          const cantoGeom = new THREE.CylinderGeometry(radio, radio, espesor, 32);
          const cantoMat = new THREE.MeshStandardMaterial({
            color: 0xdca81e, // Color dorado metálico
            metalness: 0.95,
            roughness: 0.08
          });
          const cantoMesh = new THREE.Mesh(cantoGeom, cantoMat);
          cantoMesh.rotation.x = Math.PI / 2; // Orientar cilindro de canto
          coinGroup.add(cantoMesh);

          const circleGeom = new THREE.CircleGeometry(radio * 0.95, 32);
          
          // Relieve 3D para la Cara
          const caraBumpCanvas = this.crearBumpMapCara();
          const caraBumpTex = new THREE.CanvasTexture(caraBumpCanvas);

          const caraMat = new THREE.MeshStandardMaterial({
            map: this.crearTexturaCara(),
            bumpMap: caraBumpTex,
            bumpScale: 0.008,
            roughness: 0.22,
            metalness: 0.9,
            side: THREE.FrontSide
          });

          // Relieve 3D para la Cruz
          const cruzBumpCanvas = this.crearBumpMapCruz();
          const cruzBumpTex = new THREE.CanvasTexture(cruzBumpCanvas);

          const cruzMat = new THREE.MeshStandardMaterial({
            map: this.crearTexturaCruz(),
            bumpMap: cruzBumpTex,
            bumpScale: 0.008,
            roughness: 0.22,
            metalness: 0.9,
            side: THREE.FrontSide
          });

          const caraMesh = new THREE.Mesh(circleGeom, caraMat);
          caraMesh.position.set(0, 0, espesor / 2 + 0.002);

          const cruzMesh = new THREE.Mesh(circleGeom, cruzMat);
          cruzMesh.position.set(0, 0, -(espesor / 2 + 0.002));
          cruzMesh.rotation.y = Math.PI;

          coinGroup.add(caraMesh);
          coinGroup.add(cruzMesh);

          // Posicionar sobre el pulgar estimado inicialmente
          coinGroup.position.set(0.18, 0.0, 1.1);
          scene.add(coinGroup);

          this.loadingCoinModels = false;
          this.cdr.detectChanges();
        }, undefined, (err: any) => {
          console.error('Error cargando moneda 3D:', err);
          this.loadingCoinModels = false;
        });

      }, renderer);

    }, renderer);

    const targetCameraPos = new THREE.Vector3(2.2, 1.4, 3.8);
    const targetCameraLook = new THREE.Vector3(0, 0.4, 0);
    const currentCameraLook = new THREE.Vector3(0, 0.4, 0);
    let coinTargetRotY = Math.PI / 2;

    // Iniciar loop de animación
    const animate = () => {
      if (!this.coinFlipCanvasInitialized) return;
      this.coinFlipAnimationId = requestAnimationFrame(animate);

      const dt = Math.min(this.coinFlipClock.getDelta(), 0.03);

      const currWidth = canvas.clientWidth || 400;
      const currHeight = canvas.clientHeight || 380;
      if (canvas.width !== currWidth || canvas.height !== currHeight) {
        renderer.setSize(currWidth, currHeight, false);
        camera.aspect = currWidth / currHeight;
        camera.updateProjectionMatrix();
      }

      // Actualizar mixers de animación de los personajes
      if (this.playerCoinFlipMixer) {
        if (this.coinFlipVuelo || this.coinFlipResultadoListo) {
          this.playerCoinFlipMixer.update(dt);
        }
      }
      if (this.opponentCoinFlipMixer) this.opponentCoinFlipMixer.update(dt);

      if (this.coinFlipControls && this.coinFlipControls.enabled) {
        this.coinFlipControls.update();
      }

      // Obtener posición absoluta de la mano derecha del jugador
      const handWorldPos = new THREE.Vector3();
      if (this.coinFlipPlayerRightHand) {
        this.coinFlipPlayerRightHand.getWorldPosition(handWorldPos);
      } else {
        handWorldPos.set(0.2, 0.0, 1.1);
      }

      // --- PROCEDURAL ARM POSING (ESTIRADO/TOMAR IMPULSO/LANZAMIENTO PROCEDIMENTAL) ---
      if (!this.coinFlipVuelo) {
        if (this.arrastrando) {
          const tension = this.fuerzaActual / 400; // 0 a 1
          // Tomar impulso inclinando el brazo hacia atrás y abajo proporcionalmente
          if (this.coinFlipPlayerRightArm) {
            const armQ = this.coinFlipDefaultQuaternions.get(this.coinFlipPlayerRightArm)?.clone();
            if (armQ) {
              // A mayor tensión, más hacia atrás en X y más abierto en Z
              armQ.multiply(new THREE.Quaternion().setFromAxisAngle(new THREE.Vector3(1, 0, 0), -0.6 * tension)); 
              armQ.multiply(new THREE.Quaternion().setFromAxisAngle(new THREE.Vector3(0, 0, 1), -0.7 * tension)); 
              this.coinFlipPlayerRightArm.quaternion.slerp(armQ, 0.15);
            }
          }
          if (this.coinFlipPlayerRightForeArm) {
            const foreQ = this.coinFlipDefaultQuaternions.get(this.coinFlipPlayerRightForeArm)?.clone();
            if (foreQ) {
              // Doblar el codo proporcionalmente
              foreQ.multiply(new THREE.Quaternion().setFromAxisAngle(new THREE.Vector3(0, 0, 1), -1.1 * tension)); 
              this.coinFlipPlayerRightForeArm.quaternion.slerp(foreQ, 0.15);
            }
          }
        } else {
          // Brazo estirado hacia adelante listo para tirar
          if (this.coinFlipPlayerRightArm) {
            const armQ = this.coinFlipDefaultQuaternions.get(this.coinFlipPlayerRightArm)?.clone();
            if (armQ) {
              armQ.multiply(new THREE.Quaternion().setFromAxisAngle(new THREE.Vector3(1, 0, 0), 1.0)); 
              armQ.multiply(new THREE.Quaternion().setFromAxisAngle(new THREE.Vector3(0, 0, 1), -1.1)); 
              this.coinFlipPlayerRightArm.quaternion.slerp(armQ, 0.1);
            }
          }
          if (this.coinFlipPlayerRightForeArm) {
            const foreQ = this.coinFlipDefaultQuaternions.get(this.coinFlipPlayerRightForeArm)?.clone();
            if (foreQ) {
              foreQ.multiply(new THREE.Quaternion().setFromAxisAngle(new THREE.Vector3(0, 0, 1), -0.3)); 
              this.coinFlipPlayerRightForeArm.quaternion.slerp(foreQ, 0.1);
            }
          }
        }
      } else {
        // En pleno vuelo: simulamos el lanzamiento procedimental en los primeros 0.20 segundos
        this.coinFlipReleaseTime += dt;
        if (this.coinFlipReleaseTime < 0.22) {
          const progress = Math.min(this.coinFlipReleaseTime / 0.12, 1.0); // Flick ultra rápido
          if (this.coinFlipPlayerRightArm) {
            const armQ = this.coinFlipDefaultQuaternions.get(this.coinFlipPlayerRightArm)?.clone();
            if (armQ) {
              // Brazo extendido hacia arriba y al frente con fuerza
              armQ.multiply(new THREE.Quaternion().setFromAxisAngle(new THREE.Vector3(1, 0, 0), 1.55)); 
              armQ.multiply(new THREE.Quaternion().setFromAxisAngle(new THREE.Vector3(0, 0, 1), -1.45)); 
              this.coinFlipPlayerRightArm.quaternion.slerp(armQ, progress);
            }
          }
          if (this.coinFlipPlayerRightForeArm) {
            const foreQ = this.coinFlipDefaultQuaternions.get(this.coinFlipPlayerRightForeArm)?.clone();
            if (foreQ) {
              // Codo casi estirado en el flick
              foreQ.multiply(new THREE.Quaternion().setFromAxisAngle(new THREE.Vector3(0, 0, 1), -0.1)); 
              this.coinFlipPlayerRightForeArm.quaternion.slerp(foreQ, progress);
            }
          }
        } else {
          // Retornar brazo suavemente a pose idle
          if (this.coinFlipPlayerRightArm) {
            const armQ = this.coinFlipDefaultQuaternions.get(this.coinFlipPlayerRightArm);
            if (armQ) this.coinFlipPlayerRightArm.quaternion.slerp(armQ, 0.08);
          }
          if (this.coinFlipPlayerRightForeArm) {
            const foreQ = this.coinFlipDefaultQuaternions.get(this.coinFlipPlayerRightForeArm);
            if (foreQ) this.coinFlipPlayerRightForeArm.quaternion.slerp(foreQ, 0.08);
          }
        }
      }

      // --- POSICIÓN Y ROTACIÓN DE LA MONEDA EN REPOSO ---
      if (!this.coinFlipVuelo && !this.coinFlipResultadoListo && this.coinFlipCoinModel) {
        const shakeIntensity = this.arrastrando ? (this.fuerzaActual / 400) * 0.04 : 0;
        const shakeX = Math.sin(Date.now() * 0.18) * shakeIntensity;
        const shakeY = Math.cos(Date.now() * 0.21) * shakeIntensity;
        const shakeZ = Math.sin(Date.now() * 0.24) * shakeIntensity;

        // Situar moneda sobre la mano
        this.coinFlipCoinModel.position.copy(handWorldPos).add(new THREE.Vector3(0.02 + shakeX, 0.05 + shakeY, -0.05 + shakeZ));

        // Rotación de canto o mostrando cara/cruz
        if (!this.confirmadoLado) {
          let rotX = 0;
          let rotY = 0;
          if (this.eleccionTemporal === 'CARA') {
            rotX = -Math.PI / 2; // Cara mirando arriba
          } else if (this.eleccionTemporal === 'CRUZ') {
            rotX = Math.PI / 2;  // Cruz mirando arriba
          } else {
            rotX = 0; // De canto vertical neutro
            rotY = Math.PI / 2;
          }
          
          let targetQuat = new THREE.Quaternion().setFromEuler(new THREE.Euler(rotX, rotY, 0));
          this.coinFlipCoinModel.quaternion.slerp(targetQuat, 0.1);
        } else {
          // Si ya está confirmado, la moneda reposa horizontal en la mano para tirar (con Cara arriba)
          let targetQuat = new THREE.Quaternion().setFromEuler(new THREE.Euler(-Math.PI / 2, 0, 0));
          this.coinFlipCoinModel.quaternion.slerp(targetQuat, 0.1);
        }
      }

      // --- CONTROL DE CÁMARA CINEMÁTICA ---
      if (this.coinFlipVuelo && this.coinFlipCoinModel) {
        // 1. Fase de vuelo libre: la cámara sigue la trayectoria en espiral
        this.coinVy -= 18.0 * dt;

        this.coinFlipCoinModel.position.x += this.coinVx * dt;
        this.coinFlipCoinModel.position.y += this.coinVy * dt;
        this.coinFlipCoinModel.position.z += this.coinVz * dt;

        this.coinFlipCoinModel.rotateX(this.coinOmegaX * dt);
        this.coinFlipCoinModel.rotateY(this.coinOmegaY * dt);
        this.coinFlipCoinModel.rotateZ(this.coinOmegaZ * dt);

        if (Math.random() < 0.65) {
          this.emitirParticulaCoin(this.coinFlipCoinModel.position);
        }

        // Seguir a la moneda: la cámara se coloca detrás de la moneda en su trayectoria de vuelo y la sigue de cerca
        targetCameraPos.set(
          this.coinFlipCoinModel.position.x,
          this.coinFlipCoinModel.position.y + 0.6,
          this.coinFlipCoinModel.position.z + 1.2
        );
        
        camera.position.lerp(targetCameraPos, 0.12);
        camera.lookAt(this.coinFlipCoinModel.position);

        if (this.coinVy < 0 && this.coinFlipCoinModel.position.y <= -0.9) {
          if (this.coinRebotes < 2) {
            this.coinFlipCoinModel.position.y = -0.9;
            this.coinVy = -this.coinVy * 0.35;
            this.coinVx *= 0.4;
            this.coinVz *= 0.4;
            this.coinOmegaX *= 0.35;
            this.coinOmegaY *= 0.35;
            this.coinOmegaZ *= 0.35;
            this.coinRebotes++;
          } else {
            this.coinFlipVuelo = false;
            this.coinFlipCoinModel.position.y = -0.9;
            if (this.coinFlipControls) {
              this.coinFlipControls.enabled = true;
              this.coinFlipControls.target.copy(this.coinFlipCoinModel.position);
            }
            this.girando = false;
            this.cdr.detectChanges();
          }
        }
      } else if (this.coinFlipResultadoListo && this.coinFlipCoinModel) {
        // 2. Resultado revelado en el suelo: zoom dramático de primer plano
        let targetQuat = new THREE.Quaternion();
        if (this.coinFlipResultadoEsperado === 'CARA') {
          targetQuat.setFromEuler(new THREE.Euler(-Math.PI / 2, 0, 0));
        } else {
          targetQuat.setFromEuler(new THREE.Euler(Math.PI / 2, 0, 0));
        }
        this.coinFlipCoinModel.quaternion.slerp(targetQuat, 0.15);

        targetCameraPos.set(this.coinFlipCoinModel.position.x, -0.6, this.coinFlipCoinModel.position.z + 0.65);
        targetCameraLook.copy(this.coinFlipCoinModel.position);
        
        camera.position.lerp(targetCameraPos, 0.08);
        currentCameraLook.lerp(targetCameraLook, 0.08);
        camera.lookAt(currentCameraLook);
      } else {
        // 3. Fase estática e interactiva de selección o espera
        if (this.confirmadoLado) {
          if (this.arrastrando) {
            const tension = this.fuerzaActual / 400; // 0 a 1
            // Alejar ligeramente la cámara al tensionar el tiro (mayor Z y un poco más alta en Y)
            targetCameraPos.set(0.6, 0.5 + tension * 0.4, 2.7 + tension * 0.9);
            targetCameraLook.set(0.0, -0.2, -0.5);
          } else {
            // Pose detrás del hombro (Tercera persona listo para lanzar)
            targetCameraPos.set(0.6, 0.5, 2.7);
            targetCameraLook.set(0.0, -0.2, -0.5);
          }
        } else if (this.eleccionTemporal) {
          // Zoom dramático a la mano y moneda
          targetCameraPos.copy(handWorldPos).add(new THREE.Vector3(0.05, 0.12, 0.42));
          targetCameraLook.copy(handWorldPos);
        } else {
          // Vista general inicial
          targetCameraPos.set(2.2, 1.4, 3.8);
          targetCameraLook.set(0, 0.4, 0);
        }

        camera.position.lerp(targetCameraPos, 0.08);
        currentCameraLook.lerp(targetCameraLook, 0.08);
        camera.lookAt(currentCameraLook);
      }

      this.actualizarParticulasCoin(dt);

      renderer.render(scene, camera);
    };

    animate();
  }

  private emitirParticulaCoin(pos: THREE.Vector3): void {
    if (!this.coinFlipScene) return;
    const color = Math.random() > 0.5 ? 0xfccd4d : 0x00f0ff;
    const geom = new THREE.SphereGeometry(0.03 + Math.random() * 0.03, 6, 6);
    const mat = new THREE.MeshBasicMaterial({
      color: color,
      transparent: true,
      opacity: 0.85
    });
    const mesh = new THREE.Mesh(geom, mat);
    mesh.position.copy(pos);
    mesh.position.x += (Math.random() - 0.5) * 0.12;
    mesh.position.y += (Math.random() - 0.5) * 0.12;
    mesh.position.z += (Math.random() - 0.5) * 0.12;

    this.coinFlipScene.add(mesh);

    this.coinFlipParticles.push({
      mesh,
      vx: (Math.random() - 0.5) * 0.6,
      vy: (Math.random() - 0.5) * 0.6 - 0.3,
      vz: (Math.random() - 0.5) * 0.6,
      life: 0,
      maxLife: 25 + Math.random() * 15
    });
  }

  private actualizarParticulasCoin(dt: number): void {
    for (let i = this.coinFlipParticles.length - 1; i >= 0; i--) {
      const p = this.coinFlipParticles[i];
      p.life++;
      if (p.life >= p.maxLife) {
        this.coinFlipScene?.remove(p.mesh);
        p.mesh.geometry.dispose();
        if (Array.isArray(p.mesh.material)) {
          p.mesh.material.forEach(m => m.dispose());
        } else {
          p.mesh.material.dispose();
        }
        this.coinFlipParticles.splice(i, 1);
      } else {
        const ratio = 1.0 - p.life / p.maxLife;
        p.mesh.position.x += p.vx * dt;
        p.mesh.position.y += p.vy * dt;
        p.mesh.position.z += p.vz * dt;
        p.mesh.scale.setScalar(ratio);
        if (!Array.isArray(p.mesh.material)) {
          p.mesh.material.opacity = ratio * 0.85;
        }
      }
    }
  }

  private cleanupCoinFlipScene(): void {
    this.coinFlipCanvasInitialized = false;
    this.confirmadoLado = false;
    this.eleccionTemporal = null;

    if (this.coinFlipAnimationId) {
      cancelAnimationFrame(this.coinFlipAnimationId);
      this.coinFlipAnimationId = undefined;
    }

    if (this.coinFlipControls) {
      this.coinFlipControls.dispose();
      this.coinFlipControls = undefined;
    }

    this.coinFlipParticles.forEach(p => {
      this.coinFlipScene?.remove(p.mesh);
      p.mesh.geometry.dispose();
      if (Array.isArray(p.mesh.material)) {
        p.mesh.material.forEach(m => m.dispose());
      } else {
        p.mesh.material.dispose();
      }
    });
    this.coinFlipParticles = [];

    if (this.playerCoinFlipModel) {
      this.disposeModelMaterials(this.playerCoinFlipModel);
      this.coinFlipScene?.remove(this.playerCoinFlipModel);
      this.playerCoinFlipModel = undefined;
    }

    if (this.opponentCoinFlipModel) {
      this.disposeModelMaterials(this.opponentCoinFlipModel);
      this.coinFlipScene?.remove(this.opponentCoinFlipModel);
      this.opponentCoinFlipModel = undefined;
    }

    this.playerCoinFlipMixer = undefined;
    this.opponentCoinFlipMixer = undefined;
    this.playerCoinFlipActions.clear();
    this.opponentCoinFlipActions.clear();
    this.coinFlipPlayerRightArm = undefined;
    this.coinFlipPlayerRightForeArm = undefined;
    this.coinFlipPlayerRightHand = undefined;
    this.coinFlipDefaultQuaternions.clear();

    if (this.coinFlipCoinModel) {
      this.disposeModelMaterials(this.coinFlipCoinModel);
      this.coinFlipScene?.remove(this.coinFlipCoinModel);
      this.coinFlipCoinModel = undefined;
    }

    if (this.coinFlipRenderer) {
      this.coinFlipRenderer.dispose();
      this.coinFlipRenderer = undefined;
    }

    this.coinFlipScene = undefined;
    this.coinFlipCamera = undefined;
  }

  private crearTexturaCara(): THREE.CanvasTexture {
    const canvas = document.createElement('canvas');
    canvas.width = 256;
    canvas.height = 256;
    const ctx = canvas.getContext('2d')!;

    const gradient = ctx.createRadialGradient(128, 128, 10, 128, 128, 128);
    gradient.addColorStop(0, '#ffe066');
    gradient.addColorStop(0.5, '#f59e0b');
    gradient.addColorStop(1, '#b45309');
    ctx.fillStyle = gradient;
    ctx.beginPath();
    ctx.arc(128, 128, 128, 0, Math.PI * 2);
    ctx.fill();

    ctx.strokeStyle = '#fef08a';
    ctx.lineWidth = 14;
    ctx.stroke();

    ctx.strokeStyle = '#d97706';
    ctx.lineWidth = 4;
    ctx.beginPath();
    ctx.arc(128, 128, 110, 0, Math.PI * 2);
    ctx.stroke();

    ctx.fillStyle = '#fef08a';
    ctx.shadowColor = '#78350f';
    ctx.shadowBlur = 10;
    ctx.font = 'bold 80px "Outfit", "Inter", sans-serif';
    ctx.textAlign = 'center';
    ctx.textBaseline = 'middle';
    ctx.fillText('CARA', 128, 128);

    ctx.font = '28px sans-serif';
    ctx.shadowBlur = 0;
    ctx.fillText('★', 128, 55);
    ctx.fillText('★', 128, 201);

    const texture = new THREE.CanvasTexture(canvas);
    texture.colorSpace = THREE.SRGBColorSpace;
    return texture;
  }

  private crearTexturaCruz(): THREE.CanvasTexture {
    const canvas = document.createElement('canvas');
    canvas.width = 256;
    canvas.height = 256;
    const ctx = canvas.getContext('2d')!;

    const gradient = ctx.createRadialGradient(128, 128, 10, 128, 128, 128);
    gradient.addColorStop(0, '#e2e8f0');
    gradient.addColorStop(0.5, '#64748b');
    gradient.addColorStop(1, '#1e293b');
    ctx.fillStyle = gradient;
    ctx.beginPath();
    ctx.arc(128, 128, 128, 0, Math.PI * 2);
    ctx.fill();

    ctx.strokeStyle = '#cbd5e1';
    ctx.lineWidth = 14;
    ctx.stroke();

    ctx.strokeStyle = '#475569';
    ctx.lineWidth = 4;
    ctx.beginPath();
    ctx.arc(128, 128, 110, 0, Math.PI * 2);
    ctx.stroke();

    ctx.fillStyle = '#f1f5f9';
    ctx.shadowColor = '#0f172a';
    ctx.shadowBlur = 10;
    ctx.font = 'bold 80px "Outfit", "Inter", sans-serif';
    ctx.textAlign = 'center';
    ctx.textBaseline = 'middle';
    ctx.fillText('CRUZ', 128, 128);

    ctx.font = '28px sans-serif';
    ctx.shadowBlur = 0;
    ctx.fillText('✦', 128, 55);
    ctx.fillText('✦', 128, 201);

    const texture = new THREE.CanvasTexture(canvas);
    texture.colorSpace = THREE.SRGBColorSpace;
    return texture;
  }

  private crearBumpMapCara(): HTMLCanvasElement {
    const canvas = document.createElement('canvas');
    canvas.width = 256;
    canvas.height = 256;
    const ctx = canvas.getContext('2d')!;

    // Fondo gris base
    ctx.fillStyle = '#666666';
    ctx.beginPath();
    ctx.arc(128, 128, 128, 0, Math.PI * 2);
    ctx.fill();

    // Borde exterior elevado
    ctx.strokeStyle = '#ffffff';
    ctx.lineWidth = 14;
    ctx.stroke();

    // Borde fino interior
    ctx.strokeStyle = '#999999';
    ctx.lineWidth = 4;
    ctx.beginPath();
    ctx.arc(128, 128, 110, 0, Math.PI * 2);
    ctx.stroke();

    // Textos elevados
    ctx.fillStyle = '#ffffff';
    ctx.font = 'bold 80px "Outfit", "Inter", sans-serif';
    ctx.textAlign = 'center';
    ctx.textBaseline = 'middle';
    ctx.fillText('CARA', 128, 128);

    ctx.font = '28px sans-serif';
    ctx.fillText('★', 128, 55);
    ctx.fillText('★', 128, 201);

    return canvas;
  }

  private crearBumpMapCruz(): HTMLCanvasElement {
    const canvas = document.createElement('canvas');
    canvas.width = 256;
    canvas.height = 256;
    const ctx = canvas.getContext('2d')!;

    // Fondo gris base
    ctx.fillStyle = '#666666';
    ctx.beginPath();
    ctx.arc(128, 128, 128, 0, Math.PI * 2);
    ctx.fill();

    // Borde exterior elevado
    ctx.strokeStyle = '#ffffff';
    ctx.lineWidth = 14;
    ctx.stroke();

    // Borde fino interior
    ctx.strokeStyle = '#999999';
    ctx.lineWidth = 4;
    ctx.beginPath();
    ctx.arc(128, 128, 110, 0, Math.PI * 2);
    ctx.stroke();

    // Textos elevados
    ctx.fillStyle = '#ffffff';
    ctx.font = 'bold 80px "Outfit", "Inter", sans-serif';
    ctx.textAlign = 'center';
    ctx.textBaseline = 'middle';
    ctx.fillText('CRUZ', 128, 128);

    ctx.font = '28px sans-serif';
    ctx.fillText('✦', 128, 55);
    ctx.fillText('✦', 128, 201);

    return canvas;
  }

  private initPlayerTrainerScene(canvas: HTMLCanvasElement): void {
    const width = canvas.clientWidth || 80;
    const height = canvas.clientHeight || 110;

    const renderer = new THREE.WebGLRenderer({
      canvas: canvas,
      alpha: true,
      antialias: !this.isPotato && window.devicePixelRatio < 2,
      powerPreference: 'high-performance',
      precision: this.isPotato ? 'mediump' : 'highp',
      stencil: false,
      depth: true
    });
    renderer.setSize(width, height, false);
    renderer.setPixelRatio(this.isPotato ? 1.0 : Math.min(window.devicePixelRatio, 1.25));
    this.playerTrainerRenderer = renderer;

    const scene = new THREE.Scene();
    this.playerTrainerScene = scene;

    const camera = new THREE.PerspectiveCamera(35, width / height, 0.1, 10);
    camera.position.set(0, 0.85, 2.5);
    camera.lookAt(0, 0.85, 0);
    this.playerTrainerCamera = camera;

    const ambientLight = new THREE.AmbientLight(0xffffff, 0.85);
    scene.add(ambientLight);

    const dirLight = new THREE.DirectionalLight(0xffffff, 0.6);
    dirLight.position.set(1, 2, 1);
    scene.add(dirLight);

    let localSkin = localStorage.getItem('lobbySkinColor') || undefined;
    let localHair = localStorage.getItem('lobbyHairColor') || undefined;
    let localEye = localStorage.getItem('lobbyEyeColor') || undefined;
    let localHeight = 1.0;
    try {
      const hStr = localStorage.getItem('lobbyHeight');
      if (hStr) localHeight = parseFloat(hStr);
    } catch {}

    this.loadHandshakeCharacter(this.localPlayerCharacterId, (model, animations) => {
      this.playerTrainerModel = model;
      this.playerTrainerMixer = new THREE.AnimationMixer(model);
      animations.forEach(clip => {
        this.playerTrainerActions.set(clip.name.toLowerCase(), this.playerTrainerMixer!.clipAction(clip));
      });

      this.applyModelCustomization(
        model,
        this.localPlayerCharacterId,
        localSkin,
        localHair,
        localEye,
        localHeight
      );

      model.rotation.y = Math.PI * 4 / 5;
      scene.add(model);

      this.playTrainerIdle('player');
    }, renderer);

    this.startTrainersAnimationLoop();
  }

  private initOpponentTrainerScene(canvas: HTMLCanvasElement): void {
    const width = canvas.clientWidth || 80;
    const height = canvas.clientHeight || 110;

    const renderer = new THREE.WebGLRenderer({
      canvas: canvas,
      alpha: true,
      antialias: !this.isPotato && window.devicePixelRatio < 2,
      powerPreference: 'high-performance',
      precision: this.isPotato ? 'mediump' : 'highp',
      stencil: false,
      depth: true
    });
    renderer.setSize(width, height, false);
    renderer.setPixelRatio(this.isPotato ? 1.0 : Math.min(window.devicePixelRatio, 1.25));
    this.opponentTrainerRenderer = renderer;

    const scene = new THREE.Scene();
    this.opponentTrainerScene = scene;

    const camera = new THREE.PerspectiveCamera(35, width / height, 0.1, 10);
    camera.position.set(0, 0.85, 2.5);
    camera.lookAt(0, 0.85, 0);
    this.opponentTrainerCamera = camera;

    const ambientLight = new THREE.AmbientLight(0xffffff, 0.85);
    scene.add(ambientLight);

    const dirLight = new THREE.DirectionalLight(0xffffff, 0.6);
    dirLight.position.set(-1, 2, 1);
    scene.add(dirLight);

    const oppSkin = this.opponentSkinColor || undefined;
    const oppHair = this.opponentHairColor || undefined;
    const oppEye = this.opponentEyeColor || undefined;
    const oppHeight = this.opponentHeight || 1.0;

    const loadingId = this.opponentCharacterId;
    this.loadHandshakeCharacter(loadingId, (model, animations) => {
      if (loadingId !== this.opponentCharacterId) {
        this.disposeModelMaterials(model);
        return;
      }
      this.opponentTrainerModel = model;
      this.opponentTrainerMixer = new THREE.AnimationMixer(model);
      animations.forEach(clip => {
        this.opponentTrainerActions.set(clip.name.toLowerCase(), this.opponentTrainerMixer!.clipAction(clip));
      });

      this.applyModelCustomization(
        model,
        this.opponentCharacterId,
        oppSkin,
        oppHair,
        oppEye,
        oppHeight
      );

      model.rotation.y = -Math.PI / 5;
      scene.add(model);

      this.playTrainerIdle('opponent');
    }, renderer);

    this.startTrainersAnimationLoop();
  }

  private startTrainersAnimationLoop(): void {
    if (this.trainersAnimationId) return;
    this.trainersClock.getDelta();
    
    const animate = () => {
      this.trainersAnimationId = requestAnimationFrame(animate);
      const dt = this.trainersClock.getDelta();
      
      if (this.playerTrainerMixer) this.playerTrainerMixer.update(dt);
      if (this.opponentTrainerMixer) this.opponentTrainerMixer.update(dt);
      
      if (this.playerTrainerRenderer && this.playerTrainerScene && this.playerTrainerCamera) {
        this.playerTrainerRenderer.render(this.playerTrainerScene, this.playerTrainerCamera);
      }
      if (this.opponentTrainerRenderer && this.opponentTrainerScene && this.opponentTrainerCamera) {
        this.opponentTrainerRenderer.render(this.opponentTrainerScene, this.opponentTrainerCamera);
      }
    };
    animate();
  }

  private getAttackAnimationName(actions: Map<string, THREE.AnimationAction>): string | undefined {
    const attackHints = ['attack', 'point', 'cheer', 'dance', 'kick', 'punch', 'action', 'wave', 'greeting', 'jump'];
    for (const hint of attackHints) {
      for (const key of actions.keys()) {
        if (key.includes(hint)) {
          return key;
        }
      }
    }
    for (const key of actions.keys()) {
      if (key.includes('walk') || key.includes('run')) {
        return key;
      }
    }
    return Array.from(actions.keys())[0];
  }

  triggerTrainerAttack(side: 'player' | 'opponent'): void {
    const mixer = side === 'player' ? this.playerTrainerMixer : this.opponentTrainerMixer;
    const actions = side === 'player' ? this.playerTrainerActions : this.opponentTrainerActions;
    if (!mixer || !actions) return;

    const attackAnimName = this.getAttackAnimationName(actions);
    if (!attackAnimName) return;

    const action = actions.get(attackAnimName);
    if (!action) return;

    const current = this.currentTrainerAnims.get(mixer);
    if (current && current.state === 'attack') return;

    action.reset();
    action.setLoop(THREE.LoopOnce, 1);
    action.clampWhenFinished = false;
    action.fadeIn(0.15).play();

    if (current && current.action !== action) {
      current.action.fadeOut(0.15);
    }
    this.currentTrainerAnims.set(mixer, { state: 'attack', action });

    const onFinished = (e: any) => {
      if (e.action === action) {
        mixer.removeEventListener('finished', onFinished);
        this.playTrainerIdle(side);
      }
    };
    mixer.addEventListener('finished', onFinished);
  }

  playTrainerIdle(side: 'player' | 'opponent'): void {
    const mixer = side === 'player' ? this.playerTrainerMixer : this.opponentTrainerMixer;
    const actions = side === 'player' ? this.playerTrainerActions : this.opponentTrainerActions;
    if (!mixer || !actions) return;

    const active = this.currentTrainerAnims.get(mixer);
    if (active && active.state === 'idle') return;

    const hints = ['idle', 'standing', 'house'];
    let chosenAction: THREE.AnimationAction | undefined;

    for (const hint of hints) {
      chosenAction = actions.get(hint);
      if (chosenAction) break;
    }
    if (!chosenAction) {
      chosenAction = Array.from(actions.values())[0];
    }

    if (chosenAction) {
      chosenAction.reset().fadeIn(0.25).play();
      if (active?.action && active.action !== chosenAction) {
        active.action.fadeOut(0.25);
      }
      this.currentTrainerAnims.set(mixer, { state: 'idle', action: chosenAction });
    }
  }

  private programarFinDePartida(partida: Partida, delayMs = 0): void {
    if (this.showEndGameOverlay) return;
    if (this.pendingEndGameTimeout) {
      clearTimeout(this.pendingEndGameTimeout);
    }
    this.pendingEndGameTimeout = setTimeout(() => {
      this.pendingEndGameTimeout = null;
      this.handleGameEnd(partida);
    }, Math.max(0, delayMs));
  }

  private activarEfectoCambio(objetivo: 'bot' | 'jugador'): void {
    if (objetivo === 'bot') {
      this.switchFxBot = true;
      setTimeout(() => {
        this.switchFxBot = false;
        this.cdr.detectChanges();
      }, 550);
    } else {
      this.switchFxPlayer = true;
      setTimeout(() => {
        this.switchFxPlayer = false;
        this.cdr.detectChanges();
      }, 550);
    }
    this.cdr.detectChanges();
  }

  private obtenerFirmaActivo(carta: CartaEnJuego | null | undefined): string {
    if (!carta?.card) return '';
    return [
      carta.card.id,
      carta.hpActual,
      carta.energiasUnidas?.length || 0,
      (carta.condicionesEspeciales || []).join(','),
    ].join('|');
  }

  private getLocalizedStatusLabel(status: 'Poisoned' | 'Burned'): string {
    const language = this.i18n.currentLanguage();
    if (status === 'Poisoned') {
      switch (language) {
        case 'en':
          return 'POISONED';
        case 'pt':
          return 'ENVENENADO';
        case 'ja':
          return 'どく';
        default:
          return 'ENVENENADO';
      }
    }

    switch (language) {
      case 'en':
        return 'BURNED';
      case 'pt':
        return 'QUEIMADO';
      case 'ja':
        return 'やけど';
      default:
        return 'QUEMADO';
    }
  }

  private delay(ms: number): Promise<void> {
    return new Promise((resolve) => setTimeout(resolve, ms));
  }

  private initVersusScene(canvas: HTMLCanvasElement): void {
    this.cleanupVersusScene(false);
    this.versusCanvasInitialized = true;
    this.versusStartedAt = performance.now();
    this.versusSequenceStartedAt = 0;
    this.versusModelsLoaded = 0;
    this.versusLastShotIndex = 0;
    this.versusCameraTilt = 0;
    this.versusPlayerClips.clear();
    this.versusOpponentClips.clear();
    this.versusPlayerCurrentClip = '';
    this.versusOpponentCurrentClip = '';
    this.cinematicSceneLoadingPercentage = Math.max(this.cinematicSceneLoadingPercentage, 8);

    const width = Math.max(1, canvas.clientWidth || window.innerWidth);
    const height = Math.max(1, canvas.clientHeight || window.innerHeight);
    const renderer = new THREE.WebGLRenderer({
      canvas,
      alpha: true,
      antialias: !this.isPotato,
      powerPreference: this.isPotato ? 'low-power' : 'high-performance',
      precision: this.isPotato ? 'mediump' : 'highp',
      stencil: false
    });
    renderer.setSize(width, height, false);
    renderer.setPixelRatio(this.isPotato ? 1 : Math.min(window.devicePixelRatio, 1.35));
    renderer.outputColorSpace = THREE.SRGBColorSpace;
    renderer.toneMapping = THREE.ACESFilmicToneMapping;
    renderer.toneMappingExposure = 1.16;
    this.versusRenderer = renderer;

    const scene = new THREE.Scene();
    scene.fog = new THREE.FogExp2(0x02040a, 0.085);
    this.versusScene = scene;

    const camera = new THREE.PerspectiveCamera(31, width / height, 0.1, 40);
    camera.position.set(0, 1.05, 4.85);
    camera.lookAt(0, 0.78, 0);
    this.versusCamera = camera;

    scene.add(new THREE.AmbientLight(0xffffff, this.isPotato ? 0.7 : 0.52));
    const playerLight = new THREE.DirectionalLight(0xff3b2f, 5.8);
    playerLight.position.set(-4, 2.5, 3);
    scene.add(playerLight);
    const rivalLight = new THREE.DirectionalLight(0x38bdf8, 6.2);
    rivalLight.position.set(4, 2.7, 3);
    scene.add(rivalLight);
    const rimLight = new THREE.PointLight(0xffffff, 8, 9);
    rimLight.position.set(0, 1.15, 1.2);
    scene.add(rimLight);

    this.loadVersusParticipant('player', renderer);
    this.loadVersusParticipant('opponent', renderer);

    const clock = new THREE.Clock();
    const animate = () => {
      if (!this.versusRenderer || !this.versusScene || !this.versusCamera) return;
      this.versusAnimationId = requestAnimationFrame(animate);
      const dt = Math.min(clock.getDelta(), 0.05);
      const now = performance.now();
      const elapsed = (now - this.versusStartedAt) / 1000;
      const sequenceElapsed = this.versusSequenceStartedAt
        ? (now - this.versusSequenceStartedAt) / 1000
        : 0;

      if (!this.cinematicAssetsReady) {
        if (now - this.versusProgressLastRender > 140) {
          const deltaSecs = (now - this.versusProgressLastRender) / 1000;
          this.versusProgressLastRender = now;
          const progressCap = this.versusModelsLoaded === 0 ? 48 : this.versusModelsLoaded === 1 ? 78 : 94;
          this.cinematicSceneLoadingPercentage = Math.min(
            progressCap,
            this.cinematicSceneLoadingPercentage + deltaSecs * (this.versusModelsLoaded === 0 ? 10 : 16)
          );
          this.cdr.detectChanges();
        }
      }

      if (this.cinematicAssetsReady) {
        this.versusPlayerMixer?.update(dt);
        this.versusOpponentMixer?.update(dt);
        if (sequenceElapsed >= 13.0 && this.serverLoadingComplete && this.finalizarPreloadFn) {
          this.finalizarPreloadFn(this.lastPreloadState);
        }
      }

      const currWidth = Math.max(1, canvas.clientWidth || window.innerWidth);
      const currHeight = Math.max(1, canvas.clientHeight || window.innerHeight);
      if (canvas.width !== Math.floor(currWidth * renderer.getPixelRatio()) || canvas.height !== Math.floor(currHeight * renderer.getPixelRatio())) {
        renderer.setSize(currWidth, currHeight, false);
        camera.aspect = currWidth / currHeight;
        camera.updateProjectionMatrix();
      }

      const reveal = THREE.MathUtils.smoothstep(sequenceElapsed, 0.55, 2.4);

      // ─── CINEMATIC CAMERA SHOTS (Smash Bros inspired) ───
      // Continuous camera sequence (no hard cuts/teleporting)
      const keyframes = [
        { time: 0,   tx: 0.0, ty: 1.15, tz: 3.5,  lx: 0.0, ly: 0.95, tilt: -0.03 },
        { time: 2.0, tx: 0.0, ty: 1.12, tz: 3.8,  lx: 0.0, ly: 0.92, tilt: -0.01 },
        { time: 4.0, tx: 0.0, ty: 1.09, tz: 4.2,  lx: 0.0, ly: 0.89, tilt: 0.01 },
        { time: 7.0, tx: 0.0, ty: 1.07, tz: 4.7,  lx: 0.0, ly: 0.86, tilt: 0.03 },
        { time: 10.0,tx: 0.0, ty: 1.06, tz: 5.2,  lx: 0.0, ly: 0.84, tilt: 0.01 },
        { time: 13.0,tx: 0.0, ty: 1.05, tz: 5.6,  lx: 0.0, ly: 0.82, tilt: 0.0 }
      ];

      let k1 = keyframes[0];
      let k2 = keyframes[keyframes.length - 1];
      let easeT = 0;

      if (sequenceElapsed <= k1.time) {
        k2 = k1;
        easeT = 0;
      } else if (sequenceElapsed >= k2.time) {
        k1 = k2;
        easeT = 1;
      } else {
        for (let i = 0; i < keyframes.length - 1; i++) {
          if (sequenceElapsed >= keyframes[i].time && sequenceElapsed < keyframes[i + 1].time) {
            k1 = keyframes[i];
            k2 = keyframes[i + 1];
            const t = (sequenceElapsed - k1.time) / (k2.time - k1.time);
            easeT = THREE.MathUtils.smoothstep(t, 0, 1);
            break;
          }
        }
      }

      const targetX = THREE.MathUtils.lerp(k1.tx, k2.tx, easeT);
      const targetY = THREE.MathUtils.lerp(k1.ty, k2.ty, easeT);
      let targetZ = THREE.MathUtils.lerp(k1.tz, k2.tz, easeT);
      const lookX = THREE.MathUtils.lerp(k1.lx, k2.lx, easeT);
      const lookY = THREE.MathUtils.lerp(k1.ly, k2.ly, easeT);
      const tilt = THREE.MathUtils.lerp(k1.tilt, k2.tilt, easeT);

      // Micro-pulse for liveliness
      const pulse = Math.sin(sequenceElapsed * 3.2) * 0.018;
      targetZ += pulse;

      // Apply camera with smooth weight interpolation
      const lerpSpeed = 0.15;
      camera.position.x += (targetX - camera.position.x) * lerpSpeed;
      camera.position.y += (targetY - camera.position.y) * lerpSpeed;
      camera.position.z += (targetZ - camera.position.z) * lerpSpeed;
      camera.lookAt(lookX, lookY, 0);
      this.versusCameraTilt += (tilt - this.versusCameraTilt) * 0.12;
      camera.rotation.z = this.versusCameraTilt;

      if (this.cinematicAssetsReady) {
        this.updateVersusAnimation('player', this.localPlayerCharacterId, sequenceElapsed);
        this.updateVersusAnimation('opponent', this.opponentCharacterId, sequenceElapsed);
      }

      // Model positions kept stationary (no skating) for clean camera focus
      if (this.versusPlayerModel) {
        this.versusPlayerModel.position.x = -1.34;
        this.versusPlayerModel.position.y = 0;
        this.versusPlayerModel.position.z = Math.sin(elapsed * 1.3) * 0.03;
      }
      if (this.versusOpponentModel) {
        this.versusOpponentModel.position.x = 1.34;
        this.versusOpponentModel.position.y = 0;
        this.versusOpponentModel.position.z = -Math.sin(elapsed * 1.3) * 0.03;
      }

      renderer.render(scene, camera);
    };
    animate();
  }

  reloadVersusOpponentModel(): void {
    if (!this.versusScene || !this.versusRenderer) return;

    if (this.versusOpponentModel) {
      this.versusScene.remove(this.versusOpponentModel);
      this.disposeModelMaterials(this.versusOpponentModel);
      this.versusOpponentModel = undefined;
    }
    this.versusOpponentMixer = undefined;
    this.versusOpponentClips.clear();
    this.versusOpponentCurrentClip = '';

    this.loadVersusParticipant('opponent', this.versusRenderer);
  }

  private loadVersusParticipant(side: 'player' | 'opponent', renderer: THREE.WebGLRenderer): void {
    const isPlayer = side === 'player';
    const characterId = isPlayer ? this.localPlayerCharacterId : this.opponentCharacterId;
    this.loadHandshakeCharacter(characterId, (model, animations) => {
      if (!this.versusScene || !this.versusRenderer) {
        this.disposeModelMaterials(model);
        return;
      }

      // Guard against race conditions where the character changed while loading
      const currentId = isPlayer ? this.localPlayerCharacterId : this.opponentCharacterId;
      if (characterId !== currentId) {
        this.disposeModelMaterials(model);
        return;
      }

      const mixer = new THREE.AnimationMixer(model);
      const clipMap = isPlayer ? this.versusPlayerClips : this.versusOpponentClips;
      
      // Ensure all clips play in-place by removing any root translation tracks.
      // This prevents characters from walking/jumping away from their designated coordinates.
      animations.forEach(clip => {
        clip.tracks = clip.tracks.filter(track => {
          const name = track.name.toLowerCase();
          if (name.endsWith('.position') && (
            name.startsWith('root') ||
            name.startsWith('armature') ||
            name.startsWith('position') ||
            name.startsWith('__root') ||
            name === '.position'
          )) {
            return false;
          }
          return true;
        });
        clipMap.set(clip.name.toLowerCase(), mixer.clipAction(clip));
      });

      if (isPlayer) {
        this.applyModelCustomization(
          model,
          characterId,
          localStorage.getItem('lobbySkinColor') || '#ffe0bd',
          localStorage.getItem('lobbyHairColor') || '#5c4033',
          localStorage.getItem('lobbyEyeColor') || '#2563eb',
          parseFloat(localStorage.getItem('lobbyHeight') || '1')
        );
        model.position.set(-1.34, 0, 0);
        model.rotation.y = Math.PI * 4 / 5;
        this.versusPlayerModel = model;
        this.versusPlayerMixer = mixer;
      } else {
        this.applyModelCustomization(
          model,
          characterId,
          this.opponentSkinColor,
          this.opponentHairColor,
          this.opponentEyeColor,
          this.opponentHeight
        );
        model.position.set(1.34, 0, 0);
        model.rotation.y = -Math.PI / 5;
        this.versusOpponentModel = model;
        this.versusOpponentMixer = mixer;
      }
      this.versusScene.add(model);
      this.updateVersusAnimation(side, characterId, 0);
      mixer.update(0.001);
      this.markVersusParticipantLoaded(renderer);
    }, renderer);
  }

  private setCinematicAssetsReady(): void {
    this.versusPreloadFinished = true;
    this.tryStartVersusSequence();
  }

  private tryStartVersusSequence(): void {
    if (!this.versusPreloadFinished || !this.versusSequenceTriggered) return;
    if (this.cinematicAssetsReady) return;

    this.cinematicAssetsReady = true;
    this.versusSequenceStartedAt = performance.now();
    this.cdr.detectChanges();

    // Show loading bars with a delay of 2.0s after players start revealing
    setTimeout(() => {
      this.cinematicLoadingBarsVisible = true;
      this.cdr.detectChanges();
    }, 2000);
  }

  private markVersusParticipantLoaded(renderer: THREE.WebGLRenderer): void {
    this.versusModelsLoaded++;
    this.cinematicSceneLoadingPercentage = Math.max(
      this.cinematicSceneLoadingPercentage,
      this.versusModelsLoaded === 1 ? 58 : 88
    );
    this.cdr.detectChanges();
    if (this.versusModelsLoaded < 2 || !this.versusScene || !this.versusCamera) return;

    const warmup = typeof renderer.compileAsync === 'function'
      ? renderer.compileAsync(this.versusScene, this.versusCamera)
      : Promise.resolve();
    warmup.catch(() => undefined).finally(() => {
      if (!this.versusRenderer) return;
      this.cinematicSceneLoadingPercentage = 100;
      this.versusPreloadFinished = true;
      this.tryStartVersusSequence();
      this.cdr.detectChanges();
    });
  }

  private updateVersusAnimation(side: 'player' | 'opponent', characterId: string, elapsed: number): void {
    const plan = this.getVersusAnimationPlan(characterId);
    let token = plan[0].token;
    for (const step of plan) {
      if (elapsed >= step.at) token = step.token;
    }

    const clipMap = side === 'player' ? this.versusPlayerClips : this.versusOpponentClips;
    const current = side === 'player' ? this.versusPlayerCurrentClip : this.versusOpponentCurrentClip;
    if (current === token) return;

    const entry = [...clipMap.entries()].find(([name]) => name.includes(token))
      || [...clipMap.entries()].find(([name]) => /idle|stand|house|talk/.test(name));
    if (!entry) return;

    clipMap.forEach(action => {
      if (action !== entry[1] && action.isRunning()) action.fadeOut(0.18);
    });
    entry[1].reset().setLoop(THREE.LoopOnce, 1).fadeIn(0.18).play();
    entry[1].clampWhenFinished = true;
    if (side === 'player') this.versusPlayerCurrentClip = token;
    else this.versusOpponentCurrentClip = token;
  }

  private getVersusAnimationPlan(characterId: string): Array<{ at: number; token: string }> {
    if (characterId === 'hilda-sygna' || characterId === 'adaman' || characterId === 'giovanni-sygna' || characterId === 'hugh' || characterId === 'courtney' || characterId === 'zinnia') {
      return [
        { at: 0, token: 'appearance_2' },
        { at: 2.35, token: 'cutin_trainer' },
        { at: 4.75, token: 'sync_move_player' },
        { at: 7.35, token: 'start_battle' }
      ];
    }
    if (characterId === 'lillie' || characterId === 'irida') {
      return [
        { at: 0, token: 'appearance_1' },
        { at: 2.2, token: 'greeting_1' },
        { at: 4.55, token: 'cutin_trainer' },
        { at: 7.25, token: 'start_battle' }
      ];
    }
    if (characterId === 'ash') {
      return [
        { at: 0, token: 'yoteeligo' },
        { at: 3.0, token: 'fight' },
        { at: 6.4, token: 'talking' },
        { at: 8.2, token: 'fight' }
      ];
    }
    return [
      { at: 0, token: 'wave' },
      { at: 2.7, token: 'punch' },
      { at: 5.25, token: 'dance' },
      { at: 8.0, token: 'standing' }
    ];
  }

  private cleanupVersusScene(resetCanvasFlag = true): void {
    if (this.versusAnimationId) {
      cancelAnimationFrame(this.versusAnimationId);
      this.versusAnimationId = undefined;
    }
    if (this.versusPlayerModel) {
      this.versusScene?.remove(this.versusPlayerModel);
      this.disposeModelMaterials(this.versusPlayerModel);
    }
    if (this.versusOpponentModel) {
      this.versusScene?.remove(this.versusOpponentModel);
      this.disposeModelMaterials(this.versusOpponentModel);
    }
    this.versusPlayerModel = undefined;
    this.versusOpponentModel = undefined;
    this.versusPlayerMixer = undefined;
    this.versusOpponentMixer = undefined;
    this.versusPlayerClips.clear();
    this.versusOpponentClips.clear();
    this.versusPlayerCurrentClip = '';
    this.versusOpponentCurrentClip = '';
    this.versusModelsLoaded = 0;
    this.versusRenderer?.dispose();
    this.versusRenderer = undefined;
    this.versusScene = undefined;
    this.versusCamera = undefined;
    if (resetCanvasFlag) this.versusCanvasInitialized = false;
  }
}
