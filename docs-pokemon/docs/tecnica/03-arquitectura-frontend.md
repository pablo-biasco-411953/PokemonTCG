---
sidebar_position: 3
title: 🎨 Arquitectura Frontend
---

# 🎨 Arquitectura Frontend - React + TypeScript

---

## 📦 Estructura

```
frontend/src/
├── app/
│   ├── core/
│   │   └── services/
│   │       ├── auth.service.ts
│   │       ├── battle.service.ts
│   │       ├── card.service.ts
│   │       ├── jugador.service.ts
│   │       └── sound.service.ts
│   │
│   ├── features/
│   │   ├── battle/
│   │   │   ├── battle-board.component.ts
│   │   │   ├── components/
│   │   │   └── services/
│   │   │
│   │   ├── lobby/
│   │   │   ├── lobby.component.ts
│   │   │   └── services/
│   │   │
│   │   └── deck-builder/
│   │       ├── builder.component.ts
│   │       └── services/
│   │
│   ├── shared/
│   │   ├── components/
│   │   ├── pipes/
│   │   └── directives/
│   │
│   └── app.routes.ts
│
└── main.ts
```

---

## 🎯 Service Layer

```typescript
@Injectable({ providedIn: 'root' })
export class BattleService {
  
  private battleSubject$ = new BehaviorSubject<Battle | null>(null);
  public battle$ = this.battleSubject$.asObservable();
  
  constructor(private http: HttpClient) {}
  
  startBattle(request: StartBattleRequest): Observable<Battle> {
    return this.http.post<Battle>('/api/battle/start', request)
      .pipe(
        tap(battle => this.battleSubject$.next(battle)),
        catchError(err => this.handleError(err))
      );
  }
  
  executeAction(action: BattleAction): Observable<BattleState> {
    return this.http.post<BattleState>('/api/battle/action', action)
      .pipe(
        tap(state => this.updateLocalState(state))
      );
  }
}
```

---

## 🎭 Component Layer

```typescript
@Component({
  selector: 'app-battle-board',
  template: `...`
})
export class BattleBoardComponent implements OnInit, OnDestroy {
  
  battle$ = this.battleService.battle$;
  private destroy$ = new Subject<void>();
  
  constructor(
    private battleService: BattleService,
    private attackService: BattleAttackService
  ) {}
  
  ngOnInit() {
    this.battle$.pipe(
      takeUntil(this.destroy$)
    ).subscribe(battle => {
      // Actualizar UI
    });
  }
  
  onAttack(move: string) {
    this.battleService.executeAction({
      type: 'ATTACK',
      moveName: move
    }).subscribe();
  }
  
  ngOnDestroy() {
    this.destroy$.next();
    this.destroy$.complete();
  }
}
```

---

## 🔄 State Management (RxJS)

```typescript
// En lugar de Redux/NgRx, usamos RxJS directamente

@Injectable({ providedIn: 'root' })
export class BattleStateService {
  
  private state$ = new BehaviorSubject<BattleState>(initialState);
  
  // Observables públicos
  public currentPlayer$ = this.state$.pipe(
    map(s => s.currentPlayer),
    distinctUntilChanged()
  );
  
  public myHand$ = this.state$.pipe(
    map(s => s.myHand),
    distinctUntilChanged()
  );
  
  public opponentActive$ = this.state$.pipe(
    map(s => s.opponentActive),
    distinctUntilChanged()
  );
  
  // Acciones
  updateState(newState: Partial<BattleState>) {
    const current = this.state$.value;
    this.state$.next({ ...current, ...newState });
  }
  
  playCard(card: Card) {
    this.updateState({
      myHand: this.state$.value.myHand.filter(c => c.id !== card.id)
    });
  }
}
```

---

## 📡 HTTP Client Integration

```typescript
// Services usan HttpClient de Angular
constructor(private http: HttpClient) {}

getCards(): Observable<Card[]> {
  return this.http.get<Card[]>('/api/cards').pipe(
    shareReplay(1), // Cache result
    timeout(5000),   // 5s timeout
    retry(2),        // Reintentar 2 veces
    catchError(e => this.handleError(e))
  );
}
```

---

## 🎨 Componentes Principales

| Componente | Responsabilidad |
|-----------|-----------------|
| BattleBoard | UI principal de batalla |
| Lobby | Buscar/crear salas |
| DeckBuilder | Construir mazos |
| CardDetail | Detalle de carta |
| PlayerProfile | Perfil del jugador |

---

*Próximo: [Diseño de BD](/docs/tecnica/database-design)*
