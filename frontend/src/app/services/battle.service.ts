import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class BattleService {
  private base = 'http://localhost:8080/api/battle';

  constructor(private http: HttpClient) {}

  startBattle(username: string, mazoId: number): Observable<any> {
    return this.http.post(`${this.base}/start/${username}`, { mazoId });
  }

  getState(matchId: string): Observable<any> {
    return this.http.get(`${this.base}/state/${matchId}`);
  }

  lanzarMoneda(matchId: string): Observable<boolean> {
    return this.http.post<boolean>(`${this.base}/${matchId}/coin-flip`, {});
  }

  elegirTurno(matchId: string, vaPrimero: boolean): Observable<void> {
    return this.http.post<void>(`${this.base}/${matchId}/choose-turn`, { vaPrimero });
  }

  // 🚩 FIX: Quitamos 'posicion' porque el Backend ya no lo usa
  jugarPokemon(matchId: string, cartaId: string): Observable<void> {
    return this.http.post<void>(`${this.base}/${matchId}/play-pokemon`, { cartaId });
  }

  unirEnergia(matchId: string, cartaId: string, energiaId: string): Observable<void> {
    return this.http.post<void>(`${this.base}/${matchId}/attach-energy`, {
      cartaId: cartaId,
      energiaId: energiaId
    });
  }

  atacar(matchId: string, nombreAtaque: string): Observable<void> {
    const url = `${this.base}/${matchId}/attack?nombreAtaque=${encodeURIComponent(nombreAtaque)}`;
    return this.http.post<void>(url, {});
  }

  // 🚩 NUEVO: Para subir un Pokémon de la banca al puesto activo
  subirAActivo(matchId: string, cartaId: string): Observable<void> {
    // Enviamos el cartaId como un string plano en el body
    return this.http.post<void>(`${this.base}/${matchId}/promote`, cartaId);
  }

  jugarBot(matchId: string) {
    return this.http.post(`${this.base}/${matchId}/jugar-bot`, {});
  }

  pasarTurno(matchId: string): Observable<void> {
    return this.http.post<void>(`${this.base}/${matchId}/pass-turn`, {});
  }
  retirarPokemon(matchId: string, nuevoActivoId: string): Observable<void> {
  return this.http.post<void>(`${this.base}/${matchId}/retreat`, nuevoActivoId);
}

evolucionar(matchId: string, cartaManoId: string, cartaTableroId: string) {
    return this.http.post(`${this.base}/${matchId}/evolve`, { cartaManoId, cartaTableroId });
  }
}