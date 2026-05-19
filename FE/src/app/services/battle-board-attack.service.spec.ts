import { describe, expect, it } from 'vitest';

import { BattleBoardAttackService } from './battle-board-attack.service';

// Pruebas puntuales para validaciones y coin flip de ataques.
describe('BattleBoardAttackService', () => {
  const service = new BattleBoardAttackService();

  it('detecta ataques con fallo por cruz y conserva la descripcion traducida', () => {
    const traduccion = 'Si sale cara, hace dano. Si sale cruz, no hace nada.';

    const result = service.detectarCoinFlipAtaque(
      { texto: 'Flip a coin. If tails, this attack does nothing.', danio: 30 },
      () => traduccion
    );

    expect(result).toEqual({
      cantidadMonedas: 1,
      danioBase: 0,
      danioExtraPorCara: 30,
      descripcion: traduccion,
      esSoloEstado: false,
    });
  });

  it('valida costos mixtos de energia incluyendo colorless', () => {
    const ataque = { costo: ['Fire', 'Colorless'] };
    const activo = {
      energiasUnidas: [
        { nombre: 'Fire Energy', tipo: 'Energy' },
        { nombre: 'Water Energy', tipo: 'Energy' },
      ],
    };

    expect(service.validarEnergiaAtaque(ataque, activo)).toBe(true);
  });

  it('marca energias cumplidas y faltantes en orden', () => {
    const ataque = { costo: ['Fire', 'Water', 'Colorless'] };
    const activo = {
      energiasUnidas: [
        { tipo: 'Fire' },
        { tipo: 'Lightning' },
      ],
    };

    expect(service.getCheckEnergiasAtaque(ataque, activo)).toEqual([
      { tipo: 'Fire', cumplido: true },
      { tipo: 'Water', cumplido: false },
      { tipo: 'Colorless', cumplido: true },
    ]);

    expect(service.getFaltantesAtaque(ataque, activo)).toEqual([
      { tipo: 'Water', cantidad: 1 },
    ]);
  });
});
