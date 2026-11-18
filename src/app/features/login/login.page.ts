import { Component, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-login-page',
  standalone: true,
  templateUrl: './login.page.html',
  styleUrl: './login.page.scss',
})
export class LoginPage {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  protected readonly codigo = signal('');
  protected readonly pin = signal('');
  protected readonly cargando = signal(false);
  protected readonly error = signal<string | null>(null);

  protected onCodigo(valor: string): void {
    if (/^\d{0,12}$/.test(valor)) {
      this.codigo.set(valor);
      this.error.set(null);
    }
  }

  protected onPin(valor: string): void {
    if (/^\d{0,10}$/.test(valor)) {
      this.pin.set(valor);
      this.error.set(null);
    }
  }

  protected get puedeEnviar(): boolean {
    return this.codigo().length > 0 && this.pin().length >= 3 && !this.cargando();
  }

  protected enviar(): void {
    if (!this.puedeEnviar) return;
    this.cargando.set(true);
    this.error.set(null);

    this.auth.login(this.codigo(), this.pin()).subscribe({
      next: () => this.router.navigate(['/inventario']),
      error: (err: Error) => {
        this.error.set(err.message || 'No se pudo iniciar sesión.');
        this.cargando.set(false);
      },
    });
  }
}
