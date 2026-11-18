import { Component, input, output } from '@angular/core';

@Component({
  selector: 'app-modal',
  standalone: true,
  templateUrl: './modal.component.html',
  styleUrl: './modal.component.scss',
})
export class ModalComponent {
  readonly titulo = input('');
  readonly ancho = input<'normal' | 'angosto'>('normal');
  readonly cerrar = output<void>();
}
