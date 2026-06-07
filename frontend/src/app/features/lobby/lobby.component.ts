import { Component, OnInit, ChangeDetectorRef, ViewChild, ElementRef, AfterViewInit, HostListener, NgZone, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { SobreService } from './services/sobre.service';
import { MazoService } from '../deck-builder/services/mazo.service';
import { DeckBuilderComponent } from '../deck-builder/deck-builder.component';
import { JugadorService } from '../../core/services/jugador.service';
import { BattleService } from '../battle/services/battle.service';
import { CardService } from '../../core/services/card.service';
import { Router } from '@angular/router';
import { AperturaSobreComponent } from './components/apertura-sobre/apertura-sobre';
import { TranslatePipe } from '../../i18n/translate.pipe';
import { I18nService } from '../../i18n/i18n.service';
import { Card } from '../../shared/models/card';
import { Jugador, JugadorDatosResponse } from '../../shared/models/jugador';
import { Mazo } from '../../shared/models/mazo';
import { Partida } from '../../shared/models/battle';
import { firstValueFrom } from 'rxjs';
import { getWsUrl as getApiWsUrl } from '../../core/services/api-config';
import * as THREE from 'three';
import { GLTFLoader } from 'three/examples/jsm/loaders/GLTFLoader.js';
import { KTX2Loader } from 'three/examples/jsm/loaders/KTX2Loader.js';
import { MeshoptDecoder } from 'three/examples/jsm/libs/meshopt_decoder.module.js';
import { clone as cloneSkeleton } from 'three/examples/jsm/utils/SkeletonUtils.js';

// Datos usados por el zoom flotante de una carta.
export interface PokemonZoomUI {
  id: string;
  nombre: string;
  imagen: string;
  pokedexId: number;
  hp: number | string;
  tipo: string;
  attacks: string;
  hpIcon: string;
  typeIcon: string;
  attacksIcon: string;
}

interface HubSpot {
  id: 'deck' | 'battle' | 'packs' | 'santoro';
  short: string;
  kicker: string;
  label: string;
  description: string;
  position: THREE.Vector3;
  color: number;
  screenX?: number;
  screenY?: number;
  group?: THREE.Group;
}

interface CharacterOption {
  id: string;
  label: string;
  path: string;
  scale: number;
  yOffset: number;
  rotationY: number;
  idleHints: string[];
  walkHints: string[];
  runHints: string[];
}

interface CampusNpc {
  root: THREE.Group;
  mixer?: THREE.AnimationMixer;
  actions: Map<string, THREE.AnimationAction>;
  active?: THREE.AnimationAction;
}

type SantoroDialogState = 'intro' | 'thanks' | 'forced' | 'gift' | 'afterGift' | 'repeat';

export interface OtherPlayerNPC {
  username: string;
  characterId: string;
  skinColor: string;
  hairColor: string;
  eyeColor: string;
  height: number;
  pikachuEnabled: boolean;
  root: THREE.Group;
  modelGroup?: THREE.Group;
  mixer?: THREE.AnimationMixer;
  actions: Map<string, THREE.AnimationAction>;
  active?: THREE.AnimationAction;
  isPlayingEmote?: boolean;
  pikachu?: CampusNpc;
  fallbackGroup?: THREE.Group;
  targetPosition: THREE.Vector3;
  targetRotationY: number;
  currentAnimation: 'idle' | 'walking' | 'running';
  fullModelLoaded?: boolean;
  fullModelLoading?: boolean;
  highDetailActive?: boolean;
  screenX?: number;
  screenY?: number;
  chatBubble?: string;
  chatBubbleTimeout?: any;
}

@Component({
  selector: 'app-lobby',
  templateUrl: './lobby.component.html',
  styleUrls: ['./lobby.component.scss'],
  standalone: true,
  imports: [CommonModule, FormsModule, AperturaSobreComponent, DeckBuilderComponent, TranslatePipe]
})
export class LobbyComponent implements OnInit, AfterViewInit, OnDestroy {
  @ViewChild('hubCanvas', { static: true }) hubCanvas!: ElementRef<HTMLCanvasElement>;
  @ViewChild('previewCanvas') previewCanvas?: ElementRef<HTMLCanvasElement>;

  // WebSocket Multiplayer
  private socket?: WebSocket;
  private reconnectTimeout?: ReturnType<typeof setTimeout>;
  private lobbyDestroyed = false;
  private personalizationSynced = false;
  otherPlayers = new Map<string, OtherPlayerNPC>();
  private localAnimationState: 'idle' | 'walking' | 'running' = 'idle';
  private lastMoveSentTime = 0;
  private lastSentRotY = 0;
  private lastSentAnimation: 'idle' | 'walking' | 'running' = 'idle';

  // Estado de Chat
  chatActive = false;
  chatText = '';
  chatLog: Array<{ sender: string; text: string; system?: boolean }> = [];
  localChatBubble: string | null = null;
  localBubbleScreenX?: number;
  localBubbleScreenY?: number;
  private localBubbleTimeout?: any;

  // Estado de Emotes
  showEmoteMenu = false;
  isPlayingEmote = false;
  emoteAnimationName?: string;

  get otherPlayersList(): OtherPlayerNPC[] {
    return Array.from(this.otherPlayers.values());
  }

  // Estado principal del lobby.
  jugador: Jugador | null = null;
  mazos: Mazo[] = [];
  slotsVacios: number[] = [];
  sobresDisponibles: number = 0;
  santoCoins: number = 0;
  cantidadCartas: number = 0;
  cantidadCartasUnicas: number = 0;
  mostrarAnimacionSobre: boolean = false;
  cartasNuevas: Card[] = [];
  showDebugPanel: boolean = false;
  debugPanelX = 18;
  debugPanelY = 18;
  debugFps = 0;
  debugFrameMs = 0;
  debugDrawCalls = 0;
  debugTriangles = 0;
  debugGeometries = 0;
  debugTextures = 0;
  debugPixelRatio = 1;
  debugAdaptiveScale = 1;
  private debugStatsTime = 0;
  private debugStatsFrames = 0;
  private debugPanelDragging = false;
  private debugPanelDragOffsetX = 0;
  private debugPanelDragOffsetY = 0;
  debugSobresCantidad: number = 0;
  debugCatalogoCompleto: Card[] = [];
  debugCatalogoFiltrado: Card[] = [];
  debugSelectedIndex: number = 0;
  debugSearchText: string = '';
  debugTargetMazoId: number | null = null;
  debugReplaceCardId: string | null = null;
  debugAccionEnCurso: boolean = false;
  selectedBattleDeckId: number | null = null;
  battlePanelOpen = false;
  kioskShopOpen = false;
  vendorCameraFocus = false;
  deckBuilderOpen = false;
  santoroDialogOpen = false;
  kiosqueraDialogOpen = false;
  santoroDialogState: SantoroDialogState = 'intro';
  santoroGiftModalOpen = false;
  santoroQuestTracking = false;
  santoroGiftClaimed = false;
  santoroQuestState = 'AVAILABLE';
  santoroRewardLoading = false;
  characterMenuOpen = false;
  selectedCharacterId = localStorage.getItem('lobbyCharacter') || 'hilda-sygna';
  pikachuEnabled = localStorage.getItem('pikachuCompanion') !== 'false';
  dayPhaseLabel = 'lobby.phase.morning';
  currentInteraction: HubSpot | null = null;
  graphicsQuality: 'low' | 'medium' | 'high' = 'medium';
  landscapeHintDismissed = localStorage.getItem('lobbyLandscapeHintDismissed') === 'true';
  readonly packBundlePrices: Record<number, number> = { 1: 80, 3: 200, 5: 300 };

  // Interaction & Context Menu State
  selectedPlayerForMenu: OtherPlayerNPC | null = null;
  playerMenuX = 0;
  playerMenuY = 0;

  // Challenge / Duel State
  incomingChallenge: any = null;
  waitingForChallengeResponse = false;
  challengedUsername = '';
  challengeSecondsLeft = 10;
  challengeTimerDashoffset = 0;
  private challengeTimerInterval: any = null;

  // Trade State
  incomingTradeInvite: any = null;
  waitingForTradeResponse = false;
  tradeInvitedUsername = '';
  activeTradeSession = false;
  tradeOpponentUsername = '';
  tradeLeftCards: Card[] = [];
  tradeRightCards: Card[] = [];
  tradeLeftReady = false;
  tradeRightReady = false;
  tradeCollectionSearchText = '';
  tradeCollectionFilterRarity = '';
  tradeCollectionFilterType = '';
  tradeCollectionFilterSupertype = '';
  tradeCollectionFilterSubtype = '';
  tradeCollectionFiltrada: Card[] = [];
  tradeCollectionLoading = false;
  tradeCollectionError = '';
  tradeCollectionRarities: string[] = [];
  tradeCollectionTypes: string[] = [];
  tradeCollectionSupertypes: string[] = [];
  tradeCollectionSubtypes: string[] = [];
  tradeShowValueWarning = false;
  tradeShowContinuationPrompt = false;
  tradeWaitingForContinuation = false;
  userTradeCollection: Card[] = [];
  private tradeCollectionLoaded = false;

  get graphicsQualityLabel(): string {
    if (this.graphicsQuality === 'low') return this.i18n.translate('lobby.low') + ' 🔴';
    if (this.graphicsQuality === 'medium') return this.i18n.translate('lobby.medium') + ' 🟡';
    return this.i18n.translate('lobby.high') + ' 🟢';
  }

  // Propiedades para el modal de personalizaciÃ³n y Onboarding
  customizationModalOpen = false;
  showFirstTimeSetup = false;
  
  customizerGender: 'pibe' | 'piba' = 'piba';
  customizerSelectedId = 'hilda-sygna';
  customizerHeight = 1.0;
  customizerSkinColor = '#ffe0bd';
  customizerHairColor = '#5c4033';
  customizerEyeColor = '#2563eb';
  customizerPikachu = true;
  customizerRotationY = 0;

  // Variables para la escena de previsualizaciÃ³n 3D secundaria aislada en el modal
  private previewRenderer?: THREE.WebGLRenderer;
  private previewScene?: THREE.Scene;
  private previewCamera?: THREE.PerspectiveCamera;
  private previewModelGroup = new THREE.Group();
  private previewMixer?: THREE.AnimationMixer;
  private previewActions = new Map<string, THREE.AnimationAction>();
  private activePreviewAction?: THREE.AnimationAction;
  private previewMixers: THREE.AnimationMixer[] = [];

  // Swatches predefinidos premium de rasgos de entrenador
  readonly skinColors = [
    { name: 'Alabastro', value: '#fff1e0' },
    { name: 'Claro NÃ³rdico', value: '#ffe0bd' },
    { name: 'Durazno CÃ¡lido', value: '#ffcd94' },
    { name: 'Trigo', value: '#f0c294' },
    { name: 'Bronceado UTN', value: '#e0ac69' },
    { name: 'Oliva Dorado', value: '#c68642' },
    { name: 'Canela', value: '#b0733a' },
    { name: 'MarrÃ³n Oscuro', value: '#8d5524' },
    { name: 'Ã‰bano Profundo', value: '#5c3412' },
    { name: 'Gris Androide', value: '#d1d5db' }
  ];

  readonly hairColors = [
    { name: 'CastaÃ±o Oscuro', value: '#3d2314' },
    { name: 'Chocolate', value: '#5c4033' },
    { name: 'CastaÃ±o Claro', value: '#a87c55' },
    { name: 'Negro Azabache', value: '#121212' },
    { name: 'Rubio Platino', value: '#fef08a' },
    { name: 'Rubio Dorado', value: '#f59e0b' },
    { name: 'Pelirrojo Fuego', value: '#ea580c' },
    { name: 'Naranja NeÃ³n', value: '#ff7c3b' },
    { name: 'Rosa Sakura', value: '#ec4899' },
    { name: 'Violeta ElÃ©ctrico', value: '#8b5cf6' },
    { name: 'Azul UTN', value: '#3b82f6' },
    { name: 'Celeste Pastel', value: '#60a5fa' },
    { name: 'Verde Esmeralda', value: '#10b981' },
    { name: 'Blanco Sabio', value: '#f8fafc' }
  ];

  readonly eyeColors = [
    { name: 'Azul ElÃ©ctrico', value: '#2563eb' },
    { name: 'Celeste Cristal', value: '#38bdf8' },
    { name: 'Verde Esmeralda', value: '#16a34a' },
    { name: 'Verde Menta', value: '#34d399' },
    { name: 'MarrÃ³n Avellana', value: '#854d0e' },
    { name: 'Rojo RubÃ­', value: '#dc2626' },
    { name: 'Violeta MÃ­stico', value: '#7c3aed' },
    { name: 'Dorado PokÃ©mon', value: '#facc15' }
  ];

  get filteredCharacterOptions(): CharacterOption[] {
    if (this.customizerGender === 'pibe') {
      return this.characterOptions.filter(o => o.id === 'ash' || o.id === 'robot');
    } else {
      return this.characterOptions.filter(o => o.id === 'hilda-sygna' || o.id === 'lillie');
    }
  }

  readonly characterOptions: CharacterOption[] = [
    { id: 'hilda-sygna', label: 'Hilda Sygna', path: '/models-optimized/characters/hilda_sygna_10.glb', scale: 1, yOffset: 0, rotationY: Math.PI, idleHints: ['idle'], walkHints: ['walk_1', 'walk'], runHints: ['run_1', 'run'] },
    { id: 'lillie', label: 'Lillie', path: '/models-optimized/characters/lillie__anniversary_50.glb', scale: 1, yOffset: 0, rotationY: Math.PI, idleHints: ['idle'], walkHints: ['walk_1', 'walk'], runHints: ['walk_1', 'walk'] },
    { id: 'ash', label: 'Ash', path: '/models-optimized/characters/ash_ketchup_-_pokemon.glb', scale: 0.82, yOffset: 0, rotationY: Math.PI, idleHints: ['house', 'talking', 'walking'], walkHints: ['walking'], runHints: ['walking'] },
    { id: 'robot', label: 'Robot CC0', path: '/models-optimized/player/RobotExpressive.glb', scale: 0.42, yOffset: 0, rotationY: Math.PI, idleHints: ['idle', 'standing'], walkHints: ['walking', 'walk'], runHints: ['running', 'run'] }
  ];
  hubSpots: HubSpot[] = [
    {
      id: 'deck',
      short: 'DECK',
      kicker: 'DECK LAB',
      label: 'Armar mazo',
      description: 'EntrÃ¡ al laboratorio para editar tu estrategia.',
      position: new THREE.Vector3(-15, 0, -5),
      color: 0x3bd6ff
    },
    {
      id: 'battle',
      short: 'VS',
      kicker: 'BATTLE GATE',
      label: 'Iniciar batalla',
      description: 'ElegÃ­ un mazo y cruzÃ¡ el portal de combate.',
      position: new THREE.Vector3(4, 0, -25),
      color: 0xffd44a
    },
    {
      id: 'packs',
      short: 'PACK',
      kicker: 'KIOSCO UTN',
      label: 'Comprar sobres',
      description: 'ActivÃ¡ el altar para revelar nuevas cartas.',
      position: new THREE.Vector3(-19.0, 0, -13.5),
      color: 0x7cff9d
    },
    {
      id: 'santoro',
      short: 'PROFE',
      kicker: 'MISIÓN UTN',
      label: 'Hablar con Santoro',
      description: 'El profe tiene tu primera misión y una ayuda inicial.',
      position: new THREE.Vector3(0.8, 0, -27.5),
      color: 0xfacc15
    }
  ];

  // Scanner / Zoom
  pkmZoom: PokemonZoomUI | null = null;
  zoomX: number = 0;
  zoomY: number = 0;

  // Loading manager para el escenario 3D del Lobby
  private hubLoadingManager = new THREE.LoadingManager();
  private remoteAvatarLoadingManager = new THREE.LoadingManager();
  showHubLoadingOverlay = true;
  hubLoadingProgress = 0;
  hubVisualProgress = 0;
  private visualProgressInterval?: any;

  constructor(
    private sobreService: SobreService,
    private mazoService: MazoService,
    private jugadorService: JugadorService,
    private battleService: BattleService,
    private cardService: CardService,
    private router: Router,
    private ngZone: NgZone,
    private cdr: ChangeDetectorRef,
    public i18n: I18nService
  ) {
    this.hubLoadingManager.onStart = (_url, itemsLoaded, itemsTotal) => {
      if (itemsTotal <= 0 || !this.showHubLoadingOverlay && this.hubLoadingProgress >= 100) return;
      this.ngZone.run(() => {
        this.showHubLoadingOverlay = true;
        this.hubLoadingProgress = 0;
        this.hubVisualProgress = 0;
        this.cdr.detectChanges();
        this.startVisualProgressLoop();
      });
    };

    this.hubLoadingManager.onProgress = (_url, itemsLoaded, itemsTotal) => {
      if (!this.showHubLoadingOverlay && this.hubLoadingProgress >= 100) return;
      this.ngZone.run(() => {
        const progress = Math.round((itemsLoaded / itemsTotal) * 100);
        // Aseguramos que la carga real progrese, pero evitamos saltos toscos
        this.hubLoadingProgress = Math.max(this.hubLoadingProgress, progress);
        this.cdr.detectChanges();
      });
    };

    this.hubLoadingManager.onLoad = () => {
      this.ngZone.run(() => {
        this.hubLoadingProgress = 100;
        this.cdr.detectChanges();
      });
    };

    this.hubLoadingManager.onError = (url) => {
      console.warn('Error cargando recurso 3D:', url);
    };
  }

  private startVisualProgressLoop() {
    if (this.visualProgressInterval) {
      clearInterval(this.visualProgressInterval);
    }
    this.hubVisualProgress = 0;

    this.ngZone.runOutsideAngular(() => {
      this.visualProgressInterval = setInterval(() => {
        if (this.hubVisualProgress < this.hubLoadingProgress) {
          const gap = this.hubLoadingProgress - this.hubVisualProgress;
          // Si la brecha es grande (ej. carga rÃ¡pida o cachÃ©), el incremento es mayor, pero siempre continuo
          const step = Math.max(1, Math.min(4, Math.floor(gap * 0.08)));
          this.hubVisualProgress += step;

          this.ngZone.run(() => {
            this.cdr.detectChanges();
          });
        } else if (this.hubVisualProgress >= 100) {
          clearInterval(this.visualProgressInterval);
          this.visualProgressInterval = undefined;

          this.ngZone.run(() => {
            setTimeout(() => {
              this.showHubLoadingOverlay = false;
              this.cdr.detectChanges();
            }, 900); // 900ms para un desvanecimiento estÃ©tico y suave
          });
        }
      }, 30); // Loop a ~33fps para una transiciÃ³n sedosa de la barra y Pikachu
    });
  }

  private renderer?: THREE.WebGLRenderer;
  private scene?: THREE.Scene;
  private camera?: THREE.PerspectiveCamera;
  private player = new THREE.Group();
  private playerVelocity = new THREE.Vector3();
  private playerMixer?: THREE.AnimationMixer;
  private playerActions = new Map<string, THREE.AnimationAction>();
  private activePlayerAction?: THREE.AnimationAction;
  private currentCharacterOption?: CharacterOption;
  private pikachu?: CampusNpc;
  private kioskVendor?: CampusNpc;
  private santoroNpc?: CampusNpc;
  private santoroPathGroup?: THREE.Group;
  private santoroBones: {
    spine?: THREE.Bone;
    spine1?: THREE.Bone;
    spine2?: THREE.Bone;
    neck?: THREE.Bone;
    head?: THREE.Bone;
    leftArm?: THREE.Bone;
    rightArm?: THREE.Bone;
    leftForeArm?: THREE.Bone;
    rightForeArm?: THREE.Bone;
    leftShoulder?: THREE.Bone;
    rightShoulder?: THREE.Bone;
  } = {};
  private santoroDefaultQuaternions = new Map<THREE.Bone, THREE.Quaternion>();
  private santoroHeadYaw = 0;
  private santoroHeadPitch = 0;
  private santoroTargetHeadYaw = 0;
  private santoroTargetHeadPitch = 0;
  private santoroGazeTimer = 0;
  private ktx2Loader?: KTX2Loader;
  private sceneMixers: THREE.AnimationMixer[] = [];
  private ambientLight?: THREE.AmbientLight;
  private sunLight?: THREE.DirectionalLight;
  private moonLight?: THREE.DirectionalLight;
  private sunMesh?: THREE.Mesh;
  private moonMesh?: THREE.Mesh;
  private skyDome?: THREE.Mesh;
  private clouds: THREE.Group[] = [];
  private mountainMaterials: THREE.MeshBasicMaterial[] = [];
  private boundaryMesh?: THREE.Mesh;
  keys = new Set<string>();
  mobileJoystickActive = false;
  mobileJoystickX = 0;
  mobileJoystickY = 0;
  private mobileJoystickPointerId: number | null = null;
  private clock = new THREE.Clock();
  private animationId = 0;
  private worldObjects: THREE.Object3D[] = [];
  private disposable: Array<THREE.BufferGeometry | THREE.Material | THREE.Texture> = [];
  private lastInteractionId: string | null = null;
  private lampLights: THREE.PointLight[] = [];
  private lampEmissiveMaterials: THREE.Material[] = [];
  private readonly playerAssetPath = '/models-optimized/player/RobotExpressive.glb';
  private readonly gltfCache = new Map<string, Promise<any>>();
  private textureLoader = new THREE.TextureLoader(this.hubLoadingManager);
  private textureCache = new Map<string, THREE.Texture>();
  private planeGeometryCache = new Map<string, THREE.PlaneGeometry>();
  private cardMaterialCache = new Map<string, THREE.MeshStandardMaterial>();
  private hubEventCleanups: Array<() => void> = [];
  private lastShadowMode = '';
  private pageVisible = typeof document === 'undefined' ? true : !document.hidden;
  private readonly tempVec3 = new THREE.Vector3();
  private readonly tempVec3b = new THREE.Vector3();
  private readonly tempLookAt = new THREE.Vector3();
  private readonly skyDayColor = new THREE.Color(0x86cfff);
  private readonly skySunsetColor = new THREE.Color(0xfc5a03);
  private readonly skyNightColor = new THREE.Color(0x030712);
  private readonly fogBaseColor = new THREE.Color(0x07111f);
  private readonly sunBaseColor = new THREE.Color(0xfff4c0);
  private readonly whiteColor = new THREE.Color(0xffffff);
  private readonly moonAmbientColor = new THREE.Color(0xc7d2fe);
  private readonly sunMeshDayColor = new THREE.Color(0xfffdb5);
  private readonly sunMeshSunsetColor = new THREE.Color(0xff3b00);
  private readonly moonMeshNightColor = new THREE.Color(0xe0e7ff);
  private readonly moonMeshSunsetColor = new THREE.Color(0xfca5a5);
  private readonly dynamicSkyColor = new THREE.Color();
  private readonly dynamicFogColor = new THREE.Color();
  private readonly dynamicLightColor = new THREE.Color();
  private readonly dynamicAmbientColor = new THREE.Color();
  private readonly dynamicSunMeshColor = new THREE.Color();
  private readonly dynamicMoonMeshColor = new THREE.Color();
  private frameCounter = 0;
  private fpsSampleTime = 0;
  private fpsSampleFrames = 0;
  private adaptivePixelRatioScale = 1;
  private lastAppliedPixelRatio = 0;
  audioEnabled = localStorage.getItem('lobbyAudioEnabled') !== 'false';
  musicEnabled = localStorage.getItem('lobbyMusicEnabled') !== 'false';
  sfxEnabled = localStorage.getItem('lobbySfxEnabled') !== 'false';
  audioVolume = Number(localStorage.getItem('lobbyAudioVolume') || '0.55');
  private ambientTracks = ['/assets/login-bgm.mp3'];
  private ambientTrackIndex = Number(localStorage.getItem('lobbyAmbientTrack') || '0');
  private ambientAudio?: HTMLAudioElement;
  private audioContext?: AudioContext;
  private masterGain?: GainNode;
  private sfxGain?: GainNode;
  private footstepClock = 0;
  private nextPikachuChirpAt = 2.5;
  private remoteAudioCheckClock = 0;
  private remotePikachuChirpTimers = new Map<string, number>();
  private readonly groundLift = 0.035;
  private readonly remoteFullModelDistanceSq = 38 * 38;
  private readonly remoteCompanionDistanceSq = 28 * 28;
  private kioskProductPlaceholders?: THREE.Group;
  private kioskProductsDetailedLoaded = false;

  // Control de cÃ¡mara con mouse (Ã³rbita y zoom)
  private cameraOrbitYaw = 0;
  private cameraOrbitPitch = 0.25;
  private cameraZoomDistance = 7.0;
  private isDraggingCamera = false;
  private lastMouseX = 0;
  private lastMouseY = 0;
  private cameraHoldTimeout?: ReturnType<typeof setTimeout>;
  private cameraPointerId: number | null = null;
  private cameraPointerStartX = 0;
  private cameraPointerStartY = 0;
  private readonly cycleSeconds = 720;
  private readonly cardTexturePaths = [
    '/images/cards/base1-4.png',
    '/images/cards/base1-15.png',
    '/images/cards/base1-9.png',
    '/images/cards/xy1-1.png',
    '/images/cards/swsh1-25.png',
    '/images/cards/sm9-1.png',
    '/images/cards/back.png'
  ];

  private get isMobileViewport(): boolean {
    return window.matchMedia?.('(pointer: coarse)').matches || window.innerWidth <= 920 || window.innerHeight <= 520;
  }

  // Recupera al jugador guardado y carga su estado inicial.
  ngOnInit(): void {
    const storedQuality = localStorage.getItem('ptcg_graphics_quality') as 'low' | 'medium' | 'high' | null;
    if (storedQuality) {
      this.graphicsQuality = storedQuality;
    } else {
      this.graphicsQuality = this.detectGraphicsQuality();
      localStorage.setItem('ptcg_graphics_quality', this.graphicsQuality);
    }

    try {
      const data = localStorage.getItem('jugador');
      if (data) {
        this.jugador = JSON.parse(data);
        this.refrescarTodo();
        // VerificaciÃ³n de Onboarding por primera vez
        const setupCompleted = localStorage.getItem('firstTimeSetup');
        if (!setupCompleted) {
          this.showFirstTimeSetup = true;
          this.customizerGender = 'piba'; // Piba por defecto
          this.customizerSelectedId = 'hilda-sygna';
          this.customizerHeight = 1.0;
          this.customizerSkinColor = '#ffe0bd';
          this.customizerHairColor = '#5c4033';
          this.customizerEyeColor = '#2563eb';
          this.customizerPikachu = true;
        }
      } else {
        this.router.navigate(['/login']);
      }
    } catch (error) {
      console.error('Error parseando datos del jugador:', error);
      this.router.navigate(['/login']);
    }
  }

  private detectGraphicsQuality(): 'low' | 'medium' | 'high' {
    try {
      const canvas = document.createElement('canvas');
      const gl = canvas.getContext('webgl') || canvas.getContext('experimental-webgl') as any;
      if (!gl) return 'medium';
      
      const debugInfo = gl.getExtension('WEBGL_debug_renderer_info');
      if (!debugInfo) return 'medium';
      
      const renderer = gl.getParameter(debugInfo.UNMASKED_RENDERER_WEBGL) || '';
      console.log('Detected GL_RENDERER:', renderer);
      const rendererLower = renderer.toLowerCase();
      
      if (
        rendererLower.includes('intel') ||
        rendererLower.includes('uhd') ||
        rendererLower.includes('hd graphics') ||
        rendererLower.includes('mobile') ||
        rendererLower.includes('microsoft basic') ||
        rendererLower.includes('swiftshader') ||
        rendererLower.includes('software') ||
        rendererLower.includes('llvmpipe')
      ) {
        return 'low';
      }
      if (
        rendererLower.includes('nvidia') ||
        rendererLower.includes('geforce') ||
        rendererLower.includes('rtx') ||
        rendererLower.includes('gtx') ||
        rendererLower.includes('radeon') ||
        rendererLower.includes('amd') ||
        rendererLower.includes('apple gpu') ||
        rendererLower.includes('m1') ||
        rendererLower.includes('m2') ||
        rendererLower.includes('m3')
      ) {
        return 'high';
      }
      return 'medium';
    } catch (e) {
      console.warn('Error detecting WebGL renderer:', e);
      return 'medium';
    }
  }

  setGraphicsQuality(quality: 'low' | 'medium' | 'high', updateRenderer: boolean = true) {
    this.graphicsQuality = quality;
    localStorage.setItem('ptcg_graphics_quality', quality);
    if (updateRenderer) {
      this.updateRendererPixelRatio();
      this.updateCachedTextureQuality();
      this.updateShadowQuality();
      // Force update shadow state immediately
      if (this.scene && this.sunLight && this.moonLight) {
        const elapsed = this.clock.elapsedTime;
        this.updateDayNightCycle(elapsed);
      }
      this.cdr.detectChanges();
    }
  }

  cycleGraphicsQuality() {
    const qualities: ('low' | 'medium' | 'high')[] = ['low', 'medium', 'high'];
    const nextIndex = (qualities.indexOf(this.graphicsQuality) + 1) % qualities.length;
    this.setGraphicsQuality(qualities[nextIndex]);
  }

  updateRendererPixelRatio() {
    if (!this.renderer) return;
    let maxDPR = 1.35;
    if (this.graphicsQuality === 'low') maxDPR = 1.0;
    else if (this.graphicsQuality === 'medium') maxDPR = 1.25;
    else if (this.graphicsQuality === 'high') maxDPR = 1.6;
    const target = Math.min(window.devicePixelRatio, maxDPR) * this.adaptivePixelRatioScale;
    const capped = Math.max(0.72, target);
    if (Math.abs(capped - this.lastAppliedPixelRatio) < 0.03) return;
    this.lastAppliedPixelRatio = capped;
    this.renderer.setPixelRatio(capped);
    this.resizeHub();
  }

  private updateCachedTextureQuality() {
    const anisotropy = this.getRuntimeAnisotropy();
    this.textureCache.forEach((texture) => {
      texture.anisotropy = anisotropy;
      texture.needsUpdate = true;
    });
  }

  private updateAdaptivePerformance(delta: number) {
    if (!this.renderer) return;

    this.fpsSampleTime += delta;
    this.fpsSampleFrames++;
    if (this.fpsSampleTime < 1.4) return;

    const fps = this.fpsSampleFrames / this.fpsSampleTime;
    const overlayPressure = this.deckBuilderOpen || this.activeTradeSession || this.kioskShopOpen || this.mostrarAnimacionSobre;
    const minScale = overlayPressure ? 0.7 : 0.78;
    const maxScale = overlayPressure ? 0.88 : 1;
    let nextScale = this.adaptivePixelRatioScale;

    if (fps < 42) {
      nextScale = Math.max(minScale, nextScale - 0.08);
    } else if (fps > 57) {
      nextScale = Math.min(maxScale, nextScale + 0.04);
    } else if (overlayPressure && nextScale > maxScale) {
      nextScale = maxScale;
    }

    this.fpsSampleTime = 0;
    this.fpsSampleFrames = 0;

    if (Math.abs(nextScale - this.adaptivePixelRatioScale) >= 0.03) {
      this.adaptivePixelRatioScale = nextScale;
      this.updateRendererPixelRatio();
    }
  }

  private getRuntimeAnisotropy(): number {
    const max = this.renderer?.capabilities.getMaxAnisotropy() ?? 1;
    if (this.graphicsQuality === 'low' || this.adaptivePixelRatioScale < 0.85) return Math.min(max, 2);
    if (this.graphicsQuality === 'medium') return Math.min(max, 4);
    return Math.min(max, 8);
  }

  private updateShadowQuality() {
    if (!this.renderer) return;
    const enabled = this.graphicsQuality !== 'low';
    this.renderer.shadowMap.enabled = enabled;
    this.renderer.shadowMap.type = THREE.PCFSoftShadowMap;
    this.worldObjects.forEach((object) => {
      if (object instanceof THREE.Mesh && object.userData['radius']) {
        object.castShadow = this.graphicsQuality === 'high';
      }
    });

    const sunSize = this.graphicsQuality === 'high' ? 2048 : 1024;
    const moonSize = this.graphicsQuality === 'high' ? 1024 : 512;
    if (this.sunLight) {
      this.sunLight.castShadow = enabled;
      this.sunLight.shadow.mapSize.set(sunSize, sunSize);
      this.sunLight.shadow.camera.left = this.graphicsQuality === 'high' ? -58 : -42;
      this.sunLight.shadow.camera.right = this.graphicsQuality === 'high' ? 58 : 42;
      this.sunLight.shadow.camera.top = this.graphicsQuality === 'high' ? 58 : 42;
      this.sunLight.shadow.camera.bottom = this.graphicsQuality === 'high' ? -58 : -42;
      this.sunLight.shadow.camera.far = this.graphicsQuality === 'high' ? 125 : 95;
      this.sunLight.shadow.radius = this.graphicsQuality === 'high' ? 3 : 2;
      this.sunLight.shadow.normalBias = 0.018;
      this.sunLight.shadow.map?.dispose();
      this.sunLight.shadow.map = null as any;
      this.sunLight.shadow.camera.updateProjectionMatrix();
    }

    if (this.moonLight) {
      this.moonLight.castShadow = enabled && this.graphicsQuality === 'high';
      this.moonLight.shadow.mapSize.set(moonSize, moonSize);
      this.moonLight.shadow.radius = 2;
      this.moonLight.shadow.normalBias = 0.022;
      this.moonLight.shadow.map?.dispose();
      this.moonLight.shadow.map = null as any;
      this.moonLight.shadow.camera.updateProjectionMatrix();
    }
  }

  // Intenta reproducir el video decorativo apenas exista en el DOM.
  ngAfterViewInit(): void {
    this.ngZone.runOutsideAngular(() => {
      this.requestLandscapeOrientation();
      this.initHubWorld();
      this.connectWebSocket();
      this.animateHub();
      
      if (this.showFirstTimeSetup) {
        this.initPreviewScene();
      }
    });
  }

  ngOnDestroy(): void {
    this.lobbyDestroyed = true;
    if (this.reconnectTimeout) {
      clearTimeout(this.reconnectTimeout);
      this.reconnectTimeout = undefined;
    }
    if (this.cameraHoldTimeout) {
      clearTimeout(this.cameraHoldTimeout);
      this.cameraHoldTimeout = undefined;
    }
    if (this.socket) {
      this.socket.close();
      this.socket = undefined;
    }
    if (this.visualProgressInterval) {
      clearInterval(this.visualProgressInterval);
    }
    cancelAnimationFrame(this.animationId);
    this.hubEventCleanups.forEach((cleanup) => cleanup());
    this.hubEventCleanups = [];
    window.removeEventListener('resize', this.resizeHub);
    if (this.playerMixer) {
      this.playerMixer.stopAllAction();
      this.playerMixer.uncacheRoot(this.player);
    }
    // Limpiar otros jugadores
    this.otherPlayers.forEach((p) => {
      p.mixer?.stopAllAction();
      if (p.pikachu) {
        p.pikachu.mixer?.stopAllAction();
      }
    });
    this.otherPlayers.clear();

    this.disposable.forEach((item) => item.dispose());
    this.ktx2Loader?.dispose();
    this.shutdownLobbyAudio();
    this.renderer?.dispose();
  }

  @HostListener('window:keydown', ['$event'])
  onKeyDown(event: KeyboardEvent) {
    this.unlockLobbyAudio();
    if (event.key === 'F3') {
      event.preventDefault();
      this.showDebugPanel = !this.showDebugPanel;

      if (this.showDebugPanel && this.debugCatalogoCompleto.length === 0) {
        this.cargarCatalogoGodMode();
      }
      return;
    }

    if (event.key === 'Enter') {
      event.preventDefault();
      this.toggleChat();
      return;
    }

    if (this.isTypingTarget(event.target)) return;

    const key = event.key.toLowerCase();
    if (key === 'e') {
      event.preventDefault();
      this.ngZone.run(() => this.interactWithCurrentSpot());
      return;
    }

    if (['w', 'a', 's', 'd', 'arrowup', 'arrowdown', 'arrowleft', 'arrowright', 'shift'].includes(key)) {
      event.preventDefault();
      this.keys.add(key);
    }
  }

  @HostListener('window:keyup', ['$event'])
  onKeyUp(event: KeyboardEvent) {
    this.keys.delete(event.key.toLowerCase());
  }

  @HostListener('window:pointerdown')
  onAnyPointerDown() {
    this.unlockLobbyAudio();
  }

  @HostListener('window:pointermove', ['$event'])
  onWindowPointerMove(event: PointerEvent) {
    if (!this.debugPanelDragging) return;
    const maxX = Math.max(12, window.innerWidth - 260);
    const maxY = Math.max(12, window.innerHeight - 120);
    this.debugPanelX = Math.max(8, Math.min(maxX, event.clientX - this.debugPanelDragOffsetX));
    this.debugPanelY = Math.max(8, Math.min(maxY, event.clientY - this.debugPanelDragOffsetY));
  }

  @HostListener('window:pointerup')
  onWindowPointerUp() {
    this.debugPanelDragging = false;
  }

  startDebugPanelDrag(event: PointerEvent) {
    event.preventDefault();
    event.stopPropagation();
    this.debugPanelDragging = true;
    this.debugPanelDragOffsetX = event.clientX - this.debugPanelX;
    this.debugPanelDragOffsetY = event.clientY - this.debugPanelY;
  }

  // Refresca resumen, sobres y mazos del jugador.
  refrescarTodo(preferNewestDeck = false) {
    if (!this.jugador?.username || this.lobbyDestroyed) return;

    this.jugadorService.getJugador(this.jugador.username).subscribe({
      next: (res: JugadorDatosResponse) => {
        this.sobresDisponibles = res.sobresDisponibles ?? 0;
        this.santoCoins = res.santoCoins ?? 0;
        this.debugSobresCantidad = this.sobresDisponibles;

        if (res.cartasObtenidas && Array.isArray(res.cartasObtenidas)) {
          const idsUnicos = new Set(res.cartasObtenidas.map((c: Card) => c.pokemonId || c.id));
          this.cantidadCartasUnicas = idsUnicos.size;
          this.setTradeCollection(res.cartasObtenidas);
          this.tradeCollectionLoaded = true;
        } else {
          this.cantidadCartasUnicas = res.cantidadCartas ?? 0;
        }

        this.jugador = { ...this.jugador!, ...res };

        if (res.characterId) {
          localStorage.setItem('lobbyCharacter', res.characterId);
          this.selectedCharacterId = res.characterId;
        }
        if (res.skinColor) {
          localStorage.setItem('lobbySkinColor', res.skinColor);
        }
        if (res.hairColor) {
          localStorage.setItem('lobbyHairColor', res.hairColor);
        }
        if (res.eyeColor) {
          localStorage.setItem('lobbyEyeColor', res.eyeColor);
        }
        if (res.height) {
          localStorage.setItem('lobbyHeight', res.height.toString());
        }
        if (res.pikachuCompanion !== undefined) {
          localStorage.setItem('pikachuCompanion', res.pikachuCompanion ? 'true' : 'false');
          this.pikachuEnabled = res.pikachuCompanion;
        }

        this.personalizationSynced = true;
        this.ngZone.runOutsideAngular(() => {
          this.loadAnimatedPlayerAsset();
          if (this.pikachuEnabled) {
            this.createPikachuCompanion();
          }
        });
        this.sendJoinMessage();

        this.cdr.detectChanges();
      },
      error: (err) => console.error('Error al obtener datos del jugador', err)
    });

    this.cargarMazosDeJugador(preferNewestDeck);
    this.precargarColeccionTrade();
    this.cargarSantoroQuest();
  }

  private cargarSantoroQuest() {
    if (!this.jugador?.username) return;

    this.jugadorService.getSantoroQuest(this.jugador.username).subscribe({
      next: (quest) => {
        this.applySantoroQuest(quest);
        this.updateSantoroQuestPath();
        this.cdr.detectChanges();
      },
      error: (err) => console.error('Error cargando mision de Santoro', err)
    });
  }

  private applySantoroQuest(quest: { giftClaimed: boolean; tracking: boolean; state: string; sobresDisponibles: number }) {
    this.santoroGiftClaimed = !!quest.giftClaimed;
    this.santoroQuestTracking = !!quest.tracking && !this.santoroGiftClaimed;
    this.santoroQuestState = quest.state || (this.santoroGiftClaimed ? 'COMPLETED' : 'AVAILABLE');
    if (typeof quest.sobresDisponibles === 'number') {
      this.sobresDisponibles = quest.sobresDisponibles;
      this.debugSobresCantidad = quest.sobresDisponibles;
    }
  }

  precargarColeccionTrade(force = false) {
    if (!this.jugador?.username) return;
    if (!force && this.tradeCollectionLoaded && this.userTradeCollection.length > 0) return;

    this.tradeCollectionLoading = true;
    this.tradeCollectionError = '';

    this.jugadorService.getColeccion(this.jugador.username).subscribe({
      next: (cards) => {
        this.setTradeCollection(cards);
        this.tradeCollectionLoaded = true;
        this.tradeCollectionLoading = false;
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error('Error al precargar coleccion para trade:', err);
        this.tradeCollectionLoading = false;
        this.tradeCollectionError = 'No se pudo cargar tu coleccion.';
        this.cdr.detectChanges();
      }
    });
  }

  private loadCachedGltf(path: string, manager?: THREE.LoadingManager): Promise<{ scene: THREE.Group; animations: THREE.AnimationClip[] }> {
    if (!this.gltfCache.has(path)) {
      const loader = this.createGltfLoader(manager || this.hubLoadingManager);
      this.gltfCache.set(path, new Promise((resolve, reject) => {
        loader.load(path, resolve, undefined, reject);
      }));
    }

    return this.gltfCache.get(path)!.then((gltf: any) => ({
      scene: cloneSkeleton(gltf.scene) as THREE.Group,
      animations: gltf.animations || []
    }));
  }

  private createGltfLoader(manager?: THREE.LoadingManager): GLTFLoader {
    const loader = new GLTFLoader(manager || this.hubLoadingManager);
    loader.setMeshoptDecoder(MeshoptDecoder);
    const ktx2 = this.getKtx2Loader();
    if (ktx2) {
      loader.setKTX2Loader(ktx2);
    }
    return loader;
  }

  private getKtx2Loader(): KTX2Loader | undefined {
    if (!this.ktx2Loader) {
      this.ktx2Loader = new KTX2Loader()
        .setTranscoderPath('/basis/');
      if (this.renderer) {
        this.ktx2Loader.detectSupport(this.renderer);
      }
    }
    return this.ktx2Loader;
  }

  private setTradeCollection(cards: Card[]) {
    this.userTradeCollection = Array.isArray(cards) ? cards : [];
    this.tradeCollectionRarities = this.getUniqueCardValues(this.userTradeCollection, (card) => card.rarity || 'Common');
    this.tradeCollectionTypes = this.getUniqueCardValues(this.userTradeCollection, (card) => card.tipo);
    this.tradeCollectionSupertypes = this.getUniqueCardValues(this.userTradeCollection, (card) => card.supertype);
    this.tradeCollectionSubtypes = this.getUniqueCardValues(this.userTradeCollection, (card) => card.subtypes || []);
    this.filtrarColeccionTrade();
  }

  private getUniqueCardValues(cards: Card[], picker: (card: Card) => string | string[] | undefined | null): string[] {
    const values = new Set<string>();
    cards.forEach((card) => {
      const raw = picker(card);
      const list = Array.isArray(raw) ? raw : [raw];
      list.forEach((value) => {
        const clean = (value || '').toString().trim();
        if (clean) values.add(clean);
      });
    });
    return Array.from(values).sort((a, b) => a.localeCompare(b));
  }

  // Carga los mazos visibles y rellena slots vacios.
  cargarMazosDeJugador(preferNewestDeck = false) {
    if (!this.jugador?.username) return;

    this.mazoService.getMazosByJugador(this.jugador.username).subscribe({
      next: (res: Mazo[]) => {
        this.mazos = res;
        const faltantes = 2 - this.mazos.length;
        this.slotsVacios = faltantes > 0 ? Array(faltantes).fill(0) : [];
        if (!this.debugTargetMazoId && this.mazos.length > 0) {
          this.debugTargetMazoId = this.mazos[0].id;
        }
        this.syncSelectedBattleDeck(preferNewestDeck);
        this.sincronizarCartaAReemplazar();
        this.cdr.detectChanges();
      }
    });
  }

  private getBattleDeckCandidates(): Mazo[] {
    return this.mazos.filter((mazo) => (mazo.cartas?.length ?? 0) === 60);
  }

  private syncSelectedBattleDeck(preferNewestDeck = false): number | null {
    const candidates = this.getBattleDeckCandidates();
    if (!candidates.length) {
      this.selectedBattleDeckId = null;
      return null;
    }

    const currentStillValid = candidates.some((mazo) => mazo.id === this.selectedBattleDeckId);
    if (currentStillValid && !preferNewestDeck) {
      return this.selectedBattleDeckId;
    }

    const selected = preferNewestDeck ? candidates[candidates.length - 1] : candidates[0];
    this.selectedBattleDeckId = selected.id;
    return selected.id;
  }

  private async ensureBattleDeckSelected(refresh = false): Promise<number | null> {
    const localSelection = this.syncSelectedBattleDeck(false);
    if (localSelection && !refresh) return localSelection;
    if (!this.jugador?.username) return localSelection;

    try {
      const res = await firstValueFrom(this.mazoService.getMazosByJugador(this.jugador.username));
      this.mazos = res;
      const faltantes = 2 - this.mazos.length;
      this.slotsVacios = faltantes > 0 ? Array(faltantes).fill(0) : [];
      if (!this.debugTargetMazoId && this.mazos.length > 0) {
        this.debugTargetMazoId = this.mazos[0].id;
      }
      const refreshedSelection = this.syncSelectedBattleDeck(false);
      this.sincronizarCartaAReemplazar();
      this.cdr.detectChanges();
      return refreshedSelection;
    } catch (err) {
      console.error('Error asegurando mazo para batalla', err);
      return localSelection;
    }
  }

  // Arma la tarjeta ampliada para inspeccion rapida.
  mostrarZoom(carta: Card, event: MouseEvent) {
    const pkm: PokemonZoomUI = {
      id: carta.id,
      nombre: carta.nombre,
      imagen: carta.imagen,
      pokedexId: carta.pokemonId || 1,
      hp: carta.hp || 70,
      tipo: carta.tipo || 'GRASS',
      attacks: carta.attacks || '',
      hpIcon: 'â™¥',
      typeIcon: 'Ã°Å¸ÂÆ’',
      attacksIcon: 'â€¢'
    };
    this.pkmZoom = pkm;
    this.actualizarPosicion(event);
  }

  // Cierra el zoom flotante.
  ocultarZoom() { this.pkmZoom = null; }

  // Reposiciona el zoom segun el mouse.
  actualizarPosicion(event: MouseEvent) {
    if (this.pkmZoom) {
      this.zoomX = event.clientX + 25;
      this.zoomY = event.clientY - 210;
      this.cdr.detectChanges();
    }
  }

  getImagenDebugCarta(id: string): string {
    return `/images/cards/${id}.png`;
  }

  // Abre un sobre y dispara la animacion de revelado.
  abrirSobres() {
    if (!this.jugador?.username || this.sobresDisponibles <= 0) return;

    this.sobreService.abrirSobre(this.jugador.username).subscribe({
      next: (res: Card[]) => {
        this.kioskShopOpen = false;
        this.vendorCameraFocus = false;
        this.cartasNuevas = res;
        this.mostrarAnimacionSobre = true;
        this.sobresDisponibles--;
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error('Error al abrir sobre', err);
      }
    });
  }

  // Cierra la experiencia de apertura y refresca el lobby.
  finalizarApertura() {
    console.log('Cerrando apertura de sobres...');
    this.cartasNuevas = [];
    this.mostrarAnimacionSobre = false;
    document.body.classList.remove('modal-open');
    this.cdr.detectChanges();
    this.refrescarTodo();
  }

  // Cierra el actual y abre otro inmediatamente.
  abrirOtroSobre() {
    this.mostrarAnimacionSobre = false;
    this.cdr.detectChanges();
    
    // Pequeño timeout para asegurar que Angular destruya y vuelva a crear el componente
    setTimeout(() => {
      this.abrirSobres();
    }, 50);
  }

  // Navega al editor de mazos.
  irAlDeckBuilder() {
    this.deckBuilderOpen = true;
    this.keys.clear();
    this.playerVelocity.set(0, 0, 0);
    this.setPlayerAnimation('idle');
    this.cdr.detectChanges();
  }

  cerrarDeckBuilder(refresh = false) {
    this.deckBuilderOpen = false;
    if (refresh) {
      this.refrescarTodo(true);
    }
    this.cdr.detectChanges();
  }

  // Crea una partida usando el mazo elegido.
  buscarPartida(mazoId: number) {
    if (!this.jugador?.username) return;
    this.battleService.startBattle(this.jugador!.username, mazoId).subscribe({
      next: (partida: Partida) => {
        if (partida && partida.id) {
          this.router.navigate(['/battle', partida.id]);
        }
      },
      error: (err) => console.error('Error al iniciar batalla', err)
    });
  }

  async startSelectedBattle() {
    const mazoId = await this.ensureBattleDeckSelected(true);
    if (!mazoId) {
      this.irAlDeckBuilder();
      return;
    }

    this.buscarPartida(mazoId);
  }

  get selectedCharacterLabel(): string {
    return this.characterOptions.find((option) => option.id === this.selectedCharacterId)?.label || 'Trainer';
  }

  openCustomizationModal() {
    this.customizerGender = (this.selectedCharacterId === 'ash' || this.selectedCharacterId === 'robot') ? 'pibe' : 'piba';
    this.customizerSelectedId = this.selectedCharacterId;
    this.customizerHeight = parseFloat(localStorage.getItem('lobbyHeight') || '1.0');
    this.customizerSkinColor = localStorage.getItem('lobbySkinColor') || '#ffe0bd';
    this.customizerHairColor = localStorage.getItem('lobbyHairColor') || '#5c4033';
    this.customizerEyeColor = localStorage.getItem('lobbyEyeColor') || '#2563eb';
    this.customizerPikachu = this.pikachuEnabled;
    this.customizerRotationY = 0;

    this.customizationModalOpen = true;
    this.characterMenuOpen = false;
    this.cdr.detectChanges();

    // Inicializar el escenario 3D de previsualizaciÃ³n en el modal
    this.initPreviewScene();
  }

  changeCustomizerGender(gender: 'pibe' | 'piba') {
    this.customizerGender = gender;
    this.customizerSelectedId = gender === 'pibe' ? 'ash' : 'hilda-sygna';
    
    // Vista previa instantÃ¡nea al cambiar de gÃ©nero en el 3D del modal
    this.onCustomizerModelSelect(this.customizerSelectedId);
    this.cdr.detectChanges();
  }

  onCustomizerModelSelect(id: string) {
    this.customizerSelectedId = id;
    
    this.ngZone.runOutsideAngular(() => {
      this.loadPreviewModel();
      this.loadPreviewPikachu();
    });
  }

  applyLivePreviewCustomizations() {
    const model = this.previewModelGroup.getObjectByName('PreviewCharacterModel') as THREE.Group;
    if (model) {
      this.applyPreviewCustomizationsDirect(model);
    }
    
    // Actualizar el Pikachu del preview si se des/activÃ³ en vivo
    this.loadPreviewPikachu();

    // Reproducir pose expresiva en el lienzo de vista previa
    this.playPreviewReactAnimation();
  }

  // ================= SCENE DE PREVISUALIZACIÃ“N 3D AISLADA (MODAL) =================
  private initPreviewScene() {
    this.ngZone.runOutsideAngular(() => {
      let attempts = 0;
      const tryInit = () => {
        attempts++;
        const canvas = document.querySelector('.customizer-preview-canvas') as HTMLCanvasElement;
        if (!canvas) {
          if (attempts < 8) {
            setTimeout(tryInit, 100);
          } else {
            console.error('Failed to locate preview canvas in customization modal after multiple attempts.');
          }
          return;
        }

        const width = canvas.clientWidth || 220;
        const height = canvas.clientHeight || 300;
        console.log(`Initializing preview scene on canvas: ${width}x${height}`);

        // Dispose existing preview renderer if any to prevent leaks
        if (this.previewRenderer) {
          this.destroyPreviewScene();
        }

        this.previewScene = new THREE.Scene();
        this.previewScene.background = null; // Fondo transparente para combinar con glassmorphism

        this.previewCamera = new THREE.PerspectiveCamera(45, width / height, 0.1, 20);
        this.previewCamera.position.set(0, 1.0, 2.0);
        this.previewCamera.lookAt(0, 0.82, 0);

        this.previewRenderer = new THREE.WebGLRenderer({
          canvas: canvas,
          alpha: true,
          antialias: true
        });
        this.previewRenderer.setPixelRatio(Math.min(window.devicePixelRatio, 1.35));
        this.previewRenderer.setSize(width, height, false);
        this.previewRenderer.shadowMap.enabled = true;

        // IluminaciÃ³n dedicada del preview
        const ambient = new THREE.AmbientLight(0xffffff, 1.6);
        this.previewScene.add(ambient);

        const dirLight = new THREE.DirectionalLight(0xffffff, 2.6);
        dirLight.position.set(1.5, 3.5, 2.5);
        dirLight.castShadow = true;
        this.previewScene.add(dirLight);

        // AÃ±adir el grupo de modelo
        this.previewModelGroup = new THREE.Group();
        this.previewScene.add(this.previewModelGroup);

        // Cargar los elementos iniciales
        this.loadPreviewModel();
        this.loadPreviewPikachu();

        // Iniciar bucle de animaciÃ³n
        this.animatePreview();
      };

      setTimeout(tryInit, 100);
    });
  }

  private loadPreviewModel() {
    if (!this.previewScene) return;

    this.previewModelGroup.clear();
    this.previewMixer?.stopAllAction();
    this.previewMixer = undefined;
    this.previewActions.clear();
    this.activePreviewAction = undefined;

    // Marcador de carga procedimental
    const bodyGeom = new THREE.CapsuleGeometry(0.18, 0.44, 6, 12);
    const bodyMat = new THREE.MeshStandardMaterial({ color: 0x3b82f6 });
    const fallbackMesh = new THREE.Mesh(bodyGeom, bodyMat);
    fallbackMesh.position.y = 0.44;
    this.previewModelGroup.add(fallbackMesh);

    const option = this.characterOptions.find((o) => o.id === this.customizerSelectedId) || this.characterOptions[0];
    const loader = this.createGltfLoader();
    loader.load(
      option.path,
      (gltf) => {
        this.previewModelGroup.clear();

        const model = gltf.scene;
        model.name = 'PreviewCharacterModel';
        
        model.scale.setScalar(option.scale);
        model.position.set(0, option.yOffset, 0);
        model.rotation.y = option.rotationY;
        
        model.traverse((child) => {
          if ((child as THREE.Mesh).isMesh) {
            child.castShadow = true;
            child.receiveShadow = true;
          }
        });

        // Aplicar rasgos elegidos en vivo
        this.applyPreviewCustomizationsDirect(model);

        this.previewModelGroup.add(model);
        this.previewMixer = new THREE.AnimationMixer(model);

        gltf.animations.forEach((clip) => {
          const action = this.previewMixer!.clipAction(clip);
          this.previewActions.set(clip.name.toLowerCase(), action);
        });

        // Pose idle
        const candidates = option.idleHints ?? ['idle', 'standing'];
        let idleAction: THREE.AnimationAction | undefined;
        for (const name of candidates) {
          const act = this.previewActions.get(name.toLowerCase());
          if (act) {
            idleAction = act;
            break;
          }
        }
        if (idleAction) {
          idleAction.play();
          this.activePreviewAction = idleAction;
        }
      },
      undefined,
      (err) => console.warn('Error loading GLB in preview', err)
    );
  }

  private applyPreviewCustomizationsDirect(model: THREE.Group) {
    const skinHex = this.customizerSkinColor;
    const hairHex = this.customizerHairColor;
    const eyeHex = this.customizerEyeColor;
    const heightFactor = this.customizerHeight;

    const option = this.characterOptions.find(o => o.id === this.customizerSelectedId) || this.characterOptions[0];
    model.scale.setScalar(option.scale * heightFactor);
    this.tintCharacterParts(model, { skinHex, hairHex, eyeHex });
  }

  private loadPreviewPikachu() {
    if (!this.previewScene) return;

    const existing = this.previewScene.getObjectByName('PreviewPikachu');
    if (existing) this.previewScene.remove(existing);

    if (!this.customizerPikachu) return;

    const loader = this.createGltfLoader();
    loader.load(
      '/models-optimized/characters/pikachu.glb',
      (gltf) => {
        const model = gltf.scene;
        model.name = 'PreviewPikachu';
        
        model.scale.setScalar(0.016);
        model.position.set(-0.35, 0, 0.15);
        model.rotation.y = Math.PI / 6;
        
        model.traverse((child) => {
          if ((child as THREE.Mesh).isMesh) {
            child.castShadow = true;
          }
        });

        this.previewScene?.add(model);

        const mixer = new THREE.AnimationMixer(model);
        if (gltf.animations.length > 0) {
          mixer.clipAction(gltf.animations[0]).play();
        }
        this.previewMixers.push(mixer);
      }
    );
  }

  private animatePreview() {
    if (!this.previewRenderer || !this.previewScene || !this.previewCamera) return;

    if (!this.showFirstTimeSetup && !this.customizationModalOpen) {
      this.destroyPreviewScene();
      return;
    }

    requestAnimationFrame(() => this.animatePreview());

    const delta = 0.016;
    this.previewMixer?.update(delta);
    this.previewMixers.forEach((m) => m.update(delta));

    this.previewRenderer.render(this.previewScene, this.previewCamera);
  }

  private destroyPreviewScene() {
    if (this.previewReactTimeout) {
      clearTimeout(this.previewReactTimeout);
      this.previewReactTimeout = undefined;
    }
    this.previewMixer?.stopAllAction();
    this.previewMixer = undefined;
    this.previewMixers.forEach((m) => m.stopAllAction());
    this.previewMixers = [];
    this.previewRenderer?.dispose();
    this.previewRenderer = undefined;
    this.previewScene = undefined;
    this.previewCamera = undefined;
  }

  private previewReactTimeout?: any;

  playPreviewReactAnimation() {
    if (!this.previewMixer || this.previewActions.size === 0) return;

    const reactKeywords = ['pose', 'talking', 'speak', 'greeting', 'yes', 'happy', 'wave', 'jump', 'dance', 'interact', 'look'];
    let reactAction: THREE.AnimationAction | undefined;

    for (const kw of reactKeywords) {
      for (const [name, action] of this.previewActions.entries()) {
        if (name.includes(kw) && !name.includes('walk') && !name.includes('run')) {
          reactAction = action;
          break;
        }
      }
      if (reactAction) break;
    }

    if (!reactAction) {
      for (const [name, action] of this.previewActions.entries()) {
        if (!name.includes('walk') && !name.includes('run') && name !== 'idle') {
          reactAction = action;
          break;
        }
      }
    }

    if (reactAction) {
      // Limpiar cualquier timeout previo para evitar cruce de animaciones superpuestas
      if (this.previewReactTimeout) {
        clearTimeout(this.previewReactTimeout);
        this.previewReactTimeout = undefined;
      }

      // Si hay otra animaciÃ³n activa que no es esta, desvanecerla
      if (this.activePreviewAction && this.activePreviewAction !== reactAction) {
        this.activePreviewAction.fadeOut(0.12);
      }

      // Forzar reinicio y reproducciÃ³n inmediata de la animaciÃ³n gestual
      reactAction.reset();
      reactAction.setLoop(THREE.LoopOnce, 1);
      reactAction.clampWhenFinished = true;
      reactAction.fadeIn(0.08).play();
      this.activePreviewAction = reactAction;

      const duration = reactAction.getClip().duration;
      // Programar la transiciÃ³n suave de regreso al estado Idle
      this.previewReactTimeout = setTimeout(() => {
        if (this.showFirstTimeSetup || this.customizationModalOpen) {
          const option = this.characterOptions.find((o) => o.id === this.customizerSelectedId) || this.characterOptions[0];
          const candidates = option.idleHints ?? ['idle', 'standing'];
          let idleAction: THREE.AnimationAction | undefined;
          for (const name of candidates) {
            const act = this.previewActions.get(name.toLowerCase());
            if (act) {
              idleAction = act;
              break;
            }
          }
          if (idleAction && this.activePreviewAction === reactAction) {
            idleAction.reset().fadeIn(0.18).play();
            reactAction.fadeOut(0.18);
            this.activePreviewAction = idleAction;
          }
        }
        this.previewReactTimeout = undefined;
      }, (duration * 1000) - 100);
    }
  }

  get previewRotationDegrees(): number {
    const deg = Math.round((this.customizerRotationY / (Math.PI * 2)) * 360) % 360;
    return deg < 0 ? deg + 360 : deg;
  }

  onRotationSliderChange() {
    if (this.previewModelGroup) {
      this.previewModelGroup.rotation.y = this.customizerRotationY;
    }
  }

  private isDraggingPreview = false;
  private lastPreviewPointerX = 0;

  onPreviewPointerDown(event: PointerEvent) {
    if (event.button === 0 || event.pointerType === 'touch') {
      this.isDraggingPreview = true;
      this.lastPreviewPointerX = event.clientX;
      const canvas = event.currentTarget as HTMLCanvasElement;
      try {
        canvas.setPointerCapture(event.pointerId);
      } catch (e) {}
    }
  }

  onPreviewPointerMove(event: PointerEvent) {
    if (this.isDraggingPreview) {
      const dx = event.clientX - this.lastPreviewPointerX;
      this.lastPreviewPointerX = event.clientX;
      
      this.customizerRotationY = (this.customizerRotationY - dx * 0.012) % (Math.PI * 2);
      if (this.customizerRotationY < 0) {
        this.customizerRotationY += Math.PI * 2;
      }
      
      this.onRotationSliderChange();
      this.cdr.detectChanges();
    }
  }

  onPreviewPointerUp(event: PointerEvent) {
    if (this.isDraggingPreview) {
      this.isDraggingPreview = false;
      const canvas = event.currentTarget as HTMLCanvasElement;
      try {
        canvas.releasePointerCapture(event.pointerId);
      } catch (e) {}
    }
  }

  saveCustomizations() {
    // Guardar cambios finales en localStorage
    localStorage.setItem('lobbyCharacter', this.customizerSelectedId);
    localStorage.setItem('lobbySkinColor', this.customizerSkinColor);
    localStorage.setItem('lobbyHairColor', this.customizerHairColor);
    localStorage.setItem('lobbyEyeColor', this.customizerEyeColor);
    localStorage.setItem('lobbyHeight', this.customizerHeight.toString());
    localStorage.setItem('pikachuCompanion', this.customizerPikachu ? 'true' : 'false');
    localStorage.setItem('firstTimeSetup', 'completed');

    this.selectedCharacterId = this.customizerSelectedId;
    this.pikachuEnabled = this.customizerPikachu;

    // Guardar en la base de datos
    if (this.jugador?.username) {
      this.jugadorService.guardarPersonalizacion(this.jugador.username, {
        characterId: this.customizerSelectedId,
        skinColor: this.customizerSkinColor,
        hairColor: this.customizerHairColor,
        eyeColor: this.customizerEyeColor,
        height: this.customizerHeight,
        pikachuCompanion: this.customizerPikachu
      }).subscribe({
        next: () => console.log('PersonalizaciÃ³n guardada en base de datos.'),
        error: (err) => console.error('Error al guardar personalizaciÃ³n en DB:', err)
      });
    }

    // Sincronizar Pikachu en el escenario principal del lobby
    if (this.pikachuEnabled) {
      if (!this.pikachu) {
        this.ngZone.runOutsideAngular(() => this.createPikachuCompanion());
      }
    } else {
      if (this.pikachu) {
        this.scene?.remove(this.pikachu.root);
        this.pikachu.mixer?.stopAllAction();
        this.pikachu = undefined;
      }
    }

    // Recargar modelo principal del jugador en el lobby
    this.ngZone.runOutsideAngular(() => this.loadAnimatedPlayerAsset());

    this.customizationModalOpen = false;
    this.showFirstTimeSetup = false;
    this.destroyPreviewScene();
    this.cdr.detectChanges();

    // Notificar a otros de nuestra personalizaciÃ³n actualizada
    this.sendJoinMessage();
  }

  cancelCustomizations() {
    this.customizationModalOpen = false;
    this.destroyPreviewScene();
    this.cdr.detectChanges();
  }

  selectCharacter(option: CharacterOption) {
    this.selectedCharacterId = option.id;
    this.characterMenuOpen = false;
    localStorage.setItem('lobbyCharacter', option.id);
    this.ngZone.runOutsideAngular(() => this.loadAnimatedPlayerAsset());
  }

  togglePikachuCompanion() {
    this.pikachuEnabled = !this.pikachuEnabled;
    localStorage.setItem('pikachuCompanion', String(this.pikachuEnabled));

    if (this.pikachuEnabled) {
      this.ngZone.runOutsideAngular(() => this.createPikachuCompanion());
    } else if (this.pikachu) {
      this.scene?.remove(this.pikachu.root);
      this.pikachu.mixer?.stopAllAction();
      this.pikachu = undefined;
    }
    // Notificar a otros
    this.sendJoinMessage();
  }

  warpToSpot(id: HubSpot['id']) {
    const spot = this.hubSpots.find((item) => item.id === id);
    if (!spot) return;

    this.player.position.set(spot.position.x, 0, spot.position.z + 3.2);
    this.player.rotation.y = Math.atan2(
      spot.position.x - this.player.position.x,
      spot.position.z - this.player.position.z
    );
  }

  interactWithCurrentSpot() {
    if (!this.currentInteraction) return;

    if (this.currentInteraction.id === 'deck') {
      this.irAlDeckBuilder();
      return;
    }

    if (this.currentInteraction.id === 'packs') {
      this.openKiosqueraDialog();
      return;
    }

    if (this.currentInteraction.id === 'santoro') {
      this.openSantoroDialog();
      return;
    }

    if (this.currentInteraction.id === 'battle') {
      if (this.mazos.length === 1) {
        this.buscarPartida(this.mazos[0].id);
      } else {
        this.selectedBattleDeckId = this.selectedBattleDeckId ?? this.mazos[0]?.id ?? null;
        this.battlePanelOpen = true;
      }
    }
  }

  // Carga el catalogo completo para el panel de debug del lobby.
  cargarCatalogoGodMode() {
    this.cardService.getAll().subscribe({
      next: (cartas: Card[]) => {
        this.debugCatalogoCompleto = cartas;
        this.aplicarFiltrosDebug();
        this.cdr.detectChanges();
      },
      error: (err) => console.error('Error cargando catalogo debug del lobby', err)
    });
  }

  actualizarFiltroTexto(event: Event) {
    const input = event.target as HTMLInputElement;
    this.debugSearchText = (input.value || '').toLowerCase();
    this.aplicarFiltrosDebug();
  }

  aplicarFiltrosDebug() {
    this.debugCatalogoFiltrado = this.debugCatalogoCompleto.filter((carta) => {
      if (!this.debugSearchText) return true;

      const nombre = carta.nombre?.toLowerCase() || '';
      const ataques = carta.ataques || [];
      const atacaMatch = ataques.some((ataque) =>
        (ataque.nombre || '').toLowerCase().includes(this.debugSearchText) ||
        (ataque.texto || '').toLowerCase().includes(this.debugSearchText)
      );

      return nombre.includes(this.debugSearchText) || atacaMatch;
    });

    this.debugSelectedIndex = 0;
  }

  get debugSelectedCard(): Card | null {
    if (!this.debugCatalogoFiltrado.length) return null;
    return this.debugCatalogoFiltrado[this.debugSelectedIndex] || null;
  }

  nextDebugCard() {
    if (!this.debugCatalogoFiltrado.length) return;
    this.debugSelectedIndex = (this.debugSelectedIndex + 1) % this.debugCatalogoFiltrado.length;
  }

  prevDebugCard() {
    if (!this.debugCatalogoFiltrado.length) return;
    this.debugSelectedIndex = (this.debugSelectedIndex - 1 + this.debugCatalogoFiltrado.length) % this.debugCatalogoFiltrado.length;
  }

  onDebugMazoChange(event: Event) {
    const select = event.target as HTMLSelectElement;
    this.debugTargetMazoId = select.value ? Number(select.value) : null;
    this.sincronizarCartaAReemplazar();
  }

  onDebugReplaceChange(event: Event) {
    const select = event.target as HTMLSelectElement;
    this.debugReplaceCardId = select.value || null;
  }

  get debugMazoSeleccionado(): Mazo | null {
    if (!this.debugTargetMazoId) return null;
    return this.mazos.find((mazo) => mazo.id === this.debugTargetMazoId) || null;
  }

  debugSetSobres() {
    if (!this.jugador?.username || this.debugAccionEnCurso) return;

    this.debugAccionEnCurso = true;
    this.jugadorService.debugSetSobres(this.jugador.username, this.debugSobresCantidad).subscribe({
      next: () => {
        this.debugAccionEnCurso = false;
        this.refrescarTodo();
      },
      error: (err) => {
        this.debugAccionEnCurso = false;
        console.error('Error seteando sobres en God Mode', err);
        alert(err.error || this.i18n.translate('alert.setPacksError'));
      }
    });
  }

  debugInyectarCartaEnMazo() {
    const carta = this.debugSelectedCard;
    const mazo = this.debugMazoSeleccionado;

    if (!carta || !mazo || this.debugAccionEnCurso) return;

    const requiereReemplazo = (mazo.cartas?.length || 0) >= 60;
    if (requiereReemplazo && !this.debugReplaceCardId) {
      alert(this.i18n.translate('alert.chooseCardToReplace'));
      return;
    }

    this.debugAccionEnCurso = true;
    this.mazoService.debugInjectCard(mazo.id, carta.id, this.debugReplaceCardId).subscribe({
      next: () => {
        this.debugAccionEnCurso = false;
        this.refrescarTodo();
      },
      error: (err) => {
        this.debugAccionEnCurso = false;
        console.error('Error inyectando carta en el mazo', err);
        alert(err.error || this.i18n.translate('alert.modifyDeckError'));
      }
    });
  }

  private initHubWorld() {
    const canvas = this.hubCanvas.nativeElement;
    this.renderer = new THREE.WebGLRenderer({
      canvas,
      antialias: this.graphicsQuality === 'high' && !this.isMobileViewport,
      alpha: false,
      powerPreference: 'high-performance'
    });
    this.updateRendererPixelRatio();
    this.getKtx2Loader()?.detectSupport(this.renderer);
    this.renderer.shadowMap.enabled = true;
    this.renderer.shadowMap.type = THREE.PCFSoftShadowMap;
    this.renderer.setClearColor(0x030712);

    this.scene = new THREE.Scene();
    this.scene.fog = new THREE.FogExp2(0x07111f, 0.019);

    if (this.isMobileViewport) {
      this.cameraZoomDistance = 4.8;
      this.cameraOrbitPitch = 0.18;
    }

    this.camera = new THREE.PerspectiveCamera(56, 1, 0.1, 180);
    this.camera.position.set(0, 5.2, 11.5);

    // Eventos de mouse y rueda para rotaciÃ³n (Ã³rbita) y zoom de la cÃ¡mara
    const onCameraPointerDown = (event: PointerEvent) => {
      this.cameraPointerId = event.pointerId;
      this.cameraPointerStartX = event.clientX;
      this.cameraPointerStartY = event.clientY;
      this.lastMouseX = event.clientX;
      this.lastMouseY = event.clientY;

      if (event.pointerType === 'mouse') {
        if (event.button !== 0) return;
        this.isDraggingCamera = true;
        return;
      }

      this.cameraHoldTimeout = setTimeout(() => {
        if (this.cameraPointerId === event.pointerId) {
          this.isDraggingCamera = true;
          canvas.setPointerCapture?.(event.pointerId);
        }
      }, 240);
    };

    const onCameraPointerMove = (event: PointerEvent) => {
      if (event.pointerId !== this.cameraPointerId) return;
      if (this.santoroDialogOpen || this.kiosqueraDialogOpen || this.kioskShopOpen) return;

      if (!this.isDraggingCamera && event.pointerType !== 'mouse') {
        const moved = Math.hypot(event.clientX - this.cameraPointerStartX, event.clientY - this.cameraPointerStartY);
        if (moved > 14 && this.cameraHoldTimeout) {
          clearTimeout(this.cameraHoldTimeout);
          this.cameraHoldTimeout = undefined;
        }
        return;
      }

      if (this.isDraggingCamera) {
        event.preventDefault();
        const dx = event.clientX - this.lastMouseX;
        const dy = event.clientY - this.lastMouseY;

        this.cameraOrbitYaw -= dx * 0.007;
        this.cameraOrbitPitch = Math.max(-0.4, Math.min(1.2, this.cameraOrbitPitch + dy * 0.005));

        this.lastMouseX = event.clientX;
        this.lastMouseY = event.clientY;
      }
    };

    const onCameraPointerUp = (event: PointerEvent) => this.stopCameraPointer(event);
    const onCameraWheel = (event: WheelEvent) => {
      event.preventDefault();
      if (this.santoroDialogOpen || this.kiosqueraDialogOpen || this.kioskShopOpen) return;
      this.cameraZoomDistance = Math.max(3.0, Math.min(18.0, this.cameraZoomDistance + event.deltaY * 0.007));
    };

    canvas.addEventListener('pointerdown', onCameraPointerDown);
    window.addEventListener('pointermove', onCameraPointerMove, { passive: false });
    window.addEventListener('pointerup', onCameraPointerUp);
    window.addEventListener('pointercancel', onCameraPointerUp);
    canvas.addEventListener('wheel', onCameraWheel, { passive: false });
    const onVisibilityChange = () => {
      this.pageVisible = !document.hidden;
      if (this.pageVisible) {
        this.clock.getDelta();
      }
    };
    document.addEventListener('visibilitychange', onVisibilityChange);
    this.hubEventCleanups.push(
      () => canvas.removeEventListener('pointerdown', onCameraPointerDown),
      () => window.removeEventListener('pointermove', onCameraPointerMove),
      () => window.removeEventListener('pointerup', onCameraPointerUp),
      () => window.removeEventListener('pointercancel', onCameraPointerUp),
      () => canvas.removeEventListener('wheel', onCameraWheel),
      () => document.removeEventListener('visibilitychange', onVisibilityChange)
    );

    this.createHubLights();
    this.createHubArena();
    this.createTrainerAvatar();
    this.createHubSpots();
    this.createSantoroNpc();
    this.updateSantoroQuestPath();
    this.createFloatingCards();
    this.resizeHub();
    window.addEventListener('resize', this.resizeHub);
  }

  private readonly resizeHub = () => {
    if (!this.renderer || !this.camera) return;

    const width = window.innerWidth;
    const height = window.innerHeight;
    this.renderer.setSize(width, height, false);
    this.camera.aspect = width / height;
    this.camera.updateProjectionMatrix();
  };

  private createHubLights() {
    if (!this.scene) return;

    this.ambientLight = new THREE.AmbientLight(0xbfe7ff, 0.95);
    this.scene.add(this.ambientLight);

    this.sunLight = new THREE.DirectionalLight(0xfff4c0, 3.4);
    this.sunLight.castShadow = true;
    this.sunLight.shadow.mapSize.set(1024, 1024);
    this.sunLight.shadow.camera.near = 1;
    this.sunLight.shadow.camera.far = 95;
    this.sunLight.shadow.camera.left = -42;
    this.sunLight.shadow.camera.right = 42;
    this.sunLight.shadow.camera.top = 42;
    this.sunLight.shadow.camera.bottom = -42;
    this.sunLight.shadow.bias = -0.0005;
    this.sunLight.shadow.normalBias = 0.015;
    this.scene.add(this.sunLight);

    this.moonLight = new THREE.DirectionalLight(0xc7d2fe, 0.25);
    this.moonLight.castShadow = true;
    this.moonLight.shadow.bias = -0.0005;
    this.moonLight.shadow.normalBias = 0.018;
    this.moonLight.position.set(-20, 18, 18);
    this.scene.add(this.moonLight);

    const sunGeometry = new THREE.SphereGeometry(9.5, 32, 24); // Esfera solar majestuosa
    const sunMaterial = new THREE.MeshBasicMaterial({ color: 0xfff2a3, fog: false });
    this.sunMesh = new THREE.Mesh(sunGeometry, sunMaterial);
    this.sunMesh.renderOrder = -19; // Domo de cielo trasero
    this.scene.add(this.sunMesh);
    this.disposable.push(sunGeometry, sunMaterial);

    const moonGeometry = new THREE.SphereGeometry(4.8, 28, 18); // Esfera lunar majestuosa
    const moonMaterial = new THREE.MeshBasicMaterial({ color: 0xe0e7ff, fog: false });
    this.moonMesh = new THREE.Mesh(moonGeometry, moonMaterial);
    this.moonMesh.renderOrder = -19; // Domo de cielo trasero
    this.scene.add(this.moonMesh);
    this.disposable.push(moonGeometry, moonMaterial);

    const skyGeometry = new THREE.SphereGeometry(120, 40, 24);
    const skyMaterial = new THREE.MeshBasicMaterial({ color: 0x87ceeb, side: THREE.BackSide, fog: false });
    this.skyDome = new THREE.Mesh(skyGeometry, skyMaterial);
    this.skyDome.renderOrder = -20; // Fondo absoluto
    this.scene.add(this.skyDome);
    this.disposable.push(skyGeometry, skyMaterial);

    this.createClouds();

    const kioskLight = new THREE.PointLight(0xffd37a, 2.2, 18);
    kioskLight.position.set(-18, 4.2, -18);
    this.scene.add(kioskLight);
    this.updateShadowQuality();
  }

  pressMobileKey(event: PointerEvent, key: string) {
    event.preventDefault();
    this.unlockLobbyAudio();
    (event.currentTarget as HTMLElement | null)?.setPointerCapture?.(event.pointerId);
    this.keys.add(key);
  }

  releaseMobileKey(event: PointerEvent, key: string) {
    event.preventDefault();
    this.keys.delete(key);
  }

  private stopCameraPointer(event: PointerEvent) {
    if (event.pointerId !== this.cameraPointerId) return;
    if (this.cameraHoldTimeout) {
      clearTimeout(this.cameraHoldTimeout);
      this.cameraHoldTimeout = undefined;
    }
    this.isDraggingCamera = false;
    this.cameraPointerId = null;
  }

  startMobileJoystick(event: PointerEvent) {
    event.preventDefault();
    this.unlockLobbyAudio();
    this.mobileJoystickActive = true;
    this.mobileJoystickPointerId = event.pointerId;
    (event.currentTarget as HTMLElement | null)?.setPointerCapture?.(event.pointerId);
    this.updateMobileJoystick(event);
  }

  moveMobileJoystick(event: PointerEvent) {
    if (!this.mobileJoystickActive || event.pointerId !== this.mobileJoystickPointerId) return;
    event.preventDefault();
    this.updateMobileJoystick(event);
  }

  endMobileJoystick(event: PointerEvent) {
    if (this.mobileJoystickPointerId !== null && event.pointerId !== this.mobileJoystickPointerId) return;
    event.preventDefault();
    this.mobileJoystickActive = false;
    this.mobileJoystickPointerId = null;
    this.mobileJoystickX = 0;
    this.mobileJoystickY = 0;
  }

  mobileCameraZoomStep(direction: number) {
    this.unlockLobbyAudio();
    this.cameraZoomDistance = Math.max(3.2, Math.min(10.5, this.cameraZoomDistance + direction * 0.8));
  }

  private updateMobileJoystick(event: PointerEvent) {
    const pad = event.currentTarget as HTMLElement;
    const rect = pad.getBoundingClientRect();
    const centerX = rect.left + rect.width / 2;
    const centerY = rect.top + rect.height / 2;
    const maxRadius = Math.min(rect.width, rect.height) * 0.34;
    const rawX = event.clientX - centerX;
    const rawY = event.clientY - centerY;
    const distance = Math.hypot(rawX, rawY);
    const scale = distance > maxRadius ? maxRadius / distance : 1;
    const x = rawX * scale;
    const y = rawY * scale;

    this.mobileJoystickX = Math.abs(x) < 4 ? 0 : x / maxRadius;
    this.mobileJoystickY = Math.abs(y) < 4 ? 0 : y / maxRadius;
  }

  private requestLandscapeOrientation() {
    // No bloqueamos orientacion: solo mostramos una sugerencia descartable en mobile.
  }

  dismissLandscapeHint() {
    this.landscapeHintDismissed = true;
    localStorage.setItem('lobbyLandscapeHintDismissed', 'true');
  }

  openKiosqueraDialog() {
    this.stopSantoroTracking();
    this.kioskShopOpen = false;
    this.battlePanelOpen = false;
    this.santoroDialogOpen = false;
    this.kiosqueraDialogOpen = true;
    this.vendorCameraFocus = true; // Enfocar cámara en la kiosquera
    if (this.kioskVendor) {
      this.setNpcAnimation(this.kioskVendor, ['mouth_01', 'speak_1', 'greeting_1']);
    }
    this.cdr.detectChanges();
  }

  closeKiosqueraDialog() {
    this.kiosqueraDialogOpen = false;
    this.vendorCameraFocus = false;
    if (this.kioskVendor) {
      this.setNpcAnimation(this.kioskVendor, ['idle']);
    }
    this.cdr.detectChanges();
  }

  kiosqueraVerProductos() {
    this.kiosqueraDialogOpen = false;
    this.openKioskShop();
  }

  openKioskShop() {
    this.kiosqueraDialogOpen = false;
    this.kioskShopOpen = true;
    this.vendorCameraFocus = true;
    this.battlePanelOpen = false;
    this.ensureDetailedKioskProductsLoaded();
    if (this.kioskVendor) {
      this.setNpcAnimation(this.kioskVendor, ['mouth_01', 'speak_1', 'greeting_1']);
    }
    this.cdr.detectChanges();
  }

  closeKioskShop() {
    this.kioskShopOpen = false;
    this.vendorCameraFocus = false;
    this.cdr.detectChanges();
  }

  private playVendorPurchaseAnimation() {
    if (!this.kioskVendor) return;
    const npc = this.kioskVendor;
    const poseEntry = Array.from(npc.actions.entries()).find(([name]) => name.includes('pose_03'));
    if (poseEntry) {
      const action = poseEntry[1];
      npc.active?.fadeOut(0.08);
      action.stop();
      action.reset();
      action.setLoop(THREE.LoopOnce, 1);
      action.clampWhenFinished = false;
      action.fadeIn(0.12).play();
      npc.active = action;
    }
  }

  buyPackBundle(amount: number) {
    if (!this.jugador?.username) return;
    const cost = this.getPackBundlePrice(amount);
    if (this.santoCoins < cost) return;

    this.jugadorService.buyPacks(this.jugador.username, amount).subscribe({
      next: (res) => {
        this.sobresDisponibles = res.sobresDisponibles ?? this.sobresDisponibles;
        this.santoCoins = res.santoCoins ?? this.santoCoins;
        this.debugSobresCantidad = this.sobresDisponibles;
        this.jugador = { ...this.jugador!, ...res };
        this.playVendorPurchaseAnimation();
        this.cdr.detectChanges();
      },
      error: (err) => console.error('Error comprando sobres en kiosco', err)
    });
  }

  getPackBundlePrice(amount: number): number {
    return this.packBundlePrices[amount] ?? 999999;
  }

  canBuyPackBundle(amount: number): boolean {
    return this.santoCoins >= this.getPackBundlePrice(amount);
  }

  get santoroQuestTitle(): string {
    return this.santoroGiftClaimed
      ? this.i18n.translate('lobby.quest.santoroUnlocked')
      : this.i18n.translate('lobby.quest.findSantoro');
  }

  get santoroQuestCopy(): string {
    return this.santoroGiftClaimed
      ? this.i18n.translate('lobby.quest.santoroUnlockedDesc')
      : this.i18n.translate('lobby.quest.findSantoroDesc');
  }

  get santoroNickname(): string {
    const female = this.selectedCharacterId === 'hilda-sygna' || this.selectedCharacterId === 'lillie';
    return female ? 'Cacha' : 'Cacho';
  }

  toggleSantoroTracking() {
    if (!this.jugador?.username) return;
    const nextTracking = !this.santoroQuestTracking;
    this.jugadorService.setSantoroTracking(this.jugador.username, nextTracking).subscribe({
      next: (quest) => {
        this.applySantoroQuest(quest);
        this.updateSantoroQuestPath();
        this.cdr.detectChanges();
      },
      error: (err) => console.error('Error actualizando tracking de Santoro', err)
    });
  }

  stopSantoroTracking() {
    if (!this.jugador?.username) {
      this.santoroQuestTracking = false;
      this.updateSantoroQuestPath();
      return;
    }
    this.jugadorService.setSantoroTracking(this.jugador.username, false).subscribe({
      next: (quest) => {
        this.applySantoroQuest(quest);
        this.updateSantoroQuestPath();
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error('Error frenando tracking de Santoro', err);
        this.santoroQuestTracking = false;
        this.updateSantoroQuestPath();
      }
    });
  }

  openSantoroDialog() {
    this.stopSantoroTracking();
    this.kioskShopOpen = false;
    this.battlePanelOpen = false;
    this.vendorCameraFocus = false;
    this.santoroDialogState = this.santoroGiftClaimed ? 'repeat' : 'intro';
    this.santoroDialogOpen = true;
    if (this.santoroNpc) this.setNpcAnimation(this.santoroNpc, ['point', 'thumbsUp', 'idle_eyes'], 0.12);
    this.cdr.detectChanges();
  }

  closeSantoroDialog() {
    this.santoroDialogOpen = false;
    this.santoroGiftModalOpen = false;
    if (this.santoroNpc) this.setNpcAnimation(this.santoroNpc, ['idle_eyes', 'idle'], 0.16);
    this.cdr.detectChanges();
  }

  chooseSantoroAnswer(kind: 'thanks' | 'reject') {
    this.santoroDialogState = kind === 'thanks' ? 'thanks' : 'forced';
    if (this.santoroNpc) this.setNpcAnimation(this.santoroNpc, kind === 'thanks' ? ['thumbsUp', 'allOpen'] : ['point', 'thumbDown'], 0.1);
  }

  acceptSantoroGift() {
    if (!this.jugador?.username || this.santoroRewardLoading) return;
    this.santoroRewardLoading = true;

    this.jugadorService.claimSantoroGift(this.jugador.username).subscribe({
      next: (quest) => {
        this.applySantoroQuest(quest);
        this.santoroRewardLoading = false;
        this.santoroDialogState = 'gift';
        this.santoroGiftModalOpen = true;
        if (this.santoroNpc) this.setNpcAnimation(this.santoroNpc, ['thumbsUp', 'allOpen', 'idle_eyes'], 0.1);
        this.updateSantoroQuestPath();
        this.refrescarTodo();
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.santoroRewardLoading = false;
        console.error('Error entregando regalo de Santoro', err);
        this.cdr.detectChanges();
      }
    });
  }

  continueSantoroAfterGift() {
    this.santoroGiftModalOpen = false;
    this.santoroDialogState = 'afterGift';
    this.cdr.detectChanges();
  }

  santoroManageDeck() {
    this.closeSantoroDialog();
    this.irAlDeckBuilder();
  }

  private createSantoroNpc() {
    if (!this.scene || this.santoroNpc) return;
    const spot = this.hubSpots.find((item) => item.id === 'santoro');
    if (!spot) return;

    const root = new THREE.Group();
    root.position.set(spot.position.x + 0.15, this.getGroundHeightAt(spot.position.x, spot.position.z) + this.groundLift, spot.position.z - 1.05);
    root.rotation.y = 0; // Por defecto mira hacia el centro/calle
    this.scene.add(root);

    const npc: CampusNpc = { root, actions: new Map() };
    this.santoroNpc = npc;

    this.loadSceneAsset('/models-optimized/characters/santoro.glb', (model, animations) => {
      model.name = 'SantoroNPC';
      model.scale.set(1.42, 1.18, 1.34);
      this.centerModelPivot(model);
      this.alignModelBottom(model, 0);
      model.rotation.y = 0;
      this.applySantoroBasePose(model);

      // Recopilar huesos de Santoro y guardar su pose original
      this.santoroBones = {};
      this.santoroDefaultQuaternions.clear();
      model.traverse((child) => {
        if (child instanceof THREE.Mesh) {
          if (child.material && (child.material as any).name === 'Streamoji_Outfit_Top') {
            const mat = child.material as THREE.MeshStandardMaterial;
            mat.map = null;
            mat.color.setHex(0xc0c0c0);
            mat.needsUpdate = true;
          }
        }
        if (child instanceof THREE.Bone) {
          const name = child.name.toLowerCase();
          const bone = child as THREE.Bone;
          if (name === 'spine') this.santoroBones.spine = bone;
          else if (name === 'spine1') this.santoroBones.spine1 = bone;
          else if (name === 'spine2') this.santoroBones.spine2 = bone;
          else if (name === 'neck') this.santoroBones.neck = bone;
          else if (name === 'head') this.santoroBones.head = bone;
          else if (name === 'leftarm') this.santoroBones.leftArm = bone;
          else if (name === 'rightarm') this.santoroBones.rightArm = bone;
          else if (name === 'leftforearm') this.santoroBones.leftForeArm = bone;
          else if (name === 'rightforearm') this.santoroBones.rightForeArm = bone;
          else if (name === 'leftshoulder') this.santoroBones.leftShoulder = bone;
          else if (name === 'rightshoulder') this.santoroBones.rightShoulder = bone;

          this.santoroDefaultQuaternions.set(bone, bone.quaternion.clone());
        }
      });

      root.add(model);

      npc.mixer = new THREE.AnimationMixer(model);
      animations.forEach((clip) => npc.actions.set(clip.name.toLowerCase(), npc.mixer!.clipAction(clip)));
      this.setNpcAnimation(npc, ['idle_eyes_2', 'idle_eyes', 'idle'], 0);
    });
  }

  private applySantoroBasePose(model: THREE.Group) {
    // Las animaciones nativas de santoro.glb se encargan de posicionar las articulaciones de forma natural.
  }

  private animateSantoroBones(delta: number) {
    if (!this.santoroNpc) return;
    const time = this.clock.elapsedTime;
    const bones = this.santoroBones;
    const defaults = this.santoroDefaultQuaternions;

    const resetBone = (bone: THREE.Bone | undefined) => {
      if (bone && defaults.has(bone)) {
        bone.quaternion.copy(defaults.get(bone)!);
      }
    };

    // Restablecer pose original de Santoro (que ya es una postura relajada con brazos caídos)
    resetBone(bones.spine);
    resetBone(bones.spine1);
    resetBone(bones.spine2);
    resetBone(bones.neck);
    resetBone(bones.head);
    resetBone(bones.leftArm);
    resetBone(bones.rightArm);
    resetBone(bones.leftForeArm);
    resetBone(bones.rightForeArm);

    // Ejes locales del hueso
    const localX = new THREE.Vector3(1, 0, 0);
    const localY = new THREE.Vector3(0, 1, 0);
    const localZ = new THREE.Vector3(0, 0, 1);

    // 1. Respiración (pecho y columna, oscilaciones en eje local X)
    // Simula inhalar y exhalar a una frecuencia natural tranquila
    const breath = Math.sin(time * 1.8);
    if (bones.spine) bones.spine.rotateOnAxis(localX, -breath * 0.015 - 0.005);
    if (bones.spine1) bones.spine1.rotateOnAxis(localX, -breath * 0.012);
    if (bones.spine2) bones.spine2.rotateOnAxis(localX, -breath * 0.008);

    // 2. Cabeza y cuello (mirar a los lados / gesticular)
    if (bones.neck) {
      if (this.santoroDialogOpen) {
        bones.neck.rotateOnAxis(localY, Math.cos(time * 2.6) * 0.06);
        bones.neck.rotateOnAxis(localX, Math.sin(time * 4.2) * 0.05 + 0.02);
      } else {
        // Actualizar el temporizador de mirada libre
        this.santoroGazeTimer -= delta;
        if (this.santoroGazeTimer <= 0) {
          // 60% chance de mirar al frente (yaw = 0), 40% de mirar a algún costado
          if (Math.random() < 0.6) {
            this.santoroTargetHeadYaw = 0;
            this.santoroTargetHeadPitch = 0;
          } else {
            this.santoroTargetHeadYaw = (Math.random() - 0.5) * 0.7; // +/- 20 grados
            this.santoroTargetHeadPitch = (Math.random() - 0.5) * 0.12; // +/- 3.5 grados
          }
          this.santoroGazeTimer = 3.5 + Math.random() * 4.5; // mantener mirada por 3.5 a 8s
        }

        // Interpolación fluida hacia la dirección de mirada objetivo
        this.santoroHeadYaw = THREE.MathUtils.lerp(this.santoroHeadYaw, this.santoroTargetHeadYaw, delta * 3.0);
        this.santoroHeadPitch = THREE.MathUtils.lerp(this.santoroHeadPitch, this.santoroTargetHeadPitch, delta * 3.0);

        bones.neck.rotateOnAxis(localY, this.santoroHeadYaw * 0.6);
        bones.neck.rotateOnAxis(localX, this.santoroHeadPitch * 0.6);
      }
    }

    if (bones.head) {
      if (this.santoroDialogOpen) {
        bones.head.rotateOnAxis(localX, Math.abs(Math.sin(time * 4.2)) * 0.06 - 0.03);
        bones.head.rotateOnAxis(localZ, Math.cos(time * 0.85) * 0.025);
      } else {
        // En reposo, inclinar ligeramente la cabeza acompañando la rotación (roll y yaw natural)
        bones.head.rotateOnAxis(localY, this.santoroHeadYaw * 0.4);
        bones.head.rotateOnAxis(localX, this.santoroHeadPitch * 0.4);
        bones.head.rotateOnAxis(localZ, this.santoroHeadYaw * 0.15); // roll lateral
      }
    }

    // 3. Brazos relajados / gestos al hablar
    if (bones.leftArm && bones.rightArm) {
      let leftArmPitch = 0; 
      let rightArmPitch = 0;
      let leftArmYaw = 0; 
      let rightArmYaw = 0; 
      
      let leftForeArmYaw = 0; // Hinge del codo en eje local Z
      let rightForeArmYaw = 0; // Hinge del codo en eje local Z

      if (this.santoroDialogOpen) {
        // Hablando: Gestos de ademanes conversacionales de explicación
        // Levantar y llevar los brazos hacia adelante (Pitch X y Yaw Z combinados)
        leftArmPitch = 0.25 + Math.sin(time * 2.8) * 0.05;
        leftArmYaw = 0.70 + Math.cos(time * 2.4) * 0.08;
        leftForeArmYaw = 0.85 + Math.sin(time * 2.8) * 0.15; // Codo izquierdo doblado hacia adelante/arriba

        rightArmPitch = 0.27 + Math.cos(time * 2.5) * 0.05;
        rightArmYaw = -0.68 - Math.sin(time * 2.3) * 0.08;
        rightForeArmYaw = -0.85 - Math.cos(time * 2.5) * 0.15; // Codo derecho doblado hacia adelante/arriba (mirrored)
      } else {
        // Pasivo/Idle: Pose de caída natural cerrada al costado del cuerpo, con sutiles oscilaciones por respiración.
        // Los ángulos base (0.55 Pitch, +/-0.05 Yaw, -/+0.45 ForeArmYaw) anulan la apertura A-pose y los hacen colgar rectos.
        leftArmPitch = 0.55 + breath * 0.015;
        leftArmYaw = 0.05 + breath * 0.02;
        leftForeArmYaw = -0.45 + breath * 0.008;

        rightArmPitch = 0.55 + breath * 0.015;
        rightArmYaw = -0.05 - breath * 0.02;
        rightForeArmYaw = 0.45 - breath * 0.008;
      }

      // Aplicar las rotaciones sobre los ejes locales de los brazos
      bones.leftArm.rotateOnAxis(localX, leftArmPitch);
      bones.leftArm.rotateOnAxis(localZ, leftArmYaw);
      
      bones.rightArm.rotateOnAxis(localX, rightArmPitch);
      bones.rightArm.rotateOnAxis(localZ, rightArmYaw);

      if (bones.leftForeArm) bones.leftForeArm.rotateOnAxis(localZ, leftForeArmYaw);
      if (bones.rightForeArm) bones.rightForeArm.rotateOnAxis(localZ, rightForeArmYaw);
    }
  }

  private updateSantoroNpc(delta: number) {
    if (!this.santoroNpc) return;
    this.santoroNpc.mixer?.update(delta);

    // Animación de huesos en cada frame
    this.animateSantoroBones(delta);

    // Seguimiento del jugador en cada frame
    const dx = this.player.position.x - this.santoroNpc.root.position.x;
    const dz = this.player.position.z - this.santoroNpc.root.position.z;
    const distSq = dx * dx + dz * dz;

    let targetAngle = 0; // Por defecto mira de frente al campus
    if (distSq < 42) {
      targetAngle = Math.atan2(dx, dz);
    }

    this.santoroNpc.root.rotation.y = THREE.MathUtils.lerp(
      this.santoroNpc.root.rotation.y,
      targetAngle,
      0.16
    );
  }

  private updateSantoroQuestPath() {
    if (!this.scene) return;
    if (this.santoroPathGroup) {
      this.scene.remove(this.santoroPathGroup);
      this.santoroPathGroup.traverse((child) => {
        const mesh = child as THREE.Mesh;
        mesh.geometry?.dispose?.();
        const mat = mesh.material as THREE.Material | THREE.Material[] | undefined;
        if (Array.isArray(mat)) mat.forEach((m) => m.dispose());
        else mat?.dispose?.();
      });
      this.santoroPathGroup = undefined;
    }

    if (!this.santoroQuestTracking || this.santoroGiftClaimed) return;
    const spot = this.hubSpots.find((item) => item.id === 'santoro');
    if (!spot) return;

    const group = new THREE.Group();
    group.name = 'SantoroQuestPath';
    const start = this.player.position.clone();
    const end = spot.position.clone();
    const direction = end.clone().sub(start);
    const distance = Math.hypot(direction.x, direction.z);
    if (distance < 1) return;
    direction.normalize();

    const arrowGeometry = new THREE.ConeGeometry(0.22, 0.62, 3);
    const arrowMaterial = new THREE.MeshBasicMaterial({
      color: 0xfacc15,
      transparent: true,
      opacity: 0.86,
      blending: THREE.AdditiveBlending,
      depthWrite: false
    });

    const count = Math.min(18, Math.max(4, Math.floor(distance / 1.35)));
    for (let i = 1; i <= count; i++) {
      const t = i / (count + 1);
      const x = start.x + (end.x - start.x) * t;
      const z = start.z + (end.z - start.z) * t;
      const arrow = new THREE.Mesh(arrowGeometry.clone(), arrowMaterial.clone());
      arrow.position.set(x, this.getGroundHeightAt(x, z) + 0.08, z);
      arrow.rotation.x = -Math.PI / 2;
      arrow.rotation.z = Math.atan2(direction.x, direction.z);
      arrow.userData = { phase: i * 0.35 };
      group.add(arrow);
    }

    this.santoroPathGroup = group;
    this.scene.add(group);
  }

  private updateSantoroPathArrows(elapsed: number) {
    if (!this.santoroPathGroup) return;
    this.santoroPathGroup.children.forEach((child) => {
      const arrow = child as THREE.Mesh;
      const pulse = 0.68 + Math.sin(elapsed * 4 + (arrow.userData['phase'] || 0)) * 0.24;
      arrow.scale.setScalar(0.82 + pulse * 0.28);
      const mat = arrow.material as THREE.MeshBasicMaterial;
      if (mat) mat.opacity = 0.52 + pulse * 0.32;
    });
  }

  private createClouds() {
    if (!this.scene) return;

    this.loadSceneAsset('/models-optimized/cloud_polygon_opt.glb', (loadedModel) => {
      // Material básico semi-transparente sin afectación por niebla para visualización en el fondo celeste
      const cloudMaterial = new THREE.MeshBasicMaterial({
        color: 0xffffff,
        transparent: true,
        opacity: 0.86,
        fog: false
      });
      this.disposable.push(cloudMaterial);

      // Reemplazar material
      loadedModel.traverse((child) => {
        if (child instanceof THREE.Mesh) {
          child.material = cloudMaterial;
          child.castShadow = false;
          child.receiveShadow = false;
        }
      });

      // Generar 12 nubes tridimensionales optimizadas flotantes clonando el modelo completo para conservar las escalas locales
      for (let i = 0; i < 12; i++) {
        const cloud = new THREE.Group();
        cloud.name = `OptimizedCloud_${i}`;
        const angle = (i / 12) * Math.PI * 2;
        const radius = 48 + (i % 4) * 8;
        cloud.position.set(Math.cos(angle) * radius, 21 + (i % 3) * 3.5, Math.sin(angle) * radius - 8);
        cloud.userData = { angle, radius, speed: 0.007 + i * 0.0012, y: cloud.position.y };

        const clone = loadedModel.clone();
        // El tamaño base con la escala interna del modelo de Blender es ~1.5m.
        // Lo escalamos a un tamaño majestuoso y natural de nube en el cielo
        const scaleVal = 6.0 + (i % 3) * 2.5; // Escalas de 6x a 11x (tamaño de ~10m a ~17m)
        clone.scale.set(scaleVal, scaleVal * 0.5, scaleVal * 0.75);
        clone.rotation.set(Math.random() * 0.1, Math.random() * Math.PI * 2, Math.random() * 0.1);
        cloud.add(clone);

        this.clouds.push(cloud);
        this.worldObjects.push(cloud);
        this.scene!.add(cloud);
      }
    });
  }

  private updateDayNightCycle(_elapsed: number) {
    if (!this.scene || !this.sunLight || !this.moonLight || !this.ambientLight || !this.sunMesh || !this.moonMesh || !this.skyDome) return;

    const syncedElapsed = Date.now() / 1000;
    const t = (syncedElapsed % this.cycleSeconds) / this.cycleSeconds;
    const orbit = t * Math.PI * 2 - Math.PI * 0.08;
    const sunHeight = Math.sin(orbit);
    
    // OptimizaciÃ³n de sombras segÃºn la calidad grÃ¡fica activa
    const sunVisible = sunHeight > 0.03;
    const shadowMode =
      this.graphicsQuality === 'low'
        ? 'none'
        : this.graphicsQuality === 'medium'
          ? (sunVisible ? 'sun' : 'moon')
          : 'both';
    if (shadowMode !== this.lastShadowMode) {
      this.sunLight.castShadow = shadowMode === 'sun' || shadowMode === 'both';
      this.moonLight.castShadow = shadowMode === 'moon' || shadowMode === 'both';
      this.lastShadowMode = shadowMode;
    }
    // 1. Posicionamiento original de luces direccionales para mantener direcciÃ³n de sombras perfecta
    const sunX = Math.cos(orbit) * 58;
    const sunY = Math.max(-12, sunHeight * 42);
    const sunZ = -24 + Math.sin(t * Math.PI * 2 + 0.7) * 16;
    this.sunLight.position.set(sunX, sunY, sunZ);
    this.moonLight.position.set(-sunX, Math.max(8, -sunY), -sunZ);

    // 2. Posicionamiento fÃ­sico de las mallas visuales (sunMesh y moonMesh) a gran distancia (106)
    // Esto las sitÃºa fuera de la caja de montaÃ±as (Z/X = 78) pero dentro del domo celeste (radio 120),
    // resolviendo el problema de oclusiÃ³n de forma fÃ­sica y matemÃ¡ticamente perfecta.
    const rawX = Math.cos(orbit);
    const rawY = Math.sin(orbit);
    const rawZ = -0.32 + Math.sin(orbit) * 0.15; // InclinaciÃ³n celeste diagonal hermosa
    this.tempVec3.set(rawX, rawY, rawZ).normalize();
    this.sunMesh.position.copy(this.tempVec3).multiplyScalar(106.0);
    this.moonMesh.position.copy(this.tempVec3).multiplyScalar(-106.0);

    // 3. Cálculo de factores de fase dinámica mediante un sistema de pesos ponderados
    // sunrise/sunset: transición suave que evita el solapamiento o descoloramiento.
    let distToSunrise = Math.abs(t - 0.02);
    if (distToSunrise > 0.5) distToSunrise = 1.0 - distToSunrise;
    const sunriseFactor = Math.max(0, 1 - distToSunrise / 0.06);

    const sunsetFactor = Math.max(0, 1 - Math.abs(t - 0.58) / 0.10);
    const twilight = Math.max(sunriseFactor, sunsetFactor);

    let wDay = 0;
    if (t >= 0.02 && t < 0.08) {
      wDay = (t - 0.02) / 0.06;
    } else if (t >= 0.08 && t < 0.48) {
      wDay = 1.0;
    } else if (t >= 0.48 && t < 0.58) {
      wDay = 1.0 - (t - 0.48) / 0.10;
    }

    let wNight = 0;
    if (t >= 0.58 && t < 0.68) {
      wNight = (t - 0.58) / 0.10;
    } else if (t >= 0.68 && t < 0.96) {
      wNight = 1.0;
    } else {
      let val = t >= 0.96 ? t : t + 1.0;
      wNight = Math.max(0, 1.0 - (val - 0.96) / 0.06);
    }

    const wTwilight = twilight;
    const daylight = wDay + wTwilight;
    const night = wNight;

    // 4. Ajustes de intensidades lumínicas
    this.sunLight.intensity = wDay * 3.8 + wTwilight * 1.6;
    this.moonLight.intensity = wNight * 1.35 + wTwilight * 0.15;
    this.ambientLight.intensity = wDay * 1.05 + wTwilight * 0.48 + wNight * 0.22;

    // 5. Gradiente dinámico de color del domo del cielo usando pesos
    this.dynamicSkyColor.setRGB(
      this.skyDayColor.r * wDay + this.skySunsetColor.r * wTwilight + this.skyNightColor.r * wNight,
      this.skyDayColor.g * wDay + this.skySunsetColor.g * wTwilight + this.skyNightColor.g * wNight,
      this.skyDayColor.b * wDay + this.skySunsetColor.b * wTwilight + this.skyNightColor.b * wNight
    );
    (this.skyDome.material as THREE.MeshBasicMaterial).color.copy(this.dynamicSkyColor);
    
    this.dynamicFogColor.copy(this.dynamicSkyColor).lerp(this.fogBaseColor, 0.55 * wNight);
    this.scene.fog?.color.copy(this.dynamicFogColor);
    this.renderer?.setClearColor(this.dynamicSkyColor);

    // Tinte dinámico de las montañas según el ciclo (lerpeado hacia naranja en atardecer/amanecer y oscurecido en noche)
    this.mountainMaterials.forEach((mat) => {
      const base = mat.userData['baseColor'] as THREE.Color;
      if (base) {
        mat.color.copy(base)
          .lerp(this.skySunsetColor, wTwilight * 0.65)
          .lerp(this.skyNightColor, wNight * 0.75);
      }
    });

    // 6. Tinte de las luces direccionales y ambientales
    this.sunLight.color.copy(this.sunBaseColor).lerp(this.skySunsetColor, wTwilight * 0.95);
    this.ambientLight.color.copy(this.whiteColor)
      .lerp(this.skySunsetColor, wTwilight * 0.88)
      .lerp(this.moonAmbientColor, wNight * 0.5);

    // 7. Tinte dinámico del Sol majestuoso: de amarillo a rojo-naranja ardiente al atardecer/amanecer
    this.dynamicSunMeshColor.copy(this.sunMeshDayColor).lerp(this.sunMeshSunsetColor, wTwilight * 0.98);
    (this.sunMesh.material as THREE.MeshBasicMaterial).color.copy(this.dynamicSunMeshColor);

    // 8. Tinte dinámico de la Luna: de índigo plateado a rosáceo suave al cruzarse con el crepúsculo
    this.dynamicMoonMeshColor.copy(this.moonMeshNightColor).lerp(this.moonMeshSunsetColor, wTwilight * 0.5);
    (this.moonMesh.material as THREE.MeshBasicMaterial).color.copy(this.dynamicMoonMeshColor);

    this.sunMesh.visible = daylight > 0.05;
    this.moonMesh.visible = (night + wTwilight * 0.5) > 0.15;

    // Control dinámico de faroles: encendidos de noche, apagados de día
    this.lampLights.forEach((light) => {
      light.intensity = night * 4.8;
    });

    this.lampEmissiveMaterials.forEach((material) => {
      if ('emissiveIntensity' in material) {
        (material as any).emissiveIntensity = night * 4.5;
      }
      if ('emissive' in material) {
        (material as any).emissive.setHex(0xfff1a6).multiplyScalar(night);
      }
    });

    let nextLabel = 'lobby.phase.morning';
    if (t >= 0.08 && t < 0.48) {
      nextLabel = 'lobby.phase.afternoon';
    } else if (t >= 0.48 && t < 0.68) {
      nextLabel = 'lobby.phase.sunset';
    } else if (t >= 0.68 && t < 0.96) {
      nextLabel = 'lobby.phase.night';
    }
    if (nextLabel !== this.dayPhaseLabel) {
      this.ngZone.run(() => {
        this.dayPhaseLabel = nextLabel;
        this.cdr.detectChanges();
      });
    }
  }

  private createHubArena() {
    if (!this.scene) return;

    const floorGeometry = new THREE.PlaneGeometry(160, 160);
    const floorMaterial = new THREE.MeshStandardMaterial({
      map: this.createGrassTexture(),
      color: 0xe9ffe0,
      roughness: 0.96
    });
    const floor = new THREE.Mesh(floorGeometry, floorMaterial);
    floor.rotation.x = -Math.PI / 2;
    floor.receiveShadow = true;
    this.scene.add(floor);
    this.disposable.push(floorGeometry, floorMaterial);

    this.createCampusWalkway();
    this.createMainUtnBuilding();
    this.createConeRoofBuildingAndKiosk();
    this.createCampusTrees();
    this.createMountains();
    this.createCampusBackgroundDetails();
    this.createAmbientPokemon();
    this.createUtnFrcCampusAdditions();

    // Map boundary visual forcefield
    const boundaryGeom = new THREE.CylinderGeometry(30.5, 30.5, 3.5, 32, 4, true);
    const boundaryMat = new THREE.MeshBasicMaterial({
      color: 0x22d3ee, // cyan
      transparent: true,
      opacity: 0.0,
      side: THREE.DoubleSide,
      wireframe: true
    });
    this.boundaryMesh = new THREE.Mesh(boundaryGeom, boundaryMat);
    this.boundaryMesh.position.set(0, 1.75, 0);
    this.scene.add(this.boundaryMesh);
    this.disposable.push(boundaryGeom, boundaryMat);
  }

  private createGrassTexture(): THREE.CanvasTexture {
    const canvas = document.createElement('canvas');
    canvas.width = 512;
    canvas.height = 512;
    const ctx = canvas.getContext('2d')!;
    const gradient = ctx.createLinearGradient(0, 0, 512, 512);
    gradient.addColorStop(0, '#78ad56');
    gradient.addColorStop(0.42, '#6f9f4b');
    gradient.addColorStop(1, '#4f7f39');
    ctx.fillStyle = gradient;
    ctx.fillRect(0, 0, 512, 512);

    for (let i = 0; i < 90; i++) {
      const x = Math.random() * 512;
      const y = Math.random() * 512;
      const radius = 18 + Math.random() * 56;
      const patch = ctx.createRadialGradient(x, y, 0, x, y, radius);
      patch.addColorStop(0, Math.random() > 0.5 ? 'rgba(42, 96, 40, .18)' : 'rgba(174, 217, 119, .16)');
      patch.addColorStop(1, 'rgba(255,255,255,0)');
      ctx.fillStyle = patch;
      ctx.fillRect(x - radius, y - radius, radius * 2, radius * 2);
    }

    for (let i = 0; i < 3200; i++) {
      const x = Math.random() * 512;
      const y = Math.random() * 512;
      const length = 2 + Math.random() * 7;
      ctx.strokeStyle = Math.random() > 0.48 ? 'rgba(34, 84, 31, .22)' : 'rgba(199, 236, 151, .18)';
      ctx.lineWidth = 0.45 + Math.random() * 0.8;
      ctx.beginPath();
      ctx.moveTo(x, y);
      ctx.lineTo(x + Math.random() * 4 - 2, y - length);
      ctx.stroke();
    }

    const texture = new THREE.CanvasTexture(canvas);
    texture.colorSpace = THREE.SRGBColorSpace;
    texture.wrapS = THREE.RepeatWrapping;
    texture.wrapT = THREE.RepeatWrapping;
    texture.repeat.set(9, 9);
    texture.anisotropy = this.getRuntimeAnisotropy();
    this.disposable.push(texture);
    return texture;
  }

  private getGroundHeightAt(x: number, z: number): number {
    let height = 0;
    const inside = (cx: number, cz: number, sx: number, sz: number) =>
      x >= cx - sx / 2 && x <= cx + sx / 2 && z >= cz - sz / 2 && z <= cz + sz / 2;

    if (inside(0, -8, 9.2, 58)) height = Math.max(height, 0.09);
    if (inside(-5.15, -8, 1.1, 58) || inside(5.15, -8, 1.1, 58)) height = Math.max(height, 0.15);
    if (inside(-18, -18, 16, 18)) height = Math.max(height, 0.08);
    if (inside(18, -4, 24, 13)) height = Math.max(height, 0.08);
    if (inside(-19, -19.6, 10.4, 8.4)) height = Math.max(height, 0.4);

    return height;
  }

  private addBox(size: [number, number, number], position: [number, number, number], color: number, options: { roughness?: number; metalness?: number; emissive?: number; opacity?: number } = {}) {
    const geometry = new THREE.BoxGeometry(...size);
    const material = new THREE.MeshStandardMaterial({
      color,
      roughness: options.roughness ?? 0.72,
      metalness: options.metalness ?? 0.05,
      emissive: options.emissive ?? 0x000000,
      transparent: options.opacity !== undefined,
      opacity: options.opacity ?? 1
    });
    const mesh = new THREE.Mesh(geometry, material);
    mesh.position.set(...position);
    mesh.castShadow = true;
    mesh.receiveShadow = true;
    this.scene!.add(mesh);
    this.disposable.push(geometry, material);
    return mesh;
  }

  private createCampusWalkway() {
    this.addBox([9.2, 0.09, 58], [0, 0.045, -8], 0xa8a29e, { roughness: 0.82 });
    this.addBox([1.1, 0.12, 58], [-5.15, 0.09, -8], 0xd6d3d1);
    this.addBox([1.1, 0.12, 58], [5.15, 0.09, -8], 0xd6d3d1);

    for (let z = 18; z > -36; z -= 3.2) {
      this.addBox([8.8, 0.012, 0.06], [0, 0.105, z], 0x78716c, { opacity: 0.38 });
      this.addBox([0.055, 0.012, 2.55], [-1.6, 0.11, z - 1.45], 0x78716c, { opacity: 0.25 });
      this.addBox([0.055, 0.012, 2.55], [1.6, 0.11, z - 1.45], 0x78716c, { opacity: 0.25 });
    }

    this.addBox([16, 0.08, 18], [-18, 0.04, -18], 0xb8b0a7, { roughness: 0.85 });
    this.addBox([24, 0.08, 13], [18, 0.04, -4], 0x84a66b, { roughness: 0.9 });
  }

  private createMainUtnBuilding() {
    const wall = this.addBox([18, 10.2, 55], [21, 5.1, -8], 0xd7cfba, { roughness: 0.78 });
    wall.castShadow = false;
    this.addBox([18.6, 0.65, 56], [21, 10.55, -8], 0xb9ad97);
    this.addBox([0.42, 9.4, 55.4], [11.35, 4.95, -8], 0x9ca3af, { metalness: 0.2, roughness: 0.45 });

    const glassMaterial = new THREE.MeshStandardMaterial({
      color: 0x8ecae6,
      roughness: 0.18,
      metalness: 0.08,
      transparent: true,
      opacity: 0.44,
      emissive: 0x0e7490,
      emissiveIntensity: 0.06
    });
    this.disposable.push(glassMaterial);

    for (let z = 15; z >= -30; z -= 6) {
      for (let y = 3; y <= 8.3; y += 2.65) {
        const geometry = new THREE.BoxGeometry(0.14, 1.1, 4.3);
        const glass = new THREE.Mesh(geometry, glassMaterial);
        glass.position.set(11.12, y, z);
        glass.castShadow = false;
        this.scene!.add(glass);
        this.disposable.push(geometry);
      }
    }

    for (let z = 17; z >= -32; z -= 4.1) {
      const frame = this.addBox([0.2, 9.8, 0.11], [11.02, 5.2, z], 0x4b5563, { metalness: 0.4, roughness: 0.35 });
      frame.castShadow = false;
    }

    for (let z = 16; z >= -30; z -= 8) {
      this.addBox([0.18, 0.18, 5.6], [10.96, 6.45, z], 0xe5e7eb, { metalness: 0.2, roughness: 0.35 });
    }

    this.addGlassTower(9.5, -25);
    this.addBox([5.2, 3.4, 0.35], [10.7, 1.72, 17.8], 0x1f2937, { metalness: 0.2, roughness: 0.25 });
  }

  private addGlassTower(x: number, z: number) {
    const tower = this.addBox([5.2, 11.8, 7.2], [x, 5.9, z], 0x6ea8c8, { opacity: 0.36, metalness: 0.12, roughness: 0.18 });
    tower.castShadow = false;

    for (let y = 1.8; y < 11.2; y += 2) {
      this.addBox([5.4, 0.1, 7.4], [x, y, z], 0x475569, { metalness: 0.32, roughness: 0.4 });
    }

    for (let offset = -2.3; offset <= 2.3; offset += 1.15) {
      this.addBox([0.08, 11.5, 7.6], [x + offset, 5.75, z], 0x334155, { metalness: 0.38, roughness: 0.38 });
      this.addBox([5.6, 11.5, 0.08], [x, 5.75, z + offset], 0x334155, { metalness: 0.38, roughness: 0.38 });
    }
  }

  private createConeRoofBuildingAndKiosk() {
    // Estructura hueca del kiosco (Pared trasera, pared izquierda, pared derecha y cielorraso)
    this.addBox([9.8, 3.8, 0.22], [-19, 1.9, -23.4], 0xd8c7a8, { roughness: 0.82 });
    this.addBox([0.22, 3.8, 7.8], [-23.8, 1.9, -19.6], 0xd8c7a8, { roughness: 0.82 });
    this.addBox([0.22, 3.8, 7.8], [-14.2, 1.9, -19.6], 0xd8c7a8, { roughness: 0.82 });
    this.addBox([9.8, 0.22, 7.8], [-19, 3.8, -19.6], 0xd8c7a8, { roughness: 0.82 });

    // Puerta lateral decorativa (no interactuable) en la pared derecha del kiosco
    // Marco de la puerta (marrÃ³n oscuro)
    this.addBox([0.15, 2.7, 1.3], [-14.1, 1.35, -19.6], 0x3e2723, { roughness: 0.85 });
    // Panel de la puerta (madera marrÃ³n mediana, ligeramente sobresaliente para volumen)
    this.addBox([0.12, 2.58, 1.14], [-14.08, 1.29, -19.6], 0x5a3822, { roughness: 0.75 });
    // Picaporte/Manija metÃ¡lica (pequeÃ±o cilindro dorado o cromado)
    const handleGeom = new THREE.CylinderGeometry(0.02, 0.02, 0.16, 8);
    handleGeom.rotateX(Math.PI / 2);
    const handleMat = new THREE.MeshStandardMaterial({ color: 0xd1d5db, metalness: 0.9, roughness: 0.1 });
    const handle = new THREE.Mesh(handleGeom, handleMat);
    handle.position.set(-14.01, 1.29, -19.1); // En el borde de la puerta, a la altura de la mano
    this.scene!.add(handle);
    this.disposable.push(handleGeom, handleMat);

    this.addBox([10.4, 0.4, 8.4], [-19, 0.2, -19.6], 0xc8b89a, { roughness: 0.86 });
    const roofGeometry = new THREE.ConeGeometry(7, 2.15, 4);
    const roofMaterial = new THREE.MeshStandardMaterial({ color: 0x526f54, roughness: 0.58, metalness: 0.05 });
    const roof = new THREE.Mesh(roofGeometry, roofMaterial);
    roof.position.set(-19, 4.95, -19.6);
    roof.rotation.y = Math.PI / 4;
    roof.castShadow = true;
    this.scene!.add(roof);
    this.disposable.push(roofGeometry, roofMaterial);

    this.addBox([7.2, 2.15, 0.22], [-19, 2.2, -15.58], 0x111827, { opacity: 0.42, metalness: 0.1 });
    this.addBox([7.7, 0.22, 0.28], [-19, 3.38, -15.42], 0xd8c7a8);
    this.addBox([7.7, 0.22, 0.28], [-19, 1.05, -15.42], 0xd8c7a8);
    this.addBox([0.25, 2.35, 0.28], [-23, 2.2, -15.42], 0xd8c7a8);
    this.addBox([0.25, 2.35, 0.28], [-15, 2.2, -15.42], 0xd8c7a8);

    // Mostrador al frente
    this.addBox([8.9, 0.32, 0.95], [-19, 1.05, -15.2], 0x8b5e34, { roughness: 0.62 });

    // Pared base del mostrador (cierra la parte de abajo para que no sea hueca)
    this.addBox([8.9, 0.9, 0.22], [-19, 0.45, -15.42], 0xd8c7a8, { roughness: 0.82 });
    
    // Paneles traseros de soporte de madera oscura (verticales, marrones, pegados contra el mostrador)
    const leftBacking = this.addBox([2.4, 1.8, 0.08], [-21.6, 1.82, -15.65], 0x3e2723, { roughness: 0.85 });
    leftBacking.rotation.y = 0;

    const rightBacking = this.addBox([2.4, 1.8, 0.08], [-16.4, 1.82, -15.65], 0x3e2723, { roughness: 0.85 });
    rightBacking.rotation.y = 0;

    // EstanterÃ­as rectas a la izquierda y derecha de Hilda (2.4 de ancho, paralelas al fondo, pegadas contra el mostrador)
    const shelfHeights = [1.12, 1.82, 2.52];
    shelfHeights.forEach((y) => {
      // EstanterÃ­a izquierda (recta, inclinada hacia abajo de 0.08 rad, z = -15.45)
      const leftShelf = this.addBox([2.4, 0.14, 0.55], [-21.6, y, -15.45], 0x5a3822);
      leftShelf.rotation.set(0.08, 0, 0, 'YXZ');

      // EstanterÃ­a derecha (recta, inclinada hacia abajo de 0.08 rad, z = -15.45)
      const rightShelf = this.addBox([2.4, 0.14, 0.55], [-16.4, y, -15.45], 0x5a3822);
      rightShelf.rotation.set(0.08, 0, 0, 'YXZ');
    });

    // Tubo de luz fluorescente fÃ­sico en el cielorraso del kiosco
    const lampGeom = new THREE.CylinderGeometry(0.05, 0.05, 2.8, 8);
    lampGeom.rotateZ(Math.PI / 2);
    const lampMat = new THREE.MeshBasicMaterial({ color: 0xffffff });
    const lamp = new THREE.Mesh(lampGeom, lampMat);
    lamp.position.set(-19.0, 3.7, -19.0);
    this.scene!.add(lamp);
    this.disposable.push(lampGeom, lampMat);

    // Fuente de luz real del techo (cÃ¡lida, ilumina todo el interior)
    const ceilingLight = new THREE.PointLight(0xfff2cc, 4.8, 15);
    ceilingLight.position.set(-19.0, 3.5, -19.0);
    ceilingLight.castShadow = false;
    this.scene!.add(ceilingLight);

    this.addBox([9.8, 0.28, 1.2], [-19, 3.45, -14.9], 0x9b2f27, { roughness: 0.58 });

    this.addCantinaSign();

    this.addKioskProducts();
    this.addVendor();
  }

  private addKioskProducts() {
    this.createKioskProductPlaceholders();

    // Los GLB reales de productos se cargan bajo demanda al acercarse al kiosco.

    // MÃ¡quinas expendedoras al fondo (Coca-Cola al centro, PS1 al rincÃ³n izquierdo)
    [
      { path: '/models-optimized/kiosk/vending_machine_-_ps1_low_poly.glb', x: -21.4, z: -21.4, height: 2.25 },
      { path: '/models-optimized/kiosk/vending_machine_coca_cola.glb', x: -19.0, z: -21.4, height: 2.25 }
    ].forEach((vm) => {
      this.loadSceneAsset(vm.path, (model) => {
        this.fitModelToHeight(model, vm.height);
        this.centerModelPivot(model);
        
        model.position.set(vm.x, 0.4, vm.z);
        model.rotation.y = 0; // facing front! (No Math.PI!)
        this.alignModelBottom(model, 0.4);
        this.scene!.add(model);
      }, { castShadow: false });
    });

    // Luz focalizada sobre la mÃ¡quina expendedora de Coca-Cola en el centro del fondo
    const vmLight = new THREE.PointLight(0xff3333, 4.2, 8);
    vmLight.position.set(-19.0, 2.3, -20.6);
    this.scene!.add(vmLight);
  }

  private createKioskProductPlaceholders() {
    if (!this.scene || this.kioskProductPlaceholders) return;

    const group = new THREE.Group();
    const canGeometry = new THREE.CylinderGeometry(0.045, 0.045, 0.22, 8);
    const boxGeometry = new THREE.BoxGeometry(0.11, 0.18, 0.055);
    const bagGeometry = new THREE.BoxGeometry(0.15, 0.2, 0.035);
    const materials = [
      new THREE.MeshStandardMaterial({ color: 0xff3344, roughness: 0.55 }),
      new THREE.MeshStandardMaterial({ color: 0x22d3ee, roughness: 0.55 }),
      new THREE.MeshStandardMaterial({ color: 0xfacc15, roughness: 0.6 }),
      new THREE.MeshStandardMaterial({ color: 0x111827, roughness: 0.45 }),
      new THREE.MeshStandardMaterial({ color: 0x16a34a, roughness: 0.55 })
    ];
    this.disposable.push(canGeometry, boxGeometry, bagGeometry, ...materials);

    const shelfHeights = [1.12, 1.82, 2.52];
    shelfHeights.forEach((y, row) => {
      [-21.6, -16.4].forEach((shelfX, shelfIndex) => {
        for (let col = 0; col < 7; col++) {
          const type = (row + col + shelfIndex) % 3;
          const geometry = type === 0 ? canGeometry : type === 1 ? boxGeometry : bagGeometry;
          const material = materials[(row * 3 + col + shelfIndex) % materials.length];
          const item = new THREE.Mesh(geometry, material);
          item.position.set(shelfX - 0.9 + col * 0.3, y + 0.21, -15.45);
          item.rotation.set(0.08, 0.12 * ((col % 3) - 1), type === 2 ? 0.16 : 0, 'YXZ');
          item.castShadow = false;
          item.receiveShadow = false;
          group.add(item);
        }
      });
    });

    this.kioskProductPlaceholders = group;
    this.scene.add(group);
  }

  private ensureDetailedKioskProductsLoaded() {
    if (this.kioskProductsDetailedLoaded || !this.scene) return;
    this.kioskProductsDetailedLoaded = true;

    const products = [
      { path: '/models-optimized/kiosk/coca_cola_bottle.glb', maxDim: 0.30 },
      { path: '/models-optimized/kiosk/lays_classic__hd_textures__free_download.glb', maxDim: 0.35 },
      { path: '/models-optimized/kiosk/cheetos_rasa_keju.glb', maxDim: 0.32 },
      { path: '/models-optimized/kiosk/black_monster_energy_drink.glb', maxDim: 0.20 },
      { path: '/models-optimized/kiosk/monster_energy_drink_mango.glb', maxDim: 0.20 },
      { path: '/models-optimized/kiosk/monster_zero_ultra.glb', maxDim: 0.20 },
      { path: '/models-optimized/kiosk/pringles.glb', maxDim: 0.28 },
      { path: '/models-optimized/kiosk/oreo.glb', maxDim: 0.28 },
      { path: '/models-optimized/kiosk/kitkat_chunky_salted_caramel.glb', maxDim: 0.20 },
      { path: '/models-optimized/kiosk/chocolate_milka.glb', maxDim: 0.22 },
      { path: '/models-optimized/kiosk/red_dorito.glb', maxDim: 0.30 },
      { path: '/models-optimized/kiosk/fanta_can.glb', maxDim: 0.19 },
      { path: '/models-optimized/kiosk/fernet_botella.glb', maxDim: 0.34 },
      { path: '/models-optimized/kiosk/malboro.glb', maxDim: 0.18 },
      { path: '/models-optimized/kiosk/mate_argentino.glb', maxDim: 0.26 },
      { path: '/models-optimized/kiosk/pepsi.glb', maxDim: 0.19 }
    ];

    const shelfHeights = [1.12, 1.82, 2.52];

    shelfHeights.forEach((y, row) => {
      [-21.6, -16.4].forEach((shelfX, shelfIndex) => {
        for (let col = 0; col < 7; col++) {
          const productOffset = shelfIndex === 0 ? 0 : 2;
          const item = products[(row * 3 + col + productOffset) % products.length];
          this.loadSceneAsset(item.path, (loadedModel) => {
            const model = loadedModel.clone();
            this.fitModelToMaxDimension(model, item.maxDim);
            this.centerModelPivot(model);

            model.position.set(shelfX - 0.9 + col * 0.3, y + 0.08, -15.45);
            if (item.path.includes('kitkat') || item.path.includes('milka') || item.path.includes('malboro')) {
              model.rotation.set(0.08, Math.PI / 3, 1.3, 'YXZ');
            } else if (item.path.includes('oreo') || item.path.includes('mate')) {
              model.rotation.set(0.08, Math.PI + 0.2, 0.4, 'YXZ');
            } else if (item.path.includes('dorito')) {
              model.rotation.set(0.05, -0.15, 0.14, 'YXZ');
            } else if (item.path.includes('fernet')) {
              model.rotation.set(0, Math.PI, 0, 'YXZ');
            } else if (
              item.path.includes('coca') ||
              item.path.includes('fanta') ||
              item.path.includes('pepsi') ||
              item.path.includes('monster')
            ) {
              model.rotation.set(0.02, Math.PI, 0, 'YXZ');
            } else {
              model.rotation.set(0.08, 0, 0, 'YXZ');
            }
            this.alignModelBottom(model, y + 0.08);
            this.scene!.add(model);
          }, { castShadow: false, receiveShadow: false });
        }
      });
    });

    setTimeout(() => {
      if (this.kioskProductPlaceholders) {
        this.kioskProductPlaceholders.visible = false;
      }
    }, 1200);

    this.loadSceneAsset('/models-optimized/kiosk/mate_argentino.glb', (loadedModel) => {
      const model = loadedModel.clone();
      this.fitModelToMaxDimension(model, 0.30);
      this.centerModelPivot(model);
      model.position.set(-19.78, 1.24, -15.62);
      model.rotation.set(0.05, Math.PI + 0.38, 0.02, 'YXZ');
      this.alignModelBottom(model, 1.24);
      this.scene!.add(model);
    }, { castShadow: false, receiveShadow: false });
  }

  private fitModelToHeight(model: THREE.Object3D, targetHeight: number) {
    const box = new THREE.Box3().setFromObject(model);
    const size = new THREE.Vector3();
    box.getSize(size);
    const height = size.y || 1;
    const scale = targetHeight / height;
    model.scale.multiplyScalar(scale);
  }

  private fitModelToMaxDimension(model: THREE.Object3D, targetMaxDim: number) {
    const box = new THREE.Box3().setFromObject(model);
    const size = new THREE.Vector3();
    box.getSize(size);
    const maxDim = Math.max(size.x, size.y, size.z) || 1;
    const scale = targetMaxDim / maxDim;
    model.scale.multiplyScalar(scale);
  }

  private alignModelBottom(model: THREE.Object3D, targetY: number) {
    const box = new THREE.Box3().setFromObject(model);
    model.position.y += targetY - box.min.y;
  }

  private centerModelPivot(model: THREE.Object3D) {
    const box = new THREE.Box3().setFromObject(model);
    const center = new THREE.Vector3();
    box.getCenter(center);
    
    model.children.forEach((child) => {
      child.position.x -= center.x;
      child.position.z -= center.z;
    });
  }

  private addCantinaSign() {
    if (!this.scene) return;

    // Crear un lienzo canvas 2D para pintar las letras y el decorado del cartel
    const canvas = document.createElement('canvas');
    canvas.width = 1024;
    canvas.height = 256;
    const ctx = canvas.getContext('2d')!;

    // 1. Fondo degradado vertical elegante (pizarra/azul acero oscuro)
    const bgGrad = ctx.createLinearGradient(0, 0, 0, 256);
    bgGrad.addColorStop(0, '#0f172a');
    bgGrad.addColorStop(0.5, '#1e293b');
    bgGrad.addColorStop(1, '#0f172a');
    ctx.fillStyle = bgGrad;
    ctx.fillRect(0, 0, 1024, 256);

    // 2. Bordes dorados dobles con grosores diferenciados
    ctx.strokeStyle = '#ffd700';
    ctx.lineWidth = 10;
    ctx.strokeRect(14, 14, 996, 228);
    ctx.strokeStyle = '#d4af37';
    ctx.lineWidth = 4;
    ctx.strokeRect(24, 24, 976, 208);

    // 3. Pequeños rombos dorados decorativos en las 4 esquinas internas
    ctx.fillStyle = '#ffd700';
    const drawDiamond = (cx: number, cy: number, r: number) => {
      ctx.beginPath();
      ctx.moveTo(cx, cy - r);
      ctx.lineTo(cx + r, cy);
      ctx.lineTo(cx, cy + r);
      ctx.lineTo(cx - r, cy);
      ctx.closePath();
      ctx.fill();
    };
    drawDiamond(44, 44, 10);
    drawDiamond(980, 44, 10);
    drawDiamond(44, 212, 10);
    drawDiamond(980, 212, 10);

    // 4. Subtítulo superior: "CANTINA"
    ctx.font = 'bold 32px "Arial Black", "Montserrat", sans-serif';
    ctx.fillStyle = '#f8fafc';
    ctx.textAlign = 'center';
    ctx.shadowColor = 'rgba(0, 0, 0, 0.6)';
    ctx.shadowBlur = 4;
    ctx['letterSpacing'] = '14px'; // Evita errores de tipo en TS antiguo
    ctx.fillText('CANTINA', 512, 75);

    // 5. Título principal: "El Loco Amaya" en tipografía cursiva serif y degradado de oro puro
    const textGrad = ctx.createLinearGradient(0, 90, 0, 210);
    textGrad.addColorStop(0, '#fffbeb');
    textGrad.addColorStop(0.3, '#fde047');
    textGrad.addColorStop(0.7, '#eab308');
    textGrad.addColorStop(1, '#a16207');

    ctx.font = 'italic bold 90px "Georgia", "Times New Roman", serif';
    ctx.fillStyle = textGrad;
    ctx.shadowColor = 'rgba(0, 0, 0, 0.8)';
    ctx.shadowBlur = 12;
    ctx.shadowOffsetX = 4;
    ctx.shadowOffsetY = 6;
    ctx['letterSpacing'] = '0px';
    ctx.fillText('“El Loco Amaya”', 512, 185);

    // Instanciar textura a partir del canvas
    const signTexture = new THREE.CanvasTexture(canvas);
    signTexture.colorSpace = THREE.SRGBColorSpace;
    signTexture.minFilter = THREE.LinearMipmapLinearFilter;
    signTexture.magFilter = THREE.LinearFilter;
    this.disposable.push(signTexture);

    // Crear grupo contenedor 3D posicionado por encima de la viga roja
    const signGroup = new THREE.Group();
    signGroup.name = 'CantinaSignGroup';
    signGroup.position.set(-19.0, 4.25, -14.28); // 2cm adelante de la viga roja para evitar Z-Fighting

    // 1. Marco de soporte de madera muy oscura
    const frameGeometry = new THREE.BoxGeometry(4.8, 1.15, 0.08);
    const frameMaterial = new THREE.MeshStandardMaterial({ color: 0x18120d, roughness: 0.88 });
    const frameMesh = new THREE.Mesh(frameGeometry, frameMaterial);
    frameMesh.castShadow = true;
    signGroup.add(frameMesh);
    this.disposable.push(frameGeometry, frameMaterial);

    // 2. Placa central
    const plateGeometry = new THREE.PlaneGeometry(4.68, 1.03);
    const plateMaterial = new THREE.MeshStandardMaterial({
      map: signTexture,
      roughness: 0.20,
      metalness: 0.15,
      side: THREE.FrontSide
    });
    const plateMesh = new THREE.Mesh(plateGeometry, plateMaterial);
    plateMesh.position.set(0, 0, 0.045);
    signGroup.add(plateMesh);
    this.disposable.push(plateGeometry, plateMaterial);

    // 3. Dos soportes cilíndricos negros simulando anclaje metálico a la viga inferior
    const supportMat = new THREE.MeshStandardMaterial({ color: 0x18181b, roughness: 0.5, metalness: 0.8 });
    const leftSupportGeom = new THREE.CylinderGeometry(0.03, 0.03, 0.35, 8);
    const leftSupport = new THREE.Mesh(leftSupportGeom, supportMat);
    leftSupport.position.set(-1.8, -0.65, 0.0);
    signGroup.add(leftSupport);
    this.disposable.push(leftSupportGeom);

    const rightSupportGeom = new THREE.CylinderGeometry(0.03, 0.03, 0.35, 8);
    const rightSupport = new THREE.Mesh(rightSupportGeom, supportMat);
    rightSupport.position.set(1.8, -0.65, 0.0);
    signGroup.add(rightSupport);
    this.disposable.push(rightSupportGeom, supportMat);

    this.scene.add(signGroup);
  }

  private addVendor() {
    const root = new THREE.Group();
    root.position.set(-19.6, 0.4, -17.2);
    root.rotation.y = 0;
    this.scene!.add(root);
    const npc: CampusNpc = { root, actions: new Map() };
    this.kioskVendor = npc;

    this.loadSceneAsset('/models-optimized/characters/hilda_regular_00.glb', (model, animations) => {
      model.scale.setScalar(1.22);
      model.position.set(0, -0.12, 0);
      root.add(model);
      npc.mixer = new THREE.AnimationMixer(model);
      animations.forEach((clip) => npc.actions.set(clip.name.toLowerCase(), npc.mixer!.clipAction(clip)));
      
      // Manejar la finalizaciÃ³n de pose_03 de reproducciÃ³n Ãºnica
      npc.mixer.addEventListener('finished', (e) => {
        const poseEntry = Array.from(npc.actions.entries()).find(([name]) => name.includes('pose_03'));
        if (poseEntry && e.action === poseEntry[1]) {
          poseEntry[1].stop();
          npc.active = undefined;
          this.setNpcAnimation(npc, ['speak_1', 'mouth_01', 'idle'], 0.12);
        }
      });

      this.setNpcAnimation(npc, ['idle'], 0);
    });
  }

  private createCampusTrees() {
    const treePositions = [[-7, -8], [-8, 7], [6.8, 13], [29, 10], [28, -19], [-30, -2], [-31, -24]];
    
    this.loadSceneAsset('/models-optimized/environment/pine_tree.glb', (loadedModel) => {
      treePositions.forEach(([x, z]) => {
        const model = loadedModel.clone();
        
        // Altura aleatoria para lograr variedad de tamaÃ±os (unos mÃ¡s grandes, otros mÃ¡s chicos)
        const targetHeight = 4.2 + Math.random() * 3.6;
        this.fitModelToHeight(model, targetHeight);
        this.centerModelPivot(model);
        
        model.position.set(x, 0, z);
        this.alignModelBottom(model, 0); // Poner al ras del suelo
        
        // RotaciÃ³n aleatoria en Y para que luzca sÃºper natural
        model.rotation.y = Math.random() * Math.PI * 2;
        
        this.scene!.add(model);
      }, { castShadow: false });
    });
  }

  private createMountains() {
    const loader = new THREE.TextureLoader();
    loader.load('/images/background_mountains.png', (texture) => {
      const image = texture.image;
      const canvas = document.createElement('canvas');
      canvas.width = image.width;
      canvas.height = image.height;
      const ctx = canvas.getContext('2d')!;
      ctx.drawImage(image, 0, 0);

      const imgData = ctx.getImageData(0, 0, canvas.width, canvas.height);
      const data = imgData.data;
      for (let i = 0; i < data.length; i += 4) {
        const r = data[i];
        const g = data[i + 1];
        const b = data[i + 2];
        // Convertir el fondo negro en transparente para el stenciling
        if (r < 15 && g < 15 && b < 15) {
          data[i + 3] = 0;
        }
      }
      ctx.putImageData(imgData, 0, 0);

      const alphaTexture = new THREE.CanvasTexture(canvas);
      alphaTexture.colorSpace = THREE.SRGBColorSpace;
      alphaTexture.wrapS = THREE.RepeatWrapping;
      alphaTexture.repeat.x = 2.0; // Repetir horizontalmente para cubrir todo el fondo

      this.disposable.push(alphaTexture);

      // Inicializar y registrar dinámicamente los materiales para teñirlos según el ciclo día/noche
      this.mountainMaterials = [];
      const registerMountainMaterial = (colorHex: number) => {
        const mat = new THREE.MeshBasicMaterial({
          map: alphaTexture,
          transparent: true,
          color: colorHex,
          side: THREE.DoubleSide,
          depthWrite: false
        });
        mat.userData['baseColor'] = mat.color.clone();
        this.mountainMaterials.push(mat);
        return mat;
      };

      // --- CAPA TRASERA DE MONTAÑAS (Existentes, frente al kiosco) ---
      const materialBack = registerMountainMaterial(0x142030);
      const geometryBack = new THREE.PlaneGeometry(160, 32);
      const mountainsBack = new THREE.Mesh(geometryBack, materialBack);
      mountainsBack.position.set(0, 11.5, -78);
      mountainsBack.renderOrder = -10; // Dibujarse antes que las mallas del escenario (renderOrder 0)
      this.scene!.add(mountainsBack);
      this.disposable.push(geometryBack, materialBack);

      const materialFront = registerMountainMaterial(0x22354c);
      const geometryFront = new THREE.PlaneGeometry(160, 28);
      const mountainsFront = new THREE.Mesh(geometryFront, materialFront);
      mountainsFront.position.set(18, 9.5, -68);
      mountainsFront.renderOrder = -10;
      this.scene!.add(mountainsFront);
      this.disposable.push(geometryFront, materialFront);

      // --- CAPAS LATERALES DE MONTAÑAS (Para rodear el escenario) ---
      const materialLeftBack = registerMountainMaterial(0x121e2e);
      const geometryLeftBack = new THREE.PlaneGeometry(160, 38);
      const mountainsLeftBack = new THREE.Mesh(geometryLeftBack, materialLeftBack);
      mountainsLeftBack.position.set(-78, 16.0, 0);
      mountainsLeftBack.rotation.y = Math.PI / 2; // Orientada hacia el centro
      mountainsLeftBack.renderOrder = -10;
      this.scene!.add(mountainsLeftBack);
      this.disposable.push(geometryLeftBack, materialLeftBack);

      const materialLeftFront = registerMountainMaterial(0x1e3046);
      const geometryLeftFront = new THREE.PlaneGeometry(160, 34);
      const mountainsLeftFront = new THREE.Mesh(geometryLeftFront, materialLeftFront);
      mountainsLeftFront.position.set(-68, 14.0, 10);
      mountainsLeftFront.rotation.y = Math.PI / 2;
      mountainsLeftFront.renderOrder = -10;
      this.scene!.add(mountainsLeftFront);
      this.disposable.push(geometryLeftFront, materialLeftFront);

      const materialRightBack = registerMountainMaterial(0x121e2e);
      const geometryRightBack = new THREE.PlaneGeometry(160, 24);
      const mountainsRightBack = new THREE.Mesh(geometryRightBack, materialRightBack);
      mountainsRightBack.position.set(78, 9.0, 0);
      mountainsRightBack.rotation.y = -Math.PI / 2; // Orientada hacia el centro
      mountainsRightBack.renderOrder = -10;
      this.scene!.add(mountainsRightBack);
      this.disposable.push(geometryRightBack, materialRightBack);

      const materialRightFront = registerMountainMaterial(0x1e3046);
      const geometryRightFront = new THREE.PlaneGeometry(160, 20);
      const mountainsRightFront = new THREE.Mesh(geometryRightFront, materialRightFront);
      mountainsRightFront.position.set(68, 7.0, -10);
      mountainsRightFront.rotation.y = -Math.PI / 2;
      mountainsRightFront.renderOrder = -10;
      this.scene!.add(mountainsRightFront);
      this.disposable.push(geometryRightFront, materialRightFront);

      // --- CAPAS TRASERAS/REAR DE MONTAÑAS (En la parte de atrás del mapa, Z = 78) ---
      const materialRearBack = registerMountainMaterial(0x142030);
      const geometryRearBack = new THREE.PlaneGeometry(160, 26);
      const mountainsRearBack = new THREE.Mesh(geometryRearBack, materialRearBack);
      mountainsRearBack.position.set(0, 10.0, 78);
      mountainsRearBack.rotation.y = Math.PI; // Orientada hacia el centro
      mountainsRearBack.renderOrder = -10;
      this.scene!.add(mountainsRearBack);
      this.disposable.push(geometryRearBack, materialRearBack);

      const materialRearFront = registerMountainMaterial(0x22354c);
      const geometryRearFront = new THREE.PlaneGeometry(160, 22);
      const mountainsRearFront = new THREE.Mesh(geometryRearFront, materialRearFront);
      mountainsRearFront.position.set(-15, 8.0, 68);
      mountainsRearFront.rotation.y = Math.PI;
      mountainsRearFront.renderOrder = -10;
      this.scene!.add(mountainsRearFront);
      this.disposable.push(geometryRearFront, materialRearFront);
    });
  }

  private createCampusBackgroundDetails() {
    this.addBox([28, 6.5, 12], [-17, 3.25, -47], 0xc9bea8, { roughness: 0.82 });
    this.addBox([28.4, 0.5, 12.4], [-17, 6.75, -47], 0xa99f8f);
    for (let x = -28; x <= -5; x += 4) {
      this.addBox([2.3, 0.8, 0.16], [x, 4.5, -40.8], 0x93a7b7, { opacity: 0.46, metalness: 0.08 });
    }

    // Cargar la antena parabÃ³lica 3D real provista por el usuario y colocarla en el pasto
    this.loadSceneAsset('/models-optimized/environment/parabolic_antenna.glb', (model) => {
      this.fitModelToHeight(model, 3.2);
      this.centerModelPivot(model);
      
      // Ubicar en el pasto a la izquierda/fondo
      model.position.set(-27.5, 0, -18.0);
      this.alignModelBottom(model, 0); // Al ras del pasto
      model.rotation.set(0.2, 0.65, 0); // Apuntando al cielo
      
      this.scene!.add(model);
    }, { castShadow: false });

    for (let z = 16; z > -35; z -= 9) {
      this.addLampPost(-6.2, z);
      this.addLampPost(6.2, z - 4);
    }

    // Colocar faroles estratÃ©gicos de alta fidelidad 3D en zonas clave
    this.addStrategicLantern(-11.0, -13.5, 0);       // Cerca del mostrador del kiosco (Hilda)
    this.addStrategicLantern(5.0, -2.0, Math.PI);      // Cerca del sendero de la plaza UTN
  }

  private createUtnFrcCampusAdditions() {
    // 1. BENCHES AND TRASH CANS
    const benchPositions = [
      { x: -3.5, z: 10, rot: Math.PI / 2 },
      { x: -3.5, z: -10, rot: Math.PI / 2 },
      { x: 3.5, z: 5, rot: -Math.PI / 2 },
      { x: 3.5, z: -15, rot: -Math.PI / 2 }
    ];

    benchPositions.forEach((pos) => {
      this.addProceduralBench(pos.x, pos.z, pos.rot);
    });

    const trashPositions = [
      { x: -3.6, z: 8 },
      { x: 3.6, z: -12 }
    ];
    trashPositions.forEach((pos) => {
      this.addProceduralTrashCan(pos.x, pos.z);
    });

    // 2. ACADEMIC PAVILION B (Out of bounds)
    this.addBox([25, 8.5, 12], [-38, 4.25, 5], 0xa8a29e, { roughness: 0.85 }); // Concrete block
    this.addBox([25.4, 0.6, 12.4], [-38, 8.8, 5], 0x7c2d12); // Terracotta roof line

    const glassMaterial = new THREE.MeshStandardMaterial({
      color: 0x1e293b,
      roughness: 0.2,
      metalness: 0.8
    });
    this.disposable.push(glassMaterial);

    for (let z = -4; z <= 14; z += 4.5) {
      for (let y = 2.2; y <= 6.5; y += 3.8) {
        const windowGeom = new THREE.BoxGeometry(0.12, 1.6, 2.2);
        const win = new THREE.Mesh(windowGeom, glassMaterial);
        win.position.set(-31.9, y, z);
        this.scene!.add(win);
        this.disposable.push(windowGeom);
      }
    }

    // 3. SPORTS COURT (Football field, Out of bounds)
    this.addBox([22, 0.02, 14], [38, 0.01, 20], 0x3f6212, { roughness: 0.95 }); // Grass court
    this.addBox([22.2, 0.022, 0.12], [38, 0.02, 27], 0xffffff); // Top line
    this.addBox([22.2, 0.022, 0.12], [38, 0.02, 13], 0xffffff); // Bottom line
    this.addBox([0.12, 0.022, 14.2], [27, 0.02, 20], 0xffffff); // Left line
    this.addBox([0.12, 0.022, 14.2], [49, 0.02, 20], 0xffffff); // Right line
    this.addBox([0.12, 0.022, 14.2], [38, 0.02, 20], 0xffffff); // Center line

    this.addGoalPost(27, 20, Math.PI / 2);
    this.addGoalPost(49, 20, -Math.PI / 2);

    // 4. PARKING LOT AND PROCEDURAL CARS
    this.addBox([15, 0.02, 10], [-25, 0.01, 36], 0x334155, { roughness: 0.88 });
    for (let x = -31; x <= -19; x += 3.5) {
      this.addBox([0.1, 0.022, 8.5], [x, 0.02, 36], 0xeab308);
    }
    this.addProceduralCar(-29.5, 36, 0xef4444);
    this.addProceduralCar(-26.0, 36, 0x3b82f6);
    this.addProceduralCar(-22.5, 36, 0x94a3b8);

    // 5. ENTRANCE ARCHWAY / SIGN
    this.addBox([0.4, 4.2, 0.4], [-4.2, 2.1, 22], 0x1e293b, { roughness: 0.6 });
    this.addBox([0.4, 4.2, 0.4], [4.2, 2.1, 22], 0x1e293b, { roughness: 0.6 });
    this.addBox([8.8, 0.6, 0.4], [0, 4.2, 22], 0x1e3a8a, { roughness: 0.5 });

    const signGeom = new THREE.PlaneGeometry(5.2, 0.45);
    const signMat = new THREE.MeshStandardMaterial({
      color: 0xffffff,
      emissive: 0x38bdf8,
      emissiveIntensity: 1.2,
      roughness: 0.1
    });
    const signMesh = new THREE.Mesh(signGeom, signMat);
    signMesh.position.set(0, 4.2, 21.78);
    this.scene!.add(signMesh);
    this.disposable.push(signGeom, signMat);
  }

  private addGoalPost(x: number, z: number, rotationY: number) {
    const postMat = new THREE.MeshStandardMaterial({ color: 0xffffff, roughness: 0.3 });
    const postGroup = new THREE.Group();
    postGroup.position.set(x, 0, z);
    postGroup.rotation.y = rotationY;

    const barLeft = new THREE.Mesh(new THREE.CylinderGeometry(0.04, 0.04, 1.6), postMat);
    barLeft.position.set(0, 0.8, -1.4);
    barLeft.castShadow = true;
    postGroup.add(barLeft);

    const barRight = new THREE.Mesh(new THREE.CylinderGeometry(0.04, 0.04, 1.6), postMat);
    barRight.position.set(0, 0.8, 1.4);
    barRight.castShadow = true;
    postGroup.add(barRight);

    const crossbar = new THREE.Mesh(new THREE.CylinderGeometry(0.04, 0.04, 2.8), postMat);
    crossbar.rotation.x = Math.PI / 2;
    crossbar.position.set(0, 1.6, 0);
    crossbar.castShadow = true;
    postGroup.add(crossbar);

    this.scene!.add(postGroup);
    this.disposable.push(postMat);
  }

  private addProceduralCar(x: number, z: number, color: number) {
    const carGroup = new THREE.Group();
    carGroup.position.set(x, 0, z);

    const bodyMat = new THREE.MeshStandardMaterial({ color: color, roughness: 0.2, metalness: 0.5 });
    const cabinMat = new THREE.MeshStandardMaterial({ color: 0x0f172a, roughness: 0.1, metalness: 0.9 });
    const wheelMat = new THREE.MeshStandardMaterial({ color: 0x111827, roughness: 0.9 });

    const body = new THREE.Mesh(new THREE.BoxGeometry(1.6, 0.48, 0.95), bodyMat);
    body.position.set(0, 0.34, 0);
    body.castShadow = true;
    carGroup.add(body);

    const cabin = new THREE.Mesh(new THREE.BoxGeometry(0.95, 0.34, 0.82), cabinMat);
    cabin.position.set(-0.1, 0.75, 0);
    cabin.castShadow = true;
    carGroup.add(cabin);

    const wheelGeom = new THREE.CylinderGeometry(0.18, 0.18, 0.15, 12);
    wheelGeom.rotateX(Math.PI / 2);

    const wheelFL = new THREE.Mesh(wheelGeom, wheelMat);
    wheelFL.position.set(0.5, 0.18, 0.42);
    carGroup.add(wheelFL);

    const wheelFR = new THREE.Mesh(wheelGeom, wheelMat);
    wheelFR.position.set(0.5, 0.18, -0.42);
    carGroup.add(wheelFR);

    const wheelBL = new THREE.Mesh(wheelGeom, wheelMat);
    wheelBL.position.set(-0.5, 0.18, 0.42);
    carGroup.add(wheelBL);

    const wheelBR = new THREE.Mesh(wheelGeom, wheelMat);
    wheelBR.position.set(-0.5, 0.18, -0.42);
    carGroup.add(wheelBR);

    this.scene!.add(carGroup);
    this.disposable.push(bodyMat, cabinMat, wheelMat, wheelGeom);
  }

  private addProceduralBench(x: number, z: number, rotationY: number) {
    const seatMat = new THREE.MeshStandardMaterial({ color: 0x78350f, roughness: 0.6 });
    const metalMat = new THREE.MeshStandardMaterial({ color: 0x334155, metalness: 0.7, roughness: 0.4 });

    const bench = new THREE.Group();
    bench.position.set(x, 0, z);
    bench.rotation.y = rotationY;

    const seat = new THREE.Mesh(new THREE.BoxGeometry(1.5, 0.08, 0.48), seatMat);
    seat.position.set(0, 0.45, 0);
    seat.castShadow = true;
    bench.add(seat);

    const back = new THREE.Mesh(new THREE.BoxGeometry(1.5, 0.32, 0.08), seatMat);
    back.position.set(0, 0.82, -0.2);
    back.rotation.x = -0.15;
    back.castShadow = true;
    bench.add(back);

    const legLeft = new THREE.Mesh(new THREE.BoxGeometry(0.08, 0.8, 0.48), metalMat);
    legLeft.position.set(-0.65, 0.4, 0);
    legLeft.castShadow = true;
    bench.add(legLeft);

    const legRight = new THREE.Mesh(new THREE.BoxGeometry(0.08, 0.8, 0.48), metalMat);
    legRight.position.set(0.65, 0.4, 0);
    legRight.castShadow = true;
    bench.add(legRight);

    this.scene!.add(bench);
    this.disposable.push(seatMat, metalMat);
  }

  private addProceduralTrashCan(x: number, z: number) {
    const binMat = new THREE.MeshStandardMaterial({ color: 0x1e3a8a, roughness: 0.5 });
    const binGeom = new THREE.CylinderGeometry(0.22, 0.18, 0.65, 12);
    const bin = new THREE.Mesh(binGeom, binMat);
    bin.position.set(x, 0.325, z);
    bin.castShadow = true;
    this.scene!.add(bin);
    this.disposable.push(binMat, binGeom);
  }

  private addLampPost(x: number, z: number) {
    // Post base (procedural dark pole)
    const pole = this.addBox([0.16, 3.4, 0.16], [x, 1.7, z], 0x475569, { metalness: 0.35, roughness: 0.42 });
    pole.castShadow = true;

    // Light bulb/glass part (procedural glowing box)
    const lightMaterial = new THREE.MeshStandardMaterial({
      color: 0xf8fafc,
      emissive: 0xfff1a6,
      emissiveIntensity: 0.0, // Empezamos apagado, se controla en updateDayNightCycle
      transparent: true,
      opacity: 0.82
    });
    this.disposable.push(lightMaterial);
    this.lampEmissiveMaterials.push(lightMaterial);

    const bulbGeometry = new THREE.BoxGeometry(0.62, 0.78, 0.2);
    this.disposable.push(bulbGeometry);

    const bulb = new THREE.Mesh(bulbGeometry, lightMaterial);
    bulb.position.set(x, 3.45, z);
    this.scene!.add(bulb);

    // Luz real del farol (cÃ¡lida e iluminadora, empieza en 0 y se actualiza dinÃ¡micamente)
    const light = new THREE.PointLight(0xfff1a6, 0.0, 15);
    light.position.set(x, 3.45, z);
    light.castShadow = false;
    this.scene!.add(light);

    this.lampLights.push(light);
  }

  private addStrategicLantern(x: number, z: number, rotationY: number) {
    this.loadSceneAsset('/models-optimized/environment/street_lantern.glb', (model) => {
      this.fitModelToHeight(model, 3.8);
      this.centerModelPivot(model);
      
      model.position.set(x, 0, z);
      this.alignModelBottom(model, 0); // Al ras del suelo
      model.rotation.y = rotationY;
      
      this.scene!.add(model);

      // TravesÃ­a para clonar y guardar los materiales emisivos de la linterna y asÃ­ hacerlos brillar de noche
      model.traverse((child) => {
        if (child instanceof THREE.Mesh) {
          if (child.material) {
            const mat = child.material.clone() as THREE.MeshStandardMaterial;
            child.material = mat;
            this.lampEmissiveMaterials.push(mat);
            this.disposable.push(mat);
          }
        }
      });

      // Luz real del farol (cÃ¡lida e iluminadora, empieza en 0 y se actualiza dinÃ¡micamente)
      const light = new THREE.PointLight(0xfff1a6, 0.0, 15);
      light.position.set(x, 3.4, z);
      light.castShadow = false; // OptimizaciÃ³n de rendimiento
      this.scene!.add(light);
      
      this.lampLights.push(light);
    }, { castShadow: false });
  }

  private createAmbientPokemon() {
    this.loadSceneAsset('/models-optimized/characters/bulbasaur_pokemon_animated.glb', (model, animations) => {
      model.position.set(8, 0, 8);
      model.scale.setScalar(0.55);
      model.rotation.y = -0.8;
      this.scene!.add(model);
      const mixer = new THREE.AnimationMixer(model);
      const walk = animations.find((clip) => /walk|idle/i.test(clip.name));
      if (walk) mixer.clipAction(walk).play();
      this.sceneMixers.push(mixer);
    });
  }

  private loadSceneAsset(
    path: string,
    onLoad: (model: THREE.Group, animations: THREE.AnimationClip[]) => void,
    options: { castShadow?: boolean; receiveShadow?: boolean; manager?: THREE.LoadingManager } = { castShadow: true, receiveShadow: true }
  ) {
    this.loadCachedGltf(path, options.manager || this.hubLoadingManager).then(
      (gltf) => {
        const model = gltf.scene;
        
        // Eliminar luces o cÃ¡maras del archivo GLTF para no interferir con las bounding boxes de Three.js
        const toRemove: THREE.Object3D[] = [];
        model.traverse((child) => {
          if (
            child instanceof THREE.Camera || 
            child instanceof THREE.Light || 
            child.type.includes('Light') || 
            child.type.includes('Camera')
          ) {
            toRemove.push(child);
          }
        });
        toRemove.forEach((child) => child.parent?.remove(child));

        model.traverse((child) => {
          const mesh = child as THREE.Mesh;
          if (mesh.isMesh) {
            mesh.castShadow = options.castShadow !== false;
            mesh.receiveShadow = options.receiveShadow !== false;
          }
        });
        onLoad(model, gltf.animations);
      },
      (error) => console.warn(`No se pudo cargar ${path}`, error)
    );
  }

  private createPikachuCompanion() {
    if (!this.scene || this.pikachu) return;

    const root = new THREE.Group();
    root.position.copy(this.player.position).add(new THREE.Vector3(1.2, 0, 1.8));
    this.scene.add(root);
    const companion: CampusNpc = { root, actions: new Map() };
    this.pikachu = companion;
    this.nextPikachuChirpAt = 1.2;

    this.loadSceneAsset('/models-optimized/characters/pikachu.glb', (model, animations) => {
      model.scale.setScalar(0.42);
      model.rotation.y = Math.PI;
      this.centerModelPivot(model);
      this.alignModelBottom(model, 0); // Sentar al ras del pasto
      root.add(model);
      companion.mixer = new THREE.AnimationMixer(model);
      animations.forEach((clip) => companion.actions.set(clip.name.toLowerCase(), companion.mixer!.clipAction(clip)));
      this.setNpcAnimation(companion, ['idle'], 0);
    });
  }

  private setNpcAnimation(npc: CampusNpc, hints: string[], fade = 0.18) {
    let action: THREE.AnimationAction | undefined;
    for (const hint of hints) {
      const entry = Array.from(npc.actions.entries())
        .find(([name]) => name.includes(hint.toLowerCase()));
      if (entry) {
        action = entry[1];
        break;
      }
    }

    if (!action || action === npc.active) return;
    action.reset().fadeIn(fade).play();
    npc.active?.fadeOut(fade);
    npc.active = action;
  }

  private updatePikachu(delta: number) {
    if (!this.pikachu) return;

    const root = this.pikachu.root;
    this.pikachu.mixer?.update(delta);

    // Inicializar variables de estado de la IA si no existen
    if (!root.userData['initialized']) {
      root.userData['initialized'] = true;
      root.userData['state'] = 'following'; // 'following' | 'wandering' | 'idle'
      root.userData['targetPos'] = root.position.clone();
      root.userData['idleTimer'] = 0;
      root.userData['isMoving'] = false;
    }

    const distToPlayer = root.position.distanceTo(this.player.position);
    
    // Teletransportar a Pikachu detrÃ¡s del jugador si se aleja excesivamente (distToPlayer > 12.0)
    if (distToPlayer > 12.0) {
      const behind = new THREE.Vector3(
        Math.sin(this.player.rotation.y + 0.65),
        0,
        Math.cos(this.player.rotation.y + 0.65)
      );
      const tpTarget = this.player.position.clone().addScaledVector(behind, 1.85);
      root.position.copy(tpTarget);
      root.userData['targetPos'] = tpTarget.clone();
      root.userData['state'] = 'following';
      root.userData['isMoving'] = false;
    }

    const isPlayerMoving = this.playerVelocity.lengthSq() > 0.08;

    let target = root.userData['targetPos'] as THREE.Vector3;
    let state = root.userData['state'] as string;
    let idleTimer = root.userData['idleTimer'] as number;

    // Transiciones de estado de la IA
    if (distToPlayer > 5.5 || isPlayerMoving) {
      state = 'following';
      idleTimer = 0;
    } else if (state === 'following' && distToPlayer <= 2.2) {
      state = 'idle';
      idleTimer = 1.5 + Math.random() * 2.0; // Esperar antes de merodear
    }

    // LÃ³gica y velocidad segÃºn el estado actual
    let speed = 0;

    if (state === 'following') {
      // Seguir al jugador colocÃ¡ndose en una posiciÃ³n diagonal/atrÃ¡s
      const behind = new THREE.Vector3(
        Math.sin(this.player.rotation.y + 0.65),
        0,
        Math.cos(this.player.rotation.y + 0.65)
      );
      target = this.player.position.clone().addScaledVector(behind, 1.85);
      
      // Correr si el jugador estÃ¡ acelerando (Shift presionado)
      const isRunning = this.keys.has('shift');
      speed = isRunning ? 7.6 : 4.4;
    } else if (state === 'idle') {
      speed = 0;
      target = root.position.clone(); // Quedarse en la posiciÃ³n actual
      
      if (idleTimer > 0) {
        idleTimer -= delta;
      } else {
        // Elegir un punto aleatorio en el pasto alrededor del jugador para merodear libremente
        const angle = Math.random() * Math.PI * 2;
        const radius = 1.0 + Math.random() * 1.8;
        target = this.player.position.clone().add(new THREE.Vector3(
          Math.cos(angle) * radius,
          0,
          Math.sin(angle) * radius
        ));
        state = 'wandering';
      }
    } else if (state === 'wandering') {
      speed = 1.8; // Caminata tranquila y relajada
      
      const distanceToWanderTarget = root.position.distanceTo(target);
      if (distanceToWanderTarget < 0.25 || distToPlayer > 3.4) {
        // LlegÃ³ al punto de merodeo o se alejÃ³ mucho del jugador, volver a reposo
        state = 'idle';
        idleTimer = 2.5 + Math.random() * 3.5;
        target = root.position.clone();
      }
    }

    // Guardar variables de estado actualizadas
    root.userData['state'] = state;
    target.y = this.getGroundHeightAt(target.x, target.z) + this.groundLift;
    root.userData['targetPos'] = target;
    root.userData['idleTimer'] = idleTimer;

    // Movimiento fÃ­sico y rotaciÃ³n suave con amortiguador de histeresis para evitar resets de animaciÃ³n
    const distToTarget = root.position.distanceTo(target);
    let isMoving = root.userData['isMoving'] || false;
    if (distToTarget > 0.55) {
      isMoving = true;
    }
    if (distToTarget < 0.18) {
      isMoving = false;
    }
    root.userData['isMoving'] = isMoving;
    root.position.y += (target.y - root.position.y) * (1 - Math.pow(0.0001, delta));

    if (isMoving && speed > 0) {
      const moveDir = target.clone().sub(root.position);
      moveDir.y = 0;
      moveDir.normalize();

      // Rotar suavemente hacia la direcciÃ³n de movimiento para que no rote bruscamente (con desfase Math.PI del modelo)
      const targetRotation = Math.atan2(moveDir.x, moveDir.z) + Math.PI;
      
      // Evitar rotaciones bruscas de 360 grados
      let diff = targetRotation - root.rotation.y;
      while (diff < -Math.PI) diff += Math.PI * 2;
      while (diff > Math.PI) diff -= Math.PI * 2;
      root.rotation.y += diff * (1 - Math.pow(0.0001, delta));

      // Desplazarse suavemente hacia la direcciÃ³n de movimiento
      const step = speed * delta;
      if (distToTarget > step) {
        root.position.addScaledVector(moveDir, step);
      } else {
        root.position.copy(target);
        isMoving = false;
        root.userData['isMoving'] = false;
      }

      // Elegir animaciÃ³n adecuada segÃºn velocidad
      this.setNpcAnimation(this.pikachu, speed > 4.8 ? ['run', 'running', 'walk', 'walking'] : ['walk', 'walking']);
    } else {
      // Mirar suavemente hacia el jugador cuando estÃ¡ ocioso
      const lookDir = this.player.position.clone().sub(root.position);
      lookDir.y = 0;
      if (lookDir.lengthSq() > 0.05) {
        lookDir.normalize();
        // Con desfase Math.PI del modelo para que mire de frente al jugador
        const targetRotation = Math.atan2(lookDir.x, lookDir.z) + Math.PI;
        let diff = targetRotation - root.rotation.y;
        while (diff < -Math.PI) diff += Math.PI * 2;
        while (diff > Math.PI) diff -= Math.PI * 2;
        root.rotation.y += diff * (1 - Math.pow(0.002, delta));
      }
      this.setNpcAnimation(this.pikachu, ['idle']);
    }
  }

  private updateKioskVendor(delta: number) {
    if (!this.kioskVendor) return;

    const dx = this.player.position.x + 18;
    const dz = this.player.position.z + 18;
    const distanceSq = dx * dx + dz * dz;
    if (distanceSq < 18 * 18 || this.kioskShopOpen) {
      this.ensureDetailedKioskProductsLoaded();
    }
    const farFromKiosk = distanceSq > 22 * 22;
    if (!farFromKiosk || this.frameCounter % 4 === 0) {
      this.kioskVendor.mixer?.update(delta * (farFromKiosk ? 4 : 1));
    }

    const lookTarget = this.vendorCameraFocus
      ? this.tempLookAt.set(-10.4, this.kioskVendor.root.position.y, -9.2)
      : this.player.position;
    this.kioskVendor.root.lookAt(lookTarget.x, this.kioskVendor.root.position.y, lookTarget.z);
    
    // Si el menú del kiosco está abierto o dialogando (decidiendo qué comprar / hablando)
    if (this.kioskShopOpen || this.kiosqueraDialogOpen) {
      const activeName = this.kioskVendor.active ? 
        Array.from(this.kioskVendor.actions.entries()).find(([_, act]) => act === this.kioskVendor!.active)?.[0] : '';
      
      // Si está reproduciendo pose_03 (compra), la dejamos terminar
      if (activeName && activeName.includes('pose_03')) {
        return;
      }
      
      // De lo contrario, loops speak_1 (hablando)
      this.setNpcAnimation(this.kioskVendor, ['speak_1']);
      return;
    }

    if (this.currentInteraction?.id === 'packs') {
      this.setNpcAnimation(this.kioskVendor, ['speak_1', 'greeting_1']);
    } else if (distanceSq < 7 * 7) {
      this.setNpcAnimation(this.kioskVendor, ['greeting_1', 'wave_1', 'idle']);
    } else {
      this.setNpcAnimation(this.kioskVendor, ['idle']);
    }
  }

  private createArenaRunways() {
    if (!this.scene) return;

    const runwayMaterial = new THREE.MeshStandardMaterial({
      color: 0x111827,
      emissive: 0x082f49,
      emissiveIntensity: 0.18,
      roughness: 0.52,
      metalness: 0.22
    });
    this.disposable.push(runwayMaterial);

    const routes = [
      { x: -7.5, z: -5.5, rot: -0.72, length: 20 },
      { x: 7.5, z: -5.5, rot: 0.72, length: 20 },
      { x: 0, z: -12, rot: 0, length: 26 }
    ];

    routes.forEach((route) => {
      const geometry = new THREE.BoxGeometry(2.35, 0.08, route.length);
      const runway = new THREE.Mesh(geometry, runwayMaterial);
      runway.position.set(route.x, 0.03, route.z);
      runway.rotation.y = route.rot;
      runway.receiveShadow = true;
      this.scene!.add(runway);
      this.disposable.push(geometry);
    });
  }

  private createPerimeterBanners() {
    if (!this.scene) return;

    const bannerBacks = [
      { x: -18, z: -28, rot: 0.18, texture: '/images/cards/base1-4.png' },
      { x: -9, z: -30, rot: 0.05, texture: '/images/cards/base1-2.png' },
      { x: 9, z: -30, rot: -0.05, texture: '/images/cards/swsh1-25.png' },
      { x: 18, z: -28, rot: -0.18, texture: '/images/cards/xy1-1.png' }
    ];

    bannerBacks.forEach((banner) => {
      const card = this.createTexturedCard(banner.texture, 2.45, 3.42);
      card.position.set(banner.x, 3.2, banner.z);
      card.rotation.set(-0.05, banner.rot, 0);
      card.castShadow = true;
      this.scene!.add(card);
      this.worldObjects.push(card);
    });
  }

  private createSceneryPillar(x: number, z: number, color: number) {
    if (!this.scene) return;

    const baseGeometry = new THREE.CylinderGeometry(0.8, 1.15, 2.2, 8);
    const baseMaterial = new THREE.MeshStandardMaterial({
      color,
      emissive: color,
      emissiveIntensity: 0.12,
      roughness: 0.5,
      metalness: 0.28
    });
    const base = new THREE.Mesh(baseGeometry, baseMaterial);
    base.position.set(x, 1.1, z);
    base.castShadow = true;
    base.receiveShadow = true;
    this.scene.add(base);
    this.disposable.push(baseGeometry, baseMaterial);

    const crystalGeometry = new THREE.OctahedronGeometry(0.65, 0);
    const crystalMaterial = new THREE.MeshStandardMaterial({
      color: 0xe0f2fe,
      emissive: color,
      emissiveIntensity: 0.85,
      metalness: 0.08,
      roughness: 0.2,
      transparent: true,
      opacity: 0.92
    });
    const crystal = new THREE.Mesh(crystalGeometry, crystalMaterial);
    crystal.position.set(x, 2.75, z);
    crystal.castShadow = true;
    crystal.userData = { spin: 0.45 + Math.random() * 0.35 };
    this.worldObjects.push(crystal);
    this.scene.add(crystal);
    this.disposable.push(crystalGeometry, crystalMaterial);
  }

  private createTrainerAvatar() {
    if (!this.scene) return;

    this.player = new THREE.Group();
    this.player.position.set(0, 0, 8.5);
    this.player.position.y = this.getGroundHeightAt(this.player.position.x, this.player.position.z) + this.groundLift;
    this.scene.add(this.player);
    this.createFallbackTrainerAvatar();
    this.loadAnimatedPlayerAsset();
    if (this.pikachuEnabled) this.createPikachuCompanion();
  }

  private createFallbackTrainerAvatar() {
    const bodyMaterial = new THREE.MeshStandardMaterial({ color: 0x2563eb, roughness: 0.42, metalness: 0.12 });
    const jacketMaterial = new THREE.MeshStandardMaterial({ color: 0xf8fafc, roughness: 0.38 });
    const capMaterial = new THREE.MeshStandardMaterial({ color: 0xef4444, roughness: 0.35 });
    const darkMaterial = new THREE.MeshStandardMaterial({ color: 0x111827, roughness: 0.5 });
    this.disposable.push(bodyMaterial, jacketMaterial, capMaterial, darkMaterial);

    const body = new THREE.Mesh(new THREE.CapsuleGeometry(0.38, 0.78, 8, 18), bodyMaterial);
    body.position.y = 1.05;
    body.castShadow = true;
    this.player.add(body);
    this.disposable.push(body.geometry);

    const chest = new THREE.Mesh(new THREE.BoxGeometry(0.86, 0.5, 0.18), jacketMaterial);
    chest.position.set(0, 1.16, 0.28);
    chest.castShadow = true;
    this.player.add(chest);
    this.disposable.push(chest.geometry);

    const head = new THREE.Mesh(new THREE.SphereGeometry(0.28, 24, 18), new THREE.MeshStandardMaterial({ color: 0xffd2a6, roughness: 0.52 }));
    head.position.y = 1.78;
    head.castShadow = true;
    this.player.add(head);
    this.disposable.push(head.geometry, head.material as THREE.Material);

    const cap = new THREE.Mesh(new THREE.SphereGeometry(0.31, 24, 12, 0, Math.PI * 2, 0, Math.PI / 2), capMaterial);
    cap.position.y = 1.89;
    cap.rotation.x = -0.08;
    cap.castShadow = true;
    this.player.add(cap);
    this.disposable.push(cap.geometry);

    const brim = new THREE.Mesh(new THREE.BoxGeometry(0.44, 0.06, 0.28), capMaterial);
    brim.position.set(0, 1.87, 0.24);
    brim.castShadow = true;
    this.player.add(brim);
    this.disposable.push(brim.geometry);

    const backpack = new THREE.Mesh(new THREE.BoxGeometry(0.55, 0.72, 0.2), darkMaterial);
    backpack.position.set(0, 1.12, -0.35);
    backpack.castShadow = true;
    this.player.add(backpack);
    this.disposable.push(backpack.geometry);

    const legGeometry = new THREE.CapsuleGeometry(0.12, 0.48, 6, 10);
    const leftLeg = new THREE.Mesh(legGeometry, darkMaterial);
    leftLeg.position.set(-0.17, 0.42, 0);
    leftLeg.castShadow = true;
    this.player.add(leftLeg);
    const rightLeg = leftLeg.clone();
    rightLeg.position.x = 0.17;
    this.player.add(rightLeg);
    this.disposable.push(legGeometry);
  }

  private loadAnimatedPlayerAsset() {
    const option = this.characterOptions.find((item) => item.id === this.selectedCharacterId) || this.characterOptions[0];
    this.currentCharacterOption = option;
    this.playerMixer?.stopAllAction();
    this.playerMixer = undefined;
    this.playerActions.clear();
    this.activePlayerAction = undefined;
    this.player.clear();
    this.createFallbackTrainerAvatar();

    const loader = this.createGltfLoader(this.hubLoadingManager);
    loader.load(
      option.path,
      (gltf) => {
        this.player.clear();

        const model = gltf.scene;
        model.name = 'AnimatedPlayerAsset';
        model.scale.setScalar(option.scale);
        model.position.set(0, option.yOffset, 0);
        model.rotation.y = option.rotationY;
        model.traverse((child) => {
          const mesh = child as THREE.Mesh;
          if (mesh.isMesh) {
            mesh.castShadow = true;
            mesh.receiveShadow = true;
          }
        });

        // Aplicar personalizaciÃ³n de rasgos (piel, pelo, ojos, altura)
        this.applyCharacterCustomizations(model);
        this.normalizeVisibleCharacterHeight(model, 1.72 * this.normalizeHeight(parseFloat(localStorage.getItem('lobbyHeight') || '1.0')));

        this.player.add(model);
        this.playerMixer = new THREE.AnimationMixer(model);
        this.playerActions.clear();

        gltf.animations.forEach((clip) => {
          const action = this.playerMixer!.clipAction(clip);
          this.playerActions.set(clip.name.toLowerCase(), action);
        });

        this.setPlayerAnimation('idle', 0);
      },
      undefined,
      (error) => {
        console.warn('No se pudo cargar el player GLB, usando fallback procedural.', error);
      }
    );
  }

  private applyCharacterCustomizations(model: THREE.Group) {
    const skinHex = localStorage.getItem('lobbySkinColor');
    const hairHex = localStorage.getItem('lobbyHairColor');
    const eyeHex = localStorage.getItem('lobbyEyeColor');
    const heightStr = localStorage.getItem('lobbyHeight') || '1.0';
    const heightFactor = parseFloat(heightStr);

    const option = this.currentCharacterOption || this.characterOptions[0];
    model.scale.setScalar(option.scale * heightFactor);
    this.tintCharacterParts(model, { skinHex, hairHex, eyeHex });
  }

  private normalizeVisibleCharacterHeight(model: THREE.Group, targetHeight: number) {
    model.updateMatrixWorld(true);
    const box = new THREE.Box3().setFromObject(model);
    const size = box.getSize(new THREE.Vector3());
    if (!Number.isFinite(size.y) || size.y <= 0.01) return;

    model.scale.multiplyScalar(targetHeight / size.y);
    this.alignModelBottom(model, 0);
  }

  private setPlayerAnimation(preferred: 'idle' | 'walking' | 'running', fade = 0.22) {
    this.localAnimationState = preferred;
    const action = this.findPlayerAction(preferred);
    if (!action || action === this.activePlayerAction) return;

    action.reset().fadeIn(fade).play();
    this.activePlayerAction?.fadeOut(fade);
    this.activePlayerAction = action;
  }

  private findPlayerAction(preferred: 'idle' | 'walking' | 'running'): THREE.AnimationAction | undefined {
    const option = this.currentCharacterOption;
    const candidates: Record<typeof preferred, string[]> = {
      idle: option?.idleHints ?? ['idle', 'standing'],
      walking: option?.walkHints ?? ['walking', 'walk'],
      running: option?.runHints ?? ['running', 'run']
    };

    for (const name of candidates[preferred]) {
      const exact = this.playerActions.get(name);
      if (exact) return exact;
    }

    return Array.from(this.playerActions.entries())
      .find(([name]) => candidates[preferred].some((candidate) => name.includes(candidate)))?.[1];
  }

  private createHubSpots() {
    if (!this.scene) return;

    this.hubSpots.forEach((spot) => {
      const group = new THREE.Group();
      group.position.copy(spot.position);
      group.userData = { spotId: spot.id };

      const ringMaterial = new THREE.MeshBasicMaterial({ color: spot.color, transparent: true, opacity: 0.72 });
      const ringGeometry = new THREE.TorusGeometry(1.12, 0.035, 10, 96);
      const ring = new THREE.Mesh(ringGeometry, ringMaterial);
      ring.rotation.x = Math.PI / 2;
      ring.position.y = 0.08;
      group.add(ring);
      this.disposable.push(ringGeometry, ringMaterial);

      const beamGeometry = new THREE.CylinderGeometry(0.92, 1.15, 3.2, 32, 1, true);
      const beamMaterial = new THREE.MeshBasicMaterial({
        color: spot.color,
        transparent: true,
        opacity: 0.13,
        side: THREE.DoubleSide,
        blending: THREE.AdditiveBlending,
        depthWrite: false
      });
      const beam = new THREE.Mesh(beamGeometry, beamMaterial);
      beam.position.y = 1.6;
      group.add(beam);
      this.disposable.push(beamGeometry, beamMaterial);

      this.decorateSpot(spot, group);

      spot.group = group;
      this.worldObjects.push(group);
      this.scene!.add(group);

      const light = new THREE.PointLight(spot.color, 2.4, 8);
      light.position.set(spot.position.x, 2.1, spot.position.z);
      this.scene!.add(light);
    });
  }

  private getCachedTexture(path: string): THREE.Texture {
    const cached = this.textureCache.get(path);
    if (cached) return cached;

    const texture = this.textureLoader.load(path);
    texture.colorSpace = THREE.SRGBColorSpace;
    texture.anisotropy = this.getRuntimeAnisotropy();
    texture.generateMipmaps = true;
    this.textureCache.set(path, texture);
    this.disposable.push(texture);
    return texture;
  }

  private getCachedPlaneGeometry(width: number, height: number): THREE.PlaneGeometry {
    const key = `${width.toFixed(3)}x${height.toFixed(3)}`;
    const cached = this.planeGeometryCache.get(key);
    if (cached) return cached;

    const geometry = new THREE.PlaneGeometry(width, height);
    this.planeGeometryCache.set(key, geometry);
    this.disposable.push(geometry);
    return geometry;
  }

  private getCachedCardMaterial(texturePath: string, emissive = 0x000000, emissiveIntensity = 0): THREE.MeshStandardMaterial {
    const key = `${texturePath}|${emissive}|${emissiveIntensity}`;
    const cached = this.cardMaterialCache.get(key);
    if (cached) return cached;

    const material = new THREE.MeshStandardMaterial({
      map: this.getCachedTexture(texturePath),
      color: 0xffffff,
      side: THREE.DoubleSide,
      roughness: emissiveIntensity ? 0.28 : 0.34,
      metalness: emissiveIntensity ? 0.08 : 0.04,
      emissive,
      emissiveIntensity
    });
    this.cardMaterialCache.set(key, material);
    this.disposable.push(material);
    return material;
  }

  private createTexturedCard(texturePath: string, width = 0.72, height = 1.0): THREE.Mesh {
    return new THREE.Mesh(
      this.getCachedPlaneGeometry(width, height),
      this.getCachedCardMaterial(texturePath)
    );
  }

  private createPackMesh(width = 0.92, height = 1.22): THREE.Mesh {
    return new THREE.Mesh(
      this.getCachedPlaneGeometry(width, height),
      this.getCachedCardMaterial('/images/cards/sobre.png', 0x10351e, 0.18)
    );
  }

  private decorateSpot(spot: HubSpot, group: THREE.Group) {
    const colorMaterial = new THREE.MeshStandardMaterial({
      color: spot.color,
      emissive: spot.color,
      emissiveIntensity: 0.35,
      roughness: 0.28,
      metalness: 0.25
    });
    this.disposable.push(colorMaterial);

    if (spot.id === 'battle') {
      const gateGeometry = new THREE.TorusGeometry(1.25, 0.09, 14, 96);
      const gate = new THREE.Mesh(gateGeometry, colorMaterial);
      gate.position.y = 1.65;
      gate.rotation.y = Math.PI / 2;
      gate.castShadow = true;
      group.add(gate);
      this.disposable.push(gateGeometry);
      return;
    }

    if (spot.id === 'packs') {
      const signGeometry = new THREE.BoxGeometry(1.55, 0.08, 0.88);
      const sign = new THREE.Mesh(signGeometry, colorMaterial);
      sign.position.y = 0.08;
      sign.castShadow = false;
      group.add(sign);
      this.disposable.push(signGeometry);
      return;
    }

    if (spot.id === 'santoro') {
      const deskGeometry = new THREE.BoxGeometry(1.55, 0.28, 0.82);
      const desk = new THREE.Mesh(deskGeometry, colorMaterial);
      desk.position.set(0, 0.72, 0.12);
      desk.castShadow = true;
      desk.receiveShadow = true;
      group.add(desk);
      this.disposable.push(deskGeometry);

      const pack = this.createPackMesh(0.38, 0.52);
      pack.position.set(-0.38, 0.98, -0.02);
      pack.rotation.set(-Math.PI / 2, 0.25, 0);
      group.add(pack);
      return;
    }

    const tableGeometry = new THREE.BoxGeometry(1.85, 0.25, 1.05);
    const table = new THREE.Mesh(tableGeometry, colorMaterial);
    table.position.y = 0.85;
    table.castShadow = true;
    group.add(table);
    this.disposable.push(tableGeometry);

    for (let i = 0; i < 4; i++) {
      const texture = this.cardTexturePaths[i % this.cardTexturePaths.length];
      const card = this.createTexturedCard(texture, 0.36, 0.5);
      card.position.set(-0.52 + i * 0.35, 1.04, 0.02 + (i % 2) * 0.14);
      card.rotation.set(-Math.PI / 2, -0.18 + i * 0.11, 0);
      group.add(card);
    }
  }

  private createFloatingCards() {
    if (!this.scene) return;

    for (let i = 0; i < 14; i++) {
      const texture = this.cardTexturePaths[i % this.cardTexturePaths.length];
      const card = this.createTexturedCard(texture, 0.44 + (i % 3) * 0.05, 0.62 + (i % 3) * 0.08);
      const angle = (i / 14) * Math.PI * 2;
      const radius = 14 + (i % 4) * 1.2;
      card.position.set(Math.cos(angle) * radius, 1.6 + (i % 4) * 0.42, Math.sin(angle) * radius - 9);
      card.rotation.set(Math.random(), angle, Math.random() * 0.5);
      card.userData = { angle, radius, speed: 0.06 + (i % 6) * 0.018, y: card.position.y, orbitalOffset: -5 };
      card.castShadow = this.graphicsQuality === 'high';
      this.worldObjects.push(card);
      this.scene.add(card);
    }
  }

  private animateHub() {
    this.animationId = requestAnimationFrame(() => this.animateHub());
    if (!this.renderer || !this.scene || !this.camera) return;
    if (!this.pageVisible) {
      this.clock.getDelta();
      return;
    }

    const delta = Math.min(this.clock.getDelta(), 0.05);
    const elapsed = this.clock.elapsedTime;
    this.frameCounter++;
    this.updateAdaptivePerformance(delta);
    this.updateDebugStats(delta);
    const overlayPressure = this.deckBuilderOpen || this.activeTradeSession || this.kioskShopOpen || this.mostrarAnimacionSobre;
    const reduceDecorativeWork = overlayPressure || this.adaptivePixelRatioScale < 0.9;
    const renderThisFrame = !reduceDecorativeWork || this.frameCounter % 2 === 0;

    this.playerMixer?.update(delta);
    if (!reduceDecorativeWork || this.frameCounter % 2 === 0) {
      this.sceneMixers.forEach((mixer) => mixer.update(delta * (reduceDecorativeWork ? 2 : 1)));
    }
    this.updatePlayer(delta);
    this.updatePikachu(delta);
    this.updateKioskVendor(delta);
    this.updateSantoroNpc(delta);
    this.updateSantoroPathArrows(elapsed);
    this.updateLobbyAudio(delta);
    this.updateRemoteDetailBudget();

    this.updateDayNightCycle(elapsed);
    if (!reduceDecorativeWork || this.frameCounter % 2 === 0) {
      this.updateHubObjects(elapsed);
    }
    this.cameraOrbitPitch = Math.max(-0.4, Math.min(1.2, this.cameraOrbitPitch)); // Clampar Pitch
    
    // Update camera first
    this.updateCamera(delta);
    
    // Force camera matrix update to prevent projection lag
    this.camera.updateMatrixWorld(true);

    // Update other players and local bubble projection using the new camera matrices
    this.updateOtherPlayers(delta);
    this.updateLocalPlayerBubbleProjection();
    this.updateInteractionState();
    if (this.santoroQuestTracking && !this.santoroGiftClaimed && this.frameCounter % 45 === 0) {
      this.updateSantoroQuestPath();
    }

    this.checkAndSendMove();
    if (renderThisFrame) {
      this.renderer.render(this.scene, this.camera);
    }
  }

  private updateDebugStats(delta: number) {
    if (!this.showDebugPanel || !this.renderer) return;

    this.debugStatsTime += delta;
    this.debugStatsFrames++;
    if (this.debugStatsTime < 0.5) return;

    const fps = this.debugStatsFrames / this.debugStatsTime;
    const info = this.renderer.info;

    this.ngZone.run(() => {
      this.debugFps = Math.round(fps);
      this.debugFrameMs = Math.round((1000 / Math.max(1, fps)) * 10) / 10;
      this.debugDrawCalls = info.render.calls;
      this.debugTriangles = info.render.triangles;
      this.debugGeometries = info.memory.geometries;
      this.debugTextures = info.memory.textures;
      this.debugPixelRatio = Math.round(this.lastAppliedPixelRatio * 100) / 100;
      this.debugAdaptiveScale = Math.round(this.adaptivePixelRatioScale * 100);
      this.cdr.detectChanges();
    });

    this.debugStatsTime = 0;
    this.debugStatsFrames = 0;
  }

  toggleLobbyAudio() {
    this.audioEnabled = !this.audioEnabled;
    localStorage.setItem('lobbyAudioEnabled', String(this.audioEnabled));
    if (this.audioEnabled) {
      this.unlockLobbyAudio();
    } else {
      this.pauseAmbientMusic();
    }
  }

  toggleLobbyMusic() {
    this.musicEnabled = !this.musicEnabled;
    localStorage.setItem('lobbyMusicEnabled', String(this.musicEnabled));
    if (this.musicEnabled) {
      this.unlockLobbyAudio();
    } else {
      this.pauseAmbientMusic();
    }
  }

  toggleLobbySfx() {
    this.sfxEnabled = !this.sfxEnabled;
    localStorage.setItem('lobbySfxEnabled', String(this.sfxEnabled));
  }

  cycleLobbyVolume() {
    const levels = [0.25, 0.45, 0.65, 0.85];
    const current = levels.findIndex((level) => this.audioVolume <= level + 0.01);
    this.audioVolume = levels[(current + 1) % levels.length];
    localStorage.setItem('lobbyAudioVolume', String(this.audioVolume));
    this.applyLobbyAudioLevels();
    this.unlockLobbyAudio();
  }

  get audioVolumeLabel(): string {
    return `${Math.round(this.audioVolume * 100)}%`;
  }

  private unlockLobbyAudio() {
    if (!this.audioEnabled) return;
    this.ensureLobbyAudioGraph();
    this.audioContext?.resume().catch(() => undefined);
    this.playAmbientMusic();
  }

  private ensureLobbyAudioGraph() {
    if (!this.audioContext) {
      const AudioCtor = window.AudioContext || (window as any).webkitAudioContext;
      if (!AudioCtor) return;
      this.audioContext = new AudioCtor();
      this.masterGain = this.audioContext.createGain();
      this.sfxGain = this.audioContext.createGain();
      this.sfxGain.connect(this.masterGain);
      this.masterGain.connect(this.audioContext.destination);
    }

    if (!this.ambientAudio) {
      const track = this.ambientTracks[this.ambientTrackIndex % this.ambientTracks.length];
      this.ambientAudio = new Audio(track);
      this.ambientAudio.loop = true;
      this.ambientAudio.preload = 'auto';
    }

    this.applyLobbyAudioLevels();
  }

  private applyLobbyAudioLevels() {
    if (this.masterGain) this.masterGain.gain.value = this.audioEnabled ? this.audioVolume : 0;
    if (this.sfxGain) this.sfxGain.gain.value = this.sfxEnabled ? 0.78 : 0;
    if (this.ambientAudio) {
      this.ambientAudio.volume = this.audioEnabled && this.musicEnabled ? Math.min(0.42, this.audioVolume * 0.48) : 0;
    }
  }

  private playAmbientMusic() {
    if (!this.audioEnabled || !this.musicEnabled) return;
    this.ensureLobbyAudioGraph();
    this.applyLobbyAudioLevels();
    this.ambientAudio?.play().catch(() => undefined);
  }

  private pauseAmbientMusic() {
    this.ambientAudio?.pause();
  }

  private shutdownLobbyAudio() {
    this.pauseAmbientMusic();
    this.ambientAudio = undefined;
    this.audioContext?.close().catch(() => undefined);
    this.audioContext = undefined;
    this.masterGain = undefined;
    this.sfxGain = undefined;
  }

  private updateLobbyAudio(delta: number) {
    if (!this.audioEnabled || !this.sfxEnabled || !this.audioContext || !this.sfxGain) return;

    const speedSq = this.playerVelocity.lengthSq();
    const isMoving = speedSq > 0.18 && !this.kioskShopOpen && !this.deckBuilderOpen && !this.santoroDialogOpen && !this.kiosqueraDialogOpen;
    if (isMoving) {
      const running = this.localAnimationState === 'running';
      this.footstepClock -= delta;
      if (this.footstepClock <= 0) {
        this.playFootstep(this.player.position, running);
        this.footstepClock = running ? 0.24 : 0.38;
      }
    } else {
      this.footstepClock = 0;
    }

    this.nextPikachuChirpAt -= delta;
    if (this.pikachu && this.nextPikachuChirpAt <= 0) {
      const distance = this.pikachu.root.position.distanceTo(this.player.position);
      if (distance < 7.5) this.playPikachuChirp(this.pikachu.root.position, false);
      this.nextPikachuChirpAt = 4 + Math.random() * 6;
    }

    this.remoteAudioCheckClock -= delta;
    if (this.remoteAudioCheckClock > 0) return;
    const remoteDelta = Math.max(delta, 0.5);
    this.remoteAudioCheckClock = 0.5;

    this.otherPlayers.forEach((other) => {
      if (!other.pikachu) return;
      const current = (this.remotePikachuChirpTimers.get(other.username) ?? (5 + Math.random() * 8)) - remoteDelta;
      if (current <= 0) {
        const distance = other.pikachu.root.position.distanceTo(this.player.position);
        if (distance < 10) this.playPikachuChirp(other.pikachu.root.position, true);
        this.remotePikachuChirpTimers.set(other.username, 6 + Math.random() * 9);
      } else {
        this.remotePikachuChirpTimers.set(other.username, current);
      }
    });
  }

  private playFootstep(position: THREE.Vector3, running: boolean) {
    const ctx = this.audioContext;
    if (!ctx || !this.sfxGain) return;
    const { pan, volume } = this.getSpatialAudioMix(position, 18);
    if (volume <= 0.01) return;

    const osc = ctx.createOscillator();
    const gain = ctx.createGain();
    const filter = ctx.createBiquadFilter();
    const panner = ctx.createStereoPanner();

    osc.type = 'triangle';
    osc.frequency.setValueAtTime(running ? 92 : 74, ctx.currentTime);
    osc.frequency.exponentialRampToValueAtTime(running ? 42 : 36, ctx.currentTime + 0.07);
    filter.type = 'lowpass';
    filter.frequency.value = running ? 620 : 480;
    panner.pan.value = pan;
    gain.gain.setValueAtTime(0.0001, ctx.currentTime);
    gain.gain.exponentialRampToValueAtTime((running ? 0.11 : 0.075) * volume, ctx.currentTime + 0.012);
    gain.gain.exponentialRampToValueAtTime(0.0001, ctx.currentTime + 0.105);

    osc.connect(filter);
    filter.connect(gain);
    gain.connect(panner);
    panner.connect(this.sfxGain);
    osc.start();
    osc.stop(ctx.currentTime + 0.12);
  }

  private playPikachuChirp(position: THREE.Vector3, distant: boolean) {
    const ctx = this.audioContext;
    if (!ctx || !this.sfxGain) return;
    const { pan, volume } = this.getSpatialAudioMix(position, distant ? 12 : 9);
    if (volume <= 0.015) return;

    const now = ctx.currentTime;
    const panner = ctx.createStereoPanner();
    const gain = ctx.createGain();
    panner.pan.value = pan;
    gain.gain.setValueAtTime(0.0001, now);
    gain.gain.exponentialRampToValueAtTime((distant ? 0.055 : 0.105) * volume, now + 0.02);
    gain.gain.exponentialRampToValueAtTime(0.0001, now + 0.55);
    panner.connect(gain);
    gain.connect(this.sfxGain);

    [980, 1320, 1180, 1560, 1280, 1720].forEach((freq, index) => {
      const osc = ctx.createOscillator();
      osc.type = index % 2 === 0 ? 'sine' : 'triangle';
      const start = now + index * 0.052;
      osc.frequency.setValueAtTime(freq, start);
      osc.frequency.exponentialRampToValueAtTime(freq * (index < 3 ? 1.16 : 0.92), start + 0.08);
      osc.connect(panner);
      osc.start(start);
      osc.stop(start + 0.105);
    });
  }

  private getSpatialAudioMix(position: THREE.Vector3, maxDistance: number): { pan: number; volume: number } {
    const distance = position.distanceTo(this.player.position);
    const volume = Math.max(0, 1 - distance / maxDistance) ** 1.6;
    if (!this.camera) return { pan: 0, volume };

    const toSound = position.clone().sub(this.camera.position);
    toSound.y = 0;
    if (toSound.lengthSq() < 0.001) return { pan: 0, volume };
    toSound.normalize();

    const right = new THREE.Vector3(1, 0, 0).applyQuaternion(this.camera.quaternion);
    right.y = 0;
    right.normalize();
    return { pan: Math.max(-0.9, Math.min(0.9, right.dot(toSound))), volume };
  }

  private updatePlayer(delta: number) {
    if (this.kioskShopOpen || this.deckBuilderOpen || this.santoroDialogOpen || this.kiosqueraDialogOpen) {
      this.playerVelocity.set(0, 0, 0);
      this.setPlayerAnimation('idle');
      return;
    }

    const rotateSpeed = 2.35;
    const isRunning = this.keys.has('shift');
    const walkSpeed = isRunning ? 7.4 : 4.1;
    const forwardPressed = this.keys.has('w') || this.keys.has('arrowup');
    const backPressed = this.keys.has('s') || this.keys.has('arrowdown');
    const leftPressed = this.keys.has('a') || this.keys.has('arrowleft');
    const rightPressed = this.keys.has('d') || this.keys.has('arrowright');
    const analogTurn = Math.abs(this.mobileJoystickX) > 0.08 ? this.mobileJoystickX : 0;
    const analogMove = Math.abs(this.mobileJoystickY) > 0.08 ? this.mobileJoystickY : 0;

    const isMovingInput = forwardPressed || backPressed || leftPressed || rightPressed || analogTurn !== 0 || analogMove !== 0;
    if (isMovingInput && this.isPlayingEmote) {
      this.isPlayingEmote = false;
      this.emoteAnimationName = undefined;
      this.activePlayerAction?.fadeOut(0.15);
      this.activePlayerAction = undefined;
    }

    if (analogTurn !== 0) {
      this.player.rotation.y -= analogTurn * rotateSpeed * delta;
    } else {
      if (leftPressed) this.player.rotation.y += rotateSpeed * delta;
      if (rightPressed) this.player.rotation.y -= rotateSpeed * delta;
    }

    const direction = new THREE.Vector3(Math.sin(this.player.rotation.y), 0, Math.cos(this.player.rotation.y));
    const keyboardMove = (forwardPressed ? -1 : 0) + (backPressed ? 1 : 0);
    const move = analogMove !== 0 ? analogMove : keyboardMove;
    this.playerVelocity.lerp(direction.multiplyScalar(move * walkSpeed), 0.18);

    // Guardar posiciÃ³n anterior para resolver colisiones
    const oldX = this.player.position.x;
    const oldZ = this.player.position.z;

    // Mover temporalmente en X y Z
    this.player.position.addScaledVector(this.playerVelocity, delta);

    // Cajas delimitadoras de colisiÃ³n para los edificios (UTN, torre de vidrio, kiosco, fondo)
    const collisionBoxes = [
      { minX: 11.2, maxX: 31.0, minZ: -36.0, maxZ: 20.0 }, // Edificio Principal UTN
      { minX: 6.5, maxX: 12.3, minZ: -29.0, maxZ: -21.0 }, // Torre de Vidrio
      { minX: -24.2, maxX: -13.8, minZ: -23.8, maxZ: -15.0 }, // Estructura Exterior Kiosco
      { minX: -31.5, maxX: -2.5, minZ: -53.5, maxZ: -40.5 }  // Edificio de Fondo
    ];

    const playerRadius = 0.45;
    for (const box of collisionBoxes) {
      if (
        this.player.position.x + playerRadius > box.minX &&
        this.player.position.x - playerRadius < box.maxX &&
        this.player.position.z + playerRadius > box.minZ &&
        this.player.position.z - playerRadius < box.maxZ
      ) {
        // Resolver eje X primero
        this.player.position.x = oldX;
        if (
          this.player.position.x + playerRadius > box.minX &&
          this.player.position.x - playerRadius < box.maxX &&
          this.player.position.z + playerRadius > box.minZ &&
          this.player.position.z - playerRadius < box.maxZ
        ) {
          // Si sigue colisionando, revertir eje Z y permitir X (efecto deslizamiento)
          this.player.position.z = oldZ;
          this.player.position.x = oldX + this.playerVelocity.x * delta;
          if (
            this.player.position.x + playerRadius > box.minX &&
            this.player.position.x - playerRadius < box.maxX &&
            this.player.position.z + playerRadius > box.minZ &&
            this.player.position.z - playerRadius < box.maxZ
          ) {
            this.player.position.x = oldX;
          }
        }
      }
    }

    if (move !== 0) {
      this.setPlayerAnimation(isRunning ? 'running' : 'walking');
    } else if (!this.isPlayingEmote) {
      this.setPlayerAnimation('idle');
    }

    const maxRadius = 30.5;
    const dist = Math.hypot(this.player.position.x, this.player.position.z);
    if (dist > maxRadius) {
      this.player.position.multiplyScalar(maxRadius / dist);
    }
    if (this.boundaryMesh) {
      const boundaryOpacity = Math.max(0, Math.min(0.48, (dist - 19.5) / (30.5 - 19.5)));
      (this.boundaryMesh.material as THREE.MeshBasicMaterial).opacity = boundaryOpacity;
    }

    if (!this.playerMixer) {
      const bob = Math.sin(this.clock.elapsedTime * (forwardPressed || backPressed ? 11 : 3)) * (forwardPressed || backPressed ? 0.045 : 0.015);
      this.player.position.y = this.getGroundHeightAt(this.player.position.x, this.player.position.z) + this.groundLift + Math.max(0, bob);
    } else {
      this.player.position.y = this.getGroundHeightAt(this.player.position.x, this.player.position.z) + this.groundLift;
    }
  }

  private updateHubObjects(elapsed: number) {
    this.worldObjects.forEach((object) => {
      if (object.name.startsWith('OptimizedCloud_')) {
        // Viento/Deriva orbital lento: avanzan de forma continua
        const angle = object.userData['angle'] + elapsed * 0.05 * object.userData['speed'];
        object.position.x = Math.cos(angle) * object.userData['radius'];
        object.position.z = Math.sin(angle) * object.userData['radius'] - 8;
        // Balanceo vertical sutilísimo tipo flotación
        object.position.y = object.userData['y'] + Math.sin(elapsed * 0.4 + object.userData['angle']) * 0.38;
        // Rotación local sumamente lenta (sólo cambia de orientación imperceptiblemente)
        object.rotation.y = angle * 0.08;
      } else if (object instanceof THREE.Mesh && object.userData['radius']) {
        const dx = object.position.x - this.player.position.x;
        const dz = object.position.z - this.player.position.z;
        const farDecor = dx * dx + dz * dz > 1600;
        object.visible = !farDecor || this.graphicsQuality === 'high';
        if (!object.visible) return;

        const angle = object.userData['angle'] + elapsed * object.userData['speed'];
        object.position.x = Math.cos(angle) * object.userData['radius'];
        object.position.z = Math.sin(angle) * object.userData['radius'] + (object.userData['orbitalOffset'] ?? -2);
        object.position.y = object.userData['y'] + Math.sin(elapsed * 1.4 + object.userData['angle']) * 0.18;
        object.rotation.y += 0.01;
        object.rotation.x += 0.004;
      } else if (object.userData['spin']) {
        object.rotation.y += object.userData['spin'] * 0.01;
      } else if (object instanceof THREE.Group) {
        if (this.adaptivePixelRatioScale < 0.85 && this.frameCounter % 4 !== 0) return;
        object.rotation.y += 0.006;
        object.children.forEach((child, index) => {
          child.position.y += Math.sin(elapsed * 2.2 + index) * 0.0008;
        });
      }
    });
  }

  private updateCamera(delta: number) {
    if (!this.camera) return;

    if (this.showFirstTimeSetup || this.customizationModalOpen) {
      // Hermoso plano medio corto de primer plano frente al jugador para personalizarlo en tiempo real!
      const angle = this.player.rotation.y + Math.PI; // Enfocar de frente
      const previewDistance = 1.95;
      const targetPosition = this.tempVec3.set(
        this.player.position.x + Math.sin(angle) * previewDistance,
        this.player.position.y + 1.15,
        this.player.position.z + Math.cos(angle) * previewDistance
      );
      const lookAt = this.tempVec3b.set(this.player.position.x, this.player.position.y + 0.88, this.player.position.z);
      
      this.camera.position.lerp(targetPosition, 1 - Math.pow(0.0005, delta));
      this.camera.lookAt(lookAt);
      return;
    }

    if (this.santoroDialogOpen && this.santoroNpc) {
      // Zoom cinematográfico dinámico sobre Santoro de cara a su rotación
      const npcPos = this.santoroNpc.root.position;
      const angle = this.santoroNpc.root.rotation.y;
      const camDistance = 1.65; // Distancia media corta premium
      const targetPosition = this.tempVec3.set(
        npcPos.x + Math.sin(angle) * camDistance,
        npcPos.y + 1.54,
        npcPos.z + Math.cos(angle) * camDistance
      );
      const lookAt = this.tempVec3b.set(npcPos.x, npcPos.y + 1.44, npcPos.z);

      this.camera.position.lerp(targetPosition, 1 - Math.pow(0.0005, delta));
      this.camera.lookAt(lookAt);
      return;
    }

    if (this.vendorCameraFocus) {
      // Apuntar al centro de Hilda re-ubicada en X=-19.6 para que no la tape la estanterÃ­a
      const targetPosition = this.tempVec3.set(-17.8, 1.8, -14.5);
      const lookAt = this.tempVec3b.set(-19.6, 1.5, -17.2);
      this.camera.position.lerp(targetPosition, 1 - Math.pow(0.0008, delta));
      this.camera.lookAt(lookAt);
      return;
    }

    // Calcular Ã³rbita de cÃ¡mara basÃ¡ndose en la rotaciÃ³n del jugador + desvÃ­o por arrastre del mouse (Yaw)
    const baseAngle = this.player.rotation.y;
    const finalYaw = baseAngle + this.cameraOrbitYaw;

    // Calcular la posiciÃ³n esfÃ©rica del offset de la cÃ¡mara segÃºn Yaw y Pitch
    const horizontalDistance = this.cameraZoomDistance * Math.cos(this.cameraOrbitPitch);
    const verticalDistance = this.cameraZoomDistance * Math.sin(this.cameraOrbitPitch);

    const targetPosition = this.tempVec3.set(
      this.player.position.x + Math.sin(finalYaw) * horizontalDistance,
      this.player.position.y + verticalDistance,
      this.player.position.z + Math.cos(finalYaw) * horizontalDistance
    );
    const lookAt = this.tempVec3b.set(this.player.position.x, this.player.position.y + 1.55, this.player.position.z);

    this.camera.position.lerp(targetPosition, 1 - Math.pow(0.001, delta));
    this.camera.lookAt(lookAt);
  }

  private updateInteractionState() {
    if (!this.camera) return;

    let closestId: HubSpot['id'] | null = null;
    let minDistance = Number.POSITIVE_INFINITY;
    const playerX = this.player.position.x;
    const playerZ = this.player.position.z;

    this.hubSpots.forEach((spot) => {
      const dx = playerX - spot.position.x;
      const dz = playerZ - spot.position.z;
      const dist = Math.hypot(dx, dz);
      if (dist < 3.6 && dist < minDistance) {
        minDistance = dist;
        closestId = spot.id;
      }

      if (this.frameCounter % 2 === 0 || closestId === spot.id) {
        this.tempVec3.copy(spot.position);
        this.tempVec3.y += 2.3;
        this.tempVec3.project(this.camera!);
        spot.screenX = (this.tempVec3.x * 0.5 + 0.5) * 100;
        spot.screenY = (-this.tempVec3.y * 0.5 + 0.5) * 100;
      }
    });

    const closest = closestId ? this.hubSpots.find((spot) => spot.id === closestId) ?? null : null;
    const nextId = closestId;
    if (nextId !== this.lastInteractionId) {
      this.lastInteractionId = nextId;
      this.ngZone.run(() => {
        this.currentInteraction = closest;
        if (nextId === 'santoro' && this.santoroQuestTracking) {
          this.stopSantoroTracking();
        }
        this.cdr.detectChanges();
      });
    }
  }

  // ================= CLIENTE WEBSOCKET & MULTIPLAYER REPLICATION =================

  private getWsUrl(): string {
    const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
    const host = window.location.hostname || 'localhost';
    return getApiWsUrl() + '/lobby-ws';
  }

  private connectWebSocket() {
    if (!this.jugador?.username) return;

    try {
      const wsUrl = this.getWsUrl();
      console.log('Conectando a WebSocket del Lobby:', wsUrl);
      this.socket = new WebSocket(wsUrl);

      this.socket.onopen = () => {
        console.log('ConexiÃ³n WebSocket establecida con Ã©xito.');
        if (this.personalizationSynced) {
          this.sendJoinMessage();
        }
      };

      this.socket.onmessage = (event) => {
        this.ngZone.run(() => {
          this.handleWebSocketMessage(event.data);
        });
      };

      this.socket.onclose = () => {
        if (this.lobbyDestroyed) return;
        console.warn('ConexiÃ³n WebSocket cerrada. Reintentando en 5 segundos...');
        this.reconnectTimeout = setTimeout(() => this.connectWebSocket(), 5000);
      };

      this.socket.onerror = (error) => {
        console.error('Error en WebSocket del Lobby:', error);
      };
    } catch (e) {
      console.error('Error al instanciar WebSocket:', e);
    }
  }

  private sendJoinMessage() {
    if (!this.socket || this.socket.readyState !== WebSocket.OPEN || !this.jugador?.username) return;

    const payload = {
      type: 'JOIN',
      ...this.getLocalLobbyIdentityPayload(),
      x: this.player.position.x,
      y: this.player.position.y,
      z: this.player.position.z,
      rotY: this.player.rotation.y,
      animation: this.localAnimationState
    };

    this.socket.send(JSON.stringify(payload));
  }

  private getLocalLobbyIdentityPayload() {
    return {
      username: this.jugador!.username,
      characterId: this.normalizeCharacterId(this.selectedCharacterId),
      skinColor: localStorage.getItem('lobbySkinColor') || '#ffe0bd',
      hairColor: localStorage.getItem('lobbyHairColor') || '#5c4033',
      eyeColor: localStorage.getItem('lobbyEyeColor') || '#2563eb',
      height: this.normalizeHeight(parseFloat(localStorage.getItem('lobbyHeight') || '1.0')),
      pikachuCompanion: this.pikachuEnabled
    };
  }

  private normalizeCharacterId(characterId?: string): string {
    return this.characterOptions.some((option) => option.id === characterId) ? characterId! : this.characterOptions[0].id;
  }

  private normalizeHeight(height: number): number {
    if (!Number.isFinite(height) || height <= 0) return 1;
    return Math.max(0.72, Math.min(1.28, height));
  }

  private checkAndSendMove() {
    if (!this.socket || this.socket.readyState !== WebSocket.OPEN || !this.jugador?.username) return;

    const now = performance.now();
    const minMoveInterval = this.isMobileViewport || this.adaptivePixelRatioScale < 0.9 ? 70 : 45;
    if (now - this.lastMoveSentTime < minMoveInterval) return;

    const isMoving = this.playerVelocity.lengthSq() > 0.001 || Math.abs(this.player.rotation.y - this.lastSentRotY) > 0.01;
    if (!isMoving && this.localAnimationState === 'idle' && this.lastSentAnimation === 'idle') {
      return;
    }

    const payload = {
      type: 'MOVE',
      ...this.getLocalLobbyIdentityPayload(),
      x: this.player.position.x,
      y: this.player.position.y,
      z: this.player.position.z,
      rotY: this.player.rotation.y,
      animation: this.localAnimationState
    };

    this.socket.send(JSON.stringify(payload));
    this.lastMoveSentTime = now;
    this.lastSentRotY = this.player.rotation.y;
    this.lastSentAnimation = this.localAnimationState;
  }

  private handleWebSocketMessage(dataStr: string) {
    try {
      const msg = JSON.parse(dataStr);
      if (!msg || !msg.username || msg.username === this.jugador?.username) return;

      const type = msg.type;
      const username = msg.username;

      if (type === 'JOIN') {
        this.registerOtherPlayer(msg);
      } else if (type === 'MOVE') {
        this.updateOtherPlayerState(msg);
      } else if (type === 'LEAVE') {
        this.removeOtherPlayer(username);
      } else if (type === 'CHAT') {
        this.addChatMessage(username, msg.text);
        const p = this.otherPlayers.get(username);
        if (p) {
          this.showOtherPlayerSpeechBubble(p, msg.text);
        }
      } else if (type === 'EMOTE') {
        const p = this.otherPlayers.get(username);
        if (p) {
          this.playOtherPlayerEmote(p, msg.emote);
        }
      } else if (type === 'CHALLENGE_DUEL') {
        this.handleIncomingChallenge(msg);
      } else if (type === 'CHALLENGE_DUEL_RESPONSE') {
        this.handleChallengeResponse(msg);
      } else if (type === 'BATTLE_START') {
        this.handleBattleStart(msg);
      } else if (type === 'INVITE_TRADE') {
        this.handleIncomingTradeInvite(msg);
      } else if (type === 'INVITE_TRADE_RESPONSE') {
        this.handleTradeInviteResponse(msg);
      } else if (type === 'TRADE_UPDATE') {
        this.handleTradeUpdate(msg);
      } else if (type === 'TRADE_CLOSE') {
        this.handleTradeClose(msg);
      }
    } catch (e) {
      console.error('Error parseando mensaje WebSocket:', e);
    }
  }

  private registerOtherPlayer(msg: any) {
    if (!this.scene) {
      setTimeout(() => this.registerOtherPlayer(msg), 100);
      return;
    }

    const username = msg.username;
    if (this.otherPlayers.has(username)) {
      this.updateOtherPlayerState(msg);
      return;
    }

    console.log('Registrando nuevo jugador online en el lobby:', username);

    const root = new THREE.Group();
    root.position.set(msg.x, this.getGroundHeightAt(Number(msg.x), Number(msg.z)) + this.groundLift, msg.z);
    root.rotation.y = msg.rotY;
    this.scene?.add(root);

    const otherPlayer: OtherPlayerNPC = {
      username: username,
      characterId: this.normalizeCharacterId(msg.characterId),
      skinColor: msg.skinColor || '#ffe0bd',
      hairColor: msg.hairColor || '#5c4033',
      eyeColor: msg.eyeColor || '#2563eb',
      height: this.normalizeHeight(Number(msg.height)),
      pikachuEnabled: msg.pikachuCompanion,
      root: root,
      actions: new Map(),
      targetPosition: new THREE.Vector3(msg.x, this.getGroundHeightAt(Number(msg.x), Number(msg.z)) + this.groundLift, msg.z),
      targetRotationY: msg.rotY,
      currentAnimation: msg.animation || 'idle',
      highDetailActive: false
    };

    this.createFallbackOtherPlayerAvatar(otherPlayer);
    this.otherPlayers.set(username, otherPlayer);
    this.updateRemoteDetailBudget(true);
  }

  private loadOtherPlayerModel(p: OtherPlayerNPC) {
    if (p.fullModelLoaded || p.fullModelLoading) return;
    p.fullModelLoading = true;
    p.characterId = this.normalizeCharacterId(p.characterId);
    p.height = this.normalizeHeight(Number(p.height));
    const option = this.characterOptions.find((item) => item.id === p.characterId) || this.characterOptions[0];
    const loader = this.createGltfLoader(this.remoteAvatarLoadingManager);
    loader.load(
      option.path,
      (gltf) => {
        if (!this.otherPlayers.has(p.username)) {
          gltf.scene.clear();
          return;
        }

        if (p.modelGroup) {
          p.root.remove(p.modelGroup);
        }

        const model = gltf.scene;
        model.name = 'AnimatedOtherPlayerAsset';
        model.scale.setScalar(option.scale);
        model.position.set(0, option.yOffset, 0);
        model.rotation.y = option.rotationY;
        model.traverse((child) => {
          const mesh = child as THREE.Mesh;
          if (mesh.isMesh) {
            mesh.castShadow = true;
            mesh.receiveShadow = true;
          }
        });

        this.applyOtherPlayerCustomizations(model, p);
        this.normalizeVisibleCharacterHeight(model, 1.72 * p.height);

        p.root.add(model);
        p.modelGroup = model;
        p.mixer = new THREE.AnimationMixer(model);
        p.fullModelLoaded = true;
        p.fullModelLoading = false;
        p.fallbackGroup && (p.fallbackGroup.visible = false);
        p.modelGroup.visible = p.highDetailActive !== false;
        p.actions.clear();

        gltf.animations.forEach((clip) => {
          const action = p.mixer!.clipAction(clip);
          p.actions.set(clip.name.toLowerCase(), action);
        });

        this.setOtherPlayerAnimation(p, p.currentAnimation, 0);
      },
      undefined,
      (error) => {
        p.fullModelLoading = false;
        console.warn('No se pudo cargar el player GLB de ' + p.username, error);
      }
    );
  }

  private applyOtherPlayerCustomizations(model: THREE.Group, p: OtherPlayerNPC) {
    const skinHex = p.skinColor;
    const hairHex = p.hairColor;
    const eyeHex = p.eyeColor;
    const heightFactor = p.height;

    const option = this.characterOptions.find(o => o.id === p.characterId) || this.characterOptions[0];
    model.scale.setScalar(option.scale * heightFactor);
    this.tintCharacterParts(model, { skinHex, hairHex, eyeHex });
  }

  private tintCharacterParts(
    model: THREE.Group,
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
        child.material = child.material.map((mat: any) => this.cloneTintableMaterial(mat));
      } else {
        child.material = this.cloneTintableMaterial(child.material);
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

  private createFallbackOtherPlayerAvatar(p: OtherPlayerNPC) {
    if (p.fallbackGroup) {
      p.fallbackGroup.visible = !p.modelGroup?.visible;
      return;
    }
    const fallback = new THREE.Group();
    p.fallbackGroup = fallback;
    p.root.add(fallback);

    const bodyMaterial = new THREE.MeshStandardMaterial({ color: 0x64748b, roughness: 0.42, metalness: 0.12 });
    const jacketMaterial = new THREE.MeshStandardMaterial({ color: 0xe2e8f0, roughness: 0.38 });
    const capMaterial = new THREE.MeshStandardMaterial({ color: 0x94a3b8, roughness: 0.35 });
    const darkMaterial = new THREE.MeshStandardMaterial({ color: 0x334155, roughness: 0.5 });
    this.disposable.push(bodyMaterial, jacketMaterial, capMaterial, darkMaterial);

    const body = new THREE.Mesh(new THREE.CapsuleGeometry(0.38, 0.78, 8, 18), bodyMaterial);
    body.position.y = 1.05;
    body.castShadow = true;
    fallback.add(body);
    this.disposable.push(body.geometry);

    const chest = new THREE.Mesh(new THREE.BoxGeometry(0.86, 0.5, 0.18), jacketMaterial);
    chest.position.set(0, 1.16, 0.28);
    chest.castShadow = true;
    fallback.add(chest);
    this.disposable.push(chest.geometry);

    const head = new THREE.Mesh(new THREE.SphereGeometry(0.28, 24, 18), new THREE.MeshStandardMaterial({ color: p.skinColor || '#ffd2a6', roughness: 0.52 }));
    head.position.y = 1.78;
    head.castShadow = true;
    fallback.add(head);
    this.disposable.push(head.geometry, head.material as THREE.Material);

    const cap = new THREE.Mesh(new THREE.SphereGeometry(0.31, 24, 12, 0, Math.PI * 2, 0, Math.PI / 2), capMaterial);
    cap.position.y = 1.89;
    cap.rotation.x = -0.08;
    cap.castShadow = true;
    fallback.add(cap);
    this.disposable.push(cap.geometry);

    const brim = new THREE.Mesh(new THREE.BoxGeometry(0.44, 0.06, 0.28), capMaterial);
    brim.position.set(0, 1.87, 0.24);
    brim.castShadow = true;
    fallback.add(brim);
    this.disposable.push(brim.geometry);

    const backpack = new THREE.Mesh(new THREE.BoxGeometry(0.55, 0.72, 0.2), darkMaterial);
    backpack.position.set(0, 1.12, -0.35);
    backpack.castShadow = true;
    fallback.add(backpack);
    this.disposable.push(backpack.geometry);

    const legGeometry = new THREE.CapsuleGeometry(0.12, 0.48, 6, 10);
    const leftLeg = new THREE.Mesh(legGeometry, darkMaterial);
    leftLeg.position.set(-0.17, 0.42, 0);
    leftLeg.castShadow = true;
    fallback.add(leftLeg);
    const rightLeg = leftLeg.clone();
    rightLeg.position.x = 0.17;
    fallback.add(rightLeg);
    this.disposable.push(legGeometry);
  }

  private createOtherPlayerPikachu(p: OtherPlayerNPC) {
    if (!this.scene || p.pikachu) return;

    const root = new THREE.Group();
    root.position.copy(p.root.position).add(new THREE.Vector3(1.2, 0, 1.8));
    this.scene.add(root);
    const companion: CampusNpc = { root, actions: new Map() };
    p.pikachu = companion;

    this.loadSceneAsset('/models-optimized/characters/pikachu.glb', (model, animations) => {
      if (!this.otherPlayers.has(p.username)) {
        model.clear();
        this.scene?.remove(root);
        return;
      }
      model.scale.setScalar(0.42);
      model.rotation.y = Math.PI;
      this.centerModelPivot(model);
      this.alignModelBottom(model, 0);
      root.add(model);
      companion.mixer = new THREE.AnimationMixer(model);
      animations.forEach((clip) => companion.actions.set(clip.name.toLowerCase(), companion.mixer!.clipAction(clip)));
      this.setNpcAnimation(companion, ['idle'], 0);
    }, { manager: this.remoteAvatarLoadingManager });
  }

  private updateOtherPlayerState(msg: any) {
    const username = msg.username;
    const p = this.otherPlayers.get(username);
    if (!p) {
      this.registerOtherPlayer(msg);
      return;
    }

    p.targetPosition.set(msg.x, this.getGroundHeightAt(Number(msg.x), Number(msg.z)) + this.groundLift, msg.z);
    p.targetRotationY = msg.rotY;

    const needsReload = 
      p.characterId !== msg.characterId ||
      p.skinColor !== msg.skinColor ||
      p.hairColor !== msg.hairColor ||
      p.eyeColor !== msg.eyeColor ||
      p.height !== msg.height;

    if (needsReload) {
      p.characterId = this.normalizeCharacterId(msg.characterId);
      p.skinColor = msg.skinColor || '#ffe0bd';
      p.hairColor = msg.hairColor || '#5c4033';
      p.eyeColor = msg.eyeColor || '#2563eb';
      p.height = this.normalizeHeight(Number(msg.height));
      if (p.modelGroup) {
        p.root.remove(p.modelGroup);
        p.modelGroup = undefined;
      }
      p.mixer?.stopAllAction();
      p.mixer = undefined;
      p.actions.clear();
      p.active = undefined;
      p.fullModelLoaded = false;
      p.fullModelLoading = false;
      this.createFallbackOtherPlayerAvatar(p);
      this.updateRemoteDetailBudget(true);
    }

    if (p.pikachuEnabled !== msg.pikachuCompanion) {
      p.pikachuEnabled = msg.pikachuCompanion;
      if (p.pikachuEnabled && this.shouldAllowRemoteCompanion(p)) {
        this.createOtherPlayerPikachu(p);
      } else if (p.pikachu) {
        this.scene?.remove(p.pikachu.root);
        p.pikachu.mixer?.stopAllAction();
        p.pikachu = undefined;
      }
    }

    if (p.currentAnimation !== msg.animation) {
      p.currentAnimation = msg.animation || 'idle';
      this.setOtherPlayerAnimation(p, p.currentAnimation);
    }
  }

  private setOtherPlayerAnimation(p: OtherPlayerNPC, preferred: 'idle' | 'walking' | 'running', fade = 0.22) {
    if (!p.highDetailActive) return;
    if (p.isPlayingEmote) {
      if (preferred === 'idle') {
        return;
      } else {
        p.isPlayingEmote = false;
      }
    }
    if (p.actions.size === 0) return;

    const option = this.characterOptions.find((o) => o.id === p.characterId) || this.characterOptions[0];
    const candidates: Record<typeof preferred, string[]> = {
      idle: option.idleHints ?? ['idle', 'standing'],
      walking: option.walkHints ?? ['walking', 'walk'],
      running: option.runHints ?? ['running', 'run']
    };

    let action: THREE.AnimationAction | undefined;
    for (const name of candidates[preferred]) {
      const exact = p.actions.get(name);
      if (exact) {
        action = exact;
        break;
      }
    }

    if (!action) {
      action = Array.from(p.actions.entries())
        .find(([name]) => candidates[preferred].some((candidate) => name.includes(candidate)))?.[1];
    }

    if (!action || action === p.active) return;
    action.reset().fadeIn(fade).play();
    p.active?.fadeOut(fade);
    p.active = action;
  }

  private getRemoteFullModelBudget(): number {
    if (this.graphicsQuality === 'low') return this.isMobileViewport ? 1 : 3;
    if (this.adaptivePixelRatioScale < 0.82) return this.isMobileViewport ? 1 : 2;
    if (this.graphicsQuality === 'medium') return this.isMobileViewport ? 2 : 5;
    return this.isMobileViewport ? 3 : 8;
  }

  private getRemoteDistanceSq(p: OtherPlayerNPC): number {
    const dx = p.root.position.x - this.player.position.x;
    const dz = p.root.position.z - this.player.position.z;
    return dx * dx + dz * dz;
  }

  private shouldAllowRemoteCompanion(p: OtherPlayerNPC): boolean {
    if (!p.pikachuEnabled) return false;
    if (this.graphicsQuality === 'low' || this.adaptivePixelRatioScale < 0.86) return false;
    if (this.otherPlayers.size > (this.isMobileViewport ? 3 : 6)) return false;
    return this.getRemoteDistanceSq(p) < this.remoteCompanionDistanceSq;
  }

  private updateRemoteDetailBudget(force = false) {
    if (!force && this.frameCounter % 60 !== 0) return;

    const budget = this.getRemoteFullModelBudget();
    const ranked = Array.from(this.otherPlayers.values())
      .map((player) => ({ player, distanceSq: this.getRemoteDistanceSq(player) }))
      .sort((a, b) => a.distanceSq - b.distanceSq);

    const highDetail = new Set(
      ranked
        .filter((entry) => entry.distanceSq <= this.remoteFullModelDistanceSq)
        .slice(0, budget)
        .map((entry) => entry.player.username)
    );

    ranked.forEach(({ player }) => {
      const enabled = highDetail.has(player.username);
      player.highDetailActive = enabled;
      if (player.modelGroup) player.modelGroup.visible = enabled;
      if (player.fallbackGroup) player.fallbackGroup.visible = !enabled;

      if (enabled && !player.fullModelLoaded && !player.fullModelLoading) {
        this.loadOtherPlayerModel(player);
      }

      if (!enabled) {
        player.mixer?.stopAllAction();
        player.active = undefined;
      } else if (player.fullModelLoaded) {
        this.setOtherPlayerAnimation(player, player.currentAnimation, 0.12);
      }

      if (this.shouldAllowRemoteCompanion(player)) {
        this.createOtherPlayerPikachu(player);
      } else if (player.pikachu) {
        this.scene?.remove(player.pikachu.root);
        player.pikachu.mixer?.stopAllAction();
        player.pikachu = undefined;
      }
    });
  }

  private removeOtherPlayer(username: string) {
    const p = this.otherPlayers.get(username);
    if (!p) return;

    console.log('Removiendo jugador online del lobby:', username);

    if (p.pikachu) {
      this.scene?.remove(p.pikachu.root);
      p.pikachu.mixer?.stopAllAction();
    }

    this.scene?.remove(p.root);
    p.mixer?.stopAllAction();
    this.otherPlayers.delete(username);
  }

  private updateOtherPlayers(delta: number) {
    if (!this.camera) return;

    this.otherPlayers.forEach((p) => {
      const dx = p.root.position.x - this.camera!.position.x;
      const dy = p.root.position.y - this.camera!.position.y;
      const dz = p.root.position.z - this.camera!.position.z;
      const distanceSqToCamera = dx * dx + dy * dy + dz * dz;
      const farAway = distanceSqToCamera > 46 * 46;
      const veryFar = distanceSqToCamera > 70 * 70;
      const skipAnimationFrame = veryFar ? this.frameCounter % 6 !== 0 : farAway && this.frameCounter % 3 !== 0;
      if (p.highDetailActive && !skipAnimationFrame) {
        p.mixer?.update(delta * (veryFar ? 6 : farAway ? 3 : 1));
      }

      const lerpFactor = 1 - Math.pow(0.0001, delta);
      p.root.position.lerp(p.targetPosition, lerpFactor);

      let diff = p.targetRotationY - p.root.rotation.y;
      while (diff < -Math.PI) diff += Math.PI * 2;
      while (diff > Math.PI) diff -= Math.PI * 2;
      p.root.rotation.y += diff * lerpFactor;

      if (p.highDetailActive && !skipAnimationFrame) {
        this.updateOtherPlayerPikachuAI(p, delta * (veryFar ? 6 : farAway ? 3 : 1));
      }

      if ((farAway || this.adaptivePixelRatioScale < 0.9) && this.frameCounter % 2 !== 0) {
        return;
      }

      this.tempVec3.copy(p.root.position);
      this.tempVec3.y += p.height * 2.25;
      const projected = this.tempVec3.project(this.camera!);

      const isBehindCamera = projected.z > 1.0;
      if (isBehindCamera) {
        p.screenX = undefined;
        p.screenY = undefined;
        this.syncOtherPlayerOverlay(p, false);
      } else {
        p.screenX = (projected.x * 0.5 + 0.5) * 100;
        p.screenY = (-projected.y * 0.5 + 0.5) * 100;
        this.syncOtherPlayerOverlay(p, true);
      }
    });
  }

  private syncOtherPlayerOverlay(player: OtherPlayerNPC, visible: boolean) {
    const selectorName = this.escapeCssValue(player.username);
    const tag = document.querySelector<HTMLElement>(`[data-player-tag="${selectorName}"]`);
    const bubble = document.querySelector<HTMLElement>(`[data-player-bubble="${selectorName}"]`);

    [tag, bubble].forEach((element) => {
      if (!element) return;
      const shouldShow = visible && player.screenX !== undefined && player.screenY !== undefined && (element === tag || !!player.chatBubble);
      if (!shouldShow) {
        element.classList.remove('visible');
        element.style.display = 'none';
        return;
      }

      element.style.display = 'block';
      element.style.left = `${player.screenX}%`;
      element.style.top = `${player.screenY}%`;
      element.classList.add('visible');
    });
  }

  private escapeCssValue(value: string): string {
    if (typeof CSS !== 'undefined' && CSS.escape) {
      return CSS.escape(value);
    }
    return value.replace(/["\\]/g, '\\$&');
  }

  private updateOtherPlayerPikachuAI(p: OtherPlayerNPC, delta: number) {
    if (!p.pikachu) return;

    const root = p.pikachu.root;
    p.pikachu.mixer?.update(delta);

    if (!root.userData['initialized']) {
      root.userData['initialized'] = true;
      root.userData['state'] = 'following';
      root.userData['targetPos'] = root.position.clone();
      root.userData['idleTimer'] = 0;
      root.userData['isMoving'] = false;
    }

    const distToPlayer = root.position.distanceTo(p.root.position);
    
    if (distToPlayer > 12.0) {
      const behind = new THREE.Vector3(Math.sin(p.root.rotation.y + 0.65), 0, Math.cos(p.root.rotation.y + 0.65));
      const tpTarget = p.root.position.clone().addScaledVector(behind, 1.85);
      root.position.copy(tpTarget);
      root.userData['targetPos'] = tpTarget.clone();
      root.userData['state'] = 'following';
      root.userData['isMoving'] = false;
    }

    const isPlayerMoving = p.currentAnimation !== 'idle';

    let target = root.userData['targetPos'] as THREE.Vector3;
    let state = root.userData['state'] as string;
    let idleTimer = root.userData['idleTimer'] as number;

    if (distToPlayer > 5.5 || isPlayerMoving) {
      state = 'following';
      idleTimer = 0;
    } else if (state === 'following' && distToPlayer <= 2.2) {
      state = 'idle';
      idleTimer = 1.5 + Math.random() * 2.0;
    }

    let speed = 0;

    if (state === 'following') {
      const behind = new THREE.Vector3(
        Math.sin(p.root.rotation.y + 0.65),
        0,
        Math.cos(p.root.rotation.y + 0.65)
      );
      target = p.root.position.clone().addScaledVector(behind, 1.85);
      speed = p.currentAnimation === 'running' ? 7.6 : 4.4;
    } else if (state === 'idle') {
      speed = 0;
      target = root.position.clone();
      
      if (idleTimer > 0) {
        idleTimer -= delta;
      } else {
        const angle = Math.random() * Math.PI * 2;
        const radius = 1.0 + Math.random() * 1.8;
        target = p.root.position.clone().add(new THREE.Vector3(
          Math.cos(angle) * radius,
          0,
          Math.sin(angle) * radius
        ));
        state = 'wandering';
      }
    } else if (state === 'wandering') {
      speed = 1.8;
      
      const distanceToWanderTarget = root.position.distanceTo(target);
      if (distanceToWanderTarget < 0.25 || distToPlayer > 3.4) {
        state = 'idle';
        idleTimer = 2.5 + Math.random() * 3.5;
        target = root.position.clone();
      }
    }

    root.userData['state'] = state;
    target.y = this.getGroundHeightAt(target.x, target.z) + this.groundLift;
    root.userData['targetPos'] = target;
    root.userData['idleTimer'] = idleTimer;

    const distToTarget = root.position.distanceTo(target);
    let isMoving = root.userData['isMoving'] || false;
    if (distToTarget > 0.55) {
      isMoving = true;
    }
    if (distToTarget < 0.18) {
      isMoving = false;
    }
    root.userData['isMoving'] = isMoving;
    root.position.y += (target.y - root.position.y) * (1 - Math.pow(0.0001, delta));

    if (isMoving && speed > 0) {
      const moveDir = target.clone().sub(root.position);
      moveDir.y = 0;
      moveDir.normalize();

      const targetRotation = Math.atan2(moveDir.x, moveDir.z) + Math.PI;
      
      let diff = targetRotation - root.rotation.y;
      while (diff < -Math.PI) diff += Math.PI * 2;
      while (diff > Math.PI) diff -= Math.PI * 2;
      root.rotation.y += diff * (1 - Math.pow(0.0001, delta));

      const step = speed * delta;
      if (distToTarget > step) {
        root.position.addScaledVector(moveDir, step);
      } else {
        root.position.copy(target);
        isMoving = false;
        root.userData['isMoving'] = false;
      }

      this.setNpcAnimation(p.pikachu!, speed > 4.8 ? ['run', 'running', 'walk', 'walking'] : ['walk', 'walking']);
    } else {
      const lookDir = p.root.position.clone().sub(root.position);
      lookDir.y = 0;
      if (lookDir.lengthSq() > 0.05) {
        lookDir.normalize();
        const targetRotation = Math.atan2(lookDir.x, lookDir.z) + Math.PI;
        let diff = targetRotation - root.rotation.y;
        while (diff < -Math.PI) diff += Math.PI * 2;
        while (diff > Math.PI) diff -= Math.PI * 2;
        root.rotation.y += diff * (1 - Math.pow(0.002, delta));
      }
      this.setNpcAnimation(p.pikachu!, ['idle']);
    }
  }

  private isTypingTarget(target: EventTarget | null): boolean {
    const element = target as HTMLElement | null;
    if (!element) return false;
    return ['INPUT', 'TEXTAREA', 'SELECT', 'BUTTON'].includes(element.tagName);
  }

  private sincronizarCartaAReemplazar() {
    const mazo = this.debugMazoSeleccionado;
    if (!mazo || !mazo.cartas?.length) {
      this.debugReplaceCardId = null;
      return;
    }

    const yaExiste = this.debugReplaceCardId
      ? mazo.cartas.some((carta) => carta.id === this.debugReplaceCardId)
      : false;

    if (!yaExiste) {
      this.debugReplaceCardId = mazo.cartas[0].id;
    }
  }

  // ================= MÃ‰TODOS DE CHAT DEL LOBBY =================

  toggleChat() {
    this.ngZone.run(() => {
      if (!this.chatActive) {
        this.chatActive = true;
        this.keys.clear(); // Limpiar teclas activas para no moverse mientras se chatea
        this.cdr.detectChanges();
        setTimeout(() => {
          const inputEl = document.querySelector('.chat-input-wrapper input') as HTMLInputElement;
          if (inputEl) {
            inputEl.focus();
          }
        }, 50);
      } else {
        this.sendChatMessage();
      }
    });
  }

  closeChat() {
    this.ngZone.run(() => {
      this.chatActive = false;
      this.chatText = '';
      this.cdr.detectChanges();
      const inputEl = document.querySelector('.chat-input-wrapper input') as HTMLInputElement;
      if (inputEl) {
        inputEl.blur();
      }
    });
  }

  sendChatMessage() {
    if (!this.chatText.trim()) {
      this.closeChat();
      return;
    }

    const textToSend = this.chatText.trim();

    // Agregar localmente
    this.addChatMessage(this.jugador?.username || 'PLAYER', textToSend);

    // Mostrar burbuja arriba de nuestra cabeza
    this.showLocalSpeechBubble(textToSend);

    // Enviar por WebSocket
    if (this.socket && this.socket.readyState === WebSocket.OPEN && this.jugador?.username) {
      this.socket.send(JSON.stringify({
        type: 'CHAT',
        username: this.jugador.username,
        text: textToSend
      }));
    }

    this.chatText = '';
    this.closeChat();
  }

  showLocalSpeechBubble(text: string) {
    if (this.localBubbleTimeout) {
      clearTimeout(this.localBubbleTimeout);
    }
    this.localChatBubble = text;
    this.localBubbleTimeout = setTimeout(() => {
      this.localChatBubble = null;
      this.cdr.detectChanges();
    }, 5000);
  }

  addChatMessage(sender: string, text: string, system = false) {
    this.chatLog.push({ sender, text, system });
    if (this.chatLog.length > 50) {
      this.chatLog.shift();
    }
    this.cdr.detectChanges();

    // Scroll al final
    setTimeout(() => {
      const el = document.querySelector('.chat-messages');
      if (el) {
        el.scrollTop = el.scrollHeight;
      }
    }, 50);
  }

  showOtherPlayerSpeechBubble(p: OtherPlayerNPC, text: string) {
    if (p.chatBubbleTimeout) {
      clearTimeout(p.chatBubbleTimeout);
    }
    p.chatBubble = text;
    p.chatBubbleTimeout = setTimeout(() => {
      p.chatBubble = undefined;
      p.chatBubbleTimeout = undefined;
      this.cdr.detectChanges();
    }, 5000);
    this.cdr.detectChanges();
  }

  onChatInputBlur() {
    setTimeout(() => {
      if (this.chatActive) {
        this.closeChat();
      }
    }, 100);
  }

  private updateLocalPlayerBubbleProjection() {
    if (!this.camera) return;
    if (this.localChatBubble) {
      const scale = this.currentCharacterOption?.scale || 1.0;
      const heightStr = localStorage.getItem('lobbyHeight') || '1.0';
      const heightFactor = parseFloat(heightStr);
      const headPos = this.player.position.clone().add(new THREE.Vector3(0, scale * heightFactor * 2.25, 0));
      const projected = headPos.project(this.camera);
      const isBehindCamera = projected.z > 1.0;
      if (isBehindCamera) {
        this.localBubbleScreenX = undefined;
        this.localBubbleScreenY = undefined;
      } else {
        this.localBubbleScreenX = (projected.x * 0.5 + 0.5) * 100;
        this.localBubbleScreenY = (-projected.y * 0.5 + 0.5) * 100;
      }
    } else {
      this.localBubbleScreenX = undefined;
      this.localBubbleScreenY = undefined;
    }
  }

  // ================= MÃ‰TODOS DE EMOTES =================

  @HostListener('window:mousedown', ['$event'])
  onMouseDown(event: MouseEvent) {
    this.unlockLobbyAudio();
    if (event.button === 1) { // Ruedita del mouse
      event.preventDefault();
      this.toggleEmoteMenu();
    }
  }

  @HostListener('window:auxclick', ['$event'])
  onAuxClick(event: MouseEvent) {
    if (event.button === 1) {
      event.preventDefault();
    }
  }

  getAvailableEmotes(): string[] {
    const list = Array.from(this.playerActions.keys());
    return list.filter(name => !/idle|walk|run|standing/i.test(name));
  }

  toggleEmoteMenu() {
    this.ngZone.run(() => {
      this.showEmoteMenu = !this.showEmoteMenu;
      if (this.showEmoteMenu) {
        this.keys.clear(); // Limpiar movimiento al abrir el menÃº de gestos
      }
      this.cdr.detectChanges();
    });
  }

  closeEmoteMenu() {
    this.ngZone.run(() => {
      this.showEmoteMenu = false;
      this.cdr.detectChanges();
    });
  }

  triggerEmote(emoteName: string) {
    this.closeEmoteMenu();
    this.playLocalEmote(emoteName);

    // Enviar a otros
    if (this.socket && this.socket.readyState === WebSocket.OPEN && this.jugador?.username) {
      this.socket.send(JSON.stringify({
        type: 'EMOTE',
        username: this.jugador.username,
        emote: emoteName
      }));
    }
  }

  playLocalEmote(emoteName: string) {
    const action = this.playerActions.get(emoteName);
    if (!action) return;

    this.isPlayingEmote = true;
    this.emoteAnimationName = emoteName;

    if (this.activePlayerAction) {
      this.activePlayerAction.fadeOut(0.15);
    }

    action.reset();
    const isLoopable = ['dance', 'dancing', 'pose', 'idle_hints', 'talking'].some(word => emoteName.includes(word));
    if (!isLoopable) {
      action.setLoop(THREE.LoopOnce, 1);
      action.clampWhenFinished = true;

      const onFinished = (e: any) => {
        if (e.action === action) {
          this.playerMixer?.removeEventListener('finished', onFinished);
          this.isPlayingEmote = false;
          this.emoteAnimationName = undefined;
          this.setPlayerAnimation('idle', 0.25);
        }
      };
      this.playerMixer?.addEventListener('finished', onFinished);
    } else {
      action.setLoop(THREE.LoopRepeat, Number.POSITIVE_INFINITY);
    }

    action.fadeIn(0.15).play();
    this.activePlayerAction = action;
  }

  playOtherPlayerEmote(p: OtherPlayerNPC, emoteName: string) {
    const action = p.actions.get(emoteName);
    if (!action) return;

    if (p.active) {
      p.active.fadeOut(0.15);
    }

    p.isPlayingEmote = true;
    action.reset();
    const isLoopable = ['dance', 'dancing', 'pose', 'idle_hints', 'talking'].some(word => emoteName.includes(word));
    if (!isLoopable) {
      action.setLoop(THREE.LoopOnce, 1);
      action.clampWhenFinished = true;

      const onFinished = (e: any) => {
        if (e.action === action) {
          p.mixer?.removeEventListener('finished', onFinished);
          p.isPlayingEmote = false;
          this.setOtherPlayerAnimation(p, p.currentAnimation, 0.25);
        }
      };
      p.mixer?.addEventListener('finished', onFinished);
    } else {
      action.setLoop(THREE.LoopRepeat, Number.POSITIVE_INFINITY);
    }

    action.fadeIn(0.15).play();
    p.active = action;
  }

  // ================= INTERACTION MENU & CHAT STUBS =================
  openPlayerMenu(event: MouseEvent, player: OtherPlayerNPC) {
    event.stopPropagation();
    event.preventDefault();
    this.selectedPlayerForMenu = player;
    this.playerMenuX = event.clientX;
    this.playerMenuY = event.clientY;
    this.cdr.detectChanges();
  }

  @HostListener('document:click', ['$event'])
  onDocumentClick(event: MouseEvent) {
    this.selectedPlayerForMenu = null;
  }

  agregarAmigo(username: string) {
    this.selectedPlayerForMenu = null;
    this.chatLog.push({ sender: 'SISTEMA', text: `Solicitud de amistad enviada a ${username} (funciÃ³n en base de datos pendiente).`, system: true });
    this.cdr.detectChanges();
  }

  // ================= DUEL CHALLENGE SYSTEM =================
  async retarADuelo(username: string) {
    this.selectedPlayerForMenu = null;
    const mazoId = await this.ensureBattleDeckSelected(true);
    if (!mazoId) {
      this.chatLog.push({ sender: 'SISTEMA', text: 'Primero crea o completa un mazo de 60 cartas para retar a duelo.', system: true });
      this.irAlDeckBuilder();
      this.cdr.detectChanges();
      return;
    }

    this.waitingForChallengeResponse = true;
    this.challengedUsername = username;
    this.chatLog.push({ sender: 'SISTEMA', text: `Has retado a ${username} a un duelo.`, system: true });

    this.socket?.send(JSON.stringify({
      type: 'CHALLENGE_DUEL',
      username: this.jugador?.username,
      targetUsername: username
    }));

    this.startChallengeTimer(() => {
      this.cancelarRetoDuelo();
      this.chatLog.push({ sender: 'SISTEMA', text: `El oponente ${username} no respondiÃ³ el reto a duelo.`, system: true });
    });
    this.cdr.detectChanges();
  }

  cancelarRetoDuelo() {
    this.waitingForChallengeResponse = false;
    this.stopChallengeTimer();
    this.socket?.send(JSON.stringify({
      type: 'CHALLENGE_DUEL_RESPONSE',
      username: this.jugador?.username,
      targetUsername: this.challengedUsername,
      accepted: false
    }));
    this.cdr.detectChanges();
  }

  handleIncomingChallenge(msg: any) {
    this.incomingChallenge = msg;
    this.startChallengeTimer(() => {
      this.responderDuelo(false);
    });
    this.cdr.detectChanges();
  }

  async responderDuelo(accepted: boolean) {
    const challenger = this.incomingChallenge?.username;
    this.incomingChallenge = null;
    this.stopChallengeTimer();
    const mazoId = accepted ? await this.ensureBattleDeckSelected(true) : null;

    if (accepted && !mazoId) {
      this.chatLog.push({ sender: 'SISTEMA', text: 'ElegÃ­ un mazo sincronizado antes de aceptar el duelo.', system: true });
      accepted = false;
    }

    this.socket?.send(JSON.stringify({
      type: 'CHALLENGE_DUEL_RESPONSE',
      username: this.jugador?.username,
      targetUsername: challenger,
      accepted: accepted,
      details: mazoId ? mazoId.toString() : ''
    }));
    this.cdr.detectChanges();
  }

  async handleChallengeResponse(msg: any) {
    this.waitingForChallengeResponse = false;
    this.stopChallengeTimer();

    if (msg.accepted) {
      this.chatLog.push({ sender: 'SISTEMA', text: `${msg.username} aceptÃ³ el combate. Iniciando...`, system: true });
      this.cdr.detectChanges();

      const myMazoId = await this.ensureBattleDeckSelected(true);
      const p2MazoId = parseInt(msg.details || '0');
      if (!myMazoId) {
        this.chatLog.push({ sender: 'SISTEMA', text: `Error: No tienes mazo seleccionado.`, system: true });
        this.cdr.detectChanges();
        return;
      }
      if (!p2MazoId) {
        this.chatLog.push({ sender: 'SISTEMA', text: `${msg.username} no tiene mazo seleccionado.`, system: true });
        this.cdr.detectChanges();
        return;
      }

      this.battleService.startBattleOnline(
        this.jugador!.username,
        myMazoId,
        msg.username,
        p2MazoId
      ).subscribe({
        next: (partida) => {
          this.socket?.send(JSON.stringify({
            type: 'BATTLE_START',
            username: this.jugador?.username,
            targetUsername: msg.username,
            details: partida.id
          }));
          this.router.navigate(['/battle', partida.id]);
        },
        error: (err) => {
          console.error('Error al arrancar batalla online:', err);
          const backendMessage = typeof err.error === 'string' ? err.error : err.message;
          alert(this.i18n.translate('alert.startOnlineBattleError', { error: backendMessage }));
        }
      });
    } else {
      this.chatLog.push({ sender: 'SISTEMA', text: `${msg.username} rechazÃ³ el duelo o la invitaciÃ³n expirÃ³.`, system: true });
      this.cdr.detectChanges();
    }
  }

  handleBattleStart(msg: any) {
    const partidaId = msg.details;
    if (!partidaId) {
      this.chatLog.push({ sender: 'SISTEMA', text: 'LlegÃ³ inicio de batalla sin ID de partida.', system: true });
      this.cdr.detectChanges();
      return;
    }
    this.router.navigate(['/battle', partidaId]);
  }

  startChallengeTimer(onTimeout: () => void) {
    this.stopChallengeTimer();
    this.challengeSecondsLeft = 10;
    this.challengeTimerDashoffset = 0;
    this.challengeTimerInterval = setInterval(() => {
      this.challengeSecondsLeft--;
      this.challengeTimerDashoffset = (10 - this.challengeSecondsLeft) * 10;
      this.cdr.detectChanges();
      if (this.challengeSecondsLeft <= 0) {
        this.stopChallengeTimer();
        onTimeout();
      }
    }, 1000);
  }

  stopChallengeTimer() {
    if (this.challengeTimerInterval) {
      clearInterval(this.challengeTimerInterval);
      this.challengeTimerInterval = null;
    }
  }

  // ================= CARD TRADING SYSTEM =================
  invitarIntercambio(username: string) {
    this.selectedPlayerForMenu = null;
    this.waitingForTradeResponse = true;
    this.tradeInvitedUsername = username;
    this.chatLog.push({ sender: 'SISTEMA', text: `InvitaciÃ³n de intercambio enviada a ${username}.`, system: true });

    this.socket?.send(JSON.stringify({
      type: 'INVITE_TRADE',
      username: this.jugador?.username,
      targetUsername: username
    }));

    this.startChallengeTimer(() => {
      this.cancelarTradeInvite();
      this.chatLog.push({ sender: 'SISTEMA', text: `${username} no respondiÃ³ a la propuesta de intercambio.`, system: true });
    });
    this.cdr.detectChanges();
  }

  cancelarTradeInvite() {
    this.waitingForTradeResponse = false;
    this.stopChallengeTimer();
    this.socket?.send(JSON.stringify({
      type: 'INVITE_TRADE_RESPONSE',
      username: this.jugador?.username,
      targetUsername: this.tradeInvitedUsername,
      accepted: false
    }));
    this.cdr.detectChanges();
  }

  handleIncomingTradeInvite(msg: any) {
    this.incomingTradeInvite = msg;
    this.startChallengeTimer(() => {
      this.responderTrade(false);
    });
    this.cdr.detectChanges();
  }

  responderTrade(accepted: boolean) {
    const challenger = this.incomingTradeInvite?.username;
    this.incomingTradeInvite = null;
    this.stopChallengeTimer();

    this.socket?.send(JSON.stringify({
      type: 'INVITE_TRADE_RESPONSE',
      username: this.jugador?.username,
      targetUsername: challenger,
      accepted: accepted
    }));

    if (accepted) {
      this.abrirSalaTrading(challenger);
    }
    this.cdr.detectChanges();
  }

  handleTradeInviteResponse(msg: any) {
    this.waitingForTradeResponse = false;
    this.stopChallengeTimer();

    if (msg.accepted) {
      this.abrirSalaTrading(msg.username);
    } else {
      this.chatLog.push({ sender: 'SISTEMA', text: `${msg.username} rechazÃ³ la invitaciÃ³n de intercambio.`, system: true });
      this.cdr.detectChanges();
    }
  }

  abrirSalaTrading(opponentUsername: string) {
    this.activeTradeSession = true;
    this.tradeOpponentUsername = opponentUsername;
    this.tradeLeftCards = [];
    this.tradeRightCards = [];
    this.tradeLeftReady = false;
    this.tradeRightReady = false;
    this.tradeCollectionSearchText = '';
    this.tradeCollectionFilterRarity = '';
    this.tradeCollectionFilterType = '';
    this.tradeCollectionFilterSupertype = '';
    this.tradeCollectionFilterSubtype = '';
    this.tradeShowValueWarning = false;
    this.tradeShowContinuationPrompt = false;
    this.tradeWaitingForContinuation = false;
    this.filtrarColeccionTrade();
    this.precargarColeccionTrade(!this.tradeCollectionLoaded || this.userTradeCollection.length === 0);
    this.cdr.detectChanges();
  }

  cerrarTradeSala() {
    this.activeTradeSession = false;
    this.socket?.send(JSON.stringify({
      type: 'TRADE_CLOSE',
      username: this.jugador?.username,
      targetUsername: this.tradeOpponentUsername
    }));
    this.chatLog.push({ sender: 'SISTEMA', text: `Se ha cerrado la sala de intercambio.`, system: true });
    this.cdr.detectChanges();
  }

  getEmptySlots(length: number): number[] {
    const slots = 3 - length;
    return slots > 0 ? Array(slots).fill(0) : [];
  }

  addCardToTrade(card: Card) {
    if (this.tradeLeftReady) return;
    if (this.tradeLeftCards.length >= 3) {
      alert(this.i18n.translate('alert.maxExchangeCards'));
      return;
    }
    this.tradeLeftCards.push(card);
    this.notifyTradeUpdate();
    this.cdr.detectChanges();
  }

  removeCardFromTrade(card: Card) {
    if (this.tradeLeftReady) return;
    this.tradeLeftCards = this.tradeLeftCards.filter(c => c.id !== card.id);
    this.notifyTradeUpdate();
    this.cdr.detectChanges();
  }

  isCardInTrade(card: Card): boolean {
    return this.tradeLeftCards.some(c => c.id === card.id);
  }

  notifyTradeUpdate() {
    this.socket?.send(JSON.stringify({
      type: 'TRADE_UPDATE',
      username: this.jugador?.username,
      targetUsername: this.tradeOpponentUsername,
      accepted: this.tradeLeftReady,
      details: JSON.stringify(this.tradeLeftCards)
    }));
  }

  toggleTradeReady() {
    if (this.tradeLeftCards.length === 0) return;

    if (!this.tradeLeftReady) {
      // Check rarity value differences
      const myValue = this.tradeLeftCards.reduce((acc, card) => acc + this.getCardRarityValue(card.rarity || 'Common'), 0);
      const oppValue = this.tradeRightCards.reduce((acc, card) => acc + this.getCardRarityValue(card.rarity || 'Common'), 0);

      if (myValue > oppValue + 2) {
        this.tradeShowValueWarning = true;
        this.cdr.detectChanges();
        return;
      }
    }

    this.confirmarReady();
  }

  confirmarAdvertenciaValor(confirm: boolean) {
    this.tradeShowValueWarning = false;
    if (confirm) {
      this.confirmarReady();
    }
    this.cdr.detectChanges();
  }

  confirmarReady() {
    this.tradeLeftReady = !this.tradeLeftReady;
    this.notifyTradeUpdate();

    if (this.tradeLeftReady && this.tradeRightReady) {
      this.executeTradeTransaction();
    }
    this.cdr.detectChanges();
  }

  executeTradeTransaction() {
    const myIds = this.tradeLeftCards.map(c => c.id);
    const oppIds = this.tradeRightCards.map(c => c.id);

    this.jugadorService.ejecutarTrade(
      this.jugador!.username,
      this.tradeOpponentUsername,
      myIds,
      oppIds
    ).subscribe({
      next: () => {
        this.tradeShowContinuationPrompt = true;
        this.tradeCollectionLoaded = false;
        this.precargarColeccionTrade(true);
        this.refrescarTodo();
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error('Error al realizar el trade:', err);
        alert(this.i18n.translate('alert.exchangeFailed', { error: err.message }));
        this.tradeLeftReady = false;
        this.notifyTradeUpdate();
        this.cdr.detectChanges();
      }
    });
  }

  responderSeguirTrading(continueTrading: boolean) {
    this.tradeShowContinuationPrompt = false;
    if (continueTrading) {
      this.tradeWaitingForContinuation = true;
      this.socket?.send(JSON.stringify({
        type: 'TRADE_UPDATE',
        username: this.jugador?.username,
        targetUsername: this.tradeOpponentUsername,
        accepted: true,
        details: 'CONTINUE'
      }));
    } else {
      this.cerrarTradeSala();
    }
    this.cdr.detectChanges();
  }

  handleTradeUpdate(msg: any) {
    if (msg.details === 'CONTINUE') {
      if (this.tradeWaitingForContinuation) {
        // Both clicked continue trading! Reset trading session
        this.abrirSalaTrading(this.tradeOpponentUsername);
      } else {
        this.tradeRightReady = true;
        this.cdr.detectChanges();
      }
      return;
    }

    try {
      const oppCards = JSON.parse(msg.details || '[]');
      this.tradeRightCards = oppCards;
      this.tradeRightReady = msg.accepted;

      if (this.tradeLeftReady && this.tradeRightReady) {
        this.executeTradeTransaction();
      }
      this.cdr.detectChanges();
    } catch (e) {
      console.error('Error parsing trade update details:', e);
    }
  }

  handleTradeClose(msg: any) {
    this.activeTradeSession = false;
    this.tradeShowContinuationPrompt = false;
    this.tradeWaitingForContinuation = false;
    this.chatLog.push({ sender: 'SISTEMA', text: `${this.tradeOpponentUsername} cerrÃ³ la sala de intercambio.`, system: true });
    this.cdr.detectChanges();
  }

  filtrarColeccionTrade() {
    const search = this.tradeCollectionSearchText.trim().toLowerCase();
    this.tradeCollectionFiltrada = this.userTradeCollection.filter(c => {
      const attackText = [
        c.attacks,
        ...(c.ataques || []).flatMap((atk) => [atk.nombre, atk.texto])
      ].filter(Boolean).join(' ').toLowerCase();
      const matchSearch = !search ||
        c.nombre.toLowerCase().includes(search) ||
        c.id.toLowerCase().includes(search) ||
        attackText.includes(search);
      const matchRarity = !this.tradeCollectionFilterRarity || (c.rarity || 'Common') === this.tradeCollectionFilterRarity;
      const matchType = !this.tradeCollectionFilterType || c.tipo === this.tradeCollectionFilterType;
      const matchSupertype = !this.tradeCollectionFilterSupertype || c.supertype === this.tradeCollectionFilterSupertype;
      const matchSubtype = !this.tradeCollectionFilterSubtype || (c.subtypes || []).includes(this.tradeCollectionFilterSubtype);
      return matchSearch && matchRarity && matchType && matchSupertype && matchSubtype;
    });
  }

  limpiarFiltrosTrade() {
    this.tradeCollectionSearchText = '';
    this.tradeCollectionFilterRarity = '';
    this.tradeCollectionFilterType = '';
    this.tradeCollectionFilterSupertype = '';
    this.tradeCollectionFilterSubtype = '';
    this.filtrarColeccionTrade();
  }

  trackCardById(_index: number, card: Card): string {
    return card.id;
  }

  trackOtherPlayerByUsername(_index: number, player: OtherPlayerNPC): string {
    return player.username;
  }

  getCardRarityValue(rarity: string): number {
    if (rarity === 'Secret Rare') return 4;
    if (rarity === 'Rare') return 2;
    if (rarity === 'Uncommon') return 1;
    return 0; // Common
  }

  isMyOfferFair(): boolean {
    return this.tradeLeftCards.length > 0;
  }

  isOpponentOfferFair(): boolean {
    return this.tradeRightCards.length > 0;
  }

  getMyOfferFairnessText(): string {
    if (this.tradeLeftCards.length === 0) return this.i18n.translate('lobby.vacas');
    const myValue = this.tradeLeftCards.reduce((acc, card) => acc + this.getCardRarityValue(card.rarity || 'Common'), 0);
    const oppValue = this.tradeRightCards.reduce((acc, card) => acc + this.getCardRarityValue(card.rarity || 'Common'), 0);
    if (myValue > oppValue + 2 && this.tradeRightCards.length > 0) return this.i18n.translate('lobby.unfairOffer');
    return this.i18n.translate('lobby.fairOffer');
  }

  getOpponentOfferFairnessText(): string {
    if (this.tradeRightCards.length === 0) return this.i18n.translate('lobby.vacas');
    const myValue = this.tradeLeftCards.reduce((acc, card) => acc + this.getCardRarityValue(card.rarity || 'Common'), 0);
    const oppValue = this.tradeRightCards.reduce((acc, card) => acc + this.getCardRarityValue(card.rarity || 'Common'), 0);
    if (oppValue > myValue + 2 && this.tradeLeftCards.length > 0) return this.i18n.translate('lobby.unfairOffer');
    return this.i18n.translate('lobby.fairOffer');
  }
}


