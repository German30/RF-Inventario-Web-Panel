# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

```bash
npm start            # ng serve — dev server on http://localhost:4200 (proxies nothing; talks to backend directly)
npm run build        # ng build — production config by default, output to dist/
npm run watch        # ng build --watch --configuration development
npm test             # ng test — Karma + Jasmine (Chrome launcher), single run needs --watch=false
ng test --include='**/producto-form.component.spec.ts'   # run one spec file
ng generate component features/<area>/<name>             # scaffolds with SCSS style (configured default)
```

### Docker

```bash
docker compose up --build      # build image + serve on http://localhost:4200 (container listens on 8080)
docker build -t rf-inventario-panel .
```

Multi-stage `Dockerfile`: `node:22-alpine` runs `npm ci && npm run build` (production config), then the static `dist/rf-inventario-panel/browser` bundle is served by rootless `nginxinc/nginx-unprivileged` using `nginx.conf` (SPA fallback to `index.html`, immutable caching for hashed assets, `no-cache` for `index.html`). No API proxy — the browser calls the already-dockerized backend at `http://localhost:8082/` directly (baked in at build time via `environment.ts`). Host port 4200 is deliberate so the browser origin matches the backend's CORS allow-list. Rebuild the image to change the backend URL.

### Linting

There is **no linter or format check wired up**. Only Prettier config lives in `package.json` (`printWidth: 100`, `singleQuote`, Angular parser for HTML). TypeScript is fully `strict` plus `noPropertyAccessFromIndexSignature`, `noImplicitReturns`, and Angular `strictTemplates` — the compiler is the main safety net.

## Big-picture architecture

Angular 20 single-page app, **standalone components only** (no NgModules), **signals** for all component and service state, zone-based change detection with `eventCoalescing`. It is an admin panel that is a thin window onto an already-built Spring Boot + MySQL backend shared with the RF Inventario Android app — see `PRODUCT.md` for the domain and `DESIGN.md` for the visual system.

### Layers

- `src/app/core/` — cross-cutting singletons, all `providedIn: 'root'`:
  - `models/models.ts` — hand-written mirrors of the backend DTOs. **Field names stay in Spanish exactly as the API returns them; never remap to English.** Also defines the typed `ApiError` and `ConflictoEdicionError` classes thrown downstream.
  - `interceptors/auth.interceptor.ts` — attaches `Authorization: Bearer <token>` to every non-login request; on `401` (outside login) clears the session and routes to `/login`; converts every failed response into `ApiError`, and a product-edit `409` into `ConflictoEdicionError` carrying the server's current `ProductoResponse`. Components catch these typed errors, not raw `HttpErrorResponse`.
  - `services/` — one service per resource (`auth`, `producto`, `operario`), each a thin `HttpClient` wrapper returning `Observable`s. URLs built from `environment.apiBaseUrl` (note the trailing slash convention: `` `${base}api/productos` ``).
  - `guards/auth.guard.ts` — `CanActivateFn` gating the authenticated shell; redirects to `/login` via `createUrlTree`.
- `src/app/features/` — one folder per screen area. `*.page.ts` = routed page, `*.component.ts` = dialog/drawer/form used by a page. Pages hold the list + orchestrate modal signals; child components own their own form state and emit results via `output()`.
- `src/app/shared/components/` — presentational primitives: `modal`, `confirm`, `toast` (+ `toast.service.ts`), and `stamp` (`app-stamp`, the single status-badge component that replaces every ad-hoc colored chip — see DESIGN.md).

### Routing (`app.routes.ts`)

`/login` is public and lazy-loaded. Everything else is a child of the lazy `ShellComponent` (header + side nav + `<router-outlet>`), guarded by `authGuard`, with `/resumen` (default: KPIs, alerts, CSV downloads), `/inventario`, `/pedidos` (create purchase/sales orders the terminals process), `/conteos` (review and apply cycle counts), `/ubicaciones` and `/operarios` as feature routes. Warehouse endpoints are wrapped by `core/services/deposito.service.ts`. All routes use `loadComponent`.

### Auth & session model

`AuthService` is the source of truth. Session is persisted to `localStorage` under `rf-panel-sesion` and restored synchronously in the service's field initializer, so `estaLogueado()` is correct before the first guard runs.

**The panel has its own login**, separate from the Android app's: `AuthService.login()` calls `POST api/panel/auth/login` (numeric `codigo` + `pin`, accounts live in the backend's `panel_admins` table since migration V5) and expects `rol === 'PANEL'`; anything else throws a presentable error instead of storing the session. `restaurar()` applies the same check to persisted sessions. The interceptor treats any URL ending in `/auth/login` as the login call (no 401 bounce). JWT expires in ~12h backend-side; there is no refresh — a `401` just bounces to login.

### Optimistic locking (product edits)

`ProductoResponse.actualizadoEn` is a version stamp. `ProductoFormComponent` keeps it as `versionBase` and sends it back in `ProductoRequest.actualizadoEn`. If the edit lost a race the backend returns `409` + the real current product; the interceptor turns that into `ConflictoEdicionError`, and the form shows the conflict explicitly with a "use the server's values" action rather than a generic error. Do not swallow or generic-ify this path — surfacing write conflicts is a stated product principle.

### Backend dependency

Spring Boot API, dev default `http://localhost:8082/` (CORS already allows `http://localhost:4200`). Both `src/environments/environment.ts` and `environment.development.ts` currently point there; the production file is meant to be swapped in the build pipeline or served same-origin. Swagger UI at `/swagger-ui.html`. Seeded accounts: `9001`/`4321` (panel admin), `1001`/`1234` (app operator — does not exist for the panel login).

## Conventions

- **UI language is Spanish throughout** — identifiers, comments, user-facing strings. Reuse the domain nouns the app and backend already use (operario, producto, ajuste, historial, código, PIN, SKU, cantidad, ubicación, movimiento). Don't invent new labels.
- Component state: `signal()` / `computed()`; page components expose state as `protected readonly`. Reading an `input()` signal in a constructor is too early — use an `effect({ allowSignalWrites: true })` (see `ProductoFormComponent`).
- Errors reach the user via `ToastService` (`ok` / `error`); components type their `error` callbacks as `ApiError` and branch on `instanceof ConflictoEdicionError` where relevant.
- Follow `DESIGN.md`: one solid primary button per screen, `app-stamp` for all semantic status, `.num` (tabular-nums) for column-comparable values, no colored `border-left` accents, data table never collapses to cards.
