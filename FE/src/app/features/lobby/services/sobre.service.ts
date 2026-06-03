import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Card } from '../../../shared/models/card';

@Injectable({ providedIn: 'root' })
export class SobreService {
  constructor(private http: HttpClient) {}

  // Abre un sobre y devuelve las cartas obtenidas.
  abrirSobre(username: string): Observable<Card[]> {
    return this.http.post<Card[]>(`http://${window.location.hostname}:8080/api/sobres/abrir/${username}`, {});
  }
}
