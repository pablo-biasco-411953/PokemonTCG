# Manual Validation Scenarios - Batch 1: Next Turn Attack Restrictions

This document details the exact game state scenarios to test manually inside the application to verify the correct behavior of the implemented card effects.

---

## Scenario 1: Rhyperior (xy1-62) - "Rock Wrecker"
**Effect:** "This Pokémon can't attack during your next turn."

### Setup
1. Put **Rhyperior** (xy1-62) in the active position with the required energies for **Rock Wrecker**.
2. Put any opponent Pokémon in the active position.

### Execution & Verification Steps
1. Use **Rock Wrecker**. It should deal 130 damage.
2. End your turn.
3. Opponent plays their turn and passes.
4. When your turn begins:
   - Try to use **Rock Wrecker** (or any other attack of Rhyperior).
   - **Expected behavior:** The application must prevent the attack and throw an error or display that the active Pokémon cannot attack this turn.
5. Pass your turn without attacking.
6. Opponent plays their turn and passes.
7. On your next turn:
   - Try to attack with Rhyperior.
   - **Expected behavior:** You should now be able to use Rhyperior's attacks normally.

---

## Scenario 2: Yveltal (xy1-78) - "Darkness Blade"
**Effect:** "Flip a coin. If tails, this Pokémon can't attack during your next turn."

### Setup
1. Put **Yveltal** (xy1-78) in the active position with the required energies for **Darkness Blade**.

### Execution & Verification Steps (Tails case)
1. Use **Darkness Blade**.
2. If the coin flip is **Tails**:
   - End your turn.
   - Opponent plays their turn and passes.
   - When your turn starts, try to attack with Yveltal.
   - **Expected behavior:** You must be blocked from attacking.
3. If the coin flip is **Heads**:
   - End your turn.
   - Opponent plays their turn and passes.
   - When your turn starts, try to attack with Yveltal.
   - **Expected behavior:** You should be able to attack normally.

---

## Scenario 3: Wigglytuff (xy1-89) - "Hocus Pinkus"
**Effect:** "The Defending Pokémon can't attack during your opponent's next turn."

### Setup
1. Put **Wigglytuff** (xy1-89) in the active position with the required energies for **Hocus Pinkus**.
2. Opponent has a Pokémon in the active position.

### Execution & Verification Steps
1. Use **Hocus Pinkus**.
2. End your turn.
3. It is now the opponent's turn:
   - Opponent attempts to attack.
   - **Expected behavior:** The opponent's attack must be blocked, preventing them from attacking during this turn.
4. Opponent passes their turn.
5. On your turn, play normally and pass.
6. On the opponent's next turn:
   - Opponent attempts to attack.
   - **Expected behavior:** The opponent should now be able to attack normally (the restriction has expired).

---

## Scenario 4: Aegislash (xy1-86) - "King's Shield"
**Effect:** "Prevent all damage done to this Pokémon by attacks during your opponent's next turn. This Pokémon can't use King's Shield during your next turn."

### Setup
1. Put **Aegislash** (xy1-86) in the active position with the required energies for **King's Shield**.
2. Opponent has a Pokémon in the active position.

### Execution & Verification Steps
1. Use **King's Shield**. It should deal 50 damage.
2. End your turn.
3. It is now the opponent's turn:
   - Opponent attempts to attack Aegislash.
   - **Expected behavior:** The attack must hit but deal **0 damage** (invulnerable).
4. Opponent ends their turn.
5. When your turn starts:
   - Select Aegislash.
   - Try to use **King's Shield** again.
   - **Expected behavior:** The application must block using **King's Shield** (an error or message stating it is blocked).
   - Try to use Aegislash's other attack (**Buster Swing**).
   - **Expected behavior:** You should be allowed to use **Buster Swing** normally.
6. End your turn.
7. Opponent plays their turn and passes.
8. When your next turn starts:
   - Try to use **King's Shield**.
   - **Expected behavior:** You should now be allowed to use **King's Shield** again (the block has expired).

---
---

# Manual Validation Scenarios - Batch 2: Bench Damage & Bench Scaling

This section details the manual verification steps for the newly implemented bench damage and bench scaling effects.

---

## Scenario 5: Raichu (xy1-43) - "Circle Circuit"
**Effect:** "This attack does 20 damage times the number of your Benched Pokémon."

### Setup
1. Put **Raichu** (xy1-43) in the active position with the required energy to use **Circle Circuit**.
2. Place **3 Pokémon** on your bench.
3. Opponent has an active Pokémon (e.g., HP 100).

### Execution & Verification Steps
1. Use **Circle Circuit**.
2. **Expected behavior:** The attack should deal **60 damage** (20 * 3 benched Pokémon) to the active opponent Pokémon.
3. Put another Pokémon on your bench (now 4).
4. Use **Circle Circuit** again on your next turn.
5. **Expected behavior:** The attack should now deal **80 damage** (20 * 4 benched Pokémon).

---

## Scenario 6: Dugtrio (xy1-59) - "Earthquake"
**Effect:** "This attack does 10 damage to each of your Benched Pokémon."

### Setup
1. Put **Dugtrio** (xy1-59) in the active position with the required energies for **Earthquake**.
2. Place **2 Pokémon** on your bench, noting their current HP.

### Execution & Verification Steps
1. Use **Earthquake**.
2. **Expected behavior:**
   - The active opponent Pokémon receives 60 damage.
   - **Both of your benched Pokémon** receive **10 damage** each.
3. Check the HP of your benched Pokémon to confirm it has decreased by exactly 10.

---

## Scenario 7: M Blastoise-EX (xy1-30) / Trevenant (xy1-55) / Ledian (xy1-7) / Stoutland (xy1-110) / Xerneas-EX (xy1-97) - Opponent Bench Damage
**Effects:** Damage opponent's benched Pokémon (Hydro Bombard: 30 to 2 benched, Tree Slam: 20 to 2 benched, Mach Punch: 10 to 1 benched).

### Setup
1. Put **M Blastoise-EX** (xy1-30) or any of the listed Pokémon in the active position with required energies.
2. Opponent must have **at least 2 Pokémon** on their bench.

### Execution & Verification Steps (using M Blastoise-EX)
1. Use **Hydro Bombard**.
2. **Expected behavior:**
   - The active opponent Pokémon receives 120 damage.
   - **Exactly 2 of the opponent's benched Pokémon** receive **30 damage** each (chosen randomly by the backend).
3. If opponent has only 1 benched Pokémon, that single Pokémon receives 30 damage.
4. Verify that their HP values are reduced correctly.
