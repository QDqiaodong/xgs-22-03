import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import path from 'path'

export default defineConfig({
  plugins: [vue()],
  server: {
    port: Number(process.env.FRONTEND_PORT || 8232),
    host: '127.0.0.1',
    strictPort: true,
    proxy: {
      '/api': {
        target: `http://localhost:${process.env.BACKEND_PORT || 8332}`,
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/api/, '/api')
      }
    }
  },
  preview: {
    host: '127.0.0.1',
    port: Number(process.env.FRONTEND_PORT || 8232),
    strictPort: true
  },
  resolve: {
    alias: {
      '@': path.resolve(__dirname, './src')
    }
  }
})
