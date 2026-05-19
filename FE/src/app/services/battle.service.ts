import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Card } from '../model/card';
import { Partida } from '../model/battle';

@Injectable({ providedIn: 'root' })
export class BattleService {
  private base = 'http://localhost:8080/api/battle';

  constructor(private http: HttpClient) {}

  startBattle(username: string, mazoId: number): Observable<Partida> {
    return this.http.post<Partida>(`${this.base}/start/${username}`, { mazoId });
  }

  getState(matchId: string): Observable<Partida> {
    return this.http.get<Partida>(`${this.base}/state/${matchId}`);
  }

  lanzarMoneda(matchId: string): Observable<boolean> {
    return this.http.post<boolean>(`${this.base}/${matchId}/coin-flip`, {});
  }

  elegirTurno(matchId: string, vaPrimero: boolean): Observable<void> {
    return this.http.post<void>(`${this.base}/${matchId}/choose-turn`, { vaPrimero });
  }

  jugarPokemon(matchId: string, cartaId: string): Observable<void> {
    return this.http.post<void>(`${this.base}/${matchId}/play-pokemon`, { cartaId });
  }

  unirEnergia(matchId: string, cartaId: string, energiaId: string): Observable<void> {
    return this.http.post<void>(`${this.base}/${matchId}/attach-energy`, {
      cartaId,
      energiaId
    });
  }

  atacar(matchId: string, nombreAtaque: string): Observable<void> {
    const url = `${this.base}/${matchId}/attack?nombreAtaque=${encodeURIComponent(nombreAtaque)}`;
    return this.http.post<void>(url, {});
  }

  subirAActivo(matchId: string, cartaId: string): Observable<void> {
    return this.http.post<void>(`${this.base}/${matchId}/promote`, cartaId);
  }

  jugarBot(matchId: string): Observable<Partida> {
    return this.http.post<Partida>(`${this.base}/${matchId}/jugar-bot`, {});
  }

  pasarTurno(matchId: string): Observable<void> {
    return this.http.post<void>(`${this.base}/${matchId}/pass-turn`, {});
  }

  retirarPokemon(matchId: string, nuevoActivoId: string): Observable<void> {
    return this.http.post<void>(`${this.base}/${matchId}/retreat`, nuevoActivoId);
  }

  evolucionar(matchId: string, cartaManoId: string, cartaTableroId: string): Observable<void> {
    return this.http.post<void>(`${this.base}/${matchId}/evolve`, { cartaManoId, cartaTableroId });
  }

  debugDrawCard(matchId: string, cardId: string): Observable<Partida> {
    return this.http.post<Partida>(`${this.base}/${matchId}/debug/draw`, { cardId });
  }

  debugForzarEstado(matchId: string, objetivo: string, estado: string): Observable<Partida> {
    return this.http.post<Partida>(`${this.base}/${matchId}/debug/status`, { objetivo, estado });
  }

  debugSetHp(matchId: string, objetivo: string, hp: number): Observable<Partida> {
    return this.http.post<Partida>(`${this.base}/${matchId}/debug/hp`, { objetivo, hp });
  }

  getCardCatalogDebug(): Observable<Card[]> {
    return this.http.get<Card[]>(`${this.base}/debug/catalog`);
  }
}
