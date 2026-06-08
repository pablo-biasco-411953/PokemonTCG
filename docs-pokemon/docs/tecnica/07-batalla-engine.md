---
sidebar_position: 7
title: ⚙️ Battle Engine
---

# ⚙️ Battle Engine - Lógica de Batalla

---

## Estado de Batalla

```typescript
interface BattleState {
  id: Long;
  currentPlayer: 1 | 2;
  phase: 'INIT' | 'MAIN' | 'ATTACK' | 'CLEANUP';
  
  player1: {
    active: Pokemon;
    bench: Pokemon[];
    hand: Card[];
    deck: Card[];
    discarded: Card[];
    prizes: Card[];
  };
  
  player2: {
    // Mismo que player1
  };
}
```

---

## Validación de Acciones

```java
private boolean isActionValid(BattleState state, BattleAction action) {
  // Validar que es el turno del jugador
  if (state.currentPlayer != action.playerNumber) return false;
  
  // Validar por fase
  switch(action.getType()) {
    case JUGAR_POKEMON:
      return canPlayPokemon(state);
    case ATACAR:
      return canAttack(state) && hasEnoughEnergy(state, action);
    case PASAR_TURNO:
      return true; // Siempre puedes pasar
  }
  
  return false;
}
```

---

## Cálculo de Daño

```java
private int calculateDamage(Pokemon attacker, Pokemon defender, Attack attack) {
  int baseDamage = attack.getDamage();
  
  // Modificadores de tipo (efectividad)
  if (isTypeEffective(attacker, defender)) {
    baseDamage *= 2;  // 2x daño
  }
  
  if (isWeakTo(defender, attacker)) {
    baseDamage += 20; // +20 por debilidad
  }
  
  if (hasResistance(defender, attacker)) {
    baseDamage -= 20; // -20 por resistencia
  }
  
  return Math.max(0, baseDamage);
}
```

---

## KO Detection

```java
private void checkKO(BattleState state) {
  if (state.player1.active.getHp() <= 0) {
    koPlayer(state, 1);
    state.player2.prizes.add(drawPrize());
  }
  
  if (state.player2.prizes.size() >= 6) {
    declareWinner(state, 2);
  }
}
```

---

*Próximo: [WebSocket & Lobby](/docs/tecnica/websocket-lobby)*
