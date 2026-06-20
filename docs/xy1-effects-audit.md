# Auditoria de efectos XY1

> Este archivo queda como resumen narrativo. La auditoria operativa ahora vive en:
>
> - `docs/card-audit/xy1-effect-audit.tsv`
> - `docs/card-audit/implementation-backlog.md`
> - `docs/architecture/game-engine-plan.md`

El catalogo actual contiene 146 cartas, 202 ataques y 161 ataques con texto de efecto.

## Monedas

Hay 47 ataques con lanzamiento de moneda. Se separan en:

- dano adicional por cara;
- dano multiplicado por cantidad de caras;
- lanzamientos hasta obtener cruz;
- cantidad de monedas segun energias o contadores de dano;
- proteccion en cara;
- condicion especial en cara;
- efectos diferentes para cara y cruz;
- descarte, busqueda, cambio o restricciones;
- dano de retroceso en cruz.

El frontend no debe calcular estos resultados. Solo representa
`ultimasMonedasLanzadas`, que es la resolucion autoritativa del backend.

## Primera pasada implementada

- Dig y Scrunch: proteccion en cara.
- Pin Missile, Flash Needle, Spike Cannon, Dual Blades, Double Hit y equivalentes:
  dano por cada cara.
- Continuous Tumble y Spiny Rush: lanzamientos hasta cruz.
- Rock Black y Seething Anger: cantidad dinamica de monedas.
- Body Slam y estados equivalentes: el dano base se aplica siempre y la moneda
  controla solamente el estado.
- Distortion Beam: Dormido en cara, Confundido en cruz.
- Splash Bomb: dano normal y retroceso solamente en cruz.
- Hyper Fang: el dano se aplica solamente en cara.
- Double Draw, Filch y Me First: robo automatico desde el mazo con registro en el log.
- Pheromotion, Lead, Nasty Plot, Flame Charge y Energy Glide: seleccion guiada
  y validada por el backend para cartas legales del mazo.

## Acciones guiadas

Las decisiones ya no deben elegir automaticamente la primera coincidencia. La partida
puede pasar a `ESPERANDO_INTERACCION` con opciones legales, cantidad minima/maxima y
destino. El cliente muestra la seleccion y el backend vuelve a validar antes de mover
cartas y barajar.

## Pendientes por familia

- seleccion interactiva de objetivos de banca;
- busquedas y recuperaciones con eleccion del jugador;
- descarte de cartas concretas;
- bloqueos temporales de Supporter o ataques;
- ataques que modifican el siguiente turno;
- efectos sobre herramientas;
- dano a banca y dano propio a toda la banca;
- ordenamiento o inspeccion del mazo;
- efectos de habilidades, objetos, Supporter y estadios.

Cada familia debe cerrarse con pruebas de las cartas XY1 que la utilizan.
