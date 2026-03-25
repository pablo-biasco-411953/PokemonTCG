import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class BattleService {
  private apiUrl = 'http://localhost:8080/api/battle';

  constructor(private http: HttpClient) { }

  // 1. Iniciar la batalla (POST)
  startBattle(username: string, mazoId: number): Observable<any> {
    return this.http.post(`${this.apiUrl}/start/${username}`, { mazoId });
  }

  // 2. Obtener el estado (GET) - Usando el ID dinámico
  getState(matchId: string): Observable<any> {
    return this.http.get(`${this.apiUrl}/state/${matchId}`);
  }

  // 3. Lanzar moneda (POST)
  lanzarMoneda(matchId: string): Observable<boolean> {
    return this.http.post<boolean>(`${this.apiUrl}/${matchId}/coin-flip`, {});
  }

  // 4. JUGAR POKÉMON (El que te faltaba)
  jugarPokemon(matchId: string, cartaId: string, posicion: number): Observable<void> {
    const body = { cartaId, posicion };
    return this.http.post<void>(`${this.apiUrl}/${matchId}/play-pokemon`, body);
  }

// src/app/services/battle.service.ts

atacar(matchId: string): Observable<void> {
  // El endpoint debe coincidir con el @PostMapping("/{matchId}/attack") de tu Java
  return this.http.post<void>(`${this.apiUrl}/${matchId}/attack`, {});
}

  // 5. PASAR TURNO (POST)
  pasarTurno(matchId: string): Observable<void> {
    return this.http.post<void>(`${this.apiUrl}/${matchId}/pass-turn`, {});
  }

  // 6. UNIR ENERGÍA (Por si lo implementás después)
  unirEnergia(matchId: string, cartaId: string, energiaId: string): Observable<void> {
    const body = { cartaId, energiaId };
    return this.http.post<void>(`${this.apiUrl}/${matchId}/attach-energy`, body);
  }
}