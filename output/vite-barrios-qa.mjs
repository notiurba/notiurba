import { mergeConfig } from '../frontend/node_modules/vite/dist/node/index.js'
import base from '../frontend/vite.config.js'

// Entorno de QA aislado: no modifica .env.local ni el Vite existente de 5173.
export default mergeConfig(base, {
  root: new URL('../frontend', import.meta.url).pathname.replace(/^\/([A-Za-z]:)/, '$1'),
  server: {
    host: '127.0.0.1', port: 5174, strictPort: true,
    proxy: { '/api': { target: 'http://127.0.0.1:18090', changeOrigin: true, secure: false } },
  },
})
