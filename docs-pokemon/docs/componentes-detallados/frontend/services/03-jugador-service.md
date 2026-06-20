---
sidebar_position: 3
title: 👤 JugadorService
---

# 👤 JugadorService - Servicio de Jugador

> Servicio Angular para obtener datos del perfil, gestionar colección, monedas, misiones y customización

---

## 📍 Ubicación

`frontend/src/app/core/services/jugador.service.ts`

---

## 🏗️ Definición del Servicio

```typescript
@Injectable({ providedIn: 'root' })
export class JugadorService {
  private apiUrl = `${getBackendUrl()}/api/jugadores`;
  
  constructor(private http: HttpClient) {}
}
```

**Tipo**: Servicio raíz
**Dependencias**: HttpClient
**Inyectables en**: Raíz (disponible globalmente)

---

## 📡 Métodos Principales

### 1. getJugador(username: string)

**Obtener resumen del jugador (datos públicos)**

```typescript
getJugador(username: string): Observable<JugadorDatosResponse> {
  return this.http.get<JugadorDatosResponse>(
    `${this.apiUrl}/${username}/datos`,
    { withCredentials: true }
  );
}
```

**Parámetros**:
- `username: string` - Nombre de usuario

**Retorno**: `Observable<JugadorDatosResponse>` - Datos del jugador

**Endpoint**: GET `/api/jugadores/[username]/datos`

**Response Ejemplo**:
```json
{
  "id": 1,
  "username": "Pikachu123",
  "email": "pikachu@example.com",
  "sobresDisponibles": 5,
  "santoroPoints": 250,
  "characterId": "pikachu",
  "skinColor": "#FFD700",
  "hairColor": "#000000",
  "eyeColor": "#0066CC"
}
```

**Opciones**:
- `withCredentials: true` - Envía cookies con la solicitud (autenticación)

**Uso en componente**:
```typescript
this.jugadorService.getJugador('Pikachu123').subscribe(
  (datos: JugadorDatosResponse) => {
    console.log(`Jugador: ${datos.username}`);
    this.santoroPoints = datos.santoroPoints;
  }
);
```

---

### 2. getColeccion(username: string)

**Obtener todas las cartas de la colección del jugador**

```typescript
getColeccion(username: string): Observable<Card[]> {
  return this.http.get<Card[]>(
    `${this.apiUrl}/${username}/coleccion`
  );
}
```

**Parámetros**:
- `username: string` - Nombre de usuario

**Retorno**: `Observable<Card[]>` - Array de cartas poseídas

**Endpoint**: GET `/api/jugadores/[username]/coleccion`

**Response Ejemplo**:
```json
[
  {
    "id": "xy1-001",
    "nombre": "Pikachu",
    "hp": "40",
    "tipo": "Lightning",
    "imagen": "https://...",
    "costoRetirada": 1
  },
  {
    "id": "xy1-002",
    "nombre": "Charmeleon",
    "hp": "80",
    "tipo": "Fire",
    "imagen": "https://..."
  }
]
```

**Uso en componente**:
```typescript
this.jugadorService.getColeccion('Pikachu123').subscribe(
  (cartas: Card[]) => {
    console.log(`Colección: ${cartas.length} cartas`);
    this.collection = cartas;
  }
);
```

---

### 3. getSantoroQuest(username: string)

**Obtener estado de la misión Santoro**

```typescript
getSantoroQuest(username: string): Observable<SantoroQuestResponse> {
  return this.http.get<SantoroQuestResponse>(
    `${this.apiUrl}/${username}/quests/santoro`
  );
}
```

**Parámetros**:
- `username: string` - Nombre de usuario

**Retorno**: `Observable<SantoroQuestResponse>` - Estado de misión

**Endpoint**: GET `/api/jugadores/[username]/quests/santoro`

**Response Ejemplo**:
```json
{
  "giftClaimed": false,
  "tracking": true,
  "state": "IN_PROGRESS",
  "sobresDisponibles": 5
}
```

**Campos**:
- `giftClaimed: boolean` - ¿Ha reclamado el regalo?
- `tracking: boolean` - ¿Está rastreando la misión?
- `state: string` - Estado actual (IN_PROGRESS, COMPLETED, AVAILABLE)
- `sobresDisponibles: number` - Sobres disponibles

---

### 4. setSantoroTracking(username: string, tracking: boolean)

**Activar/desactivar rastreo de misión Santoro**

```typescript
setSantoroTracking(
  username: string,
  tracking: boolean
): Observable<SantoroQuestResponse> {
  return this.http.post<SantoroQuestResponse>(
    `${this.apiUrl}/${username}/quests/santoro/tracking`,
    { tracking }
  );
}
```

**Parámetros**:
- `username: string` - Nombre de usuario
- `tracking: boolean` - true = activar rastreo, false = desactivar

**Retorno**: `Observable<SantoroQuestResponse>` - Estado actualizado

**Endpoint**: POST `/api/jugadores/[username]/quests/santoro/tracking`

**Request Body**:
```json
{ "tracking": true }
```

**Uso**:
```typescript
this.jugadorService.setSantoroTracking('Pikachu123', true).subscribe(
  (questState) => {
    console.log('Rastreo activado');
  }
);
```

---

### 5. claimSantoroGift(username: string)

**Reclamar regalo de la misión Santoro**

```typescript
claimSantoroGift(username: string): Observable<SantoroQuestResponse> {
  return this.http.post<SantoroQuestResponse>(
    `${this.apiUrl}/${username}/quests/santoro/claim`,
    {}
  );
}
```

**Parámetros**:
- `username: string` - Nombre de usuario

**Retorno**: `Observable<SantoroQuestResponse>` - Estado actualizado

**Endpoint**: POST `/api/jugadores/[username]/quests/santoro/claim`

**Efectos**:
- Marca `giftClaimed: true`
- Puede otorgar sobres o monedas

**Uso**:
```typescript
this.jugadorService.claimSantoroGift('Pikachu123').subscribe(
  (questState) => {
    console.log(`Sobres disponibles: ${questState.sobresDisponibles}`);
  }
);
```

---

### 6. rewardCoins(username: string, amount: number)

**Recompensar monedas al jugador**

```typescript
rewardCoins(username: string, amount: number): Observable<JugadorDatosResponse> {
  return this.http.post<JugadorDatosResponse>(
    `${this.apiUrl}/${username}/coins/reward`,
    { amount }
  );
}
```

**Parámetros**:
- `username: string` - Nombre de usuario
- `amount: number` - Cantidad de monedas a recompensar

**Retorno**: `Observable<JugadorDatosResponse>` - Datos actualizados

**Endpoint**: POST `/api/jugadores/[username]/coins/reward`

**Request Body**:
```json
{ "amount": 100 }
```

**Validaciones**:
- `amount > 0` - Cantidad positiva
- `username` existe en BD

**Uso (premio por batalla)**:
```typescript
this.jugadorService.rewardCoins('Pikachu123', 50).subscribe(
  (updated) => {
    console.log(`Nuevos puntos: ${updated.santoroPoints}`);
  }
);
```

---

### 7. spendCoins(username: string, amount: number)

**Gastar monedas del jugador**

```typescript
spendCoins(username: string, amount: number): Observable<JugadorDatosResponse> {
  return this.http.post<JugadorDatosResponse>(
    `${this.apiUrl}/${username}/coins/spend`,
    { amount }
  );
}
```

**Parámetros**:
- `username: string` - Nombre de usuario
- `amount: number` - Cantidad a gastar

**Retorno**: `Observable<JugadorDatosResponse>` - Datos actualizados

**Endpoint**: POST `/api/jugadores/[username]/coins/spend`

**Validaciones**:
- `santoroPoints >= amount` - Fondos suficientes

**Uso (compra en tienda)**:
```typescript
this.jugadorService.spendCoins('Pikachu123', 50).subscribe(
  (updated) => {
    if (updated.santoroPoints >= 0) {
      console.log('Compra exitosa');
    }
  },
  (error) => {
    console.error('Fondos insuficientes');
  }
);
```

---

### 8. buyPacks(username: string, amount: number)

**Comprar sobres con monedas**

```typescript
buyPacks(username: string, amount: number): Observable<JugadorDatosResponse> {
  return this.http.post<JugadorDatosResponse>(
    `${this.apiUrl}/${username}/packs/buy`,
    { amount }
  );
}
```

**Parámetros**:
- `username: string` - Nombre de usuario
- `amount: number` - Cantidad de sobres a comprar

**Retorno**: `Observable<JugadorDatosResponse>` - Datos actualizados

**Endpoint**: POST `/api/jugadores/[username]/packs/buy`

**Request Body**:
```json
{ "amount": 3 }
```

**Validaciones**:
- `amount > 0`
- Costo calculado: `amount * costoPorSobre` (ej: 3 * 50 = 150 monedas)
- `santoroPoints >= totalCosto`

**Uso**:
```typescript
this.jugadorService.buyPacks('Pikachu123', 3).subscribe(
  (updated) => {
    console.log(`Sobres: ${updated.sobresDisponibles}`);
    console.log(`Puntos restantes: ${updated.santoroPoints}`);
  }
);
```

---

### 9. guardarPersonalizacion(username: string, config: any)

**Guardar personalización del avatar (colores, altura, etc.)**

```typescript
guardarPersonalizacion(username: string, config: any): Observable<void> {
  return this.http.post<void>(
    `${this.apiUrl}/${username}/personalizacion`,
    config
  );
}
```

**Parámetros**:
- `username: string` - Nombre de usuario
- `config: any` - Objeto con configuración

**Retorno**: `Observable<void>` - Sin contenido

**Endpoint**: POST `/api/jugadores/[username]/personalizacion`

**Request Body Ejemplo**:
```json
{
  "characterId": "pikachu",
  "skinColor": "#FFD700",
  "hairColor": "#000000",
  "eyeColor": "#0066CC",
  "height": 180,
  "pikachuCompanion": true
}
```

**Campos soportados**:
- `characterId: string` - Personaje seleccionado
- `skinColor: string` - Color de piel (hex)
- `hairColor: string` - Color de pelo (hex)
- `eyeColor: string` - Color de ojos (hex)
- `height: number` - Altura en cm
- `pikachuCompanion: boolean` - ¿Acompañante Pikachu?

**Uso en componente**:
```typescript
const config = {
  characterId: 'charizard',
  skinColor: '#FF6B35',
  hairColor: '#8B4513'
};

this.jugadorService.guardarPersonalizacion('Pikachu123', config).subscribe(
  () => {
    console.log('Personalización guardada');
  }
);
```

---

### 10. ejecutarTrade(playerA: string, playerB: string, playerACards: string[], playerBCards: string[])

**Ejecutar intercambio de cartas entre dos jugadores**

```typescript
ejecutarTrade(
  playerA: string,
  playerB: string,
  playerACards: string[],
  playerBCards: string[]
): Observable<void> {
  return this.http.post<void>(
    `${this.apiUrl}/trade/execute`,
    {
      playerA,
      playerB,
      playerACardIds: playerACards,
      playerBCardIds: playerBCards
    }
  );
}
```

**Parámetros**:
- `playerA: string` - Username del jugador A
- `playerB: string` - Username del jugador B
- `playerACards: string[]` - IDs de cartas que A cede
- `playerBCards: string[]` - IDs de cartas que B cede

**Retorno**: `Observable<void>` - Sin contenido

**Endpoint**: `POST /api/jugadores/trade/execute`

**Request Body Ejemplo**:
```json
{
  "playerA": "Pikachu123",
  "playerB": "Charizard456",
  "playerACardIds": ["xy1-001", "xy1-002"],
  "playerBCardIds": ["xy1-025", "xy1-035"]
}
```

**Validaciones**:
- Ambos jugadores existen
- Ambos poseen sus cartas
- Las cartas no están en uso (mazos activos)
- Ambos aceptan el trade (si requiere aceptación)

**Uso**:
```typescript
this.jugadorService.ejecutarTrade(
  'Pikachu123',
  'Charizard456',
  ['xy1-001', 'xy1-002'],
  ['xy1-025']
).subscribe(
  () => {
    console.log('Intercambio completado');
  },
  (error) => {
    console.error('Intercambio fallido:', error);
  }
);
```

---

### 11. debugSetSobres(username: string, cantidad: number)

**[DEBUG] Ajustar cantidad de sobres para pruebas**

```typescript
debugSetSobres(username: string, cantidad: number): Observable<JugadorDatosResponse> {
  return this.http.post<JugadorDatosResponse>(
    `${this.apiUrl}/${username}/debug/sobres`,
    { cantidad }
  );
}
```

**Parámetros**:
- `username: string` - Nombre de usuario
- `cantidad: number` - Sobres a establecer (no suma, reemplaza)

**Retorno**: `Observable<JugadorDatosResponse>` - Datos actualizados

**Endpoint**: POST `/api/jugadores/[username]/debug/sobres`

**⚠️ Notas**:
- Solo disponible en desarrollo
- Reemplaza cantidad (no suma)
- No debe usarse en producción

---

## 📊 Tipo de Datos

### JugadorDatosResponse

```typescript
interface JugadorDatosResponse {
  id: number;
  username: string;
  email: string;
  sobresDisponibles: number;
  santoroPoints: number;
  characterId?: string;
  skinColor?: string;
  hairColor?: string;
  eyeColor?: string;
  height?: number;
  pikachuCompanion?: boolean;
}
```

### SantoroQuestResponse

```typescript
interface SantoroQuestResponse {
  giftClaimed: boolean;      // Regalo reclamado
  tracking: boolean;         // ¿Rastreando?
  state: string;             // IN_PROGRESS | COMPLETED | AVAILABLE
  sobresDisponibles: number; // Sobres disponibles
}
```

---

## 🎯 Casos de Uso

### Caso 1: Cargar perfil del jugador
```typescript
export class ProfileComponent implements OnInit {
  jugador!: JugadorDatosResponse;
  
  constructor(private jugadorService: JugadorService) {}
  
  ngOnInit() {
    this.jugadorService.getJugador('Pikachu123').subscribe(
      (datos) => {
        this.jugador = datos;
        console.log(`${datos.username} tiene ${datos.santoroPoints} puntos`);
      }
    );
  }
}
```

### Caso 2: Gestión de monedas en tienda
```typescript
comprarSobres(cantidad: number) {
  this.jugadorService.buyPacks(this.username, cantidad).subscribe(
    (updated) => {
      this.santoroPoints = updated.santoroPoints;
      this.sobresDisponibles = updated.sobresDisponibles;
      this.showNotification('Compra exitosa');
    },
    (error) => {
      this.showNotification('Fondos insuficientes');
    }
  );
}
```

### Caso 3: Personalización del avatar
```typescript
aplicarPersonalizacion() {
  const config = {
    characterId: this.selectedCharacter,
    skinColor: this.skinColor,
    hairColor: this.hairColor,
    eyeColor: this.eyeColor
  };
  
  this.jugadorService.guardarPersonalizacion(this.username, config)
    .subscribe(() => {
      this.showSuccess('Personalización guardada');
    });
}
```

### Caso 4: Intercambio de cartas
```typescript
aceptarTrade(otroJugador: string, midasCartas: string[], susCarts: string[]) {
  this.jugadorService.ejecutarTrade(
    this.username,
    otroJugador,
    midasCartas,
    susCarts
  ).subscribe(
    () => {
      console.log('Trade completado');
      this.refrescarColeccion();
    },
    (error) => {
      console.error('Error en trade:', error);
    }
  );
}
```

### Caso 5: Monitorear estado de misiones
```typescript
consultarMision() {
  this.jugadorService.getSantoroQuest(this.username).subscribe(
    (questState) => {
      if (!questState.giftClaimed && questState.tracking) {
        this.mostrarBotonReclamar();
      }
    }
  );
}
```

---

## 💾 Observables y Patrón Reactivo

**Uso con RxJS operators**:
```typescript
this.jugadorService.getJugador('Pikachu123')
  .pipe(
    tap((datos) => console.log('Datos cargados:', datos)),
    map((datos) => datos.santoroPoints),
    filter((coins) => coins > 0),
    shareReplay(1)  // Cachea para múltiples suscriptores
  )
  .subscribe((coins) => {
    this.displayCoins = coins;
  });
```

---

## ⚠️ Notas Importantes

**Credenciales**:
- `withCredentials: true` en `getJugador` para enviar cookies de sesión
- Necesario para autenticación basada en sesiones

**Errores comunes**:
- 404: Usuario no existe
- 401: No autenticado (en endpoints que lo requieren)
- 400: Datos inválidos (coins negativos, etc)
- 409: Conflicto (ej: trade con cartas en uso)

**Performance**:
- Usa `shareReplay(1)` para cachear datos del jugador
- Evita múltiples llamadas a `getJugador` sin caché

---

## 📋 Métodos Resumen

| Método | Parámetros | Retorna | Endpoint |
|--------|-----------|---------|----------|
| `getJugador()` | username | `Observable<JugadorDatosResponse>` | GET /datos |
| `getColeccion()` | username | `Observable<Card[]>` | GET /coleccion |
| `getSantoroQuest()` | username | `Observable<SantoroQuestResponse>` | GET /quests/santoro |
| `setSantoroTracking()` | username, tracking | `Observable<SantoroQuestResponse>` | POST /quests/santoro/tracking |
| `claimSantoroGift()` | username | `Observable<SantoroQuestResponse>` | POST /quests/santoro/claim |
| `rewardCoins()` | username, amount | `Observable<JugadorDatosResponse>` | POST /coins/reward |
| `spendCoins()` | username, amount | `Observable<JugadorDatosResponse>` | POST /coins/spend |
| `buyPacks()` | username, amount | `Observable<JugadorDatosResponse>` | POST /packs/buy |
| `guardarPersonalizacion()` | username, config | `Observable<void>` | POST /personalizacion |
| `ejecutarTrade()` | playerA, playerB, cards | `Observable<void>` | POST /trade/execute |

---

*Próximo: [SobreService](/docs/componentes-detallados/frontend/services/04-sobre-service)*
