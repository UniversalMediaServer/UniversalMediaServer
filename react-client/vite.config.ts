import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig({
  plugins: [
    react(),
  ],
  build: {
    outDir: '../src/main/external-resources/web/react-client',
    emptyOutDir: true,
    assetsDir: 'static',
    sourcemap: true,
    chunkSizeWarningLimit: 2000,
    rollupOptions: {
      output: {
        hashCharacters: 'hex',
      },
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
    proxy: {
      '/v1': {
        target: { host: 'localhost', port: 9001 },
        configure: (proxy, _options) => {
          proxy.on('error', (err, _req, res, _target) => {
            if (!res.headersSent && !res.writableEnded) {
              res.destroy(err)
            }
          })
        },
        ws: true,
      },
    },
    watch: {
      usePolling: true,
    },
    sourcemapIgnoreList(sourcePath, _sourcemapPath) {
      return sourcePath.indexOf('node_modules') > -1
    },
  },
})
