import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Card } from '../../../shared/models/card';
import { Partida } from '../../../shared/models/battle';

@Injectable({ providedIn: 'root' })
export class BattleService {
  // Punto de entrada para todas las acciones de combate.
  private base = 'http://localhost:8080/api/battle';

  constructor(private http: HttpClient) {}

  // Inicia una partida nueva con el mazo elegido.
  startBattle(username: string, mazoId: number): Observable<Partida> {
    return this.http.post<Partida>(`${this.base}/start/${username}`, { mazoId });
  }

  // Trae el estado completo de la partida.
  getState(matchId: string): Observable<Partida> {
    return this.http.get<Partida>(`${this.base}/state/${matchId}`);
  }

  // Ejecuta el lanzamiento inicial de moneda.
  lanzarMoneda(matchId: string): Observable<boolean> {
    return this.http.post<boolean>(`${this.base}/${matchId}/coin-flip`, {});
  }

  // Define quien toma el primer turno.
  elegirTurno(matchId: string, vaPrimero: boolean): Observable<void> {
    return this.http.post<void>(`${this.base}/${matchId}/choose-turn`, { vaPrimero });
  }

  // Baja un Pokemon desde la mano al tablero.
  jugarPokemon(matchId: string, cartaId: string): Observable<void> {
    return this.http.post<void>(`${this.base}/${matchId}/play-pokemon`, { cartaId });
  }

  // Une una energia al Pokemon indicado.
  unirEnergia(matchId: string, cartaId: string, energiaId: string): Observable<void> {
    return this.http.post<void>(`${this.base}/${matchId}/attach-energy`, {
      cartaId,
      energiaId
    });
  }

  // Ejecuta el ataque elegido por nombre.
  atacar(matchId: string, nombreAtaque: string): Observable<void> {
    const url = `${this.base}/${matchId}/attack?nombreAtaque=${encodeURIComponent(nombreAtaque)}`;
    return this.http.post<void>(url, {});
  }

  // Sube un suplente de la banca al puesto activo.
  subirAActivo(matchId: string, cartaId: string): Observable<void> {
    return this.http.post<void>(`${this.base}/${matchId}/promote`, cartaId);
  }

  // Pide al backend que resuelva el turno del bot.
  jugarBot(matchId: string): Observable<Partida> {
    return this.http.post<Partida>(`${this.base}/${matchId}/jugar-bot`, {});
  }

  // Cierra el turno del jugador actual.
  pasarTurno(matchId: string): Observable<void> {
    return this.http.post<void>(`${this.base}/${matchId}/pass-turn`, {});
  }

  // Realiza la retirada del Pokemon activo.
  retirarPokemon(matchId: string, nuevoActivoId: string): Observable<void> {
    return this.http.post<void>(`${this.base}/${matchId}/retreat`, nuevoActivoId);
  }

  // Evoluciona una carta del tablero usando una carta de la mano.
  evolucionar(matchId: string, cartaManoId: string, cartaTableroId: string): Observable<void> {
    return this.http.post<void>(`${this.base}/${matchId}/evolve`, { cartaManoId, cartaTableroId });
  }

  // Fuerza una carta en mano para depuracion.
  debugDrawCard(matchId: string, cardId: string): Observable<Partida> {
    return this.http.post<Partida>(`${this.base}/${matchId}/debug/draw`, { cardId });
  }

  // Aplica un estado especial manualmente para pruebas.
  debugForzarEstado(matchId: string, objetivo: string, estado: string): Observable<Partida> {
    return this.http.post<Partida>(`${this.base}/${matchId}/debug/status`, { objetivo, estado });
  }

  // Ajusta los HP de una carta durante pruebas.
  debugSetHp(matchId: string, objetivo: string, hp: number): Observable<Partida> {
    return this.http.post<Partida>(`${this.base}/${matchId}/debug/hp`, { objetivo, hp });
  }

  // Devuelve el catalogo completo para el panel debug.
  getCardCatalogDebug(): Observable<Card[]> {
    return this.http.get<Card[]>(`${this.base}/debug/catalog`);
  }
}
