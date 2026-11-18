import { Component, OnInit, inject, input, output, signal } from '@angular/core';
import { AccionMovimiento, ApiError, MovimientoStockResponse, ProductoResponse } from '../../core/models/models';
import { ProductoService } from '../../core/services/producto.service';
import { StampComponent, TonoSello } from '../../shared/components/stamp/stamp.component';

export const ETIQUETA_ACCION: Record<AccionMovimiento, string> = {
  CREACION: 'Alta',
  EDICION: 'Edición',
  AJUSTE_STOCK: 'Ajuste',
  ELIMINACION: 'Baja',
  ENTRADA: 'Entrada',
  SALIDA: 'Salida',
  TRANSFERENCIA: 'Transferencia',
  CONTEO: 'Conteo',
  RECEPCION: 'Recepción',
  PICKING: 'Picking',
};

export const TONO_ACCION: Record<AccionMovimiento, TonoSello> = {
  CREACION: 'ok',
  EDICION: 'role',
  AJUSTE_STOCK: 'warn',
  ELIMINACION: 'danger',
  ENTRADA: 'ok',
  SALIDA: 'danger',
  TRANSFERENCIA: 'neutral',
  CONTEO: 'warn',
  RECEPCION: 'ok',
  PICKING: 'danger',
};

@Component({
  selector: 'app-historial-drawer',
  standalone: true,
  imports: [StampComponent],
  templateUrl: './historial-drawer.component.html',
  styleUrl: './historial-drawer.component.scss',
})
export class HistorialDrawerComponent implements OnInit {
  private readonly productoService = inject(ProductoService);

  readonly producto = input.required<ProductoResponse>();
  readonly cerrar = output<void>();

  protected readonly movimientos = signal<MovimientoStockResponse[]>([]);
  protected readonly cargando = signal(true);
  protected readonly error = signal<string | null>(null);

  ngOnInit(): void {
    this.productoService.movimientos(this.producto().id).subscribe({
      next: (lista) => {
        this.movimientos.set(lista);
        this.cargando.set(false);
      },
      error: (err: ApiError) => {
        this.error.set(err.message);
        this.cargando.set(false);
      },
    });
  }

  protected etiqueta(accion: AccionMovimiento): string {
    return ETIQUETA_ACCION[accion];
  }

  protected tono(accion: AccionMovimiento): TonoSello {
    return TONO_ACCION[accion];
  }

  protected fecha(epochMillis: number): string {
    return new Date(epochMillis).toLocaleString('es-AR', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    });
  }
}
