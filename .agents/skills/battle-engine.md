# Skill — Motor de Batalla

Conocimiento de dominio para trabajar en el sistema de batalla del juego.

---

## Entidades clave

| Clase | Ubicación | Descripción |
|-------|-----------|-------------|
| `Partida` | `model/battle/Partida.java` | Estado completo de una partida en curso |
| `TableroJugador` | `model/battle/TableroJugador.java` | Estado del tablero de un jugador (mano, activo, banca, descarte) |
| `CartaEnJuego` | `model/battle/CartaEnJuego.java` | Carta con estado: HP actual, energías adjuntas, estado (dormido, paralizado) |
| `Ataque` | `model/battle/Ataque.java` | Definición de un ataque (nombre, daño, costo de energía, efecto) |
| `ResultadoAtaque` | `model/battle/ResultadoAtaque.java` | Resultado de ejecutar un ataque (daño aplicado, efectos, KOs) |

## Servicios y responsabilidades

| Servicio | Responsabilidad |
|---------|-----------------|
| `BattleService` | Orquesta el flujo general: iniciar partida, distribuir turnos |
| `BattleEngineService` | Lógica central del motor: aplica acciones, actualiza estado |
| `BattleAttackService` | Calcula y aplica daño de ataques, aplica efectos especiales |
| `BattleKoService` | Detecta y resuelve KOs: mueve carta al descarte, premia al oponente |
| `BattleTurnService` | Gestiona la secuencia de acciones válidas por turno |
| `BotAIService` | Elige acciones automáticamente para el oponente bot |

## Flujo de un ataque

```
FE ──POST /api/battle/{id}/attack──→ BattleController
        └──→ BattleService.processTurn()
               └──→ BattleAttackService.executeAttack()
                      ├── calcula daño (tipo, debilidad, resistencia)
                      ├── aplica efectos del ataque
                      └──→ BattleKoService.checkKo()
                             ├── si KO: mueve carta, otorga premio
                             └── devuelve estado actualizado al FE
```

## Reglas del dominio importantes

- Un jugador solo puede atacar UNA vez por turno.
- Para atacar, el Pokémon activo debe tener las energías requeridas adjuntas.
- La debilidad duplica el daño (`x2`). La resistencia resta `-30`.
- Si el Pokémon activo queda KO, el jugador debe poner uno de su banca como nuevo activo.
- Un jugador pierde cuando no puede colocar un Pokémon activo o se queda sin premios.

## Acciones válidas por turno (no acumulables)

- Jugar un Pokémon básico en la banca
- Adjuntar UNA energía
- Evolucionar un Pokémon (no en el turno que fue jugado)
- Jugar cartas de entrenador
- Retirarse (pagar costo de retiro)
- Atacar (termina el turno)
