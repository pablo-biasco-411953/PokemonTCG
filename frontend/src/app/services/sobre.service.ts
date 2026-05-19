import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Card } from '../model/card';

@Injectable({ providedIn: 'root' })
export class SobreService {
  constructor(private http: HttpClient) {}

  abrirSobre(username: string): Observable<Card[]> {
    return this.http.post<Card[]>(`http://localhost:8080/api/sobres/abrir/${username}`, {});
  }
}
