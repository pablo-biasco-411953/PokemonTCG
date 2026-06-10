import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Mazo } from '../../../shared/models/mazo';

import { getBackendUrl } from '../../../core/services/api-config';

@Injectable({
  providedIn: 'root'
})
export class MazoService {
  // Servicio para crear, listar y actualizar mazos.
  private apiUrl = `${getBackendUrl()}/api/mazos`;

  constructor(private http: HttpClient) { }

  // Lista los mazos del jugador activo.
  getMazosByJugador(username: string): Observable<Mazo[]> {
    return this.http.get<Mazo[]>(`${this.apiUrl}/listar/${username}`);
  }

  // Actualiza un mazo ya existente.
  actualizarMazo(idMazo: number, nombre: string, cartasIds: string[]): Observable<Mazo> {
    const body = {
      id: idMazo,
      nombre,
      cartasIds
    };

    return this.http.put<Mazo>(`${this.apiUrl}/actualizar/${idMazo}`, body);
  }

  // Elimina un mazo guardado.
  eliminarMazo(idMazo: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/eliminar/${idMazo}`);
  }

  // Guarda un mazo nuevo.
  guardarMazo(nombre: string, username: string, cartas: string[]): Observable<Mazo> {
    const body = { nombre, username, cartas };
    return this.http.post<Mazo>(`${this.apiUrl}/guardar`, body);
  }

  // Inyecta una carta en un mazo de pruebas, reemplazando otra si hace falta.
  debugInjectCard(idMazo: number, cartaId: string, cartaAReemplazarId?: string | null): Observable<Mazo> {
    return this.http.post<Mazo>(`${this.apiUrl}/${idMazo}/debug/inject-card`, {
      cartaId,
      cartaAReemplazarId: cartaAReemplazarId ?? null
    });
  }
}
