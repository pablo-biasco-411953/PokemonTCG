<div align='center'>
  <img src='https://img.icons8.com/color/48/000000/pokeball.png' alt='Pokeball'/>
</div>

# ⚡ Pokémon TCG - Sistema de Batalla

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

---

## 🛠️ Stack Tecnológico

### Backend
- **Core:** Java 21 LTS (Uso de `Records` y features modernas) + Spring Boot 3.2.4
- **Persistencia:** Spring Data JPA + H2 Database (In-Memory)
- **Seguridad:** Spring Security (Autenticación de usuarios)
- **JSON Engine:** Jackson para el parseo dinámico de atributos de cartas reales.

### Frontend
- **Framework:** Angular 17 (Standalone Components)
- **Estilos:** Tailwind CSS / CSS3 para el tablero de juego inmersivo.
- **Estado:** RxJS para la gestión de eventos de batalla en tiempo real.

---

## 🧠 Core Features: Motor de Juego e IA

El diferencial técnico de este proyecto reside en su robusto **Battle Engine**, diseñado para replicar fielmente las mecánicas oficiales del TCG:

### ⚙️ Motor de Reglas (BattleEngineService)
- **Condiciones Especiales (Estados):** Soporte completo para Veneno ☠️, Quemadura 🔥, Sueño 💤, Parálisis ⚡ y Confusión 🌀.
- **Fase de Mantenimiento (Pokémon Checkup):** Resolución automática de daño por estados y lanzamiento de monedas de curación entre turnos.
- **RNG & Lanzamiento de Monedas:** Parseo dinámico de textos de ataques para calcular probabilidades de éxito, multiplicadores de daño y efectos colaterales (ej: destrucción de energía rival o bloqueo de retirada).
- **Cura en la Banca:** Sistema de retirada que limpia automáticamente los estados alterados al volver a la banca.

### 🤖 Inteligencia Artificial Heurística (BotAIService)
El bot no juega al azar; utiliza un sistema de **Puntaje Estratégico** en constante evolución:
- **Detección de Peligro por Estados:** El bot analiza si morirá por Veneno/Quemadura entre turnos y fuerza una retirada estratégica para curarse.
- **Respeto de Bloqueos:** Reconoce si está Dormido o Paralizado y omite ataques inválidos.
- **Ventaja Elemental:** Prioriza bajar a la banca y subir al activo a Pokémon que exploten la `Weakness (x2)` del rival o posean `Resistance (-20)`.
- **Gestión de Recursos:** Optimiza la unión de cartas de energía evaluando el costo específico (y el costo incoloro) de sus ataques disponibles.

---

## 🏗️ Estructura del Proyecto

```plaintext
PokemonTCG/
├── backend/                # API REST (Java)
│   └── src/main/java/com/pokemon/tcg/
│       ├── controller/     # Endpoints de la API
│       ├── model/          # Entidades (Card, Jugador, Partida, ResultadoAtaque)
│       ├── service/        # Lógica de Batalla, Procesamiento de Textos e IA
│       └── repository/     # Interfaces JPA
├── frontend/               # Interfaz de Usuario (Angular)
└── data/                   # Archivos JSON estructurados de cartas
🔌 Documentación de la API (Endpoints Principales)
🛡️ Autenticación y Jugadores
POST /api/auth/login: Login/Registro automático.

GET /api/jugadores/{username}/datos: Perfil, sobres y estadísticas.

GET /api/jugadores/{username}/coleccion: Lista de cartas obtenidas.

⚔️ Sistema de Batalla
POST /api/battle/start/{username}: Inicia un match contra la IA.

POST /api/battle/{id}/play-pokemon: Baja un Pokémon básico a la posición Activa o a la Banca.

POST /api/battle/{id}/attach-energy: Une una carta de energía a un Pokémon específico.

POST /api/battle/{id}/attack?nombreAtaque=X: Ejecuta un ataque, calculando RNG, debilidades y estados.

POST /api/battle/{id}/retreat: Retira al activo pagando el costo de energía (y validando bloqueos).

POST /api/battle/{id}/pass-turn: Finaliza el turno actual y dispara la fase de Mantenimiento.

🃏 Cartas y Mazos
GET /api/cards: Catálogo completo de cartas.

POST /api/sobres/abrir/{username}: Sistema Gacha para obtener nuevas cartas.

POST /api/mazos/guardar: Persiste un mazo customizado de 60 cartas.

🗺️ Roadmap de Desarrollo
🎯 Fase Actual: Lógica de Combate y Estados
[x] Corregido error de posicionamiento en banca.

[x] Implementado robo de carta automático al iniciar turno.

[x] Sistema de Swap / Retirada táctica validando costos.

[x] Efectos Especiales: Implementación de lógica para habilidades pasivas, lanzamiento de monedas y estados alterados (Veneno, Parálisis, etc.).

[x] Optimización de IA para reemplazo inteligente post-K.O. y supervivencia a estados.

🛠️ Próximos Desafíos Técnicos
[ ] Evoluciones: Implementación de lógica para Stage 1 y Stage 2 validando el nombre del Pokémon base (evolvesFrom).

[ ] WebSockets (Online Real-Time): Migración del sistema de turnos a Spring WebSocket (STOMP) para permitir batallas PvP reales entre usuarios.

[ ] Animaciones en Frontend: Representación visual (UI) de los lanzamientos de monedas y los iconos de estado sobre las cartas.

🎓 Créditos
Desarrollador: Pablo Alejandro Biasco

Institución: UTN - Facultad Regional Córdoba

Materia: Programación III

<div align="center">
<img src="https://img.icons8.com/color/48/000000/pokeball.png" alt="Pokeball"/>
</div>