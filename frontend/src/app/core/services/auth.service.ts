import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Jugador } from '../../shared/models/jugador';

import { getBackendUrl } from './api-config';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly apiUrl = `${getBackendUrl()}/api/auth`;

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
