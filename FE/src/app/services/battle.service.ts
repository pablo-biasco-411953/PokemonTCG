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

  jugarPokemon(matchId: string, cartaId: string, posicion: number): Observable<void> {
    return this.http.post<void>(`${this.base}/${matchId}/play-pokemon`, { cartaId, posicion });
  }

 unirEnergia(matchId: string, cartaId: string, energiaId: string): Observable<void> {
return this.http.post<void>(`${this.base}/${matchId}/attach-energy`, {
      cartaId: cartaId,
    energiaId: energiaId
  });
}

  // FIX: ahora apunta al endpoint /attack que existía solo en el frontend pero no en el backend
  atacar(matchId: string): Observable<void> {
    return this.http.post<void>(`${this.base}/${matchId}/attack`, {});
  }

  pasarTurno(matchId: string): Observable<void> {
    return this.http.post<void>(`${this.base}/${matchId}/pass-turn`, {});
  }

  
}