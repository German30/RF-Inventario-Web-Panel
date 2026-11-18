import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { ApiError, ExistenciaResponse, UbicacionResponse } from '../../core/models/models';
import { DepositoService } from '../../core/services/deposito.service';
import { ModalComponent } from '../../shared/components/modal/modal.component';
import { StampComponent } from '../../shared/components/stamp/stamp.component';
import { ToastService } from '../../shared/components/toast/toast.service';

@Component({
  selector: 'app-ubicaciones-page',
  standalone: true,
  imports: [ModalComponent, StampComponent],
  templateUrl: './ubicaciones.page.html',
  styles: `
    .form { display: grid; gap: 12px; }
    .acciones { display: flex; justify-content: flex-end; gap: 8px; }
  `,
})
export class UbicacionesPage implements OnInit {
  private readonly deposito = inject(DepositoService);
  private readonly toast = inject(ToastService);

  protected readonly ubicaciones = signal<UbicacionResponse[]>([]);
  protected readonly filtro = signal('');
  protected readonly visibles = computed(() => {
    const f = this.filtro().trim().toUpperCase();
    return f ? this.ubicaciones().filter((u) => u.codigo.includes(f) || u.zona.toUpperCase().includes(f)) : this.ubicaciones();
  });

  /** Formulario: null = cerrado; codigo vacío en `editando` = alta. */
  protected readonly editando = signal<UbicacionResponse | null>(null);
  protected readonly codigo = signal('');
  protected readonly descripcion = signal('');
  protected readonly zona = signal('');
  protected readonly error = signal<string | null>(null);

  protected readonly viendo = signal<UbicacionResponse | null>(null);
  protected readonly contenido = signal<ExistenciaResponse[] | null>(null);

  ngOnInit(): void {
    this.cargar();
  }

  protected cargar(): void {
    this.deposito.ubicaciones().subscribe({
      next: (l) => this.ubicaciones.set(l),
      error: (e: ApiError) => this.toast.error(e.message),
    });
  }

  protected abrir(u: UbicacionResponse | null): void {
    this.editando.set(u ?? { id: 0, codigo: '', descripcion: '', zona: '', activa: true });
    this.codigo.set(u?.codigo ?? '');
    this.descripcion.set(u?.descripcion ?? '');
    this.zona.set(u?.zona ?? '');
    this.error.set(null);
  }

  protected guardar(): void {
    const u = this.editando();
    if (!u) return;
    const req = { codigo: this.codigo().trim().toUpperCase(), descripcion: this.descripcion(), zona: this.zona(), activa: u.activa };
    const peticion = u.id ? this.deposito.actualizarUbicacion(u.codigo, req) : this.deposito.crearUbicacion(req);
    peticion.subscribe({
      next: (r) => {
        this.toast.ok(`Ubicación ${r.codigo} guardada.`);
        this.editando.set(null);
        this.cargar();
      },
      error: (e: ApiError) => this.error.set(e.message),
    });
  }

  protected alternarActiva(u: UbicacionResponse): void {
    this.deposito.actualizarUbicacion(u.codigo, { ...u, activa: !u.activa }).subscribe({
      next: (r) => {
        this.toast.ok(`${r.codigo} ${r.activa ? 'activada' : 'desactivada'}.`);
        this.cargar();
      },
      error: (e: ApiError) => this.toast.error(e.message),
    });
  }

  protected verContenido(u: UbicacionResponse): void {
    this.viendo.set(u);
    this.contenido.set(null);
    this.deposito.contenido(u.codigo).subscribe({
      next: (l) => this.contenido.set(l),
      error: (e: ApiError) => this.toast.error(e.message),
    });
  }
}
