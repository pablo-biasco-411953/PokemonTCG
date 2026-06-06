# Skill — Deck Builder (Armado de Mazos)

---

## Entidades clave

| Clase | Descripción |
|-------|-------------|
| `Mazo` | Mazo de un jugador: nombre, lista de cartas, referencia al jugador dueño |
| `Card` | Carta del juego: nombre, tipo, HP, ataques, energía de retiro, evoluciona de |

## Backend

- `MazoService`: lógica de creación, validación y actualización de mazos.
- `MazoController`: endpoints para CRUD de mazos.
- `CardController`: endpoints para consultar cartas disponibles.

### Reglas de validación de mazo

- Un mazo debe tener exactamente **60 cartas**.
- Máximo **4 copias** de una misma carta (excepto cartas de energía básica — sin límite).
- Debe tener al menos **1 Pokémon básico** para poder iniciar una partida.

### Endpoints relevantes

```
GET    /api/cards              ← todas las cartas disponibles (para armar el mazo)
GET    /api/mazo/{jugadorId}   ← mazos del jugador
POST   /api/mazo               ← crear nuevo mazo
PUT    /api/mazo/{id}          ← actualizar mazo existente
DELETE /api/mazo/{id}          ← eliminar mazo
```

## Frontend

- Componente principal: `features/deck-builder/deck-builder.component.ts`
- Servicio: `features/deck-builder/services/mazo.service.ts`

El deck builder permite al usuario:
1. Buscar cartas del catálogo.
2. Agregar/quitar cartas al mazo en construcción.
3. Guardar o actualizar el mazo.

La validación de las reglas (60 cartas, máx 4 copias) debe hacerse tanto en FE (UX inmediata) como en BE (seguridad).
