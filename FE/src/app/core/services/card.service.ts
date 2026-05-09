import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Card } from '../../shared/models/card';

import { getBackendUrl } from './api-config';

@Injectable({ providedIn: 'root' })
export class CardService {
  private apiUrl = `${getBackendUrl()}/api/cards`;

  constructor(private http: HttpClient) {}

  // Devuelve el catalogo completo de cartas.
  getAll(): Observable<Card[]> {
    return this.http.get<Card[]>(this.apiUrl);
  }
}
