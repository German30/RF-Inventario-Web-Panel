# Product

<!-- impeccable:product-schema 1 -->

## Platform

web

## Stack

Angular (pedido explícito del usuario). Sin generación de imágenes disponible
en este entorno → build **code-first** (sin comp previo); no aplica pregunta
de buildPath.

## Users

Supervisores/administrativos de RF Inventario, trabajando desde una notebook o
PC de oficina. **No** son los operarios de piso — esos usan únicamente la app
Android con lector físico/cámara. El panel es exclusivamente para
`rol = SUPERVISOR` (confirmado por el usuario; gateado también en el
servidor, no solo en la UI).

Tarea típica: sentado en un escritorio, sin el celular ni el lector en la
mano, revisar o corregir el catálogo, investigar una diferencia de stock
mirando el historial, o dar de alta/baja a un operario de piso.

## Product Purpose

Panel de control administrativo de RF Inventario: hace desde un navegador lo
que si no requeriría operar físicamente el celular del almacén. CRUD completo
de productos, conciliación de stock vía historial de movimientos, y ciclo de
vida de cuentas de operarios (alta, edición de rol/estado, reseteo de PIN).
Éxito = un supervisor puede, sin moverse del escritorio, confirmar qué hay en
stock, ver quién lo tocó y cuándo, corregir errores, y dar de alta/baja
operarios de piso.

## Positioning

Superficie administrativa hermana de la app Android RF Inventario: comparten
un solo backend y una sola fuente de verdad (mismo MySQL, sin duplicar datos).
Lo que un panel de inventario genérico no podría afirmar: las cantidades que
muestra son literalmente las mismas que está escaneando el piso en tiempo
real, y el historial de auditoría traza a la misma identidad (código+PIN) que
se usa en el celular.

## Operating Context

- Herramienta interna, LAN/VPN, sin exposición pública, sin tienda de apps ni marketing.
- Backend ya construido y probado en vivo (Spring Boot, JWT Bearer, MySQL).
  URL configurable; default de desarrollo `http://localhost:8082`. CORS ya
  habilitado para `http://localhost:4200` (puerto default de Angular CLI).
- Dispositivo principal: navegador de escritorio en oficina. [Inferido: debe
  seguir siendo usable en una tablet si alguna oficina la usa, pero el target
  de optimización primario es escritorio — no se confirmó explícitamente.]
- Auth: JWT Bearer, vence a las 12h por defecto, se obtiene con
  `POST /api/auth/login` (código numérico + PIN numérico). El backend permite
  a cualquier cuenta activa autenticarse; el panel debe rechazar en el login
  a una cuenta con `rol=OPERARIO` con un mensaje claro ("esta cuenta no tiene
  acceso al panel"), ya que el producto es exclusivo de supervisores.
- Terminología (interfaz en español, la misma que ya usan la app y el
  backend): operario, supervisor, producto, código, PIN, SKU, cantidad,
  ubicación, movimiento/historial, ajuste de stock.

## Capabilities and Constraints

- Alcance v1 confirmado: login; listado/búsqueda/alta/edición/borrado de
  productos; ajuste de stock (por delta); historial de movimientos por
  producto; gestión de operarios (listar/crear/editar rol y activo/resetear PIN).
- Contrato de API ya implementado y verificado en vivo (15 endpoints; ver
  README y Swagger de `RF-Inventario-Web-Panel-Back`).
- Optimistic locking en edición de producto: el servidor responde 409 con el
  producto real cuando la edición perdió la carrera contra otro cambio — la UI
  tiene que mostrar ese conflicto explícitamente, no un error genérico.
- Sin paginación en el listado de productos todavía (el backend devuelve todo).
  [Inferido: aceptable para el tamaño de catálogo esperado en un almacén
  interno; no diseñar scroll infinito/skeletons pensando en escala que no
  está confirmada.]
- Sin fotos de producto (el modelo de datos no las tiene).
- Sin endpoints de dashboard/KPIs todavía — fuera de alcance de v1 salvo que se pida.
- Un supervisor no puede desautorizarse a sí mismo (bloqueado con 409 en el servidor).

## Brand Commitments

No existe un sistema de marca formal más allá del nombre "RF Inventario" y la
convención de español ya usada en toda la app y el backend.

El panel sí tiene, desde 2026-09-04, una preferencia de mundo visual fija
confirmada por el usuario: registro de **SaaS empresarial moderno**
(referencia explícita: Linear, Notion, Vercel) — superficies neutras frías,
un único acento de acción, tipografía de trabajo (no de marca), sin
ornamento ni metáfora temática. Reemplazó una primera dirección ("hoja de
conteo" tipo ledger de papel) que el usuario descartó explícitamente por no
leerse profesional. Ver `DESIGN.md` para los tokens; cualquier superficie
nueva del panel hereda este mundo por defecto, no el descartado.

## Evidence on Hand

- Backend real corriendo en `http://localhost:8082` (dev), con Swagger UI en
  `/swagger-ui.html` — contrato de API en vivo, no un mock.
- Cuentas sembradas: `1001`/`1234` (OPERARIO), `9001`/`4321` (SUPERVISOR).
- Sin mockups, sin capturas existentes, sin archivos de logo. No hay nada que
  inventar acá (ni testimonios, ni clientes, ni métricas): es una herramienta interna.

## Product Principles

1. Una sola fuente de verdad: el panel es una ventana al mismo inventario que
   opera el piso — nunca mostrar datos como si fueran en vivo si no lo son.
2. Corregir, no pisar en silencio: cada escritura (editar producto, ajustar
   stock, cambiar un operario) debe mostrar conflictos y consecuencias con
   claridad — el backend ya se niega a pisar cambios concurrentes; la UI no
   puede esconder ni suavizar eso.
3. Supervisores, no el público: diseñar para una audiencia interna chica, de
   confianza y uso repetido, en un escritorio — la densidad y velocidad para
   completar tareas importan más que persuadir u onboardear.
4. Toda escritura es atribuible: el historial existe porque "quién hizo esto y
   cuándo" siempre tiene que poder responderse — revisarlo debe ser una acción
   natural y a mano, no una función escondida.
5. Español consistente: reusar exactamente los sustantivos que ya usan la app
   y el backend (operario, producto, ajuste, historial), no inventar etiquetas nuevas.

## Accessibility & Inclusion

Sin estándar específico exigido. Herramienta interna de escritorio — aplica la
base sólida de teclado/lector de pantalla como parte del oficio normal, sin
requisito adicional registrado.
