import { Component, inject, input, output, signal } from '@angular/core';
import { ApiError, ProductoResponse } from '../../core/models/models';
import { ProductoService } from '../../core/services/producto.service';
import { ModalComponent } from '../../shared/components/modal/modal.component';

@Component({
  selector: 'app-stock-adjust',
  standalone: true,
  imports: [ModalComponent],
  templateUrl: './stock-adjust.component.html',
  styleUrl: './stock-adjust.component.scss',
})
export class StockAdjustComponent {
  private readonly productoService = inject(ProductoService);

  readonly producto = input.required<ProductoResponse>();
  readonly cerrar = output<void>();
  readonly ajustado = output<ProductoResponse>();

  protected readonly delta = signal(0);
  protected readonly guardando = signal(false);
  protected readonly error = signal<string | null>(null);

  protected get resultado(): number {
    return this.producto().cantidad + this.delta();
  }

  protected sumar(cantidad: number): void {
    this.delta.update((d) => d + cantidad);
    this.error.set(null);
  }

  protected onDelta(valor: string): void {
    if (/^-?\d{0,7}$/.test(valor)) {
      this.delta.set(valor === '' || valor === '-' ? 0 : Number(valor));
    }
  }

  protected confirmar(): void {
    if (this.delta() === 0 || this.resultado < 0 || this.guardando()) return;
    this.guardando.set(true);
    this.error.set(null);

    this.productoService.ajustarStock(this.producto().id, this.delta()).subscribe({
      next: (res) => {
        this.guardando.set(false);
        this.ajustado.emit(res);
      },
      error: (err: ApiError) => {
        this.guardando.set(false);
        this.error.set(err.message);
      },
    });
  }
}
