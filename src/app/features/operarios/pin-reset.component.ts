import { Component, inject, input, output, signal } from '@angular/core';
import { ApiError, OperarioResponse } from '../../core/models/models';
import { OperarioService } from '../../core/services/operario.service';
import { ModalComponent } from '../../shared/components/modal/modal.component';

@Component({
  selector: 'app-pin-reset',
  standalone: true,
  imports: [ModalComponent],
  templateUrl: './pin-reset.component.html',
  styleUrl: './pin-reset.component.scss',
})
export class PinResetComponent {
  private readonly operarioService = inject(OperarioService);

  readonly operario = input.required<OperarioResponse>();
  readonly cerrar = output<void>();
  readonly reseteado = output<void>();

  protected readonly pin = signal('');
  protected readonly guardando = signal(false);
  protected readonly error = signal<string | null>(null);

  protected onPin(valor: string): void {
    if (/^\d{0,10}$/.test(valor)) this.pin.set(valor);
  }

  protected confirmar(): void {
    if (this.pin().length < 3 || this.guardando()) return;
    this.guardando.set(true);
    this.error.set(null);

    this.operarioService.resetearPin(this.operario().codigo, this.pin()).subscribe({
      next: () => {
        this.guardando.set(false);
        this.reseteado.emit();
      },
      error: (err: ApiError) => {
        this.guardando.set(false);
        this.error.set(err.message);
      },
    });
  }
}
