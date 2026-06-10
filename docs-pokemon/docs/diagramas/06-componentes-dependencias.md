---
sidebar_position: 6
title: "🧩 Componentes y Dependencias"
---

# Componentes y Dependencias

> Arbol de componentes Angular y sus servicios inyectados

---

## Rutas de la Aplicacion

```mermaid
graph TD
    ROOT[AppComponent] --> ROUTER[Angular Router]
    ROUTER --> |"/"| LOGIN[LoginComponent]
    ROUTER --> |"/lobby"| LOBBY[LobbyComponent]
    ROUTER --> |"/deck-builder"| DECK[DeckBuilderComponent]
    ROUTER --> |"/battle/:id"| BATTLE[BattleBoardComponent]
    ROUTER --> |"/**"| LOGIN
```

| Ruta | Componente | Descripcion |
|------|-----------|-------------|
| `/` | Redirect a `/login` | - |
| `/login` | `LoginComponent` | Pantalla de login/registro |
| `/lobby` | `LobbyComponent` | Campus 3D multiplayer |
| `/deck-builder` | `DeckBuilderComponent` | Constructor de mazos |
| `/battle/:id` | `BattleBoardComponent` | Tablero de batalla |
| `/**` | Redirect a `/login` | Ruta no encontrada |

---

## Arbol de Componentes

```mermaid
graph TD
    APP[AppComponent] --> LS[LanguageSelectorComponent]

    subgraph Login
        LC[LoginComponent]
    end

    subgraph Lobby
        LOBBY[LobbyComponent]
        LOBBY --> ASC[AperturaSobreComponent]
        LOBBY --> DBC_E["DeckBuilderComponent (embedded)"]
    end

    subgraph Battle
        BB[BattleBoardComponent]
        BB --> ABP[AbilitiesPanelComponent]
        BB --> DBP[DebugPanelComponent]
        BB --> DMP[DiscardModalComponent]
        BB --> CDP[CardDetailPanelComponent]
    end

    subgraph "Standalone"
        DBC[DeckBuilderComponent]
    end
```

---

## Servicios por Componente

### LoginComponent

```mermaid
graph LR
    LC[LoginComponent] --> AS[AuthService]
    LC --> SS[SoundService]
    LC --> I18N[I18nService]
    LC --> THREE_L["Three.js (background 3D)"]
```

### LobbyComponent

```mermaid
graph LR
    LOBBY[LobbyComponent] --> SOS[SobreService]
    LOBBY --> LRS[LobbyRoomService]
    LOBBY --> MS[MazoService]
    LOBBY --> JS[JugadorService]
    LOBBY --> BS[BattleService]
    LOBBY --> CS[CardService]
    LOBBY --> IPS[ImagePreloaderService]
    LOBBY --> I18N[I18nService]
    LOBBY --> WS["WebSocket (nativo)"]
    LOBBY --> THREE_L["Three.js (campus 3D)"]
```

### BattleBoardComponent

```mermaid
graph LR
    BB[BattleBoardComponent] --> BBS[BattleBoardStateService]
    BB --> BBA[BattleBoardActionService]
    BB --> BBC[BattleBoardCombatService]
    BB --> BBT[BattleBoardTurnService]
    BB --> BBATK[BattleBoardAttackService]
    BB --> BBUI[BattleBoardUIService]
    BB --> SS[SoundService]
    BB --> I18N[I18nService]
    BB --> THREE_B["Three.js (tablero 3D)"]
```

### DeckBuilderComponent

```mermaid
graph LR
    DBC[DeckBuilderComponent] --> JS[JugadorService]
    DBC --> MS[MazoService]
    DBC --> IPS[ImagePreloaderService]
    DBC --> I18N[I18nService]
```

---

## Servicios - Capas

```mermaid
graph TD
    subgraph "Core Services (Singleton)"
        AS[AuthService]
        SS[SoundService]
        IPS[ImagePreloaderService]
        I18N[I18nService]
        JS[JugadorService]
        CS[CardService]
    end

    subgraph "Feature Services"
        subgraph Lobby
            SOS[SobreService]
            LRS[LobbyRoomService]
        end
        subgraph Battle
            BService[BattleService]
            BBS[BattleBoardStateService]
            BBA[BattleBoardActionService]
            BBC[BattleBoardCombatService]
            BBT[BattleBoardTurnService]
            BBATK[BattleBoardAttackService]
            BBUI[BattleBoardUIService]
        end
        subgraph "Deck Builder"
            MS[MazoService]
        end
    end

    subgraph "External"
        HTTP["HttpClient (REST)"]
        WS["WebSocket (nativo)"]
        WAA["Web Audio API"]
        THREEJS["Three.js"]
    end

    AS --> HTTP
    JS --> HTTP
    CS --> HTTP
    SOS --> HTTP
    LRS --> HTTP
    MS --> HTTP
    BService --> HTTP
    SS --> WAA
```

---

## Pipes y Utilidades Compartidas

```mermaid
graph TD
    subgraph "i18n Module"
        I18N[I18nService] --> TP[TranslatePipe]
    end

    subgraph "Config"
        AC["api-config.ts<br/>getBackendUrl()<br/>getWsUrl()"]
    end

    subgraph "Shared Models"
        CARD["card.ts"]
        JUG["jugador.ts"]
        MAZO["mazo.ts"]
        BATTLE["battle.ts"]
    end

    TP --> |"usado en"| TEMPLATES["Todos los templates"]
    AC --> |"usado en"| SERVICES["Todos los services HTTP"]
    CARD --> |"usado en"| COMPONENTS["Todos los componentes"]
```

---

## Dependencias Externas Clave

| Dependencia | Version | Componentes que la usan |
|-------------|---------|------------------------|
| `three` | 0.183 | Login, Lobby, AperturaSobre, BattleBoard |
| `@angular/cdk` | 21.2 | Drag & Drop en batalla |
| `rxjs` | 7.8 | Todos los servicios HTTP |
| `@angular/forms` | 21.2 | Login, DeckBuilder |
