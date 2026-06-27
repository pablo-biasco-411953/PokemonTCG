# Reporte de Cobertura de Código (JaCoCo) — Backend

- **Fecha/Hora de Registro**: 2026-06-26 20:10 (Local)
- **Origen**: Ejecución completa del suite de pruebas (765 tests ejecutados exitosamente, 0 errores).
- **Estado de Cumplimiento General RNF-03**: ❌ *No Cumple* (69.83% vs >= 80% requerido).

---

## 📊 1. Cobertura de Código Global

| Componente | Requisito RNF-03 | Estado Actual (JaCoCo) | Estado de Cumplimiento |
| :--- | :---: | :---: | :---: |
| **Total del Proyecto (Backend)** | **≥ 80.00%** | **69.83%** (21,909 / 31,373 instrucciones) | ❌ *No Cumple* |

> [!IMPORTANT]
> El proyecto general se encuentra a un **10.17%** de alcanzar la cobertura mínima requerida del **80%** (aumento del **+0.98%** respecto al reporte anterior).
> La mayor parte del código sin cubrir se concentra en orquestadores masivos de flujo como `BattleEngineService.java` y ciertos comandos interactivos de cartas.

---

## 🎯 2. Cobertura de Componentes Críticos (Lógica del Juego)

El pliego RNF-03 exige una **cobertura > 90%** en tres componentes críticos de la lógica de juego. Dado que dichos componentes fueron mapeados a nombres de clases distintos en la implementación real, a continuación se desglosa el estado actual para cada uno de estos roles:

### A. Cálculo de Daño (Equivalente a `DamageCalculator`)
*   **`BattleAttackService`** (Orquestador principal de ataques): **73.32%** (349 / 476 instrucciones) ❌ *No Cumple*
*   **`EnergyCostCalculator`** (Validación de costos de energía): **87.20%** (286 / 328 instrucciones) ❌ *No Cumple*
*   **Eslabones de Pre-Daño (Chain of Responsibility):**
    *   `EfectoMonedaFalla`: **100.00%** (44/44)  ✅ *Cumple*
    *   `EfectoMultiMoneda`: **78.02%** (71/91) ❌ *No Cumple*
    *   `EfectoMonedaExtraDanio`: **90.20%** (46/51) ✅ *Cumple*
    *   `EfectoEscalaPorEnergias`: **92.06%** (58/63) ✅ *Cumple*
    *   `EfectoContadoresDanio`: **96.61%** (57/59) ✅ *Cumple*
    *   `EfectoInmunidad`: **100.00%** (59/59) ✅ *Cumple*

### B. Manejo de Efectos de Estado (Equivalente a `StatusEffectManager`)
*   **Eslabones de Condiciones Especiales (Chain of Responsibility):**
    *   `EfectoVeneno`: **100.00%** (21/21) ✅ *Cumple*
    *   `EfectoQuemadura`: **100.00%** (21/21) ✅ *Cumple*
    *   `EfectoSueno`: **100.00%** (50/50) ✅ *Cumple*
    *   `EfectoConfusion`: **100.00%** (50/50) ✅ *Cumple*
    *   `EfectoParalisis`: **100.00%** (54/54) ✅ *Cumple*
    *   `EfectoAtrapar`: **100.00%** (27/27) ✅ *Cumple*
    *   *Nota: Todos los componentes de efectos de estado individuales alcanzan el **100%** de cobertura.*

### C. Validación de Reglas (Equivalente a `RuleValidator`)
*   **`BattleEngineService`** (Validación principal de flujos de turnos): **67.90%** (4,728 / 6,963 instrucciones) ❌ *No Cumple*
*   **Comandos de Acciones del Jugador (`puedeEjecutar()`):**
    *   `ComandoJugarPokemon`: **94.57%** (87/92) ✅ *Cumple*
    *   `ComandoEvolucionar`: **86.59%** (142/164) ❌ *No Cumple*
    *   `ComandoSubirActivo`: **100.00%** (74/74) ✅ *Cumple* (Aumentado vía unit tests)
    *   `ComandoUnirEnergia`: **87.34%** (69/79) ❌ *No Cumple*
    *   `ComandoRetirarse`: **65.25%** (154/236) ❌ *No Cumple*
    *   `ComandoJugarTrainer`: **100.00%** (210/210) ✅ *Cumple* (Aumentado vía unit tests)
    *   `ComandoAtacar`: **98.57%** (277/281) ✅ *Cumple* (Aumentado vía unit tests)

---

## ⛓️ 3. Pruebas de Integración y Tests E2E (Frontend)

El pliego RNF-03 también define requerimientos para pruebas funcionales de flujo completo:

### A. Tests de Integración del Backend
*   **Requisitos:** Cubrir casos de uso principales: *partida completa, mulligan múltiple, evolución, knockout y victoria*.
*   **Estado de Cumplimiento:** ⚠️ *Parcialmente Cumplido*.
    *   **Mulligan**: Testeado a nivel de servicio unitario en `BattleEngineServiceSetupTest` y `BattleEngineServiceMoreTest`. Falta un test de integración que reproduzca específicamente el ciclo completo de mulligan múltiple.
    *   **Evolución**: Testeado unitariamente en `BattleEngineServiceEvolveSetupTest`.
    *   **Knockout**: Cubierto en `BattleKoServiceTest`.
    *   **Victoria**: Evaluado ante la imposibilidad de robar en `BattleEngineServiceTest.ejecutarTurnoBotHacePerderAlJugadorSiNoPuedeRobar()`.
    *   **Partida Completa**: ❌ **Pendiente**. No existe actualmente un test de integración de punta a punta que simule el flujo completo de una partida de principio a fin.

### B. Tests E2E del Frontend
*   **Requisitos:** Implementar al menos tests E2E básicos en el frontend cubriendo el flujo completo: *crear mazo, unirse a partida y ejecutar un turno*.
*   **Estado de Cumplimiento:** ❌ *No Cumple*.
    *   No se encuentran configuradas herramientas de testeo End-to-End (como Cypress, Playwright o similar) en el frontend (`FE/package.json`).
    *   Solo se cuenta con pruebas unitarias básicas utilizando Vitest en componentes y servicios aislados.

---

## 📈 4. Plan de Acción Recomendado (Deuda Técnica de Cobertura)

Para elevar la cobertura total a **≥ 80%** y garantizar que los módulos críticos superen el **90%**, es necesario priorizar los siguientes puntos:

1.  **Testear el `ComandoAtacar`:**
    *   Actualmente posee **0%** de cobertura. Crear una clase `ComandoAtacarTest` que valide las restricciones de ataque (saber si puede atacar bajo parálisis/sueño, verificar que tenga energías correctas, etc.) sumará un gran porcentaje de cobertura.
2.  **Aumentar la cobertura sobre `BattleEngineService`:**
    *   Es el componente más grande del sistema (más de 6,900 instrucciones bytecode). Incrementar las pruebas sobre los flujos de setup, mulligans y resolución de efectos interactivos aumentará drásticamente el porcentaje de cobertura global del proyecto.
3.  **Reforzar tests de integración de comandos:**
    *   Agregar pruebas para los flujos de `ComandoRetirarse` y `ComandoJugarTrainer` orientados a las ramas de error y validaciones (por ejemplo, intentar retirarse sin tener suficiente energía o jugar un entrenador no permitido).
4.  **Implementar caso de uso de Partida Completa**:
    *   Crear un test de integración en el backend que inicialice una partida real entre dos jugadores y simule las transiciones de turnos y jugadas básicas hasta llegar al KO y la victoria.
5.  **Configurar e implementar tests E2E en Frontend**:
    *   Configurar un framework como Playwright o Cypress en `FE/` y programar el flujo básico (crear mazo, buscar sala y jugar primer turno) para cumplir con el RNF-03 de frontend.
