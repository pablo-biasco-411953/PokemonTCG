---
sidebar_position: 1
title: "🏗️ Arquitectura General"
---

# Arquitectura General del Sistema

> Vista completa de la arquitectura full-stack del Pokemon TCG

---

## Vista de Alto Nivel

```mermaid
graph TB
    subgraph Cliente["🖥️ Cliente (Navegador)"]
        FE["Angular 21 + Three.js"]
    end

    subgraph Servidor["☕ Servidor (Spring Boot 3.2.4)"]
        CTRL[Controllers REST]
        SVC[Services]
        REPO[Repositories JPA]
        WS[WebSocket Handler]
        MEM["In-Memory Maps<br/>(Partidas, Salas)"]
    end

    subgraph Datos["🗄️ Persistencia"]
        DB[(MySQL 8.0)]
        JSON[cards.json]
        BACKUP[mazos-backup.json]
    end

    FE -->|HTTP REST| CTRL
    FE -->|WebSocket| WS
    CTRL --> SVC
    WS --> SVC
    SVC --> REPO
    SVC --> MEM
    REPO --> DB
    SVC -->|seed| JSON
    SVC -->|backup| BACKUP
```

---

## Stack Tecnologico

### Backend

| Tecnologia | Version | Uso |
|------------|---------|-----|
| Java | 21 LTS | Lenguaje principal |
| Spring Boot | 3.2.4 | Framework web |
| Spring Data JPA | 3.2.x | ORM / Persistencia |
| Hibernate | 6.x | Implementacion JPA |
| MySQL | 8.0 | Base de datos |
| Spring WebSocket | 3.2.x | Comunicacion real-time |
| Spring Mail | 3.2.x | Envio de emails |
| SpringDoc OpenAPI | 2.3.0 | Documentacion Swagger |
| Maven | 3.9.6 | Build tool |

### Frontend

| Tecnologia | Version | Uso |
|------------|---------|-----|
| Angular | 21.2 | Framework SPA |
| TypeScript | 5.x | Lenguaje |
| Three.js | 0.183 | Graficos 3D (lobby, sobres, batalla) |
| RxJS | 7.8 | Programacion reactiva |
| SCSS | - | Estilos |
| Web Audio API | Nativa | Efectos de sonido procedurales |

---

## Capas del Backend

```mermaid
graph TD
    subgraph Controllers
        AC[AuthController<br/>/api/auth]
        BC[BattleController<br/>/api/battle]
        LC[LobbyRoomController<br/>/api/lobby-rooms]
        MC[MazoController<br/>/api/mazos]
        SC[SobreController<br/>/api/sobres]
        CC[CardController<br/>/api/cards]
        JC[JugadorController<br/>/api/jugadores]
    end

    subgraph Services
        AS[AuthService]
        BES[BattleEngineService]
        BAS2[BattleAttackService]
        BKS[BattleKoService]
        BTS[BattleTurnService]
        BOT[BotAIService]
        LRS[LobbyRoomService]
        MS[MazoService]
        SS[SobreService]
        CCS[CardCatalogService]
        PRS[PasswordRecoveryService]
    end

    subgraph Repositories
        JR[JugadorRepository]
        CR[CardRepository]
        MR[MazoRepository]
    end

    AC --> AS
    AC --> PRS
    BC --> BES
    LC --> LRS
    MC --> MS
    SC --> SS
    CC --> CCS

    AS --> JR
    MS --> MR
    MS --> CR
    SS --> CR
    CCS --> CR
    BES --> JR
```

---

## Comunicacion Frontend-Backend

```mermaid
graph LR
    subgraph Frontend
        LOGIN[LoginComponent]
        LOBBY[LobbyComponent]
        BATTLE[BattleBoardComponent]
        DECK[DeckBuilderComponent]
    end

    subgraph "REST API (HTTP)"
        AUTH[/api/auth]
        BAPI[/api/battle]
        LAPI[/api/lobby-rooms]
        MAPI[/api/mazos]
        SAPI[/api/sobres]
    end

    subgraph "WebSocket"
        WSLOBBY[/lobby-ws]
    end

    LOGIN --> AUTH
    LOBBY --> LAPI
    LOBBY --> SAPI
    LOBBY --> MAPI
    LOBBY --> WSLOBBY
    BATTLE --> BAPI
    DECK --> MAPI
```

---

## Datos In-Memory vs Persistidos

| Dato | Almacenamiento | Razon |
|------|---------------|-------|
| Jugadores, Cartas, Mazos | MySQL (JPA) | Datos permanentes |
| Partidas activas | `ConcurrentHashMap` | Baja latencia en tiempo real |
| Salas del lobby | `ConcurrentHashMap` | Efimeras, no necesitan persistencia |
| Posiciones de jugadores (lobby 3D) | WebSocket (memoria) | Datos de sesion |
| Backup de mazos | Archivo JSON | Proteccion ante reinicios |
| Catalogo de cartas | cards.json + MySQL | Seed inicial desde JSON |

---

## Deployment

```mermaid
graph LR
    subgraph Desarrollo
        DEV_FE["ng serve :4200"]
        DEV_BE["spring-boot:run :8080"]
        DEV_DB["Docker MySQL :3306"]
    end

    subgraph Produccion
        RENDER["Render (Docker)"]
        PROD_DB["MySQL Externo"]
        STATIC["Static Host"]
    end

    DEV_FE --> DEV_BE
    DEV_BE --> DEV_DB

    STATIC --> RENDER
    RENDER --> PROD_DB
```
