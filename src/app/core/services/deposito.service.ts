import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  ConteoResponse,
  DashboardResponse,
  ExistenciaResponse,
  PedidoRequest,
  PedidoResponse,
  TipoPedido,
  UbicacionRequest,
  UbicacionResponse,
} from '../models/models';

/** Ubicaciones, pedidos, conteos, dashboard y reportes (lo que agregó la V6 del backend). */
@Injectable({ providedIn: 'root' })
export class DepositoService {
  private readonly base = `${environment.apiBaseUrl}api`;

  constructor(private http: HttpClient) {}

  // ---- Dashboard y reportes
  dashboard(): Observable<DashboardResponse> {
    return this.http.get<DashboardResponse>(`${this.base}/dashboard`);
  }

  /** Descarga un CSV con el token (un <a href> directo no mandaría el Authorization). */
  descargarCsv(reporte: 'inventario' | 'existencias' | 'movimientos'): void {
    this.http.get(`${this.base}/reportes/${reporte}.csv`, { responseType: 'blob' }).subscribe((blob) => {
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `${reporte}-${new Date().toISOString().slice(0, 10)}.csv`;
      a.click();
      URL.revokeObjectURL(url);
    });
  }

  // ---- Ubicaciones
  ubicaciones(): Observable<UbicacionResponse[]> {
    return this.http.get<UbicacionResponse[]>(`${this.base}/ubicaciones`);
  }

  contenido(codigo: string): Observable<ExistenciaResponse[]> {
    return this.http.get<ExistenciaResponse[]>(`${this.base}/ubicaciones/${encodeURIComponent(codigo)}/contenido`);
  }

  crearUbicacion(req: UbicacionRequest): Observable<UbicacionResponse> {
    return this.http.post<UbicacionResponse>(`${this.base}/ubicaciones`, req);
  }

  actualizarUbicacion(codigo: string, req: UbicacionRequest): Observable<UbicacionResponse> {
    return this.http.put<UbicacionResponse>(`${this.base}/ubicaciones/${encodeURIComponent(codigo)}`, req);
  }

  // ---- Pedidos
  pedidos(tipo: TipoPedido | null, abiertos: boolean): Observable<PedidoResponse[]> {
    const params: Record<string, string> = { abiertos: String(abiertos) };
    if (tipo) params['tipo'] = tipo;
    return this.http.get<PedidoResponse[]>(`${this.base}/pedidos`, { params });
  }

  pedido(id: number): Observable<PedidoResponse> {
    return this.http.get<PedidoResponse>(`${this.base}/pedidos/${id}`);
  }

  crearPedido(req: PedidoRequest): Observable<PedidoResponse> {
    return this.http.post<PedidoResponse>(`${this.base}/pedidos`, req);
  }

  accionPedido(id: number, accion: 'completar' | 'cancelar'): Observable<PedidoResponse> {
    return this.http.post<PedidoResponse>(`${this.base}/pedidos/${id}/${accion}`, {});
  }

  // ---- Conteos
  conteos(abiertos: boolean): Observable<ConteoResponse[]> {
    return this.http.get<ConteoResponse[]>(`${this.base}/conteos`, { params: { abiertos: String(abiertos) } });
  }

  conteo(id: number): Observable<ConteoResponse> {
    return this.http.get<ConteoResponse>(`${this.base}/conteos/${id}`);
  }

  abrirConteo(ubicacion: string, ciego: boolean, nota: string): Observable<ConteoResponse> {
    return this.http.post<ConteoResponse>(`${this.base}/conteos`, { ubicacion, ciego, nota });
  }

  accionConteo(id: number, accion: 'aplicar' | 'reabrir' | 'cancelar'): Observable<ConteoResponse> {
    return this.http.post<ConteoResponse>(`${this.base}/conteos/${id}/${accion}`, {});
  }
}
