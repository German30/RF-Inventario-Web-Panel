import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import { AuthService } from '../services/auth.service';
import { ApiError, ConflictoEdicionError, ConflictoEdicionResponse, ErrorResponse } from '../models/models';

/**
 * Agrega `Authorization: Bearer <token>` a cada pedido con sesión, traduce
 * errores HTTP a [ApiError]/[ConflictoEdicionError] con mensaje del backend,
 * y ante un 401 fuera del login limpia la sesión y vuelve a /login.
 */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthService);
  const router = inject(Router);

  const token = auth.token;
  const esLogin = req.url.endsWith('/auth/login');
  const conToken = token && !esLogin
    ? req.clone({ setHeaders: { Authorization: `Bearer ${token}` } })
    : req;

  return next(conToken).pipe(
    catchError((error: unknown) => {
      if (error instanceof HttpErrorResponse) {
        if (error.status === 401 && !esLogin) {
          auth.logout();
          router.navigate(['/login']);
        }

        if (error.status === 409 && esConflictoEdicion(error.error)) {
          return throwError(() => new ConflictoEdicionError(error.error.message, error.error.actual));
        }

        const cuerpo = error.error as ErrorResponse | undefined;
        const mensaje = cuerpo?.message ?? `Error del servidor (${error.status}).`;
        return throwError(() => new ApiError(mensaje, error.status, cuerpo?.campos ?? null));
      }
      return throwError(() => error);
    }),
  );
};

function esConflictoEdicion(cuerpo: unknown): cuerpo is ConflictoEdicionResponse {
  return !!cuerpo && typeof cuerpo === 'object' && 'actual' in cuerpo && 'message' in cuerpo;
}
