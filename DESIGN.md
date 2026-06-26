# Design

Visual system for AI CarWash (`car-wash-frontend/`). Tokens are CSS custom properties in `src/index.css` (OKLCH components), mapped to Tailwind utilities in `tailwind.config.js`.

## Theme

Light, daylight-first. Pure white body; the brand lives in a fresh leaf-green primary and a water-blue accent. Mood: *fresh-cut lime on white ceramic — clean water, shaded forecourt*. Color strategy: **Restrained** (accent ≤10% of any screen); the Timah chat surface carries a slightly stronger accent presence because it is the brand's voice.

## Color

Tokens are stored as bare OKLCH components (`L C H`) so Tailwind can apply alpha: `oklch(var(--color-primary) / <alpha-value>)`.

| Token | OKLCH | Role |
|---|---|---|
| `bg` | `1 0 0` | body background (pure white) |
| `surface` | `0.972 0.004 110` | cards, panels, table headers |
| `surface-2` | `0.945 0.006 110` | hover rows, wells, skeletons |
| `border` | `0.90 0.008 110` | hairlines, input borders |
| `ink` | `0.24 0.015 110` | body text (≥7:1 on bg) |
| `muted` | `0.46 0.012 110` | secondary text (≥4.5:1 on bg) |
| `primary` | `0.52 0.11 112` | primary actions, links, selection — **white text on fills** |
| `primary-strong` | `0.45 0.105 112` | hover/active of primary |
| `primary-soft` | `0.95 0.035 112` | tinted chips/backgrounds |
| `primary-soft-ink` | `0.38 0.10 112` | text on `primary-soft` |
| `accent` | `0.58 0.10 230` | Timah presence, info, indicators |
| `accent-strong` | `0.48 0.10 235` | accent fills that carry text |
| `accent-soft` / `accent-soft-ink` | `0.945 0.03 230` / `0.38 0.09 235` | info badges |
| `success` / `success-soft` / `success-soft-ink` | `0.55 0.12 150` / `0.95 0.045 150` / `0.37 0.10 150` | completed payments |
| `warning` / `warning-soft` / `warning-soft-ink` | `0.62 0.13 75` / `0.955 0.05 85` / `0.42 0.10 70` | pending states, Worker badge |
| `danger` / `danger-soft` / `danger-soft-ink` | `0.55 0.18 27` / `0.955 0.02 25` / `0.44 0.16 27` | errors, failed payments |

Role badge mapping: OWNER → primary-soft, CLERK → accent-soft, WORKER → warning-soft, CUSTOMER → neutral (`surface-2` + `muted`).

## Typography

One family: **Inter** (variable, loaded in `index.html`), system-ui fallback. Fixed rem scale (Tailwind defaults, ratio ≈1.2): `text-sm` for UI/body-dense, `text-base` prose, `text-xl`–`text-2xl` page titles. `font-semibold` for headings and emphasis; `tabular-nums` on amounts and table figures. No display font.

## Components

- **Buttons**: `rounded-lg`; primary = primary fill + white text; secondary = white + border + ink; quiet = muted text, surface hover. All have visible `:focus-visible` ring (`ring-2 ring-primary ring-offset-2`), disabled at 50% opacity.
- **Inputs**: white, `border-border`, `rounded-lg`, focus ring in primary; always paired with a visible `<label>`.
- **Badges/pills**: `rounded-full px-2 py-0.5 text-xs font-medium`, soft bg + matching `-soft-ink` text.
- **Tables**: `surface` header row, hairline dividers, `surface-2` row hover; sortable headers are real `<button>`s with `aria-sort`; loading = skeleton rows (`surface-2` pulse), never spinner text.
- **Chat bubbles**: user = primary fill, white text, `rounded-2xl rounded-br-sm`; Timah = `surface` bubble, ink text, `rounded-2xl rounded-bl-sm`, avatar with accent ring.
- **Alerts**: `danger-soft` bg, `danger-soft-ink` text, `rounded-lg`, full border in danger/20.

## Layout

App shell: white top nav (h-16, hairline bottom border), content in `max-w-7xl` containers with `px-4 sm:px-6 lg:px-8 py-8`. Pages own their containers; the chat fills the remaining viewport (`flex-1 min-h-0`). Mobile: disclosure menu under the nav (no hidden functionality on small screens).

## Motion

150–250ms, `ease-out`, state changes only (hover, focus, menu open, typing indicator, streaming caret). Every animation has a `prefers-reduced-motion: reduce` fallback (static or crossfade). No page-load choreography.
