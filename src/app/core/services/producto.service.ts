import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  AjusteStockRequest,
  MovimientoStockResponse,
  ProductoRequest,
  ProductoResponse,
} from '../models/models';

@Injectable({ providedIn: 'root' })
export class ProductoService {
  private readonly base = `${environment.apiBaseUrl}api/productos`;

  constructor(private http: HttpClient) {}

  listar(busqueda = ''): Observable<ProductoResponse[]> {
    return this.http.get<ProductoResponse[]>(this.base, { params: { q: busqueda.trim() } });
  }

  obtener(id: number): Observable<ProductoResponse> {
    return this.http.get<ProductoResponse>(`${this.base}/${id}`);
  }

  movimientos(id: number): Observable<MovimientoStockResponse[]> {
    return this.http.get<MovimientoStockResponse[]>(`${this.base}/${id}/movimientos`);
  }

  crear(req: ProductoRequest): Observable<ProductoResponse> {
    return this.http.post<ProductoResponse>(this.base, req);
  }

  actualizar(id: number, req: ProductoRequest): Observable<ProductoResponse> {
    return this.http.put<ProductoResponse>(`${this.base}/${id}`, req);
  }

  ajustarStock(id: number, delta: number): Observable<ProductoResponse> {
    const body: AjusteStockRequest = { delta };
    return this.http.patch<ProductoResponse>(`${this.base}/${id}/stock`, body);
  }

  eliminar(id: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/${id}`);
  }
}
