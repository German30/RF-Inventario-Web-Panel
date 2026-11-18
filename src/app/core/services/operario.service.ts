import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  ActualizarOperarioRequest,
  OperarioRequest,
  OperarioResponse,
  ResetPinRequest,
} from '../models/models';

@Injectable({ providedIn: 'root' })
export class OperarioService {
  private readonly base = `${environment.apiBaseUrl}api/operarios`;

  constructor(private http: HttpClient) {}

  listar(): Observable<OperarioResponse[]> {
    return this.http.get<OperarioResponse[]>(this.base);
  }

  crear(req: OperarioRequest): Observable<OperarioResponse> {
    return this.http.post<OperarioResponse>(this.base, req);
  }

  actualizar(codigo: string, req: ActualizarOperarioRequest): Observable<OperarioResponse> {
    return this.http.put<OperarioResponse>(`${this.base}/${codigo}`, req);
  }

  resetearPin(codigo: string, pin: string): Observable<OperarioResponse> {
    const body: ResetPinRequest = { pin };
    return this.http.patch<OperarioResponse>(`${this.base}/${codigo}/pin`, body);
  }
}
