import { Component, computed, effect, inject, input, output, signal } from '@angular/core';
import { AuthService } from '../../core/services/auth.service';
import { OperarioService } from '../../core/services/operario.service';
import { ApiError, OperarioResponse, RolOperario } from '../../core/models/models';
import { ModalComponent } from '../../shared/components/modal/modal.component';

@Component({
  selector: 'app-operario-form',
  standalone: true,
  imports: [ModalComponent],
  templateUrl: './operario-form.component.html',
  styleUrl: './operario-form.component.scss',
})
export class OperarioFormComponent {
  private readonly operarioService = inject(OperarioService);
  private readonly auth = inject(AuthService);

  /** null = alta. */
  readonly inicial = input<OperarioResponse | null>(null);
  readonly cerrar = output<void>();
  readonly guardado = output<OperarioResponse>();

  protected readonly esAlta = computed(() => this.inicial() === null);
  protected readonly esUnoMismo = computed(() => this.inicial()?.codigo === this.auth.codigoOperario());

  protected readonly codigo = signal('');
  protected readonly pin = signal('');
  protected readonly nombre = signal('');
  protected readonly rol = signal<RolOperario>('OPERARIO');
  protected readonly activo = signal(true);

  protected readonly guardando = signal(false);
  protected readonly error = signal<string | null>(null);

  constructor() {
    // Ver nota equivalente en ProductoFormComponent: `inicial` recién queda
    // enlazado después del constructor, por eso se lee dentro de un effect().
    effect(
      () => {
        const o = this.inicial();
        if (o) {
          this.nombre.set(o.nombre);
          this.rol.set(o.rol);
          this.activo.set(o.activo);
        }
      },
      { allowSignalWrites: true }
    );
  }

  protected onCodigo(valor: string): void {
    if (/^\d{0,12}$/.test(valor)) this.codigo.set(valor);
  }

  protected onPin(valor: string): void {
    if (/^\d{0,10}$/.test(valor)) this.pin.set(valor);
  }

  protected get esValido(): boolean {
    if (this.nombre().trim().length === 0) return false;
    if (this.esAlta()) {
      return this.codigo().length > 0 && this.pin().length >= 3;
    }
    return true;
  }

  protected guardar(): void {
    if (!this.esValido || this.guardando()) return;
    this.guardando.set(true);
    this.error.set(null);

    const peticion = this.esAlta()
      ? this.operarioService.crear({
          codigo: this.codigo().trim(),
          pin: this.pin().trim(),
          nombre: this.nombre().trim(),
          rol: this.rol(),
        })
      : this.operarioService.actualizar(this.inicial()!.codigo, {
          nombre: this.nombre().trim(),
          rol: this.rol(),
          activo: this.activo(),
        });

    peticion.subscribe({
      next: (res) => {
        this.guardando.set(false);
        this.guardado.emit(res);
      },
      error: (err: ApiError) => {
        this.guardando.set(false);
        this.error.set(err.message);
      },
    });
  }
}
