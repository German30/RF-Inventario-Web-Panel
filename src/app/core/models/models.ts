// Espejo de los DTOs del backend (rf-inventario-panel-api). Nombres de campo
// tal cual el JSON que devuelve la API — sin remapear a inglés.

export type RolOperario = 'OPERARIO' | 'SUPERVISOR';

export type AccionMovimiento =
  | 'CREACION'
  | 'EDICION'
  | 'AJUSTE_STOCK'
  | 'ELIMINACION'
  | 'ENTRADA'
  | 'SALIDA'
  | 'TRANSFERENCIA'
  | 'CONTEO'
  | 'RECEPCION'
  | 'PICKING';

export interface LoginRequest {
  codigo: string;
  pin: string;
}

export interface SesionResponse {
  token: string | null;
  expiraEn: number | null;
  codigo: string;
  nombre: string;
  rol: RolOperario | 'PANEL';
}

export interface ProductoResponse {
  id: number;
  nombre: string;
  sku: string;
  cantidad: number;
  ubicacion: string;
  actualizadoEn: number;
  actualizadoIso: string;
  descripcion: string;
  categoria: string;
  unidad: string;
  stockMinimo: number;
  costoUnitario: number;
  controlaLote: boolean;
  codigosAlternativos: string[];
  bajoMinimo: boolean;
}

export interface ProductoRequest {
  nombre: string;
  sku: string;
  cantidad: number;
  ubicacion: string;
  actualizadoEn: number | null;
  descripcion?: string;
  categoria?: string;
  unidad?: string;
  stockMinimo?: number;
  costoUnitario?: number;
  controlaLote?: boolean;
  codigosAlternativos?: string[];
}

export interface AjusteStockRequest {
  delta: number;
}

export interface MovimientoStockResponse {
  id: number;
  sku: string;
  operadorCodigo: string;
  operadorNombre: string;
  accion: AccionMovimiento;
  delta: number;
  cantidadDespues: number;
  creadoEn: number;
  creadoIso: string;
  productoId?: number;
  motivo?: string;
  referencia?: string;
  ubicacion?: string;
  ubicacionDestino?: string;
  lote?: string;
}

export interface Pagina<T> {
  contenido: T[];
  pagina: number;
  tamanio: number;
  total: number;
  totalPaginas: number;
}

export interface ExistenciaResponse {
  productoId: number;
  sku: string;
  nombre: string;
  ubicacion: string;
  lote: string;
  vence: string | null;
  cantidad: number;
}

export interface UbicacionResponse {
  id: number;
  codigo: string;
  descripcion: string;
  zona: string;
  activa: boolean;
}

export interface UbicacionRequest {
  codigo: string;
  descripcion: string;
  zona: string;
  activa: boolean;
}

export type EstadoConteo = 'ABIERTO' | 'CERRADO' | 'APLICADO' | 'CANCELADO';

export interface ConteoLinea {
  id: number;
  productoId: number;
  sku: string;
  nombre: string;
  ubicacion: string;
  lote: string;
  esperado: number | null;
  contado: number | null;
  diferencia: number | null;
  contadoPor: string | null;
  contadoEn: number | null;
}

export interface ConteoResponse {
  id: number;
  ubicacion: string;
  ciego: boolean;
  estado: EstadoConteo;
  nota: string;
  creadoPorCodigo: string;
  creadoPorNombre: string;
  creadoEn: number;
  cerradoEn: number | null;
  aplicadoEn: number | null;
  aplicadoPor: string | null;
  lineasTotales: number;
  lineasContadas: number;
  lineasConDiferencia: number | null;
  unidadesDiferencia: number | null;
  lineas: ConteoLinea[];
}

export type TipoPedido = 'COMPRA' | 'VENTA';
export type EstadoPedido = 'PENDIENTE' | 'EN_PROCESO' | 'COMPLETADO' | 'CANCELADO';

export interface PedidoLinea {
  id: number;
  productoId: number;
  sku: string;
  nombre: string;
  pedida: number;
  hecha: number;
  pendiente: number;
  sugerencias: ExistenciaResponse[];
}

export interface PedidoResponse {
  id: number;
  tipo: TipoPedido;
  numero: string;
  contraparte: string;
  estado: EstadoPedido;
  nota: string;
  creadoPor: string;
  creadoEn: number;
  completadoEn: number | null;
  unidadesPedidas: number;
  unidadesHechas: number;
  lineas: PedidoLinea[];
}

export interface PedidoRequest {
  tipo: TipoPedido;
  numero: string;
  contraparte: string;
  nota: string;
  lineas: { codigo: string; cantidad: number }[];
}

export interface DashboardResponse {
  productos: number;
  unidades: number;
  valorInventario: number;
  sinStock: number;
  bajoMinimo: number;
  movimientosHoy: number;
  conteosAbiertos: number;
  recepcionesPendientes: number;
  pickingsPendientes: number;
  alertasBajoMinimo: ProductoResponse[];
  proximosAVencer: ExistenciaResponse[];
}

export interface ConflictoEdicionResponse {
  message: string;
  actual: ProductoResponse;
}

export interface ErrorResponse {
  timestamp: string;
  status: number;
  error: string;
  message: string;
  campos?: Record<string, string> | null;
}

export interface OperarioResponse {
  codigo: string;
  nombre: string;
  rol: RolOperario;
  activo: boolean;
}

export interface OperarioRequest {
  codigo: string;
  pin: string;
  nombre: string;
  rol: RolOperario;
}

export interface ActualizarOperarioRequest {
  nombre: string;
  rol: RolOperario;
  activo: boolean;
}

export interface ResetPinRequest {
  pin: string;
}

/** Error tipado que arma el interceptor a partir de cualquier respuesta HTTP fallida. */
export class ApiError extends Error {
  constructor(
    message: string,
    public readonly status: number,
    public readonly campos?: Record<string, string> | null,
  ) {
    super(message);
    this.name = 'ApiError';
  }
}

/** 409 de edición de producto: además del mensaje, trae el estado real del servidor. */
export class ConflictoEdicionError extends ApiError {
  constructor(
    message: string,
    public readonly actual: ProductoResponse,
  ) {
    super(message, 409);
    this.name = 'ConflictoEdicionError';
  }
}
