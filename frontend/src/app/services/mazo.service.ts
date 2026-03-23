import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Mazo } from '../model/mazo';

@Injectable({ providedIn: 'root' })
export class MazoService {
  constructor(private http: HttpClient) {}

  guardarMazo(nombre: string, username: string, cartas: string[]): Observable<Mazo> {
    return this.http.post<Mazo>(`http://localhost:8080/api/mazos/guardar`, {
      nombre,
      username,
      cartas
    });
  }

  listarMazos(username: string): Observable<Mazo[]> {
    return this.http.get<Mazo[]>(`http://localhost:8080/api/mazos/listar/${username}`);
  }
}
