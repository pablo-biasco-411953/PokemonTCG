---
sidebar_position: 5
title: 🎴 MazoService
---

# 🎴 MazoService - Servicio de Mazos (Decks)

> Servicio Angular para crear, listar, actualizar y eliminar mazos de cartas

---

## 📍 Ubicación

`frontend/src/app/features/deck-builder/services/mazo.service.ts`

---

## 🏗️ Definición del Servicio

```typescript
@Injectable([ providedIn: 'root' ])
export class MazoService {
  private apiUrl = `PARAM/api/mazos`;
  
  constructor(private http: HttpClient) {}
}
```

**Tipo**: Servicio raíz
**Dependencias**: HttpClient
**Feature**: Deck Builder (Constructor de mazos)

---

## 📡 Métodos Principales

### 1. guardarMazo(nombre: string, username: string, cartas: string[])

**Crear y guardar un nuevo mazo**

```
guardarMazo(nombre: string, username: string, cartas: string[]): Observable<Mazo> {
  // POST to /api/mazos/guardar
  const body = [ nombre, username, cartas ];
  return this.http.post<Mazo>(apiUrl + '/guardar', body);
}
```

**Parámetros**:
- `nombre: string` - Nombre del mazo (ej: "Fuego Rápido")
- `username: string` - Propietario del mazo
- `cartas: string[]` - Array de IDs de cartas exactamente 60

**Retorno**: `Observable<Mazo>` - Mazo guardado con ID asignado

**Endpoint**: `POST /api/mazos/guardar`

**Request Body**:
```json
{
  "nombre": "Fuego Rápido",
  "username": "Pikachu123",
  "cartas": [
    "xy1-001",
    "xy1-002",
    "base1-energy-fire",
    "base1-energy-fire",
    ... (60 cartas total)
  ]
}
```

**Response Ejemplo**:
```json
{
  "id": 42,
  "nombre": "Fuego Rápido",
  "username": "Pikachu123",
  "cartas": [
    {
      "id": "xy1-001",
      "nombre": "Pikachu",
      "hp": "40",
      "tipo": "Lightning"
    },
    ... (60 cartas)
  ],
  "totalCartas": 60
}
```

**Validaciones**:
- `cartas.length === 60` - Exactamente 60 cartas
- `nombre` no vacío
- `username` existe en BD
- Todas las cartas existen en catálogo
- Jugador posee las cartas (en colección)

**Errores**:
- 400: Cartas < 60 o > 60
- 401: No autenticado
- 404: Usuario no encontrado
- 409: Jugador no posee alguna carta

**Uso en componente**:
```typescript
guardarMazo() {
  const cartas = this.deckBuilder.getSelectedCardIds();
  
  if (cartas.length !== 60) {
    this.showError('Exactamente 60 cartas requeridas');
    return;
  }
  
  this.mazoService.guardarMazo(
    this.nombreMazo,
    this.username,
    cartas
  ).subscribe(
    (mazoGuardado) => {
      console.log(`Mazo guardado con ID: PARAM`);
      this.showSuccess('Mazo guardado exitosamente');
    },
    (error) => {
      this.showError(error.error.message);
    }
  );
}
```

---

### 2. getMazosByJugador(username: string)

**Obtener todos los mazos de un jugador**

```
getMazosByJugador(username: string): Observable<Mazo[]> {
  // GET /api/mazos/listar/[username]
  return this.http.get<Mazo[]>(apiUrl + '/listar/' + username);
}
```

**Parámetros**:
- `username: string` - Nombre de usuario

**Retorno**: `Observable<Mazo[]>` - Array de mazos del jugador

**Endpoint**: GET `/api/mazos/listar/[username]`

**Response Ejemplo**:
```json
[
  {
    "id": 1,
    "nombre": "Fuego Rápido",
    "username": "Pikachu123",
    "totalCartas": 60,
    "cartas": [...]
  },
  {
    "id": 2,
    "nombre": "Agua Control",
    "username": "Pikachu123",
    "totalCartas": 60,
    "cartas": [...]
  }
]
```

**Uso en componente**:
```typescript
export class MazosListComponent implements OnInit {
  mazos$: Observable<Mazo[]>;
  
  constructor(private mazoService: MazoService) {
    this.mazos$ = this.mazoService.getMazosByJugador('Pikachu123');
  }
  
  ngOnInit() {
    this.mazos$.subscribe(
      (mazos) => {
        console.log(`PARAM mazos encontrados`);
        this.mostrarMazos(mazos);
      }
    );
  }
}
```

**En Template (con async pipe)**:
```html
<div *ngIf="mazos$ | async as mazos">
  <div *ngFor="let mazo of mazos" class="mazo-card">
    <h3>[{ mazo.nombre ]}</h3>
    <p>[{ mazo.totalCartas ]} cartas</p>
    <button (click)="editarMazo(mazo.id)">Editar</button>
    <button (click)="eliminarMazo(mazo.id)">Eliminar</button>
  </div>
</div>
```

---

### 3. actualizarMazo(idMazo: number, nombre: string, cartasIds: string[])

**Actualizar un mazo existente (nombre y cartas)**

```
actualizarMazo(idMazo: number, nombre: string, cartasIds: string[]): Observable<Mazo> {
  // PUT /api/mazos/actualizar/[idMazo]
  const body = [ id: idMazo, nombre, cartasIds ];
  return this.http.put<Mazo>(apiUrl + '/actualizar/' + idMazo, body);
}
```

**Parámetros**:
- `idMazo: number` - ID del mazo a actualizar
- `nombre: string` - Nuevo nombre (puede ser igual)
- `cartasIds: string[]` - Nuevo set de cartas (60 exactas)

**Retorno**: `Observable<Mazo>` - Mazo actualizado

**Endpoint**: PUT `/api/mazos/actualizar/[idMazo]`

**Request Body**:
```json
{
  "id": 42,
  "nombre": "Fuego Rápido v2",
  "cartasIds": [
    "xy1-001",
    "xy1-002",
    ... (60 cartas)
  ]
}
```

**Response**: Mazo actualizado con nuevos campos

**Validaciones**:
- `idMazo` existe y pertenece al usuario
- `cartasIds.length === 60`
- Todas las cartas existen
- Usuario posee las cartas

**Uso en componente**:
```typescript
actualizarMazo() {
  const mazoActualizado = {
    id: this.mazoId,
    nombre: this.nombreInput.value,
    cartas: this.cartasSeleccionadas
  };
  
  if (mazoActualizado.cartas.length !== 60) {
    this.showError('Debe tener exactamente 60 cartas');
    return;
  }
  
  this.mazoService.actualizarMazo(
    mazoActualizado.id,
    mazoActualizado.nombre,
    mazoActualizado.cartas
  ).subscribe(
    (updated) => {
      this.showSuccess('Mazo actualizado');
    },
    (error) => {
      this.showError('Error al actualizar');
    }
  );
}
```

---

### 4. eliminarMazo(idMazo: number)

**Eliminar un mazo (no reversible)**

```
eliminarMazo(idMazo: number): Observable<void> {
  // DELETE /api/mazos/eliminar/[idMazo]
  return this.http.delete<void>(apiUrl + '/eliminar/' + idMazo);
}
```

**Parámetros**:
- `idMazo: number` - ID del mazo a eliminar

**Retorno**: `Observable<void>` - Sin contenido

**Endpoint**: DELETE `/api/mazos/eliminar/[idMazo]`

**Validaciones**:
- `idMazo` existe
- Mazo pertenece al usuario autenticado

**Errors**:
- 404: Mazo no existe
- 401: No tienes permiso para eliminar

**Uso en componente**:
```typescript
eliminarMazo(idMazo: number) {
  const confirmacion = confirm('¿Eliminar este mazo permanentemente?');
  
  if (!confirmacion) return;
  
  this.mazoService.eliminarMazo(idMazo).subscribe(
    () => {
      console.log(`Mazo PARAM eliminado`);
      this.refrescarListaMazos();
      this.showSuccess('Mazo eliminado');
    },
    (error) => {
      console.error('Error al eliminar:', error);
    }
  );
}
```

---

### 5. debugInjectCard(idMazo: number, cartaId: string, cartaAReemplazarId?: string | null)

**[DEBUG] Inyectar carta en mazo de pruebas**

```
debugInjectCard(idMazo: number, cartaId: string, cartaAReemplazarId?: string | null) {
  // POST /api/mazos/[idMazo]/debug/inject-card
  const body = [ cartaId, cartaAReemplazarId: cartaAReemplazarId ?? null ];
  return this.http.post<Mazo>(apiUrl + '/' + idMazo + '/debug/inject-card', body);
}
```

**Parámetros**:
- `idMazo: number` - ID del mazo
- `cartaId: string` - ID de la carta a agregar
- `cartaAReemplazarId?: string` - ID de carta a reemplazar (opcional)

**Retorno**: `Observable<Mazo>` - Mazo actualizado

**Endpoint**: POST `/api/mazos/[idMazo]/debug/inject-card`

**Request Body**:
```json
{
  "cartaId": "xy1-001",
  "cartaAReemplazarId": "xy1-002"
}
```

**Comportamiento**:
- Si `cartaAReemplazarId` no provided: reemplaza primera carta que no sea única
- Si `cartaAReemplazarId` provided: reemplaza esa carta específica
- Si carta a reemplazar no existe: error 400

**⚠️ Notas**:
- Solo disponible en desarrollo
- No debe usarse en producción
- Útil para testing de balances

---

## 📊 Tipo de Datos

### Mazo

```typescript
interface Mazo {
  id: number;              // ID único del mazo
  nombre: string;          // Nombre personalizado
  username?: string;       // Propietario (opcional en response)
  cartas: Card[];          // Array de 60 cartas
  totalCartas: number;     // Siempre 60
}
```

### Card (dentro de Mazo)

```typescript
interface Card {
  id: string;              // Identificador único
  nombre: string;          // Nombre
  hp: string;              // Puntos de vida
  tipo: string;            // Tipo de energía
  imagen: string;          // URL
  costoRetirada?: number;
  supertype?: string;      // Pokemon, Energy, Trainer
}
```

---

## 🎯 Casos de Uso

### Caso 1: Flujo completo de construcción

```typescript
export class DeckBuilderComponent implements OnInit {
  selectedCards: string[] = [];
  cartasDisponibles: Card[] = [];
  mazoName = '';

  constructor(
    private cardService: CardService,
    private mazoService: MazoService
  ) {}

  ngOnInit() {
    // Cargar catálogo de cartas
    this.cardService.getAll().subscribe(
      (cartas) => {
        this.cartasDisponibles = cartas;
      }
    );
  }

  agregarCarta(cartaId: string) {
    if (this.selectedCards.length < 60) {
      this.selectedCards.push(cartaId);
    }
  }

  removerCarta(index: number) {
    this.selectedCards.splice(index, 1);
  }

  guardar() {
    if (this.selectedCards.length !== 60) {
      alert('Requiere exactamente 60 cartas');
      return;
    }

    this.mazoService.guardarMazo(
      this.mazoName,
      'Pikachu123',
      this.selectedCards
    ).subscribe(
      (mazoGuardado) => {
        console.log('Mazo guardado:', mazoGuardado.id);
      }
    );
  }
}
```

### Caso 2: Listar y seleccionar mazo para batalla

```typescript
export class SelectDeckComponent implements OnInit {
  mazos: Mazo[] = [];
  selectedMazo: Mazo | null = null;

  constructor(
    private mazoService: MazoService,
    private battleService: BattleService
  ) {}

  ngOnInit() {
    this.mazoService.getMazosByJugador('Pikachu123').subscribe(
      (mazos) => {
        this.mazos = mazos;
      }
    );
  }

  seleccionar(mazo: Mazo) {
    this.selectedMazo = mazo;
  }

  iniciarBatalla() {
    if (!this.selectedMazo) return;

    this.battleService.startBattle({
      mazoId: this.selectedMazo.id
    }).subscribe(
      (batalla) => {
        console.log('Batalla iniciada');
      }
    );
  }
}
```

### Caso 3: Editor de mazo

```typescript
editarMazo(mazoId: number) {
  // 1. Cargar mazo actual
  const mazo = this.mazos.find(m => m.id === mazoId);
  if (!mazo) return;

  this.selectedCards = [...mazo.cartas.map(c => c.id)];
  this.mazoName = mazo.nombre;

  // 2. Permitir edición
  // ... UI para cambiar cartas ...

  // 3. Guardar cambios
  const button = document.getElementById('save-btn');
  button?.addEventListener('click', () => {
    this.mazoService.actualizarMazo(
      mazoId,
      this.mazoName,
      this.selectedCards
    ).subscribe(
      () => {
        console.log('Mazo actualizado');
        this.refrescarListaMazos();
      }
    );
  });
}
```

### Caso 4: Gestión de múltiples mazos

```typescript
organizarMazos() {
  this.mazoService.getMazosByJugador(this.username).pipe(
    map(mazos => {
      // Filtrar por tipo
      return {
        rapidos: mazos.filter(m => m.nombre.includes('Rápido')),
        defensivos: mazos.filter(m => m.nombre.includes('Defensa')),
        mixtos: mazos.filter(m => !m.nombre.includes('Rápido') && !m.nombre.includes('Defensa'))
      };
    }),
    tap(categorias => {
      this.mazosPorTipo = categorias;
    })
  ).subscribe();
}
```

---

## 🔄 Integración con Otros Servicios

### Con CardService

```typescript
cargarMazoConDetalles(mazoId: number) {
  const mazo = this.mazos.find(m => m.id === mazoId);
  if (!mazo) return;

  // Obtener catálogo completo
  this.cardService.getAll().pipe(
    map(catalogo => {
      // Enriquecer cartas del mazo
      return mazo.cartas.map(cartaRef => {
        return catalogo.find(c => c.id === cartaRef.id) || cartaRef;
      });
    })
  ).subscribe(cartasEnriquecidas => {
    this.cartasDelMazo = cartasEnriquecidas;
  });
}
```

### Con BattleService

```typescript
// Iniciar batalla con mazo
iniciarBatalla(mazoId: number) {
  this.mazoService.getMazosByJugador(this.username).pipe(
    map(mazos => mazos.find(m => m.id === mazoId)),
    switchMap(mazo => 
      this.battleService.startBattle({
        mazoId: mazo!.id
      })
    )
  ).subscribe(
    (batalla) => {
      this.router.navigate(['/batalla', batalla.id]);
    }
  );
}
```

---

## ⚠️ Validaciones y Restricciones

### Reglas de Mazo

```
┌─────────────────────────────────┐
│      REGLAS DE CONSTRUCCIÓN     │
├─────────────────────────────────┤
│ Total de cartas: 60 exactas     │
│ Máximo copies de una carta: 4   │
│ Mínimo pokémon: 10              │
│ Mínimo energías: 15             │
│ Todas las cartas deben estar    │
│   en la colección del jugador   │
└─────────────────────────────────┘
```

### Backend valida

```typescript
// En backend (MazoService)
if (cartas.length !== 60) throw new Error("60 cartas requeridas");
if (!jugadorPoseeTodasLasCartas(usuario, cartas)) throw new Error("Cartas no disponibles");
if (getMaxCopiesDetectadas(cartas) > 4) throw new Error("Máximo 4 copies por carta");
```

---

## 📊 Estados y Transiciones

```
VACÍO → CONSTRUCCIÓN → VALIDACIÓN → GUARDADO → DISPONIBLE
  │          │                            │
  └──────────┴────────────────────────────┴──→ ELIMINADO
```

---

## 🎨 Patterns Recomendados

### Con RxJS shareReplay para caché

```typescript
private mazosCache$ = this.mazoService
  .getMazosByJugador(username)
  .pipe(shareReplay(1));

getMazos() {
  return this.mazosCache$;
}
```

### Con BehaviorSubject para estado local

```typescript
private mazoSeleccionado$ = new BehaviorSubject<Mazo | null>(null);

seleccionar(mazo: Mazo) {
  this.mazoSeleccionado$.next(mazo);
}

getSeleccionado() {
  return this.mazoSeleccionado$.asObservable();
}
```

---

## ⚠️ Notas Importantes

**Restricciones de edición**:
- No puedes editar un mazo en uso (en batalla activa)
- No puedes editar mazo de otro jugador
- Necesitas poseer todas las cartas

**Performance**:
- `getMazosByJugador` retorna cartas completas (puede ser grande)
- Considera usar `shareReplay(1)` en componentes
- No refresques lista en cada cambio

**Seguridad**:
- Backend valida permisos (ownership)
- Frontend debe validar también para UX
- Nunca confíes solo en validación frontend

---

## 📋 Métodos Resumen

| Método | Parámetros | Retorna | Endpoint |
|--------|-----------|---------|----------|
| `guardarMazo()` | nombre, username, cartas | `Observable<Mazo>` | POST /guardar |
| `getMazosByJugador()` | username | `Observable<Mazo[]>` | GET /listar/[username] |
| `actualizarMazo()` | id, nombre, cartasIds | `Observable<Mazo>` | PUT /actualizar/[id] |
| `eliminarMazo()` | id | `Observable<void>` | DELETE /eliminar/[id] |
| `debugInjectCard()` | id, cartaId, reemplazo? | `Observable<Mazo>` | POST /[id]/debug/inject-card |

---

*Próximo: [BattleService](/docs/componentes-detallados/frontend/services/06-battle-service)*
