import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig({
  plugins: [
    react({ include: /\.(mdx|js|jsx|ts|tsx)$/ }),
  ],
  server: {
    open: '/',
    port: 5273,
    proxy: {
      '/v1': {
        target: { host: 'localhost', port: 9002 },
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
