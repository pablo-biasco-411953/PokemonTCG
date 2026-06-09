---
sidebar_position: 11
title: 🧠 Algoritmos Clave
---

# 🧠 Algoritmos Clave del Sistema

---

## 1. Validación de Construcción de Mazo

```java
public boolean isValidDeck(List<Card> cards) {
  // Regla 1: Exactamente 60 cartas
  if (cards.size() != 60) return false;
  
  // Regla 2: Máximo 4 copias de cada carta
  Map<String, Integer> counts = new HashMap<>();
  for (Card card : cards) {
    int count = counts.getOrDefault(card.id, 0) + 1;
    if (count > 4) return false;
    counts.put(card.id, count);
  }
  
  // Regla 3: Mínimo 1 Pokémon básico
  long basicCount = cards.stream()
    .filter(c -> c.isBasic())
    .count();
  if (basicCount < 1) return false;
  
  // Regla 4: Líneas evolutivas completas
  for (Card card : cards) {
    if (card.evolutionStage > 0) {
      Card predecessor = findPredecessor(card, cards);
      if (predecessor == null) return false;
    }
  }
  
  return true;
}
```

---

## 2. Selección de Cartas al Abrir Sobre

```java
public List<Card> generateBoosterPack() {
  List<Card> pack = new ArrayList<>();
  
  // Garantía: 1 Rara
  Card rare = selectRandomByRarity(CardRarity.RARE);
  pack.add(rare);
  
  // 2 No Comunes
  for (int i = 0; i < 2; i++) {
    Card uncommon = selectRandomByRarity(CardRarity.UNCOMMON);
    pack.add(uncommon);
  }
  
  // 7 Comunes
  for (int i = 0; i < 7; i++) {
    Card common = selectRandomByRarity(CardRarity.COMMON);
    pack.add(common);
  }
  
  // 1% chance de holo rara (reemplaza una común)
  if (Math.random() < 0.01) {
    pack.remove(pack.size() - 1);
    pack.add(selectRandomByRarity(CardRarity.HOLO_RARE));
  }
  
  return pack;
}
```

---

## 3. Cálculo de Daño

```java
public int calculateDamage(Pokemon attacker, Pokemon defender, Attack attack) {
  int damage = attack.baseDamage;
  
  // Efectividad de tipo
  if (isEffectiveAgainst(attacker.type, defender.type)) {
    damage *= 2;
  }
  
  // Debilidad
  if (defender.weakness == attacker.type) {
    damage += 20;
  }
  
  // Resistencia
  if (defender.resistance == attacker.type) {
    damage = Math.max(0, damage - 20);
  }
  
  // Modificadores persistentes (Quemadura, etc)
  if (defender.isAfflictedWith(Status.BURNED)) {
    damage -= 10;
  }
  
  return Math.max(0, damage);
}
```

---

## 4. Detección de KO

```java
public void applyDamage(Pokemon target, int damage) {
  target.currentHp -= damage;
  
  if (target.currentHp <= 0) {
    target.currentHp = 0;  // No puede ser negativo
    declarePokemonKO(target);
    grantPrizeCard(target.owner.opponent);
    
    // Cambiar a siguiente Pokémon si es el activo
    if (target == target.owner.activePokemon) {
      target.owner.mustChooseNewActive = true;
    }
  }
}
```

---

## 5. Búsqueda de Línea Evolutiva

```java
public List<Card> getEvolutionLine(Card card) {
  List<Card> line = new ArrayList<>();
  
  // Subir hasta el básico
  Card current = card;
  while (current.evolutionStage > 0) {
    Card predecessor = findByName(current.evolvesFrom);
    if (predecessor == null) break;
    line.add(0, predecessor);
    current = predecessor;
  }
  
  // Añadir el card actual
  line.add(card);
  
  // Bajar hasta las evoluciones finales
  List<Card> allEvolutions = findAllEvolutions(card);
  for (Card evo : allEvolutions) {
    if (evo.evolutionStage == card.evolutionStage + 1) {
      line.add(evo);
    }
  }
  
  return line;
}
```

---

## Complejidad Temporal

| Algoritmo | Complejidad | Justificación |
|-----------|-----------|---------------|
| Validar Mazo | O(n) | Itera 60 cartas una vez |
| Generar Booster | O(1) | Selecciona 10 cartas aleatorias |
| Calcular Daño | O(1) | Solo aritmética |
| KO Detection | O(1) | Comparación simple |
| Evolución Line | O(n) | Busca en árbol de cartas |

---

*Fin de Arquitectura Técnica*
