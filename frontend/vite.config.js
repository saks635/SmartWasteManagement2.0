import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import tailwindcss from '@tailwindcss/vite'

export default defineConfig({
  plugins: [
    vue(),
    tailwindcss(), // Tailwind CSS v4 — no config file needed!
  ],
  server: {
    port: 5173,
    // Proxy: All these requests from Vue are forwarded to Spring Boot on port 8080
    // This way, session cookies work correctly (same origin: localhost)
    proxy: {
      '/api':          'http://localhost:8080',
      '/oauth2':       'http://localhost:8080',
      '/login/oauth2': 'http://localhost:8080',
      '/logout':       'http://localhost:8080',
    }
  }
})
