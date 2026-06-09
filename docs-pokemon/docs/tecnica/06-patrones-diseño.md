---
sidebar_position: 6
title: 🏛️ Patrones de Diseño
---

# 🏛️ Patrones de Diseño Implementados

---

## Service Pattern
Controllers delegan a Services, Services delegan a Repositories
```
Controller → Service → Repository
```

## DTO Pattern
Data Transfer Objects separan modelo interno del API
```
Entity (BD) ← Mapper → DTO (API)
```

## Repository Pattern
Abstracción de acceso a datos
```
Service → Repository → Database
```

## Observer Pattern (RxJS)
Observables y Subjects para estado reactivo
```typescript
public battle$ = this.battleSubject$.asObservable();
// Los componentes se suscriben automáticamente
```

## Singleton Pattern
Services inyectados por Dependency Injection
```typescript
@Injectable({ providedIn: 'root' })  // Una sola instancia
```

## Factory Pattern
Creación de cartas, jugadores, etc
```
CardFactory.create(cardData) → Card
```

---

*Próximo: [Battle Engine](/docs/tecnica/batalla-engine)*
