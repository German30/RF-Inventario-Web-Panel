import { Component, OnInit, inject, signal } from '@angular/core';
import { ApiError, ProductoResponse } from '../../core/models/models';
import { ProductoService } from '../../core/services/producto.service';
import { ToastService } from '../../shared/components/toast/toast.service';
import { ConfirmDialogComponent } from '../../shared/components/confirm/confirm-dialog.component';
import { ProductoFormComponent } from './producto-form.component';
import { StockAdjustComponent } from './stock-adjust.component';
import { HistorialDrawerComponent } from './historial-drawer.component';
import { StampComponent } from '../../shared/components/stamp/stamp.component';

@Component({
  selector: 'app-productos-list-page',
  standalone: true,
  imports: [ProductoFormComponent, StockAdjustComponent, HistorialDrawerComponent, ConfirmDialogComponent, StampComponent],
  templateUrl: './productos-list.page.html',
  styleUrl: './productos-list.page.scss',
})
export class ProductosListPage implements OnInit {
  private readonly productoService = inject(ProductoService);
  private readonly toast = inject(ToastService);

  protected readonly productos = signal<ProductoResponse[]>([]);
  protected readonly busqueda = signal('');
  protected readonly cargando = signal(true);

  protected readonly mostrarFormulario = signal(false);
  protected readonly editando = signal<ProductoResponse | null>(null);
  protected readonly ajustando = signal<ProductoResponse | null>(null);
  protected readonly verHistorialDe = signal<ProductoResponse | null>(null);
  protected readonly borrando = signal<ProductoResponse | null>(null);
  protected readonly eliminando = signal(false);

  private debounceHandle?: ReturnType<typeof setTimeout>;

  ngOnInit(): void {
    this.cargar();
  }

  protected onBusqueda(valor: string): void {
    this.busqueda.set(valor);
    clearTimeout(this.debounceHandle);
    this.debounceHandle = setTimeout(() => this.cargar(), 300);
  }

  protected cargar(): void {
    this.cargando.set(true);
    this.productoService.listar(this.busqueda()).subscribe({
      next: (lista) => {
        this.productos.set(lista);
        this.cargando.set(false);
      },
      error: (err: ApiError) => {
        this.toast.error(err.message);
        this.cargando.set(false);
      },
    });
  }

  protected abrirAlta(): void {
    this.editando.set(null);
    this.mostrarFormulario.set(true);
  }

  protected abrirEdicion(p: ProductoResponse): void {
    this.editando.set(p);
    this.mostrarFormulario.set(true);
  }

  protected cerrarFormulario(): void {
    this.mostrarFormulario.set(false);
  }

  protected onGuardado(p: ProductoResponse): void {
    this.mostrarFormulario.set(false);
    this.toast.ok(`${p.sku} guardado.`);
    this.cargar();
  }

  protected confirmarEliminar(p: ProductoResponse): void {
    this.borrando.set(p);
  }

  protected eliminar(): void {
    const p = this.borrando();
    if (!p) return;
    this.eliminando.set(true);
    this.productoService.eliminar(p.id).subscribe({
      next: () => {
        this.eliminando.set(false);
        this.borrando.set(null);
        this.toast.ok(`${p.sku} eliminado.`);
        this.cargar();
      },
      error: (err: ApiError) => {
        this.eliminando.set(false);
        this.toast.error(err.message);
        this.borrando.set(null);
      },
    });
  }

  protected abrirAjuste(p: ProductoResponse): void {
    this.ajustando.set(p);
  }

  protected onAjustado(p: ProductoResponse): void {
    this.ajustando.set(null);
    this.toast.ok(`Stock de ${p.sku} ajustado a ${p.cantidad}.`);
    this.cargar();
  }

  protected abrirHistorial(p: ProductoResponse): void {
    this.verHistorialDe.set(p);
  }
}
