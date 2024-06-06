import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [react()],
  build:{
    outDir: '../src/main/external-resources/web/react-client',
    emptyOutDir: true,
    assetsDir: 'static',
    sourcemap: true,
    chunkSizeWarningLimit: 1600,
    rollupOptions: {
      onwarn(warning, defaultHandler) {
        if (warning.code === 'SOURCEMAP_ERROR') {
          return
        }
        defaultHandler(warning)
      },
    },
  },
  css: {
    devSourcemap: true,
  },
  server: {
    open: '/',
    proxy:{
      '/': 'http://localhost:9001',
	}
  }
})
