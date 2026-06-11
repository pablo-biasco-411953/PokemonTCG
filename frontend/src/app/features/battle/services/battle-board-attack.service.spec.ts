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
      tipoEfecto: 'damage',
    });
  });

  it('describe Dig como proteccion y no como dano extra', () => {
    const result = service.detectarCoinFlipAtaque(
      {
        texto: "Flip a coin. If heads, prevent all effects of attacks, including damage, done to this Pokémon during your opponent's next turn.",
        danio: 10,
      },
      () => 'proteccion',
    );

    expect(result?.danioExtraPorCara).toBe(0);
    expect(result?.esSoloEstado).toBe(true);
    expect(result?.tipoEfecto).toBe('protection');
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

  it('reserva la energia Metal para Metal y usa otra para Colorless', () => {
    const ataque = { costo: ['Colorless', 'Metal'] };
    const activo = {
      energiasUnidas: [
        { nombre: 'Metal Energy', tipo: 'Energy' },
        { nombre: 'Water Energy', tipo: 'Energy' },
      ],
    };

    expect(service.validarEnergiaAtaque(ataque, activo)).toBe(true);
    expect(service.getFaltantesAtaque(ataque, activo)).toEqual([]);
  });

  it('cuenta Double Colorless como dos energias incoloras', () => {
    const ataque = { costo: ['Colorless', 'Colorless'] };
    const activo = {
      energiasUnidas: [{ nombre: 'Double Colorless Energy', tipo: '' }],
    };

    expect(service.validarEnergiaAtaque(ataque, activo)).toBe(true);
  });

  it('permite que Rainbow Energy cubra un requisito especifico', () => {
    const ataque = { costo: ['Metal'] };
    const activo = {
      energiasUnidas: [{ nombre: 'Rainbow Energy', tipo: '' }],
    };

    expect(service.validarEnergiaAtaque(ataque, activo)).toBe(true);
  });
});
