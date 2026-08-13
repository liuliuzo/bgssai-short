import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// User-app SPA build config.
// base '/', assetsDir 'assets' — the product is served by bgssai-short-drama-user Spring Boot from
// the root path. The dev server proxies /bgssai to the user backend on port 8081.
export default defineConfig({
  plugins: [react()],
  base: '/',
  build: {
    outDir: 'dist',
    assetsDir: 'assets',
  },
  server: {
    open: false,
    port: 3002,
    proxy: {
      '/bgssai': {
        target: 'http://localhost:8081',
        changeOrigin: true,
      },
    },
  },
})
