# Pokemon TCG - Sistema de Juego

# Pokemon TCG - Sistema de Juego

<div align='center'>
  <img src='https://img.icons8.com/color/96/000000/pokemon.png' alt='Pokemon Logo'/>
  <h1>Pok�mon Trading Card Game</h1>
  <p>Sistema completo de juego para Pok�mon TCG con backend en Spring Boot y frontend en Angular</p>
</div>

## Descripci�n General

El proyecto PokemonTCG es un sistema completo que permite jugar al juego de cartas coleccionables Pok�mon TCG (Trading Card Game) en una plataforma digital. El juego implementa las reglas b�sicas del TCG, incluyendo batallas entre jugadores, manejo de mazos, cartas, energ�a y ataques.

## Estructura del Proyecto

PokemonTCG/
+-- backend/                 # Servidor Spring Boot
�   +-- src/main/java/com/pokemon/tcg/
�   �   +-- controller/      # Controladores REST API
�   �   +-- model/           # Modelos de datos
�   �   �   +-- battle/      # Modelos de batalla
�   �   +-- repository/      # Repositorios de datos
�   �   +-- service/         # L�gica de negocio
�   +-- src/main/resources/
�       +-- cards.json       # Datos de cartas
+-- frontend/                # Frontend Angular
+-- README.md                # Documentaci�n

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
Representa una carta de Pok�mon TCG con las siguientes propiedades:
- id: Identificador �nico de la carta
- nombre: Nombre de la carta
- hp: Puntos de salud
- tipo: Tipo de Pok�mon (Fire, Water, Grass, etc.)
- imagen: URL de la imagen de la carta
- supertype: Tipo de carta (Pok�mon, Trainer, Energy)
- subtypes: Subtipos como Basic, Stage 1, Stage 2
- evolucionDe: Nombre de la carta de la que evoluciona
- ataques: Lista de ataques disponibles
- debilidades: Debilidades del Pok�mon
- resistencias: Resistencias del Pok�mon

#### Jugador (Jugador)
- id: Identificador �nico
- username: Nombre de usuario
- mazos: Lista de mazos del jugador

#### Mazo (Mazo)
- id: Identificador �nico
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
- Registro y autenticaci�n de usuarios
- Creaci�n y gesti�n de mazos de cartas
- Almacenamiento de mazos por jugador

### 2. Sistema de Batalla
- Inicio de batallas entre dos jugadores
- Lanzamiento de moneda para determinar qui�n comienza
- Selecci�n del turno inicial
- Manejo de turnos y fases de juego

### 3. Juego en Tiempo Real
- Jugar cartas Pok�mon al tablero
- Adjuntar energ�a a Pok�mon
- Ejecutar ataques entre Pok�mon
- Gesti�n de HP y estado de las cartas

### 4. Sistema de Cartas
- Carga de datos desde archivo JSON
- Manejo de tipos de energ�a requeridos para ataques
- Implementaci�n de debilidades y resistencias
- Evoluci�n de cartas


## API Endpoints

### Autenticaci�n
- POST /api/auth/login - Iniciar sesi�n
- POST /api/auth/register - Registrar nuevo usuario

### Usuarios
- GET /api/users/{username} - Obtener informaci�n del usuario
- PUT /api/users/{username} - Actualizar datos del usuario

### Mazos
- GET /api/mazos - Listar mazos del jugador
- POST /api/mazos - Crear nuevo mazo
- PUT /api/mazos/{id} - Actualizar mazo
- DELETE /api/mazos/{id} - Eliminar mazo

### Cartas
- GET /api/cards - Listar todas las cartas
- GET /api/cards/{id} - Obtener carta espec�fica

### Batallas
- POST /api/battle/start/{username} - Iniciar batalla
- POST /api/battle/{matchId}/coin-flip - Lanzar moneda
- POST /api/battle/{matchId}/choose-turn - Elegir turno
- POST /api/battle/{matchId}/attach-energy - Adjuntar energ�a
- POST /api/battle/{matchId}/play-pokemon - Jugar Pok�mon
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
El proyecto utiliza H2 Database en modo memoria. La base de datos se crea autom�ticamente al iniciar la aplicaci�n.

## Implementaci�n Actual

### Funcionalidades Completas
- ? Sistema de autenticaci�n de usuarios
- ? Gesti�n de mazos y cartas
- ? Inicio de batallas entre jugadores
- ? Lanzamiento de moneda para turnos
- ? Juego de cartas en tablero
- ? Manejo de energ�a y ataques
- ? Sistema de HP y estado de Pok�mon

### Reglas Implementadas
- ? Juego de 60 cartas por mazo
- ? Turnos alternados entre jugadores
- ? Tipos de energ�a requeridos para ataques
- ? Debilidades y resistencias
- ? Evoluci�n de cartas

## Funcionalidades Pendientes

### Reglas TCG Completas
1. Regla del Turno: Implementar correctamente el sistema de turnos con fases completas (Draw, Main, End)
2. Regla de Energ�a: A�adir soporte para diferentes tipos de energ�a y manejo de recursos
3. Regla de Evoluci�n: Mejorar la l�gica de evoluci�n de cartas con condiciones espec�ficas
4. Regla de Ataques: Implementar ataques que requieran m�s de un tipo de energ�a
5. Regla de Debuffs/Condiciones: A�adir efectos como Paralized, Confused, etc.
6. Regla de Energ�a Excedente: Manejo de energ�a sobrante al final del turno

### Funcionalidades Adicionales
1. Sistema de Nivelaci�n: Implementar niveles y experiencia para jugadores
2. Sistema de Colecci�n: A�adir m�canicas de colecci�n de cartas raras
3. Sistema de Torneos: Crear competencias entre jugadores
4. Sistema de Intercambio: Permitir intercambio de cartas entre jugadores
5. Sistema de Inventario: Gesti�n avanzada de cartas y mazos
6. Sistema de Chat: Comunicaci�n en tiempo real durante las batallas

### Mejoras T�cnicas
1. Documentaci�n API: Generar documentaci�n completa con Swagger
2. Tests Unitarios: Implementar tests para todas las funcionalidades
3. Validaciones: A�adir validaciones completas de datos
4. Logs: Implementar sistema de logs completo
5. Caching: A�adir mecanismos de cache para mejor rendimiento

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

## Objetivos del Proyecto

### Objetivo Inmediato
- Crear un sistema funcional de batalla Pok�mon TCG con reglas b�sicas
- Implementar una interfaz de usuario intuitiva para jugar
- Asegurar la persistencia de datos y usuarios

### Objetivo Mediano Plazo
- Implementar todas las reglas del juego de cartas Pok�mon TCG
- Mejorar el rendimiento y escalabilidad del sistema
- A�adir funcionalidades avanzadas como torneos y colecciones

### Objetivo Largo Plazo
- Convertirlo en una plataforma completa para jugadores de Pok�mon TCG
- Integrar con APIs externas de cartas
- Crear un ecosistema completo de juego y socializaci�n

## Contacto

Para m�s informaci�n o colaboraciones, puedes contactarme a trav�s de:

- Email: [tu-email@ejemplo.com]
- GitHub: [github.com/tu-usuario/PokemonTCG]

---

Este proyecto fue desarrollado con el objetivo educativo y parte del TPI de programación III para la Tecnicatura Universitaria en Programación - UTN.
Replicando el funcionamiento de un juego de cartas coleccionables en una plataforma digital.

<div align='center'>
  <img src='https://img.icons8.com/color/48/000000/pokeball.png' alt='Pokeball'/>
</div>
