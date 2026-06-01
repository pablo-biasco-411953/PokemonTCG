import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Jugador } from '../model/jugador';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly apiUrl = `http://${window.location.hostname}:8080/api/auth`;

  constructor(private http: HttpClient) {}

  login(username: string, password: string): Observable<Jugador> {
    return this.http.post<Jugador>(`${this.apiUrl}/login`, { username, password });
  }

  register(screenName: string, email: string, password: string, confirmPassword: string): Observable<Jugador> {
    return this.http.post<Jugador>(`${this.apiUrl}/register`, { screenName, email, password, confirmPassword });
  }

  forgotPassword(username: string, email: string): Observable<string> {
    return this.http.post(`${this.apiUrl}/forgot-password`, { username, email }, { responseType: 'text' });
  }

  resetPassword(token: string, password: string, confirmPassword: string): Observable<string> {
    return this.http.post(`${this.apiUrl}/reset-password`, { token, password, confirmPassword }, { responseType: 'text' });
  }
}
