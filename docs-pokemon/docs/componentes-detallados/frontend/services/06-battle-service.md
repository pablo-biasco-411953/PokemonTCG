---
sidebar_position: 6
title: ⚔️ BattleService
---

# ⚔️ BattleService - Servicio de Batalla

> Servicio Angular para todas las operaciones de combate: inicio, setup, turnos, ataques y sincronización

---

## 📍 Ubicación

`frontend/src/app/features/battle/services/battle.service.ts`

---

## 🏗️ Definición del Servicio

```typescript
@Injectable({ providedIn: 'root' })
export class BattleService {
  private base = `${getBackendUrl()}/api/battle`;
  
  constructor(private http: HttpClient) {}
  
  private getHeaders(): { headers: HttpHeaders } {
    // Extrae username del localStorage y lo envía en header
    const data = localStorage.getItem('jugador');
    const username = JSON.parse(data).username || '';
    return {
      headers: new HttpHeaders({
        'X-Username': username
      })
    };
  }
}
```

**Tipo**: Servicio raíz
**Dependencias**: HttpClient, HttpHeaders
**Feature**: Battle (Sistema de combate)
**Autenticación**: Headers con X-Username

---

## 📡 Métodos Principales

### SECCIÓN 1: INICIO DE BATALLA

#### startBattle(username: string, mazoId: number)

**Iniciar una batalla de un jugador vs Bot**

```
startBattle(username: string, mazoId: number): Observable<Partida> {
  // POST /api/battle/start/[username]
  return this.http.post<Partida>(base + '/start/' + username, { mazoId }, headers);
}
```

**Parámetros**:
- `username: string` - Jugador atacante
- `mazoId: number` - ID del mazo a usar

**Retorno**: `Observable<Partida>` - Batalla inicializada

**Endpoint**: `POST /api/battle/start/[username]

**Request Body**:
```json
{ "mazoId": 42 }
```

**Response Ejemplo**:
```json
{
  "id": "match-001",
  "matchId": "match-001",
  "estado": "INICIANDO",
  "jugador1": "Pikachu123",
  "jugador2": "AI_Bot",
  "turno": 0,
  "turnoJugador": "Pikachu123",
  "bancoPJ1": [],
  "activoPJ1": null,
  "discardPJ1": [],
  "bancoPJ2": [],
  "activoPJ2": null,
  "discardPJ2": []
}
```

**Validaciones**:
- `username` existe
- `mazoId` existe y pertenece al usuario
- 60 cartas en mazo

**Uso**:
```typescript
iniciarBatalla() {
  this.battleService.startBattle('Pikachu123', 42).subscribe(
    (batalla) => {
      this.matchId = batalla.matchId;
      this.estado = batalla.estado;
      console.log(`Batalla iniciada: ${batalla.matchId}`);
    }
  );
}
```

---

#### startBattleOnline(player1: string, player1MazoId: number, player2: string, player2MazoId: number)

**Iniciar batalla de dos jugadores**

```typescript
startBattleOnline(
  player1: string,
  player1MazoId: number,
  player2: string,
  player2MazoId: number
): Observable<Partida> {
  return this.http.post<Partida>(
    `${this.base}/start-online`,
    { player1, player1MazoId, player2, player2MazoId },
    this.getHeaders()
  );
}
```

**Parámetros**:
- `player1: string` - Primer jugador
- `player1MazoId: number` - Mazo de player1
- `player2: string` - Segundo jugador
- `player2MazoId: number` - Mazo de player2

**Retorno**: `Observable<Partida>` - Batalla inicializada

**Endpoint**: `POST /api/battle/start-online`

**Request Body**:
```json
{
  "player1": "Pikachu123",
  "player1MazoId": 42,
  "player2": "Charizard456",
  "player2MazoId": 85
}
```

---

### SECCIÓN 2: ESTADO Y SINCRONIZACIÓN

#### getState(matchId: string)

**Obtener estado completo de la batalla**

```
getState(matchId: string): Observable<Partida> {
  // GET /api/battle/state/[matchId]
  return this.http.get<Partida>(base + '/state/' + matchId, headers);
}
```

**Parámetros**:
- `matchId: string` - ID de la batalla

**Retorno**: `Observable<Partida>` - Estado actual

**Endpoint**: GET `/api/battle/state/[matchId]`

**Response**: Objeto Partida completo con todos los campos

**Uso en componente**:
```typescript
cargarEstado() {
  this.battleService.getState(this.matchId).subscribe(
    (partida) => {
      this.estado = partida;
      this.renderizarTablero(partida);
    }
  );
}
```

---

#### heartbeat(matchId: string)

**Sincronización periódica (keep-alive)**

```typescript
heartbeat(matchId: string): Observable<Partida> {
  return this.http.post<Partida>(
    `${this.base}/$[matchId]/heartbeat`,
    {},
    this.getHeaders()
  );
}
```

**Parámetros**:
- `matchId: string` - ID de la batalla

**Retorno**: `Observable<Partida>` - Estado actual

**Uso típico** (cada 5 segundos):
```typescript
ngOnInit() {
  setInterval(() => {
    this.battleService.heartbeat(this.matchId).subscribe(
      (partida) => {
        if (partida.estado === 'TERMINADA') {
          this.terminarBatalla(partida);
        }
      }
    );
  }, 5000);
}
```

---

#### surrender(matchId: string)

**Rendirse en la batalla**

```typescript
surrender(matchId: string): Observable<Partida> {
  return this.http.post<Partida>(
    `${this.base}/$[matchId]/surrender`,
    {},
    this.getHeaders()
  );
}
```

**Parámetros**:
- `matchId: string` - ID de la batalla

**Retorno**: `Observable<Partida>` - Batalla con estado TERMINADA

**Efectos**:
- Batalla termina inmediatamente
- Oponente gana
- Se registran estadísticas

---

### SECCIÓN 3: SETUP (PREPARACIÓN INICIAL)

#### evaluateSetup(matchId: string)

**Evaluar hand inicial (validar 7 cartas sin Pokémon)**

```typescript
evaluateSetup(matchId: string): Observable<Partida> {
  return this.http.post<Partida>(
    `${this.base}/$[matchId]/setup/evaluate`,
    {},
    this.getHeaders()
  );
}
```

**Validación**:
- Si hand tiene 0 pokémon básicos → mulligan requerida
- Si hand válida → pasar a setup

---

#### executeMulligan(matchId: string)

**Ejecutar mulligan (barajear hand y redibujar 7)**

```typescript
executeMulligan(matchId: string): Observable<Partida> {
  return this.http.post<Partida>(
    `${this.base}/$[matchId]/setup/execute-mulligan`,
    {},
    this.getHeaders()
  );
}
```

**Secuencia**:
1. Evaluatesetup detecta 0 pokémon
2. executeMulligan rebarajea
3. Si sigue sin pokémon → segunda mulligan
4. Si tiene pokémon → continuar

---

#### extraDraw(matchId: string, cantidad: number)

**Dibujar cartas adicionales (por mulligan del rival)**

```typescript
extraDraw(matchId: string, cantidad: number): Observable<Partida> {
  return this.http.post<Partida>(
    `${this.base}/$[matchId]/setup/extra-draw`,
    { cantidad },
    this.getHeaders()
  );
}
```

**Parámetros**:
- `cantidad: number` - Cartas extra a dibujar (usualmente = mulligans del rival)

---

#### placePrizes(matchId: string)

**Colocar cartas de premio (6 boca abajo)**

```typescript
placePrizes(matchId: string): Observable<Partida> {
  return this.http.post<Partida>(
    `${this.base}/$[matchId]/setup/place-prizes`,
    {},
    this.getHeaders()
  );
}
```

**Acción**: Automaticamente selecciona 6 cartas del deck y las coloca de premio

---

#### placeActiveSetup(matchId: string, cartaId: string)

**Colocar Pokémon en posición activa**

```typescript
placeActiveSetup(matchId: string, cartaId: string): Observable<Partida> {
  return this.http.post<Partida>(
    `${this.base}/$[matchId]/setup/place-active`,
    { cartaId },
    this.getHeaders()
  );
}
```

**Parámetros**:
- `cartaId: string` - ID del pokémon básico

**Validaciones**:
- Carta debe estar en mano
- Debe ser Pokémon básico

---

#### placeBenchSetup(matchId: string, cartaId: string)

**Agregar Pokémon a la banca (máximo 5)**

```typescript
placeBenchSetup(matchId: string, cartaId: string): Observable<Partida> {
  return this.http.post<Partida>(
    `${this.base}/$[matchId]/setup/place-bench`,
    { cartaId },
    this.getHeaders()
  );
}
```

**Parámetros**:
- `cartaId: string` - ID del pokémon básico

**Validaciones**:
- Banca < 5 pokémon
- Carta en mano
- Pokémon básico

---

#### confirmBenchSetup(matchId: string)

**Confirmar setup de banca (finalizar setup personal)**

```typescript
confirmBenchSetup(matchId: string): Observable<Partida> {
  return this.http.post<Partida>(
    `${this.base}/$[matchId]/setup/confirm-bench`,
    {},
    this.getHeaders()
  );
}
```

---

#### revealSetup(matchId: string)

**Revelar setup al oponente (iniciar batalla)**

```typescript
revealSetup(matchId: string): Observable<Partida> {
  return this.http.post<Partida>(
    `${this.base}/$[matchId]/setup/reveal`,
    {},
    this.getHeaders()
  );
}
```

**Secuencia completa de Setup**:
```
evaluateSetup → [mulligan si es necesario]
  ↓
placePrizes → placeActiveSetup → placeBenchSetup (múltiples) → confirmBenchSetup
  ↓
revealSetup ← (cuando ambos jugadores confirman)
  ↓
[BATALLA INICIA - Lanzar moneda]
```

---

### SECCIÓN 4: COIN FLIP Y TURNO INICIAL

#### lanzarMoneda(matchId: string, eleccion: 'CARA' | 'CRUZ')

**Lanzar moneda para decidir quién comienza**

```typescript
lanzarMoneda(
  matchId: string,
  eleccion: 'CARA' | 'CRUZ'
): Observable<Partida> {
  return this.http.post<Partida>(
    `${this.base}/$[matchId]/coin-flip`,
    { eleccion },
    this.getHeaders()
  );
}
```

**Parámetros**:
- `eleccion: 'CARA' | 'CRUZ'` - Predicción del jugador

**Response**:
- Si ganó coin flip → `turnoJugador = username`
- Si perdió → `turnoJugador = oponente`

---

#### actualizarHandshakeMoneda(matchId: string, holding: boolean, power: number)

**Actualizacion visual del handshake previo a coin flip**

```typescript
actualizarHandshakeMoneda(
  matchId: string,
  holding: boolean,
  power: number
): Observable<Partida> {
  return this.http.post<Partida>(
    `${this.base}/$[matchId]/coin-handshake`,
    { holding, power },
    this.getHeaders()
  );
}
```

**Parámetros**:
- `holding: boolean` - Está sosteniendo el botón
- `power: number` - Potencia del apretón (0-100)

**Uso** (animación):
```typescript
onMouseDown() {
  this.holding = true;
  this.power = 0;
  this.powerInterval = setInterval(() => {
    if (this.holding) {
      this.power = Math.min(this.power + 5, 100);
      this.battleService.actualizarHandshakeMoneda(
        this.matchId,
        true,
        this.power
      ).subscribe();
    }
  }, 50);
}
```

---

#### actualizarLoading(matchId: string, percentage: number)

**Actualizar porcentaje de carga (efecto visual)**

```typescript
actualizarLoading(matchId: string, percentage: number): Observable<Partida> {
  return this.http.post<Partida>(
    `${this.base}/$[matchId]/loading`,
    { percentage },
    this.getHeaders()
  );
}
```

**Parámetros**:
- `percentage: number` - Porcentaje (0-100)

---

#### elegirTurno(matchId: string, vaPrimero: boolean)

**Elegir quién toma el primer turno (después de ganar coin flip)**

```typescript
elegirTurno(matchId: string, vaPrimero: boolean): Observable<void> {
  return this.http.post<void>(
    `${this.base}/$[matchId]/choose-turn`,
    { vaPrimero },
    this.getHeaders()
  );
}
```

**Parámetros**:
- `vaPrimero: boolean` - true = quiero ir primero, false = quiero ir segundo

**Nota**: Solo se llama si el jugador ganó el coin flip

---

### SECCIÓN 5: ACCIONES DE JUEGO EN TURNO

#### jugarPokemon(matchId: string, cartaId: string)

**Bajar Pokémon desde mano al tablero**

```typescript
jugarPokemon(matchId: string, cartaId: string): Observable<void> {
  return this.http.post<void>(
    `${this.base}/$[matchId]/play-pokemon`,
    { cartaId },
    this.getHeaders()
  );
}
```

**Parámetros**:
- `cartaId: string` - ID del pokémon en mano

**Validaciones**:
- Pokémon es básico (a menos que haya evolucionable base)
- Espacio en banca (máx 5)
- En turno del jugador

---

#### unirEnergia(matchId: string, cartaId: string, energiaId: string)

**Unir energía a Pokémon**

```typescript
unirEnergia(
  matchId: string,
  cartaId: string,
  energiaId: string
): Observable<void> {
  return this.http.post<void>(
    `${this.base}/$[matchId]/attach-energy`,
    { cartaId, energiaId },
    this.getHeaders()
  );
}
```

**Parámetros**:
- `cartaId: string` - ID del pokémon (activo o banca)
- `energiaId: string` - ID de la energía en mano

**Validaciones**:
- 1 energía por turno máximo
- Energía en mano
- Pokémon en tablero

---

#### atacar(matchId: string, nombreAtaque: string)

**Ejecutar ataque**

```typescript
atacar(matchId: string, nombreAtaque: string): Observable<void> {
  const url = `${this.base}/$[matchId]/attack?nombreAtaque=${encodeURIComponent(nombreAtaque)}`;
  return this.http.post<void>(url, {}, this.getHeaders());
}
```

**Parámetros**:
- `nombreAtaque: string` - Nombre del ataque (ej: "Thunderbolt")

**Query param**: `nombreAtaque` URL-encoded

**Validaciones**:
- Pokémon activo posee el ataque
- Suficientes energías requeridas
- Es el turno del jugador

**Flujo de daño**:
1. Backend calcula daño base
2. Aplica debilidades (+20%)
3. Aplica resistencias (-20)
4. Aplica efectos especiales
5. Descuenta HP
6. Verifica KO
7. Retorna estado actualizado

---

#### subirAActivo(matchId: string, cartaId: string)

**Promover pokémon desde banca a activo (después de KO)**

```typescript
subirAActivo(matchId: string, cartaId: string): Observable<void> {
  return this.http.post<void>(
    `${this.base}/$[matchId]/promote`,
    cartaId,
    this.getHeaders()
  );
}
```

**Parámetros**:
- `cartaId: string` - ID del pokémon en banca

**Nota**: Se llama automáticamente cuando Pokémon activo es KO'd

---

#### retirarPokemon(matchId: string, nuevoActivoId: string)

**Retirar pokémon activo (si tiene energía de retirada)**

```typescript
retirarPokemon(matchId: string, nuevoActivoId: string): Observable<void> {
  return this.http.post<void>(
    `${this.base}/$[matchId]/retreat`,
    nuevoActivoId,
    this.getHeaders()
  );
}
```

**Parámetros**:
- `nuevoActivoId: string` - ID del pokémon de banca a subir

**Validaciones**:
- Pokémon activo tiene suficiente energía de retirada
- nuevoActivo está en banca
- Es el turno del jugador

---

#### evolucionar(matchId: string, cartaManoId: string, cartaTableroId: string)

**Evolucionar pokémon usando carta de mano**

```typescript
evolucionar(
  matchId: string,
  cartaManoId: string,
  cartaTableroId: string
): Observable<void> {
  return this.http.post<void>(
    `${this.base}/$[matchId]/evolve`,
    { cartaManoId, cartaTableroId },
    this.getHeaders()
  );
}
```

**Parámetros**:
- `cartaManoId: string` - Carta de evolución en mano
- `cartaTableroId: string` - Pokémon base en tablero

**Validaciones**:
- cartaTableroId.evolvesFrom === cartaManoId.id
- cartaManoId en mano
- cartaTableroId en tablero (activo o banca)
- Pokémon no evolucionó este turno

---

#### pasarTurno(matchId: string)

**Finalizar turno actual**

```typescript
pasarTurno(matchId: string): Observable<void> {
  return this.http.post<void>(
    `${this.base}/$[matchId]/pass-turn`,
    {},
    this.getHeaders()
  );
}
```

**Efectos**:
- `turno++`
- `turnoJugador` alterna
- Bot juega su turno (si es vs AI)
- Fase de draw automática

---

### SECCIÓN 6: ACCIONES DE BOT

#### jugarBot(matchId: string)

**Ejecutar turno completo del Bot**

```typescript
jugarBot(matchId: string): Observable<Partida> {
  return this.http.post<Partida>(
    `${this.base}/$[matchId]/jugar-bot`,
    {},
    this.getHeaders()
  );
}
```

**Retorno**: `Observable<Partida>` - Batalla con turno de Bot completo

**Uso**:
```typescript
finalizarMiTurno() {
  this.battleService.pasarTurno(this.matchId).subscribe(() => {
    // Esperar turno del bot
    this.battleService.jugarBot(this.matchId).subscribe(
      (partida) => {
        this.estado = partida;
        // Ahora es nuestro turno de nuevo
      }
    );
  });
}
```

---

#### jugarBotSetup(matchId: string)

**Ejecutar setup del Bot**

```typescript
jugarBotSetup(matchId: string): Observable<Partida> {
  return this.http.post<Partida>(
    `${this.base}/$[matchId]/jugar-bot-setup`,
    {},
    this.getHeaders()
  );
}
```

**Secuencia típica**:
1. Usuario finaliza setup con revealSetup
2. Backend ejecuta Bot setup automáticamente
3. Retorna estado listo para coin flip

---

### SECCIÓN 7: DEBUG (SOLO DESARROLLO)

#### debugDrawCard(matchId: string, cardId: string)

**[DEBUG] Forzar una carta a la mano**

```typescript
debugDrawCard(matchId: string, cardId: string): Observable<Partida> {
  return this.http.post<Partida>(
    `${this.base}/$[matchId]/debug/draw`,
    { cardId },
    this.getHeaders()
  );
}
```

---

#### debugForzarEstado(matchId: string, objetivo: string, estado: string)

**[DEBUG] Aplicar estado especial (BURN, POISON, etc.)**

```typescript
debugForzarEstado(
  matchId: string,
  objetivo: string,
  estado: string
): Observable<Partida> {
  return this.http.post<Partida>(
    `${this.base}/$[matchId]/debug/status`,
    { objetivo, estado },
    this.getHeaders()
  );
}
```

**Parámetros**:
- `objetivo: string` - ID del pokémon
- `estado: string` - BURN | POISON | PARALYSIS | CONFUSION | SLEEP

---

#### debugSetHp(matchId: string, objetivo: string, hp: number)

**[DEBUG] Ajustar HP de pokémon**

```typescript
debugSetHp(matchId: string, objetivo: string, hp: number): Observable<Partida> {
  return this.http.post<Partida>(
    `${this.base}/$[matchId]/debug/hp`,
    { objetivo, hp },
    this.getHeaders()
  );
}
```

**Parámetros**:
- `objetivo: string` - ID del pokémon
- `hp: number` - Nuevo valor de HP

---

#### getCardCatalogDebug()

**[DEBUG] Obtener catálogo de cartas para panel debug**

```typescript
getCardCatalogDebug(): Observable<Card[]> {
  return this.http.get<Card[]>(
    `${this.base}/debug/catalog`,
    this.getHeaders()
  );
}
```

---

## 📊 Tipo de Datos

### Partida (Battle State)

```typescript
interface Partida {
  id: string;                // UUID de batalla
  matchId: string;           // ID de matchmaking
  estado: 'INICIANDO' | 'EN_CURSO' | 'TERMINADA';
  jugador1: string;          // Username
  jugador2: string;          // Username o "AI_Bot"
  turno: number;             // Número de turno actual
  turnoJugador: string;      // Quién tiene turno
  
  // Tablero Jugador 1
  bancoPJ1: CartaEnJuego[];  // 0-5 pokémon en banca
  activoPJ1: CartaEnJuego;   // Pokémon activo (batallando)
  discardPJ1: Card[];        // Cementerio
  
  // Tablero Jugador 2
  bancoPJ2: CartaEnJuego[];
  activoPJ2: CartaEnJuego;
  discardPJ2: Card[];
}

interface CartaEnJuego {
  id: string;                // Card ID
  nombre: string;            // Nombre
  hp: number;                // HP máximo
  hpActual: number;          // HP actual
  energias: string[];        // IDs de energías unidas
  estadoEspecial?: string;   // BURN, POISON, etc
  ataques?: Ataque[];        // Ataques disponibles
}
```

---

## 🎯 Flujo Completo de Batalla

```
┌─────────────────────────────────────────────────────────┐
│                 FLUJO DE BATALLA COMPLETO               │
├─────────────────────────────────────────────────────────┤
│                                                         │
│ 1. INICIO                                              │
│    startBattle(username, mazoId)                       │
│    ↓                                                   │
│ 2. SETUP (Ambos jugadores)                            │
│    ├─ evaluateSetup()                                 │
│    ├─ [ejecuteMulligan() si es necesario]             │
│    ├─ placePrizes()                                   │
│    ├─ placeActiveSetup(pokémon)                       │
│    ├─ placeBenchSetup() × múltiples                   │
│    ├─ confirmBenchSetup()                             │
│    └─ revealSetup()                                   │
│    ↓                                                   │
│ 3. COIN FLIP                                           │
│    ├─ lanzarMoneda()                                  │
│    └─ elegirTurno()                                   │
│    ↓                                                   │
│ 4. BATALLA PRINCIPAL (loop hasta fin)                 │
│    ├─ jugadorActivo.turno() {                         │
│    │   ├─ jugarPokemon() (opcional)                   │
│    │   ├─ unirEnergia() (1 por turno)                 │
│    │   ├─ evolucionar() (opcional)                    │
│    │   ├─ atacar()                                    │
│    │   └─ pasarTurno()                                │
│    │ }                                                │
│    ├─ [subirAActivo() si hay KO]                      │
│    ├─ jugadorOpuesto.turno()                          │
│    └─ [repetir hasta victoria]                        │
│    ↓                                                   │
│ 5. FINAL                                               │
│    estado === 'TERMINADA'                             │
│    [Registrar estadísticas]                           │
│                                                        │
└─────────────────────────────────────────────────────────┘
```

---

## 💾 Patterns Recomendados

### Sincronización con Polling

```typescript
private pollingInterval: number = 2000; // 2 segundos

initializePolling() {
  this.polling$ = interval(this.pollingInterval)
    .pipe(
      switchMap(() => this.battleService.getState(this.matchId)),
      tap(estado => {
        this.battleState = estado;
        this.checkGameEnd(estado);
      }),
      catchError((error) => {
        console.error('Polling error:', error);
        return EMPTY;
      })
    )
    .subscribe();
}

stopPolling() {
  this.polling$.unsubscribe();
}
```

### Con BehaviorSubject para Estado Reactivo

```typescript
private battleState$ = new BehaviorSubject<Partida | null>(null);

getBattleState$() {
  return this.battleState$.asObservable();
}

loadBattle(matchId: string) {
  this.battleService.getState(matchId)
    .pipe(
      tap(estado => this.battleState$.next(estado))
    )
    .subscribe();
}
```

---

## ⚠️ Notas Importantes

**Autenticación**:
- Todos los métodos usan `getHeaders()` que extrae username del localStorage
- Header `X-Username` usado para validar dueño de batalla

**Validaciones Backend**:
- Verifica ownership de batalla
- Verifica turno actual (no puedes jugar en turno del oponente)
- Verifica estado de batalla (no atacar si no es turno de batalla)

**Manejo de Errores Comunes**:
- 404: Batalla no existe
- 400: Acción inválida (ej: atacar sin energía)
- 401: No autenticado / no eres el jugador

**Performance**:
- Polling frecuente puede sobrecargar servidor
- Considerar aumentar intervalo a 3-5 segundos
- Usar `distinctUntilChanged()` para evitar updates innecesarios

---

## 📋 Resumen de Métodos

| Categoría | Método | Parámetros |
|-----------|--------|-----------|
| **Inicio** | `startBattle()` | username, mazoId |
| | `startBattleOnline()` | player1, player1MazoId, player2, player2MazoId |
| **Estado** | `getState()` | matchId |
| | `heartbeat()` | matchId |
| | `surrender()` | matchId |
| **Setup** | `evaluateSetup()` | matchId |
| | `executeMulligan()` | matchId |
| | `extraDraw()` | matchId, cantidad |
| | `placePrizes()` | matchId |
| | `placeActiveSetup()` | matchId, cartaId |
| | `placeBenchSetup()` | matchId, cartaId |
| | `confirmBenchSetup()` | matchId |
| | `revealSetup()` | matchId |
| **Coin Flip** | `lanzarMoneda()` | matchId, eleccion |
| | `actualizarHandshakeMoneda()` | matchId, holding, power |
| | `actualizarLoading()` | matchId, percentage |
| | `elegirTurno()` | matchId, vaPrimero |
| **Juego** | `jugarPokemon()` | matchId, cartaId |
| | `unirEnergia()` | matchId, cartaId, energiaId |
| | `atacar()` | matchId, nombreAtaque |
| | `subirAActivo()` | matchId, cartaId |
| | `retirarPokemon()` | matchId, nuevoActivoId |
| | `evolucionar()` | matchId, cartaManoId, cartaTableroId |
| | `pasarTurno()` | matchId |
| **Bot** | `jugarBot()` | matchId |
| | `jugarBotSetup()` | matchId |
| **Debug** | `debugDrawCard()` | matchId, cardId |
| | `debugForzarEstado()` | matchId, objetivo, estado |
| | `debugSetHp()` | matchId, objetivo, hp |
| | `getCardCatalogDebug()` | — |

---

---

## 🧩 Sub-servicios de BattleBoard

`BattleService` es el servicio HTTP base del sistema de batalla. Los sub-servicios de BattleBoard lo usan como dependencia para ejecutar acciones y luego recargar el estado.

| Sub-servicio | Responsabilidad | Métodos | Doc |
|-------------|-----------------|---------|-----|
| `BattleBoardStateService` | Predicados puros sobre el estado (sin HTTP) | 5 | [08](./08-battle-board-state-service.md) |
| `BattleBoardActionService` | Decisión de acción por carta y operaciones HTTP de juego | 8 | [09](./09-battle-board-action-service.md) |
| `BattleBoardAttackService` | Análisis de ataques: coin flip, validación de energías | 4 | [10](./10-battle-board-attack-service.md) |
| `BattleBoardCombatService` | Combate, confusión, turno del bot | 6 | [11](./11-battle-board-combat-service.md) |
| `BattleBoardTurnService` | Lógica de coin flip, sueño, análisis de turno | 9 | [12](./12-battle-board-turn-service.md) |
| `BattleBoardUiService` | Presentación: sprites, HP, colores de energía | 11 | [13](./13-battle-board-ui-service.md) |

### BattleService como orquestador central

```
                        ┌─────────────────┐
                        │  BattleService  │  ← HTTP (fuente de verdad)
                        └────────┬────────┘
               ┌─────────────────┼─────────────────┐
               ▼                 ▼                 ▼
    ┌──────────────────┐ ┌───────────────┐ ┌───────────────────┐
    │ ActionService    │ │ CombatService │ │ (otros servicios) │
    │ xxxYRecargar()   │ │ atacarYRecar. │ │                   │
    └────────┬─────────┘ └───────────────┘ └───────────────────┘
             │
             ▼
    ┌──────────────────┐
    │  StateService    │  ← Predicados puros (sin HTTP)
    └──────────────────┘
```

### Patrón de dependencias

```
BattleBoardActionService
  ├── BattleService (HTTP)
  ├── BattleBoardStateService
  └── I18nService

BattleBoardCombatService
  └── BattleService (HTTP)

BattleBoardTurnService
  └── (sin dependencias inyectadas)

BattleBoardAttackService
  └── (sin dependencias inyectadas)

BattleBoardStateService
  └── (sin dependencias inyectadas)

BattleBoardUiService
  └── DomSanitizer
```

---

*Próximo: [07-lobby-room-service.md](./07-lobby-room-service.md)*
