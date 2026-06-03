import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Card } from '../../shared/models/card';

@Injectable({ providedIn: 'root' })
export class CardService {
  private apiUrl = `http://${window.location.hostname}:8080/api/cards`;

  constructor(private http: HttpClient) {}

  // Devuelve el catalogo completo de cartas.
  getAll(): Observable<Card[]> {
    return this.http.get<Card[]>(this.apiUrl);
  }
}
