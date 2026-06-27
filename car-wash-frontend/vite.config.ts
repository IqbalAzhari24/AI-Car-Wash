import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  // Force a single React instance in dev — esbuild's dep pre-bundling can
  // otherwise load a 2nd copy (via lucide-react), causing "Invalid hook call".
  resolve: {
    dedupe: ['react', 'react-dom'],
  },
})
