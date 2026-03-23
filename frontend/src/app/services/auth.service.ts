import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Jugador } from '../model/jugador';

@Injectable({ providedIn: 'root' })
export class AuthService {
  constructor(private http: HttpClient) {}

  login(username: string): Observable<Jugador> {
    return this.http.post<Jugador>(`http://localhost:8080/api/auth/login`, { username });
  }
}
