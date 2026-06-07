# Documentacion del codigo del backend

Este archivo resume como esta organizado el backend del proyecto `PokemonTCG`, cual es el recorrido de una peticion desde el frontend hasta la logica de negocio, que papel cumple cada capa y como fluye la informacion entre controllers, services, DTOs, repositorios y modelos.

La documentacion esta centrada solo en el backend.

---

## 1. Punto de entrada de la aplicacion

La aplicacion arranca en [`BackendApplication`](./src/main/java/com/pokemon/tcg/BackendApplication.java), que esta anotada con `@SpringBootApplication`.

Ese archivo cumple una unica funcion:

1. Iniciar el contexto de Spring Boot.
2. Escanear automaticamente componentes, services, controllers, repositories y configuraciones.
3. Levantar el servidor embebido en el puerto definido en `application.properties`.

El backend corre en `http://localhost:8080`.

---

## 2. Configuracion general

### `application.properties`

El archivo [`application.properties`](./src/main/resources/application.properties) define:

- `server.port=8080`
- Base de datos MySQL 8.0
- Configuración de conexión (URL, driver, usuario y contraseña de la base de datos)
- Hibernate con `ddl-auto=update`
- `spring.jpa.show-sql=true` para ver consultas
- Configuración de SMTP/Mail para la recuperación de contraseñas por correo electrónico (Gmail)

Esto significa que:

1. El proyecto requiere un servidor MySQL corriendo (típicamente levantado mediante Docker Compose).
2. Las tablas se actualizan automáticamente según las entidades JPA.
3. Se incluye soporte para envío de tokens de reinicio de contraseña por correo.

### Docker Compose

El archivo [`docker-compose.yml`](../docker-compose.yml) en la raíz del proyecto define un servicio de base de datos MySQL 8.0 (`pokefetch-db`):
- Imagen: `mysql:8.0`
- Puerto expuesto: `3306:3306`
- Base de datos: `pokemon_tcg`
- Contraseña de root: `1234`
- Persistencia mediante volumen de Docker `db_data`

### `CorsConfig`

[`CorsConfig`](./src/main/java/com/pokemon/tcg/config/CorsConfig.java) habilita CORS para el frontend Angular en `http://localhost:4200`.

Esto permite que el frontend consuma la API sin problemas de origen cruzado.

### `DataLoader`

[`DataLoader`](./src/main/java/com/pokemon/tcg/config/DataLoader.java) implementa `CommandLineRunner`, por lo que se ejecuta automaticamente al iniciar la app.

Su trabajo es:

1. Verificar si no hay cartas cargadas.
2. Leer `cards.json`.
3. Mapearlo a objetos `Card`.
4. Guardarlas en la base.
5. Crear un usuario de prueba llamado `Pablo`.
6. Generarle una coleccion inicial y un mazo de ejemplo.

Este componente hace que el backend pueda usarse apenas arranca, sin necesidad de cargar datos manualmente.

---

## 3. Arquitectura por capas

El backend sigue una estructura bastante clasica:

- `controller`: recibe requests HTTP y devuelve responses.
- `service`: contiene la logica de negocio.
- `repository`: acceso a datos con Spring Data JPA.
- `model`: entidades persistentes y modelos de juego.
- `dto`: objetos de transferencia entre frontend y backend.
- `config`: configuraciones y carga inicial.

La idea general es esta:

1. El frontend llama a un endpoint.
2. El controller valida lo minimo y traduce la request.
3. El service aplica las reglas.
4. El repository consulta o persiste datos.
5. El controller devuelve la respuesta.

---

## 4. Modelos de dominio

### `Card`

[`Card`](./src/main/java/com/pokemon/tcg/model/Card.java) representa una carta Pokemon o una energia.

Es una entidad JPA mapeada a la tabla `cards` y contiene:

- `id`
- `nombre`
- `hp`
- `tipo`
- `imagen`
- `costoRetirada`
- `supertype`
- `evolvesFrom`
- datos serializados de `subtypes`, `rules`, `attacks`, `weakness`, `resistance`

Tambien tiene listas transitorias para usar en memoria:

- `subtypes`
- `reglas`
- `ataques`
- `debilidades`
- `resistencias`

#### Detalle importante

La clase hace dos trabajos al mismo tiempo:

1. **Persistencia**: guarda version serializada en columnas de texto.
2. **Uso en memoria**: reconstruye listas al cargar desde base con `@PostLoad`.

Eso permite que la carta llegue limpia al resto del sistema y que la logica de batalla trabaje con listas reales de ataques y debilidades.

### `Jugador`

[`Jugador`](./src/main/java/com/pokemon/tcg/model/Jugador.java) representa al usuario del sistema.

Campos principales:

- `id`
- `username`
- `sobresDisponibles`
- `coleccion`

La coleccion es una relacion `@ManyToMany` con `Card`.

En este proyecto, el jugador es el dueño de:

- sus cartas,
- sus sobres,
- sus mazos.

### `Mazo`

[`Mazo`](./src/main/java/com/pokemon/tcg/model/Mazo.java) representa un mazo armado por un jugador.

Campos:

- `id`
- `nombre`
- `jugador`
- `cartas`

Es una entidad con relacion:

- `@ManyToOne` hacia `Jugador`
- `@ManyToMany` hacia `Card`

La regla central de negocio es que un mazo debe tener **exactamente 60 cartas** para guardarse.

### Modelos de batalla

Los modelos bajo `model/battle` representan la partida en memoria.

#### `Partida`

[`Partida`](./src/main/java/com/pokemon/tcg/model/battle/Partida.java) es el contenedor principal de una batalla.

Guarda:

- `id` unico tipo UUID
- `jugador` -> `TableroJugador`
- `bot` -> `TableroJugador`
- `turnoActual`
- `faseActual`
- `yaSeRetiroEsteTurno`
- `ultimasMonedasLanzadas`

La partida no se persiste en base: vive en memoria dentro de `BattleEngineService`.

#### `TableroJugador`

[`TableroJugador`](./src/main/java/com/pokemon/tcg/model/battle/TableroJugador.java) agrupa todas las zonas visibles o internas de un lado de la partida:

- `mazo`
- `mano`
- `premios`
- `activo`
- `banca`
- `pilaDescarte`

Es una estructura de juego, no una entidad JPA.

#### `CartaEnJuego`

[`CartaEnJuego`](./src/main/java/com/pokemon/tcg/model/battle/CartaEnJuego.java) representa una carta ya puesta sobre la mesa.

Contiene:

- `card` original
- `hpActual`
- `energiasUnidas`
- `condicionesEspeciales`
- `puedeAtacar`
- `invulnerable`

Se usa para mantener el estado actual de una carta durante la batalla, separado del estado original de `Card`.

#### `Ataque`

[`Ataque`](./src/main/java/com/pokemon/tcg/model/battle/Ataque.java) representa el ataque de una carta.

Campos:

- `nombre`
- `danio`
- `tiposEnergia`
- `texto`

El campo `texto` es clave porque el motor interpreta el texto del ataque para resolver efectos especiales, monedas, estados, curaciones, robo de cartas, daño a banca, etc.

#### `ResultadoAtaque`

[`ResultadoAtaque`](./src/main/java/com/pokemon/tcg/model/battle/ResultadoAtaque.java) es un record simple que devuelve:

- `danioFinal`
- `carasSacadas`

Sirve para separar el calculo del ataque de la ejecucion de efectos secundarios.

---

## 5. DTOs

Los DTOs viven en [`src/main/java/com/pokemon/tcg/dto`](./src/main/java/com/pokemon/tcg/dto) y representan los datos que el frontend manda o recibe.

### Requests

- `LoginRequest`: contiene `username`.
- `GuardarMazoRequest`: contiene `nombre`, `username`, `cartas`.
- `ActualizarMazoRequest`: contiene `nombre`, `cartas`.
- `StartBattleRequest`: contiene `mazoId`.
- `ChooseTurnRequest`: contiene `vaPrimero`.
- `JugarPokemonRequest`: contiene `cartaId` y `posicion`.
- `UnirEnergiaRequest`: contiene `cartaId` y `energiaId`.
- `EvolveRequest`: contiene `cartaManoId` y `cartaTableroId`.

### Responses

- `JugadorDatosResponse`: devuelve `username`, `sobresDisponibles` y `cantidadCartas`.
- `JugadorDTO`: tambien resume datos del jugador, aunque en la practica el controller usa mas `JugadorDatosResponse`.

El objetivo de estos DTOs es evitar que el frontend tenga que mandar o recibir entidades completas cuando solo necesita una parte de los datos.

---

## 6. Repositorios

Los repositorios son la capa de acceso a datos.

### `CardRepository`

[`CardRepository`](./src/main/java/com/pokemon/tcg/repository/CardRepository.java) extiende `JpaRepository<Card, String>`.

Agrega un metodo:

- `findTenRandomCards()`

Que devuelve 10 cartas aleatorias con SQL nativo.

### `JugadorRepository`

[`JugadorRepository`](./src/main/java/com/pokemon/tcg/repository/JugadorRepository.java) extiende `JpaRepository<Jugador, Long>`.

Agrega:

- `findByUsername(String username)`

Usa `LEFT JOIN FETCH` para traer la coleccion junto al jugador.

### `MazoRepository`

[`MazoRepository`](./src/main/java/com/pokemon/tcg/repository/MazoRepository.java) extiende `JpaRepository<Mazo, Long>`.

Agrega:

- `findByJugador(Jugador jugador)`

Sirve para listar los mazos de un usuario especifico.

---

## 7. Controllers y flujo HTTP

### `AuthController`

Ruta base: `/api/auth`

Endpoints:

- `POST /login`: Recibe `LoginRequest` (con `username` y `password`). Verifica las credenciales a través de `AuthService.login` y devuelve el jugador autenticado.
- `POST /register`: Recibe `RegisterRequest` (con `screenName`, `email`, `password`, y `confirmPassword`). Registra un nuevo usuario validando que las contraseñas coincidan y que los datos sean correctos.
- `POST /forgot-password`: Recibe `ForgotPasswordRequest` (con `username` y `email`). Genera un token de reinicio enviado por correo (si el envío de mails está configurado) para la recuperación de contraseña.
- `POST /reset-password`: Recibe `ResetPasswordRequest` (con `token`, `password`, y `confirmPassword`). Consume el token temporal y actualiza la contraseña del usuario.

### `JugadorController`

Ruta base: `/api/jugadores`

Endpoints:

- `GET /{username}/datos`
- `GET /{username}/coleccion`

`/datos` devuelve un resumen del jugador:

- username
- sobres disponibles
- cantidad de cartas

`/coleccion` devuelve todas las cartas de la coleccion.

### `CardController`

Ruta base: `/api/cards`

Endpoint:

- `GET /`

Devuelve todas las cartas disponibles en la base.

### `MazoController`

Ruta base: `/api/mazos`

Endpoints:

- `POST /guardar`
- `GET /listar/{username}`
- `PUT /actualizar/{id}`

Flujo general:

1. El controller recibe la request.
2. Llama a `MazoService`.
3. El service valida jugador, cartas y cantidad.
4. El repository guarda o actualiza el mazo.

### `SobreController`

Ruta base: `/api/sobres`

Endpoint:

- `POST /abrir/{username}`

Entrega cartas aleatorias al jugador, descuenta un sobre y agrega las cartas a la coleccion.

### `BattleController`

Ruta base: `/api/battle`

Este controller es el centro del juego. Expone acciones de batalla que consume el frontend.

Endpoints principales:

- `POST /start/{username}`
- `GET /state/{matchId}`
- `POST /{matchId}/coin-flip`
- `POST /{matchId}/choose-turn`
- `POST /{matchId}/play-pokemon`
- `POST /{matchId}/attach-energy`
- `POST /{matchId}/attack`
- `POST /{matchId}/retreat`
- `POST /{matchId}/pass-turn`
- `POST /{matchId}/evolve`
- `POST /{matchId}/jugar-bot`
- `POST /{matchId}/promote`

Tambien incluye endpoints de debug:

- `POST /{matchId}/debug/draw`
- `POST /{matchId}/debug/status`
- `POST /{matchId}/debug/hp`
- `GET /debug/catalog`

#### Flujo general de batalla

1. Se llama a `startBattle`.
2. Se crea una `Partida` y se guarda en memoria.
3. El frontend consulta el estado con `state/{matchId}`.
4. Cada accion del jugador modifica la partida en memoria.
5. `pass-turn` o `jugar-bot` avanzan el flujo de turnos.

---

## 8. Services y logica de negocio

### `AuthService`

[`AuthService`](./src/main/java/com/pokemon/tcg/service/AuthService.java) gestiona el flujo de autenticación y registro de usuarios.

Ofrece métodos para:
- `login(username, password)`: Busca al usuario en la base de datos y compara la contraseña.
- `register(screenName, email, password, confirmPassword)`: Valida la unicidad del nombre de pantalla (`screenName`) y correo (`email`), comprueba la fortaleza de la contraseña, que ambas coincidan, y crea un nuevo `Jugador` en la base de datos con una colección inicial y sobres disponibles.

### `PasswordRecoveryService`

[`PasswordRecoveryService`](./src/main/java/com/pokemon/tcg/service/PasswordRecoveryService.java) gestiona la generación, envío y consumo de tokens de recuperación de contraseñas de manera segura a través de correos electrónicos.

### `SobreService`

[`SobreService`](./src/main/java/com/pokemon/tcg/service/SobreService.java) genera sobres aleatorios.

Reglas:

1. El jugador debe existir.
2. Debe tener sobres disponibles.
3. Se obtienen todas las cartas de la base.
4. Se separan energias y pokemon por `supertype`.
5. Se genera un sobre de 10 cartas:
   - entre 2 y 5 energias,
   - el resto pokemon.
6. Se agrega el contenido a la coleccion.
7. Se descuenta 1 sobre.

### `MazoService`

[`MazoService`](./src/main/java/com/pokemon/tcg/service/MazoService.java) administra mazos.

#### `guardarMazo`

1. Busca el jugador.
2. Valida que existan exactamente 60 IDs de cartas.
3. Busca cada carta en la base.
4. Construye el mazo.
5. Persiste.

#### `actualizarMazo`

1. Busca el mazo por ID.
2. Actualiza el nombre.
3. Reemplaza sus cartas por las nuevas.
4. Guarda cambios.

#### `listarMazos`

Devuelve todos los mazos asociados al jugador.

### `BattleEngineService`

[`BattleEngineService`](./src/main/java/com/pokemon/tcg/service/BattleEngineService.java) es el orquestador principal de la batalla.

Es la pieza mas importante de la logica del juego porque coordina:

- `BotAIService`
- `BattleTurnService`
- `BattleAttackService`
- `BattleKoService`

#### Responsabilidades

1. Crear la partida.
2. Preparar el mazo de jugador y bot.
3. Repartir mano inicial y premios.
4. Manejar turnos.
5. Validar acciones.
6. Delegar el calculo de ataques.
7. Resolver KOs.
8. Exponer helpers de debug.

#### `startBattle`

Flujo:

1. Busca el jugador.
2. Busca el mazo elegido.
3. Verifica que tenga 60 cartas.
4. Duplica y mezcla el mazo para jugador y bot.
5. Reparte 7 cartas a la mano.
6. Reparte 6 premios.
7. Crea la `Partida`.
8. La deja en fase `LANZAMIENTO_MONEDA`.
9. La guarda en `partidasEnCurso`.

#### `lanzarMoneda`

Resuelve el sorteo inicial y cambia la fase a `TURNO_NORMAL`.

#### `elegirTurno`

Define quien empieza.

- Si va primero el jugador, se le asigna el turno.
- Si va primero el bot, se ejecuta inmediatamente su turno.

#### `jugarPokemon`

Valida turno, busca la carta en mano y permite bajarla:

- como activo si no existe activo,
- o a la banca si hay espacio.

Solo permite Pokemon basicos.

#### `unirEnergia`

Une una energia desde la mano a un Pokemon del tablero.

#### `realizarRetirada`

Aplica el costo de retirada, mueve energias al descarte y hace el swap entre activo y banca.

Tambien bloquea retiradas multiples en el mismo turno.

#### `subirAActivoDesdeBanca`

Se usa cuando el activo quedo vacio y hay que promover uno de banca.

#### `realizarAtaque`

1. Verifica turno.
2. Verifica activo del jugador y del bot.
3. Revisa condiciones que impiden atacar.
4. Busca el ataque por nombre.
5. Llama a `BattleAttackService.resolveAttack`.
6. Guarda historial de monedas.
7. Termina el turno llamando a `pasarTurno`.

#### `pasarTurno`

1. Limpia estados temporales del activo del jugador.
2. Quita invulnerabilidad al bot.
3. Aplica mantenimiento entre turnos.
4. Resetea la flag de retirada.
5. Cambia turno al bot.

#### `ejecutarTurnoBot`

1. Roba una carta para el bot.
2. Llama a `BotAIService.ejecutarTurno`.
3. Limpia estados temporales del activo del bot.
4. Quita invulnerabilidad al jugador.
5. Aplica mantenimiento entre turnos.
6. Devuelve el turno al jugador.
7. Roba una carta para el jugador.

#### `evolucionarPokemon`

Permite evolucionar una carta de la mano sobre un Pokemon en mesa.

Reglas:

1. La carta de evolucion debe estar en la mano.
2. El objetivo debe estar en tablero.
3. `evolvesFrom` debe coincidir con el nombre del Pokemon objetivo.
4. Se conserva el dano acumulado.
5. Se limpian condiciones especiales.

#### Metodos de debug

Los metodos `debugRobarCarta`, `debugForzarEstado`, `debugSetHp` y `obtenerCatalogoCartasDebug` sirven para pruebas manuales desde el frontend.

No forman parte de la logica competitiva normal, pero son utiles para depurar partidas.

### `BattleTurnService`

[`BattleTurnService`](./src/main/java/com/pokemon/tcg/service/BattleTurnService.java) se encarga del mantenimiento entre turnos.

#### Limpieza de fin de turno

- `limpiarActivoFinTurnoJugador`
- `limpiarActivoFinTurnoBot`

Ambos quitan bloqueos temporales como:

- `CantRetreat`
- `Paralyzed`

y reactivan `puedeAtacar`.

#### Mantenimiento entre turnos

`aplicarMantenimientoEntreTurnos` procesa estados de ambos lados:

- veneno: 10 de dano
- quemadura: 20 de dano y posible curacion
- dormido: posible despertar por moneda

Si un Pokemon cae a 0 HP por estado, delega al resolver de KO.

### `BattleAttackService`

[`BattleAttackService`](./src/main/java/com/pokemon/tcg/service/BattleAttackService.java) resuelve ataques de forma detallada.

Separa la resolucion en dos etapas:

1. Calculo del dano base y modificaciones.
2. Aplicacion de efectos secundarios.

#### `resolveAttack`

Recibe:

- `Partida`
- `Ataque`
- atacante
- defensor
- `KoResolver`

Devuelve:

- `ResultadoAtaque`
- historial de monedas

#### Que interpreta este service

El texto del ataque puede disparar muchas cosas:

- curacion
- robo de cartas
- danio a banca
- dano a si mismo
- descarte de energias
- parálisis
- veneno
- quemadura
- confusion
- bloqueo de retirada
- escudos de invulnerabilidad
- ataques con monedas
- ataques que escalan por energias unidas

Esto hace que gran parte de la logica del combate se base en reglas textuales del propio ataque.

### `BattleKoService`

[`BattleKoService`](./src/main/java/com/pokemon/tcg/service/BattleKoService.java) centraliza lo que pasa cuando un Pokemon queda fuera de combate.

#### `resolverKO`

Hace lo siguiente:

1. Identifica que tablero perdio la carta.
2. Saca al Pokemon del activo si corresponde.
3. Elimina la carta de la banca.
4. La manda al descarte.
5. El ganador roba un premio.
6. Si ya no quedan premios o Pokemon, termina la partida.
7. Si el bot perdio el activo y aun tiene banca, elige un reemplazo estrategico.

El reemplazo del bot se decide por puntaje:

- energias unidas,
- HP,
- debilidades y resistencias contra el rival.

### `BotAIService`

[`BotAIService`](./src/main/java/com/pokemon/tcg/service/BotAIService.java) maneja la inteligencia del bot.

Su objetivo no es ser perfecto, sino tomar decisiones coherentes:

1. Asegurar un activo.
2. Bajar Pokemon basicos desde la mano.
3. Evaluar retiradas tacticas.
4. Unir energias utiles.
5. Atacar si puede.

#### Orden del turno del bot

1. Si no hay activo, subir el mejor de la banca.
2. Jugar basicos desde la mano.
3. Evaluar si conviene retirarse.
4. Unir una energia util.
5. Intentar atacar.

#### Piezas importantes

- `esPokemonBasico`: filtra cartas que no deben bajarse como basicas.
- `esEnergia`: detecta energias por `supertype`.
- `puedePagarCosto`: valida si un ataque puede ejecutarse con las energias unidas.
- `calcularDanioFinal`: aplica debilidad y resistencia.
- `normalizarTipo`: traduce nombres de tipos en ingles y español.

El bot usa heuristicas simples, pero suficientes para que la partida tenga comportamiento dinamico.

---

## 9. Flujo completo de informacion

### Registro y Autenticación (Login)

#### Flujo de Registro:
1. El frontend envía `screenName`, `email`, `password`, y `confirmPassword`.
2. `AuthController` recibe la petición en `/register` y delega en `AuthService.register(...)`.
3. Se verifica si el usuario o email ya existen.
4. Se comprueba que coincidan las contraseñas.
5. Se crea el `Jugador`, asignándole cartas iniciales y sobres disponibles, y se persiste en la base de datos.

#### Flujo de Login:
1. El frontend manda `username` y `password`.
2. `AuthController` recibe la petición en `/login` y llama a `AuthService.login(...)`.
3. Si el usuario y contraseña son correctos, se devuelve el objeto `Jugador` con sus detalles básicos.

#### Flujo de Recuperación de Contraseña:
1. El usuario solicita recuperar su clave enviando `username` y `email` a `/forgot-password`.
2. `PasswordRecoveryService` genera un token único y lo envía al correo del usuario (si está habilitado el servicio de mail en `application.properties`).
3. El usuario ingresa la nueva contraseña junto con el token recibido en la petición `/reset-password`.
4. El token se valida, se consume y se actualiza la contraseña del usuario.

### Consulta de datos del jugador

1. El frontend llama a `/api/jugadores/{username}/datos`.
2. `JugadorController` busca el jugador.
3. Arma un `JugadorDatosResponse`.
4. Devuelve resumen del estado del jugador.

### Apertura de sobres

1. Se llama a `POST /api/sobres/abrir/{username}`.
2. `SobreService` valida sobres y cartas.
3. Genera cartas aleatorias.
4. Las agrega a la coleccion.
5. Actualiza el jugador.

### Guardado de mazo

1. El frontend manda un arreglo de 60 IDs.
2. `MazoController` recibe `GuardarMazoRequest`.
3. `MazoService` valida jugador, cantidad y existencia de cartas.
4. Se persiste el mazo.

### Batalla

1. Se inicia con `startBattle`.
2. Se crea una `Partida` en memoria.
3. El frontend consulta el estado con `state/{matchId}`.
4. Cada accion modifica la misma instancia de `Partida`.
5. Los services de turno, ataque y KO aplican las reglas.
6. La partida termina cuando no quedan premios o Pokemon disponibles.

---

## 10. Ideas clave para estudiar el backend

Si se quiere entender el proyecto rapido, conviene mirar este orden:

1. [`BackendApplication`](./src/main/java/com/pokemon/tcg/BackendApplication.java)
2. [`DataLoader`](./src/main/java/com/pokemon/tcg/config/DataLoader.java)
3. [`Card`](./src/main/java/com/pokemon/tcg/model/Card.java)
4. [`Partida`](./src/main/java/com/pokemon/tcg/model/battle/Partida.java)
5. [`BattleEngineService`](./src/main/java/com/pokemon/tcg/service/BattleEngineService.java)
6. [`BattleAttackService`](./src/main/java/com/pokemon/tcg/service/BattleAttackService.java)
7. [`BattleKoService`](./src/main/java/com/pokemon/tcg/service/BattleKoService.java)
8. [`BattleTurnService`](./src/main/java/com/pokemon/tcg/service/BattleTurnService.java)
9. [`BotAIService`](./src/main/java/com/pokemon/tcg/service/BotAIService.java)

Ese recorrido permite ver:

- como se cargan los datos,
- como se arma una partida,
- como se resuelven ataques,
- como se aplican estados,
- como se desencadena un KO.

---

## 11. Resumen corto

Este backend esta organizado en capas y gira alrededor de un motor de batalla que vive en memoria. Los controllers solo exponen endpoints, los services contienen la logica real, los repositorios resuelven persistencia y los modelos de batalla mantienen el estado de la partida mientras esta activa.

La pieza central es `BattleEngineService`, porque coordina el resto de servicios y hace de puente entre la API y el flujo completo del combate.
