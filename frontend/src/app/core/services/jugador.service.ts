import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Card } from '../../shared/models/card';
import { JugadorDatosResponse } from '../../shared/models/jugador';

@Injectable({
  providedIn: 'root'
})
export class JugadorService {
  // Servicio para consultar resumen y coleccion del jugador.
  private apiUrl = `http://${window.location.hostname}:8080/api/jugadores`;

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
}
