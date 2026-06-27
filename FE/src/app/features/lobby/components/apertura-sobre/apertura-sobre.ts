import { Component, ElementRef, OnInit, ViewChild, OnDestroy, HostListener, Input, Output, EventEmitter, ChangeDetectorRef, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import * as THREE from 'three';
import { CardService } from '../../../../core/services/card.service';
import { TranslatePipe } from '../../../../i18n/translate.pipe';

@Component({
  selector: 'app-apertura-sobre',
  standalone: true,
  imports: [CommonModule, TranslatePipe],
  templateUrl: './apertura-sobre.html',
  styleUrl: './apertura-sobre.scss'
})
export class AperturaSobreComponent implements OnInit, OnDestroy {
  private cardService = inject(CardService);
  @ViewChild('rendererContainer', { static: true }) rendererContainer!: ElementRef;

  @Input() cartas: any[] = [];
  @Input() sobresRestantes: number = 0;
  @Output() finalizado = new EventEmitter<void>();
  @Output() abrirOtro = new EventEmitter<void>();
private sobreGroup!: THREE.Group;
  private scene!: THREE.Scene;
  private camera!: THREE.PerspectiveCamera;
  private renderer!: THREE.WebGLRenderer;
  private particulasExplosion!: THREE.Points;
private velocidadesParticulas: THREE.Vector3[] = [];
private luzExplosion!: THREE.PointLight;
private explosiónDisparada: boolean = false;
  private animationId!: number;
private estaCortando: boolean = false; // Nuevo estado para el imán de corte
  private sobreCuerpoMesh!: THREE.Mesh;
  private sobreTapaMesh!: THREE.Mesh;
  private lineaGlow!: THREE.Mesh;
  private mazoCartas: THREE.Group[] = [];
private estaCerrandoSobre: boolean = false; // Flag de seguridad
  private raycaster = new THREE.Raycaster();
  private mouse = new THREE.Vector2();
  
  public mensajeGuia: string = 'pack.swipeToOpen';
  public estaCortado: boolean = false;
  private estaHaciendoClic: boolean = false;
  private tiempoCorte: number = 0;

  public puedePasar: boolean = false;
  public autoRevealEnCurso: boolean = false;
  public resumenVisible: boolean = false;
  public resumenCartas: any[] = [];
  
  private puntoClickInicial = new THREE.Vector2();
  private tiempoClickInicial: number = 0;
  private progresoCorte: number = 0;
  private xMaxAlcanzado: number = -1.6;
  public isEnchanted: boolean = false;
  private glowMesh!: THREE.Mesh;

  constructor(private cdr: ChangeDetectorRef) {}

  ngOnInit(): void {
    // Check if any card is rare
    this.isEnchanted = this.cartas.some(c => c.rarity && ['Rare', 'Epic', 'Legendary', 'Secret Rare', 'Rara', 'Épica', 'Legendaria'].includes(c.rarity) || c.rareza && ['Rare', 'Epic', 'Legendary', 'Secret Rare', 'Rara', 'Épica', 'Legendaria'].includes(c.rareza));

    this.initThree();
    this.crearSobrePro(); 
    this.animate();
  }

  ngOnDestroy(): void {
    if (this.animationId) cancelAnimationFrame(this.animationId);
    if (this.renderer) this.renderer.dispose();
  }

  @HostListener('mousedown', ['$event'])
  onMouseDown(event: MouseEvent) {
    this.estaHaciendoClic = true;
    this.puntoClickInicial.set(this.mouse.x, this.mouse.y);
    this.tiempoClickInicial = Date.now();
  }

  @HostListener('mouseup', ['$event'])
  @HostListener('touchend', ['$event'])
  onMouseUp(event: MouseEvent | TouchEvent) {
    if (!this.estaCortado) {
      // Ya manejamos el auto-completar en el animate()
    } else {
      // Solo procesamos swipe o reveal si el clic empezó DESPUÉS de que el sobre se haya cortado.
      // Esto evita que el release del corte original revele la primera carta sin querer.
      if (this.tiempoClickInicial > this.tiempoCorte) {
        this.procesarSwipeOReveal();
      }
    }
    this.estaHaciendoClic = false;
  }

  @HostListener('mousemove', ['$event'])
  onMouseMove(event: MouseEvent) {
    this.mouse.x = (event.clientX / window.innerWidth) * 2 - 1;
    this.mouse.y = -(event.clientY / window.innerHeight) * 2 + 1;
    
    if (this.estaHaciendoClic && !this.estaCortado) {
      this.procesarCorteCinematico();
    }
  }

  private crearExplosion() {
  const cuenta = 400;
  const geo = new THREE.BufferGeometry();
  const posiciones = new Float32Array(cuenta * 3);
  
  for (let i = 0; i < cuenta; i++) {
    posiciones[i * 3] = 0;
    posiciones[i * 3 + 1] = 0;
    posiciones[i * 3 + 2] = 0;
    this.velocidadesParticulas.push(new THREE.Vector3(
      (Math.random() - 0.5) * 0.4,
      (Math.random() - 0.5) * 0.4,
      (Math.random() - 0.5) * 0.4
    ));
  }

  geo.setAttribute('position', new THREE.BufferAttribute(posiciones, 3));
  const mat = new THREE.PointsMaterial({
    color: 0x00f2ff, // Color neón
    size: 0.08,
    transparent: true,
    blending: THREE.AdditiveBlending
  });

  this.particulasExplosion = new THREE.Points(geo, mat);
  this.scene.add(this.particulasExplosion);

  // Luz intensa momentánea
  this.luzExplosion = new THREE.PointLight(0x00f2ff, 20, 15);
  this.scene.add(this.luzExplosion);
}

  private crearExplosionSkip() {
    const cuenta = 200;
    const geo = new THREE.BufferGeometry();
    const posiciones = new Float32Array(cuenta * 3);
    
    // Reset velocities array if it's already used
    this.velocidadesParticulas = [];
    
    for (let i = 0; i < cuenta; i++) {
      posiciones[i * 3] = 0;
      posiciones[i * 3 + 1] = 0;
      posiciones[i * 3 + 2] = 0;
      this.velocidadesParticulas.push(new THREE.Vector3(
        (Math.random() - 0.5) * 0.8,
        (Math.random() - 0.5) * 0.8 + 0.2,
        (Math.random() - 0.5) * 0.8
      ));
    }

    geo.setAttribute('position', new THREE.BufferAttribute(posiciones, 3));
    const mat = new THREE.PointsMaterial({
      color: 0xffaa00, // Gold color
      size: 0.1,
      transparent: true,
      blending: THREE.AdditiveBlending
    });

    if (this.particulasExplosion) {
      this.scene.remove(this.particulasExplosion);
    }
    
    this.particulasExplosion = new THREE.Points(geo, mat);
    this.scene.add(this.particulasExplosion);

    if (this.luzExplosion) {
       this.luzExplosion.color.setHex(0xffaa00);
       this.luzExplosion.intensity = 20;
    }
  }

  private initThree() {
    this.scene = new THREE.Scene();
    this.camera = new THREE.PerspectiveCamera(35, window.innerWidth / window.innerHeight, 0.1, 1000);
    this.camera.position.z = 10;

    this.renderer = new THREE.WebGLRenderer({ antialias: true, alpha: true });
    this.renderer.setSize(window.innerWidth, window.innerHeight);
    this.renderer.setPixelRatio(Math.min(window.devicePixelRatio, 1.35));
    this.rendererContainer.nativeElement.appendChild(this.renderer.domElement);

    const pmrem = new THREE.PMREMGenerator(this.renderer);
    this.scene.environment = pmrem.fromScene(new THREE.Scene()).texture;

    this.scene.add(new THREE.AmbientLight(0xffffff, 0.7));
    
    const studioLight = new THREE.DirectionalLight(0xffffff, 1.0);
    studioLight.position.set(0, 5, 10);
    this.scene.add(studioLight);

    // LUZ HOLO: Color más natural (Blanco cálido) y menos intensa
    const holoLight = new THREE.PointLight(0xfff4e5, 1.5, 15);
    holoLight.name = "holoLight";
    this.scene.add(holoLight);

    // Fondo de partículas mágicas espaciales (Más épico)
    const bgGeo = new THREE.BufferGeometry();
    const cantParticulas = 800;
    const bgPos = new Float32Array(cantParticulas * 3);
    const bgColors = new Float32Array(cantParticulas * 3);
    
    const colorA = new THREE.Color(0x00d4ff); // Cyan
    const colorB = new THREE.Color(0xaa00ff); // Púrpura
    const colorC = new THREE.Color(0xff0088); // Rosa neón
    
    for (let i = 0; i < cantParticulas; i++) {
       bgPos[i*3] = (Math.random() - 0.5) * 80;
       bgPos[i*3+1] = (Math.random() - 0.5) * 80;
       bgPos[i*3+2] = (Math.random() - 0.5) * 60 - 20;
       
       const rand = Math.random();
       let col = colorA;
       if (rand > 0.5) col = colorB;
       if (rand > 0.85) col = colorC;
       
       bgColors[i*3] = col.r;
       bgColors[i*3+1] = col.g;
       bgColors[i*3+2] = col.b;
    }
    bgGeo.setAttribute('position', new THREE.BufferAttribute(bgPos, 3));
    bgGeo.setAttribute('color', new THREE.BufferAttribute(bgColors, 3));
    
    const bgMat = new THREE.PointsMaterial({ 
      size: 0.15, 
      vertexColors: true, 
      transparent: true, 
      opacity: 0.8, 
      blending: THREE.AdditiveBlending 
    });
    
    const bgParticles = new THREE.Points(bgGeo, bgMat);
    this.scene.add(bgParticles);
    (this as any).bgParticles = bgParticles;

    // AÚN MÁS ÉPICO: Un sol o galaxia central detrás de todo
    const galaxyGeo = new THREE.IcosahedronGeometry(45, 2);
    const galaxyMat = new THREE.MeshBasicMaterial({ 
      color: 0x2200ff, 
      wireframe: true, 
      transparent: true, 
      opacity: 0.15,
      blending: THREE.AdditiveBlending 
    });
    const galaxy = new THREE.Mesh(galaxyGeo, galaxyMat);
    galaxy.position.z = -50;
    this.scene.add(galaxy);
    (this as any).galaxy = galaxy;
    
    // Y un segundo wireframe rotando en contra
    const galaxy2 = new THREE.Mesh(
      new THREE.IcosahedronGeometry(50, 1),
      new THREE.MeshBasicMaterial({ color: 0x00ffff, wireframe: true, transparent: true, opacity: 0.08, blending: THREE.AdditiveBlending })
    );
    galaxy2.position.z = -50;
    this.scene.add(galaxy2);
    (this as any).galaxy2 = galaxy2;
  }

private crearSobrePro() {
    const loader = new THREE.TextureLoader();
    this.sobreGroup = new THREE.Group();
    this.scene.add(this.sobreGroup);

    const materialBase = new THREE.MeshPhysicalMaterial({
      color: 0xffffff, metalness: 0.7, roughness: 0.3, clearcoat: 1.0, side: THREE.DoubleSide
    });

    // CUERPO
    loader.load('images/cards/sobre.png', (tex: THREE.Texture) => {
      const image = tex.image as HTMLImageElement;
      const aspect = image.width / image.height;
      const ancho = 3.5;
      const alto = ancho / aspect;
      const geo = new THREE.PlaneGeometry(ancho, alto);
      const mat = materialBase.clone();
      mat.map = tex;
      
      if (this.isEnchanted) {
        // Minecraft-like enchantment
        mat.emissive = new THREE.Color(0x8800ff);
        mat.emissiveIntensity = 0.5;
        
        // Add a pulsing aura
        const auraMat = new THREE.MeshBasicMaterial({ color: 0x00ffff, transparent: true, opacity: 0.3, blending: THREE.AdditiveBlending });
        this.glowMesh = new THREE.Mesh(new THREE.PlaneGeometry(ancho * 1.1, alto * 1.1), auraMat);
        this.glowMesh.position.y = -alto / 2;
        this.glowMesh.position.z = -0.01;
        this.sobreGroup.add(this.glowMesh);
      }

      this.sobreCuerpoMesh = new THREE.Mesh(geo, mat);
      this.sobreCuerpoMesh.position.y = -alto / 2;
      this.sobreGroup.add(this.sobreCuerpoMesh);
    });

    // TAPA CON PIVOTE MANUAL
    loader.load('images/cards/parte-corte.png', (tex: THREE.Texture) => {
      const image = tex.image as HTMLImageElement;
      const aspect = image.width / image.height;
      const ancho = 3.5;
      const alto = ancho / aspect;
      
      // Creamos un grupo solo para la tapa para rotarla desde un extremo
      const tapaPivot = new THREE.Group();
      const geo = new THREE.PlaneGeometry(ancho, alto);
      const mat = materialBase.clone();
      mat.map = tex;
      this.sobreTapaMesh = new THREE.Mesh(geo, mat);
      
      // Desplazamos la malla dentro de su grupo para que el eje de rotación sea la base de la tapa
      this.sobreTapaMesh.position.set(0, alto / 2, 0); 
      tapaPivot.add(this.sobreTapaMesh);
      tapaPivot.position.y = 0; // La unión con el cuerpo
      
      this.sobreGroup.add(tapaPivot);
      (this.sobreTapaMesh as any).userData = { pivot: tapaPivot };
    });

    // LÍNEA DE CORTE (Láser que no falla)
    this.lineaGlow = new THREE.Mesh(
      new THREE.PlaneGeometry(3.5, 0.08),
      new THREE.MeshBasicMaterial({ color: 0x00ffff, transparent: true, opacity: 0, blending: THREE.AdditiveBlending, depthTest: false })
    );
    this.lineaGlow.position.z = 0.05;
    this.sobreGroup.add(this.lineaGlow);
  }

private deformar(geo: THREE.BufferGeometry, amt: number, cuerpo: boolean) {
    const pos = geo.attributes['position'] as THREE.BufferAttribute;
    for (let i = 0; i < pos.count; i++) {
      const x = pos.getX(i); const y = pos.getY(i);
      const z = Math.cos(Math.sqrt(x*x + y*y) * 0.8) * amt * (cuerpo ? Math.cos(y * 0.5) * 1.5 : 1);
      pos.setZ(i, z);
    }
    geo.computeVertexNormals();
  }

 private procesarCorteCinematico() {
    // 1. LÓGICA DE ENGANCHE (Solo se activa al inicio)
    if (this.estaHaciendoClic && !this.estaCortando && !this.estaCortado) {
      this.raycaster.setFromCamera(this.mouse, this.camera);
      // Chequeamos intersección tanto en cuerpo como en tapa para mayor margen
      const intersects = this.raycaster.intersectObjects([this.sobreCuerpoMesh, this.sobreTapaMesh]);
      
      if (intersects.length > 0) {
        // Si el clic fue cerca de la unión (Y central)
        const puntoLocalY = intersects[0].point.y;
        if (Math.abs(puntoLocalY) < 1.5) { 
          this.estaCortando = true;
        }
      }
    }

    // 2. LÓGICA DE PROGRESO (Modo persistente)
    if (this.estaCortando && this.estaHaciendoClic) {
      // Simplificamos: convertimos mouse.x (-1 a 1) a progreso (0 a 1)
      // Ajustamos el offset (0.7) para que el inicio del sobre coincida con el movimiento natural
      const mouseNormalizado = (this.mouse.x + 0.7) / 1.4;
      const nuevoProgreso = THREE.MathUtils.clamp(mouseNormalizado, 0, 1);

      // Solo avanzamos, nunca retrocedemos el corte (esto evita el trabado al ir lento)
      if (nuevoProgreso > this.progresoCorte) {
        this.progresoCorte = nuevoProgreso;
        
        // Actualizar visual de la línea láser
        const matGlow = this.lineaGlow.material as THREE.MeshBasicMaterial;
        matGlow.opacity = 0.8;
        this.lineaGlow.scale.x = this.progresoCorte;
        // La línea se mueve con el progreso para simular que el tajo avanza
        this.lineaGlow.position.x = -1.6 * (1 - this.progresoCorte);

        if (this.sobreTapaMesh) {
          const geoTapa = this.sobreTapaMesh.geometry as THREE.PlaneGeometry;
          const altoTapa = geoTapa.parameters.height; 

          // Rotación y elevación progresiva
          this.sobreTapaMesh.rotation.z = this.progresoCorte * 0.45;
          this.sobreTapaMesh.position.y = (altoTapa / 2) + (this.progresoCorte * 0.15);
          this.sobreTapaMesh.position.x = this.progresoCorte * 0.1;
        }
      }
    } else {
      this.estaCortando = false;
    }

    // 3. GATILLO DE SEGURIDAD (Más generoso)
    // Si llegó al 85% y sigue arrastrando, o si llegó al 92% total, se dispara.
    if (this.progresoCorte > 0.88 && !this.estaCortado) {
      this.iniciarAperturaFinal();
    }
  }
  

  private iniciarAperturaFinal() {
    this.estaCortado = true;
    this.estaHaciendoClic = false;
    this.lineaGlow.visible = false;
    this.tiempoCorte = Date.now();
    
    // DISPARAR EXPLOSIÓN
    this.crearExplosion();
    this.explosiónDisparada = true;

    this.mensajeGuia = "--- EXPLOTANDO SOBRE ---";
    this.cdr.detectChanges();

    setTimeout(() => {
      this.sobreGroup.visible = false; // El sobre desaparece definitivamente
      this.crearCartasAntiBug();
      this.mensajeGuia = "";
      this.resumenCartas = [...this.cartas];
      this.cdr.detectChanges();
      
      setTimeout(() => {
        this.mensajeGuia = 'pack.tapToReveal';
        this.puedePasar = true;
        this.cdr.detectChanges();
      }, 1400); // Esperar a que terminen de saltar
    }, 700);
  }

  getImagenCarta(id: string): string {
    return this.cardService.getImagenCarta(id);
  }

  onCardImageError(event: Event): void {
    const img = event.target as HTMLImageElement;
    this.cardService.handleCardImageError(img);
  }

  private crearCartasAntiBug() {
    const loader = new THREE.TextureLoader();
    const backTex = loader.load('images/cards/back.png');

    this.cartas.forEach((c, i) => {
      const group = new THREE.Group();
      const matFront = new THREE.MeshPhysicalMaterial({
        map: backTex,
        metalness: 0.5,
        roughness: 0.3,
        iridescence: 0.4,
        iridescenceIOR: 1.3,
        side: THREE.FrontSide
      });

      loader.load(
        this.getImagenCarta(c.id),
        (tex) => {
          matFront.map = tex;
          matFront.needsUpdate = true;
        },
        undefined,
        () => {
          matFront.map = backTex;
          matFront.needsUpdate = true;
        }
      );

      const front = new THREE.Mesh(new THREE.PlaneGeometry(2.2, 3.1), matFront);
      const back = new THREE.Mesh(
        new THREE.PlaneGeometry(2.2, 3.1),
        new THREE.MeshPhysicalMaterial({ map: backTex, metalness: 0.3, roughness: 0.7, side: THREE.FrontSide })
      );
      back.rotation.y = Math.PI;
      back.position.z = -0.02;

      group.add(front);
      group.add(back);
      group.position.set(0, -2, -6);
      group.rotation.y = Math.PI; 
      group.visible = false;
      
      (group as any).userData = { 
        estado: 'saltando', 
        targetZ: 2.0 - (i * 0.1), // MAZO ATRÁS POR DEFECTO
        vel: new THREE.Vector3() 
      };
      
      this.mazoCartas.push(group);
      this.scene.add(group);
    });
  }

  private procesarSwipeOReveal() {
    const carta = this.mazoCartas[0];
    if (!carta) return;

    const deltaX = this.mouse.x - this.puntoClickInicial.x;
    const tiempo = (Date.now() - this.tiempoClickInicial) / 1000;
    const velocidad = Math.abs(deltaX / (tiempo || 1));

    if (velocidad > 1.0 && Math.abs(deltaX) > 0.15) {
      (carta as any).userData.estado = 'shopeada';
      // Reducimos la velocidad inicial para que no desaparezca tan agresivamente
      (carta as any).userData.velSwipe = Math.sign(deltaX) * Math.max(Math.abs(deltaX * 5), 5);
    } else {
      // Si está de espaldas y la animación inicial ya terminó
      if (Math.abs(carta.rotation.y) >= Math.PI - 0.2 && (carta as any).userData.estado === 'en_mazo') {
        (carta as any).userData.estado = 'revelando';
        (carta as any).userData.tReveal = Date.now();
      }
    }
  }

  private animate() {
    this.animationId = requestAnimationFrame(() => this.animate());

    const holoLight = this.scene.getObjectByName("holoLight") as THREE.PointLight;
    if (this.explosiónDisparada && this.particulasExplosion) {
      const coords = this.particulasExplosion.geometry.attributes['position'] as THREE.BufferAttribute;
      
      for (let i = 0; i < this.velocidadesParticulas.length; i++) {
        const v = this.velocidadesParticulas[i];
        coords.setX(i, coords.getX(i) + v.x);
        coords.setY(i, coords.getY(i) + v.y);
        coords.setZ(i, coords.getZ(i) + v.z);
        
        // Gravedad sutil para que caigan
        v.y -= 0.002;
      }
      coords.needsUpdate = true;
      
      // Apagar luz y partículas
      if (this.luzExplosion) this.luzExplosion.intensity *= 0.92;
      (this.particulasExplosion.material as THREE.PointsMaterial).opacity *= 0.98;
    }

    if ((this as any).bgParticles) {
       (this as any).bgParticles.rotation.y += 0.0005;
       (this as any).bgParticles.rotation.x += 0.0002;
    }
    if ((this as any).galaxy) {
       (this as any).galaxy.rotation.y += 0.001;
       (this as any).galaxy.rotation.z += 0.0005;
    }
    if ((this as any).galaxy2) {
       (this as any).galaxy2.rotation.y -= 0.0008;
       (this as any).galaxy2.rotation.x -= 0.0004;
    }

    if (!this.estaHaciendoClic && !this.estaCortado && this.progresoCorte > 0) {
      if (this.progresoCorte < 0.5) {
        this.progresoCorte = THREE.MathUtils.lerp(this.progresoCorte, 0, 0.1);
      } else {
        this.progresoCorte = THREE.MathUtils.lerp(this.progresoCorte, 1.0, 0.05);
      }
      
      const matGlow = this.lineaGlow.material as THREE.MeshBasicMaterial;
      matGlow.opacity = 0.8 * this.progresoCorte;
      this.lineaGlow.scale.x = Math.max(0.001, this.progresoCorte);
      this.lineaGlow.position.x = -1.6 * (1 - this.progresoCorte);

      if (this.sobreTapaMesh) {
        const geoTapa = this.sobreTapaMesh.geometry as THREE.PlaneGeometry;
        const altoTapa = geoTapa.parameters.height; 
        this.sobreTapaMesh.rotation.z = this.progresoCorte * 0.45;
        this.sobreTapaMesh.position.y = (altoTapa / 2) + (this.progresoCorte * 0.15);
        this.sobreTapaMesh.position.x = this.progresoCorte * 0.1;
      }

      if (this.progresoCorte > 0.88 && !this.estaCortado) {
        this.iniciarAperturaFinal();
      }
    }

    if (holoLight) {
        holoLight.position.set(this.mouse.x * 4, this.mouse.y * 4, 7);
    }

    if (!this.estaCortado) {
      const lerp = 0.05;
      // Rotación suave del grupo
      this.sobreGroup.rotation.y = THREE.MathUtils.lerp(this.sobreGroup.rotation.y, this.mouse.x * 0.3, lerp);
      this.sobreGroup.rotation.x = THREE.MathUtils.lerp(this.sobreGroup.rotation.x, -this.mouse.y * 0.2, lerp);
      
      if (this.isEnchanted && this.sobreCuerpoMesh) {
         const t = Date.now() * 0.002;
         const matCuerpo = this.sobreCuerpoMesh.material as THREE.MeshPhysicalMaterial;
         matCuerpo.emissive.setHSL((t * 0.1) % 1, 1, 0.5);
         if (this.glowMesh) {
            (this.glowMesh.material as THREE.MeshBasicMaterial).color.setHSL(((t * 0.1) + 0.5) % 1, 1, 0.5);
            this.glowMesh.scale.setScalar(1 + Math.sin(t * 2) * 0.05);
         }
      }
    } else {
      // Si estamos en medio del proceso de apertura final...
      const ts = (Date.now() - this.tiempoCorte) / 1000;
      if (this.estaCortado && ts < 3) {
        this.camera.position.z = THREE.MathUtils.lerp(this.camera.position.z, 11, 0.04);
        this.sobreGroup.position.z -= 0.05;
        this.sobreGroup.rotation.x -= 0.01;
        
        if (this.sobreTapaMesh) {
          this.sobreTapaMesh.position.y += 0.15;
          this.sobreTapaMesh.rotation.z += 0.05;
          this.sobreTapaMesh.rotation.x += 0.05;
        }
      }

      this.mazoCartas.forEach((card, i) => {
        const ud = (card as any).userData;
        const ct = (Date.now() - (this.tiempoCorte + 700 + i * 120)) / 1000;

        if (ct > 0 && (ud.estado === 'saltando' || ud.estado === 'en_mazo')) {
          card.visible = true;
          if (ct < 1.4) {
            ud.estado = 'saltando';
            const progress = ct / 1.4; // Normalizado de 0 a 1
            card.position.y = Math.sin(progress * Math.PI) * 3.5;
            card.position.z = -6 + progress * (ud.targetZ + 6);
            card.rotation.z = Math.sin(progress * Math.PI) * 0.4;
          } else {
            ud.estado = 'en_mazo';
            if (i > 0) {
              // Reposo del mazo (Lejos del área de inspección)
              card.position.lerp(new THREE.Vector3(0, 0, ud.targetZ), 0.1);
              card.rotation.z = THREE.MathUtils.lerp(card.rotation.z, 0, 0.1);
            }
          }
        }

        // --- GESTIÓN DE CARTA ACTIVA ---
        if (i === 0 && ud.estado !== 'shopeada' && ud.estado !== 'saltando') {
          card.renderOrder = 999;
          // LA CARTA ACTIVA SALTA ADELANTE Y AL CENTRO
          const inspeccionZ = 4.5;
          card.position.z = THREE.MathUtils.lerp(card.position.z, inspeccionZ, 0.15);
          card.position.x = THREE.MathUtils.lerp(card.position.x, 0, 0.15);
          card.position.y = THREE.MathUtils.lerp(card.position.y, 0, 0.15);
          
          if (ud.estado !== 'revelando') {
             card.rotation.y = THREE.MathUtils.lerp(card.rotation.y, (ud.estado === 'esperando') ? this.mouse.x * 0.5 : Math.PI + this.mouse.x * 0.5, 0.1);
             card.rotation.x = THREE.MathUtils.lerp(card.rotation.x, -this.mouse.y * 0.4, 0.1);
          }
        }

        if (ud.estado === 'revelando') {
          const rt = (Date.now() - ud.tReveal) / 1000 * 2.2;
          if (rt < 1) {
            card.rotation.y = Math.PI - (rt * Math.PI);
            card.position.z = 5.5; // Un poquito más adelante al girar
          } else {
            card.rotation.y = 0; ud.estado = 'esperando';
          }
        }

        if (ud.estado === 'shopeada') {
          // Accelerate to make sure it flies away
          ud.velSwipe *= 1.05; 
          card.position.x += ud.velSwipe * 0.08;
          card.position.z -= 0.4;
          card.rotation.z -= ud.velSwipe * 0.03;
          if (Math.abs(card.position.x) > 30) {
            this.scene.remove(card);
            this.mazoCartas.shift();
            if (this.mazoCartas.length === 0) {
              this.finalizado.emit();
            }
          }
        }
        if (ud.estado === 'volando_lejos') {
          const t = (Date.now() - ud.tSkip) / 1000;
          card.position.y += 0.2 + (i * 0.05);
          card.position.z -= 0.5;
          card.rotation.x += 0.1;
          card.rotation.z += 0.05;
          card.scale.setScalar(Math.max(0.01, 1 - (t * 2)));
        }
      });
    }
    this.renderer.render(this.scene, this.camera);
  }

  pasarRevelado() {
    this.puedePasar = false;
    this.autoRevealEnCurso = true;
    this.cdr.detectChanges();

    this.crearExplosionSkip();
    this.explosiónDisparada = true;

    this.mazoCartas.forEach((card, i) => {
       (card as any).userData.estado = 'volando_lejos';
       (card as any).userData.tSkip = Date.now();
    });

    setTimeout(() => {
      this.mazoCartas.forEach(card => this.scene.remove(card));
      this.mazoCartas = [];
      this.autoRevealEnCurso = false;
      this.resumenVisible = true;
      this.cdr.detectChanges();
    }, 800);
  }

  onOverlayClick(event: MouseEvent) {
    if (this.resumenVisible) {
      this.finalizado.emit();
    }
  }
}
