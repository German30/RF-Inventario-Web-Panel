# RF Inventario · Panel API

Backend REST para el panel web de administración de RF Inventario y para la
app Android (que consume esta API por HTTP, ya no toca MySQL directo). Dueño
único del esquema (vía Flyway) y de las credenciales de base de datos.

## Stack

| | |
|---|---|
| Java | 21 |
| Framework | Spring Boot 3.4.5 (web, data-jpa, validation, security) |
| Auth | JWT (jjwt 0.12.6), HS256, stateless; PIN con BCrypt |
| Esquema | Flyway (`src/main/resources/db/migration/`) — nunca SQL suelto a mano |
| Docs | springdoc-openapi 2.8.6 → Swagger UI |
| DB | MySQL (`com.mysql:mysql-connector-j`), Hibernate `ddl-auto=validate` |
| Build | Maven + wrapper |
| CI | GitHub Actions (`.github/workflows/backend-ci.yml`) — build + tests en cada push/PR |

## Correr en local

Solo hace falta un MySQL vacío (o uno ya con el esquema viejo, ver
"Migraciones" abajo) — Flyway arma las tablas solo al arrancar.

```bash
./mvnw spring-boot:run
# o
./mvnw clean package && java -jar target/rf-inventario-panel-api-0.0.1-SNAPSHOT.jar
```

Por defecto: `http://localhost:8082`, MySQL en `localhost:3306`, `root` / `root123`.
Config por variables de entorno (ver `.env.example`): `DB_HOST`, `DB_PORT`,
`DB_NAME`, `DB_USER`, `DB_PASSWORD`, `SERVER_PORT`, `CORS_ORIGINS`, `JWT_SECRETO`.

## Migraciones (Flyway)

```
src/main/resources/db/migration/
├── V1__baseline.sql          # products + users
├── V2__pin_hash_bcrypt.sql   # SHA-256 -> BCrypt en pin_hash
├── V3__stock_movements.sql   # historial de auditoría
├── V4__operator_roles.sql    # users.role: OPERARIO | SUPERVISOR
├── V5__panel_admins.sql      # tabla panel_admins: el login del panel se separa del de la app
└── V6__wms_core.sql          # ubicaciones, stock por ubicación/lote, conteos, pedidos, ficha completa
```

En una base **nueva y vacía**, Flyway corre todas en orden. V6 migra los datos
existentes: cada texto de `products.location` pasa a ser una ubicación real y el
stock actual queda asignado ahí (sin lote). En la base de
desarrollo (que ya tenía `products`/`users` de antes de adoptar Flyway),
`baseline-version=1` hace que se salteé V1 y arranque desde V2. Cualquier
cambio de esquema nuevo entra como `V7__algo.sql`, nunca editando un archivo
ya aplicado.

## Endpoints

| Método | Ruta | Rol | |
|---|---|---|---|
| POST | `/api/auth/login` | público | entrar a la **app** (código + PIN de operario) → token + rol |
| GET | `/api/auth/yo` | operario | validar token actual |
| POST | `/api/panel/auth/login` | público | entrar al **panel** (código + PIN de administrador) → token |
| GET | `/api/panel/auth/yo` | **admin panel** | validar token actual del panel |
| GET | `/api/ping` | público | healthcheck |
| GET | `/api/productos?q=` | operario | listar / buscar por nombre o SKU |
| GET | `/api/productos/{id}` | operario | ver uno |
| GET | `/api/productos/sku/{sku}` | operario | ver por SKU exacto (lo usa el escáner de la app) |
| GET | `/api/productos/{id}/movimientos` | operario | historial: quién lo tocó, cuándo, qué cambió |
| POST | `/api/productos` | operario | alta (SKU único) |
| PUT | `/api/productos/{id}` | operario | editar (optimistic locking por `actualizadoEn`, 409 si choca) |
| PATCH | `/api/productos/{id}/stock` | operario | ajustar stock (`{"delta": -3}`), con lock de fila |
| DELETE | `/api/productos/{id}` | **supervisor** | eliminar (queda registrado en el historial antes de borrar) |
| GET | `/api/productos/pagina?q=&categoria=&filtro=&pagina=&tamanio=` | operario | listado paginado; `filtro` = `BAJO_MINIMO` \| `SIN_STOCK` |
| GET | `/api/productos/{id}/existencias` | operario | stock por ubicación y lote |
| GET | `/api/productos/categorias` | operario | categorías en uso |
| POST | `/api/operaciones` | operario | ENTRADA / SALIDA (FEFO) / TRANSFERENCIA / AJUSTE, idempotente por `clientOpId` |
| GET | `/api/movimientos?desde=&hasta=&sku=&operador=&accion=` | operario | historial global paginado |
| GET | `/api/sync?desde=` | operario | cambios desde el cursor (caché offline de la app) |
| GET | `/api/dashboard` | operario | KPIs, bajo mínimo, lotes por vencer |
| GET | `/api/reportes/{inventario,existencias,movimientos}.csv` | operario | exportaciones (Excel-friendly) |
| GET | `/api/ubicaciones`, `/{codigo}`, `/{codigo}/contenido` | operario | ubicaciones y qué hay en cada una |
| POST/PUT/DELETE | `/api/ubicaciones[/{codigo}]` | **supervisor** | alta / edición / desactivar (no se borran) |
| GET/POST | `/api/conteos`, `/{id}`, `/{id}/lineas`, `/{id}/cerrar` | operario | conteo cíclico (ciego por defecto) |
| POST | `/api/conteos/{id}/{aplicar,reabrir,cancelar}` | **supervisor** | aplicar diferencias al stock |
| GET | `/api/pedidos?tipo=&abiertos=`, `/{id}` | operario | pedidos de COMPRA (recepción) / VENTA (picking, con sugerencia FEFO) |
| POST | `/api/pedidos/{id}/escaneos`, `/{id}/completar` | operario | recibir / pickear por escaneo (idempotente) |
| POST | `/api/pedidos`, `/{id}/cancelar` | **supervisor** | alta / cancelación de pedidos |
| POST | `/api/diagnostico/errores` | operario | crash reports de la app (van al log) |
| GET | `/api/operarios` | **admin panel** | listar operarios (incluye desactivados) |
| POST | `/api/operarios` | **admin panel** | alta de operario |
| PUT | `/api/operarios/{codigo}` | **admin panel** | editar nombre/rol/activo |
| PATCH | `/api/operarios/{codigo}/pin` | **admin panel** | resetear PIN |

Swagger UI: `http://localhost:8082/swagger-ui.html` — el botón **Authorize** toma
el token de `/api/auth/login`.

Público: `/api/auth/login`, `/api/panel/auth/login`, `/api/ping` y la
documentación. "operario" exige un `Authorization: Bearer <token>` del login de
la app; "admin panel" exige un token del login del panel — gateado en el
servidor con `@PreAuthorize("hasRole('PANEL')")`, no solo escondido en la UI;
un token de operario que llame esos endpoints directo recibe 403. El CRUD de
inventario acepta cualquiera de los dos tokens. "supervisor" = token del panel
**o** de un operario con rol `SUPERVISOR` (`hasAnyRole('PANEL','SUPERVISOR')`).

Tras 5 PIN fallidos seguidos para el mismo código (app o panel), ese código
queda bloqueado 5 minutos (HTTP 429). Configurable con `rf.panel.login.max-fallos`
y `rf.panel.login.bloqueo-minutos`.

## Login y roles

La app y el panel entran por **tablas y endpoints distintos**, con el mismo
formato de credencial (código + PIN numéricos, PIN con **BCrypt**):

| | Tabla | Endpoint | Semilla si la tabla está vacía |
|---|---|---|---|
| **Operario** (app Android) | `users` | `POST /api/auth/login` | `1001` / `1234` |
| **Administrador** (panel web) | `panel_admins` | `POST /api/panel/auth/login` | `9001` / `4321` |

El token del panel lleva `rol=PANEL` y es lo único que abre `/api/operarios/*`.
El token de la app lleva el `RolOperario` del operario: `SUPERVISOR` habilita en
la app aplicar conteos, borrar productos y gestionar ubicaciones/pedidos, pero
**no** da acceso al panel.

Las dos tablas **no se solapan**: antes un `SUPERVISOR` de `users` entraba al
panel; la migración `V5` crea `panel_admins` y **mueve** ahí a todos los
supervisores (los copia y los borra de `users`). El historial no se rompe
porque `stock_movements` guarda código y nombre del operario desnormalizados.

Todavía no hay endpoint para dar de alta administradores del panel: se hace por
SQL o con las variables `ADMIN_SEED_*`.

## Modelo de stock (V6)

- `products.quantity` es el **total**; el detalle vive en `stock_levels`
  (producto × ubicación × lote, con vencimiento opcional). Solo
  `InventarioService` los modifica, siempre con el producto bloqueado
  (`SELECT … FOR UPDATE`), y nunca deja una existencia en negativo.
- `products.location` es la ubicación **principal**: donde entra lo que no indica otra.
- Salidas sin lote/ubicación descuentan **FEFO** (vence antes, sale antes).
- Productos con `tracks_lots` exigen lote en cada entrada.
- `processed_operations` guarda los `clientOpId` aplicados: la cola offline de la
  app puede reintentar sin duplicar movimientos.

## Auditoría

Cada alta, edición, ajuste de stock y borrado queda en `stock_movements`: quién
(código y nombre del operario, tomados del token), cuándo, y el delta/cantidad
resultante. La fila sobrevive aunque el producto se borre después — es una
bitácora, no una vista sobre `products`.

## Docker

```bash
cd ..            # RF-Inventario-Web-Panel-Back/
docker compose up -d --build
```

Se conecta al MySQL del host por `host.docker.internal:3306`. No levanta su
propia base a propósito: sería una segunda verdad del inventario.

## Estructura

```
src/main/java/com/rf/panel/
├── RfInventarioPanelApiApplication.java
├── config/OpenApiConfig.java
├── controller/   AuthController · PanelAuthController · HealthController · ProductoController · OperarioAdminController
├── dto/          LoginRequest · SesionResponse · ProductoRequest/Response · AjusteStockRequest ·
│                 MovimientoStockResponse · ConflictoEdicionResponse · OperarioRequest/Response ·
│                 ActualizarOperarioRequest · ResetPinRequest · ErrorResponse
├── entity/       Producto (products) · Operario (users, con RolOperario) ·
│                 Administrador (panel_admins) · MovimientoStock (stock_movements)
├── exception/    GlobalExceptionHandler + excepciones de dominio (incl. ConflictoEdicionException)
├── repository/   ProductoRepository (+ lock de fila) · OperarioRepository ·
│                 AdministradorRepository · MovimientoStockRepository
├── security/     SecurityConfig (BCrypt, @EnableMethodSecurity) · JwtService · JwtAuthFilter · Sesion (con rol)
└── service/      AutenticacionService · AutenticacionPanelService · ProductoService ·
                  OperarioSeeder · AdministradorSeeder · OperarioAdminService
```
