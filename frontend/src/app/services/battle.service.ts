import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class BattleService {
  constructor(private http: HttpClient) {}

  startBattle(username: string, mazoId: number): Observable<any> {
    return this.http.post<any>(`http://localhost:8080/api/battle/start/${username}`, {
      mazoId
    });
  }
}
