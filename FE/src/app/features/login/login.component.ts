import { AfterViewInit, ChangeDetectorRef, Component, ElementRef, HostListener, NgZone, OnDestroy, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import * as THREE from 'three';
import { AuthService } from '../services/auth.service';
import { SoundService } from '../services/sound.service';
import { TranslatePipe } from '../i18n/translate.pipe';

@Component({
  selector: 'app-login',
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.scss'],
  standalone: true,
  imports: [CommonModule, FormsModule, TranslatePipe]
})
export class LoginComponent implements AfterViewInit, OnDestroy {
  @ViewChild('wallpaperCanvas', { static: true }) wallpaperCanvas!: ElementRef<HTMLCanvasElement>;
  @ViewChild('wallpaperVideo') wallpaperVideo?: ElementRef<HTMLVideoElement>;

  // Cambia esta ruta por tu live wallpaper.
  // Copia el video a frontend/public/videos/login-wallpaper.mp4
  // y Angular lo sirve como /videos/login-wallpaper.mp4.
  liveWallpaperUrl = 'videos/login-wallpaper5.mp4';
  wallpaperVideoFailed = false;
  username = '';
  password = '';
  showPassword = false;
  loading = false;
  errorMessage = '';
  authCinematic = false;
  authGranted = false;
  typedAccessLines: string[] = [];
  activeAccessLine = -1;

  // Register state
  isRegisterMode = false;
  isForgotMode = false;
  regScreenName = '';
  regEmail = '';
  regPassword = '';
  regConfirmPassword = '';
  showRegPassword = false;
  acceptedTerms = false;
  registerLoading = false;
  registerError = '';
  registerSuccess = '';

  // Pikachu & password strength state
  focusedField: 'none' | 'name' | 'password' | 'confirm' = 'none';
  pikaTyping = false;
  pikaPasswordMode = false;
  buttonReady = false;
  buttonLightning = false;
  forgotUsername = '';
  forgotEmail = '';
  forgotToken = '';
  forgotNewPassword = '';
  forgotConfirmPassword = '';
  forgotLoading = false;
  forgotMessage = '';
  forgotError = '';
  resetStep = false;
  private prevPasswordChecks = { minLength: false, hasUpper: false, hasNumber: false, hasSpecial: false };
  private prevWasCovering = false;
  private prevWasHappy = false;
  private introBurstPlayed = false;

  get passwordChecks() {
    const p = this.regPassword;
    return {
      minLength: p.length >= 6,
      hasUpper: /[A-Z]/.test(p),
      hasNumber: /[0-9]/.test(p),
      hasSpecial: /[!@#$%^&*()_+\-=\[\]{};':"\\|,.<>\/?]/.test(p)
    };
  }

  get passwordStrength(): number {
    const c = this.passwordChecks;
    return (c.minLength ? 1 : 0) + (c.hasUpper ? 1 : 0) + (c.hasNumber ? 1 : 0) + (c.hasSpecial ? 1 : 0);
  }

  get isPasswordStrong(): boolean {
    return this.passwordStrength >= 4;
  }

  get canRegister(): boolean {
    return this.regScreenName.trim().length >= 2
      && this.isValidEmail(this.regEmail)
      && this.isPasswordStrong
      && this.regPassword === this.regConfirmPassword
      && this.acceptedTerms
      && !this.registerLoading;
  }

  get pikaEyeX(): number {
    const len = this.getFocusedTextLength();
    // Eyes sweep left-to-right as text gets longer, with a wider arc
    const clampedLen = Math.min(len, 20);
    // Map 0..20 chars to -8..8 pixels, with a sine curve for smoothness
    return Math.sin((clampedLen / 20) * Math.PI * 2.5 - Math.PI * 0.3) * 8;
  }

  get pikaEyeY(): number {
    const len = this.getFocusedTextLength();
    // Slight vertical movement as they follow typing
    if (this.focusedField === 'password' || this.focusedField === 'confirm') {
      return 3; // Look down at password field
    }
    return Math.cos((Math.min(len, 20) / 20) * Math.PI * 2) * 2 - 1;
  }

  get pikaPasswordCovering(): boolean {
    return !this.showRegPassword && this.pikaPasswordMode;
  }

  mouseDriftX = 0;
  mouseDriftY = 0;
  speedLines = Array.from({ length: 34 }, (_, i) => ({
    x: (i * 29) % 112 - 8,
    y: (i * 47) % 118 - 9,
    w: 95 + (i % 7) * 34,
    d: 2.2 + (i % 8) * 0.16,
    delay: i * -0.13
  }));
  auraMotes = Array.from({ length: 42 }, (_, i) => ({
    x: (i * 41) % 104 - 2,
    y: (i * 67) % 104 - 2,
    s: 3 + (i % 5) * 2,
    d: 4.5 + (i % 8) * .45,
    delay: i * -.19
  }));
  prismShards = Array.from({ length: 18 }, (_, i) => ({
    x: (i * 61) % 108 - 4,
    y: (i * 31) % 102 - 1,
    delay: i * -.27,
    d: 5.2 + (i % 6) * .55,
    rot: -36 + (i * 23) % 72
  }));
  stormCards = [
    'base1-1', 'base1-2', 'base1-4', 'base1-6', 'base1-7', 'base1-10',
    'base1-15', 'base2-1', 'base2-2', 'base2-5', 'base3-1', 'base3-3',
    'base3-6', 'base4-1', 'base4-4', 'base4-7', 'xy1-1', 'xy1-2',
    'xy1-3', 'xy1-4', 'xy1-5', 'xy1-6', 'xy1-7', 'xy1-8',
    'xy1-9', 'xy10-1', 'xy12-76', 'swsh4-49', 'sm9-82', 'bw10-98'
  ].map((id, i) => ({
    id,
    x: (i * 37) % 108 - 4,
    y: (i * 53) % 106 - 16,
    w: 48 + (i % 6) * 18,
    delay: .38 + (i % 10) * .08,
    duration: 7.2 + (i % 7) * .72,
    depth: .18 + (i % 8) * .055,
    rot: -34 + (i * 29) % 76,
    fall: 78 + (i % 5) * 34,
    z: 12 + (i % 6)
  }));
  passTransform = 'rotateY(-12deg) rotateX(8deg)';

  private renderer?: THREE.WebGLRenderer;
  private scene?: THREE.Scene;
  private camera?: THREE.PerspectiveCamera;
  private stormGroup = new THREE.Group();
  private cardGroup = new THREE.Group();
  private orbGroup = new THREE.Group();
  private particleSystem?: THREE.Points;
  private lightningBolts: THREE.Line[] = [];
  private animationId = 0;
  private readonly loginTimers: number[] = [];
  private mouse = new THREE.Vector2(0, 0);
  private clock = new THREE.Clock();
  private readonly disposable: Array<THREE.BufferGeometry | THREE.Material> = [];

  constructor(
    private authService: AuthService,
    private router: Router,
    private ngZone: NgZone,
    private cdr: ChangeDetectorRef,
    public sound: SoundService
  ) {}

  ngAfterViewInit(): void {
    this.ngZone.runOutsideAngular(() => {
      this.initWallpaper();
      this.animateWallpaper();
      setTimeout(() => this.ensureWallpaperVideoPlaying(), 80);
      setTimeout(() => this.ensureWallpaperVideoPlaying(), 650);
    });
    // Play intro burst sound once when the page loads
    if (!this.introBurstPlayed) {
      this.introBurstPlayed = true;
      // The visual intro-burst starts immediately, so we shouldn't delay the sound by 300ms.
      // Playing it immediately keeps it in sync.
      this.sound.burst();
      
      // Auto-start music if the user previously had it ON
      if (localStorage.getItem('ptcg_music') === '1') {
        this.sound.startMusic(true); // pass skipBurst=true since we already played burst()
      }
    }
  }

  ngOnDestroy(): void {
    cancelAnimationFrame(this.animationId);
    this.loginTimers.forEach((timer) => window.clearTimeout(timer));
    this.disposable.forEach((item) => item.dispose());
    this.renderer?.dispose();
    this.sound.stopMusic();
  }

  @HostListener('window:resize')
  onResize(): void {
    this.resizeWallpaper();
  }

  @HostListener('window:mousemove', ['$event'])
  onMouseMove(event: MouseEvent): void {
    const x = (event.clientX / window.innerWidth) * 2 - 1;
    const y = -(event.clientY / window.innerHeight) * 2 + 1;
    this.mouse.set(x, y);
    this.mouseDriftX = x * 42;
    this.mouseDriftY = -y * 30;
    if (this.authCinematic) return;
    this.passTransform = `rotateY(${(-12 + x * 5).toFixed(2)}deg) rotateX(${(8 - y * 4).toFixed(2)}deg) translate3d(${(x * 8).toFixed(1)}px, ${(-y * 8).toFixed(1)}px, 0)`;
  }

  onWallpaperVideoError(): void {
    this.wallpaperVideoFailed = true;
  }

  playWallpaperVideo(event: Event): void {
    const video = event.target as HTMLVideoElement;
    video.muted = true;
    video.play().catch(() => {
      this.wallpaperVideoFailed = true;
    });
  }

  private ensureWallpaperVideoPlaying(): void {
    const video = this.wallpaperVideo?.nativeElement;
    if (!video || !video.paused) return;

    video.muted = true;
    video.play().catch(() => {
      this.wallpaperVideoFailed = true;
    });
  }

  onLogin(): void {
    if (!this.username.trim() || this.password.length < 4) return;
    this.sound.click();

    this.loading = true;
    this.errorMessage = '';
    this.authCinematic = false;
    this.authGranted = false;
    this.typedAccessLines = [];
    this.activeAccessLine = -1;
    this.authService.login(this.username, this.password).subscribe({
      next: (jugador) => {
        localStorage.setItem('jugador', JSON.stringify(jugador));
        this.playAccessSequence(jugador.username || this.username.trim());
      },
      error: (error) => {
        console.error('Error en el login:', error);
        this.errorMessage = typeof error.error === 'string'
          ? error.error.replace('Error en login: ', '')
          : 'No pude conectar con el backend. Revisa que Spring Boot este levantado.';
        this.loading = false;
        this.sound.error();
      }
    });
  }

  switchToRegister(): void {
    this.sound.switchPanel();
    this.errorMessage = '';
    this.forgotError = '';
    this.forgotMessage = '';
    this.isForgotMode = false;
    this.isRegisterMode = true;
    this.buttonReady = false;
    this.buttonLightning = false;
    setTimeout(() => this.sound.panelIn(), 100);
  }

  switchToLogin(): void {
    this.sound.switchPanel();
    this.registerError = '';
    this.registerSuccess = '';
    this.forgotError = '';
    this.forgotMessage = '';
    this.isForgotMode = false;
    this.isRegisterMode = false;
    setTimeout(() => this.sound.panelIn(), 100);
  }

  switchToForgot(): void {
    this.sound.switchPanel();
    this.errorMessage = '';
    this.registerError = '';
    this.registerSuccess = '';
    this.isRegisterMode = false;
    this.isForgotMode = true;
    this.forgotUsername = this.username;
    setTimeout(() => this.sound.panelIn(), 100);
  }

  onFieldFocus(field: 'name' | 'password' | 'confirm'): void {
    this.focusedField = field;
    const wasCovering = this.pikaPasswordCovering;
    this.pikaPasswordMode = field === 'password' || field === 'confirm';
    // Sound when Pikachu starts covering eyes
    const isCoveringNow = this.pikaPasswordCovering;
    if (!this.prevWasCovering && isCoveringNow) {
      this.sound.pikaCover();
    }
    this.prevWasCovering = isCoveringNow;
  }

  onFieldBlur(): void {
    this.focusedField = 'none';
  }

  onRegisterTyping(): void {
    // Check if any new password requirement was met
    const prev = this.prevPasswordChecks;
    const curr = this.passwordChecks;
    if ((!prev.minLength && curr.minLength) ||
        (!prev.hasUpper && curr.hasUpper) ||
        (!prev.hasNumber && curr.hasNumber) ||
        (!prev.hasSpecial && curr.hasSpecial)) {
      this.sound.check();
    }
    this.prevPasswordChecks = { ...curr };

    // Check if Pikachu just became happy
    const wasHappy = this.prevWasHappy;
    const isHappyNow = this.isPasswordStrong;
    if (!wasHappy && isHappyNow) {
      this.sound.pikaHappy();
    }
    this.prevWasHappy = isHappyNow;

    this.checkButtonReady();
    this.pikaTyping = false;
    window.setTimeout(() => {
      this.pikaTyping = true;
      window.setTimeout(() => this.pikaTyping = false, 180);
    });
  }

  toggleRegisterPasswordVisibility(): void {
    this.showRegPassword = !this.showRegPassword;
    // Sound when Pikachu covers/uncovers eyes
    const isCoveringNow = this.pikaPasswordCovering;
    if (isCoveringNow !== this.prevWasCovering) {
      this.sound.pikaCover();
      this.prevWasCovering = isCoveringNow;
    }
  }

  checkButtonReady(): void {
    const wasReady = this.buttonReady;
    this.buttonReady = this.canRegister;
    // Trigger lightning flash only on the transition from not-ready to ready
    if (!wasReady && this.buttonReady) {
      this.buttonLightning = true;
      this.sound.thunder();
      setTimeout(() => this.buttonLightning = false, 1200);
    }
  }

  onRegister(): void {
    if (!this.canRegister) return;
    if (this.regPassword !== this.regConfirmPassword) {
      this.registerError = 'Las contraseñas no coinciden.';
      this.sound.error();
      return;
    }
    if (!this.acceptedTerms) {
      this.registerError = 'Debés aceptar las reglas para continuar.';
      this.sound.error();
      return;
    }

    this.sound.click();
    this.registerLoading = true;
    this.registerError = '';
    this.registerSuccess = '';

    this.authService.register(this.regScreenName, this.regEmail, this.regPassword, this.regConfirmPassword).subscribe({
      next: () => {
        this.registerSuccess = '¡Cuenta creada! Redirigiendo al login...';
        this.registerLoading = false;
        this.sound.success();
        this.username = this.regScreenName;
        this.password = '';
        setTimeout(() => {
          this.switchToLogin();
        }, 1800);
      },
      error: (error) => {
        console.error('Error en registro:', error);
        this.registerError = typeof error.error === 'string'
          ? error.error.replace('Error en registro: ', '')
          : 'No pude conectar con el backend. Revisá que Spring Boot esté levantado.';
        this.registerLoading = false;
        this.sound.error();
      }
    });
  }

  private playAccessSequence(playerName: string): void {
    this.loading = false;
    this.authCinematic = true;
    this.passTransform = 'rotateY(0deg) rotateX(0deg) translate3d(0, 0, 0)';
    this.cdr.detectChanges();

    const lines = [
      `PlayerName: ${playerName} OK`,
      'password: ********** OK',
      'Check status ... OK',
      'Salud de los pokemon OK'
    ];

    this.queueLoginTimer(() => {
      let delay = 0;
      lines.forEach((line, index) => {
        this.queueLoginTimer(() => {
          this.sound.terminal();
          this.typeAccessLine(line, index);
        }, delay);
        delay += line.length * 42 + 520;
      });

      this.queueLoginTimer(() => {
        this.authGranted = true;
        this.sound.granted();
        this.cdr.detectChanges();
      }, delay + 620);

      this.queueLoginTimer(() => {
        this.router.navigate(['/lobby']);
      }, delay + 2800);
    }, 760);
  }

  private typeAccessLine(line: string, index: number): void {
    this.activeAccessLine = index;
    this.typedAccessLines[index] = '';
    [...line].forEach((letter, letterIndex) => {
      this.queueLoginTimer(() => {
        this.typedAccessLines[index] = `${this.typedAccessLines[index]}${letter}`;
        if (letterIndex === line.length - 1) {
          this.activeAccessLine = -1;
        }
        this.cdr.detectChanges();
      }, letterIndex * 42);
    });
  }

  private queueLoginTimer(callback: () => void, delay: number): void {
    this.loginTimers.push(window.setTimeout(callback, delay));
  }

  onForgotPassword(): void {
    if (this.forgotLoading || (!this.forgotUsername.trim() && !this.isValidEmail(this.forgotEmail))) return;
    this.sound.click();

    this.forgotLoading = true;
    this.forgotError = '';
    this.forgotMessage = '';
    this.authService.forgotPassword(this.forgotUsername, this.forgotEmail).subscribe({
      next: (message) => {
        this.forgotMessage = message;
        this.resetStep = true;
        this.forgotLoading = false;
        this.sound.success();
      },
      error: (error) => {
        this.forgotError = typeof error.error === 'string'
          ? error.error.replace('Error recuperando password: ', '')
          : 'No pude conectar con el backend.';
        this.forgotLoading = false;
        this.sound.error();
      }
    });
  }

  onResetPassword(): void {
    if (!this.forgotToken.trim() || this.forgotNewPassword.length < 4 || this.forgotNewPassword !== this.forgotConfirmPassword) return;
    this.sound.click();

    this.forgotLoading = true;
    this.forgotError = '';
    this.authService.resetPassword(this.forgotToken, this.forgotNewPassword, this.forgotConfirmPassword).subscribe({
      next: (message) => {
        this.forgotMessage = message;
        this.forgotLoading = false;
        this.sound.success();
        setTimeout(() => this.switchToLogin(), 1200);
      },
      error: (error) => {
        this.forgotError = typeof error.error === 'string'
          ? error.error.replace('Error cambiando password: ', '')
          : 'No pude conectar con el backend.';
        this.forgotLoading = false;
        this.sound.error();
      }
    });
  }

  private isValidEmail(email: string): boolean {
    return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email.trim());
  }

  private getFocusedTextLength(): number {
    if (this.focusedField === 'confirm') return this.regConfirmPassword.length;
    if (this.focusedField === 'password') return this.regPassword.length;
    if (this.pikaPasswordMode) return Math.max(this.regPassword.length, this.regConfirmPassword.length);
    if (this.focusedField === 'name') return this.regScreenName.length;
    return 0;
  }

  private initWallpaper(): void {
    const canvas = this.wallpaperCanvas.nativeElement;
    this.renderer = new THREE.WebGLRenderer({ canvas, antialias: true, alpha: true });
    this.renderer.setPixelRatio(Math.min(window.devicePixelRatio, 1.8));

    this.scene = new THREE.Scene();
    this.scene.fog = new THREE.FogExp2(0x91d7ff, 0.026);

    this.camera = new THREE.PerspectiveCamera(45, 1, 0.1, 100);
    this.camera.position.set(0, 1.15, 9.2);

    this.scene.add(new THREE.AmbientLight(0x9ee8ff, 1.3));

    const sun = new THREE.DirectionalLight(0xffffff, 2.4);
    sun.position.set(-4, 6, 5);
    this.scene.add(sun);

    const electric = new THREE.PointLight(0xffd542, 9, 24);
    electric.position.set(-3.7, 2.5, 2.4);
    this.scene.add(electric);

    const aura = new THREE.PointLight(0x34d5ff, 7, 22);
    aura.position.set(2.2, -0.2, 2.8);
    this.scene.add(aura);

    this.createArena();
    this.createCards();
    this.createEnergyOrb();
    this.createParticles();
    this.createLightning();

    this.scene.add(this.stormGroup, this.cardGroup, this.orbGroup);
    this.resizeWallpaper();
  }

  private createArena(): void {
    const grid = new THREE.GridHelper(18, 34, 0x9be7ff, 0xffffff);
    grid.position.set(1.2, -2.2, -2.2);
    grid.rotation.x = Math.PI * 0.08;
    const material = grid.material as THREE.Material;
    material.transparent = true;
    material.opacity = 0.28;
    this.stormGroup.add(grid);

    const floorGeometry = new THREE.PlaneGeometry(22, 9);
    const floorMaterial = new THREE.MeshBasicMaterial({
      color: 0x6ee7ff,
      transparent: true,
      opacity: 0.09,
      side: THREE.DoubleSide
    });
    const floor = new THREE.Mesh(floorGeometry, floorMaterial);
    floor.position.set(1.5, -2.25, -2.4);
    floor.rotation.x = -Math.PI / 2.25;
    this.stormGroup.add(floor);
    this.disposable.push(floorGeometry, floorMaterial);
  }

  private createCards(): void {
    const colors = [0xffd23f, 0x29d3ff, 0xf43f5e, 0xffffff, 0x7c3aed];

    for (let i = 0; i < 15; i++) {
      const geometry = new THREE.PlaneGeometry(0.76, 1.08);
      const material = new THREE.MeshStandardMaterial({
        color: colors[i % colors.length],
        emissive: colors[i % colors.length],
        emissiveIntensity: 0.35,
        metalness: 0.35,
        roughness: 0.28,
        transparent: true,
        opacity: 0.75,
        side: THREE.DoubleSide
      });
      const card = new THREE.Mesh(geometry, material);
      const angle = (i / 15) * Math.PI * 2;
      const radius = 2.6 + (i % 3) * 0.55;
      card.position.set(Math.cos(angle) * radius + 0.8, Math.sin(angle) * 1.15 + 0.3, -1.6 - (i % 5) * 0.4);
      card.rotation.set(Math.random() * 0.5, angle, angle * 0.25);
      card.userData = { angle, radius, speed: 0.25 + Math.random() * 0.24, lift: Math.random() * Math.PI * 2 };
      this.cardGroup.add(card);
      this.disposable.push(geometry, material);

      const edgeGeometry = new THREE.EdgesGeometry(geometry);
      const edgeMaterial = new THREE.LineBasicMaterial({ color: 0xffffff, transparent: true, opacity: 0.55 });
      const edges = new THREE.LineSegments(edgeGeometry, edgeMaterial);
      card.add(edges);
      this.disposable.push(edgeGeometry, edgeMaterial);
    }
  }

  private createEnergyOrb(): void {
    const sphereGeometry = new THREE.SphereGeometry(0.92, 48, 48);
    const sphereMaterial = new THREE.MeshStandardMaterial({
      color: 0x35d7ff,
      emissive: 0x1fb6ff,
      emissiveIntensity: 1.9,
      roughness: 0.18,
      metalness: 0.2,
      transparent: true,
      opacity: 0.68
    });
    const sphere = new THREE.Mesh(sphereGeometry, sphereMaterial);
    sphere.position.set(1.75, -0.45, 1.15);
    this.orbGroup.add(sphere);
    this.disposable.push(sphereGeometry, sphereMaterial);

    for (let i = 0; i < 4; i++) {
      const torusGeometry = new THREE.TorusGeometry(1.35 + i * 0.22, 0.018, 8, 120);
      const torusMaterial = new THREE.MeshBasicMaterial({
        color: i % 2 ? 0xffdf5a : 0x9be7ff,
        transparent: true,
        opacity: 0.72
      });
      const ring = new THREE.Mesh(torusGeometry, torusMaterial);
      ring.position.copy(sphere.position);
      ring.rotation.set(Math.PI / 2.6, i * 0.72, i * 0.55);
      ring.userData = { spin: 0.32 + i * 0.14 };
      this.orbGroup.add(ring);
      this.disposable.push(torusGeometry, torusMaterial);
    }
  }

  private createParticles(): void {
    const count = 850;
    const positions = new Float32Array(count * 3);
    const colors = new Float32Array(count * 3);
    const color = new THREE.Color();

    for (let i = 0; i < count; i++) {
      positions[i * 3] = (Math.random() - 0.5) * 20;
      positions[i * 3 + 1] = (Math.random() - 0.5) * 9;
      positions[i * 3 + 2] = -Math.random() * 14 + 3;
      color.set(i % 5 === 0 ? 0xffe066 : 0xb7f5ff);
      colors[i * 3] = color.r;
      colors[i * 3 + 1] = color.g;
      colors[i * 3 + 2] = color.b;
    }

    const geometry = new THREE.BufferGeometry();
    geometry.setAttribute('position', new THREE.BufferAttribute(positions, 3));
    geometry.setAttribute('color', new THREE.BufferAttribute(colors, 3));

    const material = new THREE.PointsMaterial({
      size: 0.035,
      vertexColors: true,
      transparent: true,
      opacity: 0.9,
      blending: THREE.AdditiveBlending,
      depthWrite: false
    });

    this.particleSystem = new THREE.Points(geometry, material);
    this.scene?.add(this.particleSystem);
    this.disposable.push(geometry, material);
  }

  private createLightning(): void {
    const material = new THREE.LineBasicMaterial({
      color: 0xffef70,
      transparent: true,
      opacity: 0.95,
      blending: THREE.AdditiveBlending
    });
    this.disposable.push(material);

    for (let i = 0; i < 7; i++) {
      const geometry = new THREE.BufferGeometry();
      geometry.setAttribute('position', new THREE.BufferAttribute(new Float32Array(10 * 3), 3));
      const line = new THREE.Line(geometry, material);
      line.userData = {
        from: new THREE.Vector3(-6.4 + i * 0.42, 2.7 - i * 0.28, 1.5 - i * 0.08),
        to: new THREE.Vector3(1.2 + Math.random() * 2.2, -0.5 + Math.random() * 1.6, 1.2),
        seed: Math.random() * 100,
        next: 0
      };
      this.regenerateBolt(line, 0);
      this.stormGroup.add(line);
      this.lightningBolts.push(line);
      this.disposable.push(geometry);
    }
  }

  private regenerateBolt(line: THREE.Line, time: number): void {
    const from = line.userData['from'] as THREE.Vector3;
    const to = line.userData['to'] as THREE.Vector3;
    const points: THREE.Vector3[] = [];
    const segments = 9;

    for (let i = 0; i <= segments; i++) {
      const t = i / segments;
      const p = from.clone().lerp(to, t);
      const jitter = Math.sin((time + line.userData['seed'] + i) * 8.7) * 0.18;
      p.x += (Math.random() - 0.5) * 0.45;
      p.y += (Math.random() - 0.5) * 0.55 + jitter;
      p.z += (Math.random() - 0.5) * 0.28;
      points.push(p);
    }

    const position = line.geometry.getAttribute('position') as THREE.BufferAttribute;
    points.forEach((point, index) => position.setXYZ(index, point.x, point.y, point.z));
    position.needsUpdate = true;
    line.userData['next'] = time + 0.045 + Math.random() * 0.08;
  }

  private animateWallpaper(): void {
    this.animationId = requestAnimationFrame(() => this.animateWallpaper());
    if (!this.renderer || !this.scene || !this.camera) return;

    const t = this.clock.getElapsedTime();
    this.camera.position.x += (this.mouse.x * 0.35 - this.camera.position.x) * 0.035;
    this.camera.position.y += (1.15 + this.mouse.y * 0.22 - this.camera.position.y) * 0.035;
    this.camera.lookAt(0.4, -0.2, 0);

    this.stormGroup.rotation.y = Math.sin(t * 0.18) * 0.06 + this.mouse.x * 0.045;
    this.cardGroup.rotation.z = Math.sin(t * 0.12) * 0.035;

    this.cardGroup.children.forEach((card) => {
      const data = card.userData;
      const angle = data['angle'] + t * data['speed'];
      const radius = data['radius'];
      card.position.x = Math.cos(angle) * radius + 0.8;
      card.position.y = Math.sin(angle) * 1.05 + Math.sin(t * 1.4 + data['lift']) * 0.25 + 0.3;
      card.rotation.x += 0.004;
      card.rotation.y += 0.011;
    });

    this.orbGroup.children.forEach((item, i) => {
      item.rotation.x += 0.004 + i * 0.001;
      item.rotation.y += item.userData['spin'] ? item.userData['spin'] * 0.01 : 0.006;
    });
    this.orbGroup.scale.setScalar(1 + Math.sin(t * 2.4) * 0.025);

    if (this.particleSystem) {
      this.particleSystem.rotation.y = t * 0.015;
      const pos = this.particleSystem.geometry.getAttribute('position') as THREE.BufferAttribute;
      for (let i = 0; i < pos.count; i++) {
        const z = pos.getZ(i) + 0.025;
        pos.setZ(i, z > 4 ? -11 : z);
      }
      pos.needsUpdate = true;
    }

    this.lightningBolts.forEach((bolt) => {
      if (t > bolt.userData['next']) this.regenerateBolt(bolt, t);
      (bolt.material as THREE.LineBasicMaterial).opacity = 0.62 + Math.random() * 0.38;
    });

    this.renderer.render(this.scene, this.camera);
  }

  private resizeWallpaper(): void {
    if (!this.renderer || !this.camera) return;

    const width = window.innerWidth;
    const height = window.innerHeight;
    this.renderer.setSize(width, height, false);
    this.camera.aspect = width / height;
    this.camera.updateProjectionMatrix();
  }
}
