import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';

export const routes: Routes = [
  {
    path: 'login',
    loadComponent: () => import('./features/login/login.page').then((m) => m.LoginPage),
  },
  {
    path: '',
    canActivate: [authGuard],
    loadComponent: () => import('./features/shell/shell.component').then((m) => m.ShellComponent),
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'resumen' },
      {
        path: 'resumen',
        loadComponent: () => import('./features/resumen/resumen.page').then((m) => m.ResumenPage),
      },
      {
        path: 'pedidos',
        loadComponent: () => import('./features/pedidos/pedidos.page').then((m) => m.PedidosPage),
      },
      {
        path: 'conteos',
        loadComponent: () => import('./features/conteos/conteos.page').then((m) => m.ConteosPage),
      },
      {
        path: 'ubicaciones',
        loadComponent: () => import('./features/ubicaciones/ubicaciones.page').then((m) => m.UbicacionesPage),
      },
      {
        path: 'inventario',
        loadComponent: () =>
          import('./features/productos/productos-list.page').then((m) => m.ProductosListPage),
      },
      {
        path: 'operarios',
        loadComponent: () =>
          import('./features/operarios/operarios-list.page').then((m) => m.OperariosListPage),
      },
    ],
  },
  { path: '**', redirectTo: '' },
];
