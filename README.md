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

### Backend en macOS / Linux

En macOS y Linux no se usa `mvnw.cmd`. El comando correcto es:

```bash
./mvnw -f backend/pom.xml spring-boot:run
```

Si `./mvnw` no tiene permisos de ejecucion:

```bash
chmod +x mvnw
./mvnw -f backend/pom.xml spring-boot:run
```

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

## Errores comunes al levantar el proyecto

### Error: `Port 8080 was already in use`

Si al levantar el backend aparece un error como:

```text
Web server failed to start. Port 8080 was already in use.
```

significa que ya hay otro proceso usando el puerto `8080`.

#### Solucion en macOS / Linux

Ver que proceso esta usando el puerto:

```bash
lsof -i :8080
```

Cerrar ese proceso usando su PID:

```bash
kill -9 <PID>
```

Tambien se puede hacer en un solo comando:

```bash
kill -9 $(lsof -ti :8080)
```

Luego volver a levantar el backend:

```bash
./mvnw -f backend/pom.xml spring-boot:run
```

#### Alternativa: usar otro puerto

Si no queres cerrar el proceso actual, podes arrancar el backend en otro puerto:

```bash
./mvnw -f backend/pom.xml spring-boot:run -Dspring-boot.run.arguments=--server.port=8081
```

Importante:
Si el frontend o alguna configuracion apunta a `http://localhost:8080`, tambien habra que ajustar esa URL para usar `8081`.

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
