import { HttpClient } from '@angular/common/http';
import { Injectable, computed, signal } from '@angular/core';
import { Observable, tap } from 'rxjs';
import { environment } from '../../../environments/environment';
import { LoginRequest, SesionResponse } from '../models/models';

const STORAGE_KEY = 'rf-panel-sesion';

interface SesionGuardada {
  token: string;
  codigo: string;
  nombre: string;
  rol: 'PANEL';
}

/**
 * El panel tiene su propio login (tabla `panel_admins`, ver migración V5 del
 * backend): `POST api/panel/auth/login` devuelve un token con rol `PANEL`.
 * Las cuentas de operario de la app no existen en esa tabla, así que no
 * pueden entrar acá; el chequeo de rol es una segunda barrera del lado cliente.
 */
@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly baseUrl = environment.apiBaseUrl;

  private readonly _sesion = signal<SesionGuardada | null>(this.restaurar());

  readonly sesion = this._sesion.asReadonly();
  readonly estaLogueado = computed(() => this._sesion() !== null);
  readonly nombreOperario = computed(() => this._sesion()?.nombre ?? '');
  readonly codigoOperario = computed(() => this._sesion()?.codigo ?? '');

  constructor(private http: HttpClient) {}

  get token(): string | null {
    return this._sesion()?.token ?? null;
  }

  /** @throws Error con mensaje presentable si las credenciales fallan o el token no es de panel. */
  login(codigo: string, pin: string): Observable<SesionResponse> {
    const body: LoginRequest = { codigo: codigo.trim(), pin: pin.trim() };
    return this.http.post<SesionResponse>(`${this.baseUrl}api/panel/auth/login`, body).pipe(
      tap((res) => {
        if (res.rol !== 'PANEL') {
          throw new Error('Esta cuenta no es de administrador del panel.');
        }
        if (!res.token) {
          throw new Error('El servidor no devolvió un token de sesión.');
        }
        this.guardar({ token: res.token, codigo: res.codigo, nombre: res.nombre, rol: 'PANEL' });
      }),
    );
  }

  logout(): void {
    this._sesion.set(null);
    localStorage.removeItem(STORAGE_KEY);
  }

  private guardar(sesion: SesionGuardada): void {
    this._sesion.set(sesion);
    localStorage.setItem(STORAGE_KEY, JSON.stringify(sesion));
  }

  private restaurar(): SesionGuardada | null {
    try {
      const crudo = localStorage.getItem(STORAGE_KEY);
      if (!crudo) return null;
      const sesion = JSON.parse(crudo) as SesionGuardada;
      return sesion.token && sesion.rol === 'PANEL' ? sesion : null;
    } catch {
      return null;
    }
  }
}
