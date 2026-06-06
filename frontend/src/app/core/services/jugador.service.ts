import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Card } from '../../shared/models/card';
import { JugadorDatosResponse } from '../../shared/models/jugador';

export interface SantoroQuestResponse {
  giftClaimed: boolean;
  tracking: boolean;
  state: string;
  sobresDisponibles: number;
}

import { getBackendUrl } from './api-config';

@Injectable({
  providedIn: 'root'
})
export class JugadorService {
  // Servicio para consultar resumen y coleccion del jugador.
  private apiUrl = `${getBackendUrl()}/api/jugadores`;

  constructor(private http: HttpClient) { }

  // Obtiene el resumen visible del jugador.
  getJugador(username: string): Observable<JugadorDatosResponse> {
    return this.http.get<JugadorDatosResponse>(`${this.apiUrl}/${username}/datos`, { withCredentials: true });
  }

  // Obtiene todas las cartas de la coleccion.
  getColeccion(username: string): Observable<Card[]> {
    return this.http.get<Card[]>(this.apiUrl + '/' + username + '/coleccion');
  }

  // Ajusta manualmente la cantidad de sobres disponibles para pruebas.
  debugSetSobres(username: string, cantidad: number): Observable<JugadorDatosResponse> {
    return this.http.post<JugadorDatosResponse>(`${this.apiUrl}/${username}/debug/sobres`, { cantidad });
  }

  getSantoroQuest(username: string): Observable<SantoroQuestResponse> {
    return this.http.get<SantoroQuestResponse>(`${this.apiUrl}/${username}/quests/santoro`);
  }

  setSantoroTracking(username: string, tracking: boolean): Observable<SantoroQuestResponse> {
    return this.http.post<SantoroQuestResponse>(`${this.apiUrl}/${username}/quests/santoro/tracking`, { tracking });
  }

  claimSantoroGift(username: string): Observable<SantoroQuestResponse> {
    return this.http.post<SantoroQuestResponse>(`${this.apiUrl}/${username}/quests/santoro/claim`, {});
  }

  // Guarda la personalización de estilo en base de datos.
  rewardCoins(username: string, amount: number): Observable<JugadorDatosResponse> {
    return this.http.post<JugadorDatosResponse>(`${this.apiUrl}/${username}/coins/reward`, { amount });
  }

  spendCoins(username: string, amount: number): Observable<JugadorDatosResponse> {
    return this.http.post<JugadorDatosResponse>(`${this.apiUrl}/${username}/coins/spend`, { amount });
  }

  buyPacks(username: string, amount: number): Observable<JugadorDatosResponse> {
    return this.http.post<JugadorDatosResponse>(`${this.apiUrl}/${username}/packs/buy`, { amount });
  }

  guardarPersonalizacion(username: string, config: any): Observable<void> {
    return this.http.post<void>(`${this.apiUrl}/${username}/personalizacion`, config);
  }

  // Ejecuta la transacción de intercambio de cartas.
  ejecutarTrade(playerA: string, playerB: string, playerACards: string[], playerBCards: string[]): Observable<void> {
    return this.http.post<void>(`${this.apiUrl}/trade/execute`, {
      playerA,
      playerB,
      playerACardIds: playerACards,
      playerBCardIds: playerBCards
    });
  }
}
