import { defineConfig } from 'vite'
import uni from '@dcloudio/vite-plugin-uni'

export default defineConfig({
  plugins: [uni()],
  server: {
    proxy: {
      '/app-api': {
        target: 'http://localhost:48081/app-api',
        changeOrigin: true,
      },
    },
  },
})
