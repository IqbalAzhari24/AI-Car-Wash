/** @type {import('tailwindcss').Config} */

// Existing OKLCH tokens (light-mode system — untouched).
// Design tokens live as OKLCH components in src/index.css; this maps them
// to utilities with alpha support (e.g. bg-primary/10). See /DESIGN.md.
const token = (name) => `oklch(var(--color-${name}) / <alpha-value>)`;

export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      // ─── Font families (CLAUDE_DESIGN.MD v2) ─────────────────────────────
      fontFamily: {
        sans:    ['Inter', 'system-ui', 'sans-serif'],
        mono:    ['JetBrains Mono', 'Fira Code', 'monospace'],
        display: ['Space Grotesk', 'Inter', 'sans-serif'],
      },
      // ─── Box shadows (CLAUDE_DESIGN.MD v2) ───────────────────────────────
      boxShadow: {
        'cyan-glow':    '0 0 12px rgba(0, 240, 255, 0.20), 0 0 24px rgba(0, 240, 255, 0.08)',
        'cyan-glow-lg': '0 0 20px rgba(0, 240, 255, 0.30), 0 0 48px rgba(0, 240, 255, 0.10)',
        'panel':        '0 1px 0 rgba(0, 240, 255, 0.05), inset 0 1px 0 rgba(255,255,255,0.03)',
        'danger-glow':  '0 0 10px rgba(255, 68, 102, 0.20)',
      },
      // ─── Color tokens (CLAUDE_DESIGN.MD v2 dark-mode system) ─────────────
      // Added additively — existing OKLCH tokens below remain untouched.
      colors: {
        // Dark-mode surface scale
        base:             '#0D0D11',
        'surface-raised': '#1A1A24',
        'surface-hover':  '#1F1F2E',
        // Borders
        'border-subtle':  '#1E1E2D',
        slate:            '#3D4A6B',
        // Accent
        cyan:             '#00F0FF',
        'cyan-dim':       '#00B8C4',
        // Semantic (extend existing keys with dark-mode flat values as sub-keys)
        'aw-success':     '#00E5A0',
        'aw-warning':     '#FFB800',
        'aw-danger':      '#FF4466',
        info:             '#7B8CDE',
        // Text scale
        'text-primary':   '#E8E8F0',
        'text-secondary': '#9090A8',
        'text-muted-aw':  '#5A5A72',

        // ─── Existing OKLCH tokens (unchanged) ───────────────────────────
        bg: token('bg'),
        surface: {
          DEFAULT: token('surface'),
          2: token('surface-2'),
        },
        border: token('border'),
        ink: token('ink'),
        muted: token('muted'),
        primary: {
          DEFAULT: token('primary'),
          strong: token('primary-strong'),
          soft: token('primary-soft'),
          'soft-ink': token('primary-soft-ink'),
        },
        accent: {
          DEFAULT: token('accent'),
          strong: token('accent-strong'),
          soft: token('accent-soft'),
          'soft-ink': token('accent-soft-ink'),
        },
        success: {
          DEFAULT: token('success'),
          soft: token('success-soft'),
          'soft-ink': token('success-soft-ink'),
        },
        warning: {
          DEFAULT: token('warning'),
          soft: token('warning-soft'),
          'soft-ink': token('warning-soft-ink'),
        },
        danger: {
          DEFAULT: token('danger'),
          soft: token('danger-soft'),
          'soft-ink': token('danger-soft-ink'),
        },
      },
    },
  },
  plugins: [],
}
