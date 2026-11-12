# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

Backend REST API (`com.rf:rf-inventario-panel-api`, Java 21 / Spring Boot 3.4.5) for the RF Inventario web admin panel. It is also the only way the RF Inventario Android app reaches the inventory — the app talks HTTP to this service and no longer touches MySQL directly. This service is the **sole owner of the database schema** (via Flyway) and of the DB credentials.

The MySQL database (`rf_inventario`) is **shared with the Android app** and lives on the host, not in this repo's compose file — duplicating it would create a second source of truth. `docker compose up` here starts only the API and connects to the host MySQL over `host.docker.internal:3306`.

## Commands

```bash
./mvnw spring-boot:run                 # run locally on :8082 (needs a reachable MySQL)
./mvnw clean verify                    # compile + run all tests (what CI runs; H2, no MySQL needed)
./mvnw test                            # tests only
./mvnw test -Dtest=ProductoServiceTest # single test class
./mvnw test -Dtest=ProductoServiceTest#nombreDelMetodo   # single test method
./mvnw clean package                   # build the runnable jar -> target/rf-inventario-panel-api-0.0.1-SNAPSHOT.jar
docker compose up -d --build           # run containerized against host MySQL
```

There is no linter/formatter configured. CI (`.github/workflows/backend-ci.yml`) runs `./mvnw -B -ntp clean verify` on push/PR to `main` and uploads the jar.

Config is env-var driven with dev defaults baked into `application.properties`; see `.env.example`. Key vars: `DB_HOST/DB_PORT/DB_NAME/DB_USER/DB_PASSWORD`, `SERVER_PORT` (8082), `CORS_ORIGINS`, `JWT_SECRETO` (32+ chars, HS256), `JWT_HORAS`, `SWAGGER_ENABLED`, `SEED_CODIGO/SEED_PIN/SEED_NOMBRE`.

Swagger UI: `http://localhost:8082/swagger-ui.html` (Authorize button takes the token from `/api/auth/login`).

## Language conventions

Domain code, identifiers, comments, and API JSON fields are in **Spanish** (`Producto`, `Operario`, `crear`, `ajustarStock`, `codigo`, `cantidad`). The **database columns are English** (`products.name`, `products.quantity`, `users.code`, `users.pin_hash`) because the Android app (`InventarioDatabase.kt`) defined that schema first; JPA entities bridge the two with explicit `@Column(name = ...)`. Match the surrounding style when editing.

## Architecture

Standard layered Spring MVC: `controller` → `service` (holds `@Transactional` boundaries and all business rules) → `repository` (Spring Data JPA) → `entity`. DTOs are Java records in `dto/`; entities are never serialized directly — each response DTO has a static `from(entity)` factory. Controllers are thin and carry the OpenAPI annotations.

### Schema ownership: Flyway vs. Hibernate

- **Production/dev (MySQL):** Flyway in `src/main/resources/db/migration/` owns the schema. Hibernate runs `ddl-auto=validate` — it never alters tables, only fails startup if an entity drifts from the schema. **Any schema change is a new `V7__*.sql` file (V6 is the latest); never edit an applied migration.**
- **`baseline-version=1`:** the dev DB already had `products`/`users` before Flyway was adopted, so `V1__baseline.sql` is skipped there and migrations start at V2. A fresh empty DB runs V1–V5 and lands in the same state.
- **Tests (H2):** `src/test/resources/application.properties` disables Flyway (the migrations are MySQL SQL) and lets Hibernate generate the H2 schema from the entities with `create-drop`.

### Auth & roles — two separate identity tables

The Android app and the web panel log in against **different tables**, each with its own endpoint but the same credential shape (numeric `code` + BCrypt `pin_hash` + `name` + `active`):

| Identity | Table | Entity / repo | Login | Auth service | Seeder |
|---|---|---|---|---|---|
| App operator | `users` | `Operario` / `OperarioRepository` | `POST /api/auth/login`, `GET /api/auth/yo` | `AutenticacionService` | `OperarioSeeder` (`rf.panel.seed.*`, default `1001`) |
| Panel admin | `panel_admins` | `Administrador` / `AdministradorRepository` | `POST /api/panel/auth/login`, `GET /api/panel/auth/yo` | `AutenticacionPanelService` | `AdministradorSeeder` (`rf.panel.admin-seed.*`, default `9001`) |

Both auth services do the same timing-safe check (compare against a dummy BCrypt hash when the code is unknown, so response time doesn't leak which field was wrong). Both call `JwtService.emitir(codigo, nombre, rol)` (stateless JWT, jjwt, HS256).

`JwtAuthFilter` reads `Authorization: Bearer <token>`, rebuilds a `Sesion` record (`codigo`, `nombre`, `rol`) as the authentication principal (`@AuthenticationPrincipal Sesion`), and grants **exactly one authority per token**: `ROLE_PANEL` when `rol` is `Sesion.ROL_PANEL` (`"PANEL"`, set by the panel login), otherwise `ROLE_OPERARIO`. There is no role hierarchy.

- Inventory endpoints (`/api/productos/**`) require only authentication — both token kinds pass; the acting `Sesion` (whoever it is) is recorded in `stock_movements`.
- `/api/operarios/**` (operator admin) is gated with `@PreAuthorize("hasRole('PANEL')")` (`@EnableMethodSecurity` in `SecurityConfig`) — an operator token gets 403.
- Public routes: `/api/auth/login`, `/api/panel/auth/login`, `/api/ping`, Swagger.

The two tables **do not overlap**: `V5__panel_admins.sql` creates `panel_admins`, then *moves* every `users` row with `role = 'SUPERVISOR'` into it (copy, then `DELETE FROM users`). `stock_movements` denormalizes operator code/name so the audit log survives the delete. `users.role` (`RolOperario`, `OPERARIO`|`SUPERVISOR`, added in V4) is kept as an app-internal designation but **no longer grants panel access** and should only be `OPERARIO` from V5 on. There is currently no CRUD endpoint for panel admins — add them via SQL or seed config.

### Stock model (V6) — read before touching stock

`InventarioService` is the **only** writer of `stock_levels` (`Existencia`) and of `Producto.cantidad` (which is the denormalized total of its levels). Products, counts (`ConteoService`) and orders (`PedidoService`) all delegate to it. Rules it enforces: lock the product row first (`bloquear` → `findByIdForUpdate`), never let a level go negative, outbound without explicit lot/location is FEFO (`ExistenciaRepository.paraDescontar`), lot-tracked products require a lot on inbound, and every moved part writes a `MovimientoStock`. `products.location` is the product's *main* location (default target for inbound).

Idempotency: app operations carry a `clientOpId`; `InventarioService.yaProcesada/marcarProcesada` (table `processed_operations`) make retries from the app's offline queue no-ops. Orders and counts also lock their own row (`findByIdForUpdate`) so concurrent scans on the same order don't over-receive.

Roles: `JwtAuthFilter` grants `ROLE_PANEL` for panel tokens, `ROLE_OPERARIO` (+ `ROLE_SUPERVISOR` when the operator's `rol` is `SUPERVISOR`) for app tokens. Supervisor-only endpoints use `@PreAuthorize("hasAnyRole('PANEL','SUPERVISOR')")`. Failed logins are throttled per code by `IntentosLogin` (in-memory; 429 via `DemasiadosIntentosException`).

### Concurrency on stock

`Producto.actualizadoEn` (epoch millis, written by app or backend, not the DB) doubles as an **optimistic-locking version**. `PUT /api/productos/{id}` sends the value it read back as `actualizadoEn`; if it no longer matches, the service throws `ConflictoEdicionException` → HTTP 409 with a `ConflictoEdicionResponse` body carrying the current product state (not the plain `ErrorResponse`), so the client can reconcile.

`PATCH /api/productos/{id}/stock` (delta adjust) instead uses a **pessimistic row lock**: `ProductoRepository.findByIdForUpdate` (`@Lock(PESSIMISTIC_WRITE)`, `SELECT ... FOR UPDATE`) so two concurrent adjustments can't both read the same starting quantity and lose one write. Stock is never allowed to go negative.

### Audit trail

Every create / edit / stock-adjust / delete writes a `MovimientoStock` row (`stock_movements` table, added in V3) via `ProductoService.registrar`: who (codigo + nombre from the `Sesion`), when, the delta, and the resulting quantity. **The row is written before a delete and is never cascaded away** — it's a log, not a view over `products`, so history survives the product.

### Error handling

`GlobalExceptionHandler` (`@RestControllerAdvice`) maps domain exceptions to status + `ErrorResponse` JSON: `CredencialesInvalidasException` → 401, `RecursoNoEncontradoException` → 404, `ReglaNegocioException` (duplicate SKU, negative stock, self-deactivation) → 409, bean-validation failures → 400 with a field-error map. `SecurityConfig` handles the pre-controller 401/403 (bad/absent token, denied role) with the same `ErrorResponse` shape.

### Tests

Service tests use `@DataJpaTest` against real H2 (not mocks) because optimistic locking and audit rows depend on Hibernate/H2 actually assigning ids and timestamps. `RfInventarioPanelApiApplicationTests` is the context-load smoke test.
