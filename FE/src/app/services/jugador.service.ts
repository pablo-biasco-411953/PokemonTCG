import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Card } from '../model/card';
import { JugadorDatosResponse } from '../model/jugador';

@Injectable({
  providedIn: 'root'
})
export class JugadorService {
  private apiUrl = 'http://localhost:8080/api/jugadores';

  constructor(private http: HttpClient) { }

  getJugador(username: string): Observable<JugadorDatosResponse> {
    return this.http.get<JugadorDatosResponse>(`${this.apiUrl}/${username}/datos`, { withCredentials: true });
  }

  getColeccion(username: string): Observable<Card[]> {
    return this.http.get<Card[]>(this.apiUrl + '/' + username + '/coleccion');
  }
}
