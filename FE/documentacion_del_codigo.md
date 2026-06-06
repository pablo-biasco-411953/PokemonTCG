# Documentacion del codigo del frontend

Este archivo explica como esta armado el frontend de `PokemonTCG`, como arranca la aplicacion Angular, que hace cada pantalla, como se conectan los servicios con el backend y como fluye la informacion desde el login hasta el tablero de batalla.

La documentacion se centra solo en el frontend.

---

## 1. Punto de entrada de la aplicacion

El arranque principal esta en [`src/main.ts`](./src/main.ts).

Ahí se hace el bootstrap de la app con:

- `App` como componente raiz
- `provideHttpClient()` para habilitar peticiones HTTP
- `provideRouter(routes)` para el sistema de rutas

En otras palabras, Angular levanta la estructura base, registra el router y deja listo el cliente HTTP para hablar con el backend.

### `App`

El componente raiz esta en [`src/app/app.ts`](./src/app/app.ts) y solo actua como contenedor de rutas.

Su template, [`src/app/app.html`](./src/app/app.html), renderiza unicamente:

- `<router-outlet>`

Eso significa que la pantalla visible depende de la ruta activa.

---

## 2. Configuracion global

### `app.routes.ts`

El mapa de navegacion esta en [`src/app/app.routes.ts`](./src/app/app.routes.ts).

Rutas principales:

- `/login` -> `LoginComponent`
- `/lobby` -> `LobbyComponent`
- `/deck-builder` -> `DeckBuilderComponent`
- `/battle/:id` -> `BattleBoardComponent`

Tambien:

- la ruta vacia redirige a `login`
- cualquier ruta desconocida vuelve a `login`

### `app.config.ts`

[`src/app/app.config.ts`](./src/app/app.config.ts) concentra providers globales.

Alli se habilita:

- router
- manejo global de errores del navegador
- soporte HTTP

### Estilos globales

Los estilos globales estan en:

- [`src/styles.scss`](./src/styles.scss)
- [`src/app/app.scss`](./src/app/app.scss)

En este proyecto gran parte del look and feel vive en los estilos de cada componente, mientras que los globales quedan casi vacios.

---

## 3. Arquitectura del frontend

La app está organizada en una estructura limpia agrupada bajo carpetas principales:

- `core`: contiene servicios de datos globales, configuración de API y sonido (`src/app/core/services/`).
- `shared`: modelos y tipos de datos reutilizables (`src/app/shared/models/`).
- `features/login`: pantalla de acceso, registro interactivo y recuperación de contraseña (`src/app/features/login/`).
- `features/lobby`: panel principal del jugador con control de inventario y acceso a combate (`src/app/features/lobby/`).
- `features/lobby/components/apertura-sobre`: componente especializado 3D con Three.js para la apertura interactiva de sobres.
- `features/deck-builder`: constructor y editor de mazos de 60 cartas (`src/app/features/deck-builder/`).
- `features/battle`: tablero de batalla y paneles accesorios para debug, habilidades y pila de descarte (`src/app/features/battle/`).

El frontend sigue un flujo de UI donde cada pantalla consume un service especifico y traduce respuestas del backend a elementos visuales.

---

## 4. Modelos de datos

Los modelos viven en [`src/app/shared/models`](./src/app/shared/models).

### `card.ts`

Define la forma base de una carta consumida por el frontend.

Propiedades importantes:

- `id`
- `nombre`
- `tipo`
- `hp`
- `imagen`
- `supertype`
- `evolvesFrom`
- `costoRetirada`
- `ataques`
- `subtypes`
- `debilidades`
- `resistencias`

Tambien define:

- `Ataque`
- `CardWeakness`

Estas interfaces ayudan a tipar tanto el catalogo general como las cartas ya cargadas en juego.

### `jugador.ts`

Modela el estado del jugador en el front.

Incluye:

- `username`
- `sobresDisponibles`
- `coleccion`
- `cantidadCartas`

Tambien define `JugadorDatosResponse`, que es la respuesta resumida que usa el lobby para mostrar el estado del usuario.

### `mazo.ts`

Define el modelo de mazo usado en el lobby y el deck builder.

Contiene:

- `id`
- `nombre`
- `cartas`
- `jugador`

### `battle.ts`

Contiene las estructuras del combate:

- `CartaEnJuego`
- `TableroJugador`
- `Partida`
- `StartBattleResponse`
- `BattleActionCard`

Este archivo es importante porque representa el estado completo que el frontend recibe y vuelve a pintar desde el backend.

---

## 5. Servicios del frontend

Los services son el puente entre Angular y la API del backend.

### `AuthService`

[`src/app/core/services/auth.service.ts`](./src/app/core/services/auth.service.ts)

Responsabilidad:

- enviar credenciales al endpoint `/api/auth/login`
- enviar los datos de registro a `/api/auth/register`
- enviar los datos de restablecimiento y recuperación a `/api/auth/forgot-password` y `/api/auth/reset-password`

Este servicio maneja la comunicación completa para autenticación, registro y recuperación de contraseñas.

### `JugadorService`

[`src/app/core/services/jugador.service.ts`](./src/app/core/services/jugador.service.ts)

Responsabilidad:

- obtener el resumen del jugador
- obtener la coleccion completa

Endpoints que consume:

- `GET /api/jugadores/{username}/datos`
- `GET /api/jugadores/{username}/coleccion`

### `SobreService`

[`src/app/features/lobby/services/sobre.service.ts`](./src/app/features/lobby/services/sobre.service.ts)

Responsabilidad:

- abrir un sobre para un usuario

Endpoint:

- `POST /api/sobres/abrir/{username}`

### `MazoService`

[`src/app/features/deck-builder/services/mazo.service.ts`](./src/app/features/deck-builder/services/mazo.service.ts)

Responsabilidad:

- listar mazos de un jugador
- guardar mazos nuevos
- actualizar mazos existentes

Endpoints:

- `GET /api/mazos/listar/{username}`
- `POST /api/mazos/guardar`
- `PUT /api/mazos/actualizar/{id}`

### `BattleService`

[`src/app/features/battle/services/battle.service.ts`](./src/app/features/battle/services/battle.service.ts)

Es el servicio que centraliza todas las acciones de combate.

Responsabilidades:

- iniciar batalla
- leer estado de partida
- tirar moneda inicial
- elegir quien arranca
- jugar Pokemon
- unir energias
- atacar
- subir un Pokemon de banca
- pasar turno
- retirarse
- evolucionar
- pedir turno del bot
- usar endpoints de debug

Este service traduce cada accion de UI a una llamada HTTP concreta.

### `BattleBoardUiService`

[`src/app/features/battle/services/battle-board-ui.service.ts`](./src/app/features/battle/services/battle-board-ui.service.ts)

Es un service puramente visual.

Responsabilidades:

- resolver sprites front/back de Pokemon
- calcular porcentajes de HP
- devolver imagen local de una carta
- generar slots vacios para la banca
- decidir si una carta es Pokemon o energia
- traducir tipos de energia
- resaltar palabras clave en textos de ataques
- extraer un glosario de efectos

Tambien contiene un gran diccionario `pokedexNum` para mapear nombres de cartas a numeros de Pokedex y asi construir los sprites animados desde PokeAPI.

### `BattleBoardAttackService`

[`src/app/features/battle/services/battle-board-attack.service.ts`](./src/app/features/battle/services/battle-board-attack.service.ts)

Este servicio no habla con el backend: ayuda a interpretar ataques en el tablero.

Responsabilidades:

- detectar ataques que usan monedas
- calcular si un ataque puede pagarse con las energias unidas
- listar energias ya cumplidas
- calcular energias faltantes
- normalizar tipos

Es una capa utilitaria para que la interfaz pueda mostrar mejor el estado de ataque antes de ejecutar la accion.

---

## 6. Pantallas principales

### `LoginComponent`

Ubicado en [`src/app/features/login/login.component.ts`](./src/app/features/login/login.component.ts).

Responsabilidades y Flujos:

1. **Inicio de sesión (Login)**:
   - Captura el nombre de usuario (`username`) y la contraseña (`password`).
   - Llama a `AuthService.login` y, si la autenticación es exitosa, guarda los datos del jugador en `localStorage` e inicia una secuencia cinemática de acceso antes de redirigir al lobby.
2. **Registro de nuevos usuarios (Register)**:
   - Permite cambiar al modo de registro, capturando `screenName` (nombre en pantalla), `email` (correo electrónico), `password`, y `confirmPassword`.
   - Realiza validaciones interactivas de la fortaleza de la contraseña (utilizando animaciones de Pikachu que reaccionan al estado de la contraseña) y verifica que las contraseñas coincidan y que los términos sean aceptados antes de llamar a `AuthService.register`.
3. **Recuperación de contraseña (Forgot / Reset Password)**:
   - Permite ingresar el nombre de usuario o correo para iniciar el flujo de olvido de contraseña (`forgotPassword`).
   - Una vez enviado el token por correo, el componente permite ingresar el token recibido y la nueva contraseña para consumirlo (`resetPassword`) y actualizar las credenciales.

El HTML y SCSS de esta pantalla viven en:

- [`login.component.html`](./src/app/features/login/login.component.html)
- [`login.component.scss`](./src/app/features/login/login.component.scss)

### `LobbyComponent`

Ubicado en [`src/app/features/lobby/lobby.component.ts`](./src/app/features/lobby/lobby.component.ts).

Es la pantalla principal del jugador.

Responsabilidades:

- recuperar el jugador desde `localStorage`
- cargar resumen de jugador
- cargar mazos
- abrir sobres
- mostrar zoom de cartas
- navegar al deck builder
- iniciar partidas

#### Flujo de carga

1. Lee `localStorage`.
2. Si no existe jugador, manda a `/login`.
3. Si existe, llama a `refrescarTodo()`.
4. Carga datos del jugador con `JugadorService`.
5. Carga mazos con `MazoService`.

#### Funciones clave

- `refrescarTodo()`: actualiza sobres, cantidad de cartas y mazos.
- `cargarMazosDeJugador()`: trae los mazos del jugador.
- `abrirSobres()`: llama al backend y dispara la animacion de apertura.
- `finalizarApertura()`: cierra la experiencia de sobre y recarga el estado.
- `irAlDeckBuilder()`: navega al constructor.
- `buscarPartida(mazoId)`: inicia la partida con `BattleService`.
- `mostrarZoom(carta, event)`: abre una ficha flotante de la carta.
- `ocultarZoom()`: cierra el zoom.

El lobby combina una capa de inventario, una vista de mazos y un acceso rapido al combate.

El HTML esta en [`lobby.component.html`](./src/app/features/lobby/lobby.component.html) y los estilos en [`lobby.component.scss`](./src/app/features/lobby/lobby.component.scss).

### `DeckBuilderComponent`

Ubicado en [`src/app/features/deck-builder/deck-builder.component.ts`](./src/app/features/deck-builder/deck-builder.component.ts).

Es el editor de mazos.

Responsabilidades:

- cargar la coleccion del jugador
- filtrar cartas por nombre y tipo
- mostrar cuantas copias posee el jugador
- construir un mazo de hasta 60 cartas
- respetar el maximo de 4 copias por carta
- guardar o actualizar mazos
- mostrar inspeccion ampliada de cartas

#### Flujo principal

1. Lee el jugador desde `localStorage`.
2. Carga su coleccion con `JugadorService`.
3. Si viene un mazo en `history.state`, entra en modo edicion.
4. El usuario filtra, inspecciona y agrega cartas.
5. Cuando llega a 60 cartas, puede guardar.

#### Comportamiento importante

- `obtenerCartasUnicas()` evita duplicados visuales en la Pokedex.
- `actualizarCantidadesPoseidas()` cuenta copias reales.
- `agregarAlMazo()` valida stock, limite de 4 y limite total de 60.
- `guardar()` decide entre crear o actualizar segun exista `idMazoAEditar`.

El HTML esta en [`deck-builder.component.html`](./src/app/features/deck-builder/deck-builder.component.html) y el SCSS en [`deck-builder.component.scss`](./src/app/features/deck-builder/deck-builder.component.scss).

### `BattleBoardComponent`

Ubicado en [`src/app/features/battle/battle-board.component.ts`](./src/app/features/battle/battle-board.component.ts).

Es la pantalla mas compleja del frontend.

Responsabilidades:

- mostrar el estado completo de la partida
- reaccionar al turno actual
- permitir jugar Pokemon, energias, ataques, evolucion, retirada y pase de turno
- animar moneda, impactos, estados y cambios visuales
- sincronizar la UI con el backend
- mostrar herramientas de debug

#### Como arranca

1. Toma el `matchId` desde la ruta `/battle/:id`.
2. Carga el estado de la partida con `BattleService.getState`.
3. Configura intro visual y overlays.
4. Comienza a renderizar la mesa completa.

#### Que representa visualmente

La pantalla separa el tablero en:

- premios del bot y del jugador
- mano del bot
- banca rival
- arena central
- banca del jugador
- mano del jugador
- pilas de descarte
- paneles de info y debug

#### Acciones principales

- `jugarCarta(carta)`: baja Pokemon desde la mano.
- `gestionarUnionEnergia(...)`: une energia al activo.
- `realizarAccion(ataque)`: ejecuta un ataque.
- `iniciarModoRetirada()`: habilita la retirada.
- `pasarTurno()`: termina el turno.
- `seleccionarBanca(...)`: elige un Pokemon suplente.
- `abrirModalDescarte(...)`: revisa las cartas descartadas.
- `debugRobarCarta(...)`, `debugForzarEstado(...)`, `debugSetHp(...)`: herramientas de prueba.

#### Apoyo visual

El componente usa:

- `BattleBoardUiService` para sprites, tipos, HP y glosario
- `BattleBoardAttackService` para validar ataques y costos

Además mantiene mucho estado de UI:

- overlays de turno
- moneda
- animaciones de ataque
- daño flotante
- retroalimentacion de energias
- zoom de cartas
- paneles de ayuda
- panel de depuracion

El HTML y CSS de esta pantalla son los mas grandes del proyecto:

- [`battle-board.component.html`](./src/app/features/battle/battle-board.component.html)
- [`battle-board.component.scss`](./src/app/features/battle/battle-board.component.scss)

### `AperturaSobreComponent`

Ubicado en [`src/app/features/lobby/components/apertura-sobre/apertura-sobre.ts`](./src/app/features/lobby/components/apertura-sobre/apertura-sobre.ts).

Es una experiencia visual 3D para abrir sobres.

Responsabilidades:

- crear escena Three.js
- renderizar el sobre
- interpretar el gesto de corte
- disparar explosion de particulas
- revelar cartas una por una
- notificar cuando termina la apertura

#### Flujo visual

1. Se monta el componente.
2. Se inicializa Three.js.
3. Se crea el sobre.
4. El usuario mantiene y desliza para cortarlo.
5. Se dispara la explosion.
6. Se generan las cartas reveladas.
7. Se puede seguir tocando o deslizando para avanzar.
8. Al terminar la secuencia, se emite el cierre.

Este componente trabaja con `@Input() cartas` y `@Output() onClose`.

Los archivos visuales son:

- [`apertura-sobre.html`](./src/app/features/lobby/components/apertura-sobre/apertura-sobre.html)
- [`apertura-sobre.scss`](./src/app/features/lobby/components/apertura-sobre/apertura-sobre.scss)

---

## 7. Flujo de informacion completo

### Login

1. El usuario escribe su nombre.
2. `LoginComponent` llama a `AuthService`.
3. El backend devuelve el jugador.
4. Se guarda en `localStorage`.
5. Se redirige al lobby.

### Lobby

1. `LobbyComponent` recupera el jugador desde `localStorage`.
2. Consulta resumen y coleccion.
3. Consulta mazos.
4. Puede abrir sobres o ir al deck builder.
5. Puede iniciar una batalla con un mazo.

### Constructor de mazos

1. Se carga la coleccion del jugador.
2. Se muestran filtros y cantidades.
3. El usuario arma un mazo.
4. Se valida el limite de 60 cartas.
5. Se guarda o actualiza en el backend.

### Batalla

1. El lobby crea una partida con un mazo.
2. La ruta lleva a `/battle/:id`.
3. `BattleBoardComponent` consulta el estado de la partida.
4. Cada accion del jugador pega al backend por `BattleService`.
5. El tablero se repinta con la nueva instancia devuelta.

### Apertura de sobres

1. El lobby llama a `SobreService`.
2. Se activan las animaciones de apertura.
3. `AperturaSobreComponent` muestra la secuencia 3D.
4. Al terminar, el lobby refresca datos y mazos si hace falta.

---

## 8. Estado local y persistencia ligera

El frontend usa `localStorage` para guardar el jugador despues del login.

Eso sirve para:

- mantener la sesion visual al navegar entre pantallas
- recuperar el username en el lobby y el deck builder
- evitar pedir login en cada cambio de ruta dentro de la misma sesion del navegador

Se almacena el objeto jugador en `localStorage` con su username y sobres disponibles, pero el backend ahora exige contraseña para el inicio de sesión y valida los tokens de recuperación para asegurar las cuentas.

---

## 9. Recursos estaticos

La carpeta [`public`](./public) contiene recursos que la UI usa directamente.

Entre ellos:

- `coin-heads.png`
- `coin-tails.png`
- `card-back.png`
- `images/fondolobby.webm`
- `images/cards/*`

Estos assets son importantes porque gran parte de la identidad visual del proyecto depende de cartas reales, backs, sprites y fondos.

---

## 10. Scripts y build

En [`package.json`](./package.json) hay scripts para:

- `npm start` -> levantar Angular en desarrollo
- `npm run build` -> generar build final
- `npm test` -> ejecutar tests de Angular
- `npm run test:unit` -> correr un test puntual de batalla con Vitest

Tambien se ve que el front usa:

- Angular 21
- TypeScript
- RxJS
- Three.js
- Angular CDK para drag and drop

---

## 11. Orden recomendado para estudiar el frontend

Si queres entender la app de forma rapida, conviene leer este orden:

1. [`src/main.ts`](./src/main.ts)
2. [`src/app/app.routes.ts`](./src/app/app.routes.ts)
3. [`src/app/core/services/auth.service.ts`](./src/app/core/services/auth.service.ts)
4. [`src/app/features/login/login.component.ts`](./src/app/features/login/login.component.ts)
5. [`src/app/features/lobby/lobby.component.ts`](./src/app/features/lobby/lobby.component.ts)
6. [`src/app/features/deck-builder/deck-builder.component.ts`](./src/app/features/deck-builder/deck-builder.component.ts)
7. [`src/app/features/battle/battle-board.component.ts`](./src/app/features/battle/battle-board.component.ts)
8. [`src/app/features/lobby/components/apertura-sobre/apertura-sobre.ts`](./src/app/features/lobby/components/apertura-sobre/apertura-sobre.ts)

Ese recorrido deja ver como la app pasa de acceso, a gestion de coleccion, a armado de mazo y finalmente a combate.

---

## 12. Resumen corto

El frontend esta armado como una app Angular standalone, con rutas claras y servicios separados por dominio. El lobby organiza la experiencia principal, el deck builder gestiona la construccion de mazos, el battle board concentra toda la interaccion de combate y `AperturaSobreComponent` agrega la experiencia 3D de sobres.

La mayor parte del flujo visual depende de estado traido desde el backend, mientras que la UI se apoya en `localStorage`, servicios tipados y componentes especializados para transformar datos en una experiencia jugable.
