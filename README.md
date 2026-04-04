

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