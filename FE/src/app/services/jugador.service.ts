import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Card } from '../model/card';
import { Jugador } from '../model/jugador';

@Injectable({
  providedIn: 'root'
})
export class JugadorService {
  private apiUrl = 'http://localhost:8080/api/jugadores';

  constructor(private http: HttpClient) { }


getJugador(username: string): Observable<any> {
  return this.http.get<any>(`${this.apiUrl}/${username}/datos`, { withCredentials: true });
}

  getColeccion(username: string): Observable<Card[]> {
    return this.http.get<Card[]>(this.apiUrl + '/' + username + '/coleccion');
  }
}
