# PokemonTCG

Proyecto full-stack de Pokemon Trading Card Game con backend en Spring Boot y frontend en Angular.

## Estructura

```text
PokemonTCG/
  backend/      API Spring Boot + H2
  frontend/     Cliente Angular
  .mvn/         Maven wrapper compartido
  mvnw
  mvnw.cmd
```

## Stack actual

- Java 21
- Spring Boot 3.2.4
- H2 en memoria
- Angular 21
- RxJS
- Three.js

## Arranque local

### Backend

Desde la raiz del proyecto:

```powershell
.\mvnw.cmd -f backend\pom.xml spring-boot:run
```

El backend arranca en `http://localhost:8080`.

### Frontend

Desde `frontend/`:

```powershell
npm start
```

El frontend arranca en `http://localhost:4200`.

## Build y validacion

### Backend

```powershell
.\mvnw.cmd -f backend\pom.xml test
```

### Frontend

```powershell
cd frontend
npm.cmd run build
```

Nota:
El build de frontend compila, pero hoy falla por budgets de produccion configurados en Angular. No es un problema del refactor estructural inicial.

## Flujos base que estamos protegiendo durante el refactor

- Login
- Lobby
- Apertura de sobres
- Constructor de mazos
- Inicio de batalla
- Jugar carta, unir energia, atacar, pasar turno y retirada

## Modo de pruebas

Dentro del tablero de batalla existe un panel de depuracion o "modo dios" para testear acciones manuales.

- Se abre y se cierra con la tecla `F3`
- Solo esta disponible dentro de una partida, en la pantalla de batalla
- Permite inyectar cartas, forzar estados y ajustar HP para pruebas rapidas

## Estado del refactor

La hoja de ruta activa esta en [REFACTOR_README.md](./REFACTOR_README.md).
