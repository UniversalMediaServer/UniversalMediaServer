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
      '/v1': 'http://localhost:9002',
    },
    watch: {
      usePolling: true,
    },
  },
})
