import { Component, OnInit, inject, signal } from '@angular/core';
import { ApiError, OperarioResponse, RolOperario } from '../../core/models/models';
import { OperarioService } from '../../core/services/operario.service';
import { AuthService } from '../../core/services/auth.service';
import { ToastService } from '../../shared/components/toast/toast.service';
import { StampComponent, TonoSello } from '../../shared/components/stamp/stamp.component';
import { OperarioFormComponent } from './operario-form.component';
import { PinResetComponent } from './pin-reset.component';

@Component({
  selector: 'app-operarios-list-page',
  standalone: true,
  imports: [StampComponent, OperarioFormComponent, PinResetComponent],
  templateUrl: './operarios-list.page.html',
  styleUrl: './operarios-list.page.scss',
})
export class OperariosListPage implements OnInit {
  private readonly operarioService = inject(OperarioService);
  private readonly toast = inject(ToastService);
  protected readonly auth = inject(AuthService);

  protected readonly operarios = signal<OperarioResponse[]>([]);
  protected readonly cargando = signal(true);

  protected readonly mostrarFormulario = signal(false);
  protected readonly editando = signal<OperarioResponse | null>(null);
  protected readonly reseteandoPinDe = signal<OperarioResponse | null>(null);

  ngOnInit(): void {
    this.cargar();
  }

  protected cargar(): void {
    this.cargando.set(true);
    this.operarioService.listar().subscribe({
      next: (lista) => {
        this.operarios.set(lista);
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

  protected abrirEdicion(o: OperarioResponse): void {
    this.editando.set(o);
    this.mostrarFormulario.set(true);
  }

  protected onGuardado(o: OperarioResponse): void {
    this.mostrarFormulario.set(false);
    this.toast.ok(`${o.codigo} guardado.`);
    this.cargar();
  }

  protected onPinReseteado(): void {
    this.reseteandoPinDe.set(null);
    this.toast.ok('PIN actualizado.');
  }

  protected rolEtiqueta(rol: RolOperario): string {
    return rol === 'SUPERVISOR' ? 'Supervisor' : 'Operario';
  }

  protected rolTono(rol: RolOperario): TonoSello {
    return rol === 'SUPERVISOR' ? 'role' : 'neutral';
  }
}
