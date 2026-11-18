import { Injectable, signal } from '@angular/core';

export type TipoAviso = 'ok' | 'error';

export interface Aviso {
  id: number;
  texto: string;
  tipo: TipoAviso;
}

let siguienteId = 1;

/** Cola de avisos efímeros (se estampan y se borran solos). */
@Injectable({ providedIn: 'root' })
export class ToastService {
  private readonly _avisos = signal<Aviso[]>([]);
  readonly avisos = this._avisos.asReadonly();

  ok(texto: string): void {
    this.mostrar(texto, 'ok');
  }

  error(texto: string): void {
    this.mostrar(texto, 'error');
  }

  descartar(id: number): void {
    this._avisos.update((lista) => lista.filter((a) => a.id !== id));
  }

  private mostrar(texto: string, tipo: TipoAviso): void {
    const aviso: Aviso = { id: siguienteId++, texto, tipo };
    this._avisos.update((lista) => [...lista, aviso]);
    setTimeout(() => this.descartar(aviso.id), tipo === 'error' ? 6000 : 3500);
  }
}
