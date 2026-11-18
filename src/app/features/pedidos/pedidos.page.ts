import { Component, OnInit, inject, signal } from '@angular/core';
import { ApiError, PedidoResponse, TipoPedido } from '../../core/models/models';
import { DepositoService } from '../../core/services/deposito.service';
import { ModalComponent } from '../../shared/components/modal/modal.component';
import { StampComponent, TonoSello } from '../../shared/components/stamp/stamp.component';
import { ToastService } from '../../shared/components/toast/toast.service';

interface LineaBorrador {
  codigo: string;
  cantidad: number;
}

@Component({
  selector: 'app-pedidos-page',
  standalone: true,
  imports: [ModalComponent, StampComponent],
  templateUrl: './pedidos.page.html',
  styles: `
    .form { display: grid; gap: 12px; }
    .fila { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; }
    .linea { display: grid; grid-template-columns: 1fr 110px auto; gap: 8px; align-items: end; }
    .acciones { display: flex; justify-content: flex-end; gap: 8px; }
    .progreso { height: 6px; background: var(--line, #e5e5e5); border-radius: 3px; overflow: hidden; min-width: 80px; }
    .progreso > span { display: block; height: 100%; background: var(--accent, #3b5998); }
    .tabs { display: flex; gap: 8px; }
  `,
})
export class PedidosPage implements OnInit {
  private readonly deposito = inject(DepositoService);
  private readonly toast = inject(ToastService);

  protected readonly tipo = signal<TipoPedido | null>(null);
  protected readonly abiertos = signal(true);
  protected readonly pedidos = signal<PedidoResponse[]>([]);
  protected readonly detalle = signal<PedidoResponse | null>(null);

  protected readonly creando = signal(false);
  protected readonly nuevoTipo = signal<TipoPedido>('COMPRA');
  protected readonly numero = signal('');
  protected readonly contraparte = signal('');
  protected readonly nota = signal('');
  protected readonly lineas = signal<LineaBorrador[]>([{ codigo: '', cantidad: 1 }]);
  protected readonly error = signal<string | null>(null);

  ngOnInit(): void {
    this.cargar();
  }

  protected cargar(): void {
    this.deposito.pedidos(this.tipo(), this.abiertos()).subscribe({
      next: (l) => this.pedidos.set(l),
      error: (e: ApiError) => this.toast.error(e.message),
    });
  }

  protected filtrar(tipo: TipoPedido | null, abiertos = this.abiertos()): void {
    this.tipo.set(tipo);
    this.abiertos.set(abiertos);
    this.cargar();
  }

  protected abrirDetalle(p: PedidoResponse): void {
    this.deposito.pedido(p.id).subscribe({
      next: (d) => this.detalle.set(d),
      error: (e: ApiError) => this.toast.error(e.message),
    });
  }

  protected accion(p: PedidoResponse, accion: 'completar' | 'cancelar'): void {
    this.deposito.accionPedido(p.id, accion).subscribe({
      next: (r) => {
        this.toast.ok(`Pedido ${r.numero}: ${r.estado.toLowerCase()}.`);
        this.detalle.set(null);
        this.cargar();
      },
      error: (e: ApiError) => this.toast.error(e.message),
    });
  }

  protected nuevo(): void {
    this.creando.set(true);
    this.numero.set('');
    this.contraparte.set('');
    this.nota.set('');
    this.lineas.set([{ codigo: '', cantidad: 1 }]);
    this.error.set(null);
  }

  protected editarLinea(i: number, cambio: Partial<LineaBorrador>): void {
    this.lineas.update((ls) => ls.map((l, j) => (j === i ? { ...l, ...cambio } : l)));
  }

  protected agregarLinea(): void {
    this.lineas.update((ls) => [...ls, { codigo: '', cantidad: 1 }]);
  }

  protected quitarLinea(i: number): void {
    this.lineas.update((ls) => (ls.length > 1 ? ls.filter((_, j) => j !== i) : ls));
  }

  protected get puedeCrear(): boolean {
    return this.numero().trim().length > 0 && this.lineas().every((l) => l.codigo.trim() && l.cantidad > 0);
  }

  protected crear(): void {
    if (!this.puedeCrear) return;
    this.deposito
      .crearPedido({
        tipo: this.nuevoTipo(),
        numero: this.numero().trim(),
        contraparte: this.contraparte().trim(),
        nota: this.nota().trim(),
        lineas: this.lineas().map((l) => ({ codigo: l.codigo.trim(), cantidad: l.cantidad })),
      })
      .subscribe({
        next: (r) => {
          this.toast.ok(`Pedido ${r.numero} creado: ya aparece en las terminales.`);
          this.creando.set(false);
          this.cargar();
        },
        error: (e: ApiError) => this.error.set(e.message),
      });
  }

  protected porcentaje(p: PedidoResponse): number {
    return p.unidadesPedidas ? Math.round((p.unidadesHechas / p.unidadesPedidas) * 100) : 0;
  }

  protected tonoEstado(estado: string): TonoSello {
    return estado === 'COMPLETADO' ? 'ok' : estado === 'CANCELADO' ? 'neutral' : estado === 'EN_PROCESO' ? 'warn' : 'role';
  }

  protected fecha(ms: number): string {
    return new Date(ms).toLocaleString('es-AR', { day: '2-digit', month: '2-digit', hour: '2-digit', minute: '2-digit' });
  }
}
