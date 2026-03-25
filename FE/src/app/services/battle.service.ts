import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class BattleService {
  private apiUrl = 'http://localhost:8080/api/battle';

  constructor(private http: HttpClient) {}

  startBattle(username: string, mazoId: number): Observable<any> {
    // Coincide con @PostMapping("/start/{username}")
    return this.http.post<any>(`${this.apiUrl}/start/${username}`, { mazoId });
  }

  getState(matchId: string): Observable<any> {
    // Coincide con @GetMapping("/state/{matchId}")
    return this.http.get<any>(`${this.apiUrl}/state/${matchId}`);
  }

  pasarTurno(matchId: string): Observable<any> {
    return this.http.post<any>(`${this.apiUrl}/${matchId}/pass-turn`, {});
  }

  lanzarMoneda(matchId: string): Observable<boolean> {
    return this.http.post<boolean>(`${this.apiUrl}/${matchId}/coin-flip`, {});
  }
}