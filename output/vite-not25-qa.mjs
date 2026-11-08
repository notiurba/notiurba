import { mergeConfig } from '../frontend/node_modules/vite/dist/node/index.js'
import { fileURLToPath } from 'node:url'
import base from '../frontend/vite.config.js'
export default mergeConfig(base, {
  root:fileURLToPath(new URL('../frontend',import.meta.url)),
  cacheDir:fileURLToPath(new URL('./.vite-not25-cache',import.meta.url)),
  server:{host:'127.0.0.1',port:5175,strictPort:true,
    // Proxy exclusivamente de QA: usa el Origin local permitido por el backend desechable.
    // No certifica CORS del despliegue ni modifica la configuracion del producto.
    proxy:{'/api':{target:'http://127.0.0.1:19025',changeOrigin:true,secure:false,headers:{Origin:'http://localhost:5173'}}}},
})
