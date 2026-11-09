import { mergeConfig } from '../frontend/node_modules/vite/dist/node/index.js'
import { fileURLToPath } from 'node:url'
import base from '../frontend/vite.config.js'
export default mergeConfig(base, {
  root:fileURLToPath(new URL('../frontend',import.meta.url)),
  cacheDir:fileURLToPath(new URL('./.vite-not59-cache',import.meta.url)),
  server:{host:'127.0.0.1',port:5176,strictPort:true,
    proxy:{'/api':{target:'http://127.0.0.1:19059',changeOrigin:true,secure:false,headers:{Origin:'http://127.0.0.1:5176'}}}},
})
