---
sidebar_position: 10
title: 🔄 Manejo de Estado
---

# 🔄 Manejo de Estado - RxJS

---

## BehaviorSubject

```typescript
private gameStateSubject = new BehaviorSubject<GameState>(initialState);
public gameState$ = this.gameStateSubject.asObservable();

// Obtener estado actual
const current = this.gameStateSubject.value;

// Actualizar estado
this.gameStateSubject.next({
  ...currentState,
  currentPlayer: 2
});
```

---

## Operadores RxJS Clave

```typescript
// map: Transformar datos
cartas$.pipe(
  map(cartas => cartas.filter(c => c.hp > 50))
)

// filter: Filtrar por condición
battle$.pipe(
  filter(b => b.state === 'ACTIVE')
)

// switchMap: Cambiar observable
button.click$.pipe(
  switchMap(click => http.get('/data'))
)

// takeUntil: Suscribción hasta destrucción
component$.pipe(
  takeUntil(destroy$)
)

// distinctUntilChanged: Solo si cambió
values$.pipe(
  distinctUntilChanged()
)
```

---

## Patrón Component → Service

```typescript
// Service
@Injectable()
export class BattleService {
  private state$ = new BehaviorSubject<BattleState>(init);
  public state = this.state$.asObservable();
  
  playCard(card: Card) {
    const newState = { ...this.state$.value, hand: [...] };
    this.state$.next(newState);
  }
}

// Component
export class BattleComponent {
  battle$ = this.service.state;
  
  constructor(private service: BattleService) {}
  
  onPlayCard(card: Card) {
    this.service.playCard(card);
    // UI se actualiza automáticamente vía | async
  }
}

// Template
<div *ngIf="battle$ | async as battle">
  {{ battle.currentPlayer }}
</div>
```

---

*Próximo: [Algoritmos Clave](/docs/tecnica/algoritmos-clave)*
