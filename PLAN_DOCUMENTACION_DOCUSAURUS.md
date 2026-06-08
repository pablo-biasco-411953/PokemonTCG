# 📖 Plan de Documentación Pokemon TCG con Docusaurus

**Fecha**: 2026-06-08  
**Objetivo**: Documentar exhaustivamente el proyecto Pokemon TCG (Trading Card Game) con Docusaurus, temática Pokémon integrada, cobertura técnica y de jugabilidad.

---

## 📊 Visión General

Este proyecto es un **Trading Card Game Full-Stack** con:
- **Backend**: Spring Boot 3.2.4 (Java 21) + H2 Database
- **Frontend**: React/TypeScript (moderna)
- **Mecánicas**: Batalla turn-based, sobres de cartas, construcción de mazos, WebSocket para lobby

La documentación se divide en **3 pilares**:

1. **🎮 Jugabilidad & Reglas** - Para jugadores
2. **⚙️ Arquitectura Técnica** - Para desarrolladores
3. **🚀 Setup & Operaciones** - Para DevOps/Deployment

---

## 📁 Estructura Docusaurus Propuesta

```
docs/
├── intro.md                              # Portada temática Pokémon
├── 
├── jugabilidad/                          # 🎮 Pillar 1: Game Design
│   ├── _category_.json
│   ├── 01-overview-juego.md
│   ├── 02-mecanicas-basicas.md
│   ├── 03-cartas-tipos-energia.md
│   ├── 04-evolucion-pokemon.md
│   ├── 05-construccion-mazos.md
│   ├── 06-batalla-reglas.md
│   ├── 07-fases-turno.md
│   ├── 08-efectos-habilidades.md
│   ├── 09-sobres-booster.md
│   └── 10-items-equipamiento.md
│
├── tecnica/                              # ⚙️ Pillar 2: Architecture
│   ├── _category_.json
│   ├── 01-stack-tecnologico.md
│   ├── 02-arquitectura-backend.md
│   ├── 03-arquitectura-frontend.md
│   ├── 04-database-design.md
│   ├── 05-api-endpoints.md
│   ├── 06-patrones-diseño.md
│   ├── 07-batalla-engine.md
│   ├── 08-websocket-lobby.md
│   ├── 09-autenticacion.md
│   ├── 10-manejo-estado.md
│   └── 11-algoritmos-clave.md
│
├── componentes-detallados/               # 🔍 Nivel 3: Línea por línea
│   ├── _category_.json
│   │
│   ├── backend/
│   │   ├── _category_.json
│   │   ├── controllers/
│   │   │   ├── auth-controller.md
│   │   │   ├── battle-controller.md
│   │   │   ├── card-controller.md
│   │   │   ├── jugador-controller.md
│   │   │   ├── mazo-controller.md
│   │   │   └── sobre-controller.md
│   │   │
│   │   ├── services/
│   │   │   ├── battle-service.md
│   │   │   ├── card-service.md
│   │   │   ├── mazo-service.md
│   │   │   ├── jugador-service.md
│   │   │   └── auth-service.md
│   │   │
│   │   ├── models/
│   │   │   ├── card.md
│   │   │   ├── jugador.md
│   │   │   ├── mazo.md
│   │   │   ├── partida.md
│   │   │   └── enums.md
│   │   │
│   │   ├── repositories/
│   │   │   └── repositories-overview.md
│   │   │
│   │   └── config/
│   │       ├── cors-config.md
│   │       ├── data-loader.md
│   │       └── security-config.md
│   │
│   └── frontend/
│       ├── _category_.json
│       ├── services/
│       │   ├── auth-service.md
│       │   ├── battle.service.md
│       │   ├── card.service.md
│       │   ├── jugador.service.md
│       │   └── shared-services.md
│       │
│       ├── components/
│       │   ├── battle-board.md
│       │   ├── lobby.md
│       │   ├── apertura-sobre.md
│       │   ├── builder-mazo.md
│       │   └── shared-components.md
│       │
│       ├── types/
│       │   └── battle-types.md
│       │
│       └── hooks-utils/
│           └── hooks-utilities.md
│
├── algoritmos/                           # 🧠 Algorithms Deep Dive
│   ├── _category_.json
│   ├── batalla-ia.md
│   ├── selector-cartas.md
│   ├── validador-mazos.md
│   └── matchmaking.md
│
├── operaciones/                          # 🚀 Setup & DevOps
│   ├── _category_.json
│   ├── setup-local.md
│   ├── variables-entorno.md
│   ├── docker-deployment.md
│   ├── database-setup.md
│   ├── scripts-utiles.md
│   ├── troubleshooting.md
│   └── performance-tips.md
│
├── diagramas/                            # 📐 Visuals
│   ├── _category_.json
│   ├── arquitectura-general.md
│   ├── flujo-batalla.md
│   ├── modelo-datos.md
│   ├── flujo-autenticacion.md
│   ├── flujo-websocket.md
│   └── componentes-dependencias.md
│
└── glosario.md                           # 📚 Glossary (términos Pokémon + técnicos)

sidebars.js                              # Configuración navegación
docusaurus.config.js                     # Configuración global con temática
```

---

## 🎯 Fase 1: Inicialización de Docusaurus

### 1.1 Crear proyecto Docusaurus con temática Pokémon
```bash
# En la raíz del proyecto
npx create-docusaurus@latest docs-pokemon classic

# O instalación manual si no funciona
npm install --save-dev docusaurus @docusaurus/core @docusaurus/preset-classic
```

### 1.2 Configurar `docusaurus.config.js`
- **Title**: "⚡ Pokémon TCG - Documentación Oficial"
- **Logo**: Sprite de Pikachu/Pokéball
- **Colors temáticos**: 
  - Primario: Amarillo Pokémon (#FFCC00)
  - Secundario: Azul (#0066CC)
  - Acentos: Rojo, Verde (tipos)
- **Lenguaje**: Español

### 1.3 Crear estructura de carpetas
- `docs/` - Todos los documentos MD
- `static/` - Imágenes, gifs, sprites
- `src/pages/` - Páginas customizadas
- `docusaurus.config.js` - Config

### 1.4 Agregar plugins Docusaurus
- `@docusaurus/plugin-mermaid` - Para diagramas
- `docusaurus-plugin-sass` - Estilos customizados

---

## 🎮 Fase 2: Documentación de Jugabilidad (PILLAR 1)

### 2.1 **01-overview-juego.md**
**Temática**: "¡Bienvenido a tu Aventura Pokémon!"

Contenido:
- ¿Qué es el Pokémon TCG?
- Objetivo del juego
- Conceptos fundamentales (turno, energía, pokémon)
- Pantallas principales del juego
- Screenshots de UI

### 2.2 **02-mecanicas-basicas.md**
Conceptos:
- **Baraja**: 60 cartas mínimo
- **Mano**: 7 cartas iniciales + mulligan
- **Bench**: Pokémon en espera (5 espacios)
- **Energía**: Recursos para atacar
- **Monedas**: Mecanismo de azar
- **Estados** (Parálisis, Quemadura, Confusión, etc)

Diagramas Mermaid del tablero

### 2.3 **03-cartas-tipos-energia.md**
Tipos de cartas:
- **Pokémon Básicos**: Pueden estar en juego sin evolución
- **Pokémon Evolución (Etapa 1, Etapa 2)**
- **Entrenador**: Objetos, Soportes, Estadios
- **Energía**: 11 tipos (Fuego, Agua, Planta, etc)

Tabla interactiva de tipos y efectividades

### 2.4 **04-evolucion-pokemon.md**
Sistema de evolución:
- Línea evolutiva (Bulbasaur → Ivysaur → Venusaur)
- Requiere Pokémon anterior en banco
- Efecto al evolucionar
- Restricción: 1 evolución por turno
- Ejemplos prácticos

### 2.5 **05-construccion-mazos.md**
Reglas de construcción:
- 60 cartas exacto
- Máximo 4 copias de misma carta
- Mínimo 20 energías (recomendación)
- Arquetipos de mazo (Control, Agro, Combo)
- UI del mazo builder

### 2.6 **06-batalla-reglas.md**
Reglas de batalla:
- Condiciones de victoria (KO de 6 Pokémon)
- Sistema de turnos
- Daño y curación
- Estados de confusión, parálisis, etc
- Pérdida por decking (sin cartas)

### 2.7 **07-fases-turno.md**
Desglose exacto de un turno:

```
TURNO = Fase de Inicio → Fase Principal → Fase de Ataque → Fase de Limpieza

1. INICIO
   - Sacar carta del mazo
   - Aplicar efectos persistentes
   
2. PRINCIPAL
   - Jugar 1 Pokémon desde mano
   - Unir 1 energía (máximo)
   - Usar efectos de Entrenador
   - Usar Ataques de banco
   - Usar Habilidades
   
3. ATAQUE
   - Ejecutar 1 ataque del Pokémon activo
   - Aplicar daño
   - Aplicar efectos secundarios
   
4. LIMPIEZA
   - Fin de turno
   - Descartar cartas marcadas
```

Diagrama interactivo con ejemplos

### 2.8 **08-efectos-habilidades.md**
Tipos de efectos:
- **Habilidades Pasivas**: Siempre activas
- **Ataques**: Requieren energía y turno
- **Efectos Secundarios**: Daño a banco, descarte, etc
- **Efectos Persistentes**: Estados y condiciones

Ejemplos de cartas reales del juego

### 2.9 **09-sobres-booster.md**
Sistema de sobres:
- ¿Qué contiene un sobre?
- Garantías de cartas raras
- Probabilidades
- Efectos de Pull Rate
- Sistema de booster en el juego

### 2.10 **10-items-equipamiento.md**
Cartas de Entrenador:
- Items de una sola vez
- Herramientas persistentes (equipamiento)
- Soportes (trainer support)
- Estadios

---

## ⚙️ Fase 3: Documentación Técnica (PILLAR 2)

### 3.1 **01-stack-tecnologico.md**

```
Backend
├── Java 21
├── Spring Boot 3.2.4
│   ├── Spring Web (REST)
│   ├── Spring Data JPA
│   ├── Spring Security
│   └── Spring WebSocket
└── H2 Database (dev), PostgreSQL (prod recomendado)

Frontend
├── React 18+
├── TypeScript
├── RxJS (State Management)
├── Three.js (Gráficos 3D)
└── Angular (legacy, en transición)

Infraestructura
├── Docker
├── Docker Compose
├── Git + GitHub
└── Maven (build)
```

Justificación de cada herramienta

### 3.2 **02-arquitectura-backend.md**

Capas:
```
Controller Layer (HTTP)
    ↓
Service Layer (Business Logic)
    ↓
Repository Layer (Data Access)
    ↓
Entity/Model Layer (Entities)
    ↓
H2 Database
```

Detalle de cada capa:
- **Controllers**: `AuthController`, `BattleController`, etc
- **Services**: Lógica de negocio
- **Repositories**: Acceso a BD con Spring Data JPA
- **Models**: Entidades JPA

### 3.3 **03-arquitectura-frontend.md**

Estructura:
```
App (Root)
├── Core
│   └── Services (auth, card, jugador, sound)
├── Features
│   ├── Battle
│   ├── Lobby
│   └── Deck Builder
└── Shared
    ├── Components
    ├── Pipes
    └── Directives
```

State Management con RxJS

### 3.4 **04-database-design.md**

Schema:
```sql
Jugador
  ├── id (PK)
  ├── username
  ├── email
  ├── password_hash
  ├── fecha_registro
  └── stats (wins, losses, etc)

Carta
  ├── id (PK)
  ├── nombre
  ├── tipo (enum)
  ├── hp
  ├── ataques (JSON)
  ├── habilidades (JSON)
  ├── evolucion_de (FK a Carta)
  └── rareza

Mazo
  ├── id (PK)
  ├── jugador_id (FK)
  ├── nombre
  ├── cartas (relación M:M con Carta)
  └── fecha_creacion

Partida
  ├── id (PK)
  ├── jugador1_id (FK)
  ├── jugador2_id (FK)
  ├── ganador_id (FK)
  ├── estado (enum: LOBBY, STARTED, FINISHED)
  └── timestamp
```

Diagrama ER completo

### 3.5 **05-api-endpoints.md**

Todas las rutas REST:

```
AUTH
  POST /api/auth/register
  POST /api/auth/login
  POST /api/auth/refresh
  POST /api/auth/forgot-password

JUGADOR
  GET /api/jugador/perfil
  GET /api/jugador/:id
  PUT /api/jugador/:id
  DELETE /api/jugador/:id

CARTAS
  GET /api/cartas
  GET /api/cartas/:id
  GET /api/cartas/tipo/:tipo
  GET /api/cartas/rareza/:rareza

MAZOS
  POST /api/mazos
  GET /api/mazos/:id
  PUT /api/mazos/:id
  DELETE /api/mazos/:id
  POST /api/mazos/:id/validar

BATALLA
  POST /api/batalla/iniciar
  POST /api/batalla/:id/accion
  GET /api/batalla/:id/estado
  
WEBSOCKET
  /ws/batalla/:id
  /ws/lobby
```

Documentación OpenAPI/Swagger integrada

### 3.6 **06-patrones-diseño.md**

Patrones implementados:
- **Service Pattern**: Controllers → Services
- **Repository Pattern**: Abstracción de BD
- **DTO Pattern**: Data Transfer Objects
- **Observer Pattern**: RxJS Observables
- **Singleton**: Services en Angular
- **Factory Pattern**: Creación de entidades

Ejemplos de código para cada patrón

### 3.7 **07-batalla-engine.md**

Lógica de batalla:
- Máquina de estados de turno
- Cálculo de daño
- Validación de acciones
- Sistema de efectos secundarios
- Inyección de daño (damage injection)

Pseudocódigo de algoritmo principal

### 3.8 **08-websocket-lobby.md**

Comunicación en tiempo real:
- Conexión WebSocket
- Mensajes de lobby (join room, ready, start)
- Sincronización de estado
- Desconexión y reconexión

Diagrama de secuencia

### 3.9 **09-autenticacion.md**

Sistema de autenticación:
- JWT Tokens
- PKCE flow
- Refresh tokens
- Session storage
- CORS configuration

### 3.10 **10-manejo-estado.md**

RxJS State Management:
- Subjects
- BehaviorSubjects
- Observables
- Operators (map, filter, switchMap)

Ejemplo: Battle State Management

### 3.11 **11-algoritmos-clave.md**

Algoritmos complejos:
- Validación de movimientos (es legal jugar esta carta ahora?)
- Cálculo de daño con modificadores
- Búsqueda de línea evolutiva
- Matchmaking (si aplica)

---

## 🔍 Fase 4: Documentación de Componentes (NIVEL LÍNEA POR LÍNEA)

### 4.1 Controllers (Backend)

**auth-controller.md**
- Endpoints: POST /register, /login, /forgot-password
- Validaciones
- Mapeo a DTOs
- Manejo de errores

Ejemplo completo de cada método con anotaciones

**battle-controller.md**
- POST /batalla/iniciar
- POST /batalla/{id}/accion
- GET /batalla/{id}/estado
- Acciones posibles: jugar-carta, unir-energia, atacar, pasar-turno, retirada

**card-controller.md**
- GET /cartas
- GET /cartas/{id}
- Filtros: tipo, rareza, ataque-min, hp-min

**jugador-controller.md**
- Endpoints de perfil
- Stats (wins, losses, elo)
- Personalización

**mazo-controller.md**
- CRUD de mazos
- Validación de construcción
- Exportar/importar

**sobre-controller.md**
- Abrir sobre
- Generar cartas

### 4.2 Services (Backend)

**battle-service.md**
- Métodos principales:
  - `iniciarPartida(jugador1, jugador2)`
  - `validarAccion(accion, estado)`
  - `ejecutarAtaque(atacante, objetivo)`
  - `aplicarEfectoSecundario(efecto)`
  - `verificarCondicionVictoria(jugador)`

Cada método con:
- Firma
- Descripción
- Parámetros
- Retorno
- Casos de error
- Ejemplo de uso

**card-service.md**
- Gestión de cartas
- Cálculo de rareza
- Búsqueda y filtrado

**mazo-service.md**
- Validación de mazo (60 cartas, máximo 4 copias)
- Estadísticas del mazo (type distribution, energy count)

### 4.3 Models (Backend)

**card.md**
- Atributos: id, nombre, tipo, hp, ataques[], habilidades[], rareza
- Ataques (estructura):
  - nombre, costo energético, daño, efecto secundario
- Habilidades (estructura)
- Evolución (relación)
- JSON de ejemplo

**jugador.md**
- Atributos: id, username, email, stats (wins/losses/elo)
- Relaciones: mazos[], partidas[]

**mazo.md**
- Atributos: id, nombre, cartas[], usuario_id
- Métodos de validación
- Ejemplo de mazo válido (60 cartas listadas)

**partida.md**
- Estados: LOBBY, STARTED, FINISHED
- Información: jugador1, jugador2, ganador
- Historial de acciones

### 4.4 Frontend - Services

**auth-service.md**
- `login(credentials)` → Observable<Token>
- `register(userData)` → Observable<User>
- `logout()` → void
- `refreshToken()` → Observable<Token>
- Manejo de errores 401

**battle.service.md**
- Conexión WebSocket
- `enviarAccion(accion)` → Observable
- `obtenerEstado()` → BehaviorSubject<BattleState>
- Subscripciones y cleanup

**card.service.md**
- Caché de cartas
- `obtenerCartas()` → Observable<Card[]>
- Búsqueda local

**jugador.service.md**
- Datos de perfil
- Estadísticas

### 4.5 Frontend - Components

**battle-board.md**
- Estructura del componente
- Inputs y Outputs
- Template HTML structure
- CSS classes temáticas
- Lógica de interacción

Desglose de sub-componentes:
- Player info (HP, bench, hand size)
- Board display (active pokémon, bench)
- Action buttons (play card, attack, pass turn)
- Debug panel (F3)

**lobby.md**
- Componente raíz del lobby
- Gestión de salas
- WebSocket integration
- Estados: searching, waiting, ready

**apertura-sobre.md**
- Animación de apertura
- Lógica de reveal
- Sonidos

**builder-mazo.md**
- Búsqueda de cartas
- Adición/eliminación de cartas
- Validación en tiempo real
- Guardar mazo

---

## 🧠 Fase 5: Algoritmos & Deep Dives (ALGORITMOS CLAVE)

### 5.1 **batalla-ia.md**
Si hay IA:
- Árbol de decisiones
- Evaluación de board state
- Selección de mejor acción

### 5.2 **selector-cartas.md**
Algoritmo de selección de cartas al abrir sobre:
- Garantías de rareza
- Pool de cartas disponibles
- Seeding/RNG

### 5.3 **validador-mazos.md**
Validación exhaustiva:
- 60 cartas exacto
- Máximo 4 copias
- Mínimo 20 energías (recomendado)
- Líneas evolutivas válidas

### 5.4 **matchmaking.md**
Si hay sistema de ranked:
- ELO rating
- Búsqueda de oponentes
- Fair play

---

## 🚀 Fase 6: Operaciones & Deployment (PILLAR 3)

### 6.1 **setup-local.md**

```bash
# Clonar
git clone <repo>
cd PokemonTCG

# Backend
./mvnw -f backend/pom.xml spring-boot:run

# Frontend (en otra terminal)
cd frontend
npm install
npm start
```

Requisitos:
- Java 21
- Node.js 18+
- npm/yarn

### 6.2 **variables-entorno.md**

Backend (`backend/src/main/resources/application.properties`):
- `spring.datasource.url`
- `spring.jpa.database-platform`
- `server.port`
- `jwt.secret`
- `jwt.expiration`

Frontend (`.env`):
- `REACT_APP_API_URL`
- `REACT_APP_WS_URL`

### 6.3 **docker-deployment.md**

```dockerfile
# Backend Dockerfile
FROM openjdk:21-slim
WORKDIR /app
COPY backend/target/*.jar app.jar
EXPOSE 8080
CMD ["java", "-jar", "app.jar"]

# Frontend Dockerfile
FROM node:18-alpine
WORKDIR /app
COPY frontend .
RUN npm install && npm run build
EXPOSE 3000
CMD ["npm", "start"]
```

Docker Compose:
- Backend service
- Frontend service
- PostgreSQL service (opcional)

### 6.4 **database-setup.md**

H2 (dev):
- Automático con Spring Boot
- Console en `/h2-console`

PostgreSQL (prod):
- Script de creación
- Migrations

### 6.5 **scripts-utiles.md**

Bash scripts para:
- Limpiar caché
- Resetear BD
- Build completo
- Deploy a servidor

### 6.6 **troubleshooting.md**

Problemas comunes:
- Puerto 8080 en uso
- Node modules corruptos
- JWT token expirado
- WebSocket connection refused

### 6.7 **performance-tips.md**

Optimizaciones:
- Lazy loading de componentes
- Image preloading (para cartas)
- Caching de API responses
- Database indexing

---

## 📐 Fase 7: Diagramas Visuales

### 7.1 **arquitectura-general.md**

Diagrama Mermaid:
```
┌─ Frontend (React) ──────────────┐
│  ├─ Battle Board                │
│  ├─ Lobby                       │
│  └─ Deck Builder                │
└────────────┬─────────────────────┘
             │ HTTP REST + WebSocket
             ↓
┌─ Backend (Spring Boot) ─────────┐
│  ├─ Controllers                 │
│  ├─ Services                    │
│  └─ Repositories                │
└────────────┬─────────────────────┘
             │ JDBC/JPA
             ↓
┌─ Database (H2/PostgreSQL) ──────┐
│  ├─ Jugador                     │
│  ├─ Carta                       │
│  ├─ Mazo                        │
│  └─ Partida                     │
└─────────────────────────────────┘
```

### 7.2 **flujo-batalla.md**

Diagrama de secuencia:
```
Jugador A         Sistema         Jugador B
   │                │                │
   │─ Draw Card ──→ │                │
   │                │                │
   │─ Play Card ──→ │                │
   │                │─ Validate ────→│
   │                │                │
   │─ Attack ──────→│                │
   │                │─ Damage ──────→│
   │                │                │
   │                │─ End Turn ────→│
   │                │                │
   │                │              Play
```

### 7.3 **modelo-datos.md**

Diagrama ER de BD

### 7.4 **flujo-autenticacion.md**

Diagrama JWT + PKCE

### 7.5 **flujo-websocket.md**

Secuencia de conexión WebSocket

### 7.6 **componentes-dependencias.md**

Árbol de componentes de React

---

## 📚 Fase 8: Glosario & Referencia

### **glosario.md**

Términos del juego:
- **Pokémon Activo**: Pokémon en batalla
- **Banco**: Pokémon en espera (máx 5)
- **Mano**: Cartas en poder del jugador
- **Mazo**: Deck de 60 cartas
- **Energía**: Recurso para atacar
- **Ataque**: Acción con costo energético
- **Habilidad**: Efecto pasivo
- **Estado**: Parálisis, Quemadura, etc
- **KO**: Pokémon derrotado
- **Mulligan**: Redraw si mano inicial es mala
- **Bench**: Pokémon en espera

Términos técnicos:
- **DTO**: Data Transfer Object
- **JPA**: Java Persistence API
- **JWT**: JSON Web Token
- **WebSocket**: Comunicación bidireccional
- **Observable**: Stream reactivo
- **RxJS**: Reactive JavaScript library

---

## 🎨 Fase 9: Temática Pokémon

### Elementos visuales a integrar:

**Colores por tipo** (en código):
```css
.type-fuego { color: #FF4500; }
.type-agua { color: #4169E1; }
.type-planta { color: #228B22; }
.type-electrico { color: #FFD700; }
.type-psiquico { color: #DA70D6; }
... (11 tipos totales)
```

**Sprites y imágenes**:
- Pokémon icons para cada tipo
- Pokéball para botones
- Badges de rareza
- Efectos visuales (sparks, explosions)

**Tipografía**:
- Font: "Poppins" o similar (redondeada, amigable)
- Títulos: Bold, olor temático

**Animaciones**:
- Card flip on hover
- Attack animations
- Evolution sequences

---

## ✅ Checklist de Implementación

### Paso 0: Preparación
- [ ] Crear repo para docs (dentro de PokemonTCG)
- [ ] Inicializar Docusaurus
- [ ] Setup tema y colores

### Paso 1: Estructura Base
- [ ] Crear carpetas `docs/`
- [ ] Crear `_category_.json` en cada sección
- [ ] Crear `intro.md` con temática

### Paso 2: Jugabilidad (🎮)
- [ ] 01-overview
- [ ] 02-mecanicas
- [ ] 03-cartas
- [ ] 04-evolucion
- [ ] 05-construccion
- [ ] 06-batalla
- [ ] 07-fases
- [ ] 08-efectos
- [ ] 09-sobres
- [ ] 10-items

### Paso 3: Técnica (⚙️)
- [ ] 01-stack
- [ ] 02-arch-backend
- [ ] 03-arch-frontend
- [ ] 04-db-design
- [ ] 05-api-endpoints
- [ ] 06-patrones
- [ ] 07-batalla-engine
- [ ] 08-websocket
- [ ] 09-auth
- [ ] 10-state
- [ ] 11-algoritmos

### Paso 4: Componentes (🔍)
- [ ] Backend controllers
- [ ] Backend services
- [ ] Backend models
- [ ] Frontend services
- [ ] Frontend components

### Paso 5: Algoritmos
- [ ] Batalla IA
- [ ] Selector cartas
- [ ] Validador mazos
- [ ] Matchmaking

### Paso 6: Operaciones (🚀)
- [ ] Setup local
- [ ] Env variables
- [ ] Docker
- [ ] Database
- [ ] Scripts
- [ ] Troubleshooting
- [ ] Performance

### Paso 7: Diagramas
- [ ] Arquitectura general
- [ ] Flujo batalla
- [ ] Modelo datos
- [ ] Flujo auth
- [ ] Flujo WebSocket
- [ ] Dependencias

### Paso 8: Glosario
- [ ] Términos de juego
- [ ] Términos técnicos

### Paso 9: Polish
- [ ] Screenshots
- [ ] GIFs animados
- [ ] Correcciones gramática
- [ ] Links cruzados
- [ ] Build & test Docusaurus

---

## 📊 Estimación de Trabajo

**Total de documentos**: ~80 archivos `.md`

| Fase | Documentos | Tiempo Estimado | Prioridad |
|------|-----------|-----------------|-----------|
| Inicialización | 1 | 30 min | 🔴 CRÍTICA |
| Jugabilidad | 10 | 4-5h | 🟠 ALTA |
| Técnica | 11 | 6-8h | 🟠 ALTA |
| Componentes | 20+ | 10-15h | 🟡 MEDIA |
| Algoritmos | 4 | 2-3h | 🟡 MEDIA |
| Operaciones | 7 | 2-3h | 🟢 BAJA |
| Diagramas | 6 | 3-4h | 🟢 BAJA |
| Glosario | 1 | 1-2h | 🟢 BAJA |
| Polish | - | 2-3h | 🟢 BAJA |

**TOTAL**: ~30-40 horas de documentación exhaustiva

---

## 🎯 Objetivo Final

Una **documentación interactiva, bella y completa** que permita a cualquier desarrollador:

1. **Entender el juego** (reglas, mecánicas)
2. **Entender la arquitectura** (cómo está construido)
3. **Leer código línea por línea** (componente por componente)
4. **Aprender algoritmos clave** (cómo funciona la batalla)
5. **Hacer deploy** (cómo poner en producción)
6. **Contribuir** (cómo agregar features)

Todo con **temática Pokémon integrada** para que sea visualmente atractivo y divertido de leer.

---

**Estado**: 📋 Plan listo  
**Próximo paso**: Fase 1 - Inicializar Docusaurus
