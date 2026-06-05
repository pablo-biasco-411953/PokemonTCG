# Checklist QA inicial

## Deck Builder

- [ ] Permite más de 4 Energías Básicas.
- [ ] Mantiene límite de 4 para cartas no exentas.
- [ ] Bloquea mazos sin Pokémon Básico.
- [ ] Muestra error claro cuando el mazo no cumple reglas.

## Flujo de partida

- [ ] La partida tiene estado global visible o verificable.
- [ ] No se puede jugar carta antes de SETUP/ACTIVE.
- [ ] El turno avanza DRAW -> MAIN -> ATTACK -> BETWEEN_TURNS.
- [ ] FINISHED bloquea acciones.

## Energías

- [ ] Solo se puede unir 1 energía manual por turno.
- [ ] La restricción se reinicia al siguiente turno del jugador.
- [ ] Intentar unir una segunda energía muestra error.

## Retirada

- [ ] Al retirar, el jugador selecciona energías.
- [ ] No puede confirmar selección insuficiente.
- [ ] Las energías seleccionadas van al descarte.
- [ ] Las energías no seleccionadas quedan en el Pokémon.

## Daño

- [ ] Se aplica Debilidad.
- [ ] Se aplica Resistencia.
- [ ] Se acumula daño internamente.
- [ ] KO ocurre cuando daño acumulado >= HP.

## UI de batalla

- [ ] Contador de mazo baja inmediatamente al robar.
- [ ] Contador de mano oponente es siempre visible.
- [ ] La UI no oculta información pública relevante.

## Sobres

- [ ] Abrir sobre no congela la UI.
- [ ] Se muestran cartas obtenidas.
- [ ] Se puede cerrar y volver al lobby.
- [ ] Las cartas quedan persistidas después de refrescar.

