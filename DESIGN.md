---
name: RF Inventario · Panel
description: Panel administrativo para supervisores de almacén, en el registro de una consola de operaciones SaaS — neutros fríos, un único acento de acción, estados como fondo suave, sin ornamento.
colors:
  bg: "#f6f7f9"
  surface: "#ffffff"
  surface-hover: "#f3f4f6"
  surface-sunken: "#f9fafb"
  border: "#e5e7eb"
  border-strong: "#d1d5db"
  text: "#0f172a"
  text-secondary: "#475569"
  text-muted: "#64748b"
  accent: "#4f46e5"
  accent-hover: "#4338ca"
  accent-active: "#3730a3"
  accent-subtle: "#eef2ff"
  accent-subtle-border: "#c7d2fe"
  success-subtle: "#ecfdf5"
  success-subtle-fg: "#047857"
  success-subtle-border: "#a7f3d0"
  warning-subtle: "#fffbeb"
  warning-subtle-fg: "#b45309"
  warning-subtle-border: "#fde68a"
  danger: "#dc2626"
  danger-hover: "#b91c1c"
  danger-subtle: "#fef2f2"
  danger-subtle-fg: "#b91c1c"
  danger-subtle-border: "#fecaca"
  neutral-subtle: "#f1f5f9"
  neutral-subtle-fg: "#475569"
  neutral-subtle-border: "#e2e8f0"
typography:
  body:
    fontFamily: "'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif"
    fontSize: "14px"
    lineHeight: 1.5
  title:
    fontFamily: "{typography.body.fontFamily}"
    fontSize: "20px"
    fontWeight: 600
    letterSpacing: "-0.01em"
  label:
    fontFamily: "{typography.body.fontFamily}"
    fontSize: "13px"
    fontWeight: 500
rounded:
  sm: "6px"
  md: "8px"
  lg: "12px"
  full: "999px"
spacing:
  "1": "4px"
  "2": "8px"
  "3": "12px"
  "4": "16px"
  "5": "24px"
  "6": "32px"
  "7": "48px"
components:
  button-primary:
    backgroundColor: "{colors.accent}"
    textColor: "#ffffff"
    typography: "{typography.label}"
    rounded: "{rounded.md}"
    padding: "9px 16px"
  button-primary-hover:
    backgroundColor: "{colors.accent-hover}"
  button-danger:
    backgroundColor: "{colors.danger-subtle}"
    textColor: "{colors.danger-subtle-fg}"
    rounded: "{rounded.md}"
  button-ghost:
    backgroundColor: "transparent"
    textColor: "{colors.text-secondary}"
---

# Design System: RF Inventario · Panel

## Overview

**Creative North Star: "La consola de operaciones"**

El panel se presenta como lo que es: una consola de trabajo interna, en el mismo registro que Linear, Notion o Vercel — superficies neutras, un único acento de acción, y jerarquía construida con peso tipográfico y espaciado en vez de textura o metáfora. No hay un mundo temático que sostener; la tabla de inventario, los formularios y los estados son el producto, y el sistema existe para que se lean rápido y no se interpongan.

Este mundo **reemplaza** el sistema anterior ("Hoja de conteo": papel kraft, sellos de goma rotados, margen rojo de ledger, tipografía de máquina de escribir). Esa dirección quedó descartada por decisión explícita del usuario — se registra acá solo como anti-referencia, no como legado a conciliar. Cualquier pantalla nueva hereda este sistema, no el anterior.

La densidad sigue siendo alta y deliberada — es una herramienta de escritorio de uso repetido, no una vidriera — pero la lectura de un vistazo ahora viene de contraste tipográfico y de un badge de estado con fondo suave, nunca de un elemento decorativo.

**Key Characteristics:**
- Superficie neutra fría (fondo `#f6f7f9`, tarjetas blancas) con un único acento índigo para acción y foco
- Estados (rol, activo/baja, tipo de movimiento) como badge de fondo suave + texto de mayor contraste de la misma tinta — nunca un color plano ni un elemento rotado/"estampado"
- Un solo botón primario sólido por pantalla; el resto son ghost o bordeados
- Tipografía única (Inter) en toda la interfaz; los valores comparables en columna (SKU, cantidades, códigos, PIN) usan `font-variant-numeric: tabular-nums`, no una tipografía monoespaciada separada
- Iconos SVG dibujados a mano, trazo 1.75–2, un solo peso, nunca glifos Unicode ni emoji

## Colors

Paleta restringida: neutros fríos para toda superficie y texto, un único acento (índigo) para acción/foco, y estados semánticos expresados siempre como fondo suave + texto de mayor contraste de la misma tinta — nunca como color plano de fondo o borde de acento.

### Primary
- **Índigo de acción** `--accent` (#4f46e5): único color no neutro con licencia de fondo sólido. Botón primario, foco de teclado (anillo `--accent-subtle` a 3px), nav activo, selección de texto, ícono/marca de la consola.

### Neutral
- **Fondo de página** `--bg` (#f6f7f9): el lienzo detrás de toda pantalla.
- **Superficie** `--surface` (#ffffff): tarjetas, tablas, modales, drawer — un tono más claro que el fondo.
- **Superficie hover** `--surface-hover` (#f3f4f6) y **superficie hundida** `--surface-sunken` (#f9fafb): fila de tabla en hover, cabecera de tabla, nav lateral, estado vacío.
- **Borde** `--border` (#e5e7eb) y **borde fuerte** `--border-strong` (#d1d5db): división de tabla/tarjeta el primero, contorno de input y botón el segundo.
- **Texto** `--text` (#0f172a), **texto secundario** `--text-secondary` (#475569), **texto tenue** `--text-muted` (#64748b): jerarquía de lectura de mayor a menor peso informativo. `--text-muted` es el mínimo del sistema — no bajar de ahí sin repetir la verificación de contraste (ver Do's and Don'ts).

### Named Rules (optional, powerful)
**La Regla del Acento Único.** `--accent` es el único color con licencia de fondo sólido en un control. Todo estado semántico (éxito, alerta, error, rol) se expresa como fondo `-subtle` + texto `-subtle-fg` de esa misma familia — nunca como botón, badge o borde de color plano. Un segundo color con licencia de fondo sólido diluye cuál es la acción principal de la pantalla.

## Typography

**Body/UI Font:** Inter (con fallback -apple-system, BlinkMacSystemFont, Segoe UI, sans-serif)

**Character:** Una sola familia tipográfica de trabajo en toda la interfaz — sin par display/mono separado. La jerarquía se construye con tamaño, peso y color de texto, no con un cambio de voz tipográfica.

### Hierarchy
- **Título de página** (600, 20px, `-0.01em`): encabezado de sección ("Inventario", "Operarios"), sin kicker ni antetítulo — el título lleva su propio peso.
- **Body** (400, 14px, line-height 1.5): párrafos, celdas de tabla, mensajes.
- **Label** (500, 13px): etiquetas de campo de formulario, subtítulo de página.
- **Metadatos** (400–500, 12–12.5px, `--text-muted`): timestamps del historial, notas auxiliares.

### Named Rules (optional)
**La Regla del Número Tabular.** Cualquier valor comparable en columna (SKU, cantidad, código, PIN, delta de stock) lleva la clase utilitaria `.num` (`font-variant-numeric: tabular-nums`) en la misma Inter — nunca cambia a una tipografía monoespaciada aparte. El ancho por dígito alinea la columna sin introducir una segunda voz tipográfica.

## Layout

Layout de aplicación de una sola columna de contenido: header fijo (marca + identidad del operario) sobre un cuerpo de dos zonas — nav lateral de 208px con ícono + etiqueta, fondo `--surface-sunken`, y el contenido principal a ancho completo sobre `--bg`. El contenido casi siempre es una única tabla de datos de ancho completo o un formulario centrado en modal.

Densidad alta: padding de tabla `--space-3`/`--space-4` (12–16px), ritmo de 8 pasos (`--space-1` a `--space-7`, 4px a 48px) usado consistentemente.

Responsive: bajo ~720px el nav lateral pasa a una fila horizontal de dos ítems debajo del header y la identidad del operario se oculta (queda solo el botón de cerrar sesión); la tabla nunca colapsa a tarjetas — scrollea horizontalmente dentro de `.table-wrap`.

## Elevation & Depth

Sistema mayormente plano con una escala corta de sombras suaves de bajo contraste, siempre con offset y blur (nunca un halo de color a offset cero). La profundidad es sobre todo tonal (`--bg` vs `--surface` vs `--surface-sunken`) y la sombra solo aparece en superficies flotantes (modal, drawer, toast) o en reposo ligero (tarjeta de login, tabla).

### Shadow Vocabulary
- **`--shadow-xs`** (`0 1px 2px rgba(15,23,42,.06)`): borde inferior sutil, uso puntual.
- **`--shadow-sm`** (`0 1px 3px rgba(15,23,42,.08), 0 1px 2px rgba(15,23,42,.04)`): tarjetas y tablas en reposo.
- **`--shadow-md`** (`0 4px 12px -2px rgba(15,23,42,.10), 0 2px 4px -2px rgba(15,23,42,.06)`): tarjeta de login, toast.
- **`--shadow-lg`** (`0 16px 40px -12px rgba(15,23,42,.22), 0 4px 12px -4px rgba(15,23,42,.08)`): modal y drawer, superficies que flotan sobre un backdrop.

### Named Rules (optional)
**La Regla de la Sombra Doble.** Toda sombra del sistema combina un valor cercano de bajo blur con uno más difuso y desplazado — nunca un solo `box-shadow` de blur alto aislado. Esto evita el look "flotando sin peso" de una sombra genérica.

## Shapes

Radios consistentes por rol: `--radius-sm` (6px) en botones-ícono, `--radius-md` (8px) en botones/inputs/badges de estado con esquinas cerradas, `--radius-lg` (12px) en tarjetas/tablas/modales, `--radius-full` en el badge de estado (pill) y en el marcador de marca. Sin geometría irregular ni rotación: cada elemento tiene una forma estable, nunca "estampada".

## Components

### Buttons
- **Shape:** `--radius-md` (8px), borde 1px.
- **Primary (`.btn.btn--primary`):** fondo `--accent` sólido, texto blanco; hover `--accent-hover`, active `--accent-active`. Un solo botón primario por pantalla/formulario.
- **Ghost (`.btn.btn--ghost`):** transparente, texto `--text-secondary`; hover fondo `--surface-hover`. Usado para las acciones de fila (Stock/Historial/Editar/Eliminar/Resetear PIN) y como acción secundaria en formularios ("Cancelar").
- **Danger (`.btn.btn--danger`):** fondo `--danger-subtle` + texto `--danger-subtle-fg` en reposo; invierte a `--danger` sólido con texto blanco en hover. Reservado a confirmaciones destructivas.
- **Icon button (`.icon-btn`):** 30×30px, sin borde, `--radius-sm`; cerrar modal/drawer/toast.
- **Focus:** `outline: 2px solid var(--accent)` con offset 2px, siempre visible — es una herramienta navegada mucho por teclado.

### Cards / Containers
- **Corner Style:** `--radius-lg` (12px).
- **Background:** `--surface` sobre `--bg`.
- **Shadow Strategy:** `--shadow-sm` en reposo (login, tabla); `--shadow-lg` en superficies flotantes (modal, drawer).
- **Border:** 1px `--border`, neutro — sin acento de color en el borde de ninguna tarjeta.

### Inputs / Fields
- **Style:** caja completa — `border: 1px solid var(--border-strong)`, `--radius-md`, fondo `--surface`.
- **Focus:** borde `--accent` + anillo `box-shadow: 0 0 0 3px var(--accent-subtle)`.
- **Error:** texto `--danger-subtle-fg`, 12.5px, debajo del campo.
- **Con ícono:** `.field--icon` reserva 36px a la izquierda para un ícono SVG de 16px en `--text-muted` (usado en el buscador).

### Navigation
- **Nav lateral (`.nav-item`):** ícono 18px + etiqueta, `--radius-md`. Activo: fondo `--accent-subtle`, texto e ícono `--accent`/`--accent-active` — nunca un borde de acento, siempre relleno tonal completo.

### Badge de estado (`app-stamp`) — componente de firma
Reemplaza cualquier chip de color plano en todo el sistema (rol de operario, activo/baja, tipo de movimiento del historial). Pill (`--radius-full`), fondo `-subtle` + texto `-subtle-fg` + borde `-subtle-border` de una de cinco familias: `ok` (verde), `warn` (ámbar), `danger` (rojo), `role` (índigo), `neutral` (gris). Sin rotación, sin `mix-blend-mode`, sin reborde irregular.

### Data table (`.data-table` / `.table-wrap`)
La superficie central del producto (Inventario, Operarios). Cabecera `--surface-sunken` con texto `--text-muted` 12px, filas separadas por `--border` 1px, hover con `--surface-sunken`. Columnas numéricas alineadas a la derecha con `.num`. Nunca colapsa a tarjetas en mobile — scrollea horizontalmente dentro de `.table-wrap`, que mantiene el radio y la sombra de tarjeta.

## Do's and Don'ts

### Do:
- **Do** expresar todo estado semántico (éxito/alerta/error/rol) como fondo `-subtle` + texto `-subtle-fg` de la misma familia — usar `app-stamp`, nunca un badge o chip ad-hoc nuevo.
- **Do** usar `.num` (tabular-nums) para cualquier valor comparable en columna, en la misma Inter — nunca introducir una tipografía monoespaciada aparte.
- **Do** mantener un solo botón `.btn--primary` sólido por pantalla o formulario; el resto en `--ghost` o `--danger`.
- **Do** verificar contraste real (≥4.5:1 texto normal, ≥3:1 texto grande) antes de introducir un nuevo tono de `--text-muted` o más claro: `--text-muted` (#64748b) ya es el piso del sistema (~4.6:1 sobre `--surface`).

### Don't:
- **Don't** reintroducir el sistema "Hoja de conteo" (papel, sellos rotados, margen rojo de ledger, tipografía typewriter) en ninguna pantalla nueva — quedó reemplazado, no es un modo alternativo del sistema.
- **Don't** usar un kicker o antetítulo en mayúsculas encima de un título de página o de sección — el título lleva su propio peso; si hace falta contexto adicional, va como subtítulo debajo.
- **Don't** ponerle un `border-left` de color a una tarjeta, fila de tabla, alerta o callout — el acento y los estados semánticos se expresan con fondo, nunca con una barra lateral.
- **Don't** hacer que la tabla de datos colapse a tarjetas en mobile — debe scrollear horizontalmente y mantener su identidad de tabla en cualquier viewport.
