import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Card } from '../../../shared/models/card';
import { Partida } from '../../../shared/models/battle';
import { getBackendUrl } from '../../../core/services/api-config';

@Injectable({ providedIn: 'root' })
export class BattleService {
  // Punto de entrada para todas las acciones de combate.
  private base = `${getBackendUrl()}/api/battle`;

  constructor(private http: HttpClient) {}

  private getHeaders(): { headers: HttpHeaders } {
    let username = '';
    try {
      const data = localStorage.getItem('jugador');
      if (data) {
        username = JSON.parse(data).username || '';
      }
    } catch (e) {
      console.error('Error reading username from localStorage', e);
    }
    return {
      headers: new HttpHeaders({
        'X-Username': username
      })
    };
  }

  // Inicia una partida nueva con el mazo elegido.
  startBattle(username: string, mazoId: number): Observable<Partida> {
    return this.http.post<Partida>(`${this.base}/start/${username}`, { mazoId }, this.getHeaders());
  }

  // Inicia una partida online de dos jugadores.
  startBattleOnline(player1: string, player1MazoId: number, player2: string, player2MazoId: number): Observable<Partida> {
    return this.http.post<Partida>(`${this.base}/start-online`, {
      player1,
      player1MazoId,
      player2,
      player2MazoId
    }, this.getHeaders());
  }

  // Trae el estado completo de la partida.
  getState(matchId: string): Observable<Partida> {
    return this.http.get<Partida>(`${this.base}/state/${matchId}`, this.getHeaders());
  }

  heartbeat(matchId: string): Observable<Partida> {
    return this.http.post<Partida>(`${this.base}/${matchId}/heartbeat`, {}, this.getHeaders());
  }

  // Ejecuta el lanzamiento inicial de moneda.
  lanzarMoneda(matchId: string, eleccion: 'CARA' | 'CRUZ'): Observable<Partida> {
    return this.http.post<Partida>(`${this.base}/${matchId}/coin-flip`, { eleccion }, this.getHeaders());
  }

  // Sincroniza el saludo previo al lanzamiento de moneda.
  actualizarHandshakeMoneda(matchId: string, holding: boolean, power: number): Observable<Partida> {
    return this.http.post<Partida>(
      `${this.base}/${matchId}/coin-handshake`,
      { holding, power },
      this.getHeaders()
    );
  }

  // Define quien toma el primer turno.
  elegirTurno(matchId: string, vaPrimero: boolean): Observable<void> {
    return this.http.post<void>(`${this.base}/${matchId}/choose-turn`, { vaPrimero }, this.getHeaders());
  }

  // Baja un Pokemon desde la mano al tablero.
  jugarPokemon(matchId: string, cartaId: string): Observable<void> {
    return this.http.post<void>(`${this.base}/${matchId}/play-pokemon`, { cartaId }, this.getHeaders());
  }

  // Une una energia al Pokemon indicado.
  unirEnergia(matchId: string, cartaId: string, energiaId: string): Observable<void> {
    return this.http.post<void>(`${this.base}/${matchId}/attach-energy`, {
      cartaId,
      energiaId
    }, this.getHeaders());
  }

  // Ejecuta el ataque elegido por nombre.
  atacar(matchId: string, nombreAtaque: string): Observable<void> {
    const url = `${this.base}/${matchId}/attack?nombreAtaque=${encodeURIComponent(nombreAtaque)}`;
    return this.http.post<void>(url, {}, this.getHeaders());
  }

  // Sube un suplente de la banca al puesto activo.
  subirAActivo(matchId: string, cartaId: string): Observable<void> {
    return this.http.post<void>(`${this.base}/${matchId}/promote`, cartaId, this.getHeaders());
  }

  // Pide al backend que resuelva el turno del bot.
  jugarBot(matchId: string): Observable<Partida> {
    return this.http.post<Partida>(`${this.base}/${matchId}/jugar-bot`, {}, this.getHeaders());
  }

  // Cierra el turno del jugador actual.
  pasarTurno(matchId: string): Observable<void> {
    return this.http.post<void>(`${this.base}/${matchId}/pass-turn`, {}, this.getHeaders());
  }

  // Realiza la retirada del Pokemon activo.
  retirarPokemon(matchId: string, nuevoActivoId: string): Observable<void> {
    return this.http.post<void>(`${this.base}/${matchId}/retreat`, nuevoActivoId, this.getHeaders());
  }

  // Evoluciona una carta del tablero usando una carta de la mano.
  evolucionar(matchId: string, cartaManoId: string, cartaTableroId: string): Observable<void> {
    return this.http.post<void>(`${this.base}/${matchId}/evolve`, { cartaManoId, cartaTableroId }, this.getHeaders());
  }

  // Fuerza una carta en mano para depuracion.
  debugDrawCard(matchId: string, cardId: string): Observable<Partida> {
    return this.http.post<Partida>(`${this.base}/${matchId}/debug/draw`, { cardId }, this.getHeaders());
  }

  // Aplica un estado especial manualmente para pruebas.
  debugForzarEstado(matchId: string, objetivo: string, estado: string): Observable<Partida> {
    return this.http.post<Partida>(`${this.base}/${matchId}/debug/status`, { objetivo, estado }, this.getHeaders());
  }

  // Ajusta los HP de una carta durante pruebas.
  debugSetHp(matchId: string, objetivo: string, hp: number): Observable<Partida> {
    return this.http.post<Partida>(`${this.base}/${matchId}/debug/hp`, { objetivo, hp }, this.getHeaders());
  }

  // Devuelve el catalogo completo para el panel debug.
  getCardCatalogDebug(): Observable<Card[]> {
    return this.http.get<Card[]>(`${this.base}/debug/catalog`, this.getHeaders());
  }
}
