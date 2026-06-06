# Convenciones del Proyecto

## Naming

### Backend (Java)
- Clases: `PascalCase` — `BattleService`, `CartaEnJuego`
- Métodos y variables: `camelCase` — `atacarPokemon()`, `cartaActual`
- Constantes: `UPPER_SNAKE_CASE` — `MAX_CARTAS_MAZO`
- Paquetes: `lowercase` — `com.pokemon.tcg.service`
- DTOs: sufijo `Request` o `Response` — `StartBattleRequest`, `JugadorDatosResponse`

### Frontend (TypeScript/Angular)
- Componentes: `kebab-case` en archivos, `PascalCase` en clase — `battle-board.component.ts` / `BattleBoardComponent`
- Servicios: sufijo `Service` — `BattleService`, `MazoService`
- Interfaces/modelos: `PascalCase` sin prefijo — `Card`, `Jugador`, `Partida`
- Observables: sufijo `$` — `cartas$`, `estado$`
- Archivos de tipos: sufijo `.types.ts` — `battle-board.types.ts`

## Idioma del código

- **Dominio del juego** (entidades, métodos de negocio): español — `Jugador`, `atacar()`, `mazo`
- **Infraestructura y patrones técnicos**: inglés — `service`, `controller`, `repository`, `handler`
- **Commits y PRs**: inglés
- **Comentarios en código**: español

## Estructura de endpoints REST

```
POST   /api/auth/login
POST   /api/auth/register
GET    /api/jugador/{id}
GET    /api/cards
POST   /api/mazo
PUT    /api/mazo/{id}
POST   /api/battle/start
POST   /api/battle/{id}/attack
```

## Manejo de errores

- El BE devuelve errores con estructura consistente: `{ "error": "mensaje", "status": 400 }`.
- El FE muestra mensajes de error al usuario — nunca loguea solo en consola y sigue.

## Tests

- Archivos de test: mismo nombre que la clase + `Test` — `BattleAttackServiceTest.java`
- Un test = un comportamiento. No combines múltiples casos en un solo método.
- Nombrá los métodos de test: `metodo_condicion_resultadoEsperado`.
