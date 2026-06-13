/** @type {import('tailwindcss').Config} */

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
      colors: {
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
