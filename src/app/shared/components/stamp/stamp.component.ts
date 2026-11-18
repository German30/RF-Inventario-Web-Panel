import { Component, input } from '@angular/core';

export type TonoSello = 'ok' | 'danger' | 'warn' | 'role' | 'neutral';

/**
 * El lenguaje de estado del panel: un badge (fondo suave + texto de mayor
 * contraste de la misma tinta) en vez de un chip de color plano. Se usa para
 * rol de operario, activo/baja y tipo de movimiento del historial.
 */
@Component({
  selector: 'app-stamp',
  standalone: true,
  templateUrl: './stamp.component.html',
  styleUrl: './stamp.component.scss',
})
export class StampComponent {
  readonly texto = input.required<string>();
  readonly tono = input<TonoSello>('neutral');
}
