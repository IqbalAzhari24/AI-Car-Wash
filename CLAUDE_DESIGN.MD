# CLAUDE_DESIGN.MD — AI CarWash Visual Design System
> **Scope:** Visual design authority for all UI work on AzureWash.
> **Load order:** Load `CLAUDE.md` first, then this file. Both MUST be active for any frontend task.
> **Target:** Claude Code, Claude.ai Projects — React 19 + Tailwind CSS (mobile-first)
> **Version:** 2.0.0 | Updated: 2026-06-19 | Redesigned with ui-ux-pro-max

---

## ⚡ DESIGN PRIMACY RULES

These rules override any default Tailwind choices or Claude aesthetic preferences. NEVER violate them.

1. **Dark mode is the ONLY mode.** There is no light mode. Do not add `dark:` variants — the entire system IS dark.
2. **Mobile-first, always.** Design the mobile layout before the desktop layout. Every component must work at 375px width.
3. **No white backgrounds. Ever.** If you reach for `bg-white`, stop. Use `bg-surface` or `bg-base` from the token table.
4. **Cyan is for action only.** `#00F0FF` is reserved for interactive controls, active states, and glow indicators. Do NOT use it as decoration.
5. **Honeycomb geometry is the texture layer.** Every panel background uses the hex-grid pattern. No plain flat fills on card surfaces.
6. **No fabricated color values.** Every hex value used MUST come from the token table in this file. If a color is not listed, ask before using it.
7. **Touch targets are non-negotiable.** All interactive elements must be minimum 44×44px (Apple HIG) / 48×48dp (Material). This is a HARD constraint.
8. **Tailwind arbitrary values are permitted ONLY for radial gradients and hex-clip geometry.** All other values MUST use design token classes.
9. **Verify before delivering.** Before shipping any component or page, run the Design Pre-Delivery Checklist at the bottom of this file.

---

## 🏗️ Design Philosophy

AI CarWash is a **car wash booking and management platform** serving Malaysian customers, clerks, workers, and owners. The aesthetic is **Cyber-Organic** — precision dark surfaces with neon cyan highlights, honeycomb geometry, and clean data hierarchy. The product serves two UX contexts:

| Context | Primary Users | Device | Priority |
|---|---|---|---|
| **Consumer** | CUSTOMER booking flow | Mobile (375–430px) | Clarity, speed, trust |
| **Operations** | CLERK, WORKER dashboards | Tablet + Mobile | Dense data, quick actions |
| **Analytics** | OWNER portal | Desktop (1280px+) | Data density, charts |

Every design decision must serve one of these contexts explicitly.

---

## 🎨 Color Token System

### Core Palette — ONLY These Values

| Token Name | Hex | Tailwind Class | Role |
|---|---|---|---|
| `color-base` | `#0D0D11` | `bg-base` | Page/app background — matte black void |
| `color-surface` | `#13131A` | `bg-surface` | Card / panel background |
| `color-surface-raised` | `#1A1A24` | `bg-surface-raised` | Elevated card, modal, drawer |
| `color-surface-hover` | `#1F1F2E` | `bg-surface-hover` | Interactive surface hover state |
| `color-border` | `#2A2A3D` | `border-border` | Default structural borders |
| `color-border-subtle` | `#1E1E2D` | `border-border-subtle` | Dividers, section separators |
| `color-slate` | `#3D4A6B` | `border-slate` / `text-slate` | Muted slate blue — structural dividers |
| `color-cyan` | `#00F0FF` | `text-cyan` / `border-cyan` / `ring-cyan` | Neon cyan — actionable controls ONLY |
| `color-cyan-glow` | `#00F0FF` at 20% opacity | `shadow-cyan-glow` | Glow halo on active elements |
| `color-cyan-dim` | `#00B8C4` | `text-cyan-dim` | Subdued cyan — disabled/inactive action labels |
| `color-text-primary` | `#E8E8F0` | `text-primary` | Headings, primary body copy |
| `color-text-secondary` | `#9090A8` | `text-secondary` | Supporting text, metadata |
| `color-text-muted` | `#5A5A72` | `text-muted` | Placeholders, disabled labels |
| `color-success` | `#00E5A0` | `text-success` / `border-success` | Confirmed, completed states |
| `color-warning` | `#FFB800` | `text-warning` / `border-warning` | Pending, in-progress states |
| `color-danger` | `#FF4466` | `text-danger` / `border-danger` | Error, cancelled, destructive |
| `color-info` | `#7B8CDE` | `text-info` / `border-info` | Informational, neutral notices |

### Contrast Verification (WCAG AA Required)

| Pair | Ratio | Status |
|---|---|---|
| `#E8E8F0` on `#13131A` (primary text on surface) | ≥ 11:1 | ✅ Pass |
| `#9090A8` on `#13131A` (secondary text on surface) | ≥ 4.5:1 | ✅ Pass — verify before use |
| `#00F0FF` on `#0D0D11` (cyan on base) | ≥ 6:1 | ✅ Pass |
| `#00E5A0` on `#13131A` (success on surface) | ≥ 4.5:1 | ✅ Pass |
| `#FFB800` on `#13131A` (warning on surface) | ≥ 4.5:1 | ✅ Pass |
| `#FF4466` on `#13131A` (danger on surface) | ≥ 4.5:1 | ✅ Pass |
| `#5A5A72` on `#13131A` (muted — placeholder ONLY) | < 4.5:1 | ⚠️ Decorative use only — NEVER for readable text |

### Tailwind Config Extension (`tailwind.config.js`)

```js
// theme.extend
colors: {
  base:             '#0D0D11',
  surface:          '#13131A',
  'surface-raised': '#1A1A24',
  'surface-hover':  '#1F1F2E',
  border:           '#2A2A3D',
  'border-subtle':  '#1E1E2D',
  slate:            '#3D4A6B',
  cyan:             '#00F0FF',
  'cyan-dim':       '#00B8C4',
  primary:          '#E8E8F0',
  secondary:        '#9090A8',
  muted:            '#5A5A72',
  success:          '#00E5A0',
  warning:          '#FFB800',
  danger:           '#FF4466',
  info:             '#7B8CDE',
},
boxShadow: {
  'cyan-glow':     '0 0 12px rgba(0, 240, 255, 0.20), 0 0 24px rgba(0, 240, 255, 0.08)',
  'cyan-glow-lg':  '0 0 20px rgba(0, 240, 255, 0.30), 0 0 48px rgba(0, 240, 255, 0.10)',
  'panel':         '0 1px 0 rgba(0, 240, 255, 0.05), inset 0 1px 0 rgba(255,255,255,0.03)',
  'success-glow':  '0 0 10px rgba(0, 229, 160, 0.20)',
  'danger-glow':   '0 0 10px rgba(255, 68, 102, 0.20)',
},
screens: {
  'xs':  '375px',   // small phone (iPhone SE)
  'sm':  '430px',   // standard phone (iPhone 15)
  'md':  '768px',   // tablet
  'lg':  '1024px',  // large tablet / small laptop
  'xl':  '1280px',  // desktop
  '2xl': '1440px',  // wide desktop
},
```

---

## 🔷 Cyber-Honeycomb Texture System

The defining visual texture of AzureWash. Every card panel MUST use one of the following texture classes.

### Global CSS (`index.css` / `globals.css`)

```css
/* ─── Honeycomb dot-grid overlay ─────────────────────── */
.hex-grid {
  background-color: #13131A;
  background-image:
    radial-gradient(circle at 1px 1px, rgba(0, 240, 255, 0.07) 1px, transparent 0);
  background-size: 28px 28px;
}

/* Denser grid for hero / header panels */
.hex-grid-dense {
  background-color: #0D0D11;
  background-image:
    radial-gradient(circle at 1px 1px, rgba(0, 240, 255, 0.05) 1px, transparent 0);
  background-size: 18px 18px;
}

/* Subtle grid for mobile bottom sheets — less visual noise on small screens */
.hex-grid-subtle {
  background-color: #1A1A24;
  background-image:
    radial-gradient(circle at 1px 1px, rgba(0, 240, 255, 0.04) 1px, transparent 0);
  background-size: 22px 22px;
}

/* ─── Border states ───────────────────────────────────── */
.hex-border {
  border: 1px solid rgba(0, 240, 255, 0.18);
  box-shadow: 0 0 12px rgba(0, 240, 255, 0.10), inset 0 1px 0 rgba(255,255,255,0.03);
}

.hex-border-active {
  border: 1px solid rgba(0, 240, 255, 0.55);
  box-shadow: 0 0 20px rgba(0, 240, 255, 0.25), inset 0 1px 0 rgba(0, 240, 255, 0.05);
}

.hex-border-success {
  border: 1px solid rgba(0, 229, 160, 0.40);
  box-shadow: 0 0 12px rgba(0, 229, 160, 0.15);
}

.hex-border-danger {
  border: 1px solid rgba(255, 68, 102, 0.40);
  box-shadow: 0 0 12px rgba(255, 68, 102, 0.15);
}

/* ─── Radial glow overlays ───────────────────────────── */
.hex-radial-core {
  background:
    radial-gradient(ellipse 60% 40% at 50% 0%, rgba(0, 240, 255, 0.07) 0%, transparent 70%),
    #13131A;
}

.hex-radial-success {
  background:
    radial-gradient(ellipse 60% 40% at 50% 0%, rgba(0, 229, 160, 0.06) 0%, transparent 70%),
    #13131A;
}

/* ─── Corner accent marker ───────────────────────────── */
.hex-corner {
  position: relative;
  overflow: hidden;
}
.hex-corner::before {
  content: '';
  position: absolute;
  top: 0; left: 0;
  width: 32px; height: 32px;
  background: linear-gradient(135deg, rgba(0, 240, 255, 0.25) 0%, transparent 60%);
  clip-path: polygon(0 0, 100% 0, 0 100%);
  pointer-events: none;
}

/* ─── Safe area utilities (mobile notch / home bar) ─── */
.safe-bottom { padding-bottom: env(safe-area-inset-bottom); }
.safe-top    { padding-top:    env(safe-area-inset-top);    }

/* ─── Reduced motion ─────────────────────────────────── */
@media (prefers-reduced-motion: reduce) {
  *, *::before, *::after {
    animation-duration: 0.01ms !important;
    transition-duration: 0.01ms !important;
  }
}
```

### Texture Application Rules

| Context | Texture Class | Border Class |
|---|---|---|
| Standard card / booking panel | `hex-grid hex-corner` | `hex-border` |
| Page / section hero | `hex-grid-dense` | none |
| Active / selected card | `hex-grid hex-radial-core` | `hex-border-active` |
| Completed booking / success state | `hex-radial-success` | `hex-border-success` |
| Modal / drawer overlay | `hex-grid-subtle` | `hex-border` + backdrop blur |
| Sidebar navigation (desktop) | `hex-grid-dense` | right `border-slate` |
| Bottom sheet (mobile) | `hex-grid-subtle` | top `hex-border` |
| Data table rows | none — keep clean | `border-border-subtle` row dividers |
| KPI / stat widget | `hex-radial-core` | `hex-border` |
| Toast / notification | `hex-grid-subtle` | `hex-border` |

---

## ✏️ Typography System

### Font Stack

```js
// tailwind.config.js — fontFamily
fontFamily: {
  sans:    ['Inter', 'system-ui', 'sans-serif'],
  mono:    ['JetBrains Mono', 'Fira Code', 'monospace'],
  display: ['Space Grotesk', 'Inter', 'sans-serif'],
}
```

```html
<!-- index.html — preload critical fonts -->
<link rel="preconnect" href="https://fonts.googleapis.com" />
<link rel="preconnect" href="https://fonts.gstatic.com" crossorigin />
<link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600&family=Space+Grotesk:wght@500;600;700&family=JetBrains+Mono:wght@400;500&display=swap" rel="stylesheet" />
```

### Type Scale

| Role | Class | Weight | Font | Min Touch-Safe |
|---|---|---|---|---|
| Page title / H1 | `text-2xl sm:text-3xl font-display font-bold text-primary leading-tight` | 700 | Space Grotesk | — |
| Section heading / H2 | `text-xl font-display font-semibold text-primary` | 600 | Space Grotesk | — |
| Card title / H3 | `text-base font-sans font-semibold text-primary` | 600 | Inter | — |
| Body copy | `text-sm font-sans font-normal text-secondary leading-relaxed` | 400 | Inter | Min 16px on mobile (use `text-base` for mobile body to avoid iOS zoom) |
| Caption / metadata | `text-xs font-sans font-normal text-muted` | 400 | Inter | Decorative only — not for critical info |
| Badge / label | `text-xs font-sans font-medium tracking-wide uppercase` | 500 | Inter | — |
| Code / ID / reference | `text-xs font-mono text-cyan` | 400 | JetBrains Mono | — |
| KPI stat / number | `text-2xl sm:text-3xl font-display font-bold text-primary tabular-nums` | 700 | Space Grotesk | Use `tabular-nums` for data columns |
| Mobile body copy | `text-base font-sans font-normal text-secondary leading-relaxed` | 400 | Inter | Required at <768px |

### Line Length Constraints

- **Mobile (375–768px):** 35–55 characters per line — use `max-w-[65ch]` or rely on viewport padding
- **Desktop (≥768px):** 60–75 characters per line for prose blocks

---

## 📐 Spacing & Layout System

### Spacing Scale (8pt base — Material Design)

| Token | Value | Tailwind | Use |
|---|---|---|---|
| `space-1` | 4px | `p-1 / gap-1` | Icon gap, tight label padding |
| `space-2` | 8px | `p-2 / gap-2` | Minimum touch spacing between targets |
| `space-3` | 12px | `p-3 / gap-3` | Compact internal padding |
| `space-4` | 16px | `p-4 / gap-4` | Standard internal padding |
| `space-5` | 20px | `p-5 / gap-5` | Card content padding (mobile) |
| `space-6` | 24px | `p-6 / gap-6` | Card content padding (desktop) |
| `space-8` | 32px | `p-8 / gap-8` | Section separation |
| `space-12` | 48px | `p-12 / gap-12` | Page-level vertical rhythm |

### Breakpoints

| Name | Value | Context |
|---|---|---|
| `xs` | 375px | Small phone (iPhone SE baseline) |
| `sm` | 430px | Standard phone |
| `md` | 768px | Tablet / split view |
| `lg` | 1024px | Large tablet / small laptop |
| `xl` | 1280px | Desktop |
| `2xl` | 1440px | Wide desktop |

### Responsive App Shell

#### Mobile Customer Shell (default — no sidebar)

```jsx
{/* Root shell — mobile first */}
<div className="min-h-dvh bg-base text-primary font-sans">

  {/* Top bar — mobile */}
  <header className="fixed top-0 inset-x-0 z-30 h-14 flex items-center px-4
    bg-base/90 backdrop-blur-md border-b border-border-subtle safe-top">
    {/* Logo, page title, avatar */}
  </header>

  {/* Scrollable page content */}
  <main className="pt-14 pb-20 px-4 sm:px-6 min-h-dvh">
    {/* page content */}
  </main>

  {/* Bottom navigation — CUSTOMER / WORKER only */}
  <nav className="fixed bottom-0 inset-x-0 z-30 h-16 flex items-center
    bg-surface border-t border-border-subtle safe-bottom">
    {/* Bottom nav items — max 4 */}
  </nav>

</div>
```

#### Desktop Operations Shell (CLERK / OWNER — md+)

```jsx
<div className="min-h-dvh bg-base text-primary font-sans flex">

  {/* Sidebar — hidden on mobile, shown at md+ */}
  <aside className="hidden md:flex fixed inset-y-0 left-0 w-64 flex-col
    hex-grid-dense border-r border-slate z-20">
    {/* nav */}
  </aside>

  {/* Main content area */}
  <main className="w-full md:ml-64 min-h-dvh p-4 md:p-8">
    {/* page content */}
  </main>

</div>
```

### Page Grid

```jsx
{/* Stats row — stacks to 1-col on mobile */}
<div className="grid grid-cols-2 md:grid-cols-4 gap-4">
  <StatWidget /> {/* × 4 */}
</div>

{/* Content split — stacks on mobile */}
<div className="grid grid-cols-1 lg:grid-cols-12 gap-6 mt-6">
  <div className="lg:col-span-8"> {/* main panel */} </div>
  <div className="lg:col-span-4"> {/* side panel */} </div>
</div>
```

---

## 🧩 Component Patterns

### Touch Target Rule (HARD CONSTRAINT)
All interactive elements — buttons, links, nav items, checkboxes, dropdowns — must hit **44×44px minimum**. Use padding to extend visual elements that are smaller. NEVER remove padding just to "tighten" a compact layout.

---

### Card — Standard Panel

```jsx
<div className="relative hex-grid hex-border hex-corner rounded-xl p-5 sm:p-6 overflow-hidden">
  <h3 className="text-base font-semibold text-primary mb-1">Panel Title</h3>
  <p className="text-sm text-secondary leading-relaxed">Supporting description text.</p>
</div>
```

### Card — Active / Selected State

```jsx
<div className="relative hex-grid hex-radial-core hex-border-active hex-corner rounded-xl p-5 sm:p-6 overflow-hidden">
  {/* content */}
</div>
```

### Card — Completed / Success State

```jsx
<div className="relative hex-radial-success hex-border-success hex-corner rounded-xl p-5 sm:p-6 overflow-hidden">
  {/* content */}
</div>
```

---

### Button — Primary (Cyan Action)

```jsx
{/* min-h-[44px] ensures touch target compliance */}
<button className="
  inline-flex items-center justify-center gap-2
  min-h-[44px] px-5 py-2.5
  bg-transparent border border-cyan text-cyan
  rounded-lg text-sm font-medium font-sans
  shadow-cyan-glow
  hover:bg-cyan hover:text-base
  active:scale-[0.98]
  transition-all duration-150
  focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-cyan
  focus-visible:ring-offset-2 focus-visible:ring-offset-base
  disabled:opacity-40 disabled:pointer-events-none
">
  Action Label
</button>
```

### Button — Primary Loading State

```jsx
<button disabled className="
  inline-flex items-center justify-center gap-2
  min-h-[44px] px-5 py-2.5
  bg-cyan/20 border border-cyan/40 text-cyan-dim
  rounded-lg text-sm font-medium font-sans
  cursor-not-allowed
">
  <svg className="w-4 h-4 animate-spin" viewBox="0 0 24 24" fill="none">
    <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
    <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0..." />
  </svg>
  Processing...
</button>
```

### Button — Secondary (Slate Structural)

```jsx
<button className="
  inline-flex items-center justify-center gap-2
  min-h-[44px] px-5 py-2.5
  bg-surface-raised border border-slate text-secondary
  rounded-lg text-sm font-medium font-sans
  hover:border-border hover:text-primary
  active:scale-[0.98]
  transition-all duration-150
  focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-slate
  focus-visible:ring-offset-2 focus-visible:ring-offset-base
  disabled:opacity-40 disabled:pointer-events-none
">
  Secondary Action
</button>
```

### Button — Danger (Destructive)

```jsx
<button className="
  inline-flex items-center justify-center gap-2
  min-h-[44px] px-5 py-2.5
  bg-transparent border border-danger text-danger
  rounded-lg text-sm font-medium font-sans
  hover:bg-danger hover:text-base
  active:scale-[0.98]
  transition-all duration-150
  focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-danger
  focus-visible:ring-offset-2 focus-visible:ring-offset-base
">
  Cancel Booking
</button>
```

### Button — Ghost / Text (Tertiary)

```jsx
<button className="
  inline-flex items-center gap-1.5
  min-h-[44px] px-3
  text-secondary text-sm font-medium
  hover:text-primary
  transition-colors duration-150
  focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-slate
  focus-visible:ring-offset-2 focus-visible:ring-offset-base
">
  View details
</button>
```

---

### Status Badge (Booking State Machine)

```jsx
// State → visual mapping
// PENDING    → warning  (amber)
// CONFIRMED  → cyan     (active pulse)
// IN_PROGRESS→ cyan     (active pulse, bold border)
// COMPLETED  → success  (static, no pulse)
// CANCELLED  → danger   (static)
// NO_SHOW    → muted    (static)

const badgeConfig = {
  PENDING:     { color: 'warning', pulse: true,  label: 'Pending'     },
  CONFIRMED:   { color: 'cyan',    pulse: true,  label: 'Confirmed'   },
  IN_PROGRESS: { color: 'cyan',    pulse: true,  label: 'In Progress' },
  COMPLETED:   { color: 'success', pulse: false, label: 'Completed'   },
  CANCELLED:   { color: 'danger',  pulse: false, label: 'Cancelled'   },
  NO_SHOW:     { color: 'muted',   pulse: false, label: 'No Show'     },
};

// Example — CONFIRMED
<span className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-medium
  border border-cyan/30 bg-cyan/10 text-cyan">
  <span className="w-1.5 h-1.5 rounded-full bg-cyan animate-pulse flex-shrink-0" aria-hidden="true" />
  Confirmed
</span>
```

---

### Input Field

```jsx
<div className="flex flex-col gap-1.5">
  <label htmlFor="field-id" className="text-xs font-medium text-secondary uppercase tracking-wide">
    Field Label <span className="text-danger" aria-label="required">*</span>
  </label>
  <input
    id="field-id"
    type="text"
    aria-required="true"
    aria-describedby="field-id-hint"
    className="
      w-full min-h-[44px] px-4 py-2.5
      bg-surface border border-border
      rounded-lg text-sm text-primary placeholder:text-muted
      focus:outline-none focus-visible:border-cyan focus-visible:shadow-cyan-glow
      aria-invalid:border-danger aria-invalid:shadow-danger-glow
      transition-all duration-150
    "
    placeholder="Placeholder text"
  />
  <p id="field-id-hint" className="text-xs text-muted">Helper text goes here.</p>
</div>

{/* Error state — shown after blur, not on keystroke */}
<p role="alert" className="text-xs text-danger mt-1 flex items-center gap-1">
  <ExclamationCircleIcon className="w-3.5 h-3.5 flex-shrink-0" aria-hidden="true" />
  This field is required.
</p>
```

### Select Dropdown

```jsx
<div className="flex flex-col gap-1.5">
  <label htmlFor="select-id" className="text-xs font-medium text-secondary uppercase tracking-wide">
    Vehicle Class
  </label>
  <select
    id="select-id"
    className="
      w-full min-h-[44px] px-4 py-2.5
      bg-surface border border-border
      rounded-lg text-sm text-primary
      focus:outline-none focus-visible:border-cyan focus-visible:shadow-cyan-glow
      transition-all duration-150 appearance-none
    "
  >
    <option>Motorcycle</option>
    <option>Sedan</option>
    <option>SUV / Luxury</option>
    <option>MPV / Large</option>
  </select>
</div>
```

---

### Slot Picker (Booking Flow — Customer)

```jsx
{/* Time slot grid — 3 columns on mobile, 4 on tablet */}
<div className="grid grid-cols-3 sm:grid-cols-4 gap-2" role="group" aria-label="Available time slots">
  {/* Available slot */}
  <button
    aria-pressed="false"
    className="
      min-h-[44px] px-3 py-2
      hex-grid hex-border rounded-lg
      text-xs font-medium text-secondary text-center
      hover:hex-border-active hover:text-cyan
      active:scale-[0.97]
      transition-all duration-150
      focus-visible:ring-2 focus-visible:ring-cyan focus-visible:ring-offset-1 focus-visible:ring-offset-base
    "
  >
    09:00
  </button>

  {/* Selected slot */}
  <button
    aria-pressed="true"
    className="
      min-h-[44px] px-3 py-2
      hex-grid hex-radial-core hex-border-active rounded-lg
      text-xs font-medium text-cyan text-center
      shadow-cyan-glow
      active:scale-[0.97]
      transition-all duration-150
    "
  >
    09:30
  </button>

  {/* Full / unavailable slot */}
  <button
    disabled
    aria-label="09:30 — fully booked"
    className="
      min-h-[44px] px-3 py-2
      bg-surface-raised border border-border-subtle rounded-lg
      text-xs font-medium text-muted text-center
      cursor-not-allowed line-through
    "
  >
    10:00
  </button>
</div>
```

### Vehicle Class Selector Card

```jsx
{/* Tappable selection card — used in booking flow */}
<button
  aria-pressed={isSelected}
  className={`
    w-full min-h-[60px] flex items-center gap-3 px-4 py-3
    hex-grid hex-corner rounded-xl text-left
    transition-all duration-150 active:scale-[0.98]
    focus-visible:ring-2 focus-visible:ring-cyan focus-visible:ring-offset-2 focus-visible:ring-offset-base
    ${isSelected
      ? 'hex-border-active hex-radial-core'
      : 'hex-border hover:hex-border-active'}
  `}
>
  <CarIcon className="w-5 h-5 text-cyan flex-shrink-0" aria-hidden="true" />
  <div>
    <p className="text-sm font-medium text-primary">SUV / Luxury</p>
    <p className="text-xs text-secondary">2 time slots · RM 90.00</p>
  </div>
  {isSelected && (
    <CheckCircleIcon className="w-4 h-4 text-cyan ml-auto flex-shrink-0" aria-hidden="true" />
  )}
</button>
```

---

### Bottom Navigation (Mobile — CUSTOMER / WORKER)

```jsx
{/* Max 4 items. Labels mandatory for discoverability. */}
<nav
  aria-label="Main navigation"
  className="fixed bottom-0 inset-x-0 z-30 h-16 flex items-center
    bg-surface border-t border-border-subtle safe-bottom"
>
  {navItems.map((item) => (
    <NavLink
      key={item.to}
      to={item.to}
      className={({ isActive }) => `
        flex-1 flex flex-col items-center justify-center gap-0.5
        min-h-[44px] text-center
        transition-colors duration-150
        ${isActive ? 'text-cyan' : 'text-muted hover:text-secondary'}
      `}
      aria-label={item.label}
    >
      <item.Icon className="w-5 h-5" aria-hidden="true" />
      <span className="text-[10px] font-medium">{item.label}</span>
      {isActive && (
        <span className="absolute bottom-0 w-8 h-0.5 bg-cyan rounded-full shadow-cyan-glow" aria-hidden="true" />
      )}
    </NavLink>
  ))}
</nav>
```

### Sidebar Navigation Item (Desktop — CLERK / OWNER)

```jsx
{/* Active */}
<NavLink
  to="/bookings"
  className="flex items-center gap-3 px-4 min-h-[44px] rounded-lg
    bg-cyan/10 border-l-2 border-cyan text-cyan text-sm font-medium"
  aria-current="page"
>
  <CalendarIcon className="w-4 h-4 flex-shrink-0" aria-hidden="true" />
  Bookings
</NavLink>

{/* Inactive */}
<NavLink
  to="/workers"
  className="flex items-center gap-3 px-4 min-h-[44px] rounded-lg
    text-secondary text-sm font-medium
    hover:bg-surface-hover hover:text-primary
    transition-colors duration-150"
>
  <UsersIcon className="w-4 h-4 flex-shrink-0" aria-hidden="true" />
  Workers
</NavLink>
```

---

### KPI / Stat Widget

```jsx
<div className="relative hex-radial-core hex-border hex-corner rounded-xl p-5 overflow-hidden">
  <p className="text-xs font-medium text-muted uppercase tracking-wide mb-2">Total Bookings</p>
  <p className="text-2xl font-display font-bold text-primary tabular-nums">1,248</p>
  <p className="text-xs text-success mt-1 flex items-center gap-1">
    <ArrowUpIcon className="w-3 h-3" aria-hidden="true" />
    <span>12% this week</span>
  </p>
</div>
```

### Data Table

```jsx
<div className="w-full overflow-x-auto -mx-4 px-4 sm:mx-0 sm:px-0">
  <table className="w-full text-sm min-w-[640px]" aria-label="Bookings list">
    <thead>
      <tr className="border-b border-border-subtle">
        <th scope="col" className="text-left py-3 px-4 text-xs font-medium text-muted uppercase tracking-wide">
          Booking ID
        </th>
        {/* ... more columns */}
      </tr>
    </thead>
    <tbody>
      <tr className="border-b border-border-subtle hover:bg-surface-hover transition-colors group">
        <td className="py-3 px-4 font-mono text-xs text-cyan">AW-2024-0042</td>
        {/* ... more cells */}
      </tr>
    </tbody>
  </table>
</div>
```

### Skeleton / Loading State

```jsx
{/* Use shimmer skeletons for operations that take > 300ms — NOT a full spinner */}
<div className="animate-pulse space-y-3" aria-busy="true" aria-label="Loading bookings">
  {[...Array(5)].map((_, i) => (
    <div key={i} className="h-14 bg-surface-raised rounded-xl" />
  ))}
</div>
```

### Modal / Confirmation Dialog

```jsx
{/* Backdrop */}
<div
  className="fixed inset-0 bg-base/80 backdrop-blur-sm z-40"
  aria-hidden="true"
  onClick={onClose}
/>

{/* Dialog panel */}
<div
  role="dialog"
  aria-modal="true"
  aria-labelledby="modal-title"
  className="fixed inset-x-4 top-1/2 -translate-y-1/2 sm:inset-auto sm:left-1/2 sm:-translate-x-1/2
    sm:w-[480px] hex-grid-subtle hex-border bg-surface-raised
    rounded-2xl p-6 z-50 shadow-cyan-glow"
>
  <h2 id="modal-title" className="text-lg font-display font-semibold text-primary mb-2">
    Cancel Booking?
  </h2>
  <p className="text-sm text-secondary mb-6 leading-relaxed">
    This action cannot be undone. The slot will be released.
  </p>
  <div className="flex gap-3 justify-end">
    <button onClick={onClose} className={/* secondary button */}>Keep Booking</button>
    <button onClick={onConfirm} className={/* danger button */}>Cancel Booking</button>
  </div>
</div>
```

### Mobile Bottom Sheet

```jsx
{/* Backdrop */}
<div className="fixed inset-0 bg-base/70 backdrop-blur-sm z-40" onClick={onClose} aria-hidden="true" />

{/* Sheet — slides up from bottom */}
<div
  role="dialog"
  aria-modal="true"
  aria-labelledby="sheet-title"
  className="fixed inset-x-0 bottom-0 z-50 hex-grid-subtle
    border-t hex-border rounded-t-2xl
    max-h-[85dvh] flex flex-col safe-bottom"
>
  {/* Drag handle */}
  <div className="flex justify-center pt-3 pb-1">
    <div className="w-10 h-1 bg-border rounded-full" aria-hidden="true" />
  </div>

  {/* Scrollable content */}
  <div className="overflow-y-auto flex-1 p-6">
    <h2 id="sheet-title" className="text-lg font-display font-semibold text-primary mb-4">
      Sheet Title
    </h2>
    {/* content */}
  </div>
</div>
```

### Toast / Notification

```jsx
{/* Positioned fixed top-right desktop, top-center mobile */}
{/* aria-live="polite" ensures screen readers announce it */}
<div
  role="status"
  aria-live="polite"
  aria-atomic="true"
  className="fixed top-4 inset-x-4 sm:inset-x-auto sm:right-4 sm:w-[360px] z-50
    hex-grid-subtle hex-border rounded-xl px-4 py-3
    flex items-start gap-3 shadow-cyan-glow"
>
  <CheckCircleIcon className="w-4 h-4 text-success flex-shrink-0 mt-0.5" aria-hidden="true" />
  <div>
    <p className="text-sm font-medium text-primary">Booking Confirmed</p>
    <p className="text-xs text-secondary">Reference: AW-2024-0042</p>
  </div>
  <button
    onClick={onDismiss}
    aria-label="Dismiss notification"
    className="ml-auto text-muted hover:text-secondary min-h-[44px] min-w-[44px] flex items-center justify-center -mr-2"
  >
    <XMarkIcon className="w-4 h-4" aria-hidden="true" />
  </button>
</div>
```

### Progress / Multi-Step Indicator (Booking Flow)

```jsx
{/* 3-step booking flow: Select Slot → Select Service → Confirm & Pay */}
<ol className="flex items-center gap-2" aria-label="Booking steps">
  {steps.map((step, i) => (
    <li key={step} className="flex items-center gap-2 flex-1">
      <div className={`
        w-6 h-6 rounded-full flex items-center justify-center text-xs font-medium flex-shrink-0
        ${i < currentStep ? 'bg-success text-base' : ''}
        ${i === currentStep ? 'bg-cyan text-base shadow-cyan-glow' : ''}
        ${i > currentStep ? 'bg-surface-raised border border-border text-muted' : ''}
      `}
        aria-current={i === currentStep ? 'step' : undefined}
      >
        {i < currentStep ? <CheckIcon className="w-3.5 h-3.5" /> : i + 1}
      </div>
      <span className={`text-xs font-medium truncate
        ${i === currentStep ? 'text-primary' : 'text-muted'}`}
      >
        {step}
      </span>
      {i < steps.length - 1 && (
        <div className={`h-px flex-1 ${i < currentStep ? 'bg-success' : 'bg-border-subtle'}`} aria-hidden="true" />
      )}
    </li>
  ))}
</ol>
```

---

## 💡 Glow & State Indicator System

| State | Visual Treatment |
|---|---|
| Active / online / pulsing | `w-2 h-2 rounded-full bg-cyan animate-pulse shadow-cyan-glow` |
| Pending / processing | `w-2 h-2 rounded-full bg-warning animate-pulse` |
| Completed / confirmed | `w-2 h-2 rounded-full bg-success` (no pulse — static means done) |
| Error / cancelled / danger | `w-2 h-2 rounded-full bg-danger` |
| Inactive / offline | `w-2 h-2 rounded-full bg-muted` |
| Focused interactive element | `focus-visible:ring-2 ring-cyan ring-offset-2 ring-offset-base` |
| Loading skeleton | `animate-pulse bg-surface-raised rounded` |
| Input focus | `focus-visible:border-cyan focus-visible:shadow-cyan-glow` |

---

## 🗺️ Role-Based Page Inventory

### CUSTOMER Role — Mobile-Primary

| Page | Route | Notes |
|---|---|---|
| Home / Book Now | `/` | CTA hero, service cards, quick-book button |
| Slot Selection | `/book/slot` | Date picker → slot grid → vehicle selector |
| Service Selection | `/book/service` | Wash package cards with price/duration |
| Booking Summary | `/book/confirm` | Review + ToyyibPay payment CTA |
| Payment Callback | `/book/payment/result` | Success or failure state |
| My Bookings | `/bookings` | List of PENDING / CONFIRMED / past |
| Booking Detail | `/bookings/:id` | State timeline, actions (cancel), Timah chat |
| Valet Request | `/valet` | Location permission → haversine check → form |
| Chat (Timah) | `/chat` | WebSocket AI chat — bottom drawer on mobile |

### CLERK Role — Tablet/Mobile Operations

| Page | Route | Notes |
|---|---|---|
| Dashboard | `/clerk` | Today's bookings queue, walk-in entry |
| Booking Queue | `/clerk/bookings` | Sortable list with status actions |
| Walk-in Booking | `/clerk/walk-in` | Quick form: vehicle, service, slot, pay |
| Booking Detail | `/clerk/bookings/:id` | Confirm payment, change status, notes |
| Payment Verification | `/clerk/payment/:id` | Manual HMAC/ToyyibPay callback status |

### WORKER Role — Mobile-Primary

| Page | Route | Notes |
|---|---|---|
| My Jobs | `/worker` | Assigned jobs for today — card list |
| Job Detail | `/worker/jobs/:id` | Vehicle info, service checklist, status update |
| Job Status Update | inline action | CONFIRMED → IN_PROGRESS → COMPLETED |

### OWNER Role — Desktop-Primary

| Page | Route | Notes |
|---|---|---|
| Analytics Dashboard | `/owner` | KPI row, revenue chart, booking trend |
| Booking Management | `/owner/bookings` | Full booking table with all filters |
| Staff Management | `/owner/staff` | CLERK / WORKER accounts |
| Service Catalog | `/owner/services` | Manage wash packages, pricing |
| Slot Management | `/owner/slots` | Slot capacity, exceptions, closures |
| Valet Config | `/owner/valet` | Service radius, pricing |
| System Logs | `/owner/logs` | Audit trail |
| Shop Closures | `/owner/closures` | Add public holiday closures |

---

## 📊 Data Visualization (Owner Dashboard)

### Chart Color Sequence

Use this order for multi-series charts — never use raw hex values inline:

| Series | Color | Tailwind |
|---|---|---|
| Series 1 (primary) | `#00F0FF` (cyan) | `text-cyan` |
| Series 2 | `#00E5A0` (success) | `text-success` |
| Series 3 | `#FFB800` (warning) | `text-warning` |
| Series 4 | `#7B8CDE` (info) | `text-info` |
| Series 5 | `#FF4466` (danger) | `text-danger` |

### Chart Rules

- Always include a legend with text labels — never color alone
- Grid lines: `rgba(42, 42, 61, 0.8)` (`border-border` at 80% opacity)
- Tooltip background: `#1A1A24` (`bg-surface-raised`) with `hex-border`
- Axis text: `#9090A8` (`text-secondary`), `text-xs`
- Recharts `<CartesianGrid strokeDasharray="3 3" stroke="#2A2A3D" />`
- Use `tabular-nums` for all numeric axis labels

---

## 🎞️ Animation Rules

| Rule | Value |
|---|---|
| Micro-interaction duration | `duration-150` (150ms) |
| Panel / card transition | `duration-200` (200ms) |
| Modal / sheet enter | `duration-300` (300ms, ease-out) |
| Modal / sheet exit | `duration-200` (200ms, ease-in — exits faster) |
| Page transition | `duration-250` (250ms) |
| Loading spinner | `animate-spin` |
| Status pulse | `animate-pulse` — active indicators ONLY |
| Card lift on hover | `hover:scale-[1.01]` — subtle, desktop only |
| Button press | `active:scale-[0.98]` |
| NEVER animate | `width`, `height`, `top`, `left`, `grid-template-rows` |
| Easing | `ease-out` for entering, `ease-in` for exiting |
| Reduced motion | All keyframe animations wrapped in `.hex-grid` global above |

### Enter / Exit Transition Template (Framer Motion / CSS)

```css
/* Bottom sheet enter */
@keyframes sheet-in  { from { transform: translateY(100%); } to { transform: translateY(0); } }
@keyframes sheet-out { from { transform: translateY(0); }    to { transform: translateY(100%); } }

/* Modal enter */
@keyframes modal-in  { from { opacity: 0; transform: scale(0.96); } to { opacity: 1; transform: scale(1); } }
@keyframes modal-out { from { opacity: 1; transform: scale(1); }    to { opacity: 0; transform: scale(0.96); } }

/* Respect reduced motion */
@media (prefers-reduced-motion: reduce) {
  @keyframes sheet-in, @keyframes sheet-out { from, to { transform: translateY(0); } }
}
```

---

## 🔔 Empty States

```jsx
{/* Every empty list / zero-data view needs this — never a blank screen */}
<div className="flex flex-col items-center justify-center py-16 px-6 text-center">
  <div className="w-12 h-12 rounded-2xl hex-grid hex-border flex items-center justify-center mb-4">
    <CalendarIcon className="w-6 h-6 text-cyan-dim" aria-hidden="true" />
  </div>
  <h3 className="text-base font-semibold text-primary mb-1">No bookings yet</h3>
  <p className="text-sm text-secondary max-w-[280px] leading-relaxed mb-6">
    Book your first car wash in under 2 minutes.
  </p>
  <button className={/* primary button */}>Book Now</button>
</div>
```

---

## ♿ Accessibility Requirements

| Check | Requirement |
|---|---|
| Body text contrast | `#E8E8F0` on `#13131A` — verified ≥ 11:1 ✅ |
| Secondary text contrast | `#9090A8` on `#13131A` — must verify ≥ 4.5:1 ✅ |
| Cyan on base | `#00F0FF` on `#0D0D11` — verified ≥ 6:1 ✅ |
| Muted text | `#5A5A72` — decorative/placeholder ONLY — never body copy |
| Focus rings | `focus-visible:ring-2 ring-cyan ring-offset-2 ring-offset-base` — NEVER remove |
| Icon-only buttons | MUST have `aria-label` |
| Status badges | MUST pair color with text label — never color alone |
| Glow effects | Decorative — do NOT convey meaning via glow |
| Touch targets | Minimum 44×44px on all interactive elements |
| Skip navigation | `<a href="#main-content" className="sr-only focus:not-sr-only ...">Skip to content</a>` in app shell |
| Heading hierarchy | h1 → h2 → h3 in strict order — no skipping levels |
| Form labels | `<label>` with `htmlFor` matching input `id` — no placeholder-only labels |
| Error announcements | `role="alert"` or `aria-live="polite"` on error messages |
| Dialogs | `role="dialog" aria-modal="true" aria-labelledby` |
| Loading states | `aria-busy="true"` on loading containers |
| Tables | `<thead>`, `scope="col"` on header cells, `aria-label` on table |
| Reduced motion | CSS `prefers-reduced-motion: reduce` block in global CSS (see above) |

---

## 🚫 Design Hard Constraints

| Constraint | Reason |
|---|---|
| NEVER use `bg-white`, `bg-gray-*`, or `text-white` | No light mode |
| NEVER use cyan as decoration | Cyan = interaction affordance only |
| NEVER use flat fills on card surfaces without hex-grid | Breaks the cyber-organic aesthetic |
| NEVER hardcode hex values inline | All colors via Tailwind tokens |
| NEVER use `text-muted` for readable body text | Below 4.5:1 contrast — decorative only |
| NEVER use `border-radius > rounded-2xl` | Maintains geometric / modular feel |
| NEVER mix `rounded-full` with `rounded-none` in the same panel | Inconsistent form language |
| NEVER use gradient text for body copy | Heading accents only |
| NEVER animate `width`, `height`, or layout properties | Use `transform` / `opacity` only |
| NEVER remove focus rings | Accessibility is non-negotiable |
| NEVER make touch targets smaller than 44×44px | iOS HIG / Material requirement |
| NEVER use `100vh` on mobile | Use `min-h-dvh` — accounts for mobile browser chrome |
| NEVER use placeholder text as a field label | Always use a visible `<label>` |
| NEVER show errors only at top of form | Show errors below each field (after blur) |
| NEVER give Timah write access | Read-only chatbot — architectural constraint from CLAUDE.md |
| NEVER hard-delete booking records | Soft delete only — from CLAUDE.md |

---

## 🎨 Icon System

Use **Heroicons v2** exclusively (matches React + Tailwind workflow). Do NOT use emoji as icons.

```bash
npm install @heroicons/react
```

```jsx
import { CalendarIcon, CheckCircleIcon, XMarkIcon } from '@heroicons/react/24/outline';
import { CheckCircleIcon as CheckCircleSolid } from '@heroicons/react/24/solid';

// Standard sizes
// Navigation:  w-5 h-5
// Inline/badge: w-4 h-4
// Hero/feature: w-6 h-6 or w-8 h-8
// All icons: aria-hidden="true" (label must be on parent or sibling)
```

---

## ✅ Design Pre-Delivery Checklist

Before delivering any UI component or page, verify ALL of the following:

**Color & Tokens**
- [ ] No `bg-white`, `bg-gray-*`, or `text-white` anywhere
- [ ] No inline hex values — all colors via Tailwind token classes
- [ ] Cyan used only on interactive / active elements
- [ ] `text-muted` only used for placeholders or decorative captions

**Texture**
- [ ] Card / panel backgrounds use a hex-grid texture class
- [ ] Active / selected panels use `hex-radial-core` + `hex-border-active`
- [ ] No plain flat `bg-surface` cards without texture class

**Typography**
- [ ] Headings use `font-display` (Space Grotesk)
- [ ] Body copy uses `font-sans` (Inter)
- [ ] Code / IDs use `font-mono` (JetBrains Mono)
- [ ] Mobile body text is `text-base` (≥16px) to prevent iOS auto-zoom
- [ ] Numeric data uses `tabular-nums`

**Layout & Responsive**
- [ ] Component works at 375px width (xs breakpoint)
- [ ] No horizontal scroll at any viewport width
- [ ] `min-h-dvh` used instead of `min-h-screen` / `100vh` for full-height layouts
- [ ] Safe area insets applied on mobile shell (`safe-top`, `safe-bottom`)

**Touch & Interaction**
- [ ] All interactive elements are min 44×44px
- [ ] Minimum 8px gap between adjacent touch targets
- [ ] Buttons disable and show loading state during async operations

**Interaction States**
- [ ] All buttons have hover, active (`scale-[0.98]`), disabled states
- [ ] All inputs have `focus-visible:border-cyan` + `focus-visible:shadow-cyan-glow`
- [ ] Loading states use `animate-pulse` skeleton (not full-page spinner)
- [ ] Empty states have icon, heading, body, and CTA

**Accessibility**
- [ ] Icon-only controls have `aria-label`
- [ ] Status indicators pair color with text label
- [ ] Focus rings present on ALL interactive elements (use `focus-visible:ring-*`)
- [ ] Form inputs have `<label htmlFor>` — no placeholder-only labels
- [ ] Errors use `role="alert"` or `aria-live`
- [ ] Dialogs have `role="dialog"`, `aria-modal`, `aria-labelledby`
- [ ] Tables have `scope="col"` on headers and `aria-label` on the table element
- [ ] Heading hierarchy is sequential (h1 → h2 → h3)

**Animation**
- [ ] No `width` / `height` animations
- [ ] Duration within 150–300ms range
- [ ] Pulse animation only on live status indicators
- [ ] `prefers-reduced-motion` global rule active in CSS

---

*End of CLAUDE_DESIGN.MD v2.0.0 — Load together with CLAUDE.md for every frontend task. When in conflict: this file governs visual design; CLAUDE.md governs architecture, stack, and security decisions.*
