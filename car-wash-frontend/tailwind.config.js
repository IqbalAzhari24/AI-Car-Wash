/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      fontFamily: {
        sans:    ['Inter', 'system-ui', 'sans-serif'],
        mono:    ['JetBrains Mono', 'Fira Code', 'monospace'],
        display: ['Space Grotesk', 'Inter', 'sans-serif'],
      },
      boxShadow: {
        'cyan-glow':    '0 0 12px rgba(0, 240, 255, 0.20), 0 0 24px rgba(0, 240, 255, 0.08)',
        'cyan-glow-lg': '0 0 20px rgba(0, 240, 255, 0.30), 0 0 48px rgba(0, 240, 255, 0.10)',
        'panel':        '0 1px 0 rgba(0, 240, 255, 0.05), inset 0 1px 0 rgba(255,255,255,0.03)',
        'success-glow': '0 0 10px rgba(0, 229, 160, 0.20)',
        'danger-glow':  '0 0 10px rgba(255, 68, 102, 0.20)',
      },
      screens: {
        xs: '375px',
      },
      colors: {
        base: '#0D0D11',
        surface: {
          DEFAULT: '#13131A',
          raised:  '#1A1A24',
          hover:   '#1F1F2E',
        },
        border: {
          DEFAULT: '#2A2A3D',
          subtle:  '#1E1E2D',
        },
        slate:     '#3D4A6B',
        cyan: {
          DEFAULT: '#00F0FF',
          dim:     '#00B8C4',
        },
        primary:   '#E8E8F0',
        secondary: '#9090A8',
        muted:     '#5A5A72',
        success:   '#00E5A0',
        warning:   '#FFB800',
        danger:    '#FF4466',
        info:      '#7B8CDE',
      },
    },
  },
  plugins: [],
}
