import { Component, ElementRef, OnInit, ViewChild, OnDestroy, HostListener, Input, Output, EventEmitter, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import * as THREE from 'three';

@Component({
  selector: 'app-apertura-sobre',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './apertura-sobre.html',
  styleUrl: './apertura-sobre.scss'
})
export class AperturaSobreComponent implements OnInit, OnDestroy {
  @ViewChild('rendererContainer', { static: true }) rendererContainer!: ElementRef;

  @Input() cartas: any[] = [];
  @Output() onClose = new EventEmitter<void>();
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
  
  public mensajeGuia: string = "--- MANTÉN Y DESLIZA PARA ABRIR ---";
  public estaCortado: boolean = false;
  private estaHaciendoClic: boolean = false;
  private tiempoCorte: number = 0;
  
  private puntoClickInicial = new THREE.Vector2();
  private tiempoClickInicial: number = 0;
  private progresoCorte: number = 0;
  private xMaxAlcanzado: number = -1.6;

  constructor(private cdr: ChangeDetectorRef) {}

  ngOnInit(): void {
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

  @HostListener('mouseup')
  onMouseUp() {
    this.estaHaciendoClic = false;
    if (this.estaCortado && this.mazoCartas.length > 0) {
      this.procesarSwipeOReveal();
    }
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

  private initThree() {
    this.scene = new THREE.Scene();
    this.camera = new THREE.PerspectiveCamera(35, window.innerWidth / window.innerHeight, 0.1, 1000);
    this.camera.position.z = 10;

    this.renderer = new THREE.WebGLRenderer({ antialias: true, alpha: true });
    this.renderer.setSize(window.innerWidth, window.innerHeight);
    this.renderer.setPixelRatio(Math.min(window.devicePixelRatio, 2));
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
  }

private crearSobrePro() {
    const loader = new THREE.TextureLoader();
    this.sobreGroup = new THREE.Group();
    this.scene.add(this.sobreGroup);

    const materialBase = new THREE.MeshPhysicalMaterial({
      color: 0xffffff, metalness: 0.7, roughness: 0.3, clearcoat: 1.0, side: THREE.DoubleSide
    });

    // CUERPO
    loader.load('images/cards/sobre.png', (tex) => {
      const aspect = tex.image.width / tex.image.height;
      const ancho = 3.5;
      const alto = ancho / aspect;
      const geo = new THREE.PlaneGeometry(ancho, alto);
      const mat = materialBase.clone();
      mat.map = tex;
      this.sobreCuerpoMesh = new THREE.Mesh(geo, mat);
      this.sobreCuerpoMesh.position.y = -alto / 2;
      this.sobreGroup.add(this.sobreCuerpoMesh);
    });

    // TAPA CON PIVOTE MANUAL
    loader.load('images/cards/parte-corte.png', (tex) => {
      const aspect = tex.image.width / tex.image.height;
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
      // Solo soltamos el estado interno, pero el 'progresoCorte' se guarda
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
    this.crearCartasAntiBug();
    this.mensajeGuia = "--- TOCA O DESLIZA ---";
    this.cdr.detectChanges();
  }, 700);
}

  private crearCartasAntiBug() {
    const loader = new THREE.TextureLoader();
    const backTex = loader.load('images/cards/back.png');

    this.cartas.forEach((c, i) => {
      const group = new THREE.Group();
      const matFront = new THREE.MeshPhysicalMaterial({ 
        map: loader.load(`images/cards/${c.id}.png`), 
        metalness: 0.5, roughness: 0.3, iridescence: 0.4, iridescenceIOR: 1.3,
        side: THREE.FrontSide 
      });
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
      (carta as any).userData.velSwipe = deltaX * 15;
    } else {
      if (Math.abs(carta.rotation.y) >= Math.PI - 0.2) {
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
    if (holoLight) {
        holoLight.position.set(this.mouse.x * 4, this.mouse.y * 4, 7);
    }

    if (!this.estaCortado) {
      const lerp = 0.05;
      // Rotación suave del grupo
      this.sobreGroup.rotation.y = THREE.MathUtils.lerp(this.sobreGroup.rotation.y, this.mouse.x * 0.3, lerp);
      this.sobreGroup.rotation.x = THREE.MathUtils.lerp(this.sobreGroup.rotation.x, -this.mouse.y * 0.2, lerp);
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

        if (ct > 0 && ud.estado === 'saltando') {
          card.visible = true;
          if (ct < 1.4) {
            card.position.y = Math.sin(ct * Math.PI) * 3.5;
            card.position.z = -6 + (ct * 9);
            card.rotation.z = Math.sin(ct * 3) * 0.4;
          } else {
            // Reposo del mazo (Lejos del área de inspección)
            card.position.lerp(new THREE.Vector3(0, 0, ud.targetZ), 0.1);
            card.rotation.z = THREE.MathUtils.lerp(card.rotation.z, 0, 0.1);
          }
        }

        // --- GESTIÓN DE CARTA ACTIVA ---
        if (i === 0 && ud.estado !== 'shopeada' && ud.estado !== 'saltando') {
          card.renderOrder = 999;
          // LA CARTA ACTIVA SALTA ADELANTE (Z=4.5)
          const inspeccionZ = 4.5;
          card.position.z = THREE.MathUtils.lerp(card.position.z, inspeccionZ, 0.15);
          
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
          card.position.x += ud.velSwipe * 0.08;
          card.position.z -= 0.4;
          card.rotation.z -= ud.velSwipe * 0.03;
          if (Math.abs(card.position.x) > 35) {
            this.scene.remove(card);
            this.mazoCartas.shift();
            if (this.mazoCartas.length === 0) this.onClose.emit();
          }
        }
      });
    }
    this.renderer.render(this.scene, this.camera);
  }
}