---
sidebar_position: 4
title: 🎁 SobreService
---

# 🎁 SobreService - Servicio de Sobres

> Servicio Angular para abrir sobres (booster packs) y obtener cartas aleatorias

---

## 📍 Ubicación

`frontend/src/app/features/lobby/services/sobre.service.ts`

---

## 🏗️ Definición del Servicio

```typescript
@Injectable({ providedIn: 'root' })
export class SobreService {
  constructor(private http: HttpClient) {}
}
```

**Tipo**: Servicio raíz
**Dependencias**: HttpClient
**Feature**: Lobby (Paquetes de cartas)

---

## 📡 Método Principal

### abrirSobre(username: string)

**Abrir un sobre y obtener cartas aleatorias**

```
abrirSobre(username: string): Observable<Card[]> {
  // POST a /api/sobres/abrir/[username]
  return this.http.post<Card[]>(baseUrl + '/api/sobres/abrir/' + username, {});
}
```

**Parámetros**:
- `username: string` - Nombre de usuario que abre el sobre

**Retorno**: `Observable<Card[]>` - Array de cartas obtenidas (7-13 cartas)

**Endpoint**: POST `/api/sobres/abrir/[username]`

**Request Body**: Empty (vacío)

---

## 📊 Composición del Sobre

Un sobre contiene cartas generadas aleatoriamente según esta composición:

```
┌─────────────────────────────────┐
│        COMPOSICIÓN SOBRE        │
├─────────────────────────────────┤
│ Energías:    2-5 cartas         │
│ Pokémon:     5-8 cartas         │
│ Entrenador:  0-2 cartas (raro)  │
├─────────────────────────────────┤
│ TOTAL:       7-13 cartas        │
│ Shuffle:     Mezclado           │
└─────────────────────────────────┘
```

**Response Ejemplo**:
```json
[
  {
    "id": "base1-energy-fire",
    "nombre": "Fire Energy",
    "hp": "—",
    "tipo": "Fire",
    "imagen": "https://...",
    "supertype": "Energy"
  },
  {
    "id": "base1-001",
    "nombre": "Pikachu",
    "hp": "40",
    "tipo": "Lightning",
    "imagen": "https://...",
    "supertype": "Pokemon",
    "ataques": [
      {
        "nombre": "Thunderbolt",
        "danio": 30,
        "tiposEnergia": ["Lightning", "Colorless"]
      }
    ]
  },
  {
    "id": "base1-energy-water",
    "nombre": "Water Energy",
    "hp": "—",
    "tipo": "Water",
    "supertype": "Energy"
  }
]
```

---

## 🎯 Flujo de Apertura de Sobre

```
Usuario                    Componente         SobreService        Backend
   │                           │                   │                │
   ├─ Click "Abrir Sobre"  │                   │                │
   │───────────────────────→│                   │                │
   │                       │ abrirSobre() ─────→│                │
   │                       │                   │ POST /api/sobres│
   │                       │                   │ /abrir/user ──→ │
   │                       │                   │            Genera│
   │                       │                   │            cartas│
   │                       │                   │ ← [Card[], etc] │
   │                       │ ← Observable<Card>─                │
   │                       │                   │                │
   │                       │ subscribe()        │                │
   │                       │                   │                │
   │ ← Anima cartas ──────│                   │                │
   │ ← Sonido de apertura ─                   │                │
```

---

## 🎬 Uso en Componente

### Caso 1: Abrir sobre simple

```typescript
export class AbrirSobreComponent {
  cartasObtenidas: Card[] = [];
  loading = false;

  constructor(private sobreService: SobreService) {}

  abrirSobre() {
    this.loading = true;
    
    this.sobreService.abrirSobre('Pikachu123').subscribe(
      (cartas: Card[]) => {
        this.cartasObtenidas = cartas;
        this.loading = false;
        console.log(`Abierto sobre: PARAM cartas`);
        this.mostrarAnimacion();
      },
      (error) => {
        console.error('Error al abrir sobre:', error);
        this.loading = false;
      }
    );
  }

  mostrarAnimacion() {
    // Animar cada carta con delay
    this.cartasObtenidas.forEach((carta, index) => {
      setTimeout(() => {
        this.playFlipAnimation(index);
      }, index * 150);
    });
  }
}
```

### Caso 2: Integración con gestión de monedas

```typescript
comprarYAbrirSobres(cantidad: number) {
  // 1. Comprar sobres
  this.jugadorService.buyPacks(this.username, cantidad).subscribe(
    (updated) => {
      console.log(`Comprados PARAM sobres`);
      
      // 2. Abrir el primer sobre
      this.abrirSobreSiguiente();
    },
    (error) => {
      this.showError('Fondos insuficientes');
    }
  );
}

abrirSobreSiguiente() {
  if (this.sobresRestantes > 0) {
    this.sobreService.abrirSobre(this.username).subscribe(
      (cartas) => {
        this.todasLasCartas.push(...cartas);
        this.sobresRestantes--;
        
        // Abrir siguiente
        this.abrirSobreSiguiente();
      }
    );
  } else {
    this.mostrarResumenTotal();
  }
}
```

### Caso 3: Con animaciones y sonidos

```typescript
abrirSobreConFX() {
  this.loading = true;
  this.soundService.play('pack-open-start'); // Sonido de rasgueo

  this.sobreService.abrirSobre(this.username).subscribe(
    (cartas) => {
      this.cartasObtenidas = cartas;
      
      // Animar cada carta
      cartas.forEach((carta, index) => {
        setTimeout(() => {
          this.animarCartaFlip(index, carta);
          
          // Sonido al voltear
          if (index % 3 === 0) {
            this.soundService.play('card-flip');
          }
          
          // Sonido especial si es rara
          if (this.esCartaRara(carta)) {
            this.soundService.play('rare-card');
            this.crearParticulas(carta); // Efectos visuales
          }
        }, index * 200);
      });
      
      this.loading = false;
    }
  );
}

esCartaRara(carta: Card): boolean {
  return carta.supertype === 'Trainer' ||
         (carta.supertype === 'Pokemon' && carta.hp && parseInt(carta.hp) > 100);
}
```

### Caso 4: Procesamiento de cartas obtenidas

```typescript
procesarCartasObtenidas(cartas: Card[]) {
  const estadisticas = {
    energias: cartas.filter(c => c.supertype === 'Energy').length,
    pokemon: cartas.filter(c => c.supertype === 'Pokemon').length,
    entrenador: cartas.filter(c => c.supertype === 'Trainer').length,
    raras: cartas.filter(c => this.esRara(c)).length
  };

  console.log(`
    Energías: PARAM
    Pokémon: PARAM
    Entrenador: PARAM
    Raras: PARAM
  `);

  // Actualizar colección
  this.agregarAColeccion(cartas);
  
  // Mostrar resumen
  this.mostrarResumenSobre(estadisticas);
}
```

---

## 🔄 Integración con Otros Servicios

### Con JugadorService

```typescript
// 1. Obtener sobres disponibles
this.jugadorService.getJugador(username).pipe(
  tap(datos => console.log(`Sobres: PARAM`)),
  
  // 2. Si hay sobres, abrir
  switchMap(datos => 
    datos.sobresDisponibles > 0
      ? this.sobreService.abrirSobre(username)
      : throwError(() => new Error('Sin sobres'))
  )
).subscribe(cartas => {
  console.log(`Cartas obtenidas: PARAM`);
});
```

### Con CardService

```typescript
// Comparar cartas obtenidas con catálogo
this.sobreService.abrirSobre(username).pipe(
  switchMap(cartasNuevas => {
    return this.cardService.getAll().pipe(
      map(catalogo => {
        return cartasNuevas.map(carta => {
          const cartaDatos = catalogo.find(c => c.id === carta.id);
          return { ...carta, ...cartaDatos };
        });
      })
    );
  })
).subscribe(cartasConDatos => {
  this.cartasObtenidas = cartasConDatos;
});
```

---

## 📊 Tipo de Datos

### Card (Respuesta)

```typescript
interface Card {
  id: string;              // Identificador único
  nombre: string;          // Nombre de la carta
  hp: string;              // Puntos de vida (ej: "40", "—")
  tipo: string;            // Tipo de energía
  imagen: string;          // URL a imagen
  costoRetirada: number;   // Costo para retirar
  supertype: string;       // "Energy" | "Pokemon" | "Trainer"
  evolvesFrom?: string;    // Carta base (si evoluciona)
  subtypes?: string[];     // Tipos: Basic, Stage 1, etc
  reglas?: string[];       // Habilidades especiales
  ataques?: Ataque[];      // Array de ataques
  debilidades?: CardAttribute[];    // Debilidades
  resistencias?: CardAttribute[];    // Resistencias
}
```

---

## 🎨 Sugerencias de UI/UX

### Animación de Apertura

```typescript
// Rotar tarjeta antes de revelar
@keyframes flipCard {
  0% { transform: rotateY(0deg); }
  50% { transform: rotateY(90deg); }
  100% { transform: rotateY(0deg); }
}

// Brillo para cartas raras
@keyframes glowRare {
  0%, 100% { box-shadow: 0 0 0px gold; }
  50% { box-shadow: 0 0 20px gold; }
}
```

### Secuencia de Eventos

1. **Click**: Usuario presiona "Abrir Sobre"
2. **Loading**: Mostrar spinner
3. **Sonido**: Efecto de rasgueo
4. **Animación 1**: Tarjeta se voltea (flip)
5. **Reveal**: Se muestra cada carta con delay
6. **Efecto Especial**: Si es rara, animar con brillo
7. **Sonido**: Card flip sound para cada carta
8. **Fin**: Botón para añadir a colección o continuar

---

## ⚠️ Notas Importantes

**Validaciones Backend**:
- `sobresDisponibles > 0` - Debe tener al menos 1 sobre
- `username` debe existir y estar autenticado
- Decrementa `sobresDisponibles` automáticamente

**Consideraciones de Performance**:
- Cada llamada genera cartas aleatoriamente
- Backend usa seeding para reproducibilidad si es necesario
- No cachear respuesta (cartas son únicas por sobre)

**Manejo de Errores**:
- 404: Usuario no encontrado
- 400: Sin sobres disponibles
- 401: No autenticado
- 500: Error al generar cartas

**Aleatoridad**:
- Cada ejecución produce cartas diferentes
- Distribución es: 2-5 energías + 5-8 pokémon + 0-2 entrenador
- Finalmente se realiza shuffle

---

## 📋 Resumen

| Método | Parámetros | Retorna | Endpoint |
|--------|-----------|---------|----------|
| `abrirSobre()` | username | `Observable<Card[]>` | POST /api/sobres/abrir/[username] |

---

*Próximo: [MazoService](/docs/componentes-detallados/frontend/services/05-mazo-service)*
