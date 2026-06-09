---
sidebar_position: 2
title: 🃏 CardService
---

# 🃏 CardService - Servicio de Cartas

> Servicio Angular para obtener catálogo de cartas

---

## 📍 Ubicación

`frontend/src/app/core/services/card.service.ts`

---

## 🏗️ Definición del Servicio

```typescript
@Injectable({ providedIn: 'root' })
export class CardService {
  private apiUrl = `${getBackendUrl()}/api/cards`;
  
  constructor(private http: HttpClient) {}
}
```

**Tipo**: Servicio raíz
**Dependencias**: HttpClient

---

## 📡 Método Principal

### getAll()

**Obtener todas las cartas del catálogo**

```typescript
getAll(): Observable<Card[]> {
  return this.http.get<Card[]>(this.apiUrl);
}
```

**Retorno**: `Observable<Card[]>` - Array de todas las cartas

**Endpoint**: `GET /api/cards`

**Uso en componente**:
```typescript
this.cardService.getAll().subscribe(
  (cards: Card[]) => {
    console.log(`Catálogo cargado: ${cards.length} cartas`);
    this.cardsCatalog = cards;
  },
  (error) => {
    console.error('Error al cargar catálogo:', error);
  }
);
```

---

## 📊 Tipo de Datos

### Card

```typescript
interface Card {
  id: string;              // ej: "xy1-001"
  nombre: string;          // Nombre de la carta
  hp: string;              // Puntos de vida (ej: "40" o "—")
  tipo: string;            // Tipo de energía (Fire, Water, etc)
  imagen: string;          // URL a imagen
  costoRetirada: number;   // Costo de retirar
  supertype: string;       // Pokemon, Energy, Trainer
  evolvesFrom?: string;    // ID de carta base (si evoluciona)
  subtypes?: string[];     // Basic, Stage 1, etc
  reglas?: string[];       // Reglas especiales
  ataques?: Ataque[];      // Ataques disponibles
  debilidades?: CardAttribute[];
  resistencias?: CardAttribute[];
}

interface Ataque {
  nombre: string;
  danio: number;
  tiposEnergia: string[];
  texto?: string;
}

interface CardAttribute {
  tipo: string;
  valor: number;
}
```

---

## 🎯 Casos de Uso

### Caso 1: Cargar catálogo al iniciar
```typescript
export class CardCatalogComponent implements OnInit {
  cards$ = this.cardService.getAll();
  
  constructor(private cardService: CardService) {}
  
  ngOnInit() {
    // Observable se usa en template con async pipe
    // {{ cards$ | async | slice:0:10 }}
  }
}
```

### Caso 2: Filtrar en componente
```typescript
this.cardService.getAll().pipe(
  map(cards => cards.filter(c => c.tipo === 'Fire')),
  shareReplay(1)
).subscribe(fireCards => {
  this.fireCards = fireCards;
});
```

### Caso 3: Búsqueda de carta por ID
```typescript
this.cardService.getAll().pipe(
  map(cards => cards.find(c => c.id === 'xy1-025'))
).subscribe(pikachu => {
  console.log(pikachu); // Pikachu
});
```

---

## 💾 Caché y Performance

**Sin caché interno** en el servicio:
- ✅ Cada llamada hace GET /api/cards
- ⚠️ Backend cachea (CardCatalogService)
- 🟢 Frontend puede usar `shareReplay(1)` en componentes

**Recomendación**:
```typescript
// En un componente
private cards$ = this.cardService.getAll().pipe(
  shareReplay(1)  // Cachea el resultado
);
```

---

## 📋 Resumen

| Método | Parámetros | Retorna | Endpoint |
|--------|-----------|---------|----------|
| `getAll()` | none | `Observable<Card[]>` | GET /api/cards |

---

*Próximo: [JugadorService](/docs/componentes-detallados/frontend/services/03-jugador-service)*
