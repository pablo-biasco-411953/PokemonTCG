import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class SobreService {
  constructor(private http: HttpClient) {}

  abrirSobre(username: string): Observable<any[]> {
    return this.http.post<any[]>(`http://localhost:8080/api/sobres/abrir/${username}`, {});
  }
}
