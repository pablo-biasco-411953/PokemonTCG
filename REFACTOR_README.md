# Refactor Roadmap

Este archivo define el plan de refactor del proyecto `PokemonTCG` con un enfoque incremental y seguro.
La meta es mejorar orden, mantenibilidad y claridad sin romper el funcionamiento base.

## Objetivo

Mantener operativos estos flujos durante todo el proceso:

- Login
- Lobby
- Apertura de sobres
- Constructor de mazos
- Inicio de batalla
- Acciones base de batalla: jugar carta, unir energia, atacar, pasar turno, retirada

## Regla De Trabajo

Cada etapa debe cumplir estas condiciones antes de darse por terminada:

- La app sigue levantando en backend y frontend.
- Los flujos base siguen funcionando.
- No se mezclan cambios estructurales con cambios funcionales grandes.
- Si una etapa toca logica critica, se valida manualmente y, si aplica, con tests.

## Checklist Base De Verificacion

Usar esta lista despues de cada etapa importante:

1. Backend levanta sin errores.
2. Frontend compila y levanta sin errores.
3. Login responde.
4. Lobby carga correctamente.
5. Se puede abrir un sobre.
6. Se puede armar o visualizar un mazo.
7. Se puede iniciar una batalla.
8. Se puede atacar y pasar turno.

## Etapas Del Refactor

### Etapa 1 - Congelar el estado actual

Objetivo:
Definir el comportamiento minimo que no podemos romper.

Tareas:

- [x] Documentar comandos reales para levantar backend y frontend.
- [x] Confirmar rutas y dependencias efectivas.
- [x] Revisar el estado actual de carpetas principales.
- [x] Tomar esta checklist base como contrato de funcionamiento.

Estado actual confirmado:

- Backend Spring Boot en `backend/`
- `pom.xml` real en `backend/pom.xml`
- Wrapper Maven compartido en raiz: `mvnw`, `mvnw.cmd`, `.mvn/`
- Frontend Angular en `frontend/`
- Angular sirve assets desde `frontend/public`

Comandos reales:

- Backend: `.\mvnw.cmd -f backend/pom.xml spring-boot:run`
- Backend test/build: `.\mvnw.cmd -f backend/pom.xml test`
- Frontend: `cd frontend` y luego `npm start`
- Frontend build: `cd frontend` y luego `npm run build`

Validacion:

- Ambos proyectos levantan.
- Tenemos claro que flujos vamos a proteger.

Estado:

- [x] Completada

### Etapa 2 - Limpieza estructural segura

Objetivo:
Eliminar ruido del repo sin tocar logica de negocio.

Tareas:

- [x] Revisar archivos sobrantes en la raiz.
- [x] Corregir `.gitignore` raiz.
- [x] Identificar directorios generados que no deberian versionarse.
- [x] Revisar si `backend/backend` es una copia residual y consolidarlo.
- [x] Revisar configuraciones de IDE que no convenga dejar en el repo.

Implementacion realizada:

- Se amplio el `.gitignore` raiz para cubrir artefactos de backend, frontend, IDE y temporales.
- Se confirmo que el wrapper Maven de la raiz es parte util del repo actual, por lo que se conserva.
- Se detecto `backend/backend/.../JugadorDTO.java` como duplicado residual del DTO real y se elimina en esta etapa.
- Se elimina tambien la carpeta residual `backend/backend/`.
- Se detecto `package-lock.json` en raiz como archivo residual vacio y se elimina en esta etapa.

Pendientes de revision manual posterior:

- `index.html` en raiz
- `descargar_cartas.js` en raiz

Se dejan por ahora porque podrian ser utilidades manuales fuera del flujo principal y no impactan el arranque del sistema.

Items a revisar primero:

- `index.html` en raiz
- `package-lock.json` en raiz
- `.mvn/`, `mvnw`, `mvnw.cmd` en raiz versus `backend/`
- `backend/target`
- `frontend/node_modules`
- `frontend/.angular/cache`
- `.idea`
- `backend/backend`

Validacion:

- La estructura queda mas clara.
- No se rompe el arranque del proyecto.
- El backend compila correctamente con `.\mvnw.cmd -f backend/pom.xml test`.
- El frontend llega a compilar bundle, pero falla por budgets ya existentes de Angular en produccion. No parece una regresion causada por esta etapa.

Estado:

- [x] Completada

### Etapa 3 - Unificacion de assets del frontend

Objetivo:
Definir una sola fuente de verdad para imagenes y media.

Tareas:

- [x] Identificar que rutas se usan realmente desde Angular.
- [x] Elegir una carpeta canonica para assets publicos.
- [x] Migrar referencias de forma controlada.
- [x] Eliminar duplicados despues de validar.

Carpetas candidatas detectadas:

- `frontend/public`
- `frontend/images`
- `frontend/src/assets`
- archivos sueltos dentro de `frontend/`

Validacion:

- Lobby, batalla y apertura de sobres siguen cargando media correctamente.
- No quedan referencias rotas.

Estado:

- [x] Completada

Implementacion realizada:

- Se definio `frontend/public` como unica fuente de verdad para assets publicos.
- Se migraron referencias clave del frontend a rutas absolutas bajo `/images/...`.
- Se ajustaron rutas en `deck-builder`, `battle-board` y `apertura-sobre`.
- Se eliminaron duplicados externos a `public/`:
  - `frontend/assets`
  - `frontend/images`
  - `frontend/src/assets`
  - `frontend/back.png`
  - `frontend/card-back.png`

Validacion realizada:

- El frontend sigue resolviendo rutas de assets desde `public/`.
- El build llega a compilar bundle y mantiene el mismo bloqueo por budgets de produccion.

### Etapa 4 - Documentacion y configuracion

Objetivo:
Actualizar documentacion para que refleje el estado real del repo.

Tareas:

- [x] Corregir encoding del `README.md`.
- [x] Alinear versiones reales de Angular, Java y stack.
- [x] Corregir instrucciones de arranque.
- [x] Documentar brevemente la estructura final del repo.

Validacion:

- La documentacion coincide con la implementacion real.

Estado:

- [x] Completada

Implementacion realizada:

- Se reescribio `README.md` con texto legible y estructura breve.
- Se actualizo Angular a version real 21.
- Se dejaron comandos reales de backend y frontend.
- Se documento la estructura efectiva del repo y el estado del build.

### Etapa 5 - Refactor del backend por responsabilidades

Objetivo:
Reducir complejidad en servicios grandes sin cambiar contratos publicos al comienzo.

Tareas:

- Revisar `BattleEngineService`.
- Revisar `BotAIService`.
- Extraer helpers o servicios para:
  - calculo de dano
  - estados alterados
  - coin flips
  - validaciones de turno
  - resolucion de acciones
  - heuristicas del bot
- Mantener controladores estables en esta primera pasada.

Validacion:

- Los endpoints siguen comportandose igual.
- La logica principal queda mas modular.

Estado:

- [x] Completada

Implementacion realizada:

- Se extrajo la responsabilidad de limpieza de fin de turno y mantenimiento entre turnos a `BattleTurnService`.
- `BattleEngineService` ahora delega esa parte del ciclo de turno en un servicio dedicado.
- Se extrajo la resolucion principal de ataques a `BattleAttackService`.
- `BattleEngineService` ahora delega el flujo real de dano, monedas y efectos secundarios en ese servicio.
- Se extrajo la resolucion de KO, descarte, premios y reemplazo del bot a `BattleKoService`.
- Se reescribio `BattleEngineService` para eliminar codigo residual viejo y dejar el flujo principal mas claro.
- Se mantuvo intacto el contrato publico del motor de batalla.
- Se agrego `BattleKoServiceTest` para cubrir la nueva responsabilidad extraida.

Validacion realizada:

- El backend pasa `.\mvnw.cmd -f backend\pom.xml test`.
- Resultado actual backend: 9 tests, 0 fallos.
- No hay cambios de endpoints ni de firmas publicas en controllers.

### Etapa 6 - Refactor del frontend por responsabilidades

Objetivo:
Dividir componentes gigantes sin alterar UX base.

Tareas:

- Reducir complejidad de `battle-board.component.ts`.
- Reducir complejidad de su HTML y SCSS.
- Extraer subcomponentes visuales cuando aporte claridad.
- Extraer helpers o servicios para:
  - animaciones
  - coin flip
  - mapeo visual de cartas
  - estado derivado de batalla
  - utilidades de imagenes y rutas

Validacion:

- La pantalla sigue funcionando igual.
- El componente principal pierde responsabilidades.

Estado:

- [~] En progreso

Implementacion basica realizada:

- Se extrajo `BattleBoardUiService` desde `battle-board.component.ts`.
- El componente ahora delega:
  - glosario de estados
  - formateo visual de texto de ataques
  - sprites e imagenes
  - colores y nombres de energia
  - helpers de HP y deteccion de cartas
- Se elimino del componente el gran mapa local de Pokedex y varias utilidades de presentacion.

Validacion realizada:

- El refactor del `battle-board` ya no deja errores de TypeScript propios del componente tocado.
- `npm run build` sigue fallando por un problema previo/externo de resolucion de rutas y estilos del proyecto Angular, no por el servicio nuevo extraido.

Pendiente para la etapa ampliada:

- Seguir partiendo `battle-board.component.ts` en helpers o subcomponentes visuales.
- Reducir responsabilidades del HTML y del SCSS.
- Resolver el problema actual de build Angular para poder validar la etapa con mayor comodidad.

### Etapa 7 - Normalizacion de modelos y contratos

Objetivo:
Alinear mejor backend y frontend.

Tareas:

- Revisar DTOs del backend.
- Revisar models del frontend.
- Corregir nombres inconsistentes.
- Detectar duplicados o tipos demasiado ambiguos.

Validacion:

- Los contratos quedan mas claros.
- El tipado del frontend mejora.

Estado:

- [ ] Pendiente

### Etapa 8 - Red de seguridad

Objetivo:
Dejar validaciones para movernos con menos riesgo.

Tareas:

- Agregar tests focalizados del backend en logica critica.
- Agregar tests focalizados del frontend en servicios o componentes clave.
- Si no se automatiza todo, dejar procedimientos manuales breves y repetibles.

Validacion:

- Tenemos al menos una red minima antes de cambios mas profundos.

Estado:

- [ ] Pendiente

### Etapa 9 - Cierre y endurecimiento

Objetivo:
Cerrar el refactor con una estructura limpia y sostenible.

Tareas:

- Eliminar codigo muerto confirmado.
- Limpiar imports y archivos temporales.
- Revisar nombres de carpetas y consistencia final.
- Hacer una pasada final de documentacion.

Validacion:

- El repo queda entendible y mas mantenible.
- Los flujos base siguen funcionando.

Estado:

- [ ] Pendiente

## Orden Recomendado De Trabajo

1. Etapa 1
2. Etapa 2
3. Etapa 3
4. Etapa 4
5. Etapa 8 basica
6. Etapa 5
7. Etapa 6
8. Etapa 7
9. Etapa 8 ampliada
10. Etapa 9

## Criterio De Aprobacion De Cada Bloque

Podemos considerar una etapa completa cuando:

- La estructura mejora de forma visible.
- No aparece una regresion en los flujos base.
- El cambio deja el siguiente paso mas facil, no mas riesgoso.

## Proxima Ejecucion Recomendada

Arrancar por:

1. Etapa 1: validar arranque real de backend y frontend.
2. Etapa 2: limpieza estructural segura.

Estas dos etapas son las mas convenientes para empezar porque ordenan el terreno antes de tocar logica compleja.
