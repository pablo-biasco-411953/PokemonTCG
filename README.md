# Pokemon TCG - Sistema de Juego


<div align='center'>
  <img src='https://img.icons8.com/color/96/000000/pokemon.png' alt='Pokemon Logo'/>
  <h1>Pokémon Trading Card Game</h1>
  <p>Sistema completo de juego para Pokémon TCG con backend en Spring Boot y frontend en Angular</p>
</div>

## Descripci�n General

El proyecto PokemonTCG es un sistema completo que permite jugar al juego de cartas coleccionables Pokémon TCG (Trading Card Game) en una plataforma digital. El juego implementa las reglas b�sicas del TCG, incluyendo batallas entre jugadores, manejo de mazos, cartas, energía y ataques.

## Estructura del Proyecto

PokemonTCG/
├── backend/                # Servidor Spring Boot (Java 21)
│   └── src/main/java/com/pokemon/tcg/
│       ├── controller/     # Endpoints REST
│       ├── model/          # Entidades y lógica de batalla
│       ├── repository/     # Persistencia JPA
│       └── service/        # Lógica de negocio e IA
├── frontend/               # Interfaz Angular 17
└── README.md

## Tecnolog�as Utilizadas

### Backend (Spring Boot)
- Lenguaje: Java 21
- Framework: Spring Boot 3.2.4
- Base de Datos: H2 Database (en memoria)
- Dependencias principales:
  - Spring Web (REST API)
  - Spring Data JPA
  - H2 Database

### Frontend (Angular)
- Framework: Angular 17
- Lenguaje: TypeScript
- Herramientas: Node.js, npm

## Estructura de Datos

### Modelos Principales

#### Carta (Card)
Representa una carta de Pokémon TCG con las siguientes propiedades:
- id: Identificador único de la carta
- nombre: Nombre de la carta
- hp: Puntos de salud
- tipo: Tipo de Pokémon (Fire, Water, Grass, etc.)
- imagen: URL de la imagen de la carta
- supertype: Tipo de carta (Pokémon, Trainer, Energy)
- subtypes: Subtipos como Basic, Stage 1, Stage 2
- evolucionDe: Nombre de la carta de la que evoluciona
- ataques: Lista de ataques disponibles
- debilidades: Debilidades del Pokémon
- resistencias: Resistencias del Pokémon

#### Jugador (Jugador)
- id: Identificador único
- username: Nombre de usuario
- mazos: Lista de mazos del jugador

#### Mazo (Mazo)
- id: Identificador único
- nombre: Nombre del mazo
- cartas: Lista de cartas en el mazo

#### Partida (Partida)
Representa una batalla en curso con:
- id: Identificador de la partida
- jugador1 y jugador2: Jugadores participantes
- tableroJugador1 y tableroJugador2: Estados del tablero
- fase: Fase actual de la partida (Draw, Attack, End)
- turno: Turno actual (Jugador1 o Jugador2)

## Funcionalidades Implementadas

### 1. Gesti�n de Usuarios y Mazos
- Registro y autenticación de usuarios
- Creación y gestión de mazos de cartas
- Almacenamiento de mazos por jugador

### 2. Sistema de Batalla
- Inicio de batallas entre dos jugadores
- Lanzamiento de moneda para determinar quién comienza
- Selección del turno inicial
- Manejo de turnos y fases de juego

### 3. Juego en Tiempo Real
- Jugar cartas Pokémon al tablero
- Adjuntar energía a Pokémon
- Ejecutar ataques entre Pokémon
- Gestión de HP y estado de las cartas

### 4. Sistema de Cartas
- Carga de datos desde archivo JSON
- Manejo de tipos de energía requeridos para ataques
- Implementación de debilidades y resistencias
- Evolución de cartas


## API Endpoints

### Autenticaci�n
- POST /api/auth/login - Iniciar sesi�n
- POST /api/auth/register - Registrar nuevo usuario

### Usuarios
- GET /api/users/{username} - Obtener información del usuario
- PUT /api/users/{username} - Actualizar datos del usuario

### Mazos
- GET /api/mazos - Listar mazos del jugador
- POST /api/mazos - Crear nuevo mazo
- PUT /api/mazos/{id} - Actualizar mazo
- DELETE /api/mazos/{id} - Eliminar mazo

### Cartas
- GET /api/cards - Listar todas las cartas
- GET /api/cards/{id} - Obtener carta específica

### Batallas
- POST /api/battle/start/{username} - Iniciar batalla
- POST /api/battle/{matchId}/coin-flip - Lanzar moneda
- POST /api/battle/{matchId}/choose-turn - Elegir turno
- POST /api/battle/{matchId}/attach-energy - Adjuntar energía
- POST /api/battle/{matchId}/play-pokemon - Jugar Pokémon
- GET /api/battle/{matchId}/state - Obtener estado de la partida

## C�mo Ejecutar el Proyecto

### Requisitos Previos
1. Java 21 instalado
2. Node.js y npm instalados
3. Maven instalado

### Backend (Spring Boot)
`ash
cd backend
mvn spring-boot:run
`

### Frontend (Angular)
`ash
cd frontend
npm install
ng serve
`

### Base de Datos
El proyecto utiliza H2 Database en modo memoria. La base de datos se crea automáticamente al iniciar la aplicación.

## Implementaci�n Actual

### Funcionalidades Completas
- ? Sistema de autenticación de usuarios
- ? Gestión de mazos y cartas
- ? Inicio de batallas entre jugadores
- ? Lanzamiento de moneda para turnos
- ? Juego de cartas en tablero
- ? Manejo de energía y ataques
- ? Sistema de HP y estado de Pokémon

### Reglas Implementadas
- ? Juego de 60 cartas por mazo
- ? Turnos alternados entre jugadores
- ? Tipos de energía requeridos para ataques
- ? Debilidades y resistencias
- ? Evolución de cartas

## Funcionalidades Pendientes

### Reglas TCG Completas
1. Regla del Turno: Implementar correctamente el sistema de turnos con fases completas (Draw, Main, End)
2. Regla de energía: Añadir soporte para diferentes tipos de energía y manejo de recursos
3. Regla de Evolución: Mejorar la l�gica de evolución de cartas con condiciones específicas
4. Regla de Ataques: Implementar ataques que requieran máss de un tipo de energía
5. Regla de Debuffs/Condiciones: A�adir efectos como Paralized, Confused, etc.
6. Regla de energía Excedente: Manejo de energía sobrante al final del turno

### Funcionalidades Adicionales
1. Sistema de Nivelación: Implementar niveles y experiencia para jugadores
2. Sistema de Colección: A�adir m�canicas de colección de cartas raras
3. Sistema de Torneos: Crear competencias entre jugadores
4. Sistema de Intercambio: Permitir intercambio de cartas entre jugadores
5. Sistema de Inventario: Gestión avanzada de cartas y mazos
6. Sistema de Chat: Comunicación en tiempo real durante las batallas

### Mejoras Técnicas
1. Documentación API: Generar documentación completa con Swagger
2. Tests Unitarios: Implementar tests para todas las funcionalidades
3. Validaciones: Añadir validaciones completas de datos
4. Logs: Implementar sistema de logs completo
5. Caching: Añadir mecanismos de cache para mejor rendimiento

## Diagrama de Clases (Resumen)

`mermaid
classDiagram
    class Card {
        +String id
        +String nombre
        +String hp
        +String tipo
        +String imagen
        +String supertype
        +List~String~ subtypes
        +String evolucionDe
        +List~Ataque~ ataques
        +List~Map~ debilidades
        +List~Map~ resistencias
    }
    
    class Ataque {
        +String nombre
        +int danio
        +List~String~ tiposEnergia
    }
    
    class Jugador {
        +String id
        +String username
        +List~Mazo~ mazos
    }
    
    class Mazo {
        +String id
        +String nombre
        +List~Card~ cartas
    }
    
    class Partida {
        +String id
        +Jugador jugador1
        +Jugador jugador2
        +TableroJugador tablero1
        +TableroJugador tablero2
        +Fase fase
        +Turno turno
    }
    
    class TableroJugador {
        +List~CartaEnJuego~ pokemonEnCampo
        +List~CartaEnJuego~ cartasEnMano
        +int energiaDisponible
    }
    
    Card --> Ataque : contiene
    Jugador --> Mazo : posee
    Partida --> Jugador : participa
    Partida --> TableroJugador : tiene
`
¡Entendido! El problema es que el archivo de texto original se guardó con una codificación antigua. Te paso el README.md completamente limpio, optimizado y sin caracteres especiales problemáticos para que no tengas más esos símbolos extraños.Copiá y pegá esto directamente en tu archivo README.md:Pokémon TCG - Sistema de Juego<div align='center'><img src='https://img.icons8.com/color/96/000000/pokemon.png' alt='Pokemon Logo'/><h1>Pokémon Trading Card Game</h1><p>Sistema completo de juego para Pokémon TCG con backend en Spring Boot y frontend en Angular</p></div>Descripción GeneralEl proyecto PokemonTCG es un simulador avanzado que permite jugar al juego de cartas coleccionables Pokémon en una plataforma digital. Implementa un motor de reglas complejo, gestión de estados de batalla y una IA estratégica basada en pesos heurísticos.Estructura del ProyectoPlaintextPokemonTCG/
├── backend/                # Servidor Spring Boot (Java 21)
│   └── src/main/java/com/pokemon/tcg/
│       ├── controller/     # Endpoints REST
│       ├── model/          # Entidades y lógica de batalla
│       ├── repository/     # Persistencia JPA
│       └── service/        # Lógica de negocio e IA
├── frontend/               # Interfaz Angular 17
└── README.md               # Documentación
Tecnologías Utilizadas BackendJava 21 LTSSpring Boot 3.2.4H2 Database: Base de datos en memoria para desarrollo ágil.Jackson: Procesamiento de JSON dinámico para carga de cartas.FrontendAngular 17TypeScript & RxJSTailwind CSS: Para el diseño del tablero dinámico.Mecánicas de IA y Combate🧠 Inteligencia Artificial EstratégicaEl bot evalúa el tablero en tiempo real para tomar decisiones lógicas:Análisis de Tipos: Prioriza Pokémon con ventaja elemental (Weakness x2) y evita exponer cartas con debilidad frente al rival.Retirada Táctica: Si un Pokémon está en peligro de K.O. o estancado sin energía, la IA busca en la banca al suplente con mejor puntaje (HP/Energía) para cubrir la posición.Gestión de Mano: Evalúa qué Pokémon básico bajar a la banca basándose en las energías disponibles para cargarlo a futuro.⚔️ Motor de DañoCálculo automático de Debilidades y Resistencias.Validación de costos de energía por ataque.Sistema de reemplazo automático de Pokémon Activo tras un K.O.API Endpoints PrincipalesAcciónMétodoEndpointIniciar SesiónPOST/api/auth/loginAbrir SobrePOST/api/sobres/abrir/{username}Listar MazosGET/api/mazos/listar/{username}Iniciar BatallaPOST/api/battle/start/{username}Jugar PokémonPOST/api/battle/{matchId}/play-pokemonAtacarPOST/api/battle/{matchId}/attackCómo Ejecutar el ProyectoRequisitosJava 21+Node.js 18+MavenBackendBashcd backend
mvn clean spring-boot:run
FrontendBashcd frontend
npm install
ng serve
Implementación Actual y RoadmapFuncionalidades Completas ✅Autenticación de usuarios.Carga automática de 255 cartas reales vía JSON.IA Estratégica con lógica de pesos.Sistema de apertura de sobres y colección persistente.Tablero de batalla funcional con lógica de turnos.Próximas Mejoras 🚧Estados Alterados: Implementar Confusión, Parálisis y Veneno.Cartas Trainer: Añadir Objetos y Partidarios para mejorar el flujo del mazo.Evolución: Lógica para transformar Pokémon de Básico a Stage 1/2.Nota Educativa: Proyecto desarrollado para la cátedra de Programación III - Tecnicatura Universitaria en Programación - UTN FRC.<div align='center'><img src='https://img.icons8.com/color/48/000000/pokeball.png' alt='Pokeball'/></div>
## Objetivos del Proyecto

### Objetivo Inmediato
- Crear un sistema funcional de batalla Pokémon TCG con reglas b�sicas
- Implementar una interfaz de usuario intuitiva para jugar
- Asegurar la persistencia de datos y usuarios

### Objetivo Mediano Plazo
- Implementar todas las reglas del juego de cartas Pokémon TCG
- Mejorar el rendimiento y escalabilidad del sistema
- A�adir funcionalidades avanzadas como torneos y colecciones

### Objetivo Largo Plazo
- Convertirlo en una plataforma completa para jugadores de Pokémon TCG
- Integrar con APIs externas de cartas
- Crear un ecosistema completo de juego y socialización

## Contacto

Para m�s información o colaboraciones, puedes contactarme a trav�s de:

- Email: [tu-email@ejemplo.com]
- GitHub: [github.com/tu-usuario/PokemonTCG]

---

Este proyecto fue desarrollado con el objetivo educativo y parte del TPI de programación III para la Tecnicatura Universitaria en Programación - UTN.
Replicando el funcionamiento de un juego de cartas coleccionables en una plataforma digital.

<div align='center'>
  <img src='https://img.icons8.com/color/48/000000/pokeball.png' alt='Pokeball'/>
</div>
