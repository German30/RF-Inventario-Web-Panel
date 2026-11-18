import { CurrencyPipe } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { ApiError, DashboardResponse } from '../../core/models/models';
import { DepositoService } from '../../core/services/deposito.service';
import { StampComponent } from '../../shared/components/stamp/stamp.component';
import { ToastService } from '../../shared/components/toast/toast.service';

@Component({
  selector: 'app-resumen-page',
  standalone: true,
  imports: [CurrencyPipe, RouterLink, StampComponent],
  templateUrl: './resumen.page.html',
  styles: `
    .kpis { display: grid; grid-template-columns: repeat(auto-fill, minmax(170px, 1fr)); gap: 12px; margin-bottom: 24px; }
    .kpi { padding: 16px; display: flex; flex-direction: column; gap: 4px; text-decoration: none; color: inherit; }
    .kpi__valor { font-size: 1.6rem; font-weight: 600; }
    .kpi__label { font-size: 0.8rem; opacity: 0.75; }
    .kpi--alerta .kpi__valor { color: var(--danger, #b42318); }
    .alertas { display: grid; grid-template-columns: repeat(auto-fit, minmax(320px, 1fr)); gap: 16px; }
    .alertas h2 { font-size: 1rem; margin: 0 0 8px; }
    .reportes { display: flex; flex-wrap: wrap; gap: 8px; }
  `,
})
export class ResumenPage implements OnInit {
  private readonly deposito = inject(DepositoService);
  private readonly toast = inject(ToastService);

  protected readonly datos = signal<DashboardResponse | null>(null);

  ngOnInit(): void {
    this.cargar();
  }

  protected cargar(): void {
    this.deposito.dashboard().subscribe({
      next: (d) => this.datos.set(d),
      error: (err: ApiError) => this.toast.error(err.message),
    });
  }

  protected descargar(reporte: 'inventario' | 'existencias' | 'movimientos'): void {
    this.deposito.descargarCsv(reporte);
  }
}
