import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Mazo } from '../model/mazo';

@Injectable({
  providedIn: 'root'
})
export class MazoService {
  private apiUrl = 'http://localhost:8080/api/mazos';

  constructor(private http: HttpClient) { }

  // Asegurate que este nombre sea EXACTO:
  getMazosByJugador(username: string): Observable<Mazo[]> {
    return this.http.get<Mazo[]>(`${this.apiUrl}/listar/${username}`);
  }


actualizarMazo(idMazo: number, nombre: string, cartasIds: string[]): Observable<any> {
  const body = {
    id: idMazo,
    nombre: nombre,
    cartasIds: cartasIds
  };
  
  // Usamos PUT para actualizaciones completas del recurso
  return this.http.put(`${this.apiUrl}/actualizar/${idMazo}`, body);
}

  guardarMazo(nombre: string, username: string, cartas: string[]): Observable<any> {
    const body = { nombre, username, cartas };
    return this.http.post(`${this.apiUrl}/guardar`, body);
  }
}