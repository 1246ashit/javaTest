import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    proxy: {
      '/api': {
        target: 'https://localhost:8443',
        changeOrigin: true,
        secure: false,   // 忽略自簽憑證驗證
      },
      '/ws': {
        target: 'wss://localhost:8443',
        ws: true,
        changeOrigin: true,
        secure: false,
      },
    },
  },
})
