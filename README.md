

<div align='center'>
  <img src='https://img.icons8.com/color/48/000000/pokeball.png' alt='Pokeball'/>
</div>
a# ⚡ Pokémon TCG - Sistema de Batalla

<div align="center">
  <img src="https://img.icons8.com/color/96/000000/pokemon.png" alt="Pokemon Logo"/>
  <h1>Pokémon Trading Card Game Simulator</h1>
  <p><b>Full-stack Battle System</b> | Spring Boot 3 + Angular 17</p>
  
  ![Java](https://img.shields.io/badge/Java-21-orange?style=flat-square&logo=openjdk)
  ![Spring](https://img.shields.io/badge/Spring_Boot-3.2.4-green?style=flat-square&logo=springboot)
  ![Angular](https://img.shields.io/badge/Angular-17-red?style=flat-square&logo=angular)
  ![H2](https://img.shields.io/badge/Database-H2_In_Memory-blue?style=flat-square)
</div>

---

## 📋 Descripción General

**PokemonTCG** es un simulador avanzado del juego de cartas coleccionables Pokémon. El sistema permite gestionar colecciones, armar mazos y enfrentarse en batallas dinámicas que implementan un motor de reglas complejo y una IA estratégica.

Desarrollado como Proyecto Integrador para la cátedra de **Programación III** de la **Tecnicatura Universitaria en Programación (UTN FRC)**.

## 🛠️ Stack Tecnológico

### Backend
- **Core:** Java 21 LTS + Spring Boot 3.2.4
- **Persistencia:** Spring Data JPA + H2 Database (In-Memory)
- **Seguridad:** Spring Security (Autenticación de usuarios)
- **JSON Engine:** Jackson para el parseo dinámico de cartas reales.

### Frontend
- **Framework:** Angular 17 (Standalone Components)
- **Estilos:** Tailwind CSS / CSS3 para el tablero de juego.
- **Estado:** RxJS para la gestión de eventos de batalla en tiempo real.

---

## 🧠 Inteligencia Artificial y Motor de Juego

El proyecto destaca por su lógica de negocio aplicada al combate:

1.  **IA Estratégica:** El bot evalúa el estado del tablero mediante pesos heurísticos:
    * **Ventaja de Tipos:** Prioriza ataques que apliquen `Weakness (x2)`.
    * **Retirada Táctica:** Si el Pokémon activo tiene poco HP, la IA busca un relevo con mejor balance de energía en la banca.
    * **Gestión de Recursos:** Optimiza el uso de cartas de energía según el costo de los ataques disponibles.
2.  **Motor de Daño:** Cálculo automático que considera:
    * Debilidades y Resistencias elementales.
    * Validación de requisitos de energía previos al ataque.
    * Gestión de estados y reemplazo automático tras un K.O.

---

## 🏗️ Estructura del Proyecto

```plaintext
PokemonTCG/
├── backend/                # API REST (Java)
│   └── src/main/java/com/pokemon/tcg/
│       ├── controller/     # Endpoints de la API
│       ├── model/          # Entidades (Card, Jugador, Partida)
│       ├── service/        # Lógica de Batalla e IA
│       └── repository/     # Interfaces JPA
├── frontend/               # Interfaz de Usuario (Angular)
└── data/                   # Archivos JSON de cartas


🧠 Core Features: IA y Motor de Juego
El diferencial técnico de este proyecto reside en su Battle Engine:

IA Heurística (BotAIService): * El bot no juega al azar; utiliza un sistema de Puntaje Estratégico.

Evalúa debilidades del rival para maximizar el daño.

Prioriza la supervivencia: si un Pokémon está en peligro de K.O., busca en la banca al suplente con mayor resistencia elemental.

Motor de Reglas:

Gestión de estados de cartas en juego (HP dinámico, energías unidas).

Validación de Costo de Retirada y una sola retirada por turno.

Sistema de premios (6 cartas) y verificación de condiciones de victoria.


🔌 Documentación de la API (Endpoints)
🛡️ Autenticación y Jugadores
POST /api/auth/login: Login/Registro automático.

GET /api/jugadores/{username}/datos: Perfil, sobres y estadísticas.

GET /api/jugadores/{username}/coleccion: Lista de cartas obtenidas.

⚔️ Sistema de Batalla
POST /api/battle/start/{username}: Inicia match contra la IA.

POST /api/battle/{id}/play-pokemon: Baja un Pokémon básico a Activo o Banca.

POST /api/battle/{id}/attach-energy: Une una carta de energía a un Pokémon.

POST /api/battle/{id}/attack?nombreAtaque=X: Ejecuta daño y calcula debilidades.

POST /api/battle/{id}/retreat: Retira al activo pagando el costo de energía.

POST /api/battle/{id}/pass-turn: Finaliza el turno actual.

🃏 Cartas y Mazos
GET /api/cards: Catálogo completo de cartas.

POST /api/sobres/abrir/{username}: Gacha system para obtener nuevas cartas.

POST /api/mazos/guardar: Persiste un mazo de 60 cartas.

🚀 Guía de Ejecución
Backend: Ejecutar BackendApplication.java o vía Maven:

Bash
mvn spring-boot:run
Frontend:

Bash
npm install && ng serve
Acceso: http://localhost:4200

🗺️ Roadmap de Desarrollo (Futuras Implementaciones)
🎯 Fase Actual: Bug Fixes & Refactor
[x] Corregido error de posicionamiento en banca.

[x] Implementado robo de carta automático al iniciar turno.

[x] Optimización de IA para reemplazo inteligente post-K.O.

🛠️ Próximos Desafíos Técnicos
[ ] Evoluciones: Implementación de lógica para Stage 1 y Stage 2 validando el nombre del Pokémon base.

[ ] WebSockets (Online Real-Time): Migración del sistema de turnos a Spring WebSocket (STOMP) para permitir batallas PvP reales entre usuarios.

[ ] Efectos Especiales: Implementar lógica para cartas con habilidades pasivas y estados alterados (Veneno, Parálisis).

[ ] Sistema de Swap: Interfaz avanzada para el cambio de Pokémon Activo sin interrumpir el flujo de la partida.

🎓 Créditos
Desarrollador: Pablo Alejandro Biasco

Institución: UTN - Facultad Regional Córdoba

Materia: Programación III

<div align="center">
<img src="https://img.icons8.com/color/48/000000/pokeball.png" alt="Pokeball"/>
</div>