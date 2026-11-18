import { Component, computed, effect, inject, input, output, signal } from '@angular/core';
import { ProductoService } from '../../core/services/producto.service';
import { ApiError, ConflictoEdicionError, ProductoRequest, ProductoResponse } from '../../core/models/models';
import { ModalComponent } from '../../shared/components/modal/modal.component';

@Component({
  selector: 'app-producto-form',
  standalone: true,
  imports: [ModalComponent],
  templateUrl: './producto-form.component.html',
  styleUrl: './producto-form.component.scss',
})
export class ProductoFormComponent {
  private readonly productoService = inject(ProductoService);

  /** null = alta. */
  readonly inicial = input<ProductoResponse | null>(null);
  readonly cerrar = output<void>();
  readonly guardado = output<ProductoResponse>();

  protected readonly esAlta = computed(() => this.inicial() === null);

  protected readonly nombre = signal('');
  protected readonly sku = signal('');
  protected readonly cantidad = signal('0');
  protected readonly ubicacion = signal('');
  protected readonly categoria = signal('');
  protected readonly unidad = signal('UN');
  protected readonly stockMinimo = signal('0');
  protected readonly costo = signal('0');
  protected readonly controlaLote = signal(false);
  protected readonly codigos = signal('');

  /** Versión que se manda al servidor para el chequeo de optimistic locking. */
  private versionBase: number | null = null;

  protected readonly guardando = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly conflicto = signal<ProductoResponse | null>(null);

  constructor() {
    // `inicial` es un input signal: su valor recién queda enlazado después de
    // construir el componente, así que no se puede leer acá directamente
    // (siempre daría null, forzando el alta aunque sea una edición). Un
    // effect() corre una vez que Angular ya aplicó el input.
    effect(
      () => {
        const p = this.inicial();
        if (p) {
          this.nombre.set(p.nombre);
          this.sku.set(p.sku);
          this.cantidad.set(String(p.cantidad));
          this.ubicacion.set(p.ubicacion);
          this.categoria.set(p.categoria ?? '');
          this.unidad.set(p.unidad ?? 'UN');
          this.stockMinimo.set(String(p.stockMinimo ?? 0));
          this.costo.set(String(p.costoUnitario ?? 0));
          this.controlaLote.set(!!p.controlaLote);
          this.codigos.set((p.codigosAlternativos ?? []).join(', '));
          this.versionBase = p.actualizadoEn;
        }
      },
      { allowSignalWrites: true }
    );
  }

  protected get esValido(): boolean {
    return this.nombre().trim().length > 0 && this.sku().trim().length > 0 && this.cantidadValida !== null;
  }

  private get cantidadValida(): number | null {
    const n = Number(this.cantidad());
    return Number.isInteger(n) && n >= 0 ? n : null;
  }

  protected onCantidad(valor: string): void {
    if (/^\d{0,7}$/.test(valor)) {
      this.cantidad.set(valor);
    }
  }

  protected guardar(): void {
    if (!this.esValido || this.guardando()) return;

    const req: ProductoRequest = {
      nombre: this.nombre().trim(),
      sku: this.sku().trim(),
      cantidad: this.cantidadValida!,
      ubicacion: this.ubicacion().trim(),
      actualizadoEn: this.versionBase,
      categoria: this.categoria().trim(),
      unidad: this.unidad().trim() || 'UN',
      stockMinimo: Math.max(0, Math.floor(Number(this.stockMinimo()) || 0)),
      costoUnitario: Math.max(0, Number(this.costo().replace(',', '.')) || 0),
      controlaLote: this.controlaLote(),
      codigosAlternativos: this.codigos()
        .split(/[,;\s]+/)
        .map((c) => c.trim())
        .filter((c) => c.length > 0),
    };

    this.guardando.set(true);
    this.error.set(null);
    this.conflicto.set(null);

    const inicial = this.inicial();
    const peticion = inicial
      ? this.productoService.actualizar(inicial.id, req)
      : this.productoService.crear(req);

    peticion.subscribe({
      next: (res) => {
        this.guardando.set(false);
        this.guardado.emit(res);
      },
      error: (err: ApiError) => {
        this.guardando.set(false);
        if (err instanceof ConflictoEdicionError) {
          this.conflicto.set(err.actual);
        } else {
          this.error.set(err.message);
        }
      },
    });
  }

  /** El operario elige quedarse con lo que hay en el servidor y reintentar desde ahí. */
  protected usarValoresDelServidor(): void {
    const actual = this.conflicto();
    if (!actual) return;
    this.nombre.set(actual.nombre);
    this.sku.set(actual.sku);
    this.cantidad.set(String(actual.cantidad));
    this.ubicacion.set(actual.ubicacion);
    this.versionBase = actual.actualizadoEn;
    this.conflicto.set(null);
  }
}
