import { Component, OnInit, inject, signal } from '@angular/core';
import { ApiError, ConteoResponse } from '../../core/models/models';
import { DepositoService } from '../../core/services/deposito.service';
import { ConfirmDialogComponent } from '../../shared/components/confirm/confirm-dialog.component';
import { ModalComponent } from '../../shared/components/modal/modal.component';
import { StampComponent, TonoSello } from '../../shared/components/stamp/stamp.component';
import { ToastService } from '../../shared/components/toast/toast.service';

@Component({
  selector: 'app-conteos-page',
  standalone: true,
  imports: [ModalComponent, StampComponent, ConfirmDialogComponent],
  templateUrl: './conteos.page.html',
  styles: `
    .form { display: grid; gap: 12px; }
    .acciones { display: flex; justify-content: flex-end; gap: 8px; margin-top: 12px; }
    .dif--neg { color: var(--danger, #b42318); font-weight: 600; }
    .dif--pos { color: var(--warn, #b54708); font-weight: 600; }
  `,
})
export class ConteosPage implements OnInit {
  private readonly deposito = inject(DepositoService);
  private readonly toast = inject(ToastService);

  protected readonly abiertos = signal(true);
  protected readonly conteos = signal<ConteoResponse[]>([]);
  protected readonly detalle = signal<ConteoResponse | null>(null);
  protected readonly aplicando = signal<ConteoResponse | null>(null);

  protected readonly creando = signal(false);
  protected readonly ubicacion = signal('');
  protected readonly ciego = signal(true);
  protected readonly nota = signal('');

  ngOnInit(): void {
    this.cargar();
  }

  protected cargar(): void {
    this.deposito.conteos(this.abiertos()).subscribe({
      next: (l) => this.conteos.set(l),
      error: (e: ApiError) => this.toast.error(e.message),
    });
  }

  protected verDetalle(c: ConteoResponse): void {
    this.deposito.conteo(c.id).subscribe({
      next: (d) => this.detalle.set(d),
      error: (e: ApiError) => this.toast.error(e.message),
    });
  }

  protected accion(c: ConteoResponse, accion: 'aplicar' | 'reabrir' | 'cancelar'): void {
    this.deposito.accionConteo(c.id, accion).subscribe({
      next: (r) => {
        this.toast.ok(
          accion === 'aplicar'
            ? `Conteo #${r.id} aplicado: ${r.lineasConDiferencia ?? 0} diferencias ajustadas.`
            : `Conteo #${r.id}: ${r.estado.toLowerCase()}.`,
        );
        this.detalle.set(accion === 'reabrir' ? r : null);
        this.aplicando.set(null);
        this.cargar();
      },
      error: (e: ApiError) => {
        this.aplicando.set(null);
        this.toast.error(e.message);
      },
    });
  }

  protected crear(): void {
    this.deposito.abrirConteo(this.ubicacion().trim().toUpperCase(), this.ciego(), this.nota().trim()).subscribe({
      next: (r) => {
        this.toast.ok(`Conteo #${r.id} abierto: ${r.lineasTotales} líneas a contar.`);
        this.creando.set(false);
        this.cargar();
      },
      error: (e: ApiError) => this.toast.error(e.message),
    });
  }

  protected tono(estado: string): TonoSello {
    return estado === 'APLICADO' ? 'ok' : estado === 'CERRADO' ? 'warn' : estado === 'CANCELADO' ? 'neutral' : 'role';
  }

  protected fecha(ms: number): string {
    return new Date(ms).toLocaleString('es-AR', { day: '2-digit', month: '2-digit', hour: '2-digit', minute: '2-digit' });
  }
}
