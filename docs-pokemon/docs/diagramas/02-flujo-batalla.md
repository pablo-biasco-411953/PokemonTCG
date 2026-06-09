---
sidebar_position: 2
title: "⚔️ Flujo de Batalla"
---

# Flujo de Batalla - Diagramas de Secuencia

> Ciclo completo de una batalla Pokemon desde inicio hasta fin

---

## Inicio de Batalla

```mermaid
sequenceDiagram
    participant FE as Frontend
    participant BC as BattleController
    participant BES as BattleEngineService
    participant BOT as BotAIService
    participant DB as MySQL

    FE->>BC: POST /api/battle/start/{username}
    BC->>BES: startBattle(username, mazoId)
    BES->>DB: Cargar jugador + mazo
    BES->>BES: Crear Partida (UUID)
    BES->>BES: Barajar mazos
    BES->>BES: Repartir 7 cartas a cada jugador
    BES->>BES: Separar 6 premios
    BES->>BES: Fase = COIN_FLIP
    BES-->>BC: Partida creada
    BC-->>FE: Partida JSON
```

---

## Coin Flip y Setup

```mermaid
sequenceDiagram
    participant FE as Frontend
    participant BES as BattleEngineService
    participant BOT as BotAIService

    FE->>BES: lanzarMoneda(partidaId)
    BES->>BES: Random heads/tails
    BES->>BES: Determinar quien empieza
    BES-->>FE: Resultado + turno inicial

    Note over FE,BOT: Fase SETUP_JUGADOR

    FE->>BES: colocarActivo(partidaId, cardIndex)
    FE->>BES: colocarEnBanca(partidaId, cardIndices)
    BES->>BES: Fase = SETUP_BOT

    BES->>BOT: ejecutarSetup(partida)
    BOT->>BOT: Evaluar potencial de cada basico
    BOT->>BOT: Colocar mejor como activo
    BOT->>BOT: Llenar banca
    BES->>BES: Fase = TURNO_JUGADOR
    BES-->>FE: Partida actualizada
```

---

## Maquina de Estados (Fases)

```mermaid
stateDiagram-v2
    [*] --> INICIO
    INICIO --> COIN_FLIP
    COIN_FLIP --> MULLIGAN_CHECK
    MULLIGAN_CHECK --> SETUP_JUGADOR
    SETUP_JUGADOR --> SETUP_BOT
    SETUP_BOT --> TURNO_JUGADOR

    TURNO_JUGADOR --> RESOLUCION_ATAQUE: Atacar
    TURNO_JUGADOR --> TURNO_BOT: Pasar turno

    RESOLUCION_ATAQUE --> VERIFICAR_KO
    VERIFICAR_KO --> TURNO_BOT: Sin KO
    VERIFICAR_KO --> PREMIO: KO detectado
    PREMIO --> VERIFICAR_GANADOR
    VERIFICAR_GANADOR --> TURNO_BOT: Sin ganador
    VERIFICAR_GANADOR --> FIN_PARTIDA: Hay ganador

    TURNO_BOT --> RESOLUCION_ATAQUE_BOT: Bot ataca
    RESOLUCION_ATAQUE_BOT --> VERIFICAR_KO
    TURNO_BOT --> TURNO_JUGADOR: Bot pasa

    FIN_PARTIDA --> [*]
```

---

## Turno del Jugador

```mermaid
sequenceDiagram
    participant FE as Frontend
    participant BES as BattleEngineService

    Note over FE: Fase TURNO_JUGADOR

    FE->>BES: robarCarta(partidaId)
    BES-->>FE: Carta robada

    opt Jugar energia
        FE->>BES: adjuntarEnergia(partidaId, cardIndex, targetIndex)
        BES-->>FE: Energia adjuntada
    end

    opt Jugar Pokemon basico
        FE->>BES: colocarEnBanca(partidaId, cardIndex)
        BES-->>FE: Pokemon en banca
    end

    opt Evolucionar
        FE->>BES: evolucionarPokemon(partidaId, manoIndex, targetIndex)
        BES-->>FE: Pokemon evolucionado
    end

    opt Retirarse
        FE->>BES: retirarse(partidaId, bancaIndex)
        BES-->>FE: Nuevo activo
    end

    alt Atacar
        FE->>BES: atacar(partidaId, ataqueIndex)
        BES->>BES: Resolver ataque + efectos
        BES->>BES: Verificar KO
        BES-->>FE: Resultado del ataque
    else Pasar turno
        FE->>BES: pasarTurno(partidaId)
        BES-->>FE: Turno del bot
    end
```

---

## Turno del Bot

```mermaid
sequenceDiagram
    participant BES as BattleEngineService
    participant BOT as BotAIService
    participant EB as EstrategiaBasica

    Note over BES: Fase TURNO_BOT

    BES->>BOT: ejecutarTurno(partida)
    BOT->>EB: ejecutarTurno(partida)

    EB->>EB: Robar carta
    
    alt Sin activo
        EB->>EB: Promover desde banca
    end

    EB->>EB: Jugar cartas de la mano
    EB->>EB: evaluarRetiradaEstrategica()
    EB->>EB: gestionarEnergiaBot()
    EB->>EB: intentarAtacar()

    alt Puede atacar
        EB->>EB: Seleccionar mejor ataque (scoring)
        EB->>EB: Ejecutar ataque
    else No puede atacar
        EB->>EB: Pasar turno
    end

    BOT-->>BES: Turno completado
    BES->>BES: Aplicar efectos entre turnos
    Note over BES: Poison -10, Burn -20+coin, Sleep coin
    BES->>BES: Fase = TURNO_JUGADOR
```

---

## Resolucion de Ataque

```mermaid
sequenceDiagram
    participant BES as BattleEngineService
    participant BAS as BattleAttackService
    participant PARSER as AttackEffectParserService
    participant BKS as BattleKoService

    BES->>BAS: resolveAttack(partida, ataqueIndex)
    BAS->>BAS: Verificar costo de energia
    BAS->>BAS: Calcular danio base
    BAS->>BAS: Aplicar debilidad (x2) / resistencia (-20)

    BAS->>PARSER: parseEffects(ataque.texto)
    PARSER-->>BAS: List de BattleCommand

    loop Cada comando
        BAS->>BAS: Ejecutar comando (heal, coinflip, draw, status...)
    end

    BAS->>BAS: Aplicar danio final
    BAS-->>BES: ResultadoAtaque

    BES->>BKS: verificarKO(partida)
    alt Pokemon rival KO
        BKS->>BKS: Mover a pila de descarte
        BKS->>BKS: Otorgar carta premio
        BKS->>BKS: Verificar condicion de victoria
        alt Bot pierde activo
            BKS->>BKS: Seleccionar reemplazo estrategico
        end
    end
```

---

## Condiciones de Victoria

```mermaid
graph TD
    A{Verificar Ganador} --> B{Rival sin premios?}
    B -->|Si| C[VICTORIA: Robo todos los premios]
    B -->|No| D{Rival sin Pokemon en juego?}
    D -->|Si| E[VICTORIA: Sin Pokemon]
    D -->|No| F{Rival sin cartas en mazo?}
    F -->|Si| G[VICTORIA: Deck out]
    F -->|No| H[Continuar partida]
```
