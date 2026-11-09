// Regresion de UI: dependencias HTTP sustituidas, sin conexion a la base.
// Se prueban los componentes reales y el mismo codigo; NO acredita integracion API.
import { useEffect, useState } from 'react'
import { createRoot } from 'react-dom/client'
import { MemoryRouter, Routes, Route, useLocation } from 'react-router-dom'
import '../src/compartido/estilos/base.css'

if (!import.meta.env.DEV || location.hostname !== '127.0.0.1' || location.port !== '5176') {
  throw new Error('Prueba disponible solo en el Vite QA local 5176')
}
class Memoria {
  datos = new Map()
  getItem(k) { return this.datos.get(k) ?? null }
  setItem(k, v) { this.datos.set(k, String(v)) }
  removeItem(k) { this.datos.delete(k) }
}
// Fachadas de esta ventana: no se cambia el almacenamiento real de la cuenta abierta.
Object.defineProperty(window, 'localStorage', { value: new Memoria() })
Object.defineProperty(window, 'sessionStorage', { value: new Memoria() })
localStorage.setItem('notiurba.token', 'ui-fixture.' + btoa(JSON.stringify({rol:'CIUDADANO',exp:4102444800})) + '.sin-firma')
const codigo = 'NU-000001'
const ruta = { idCategoria:1, categoria:'Vías, huecos, andenes e infraestructura', entidad:'Destino ficticio de prueba UI', dependencia:'Sin envío externo', idCanal:1, permiteCorreo:true, acceso:'qa@example.test', requiereUbicacion:true }
sessionStorage.setItem('notiurba.reporteGuardado', JSON.stringify({codigoSeguimiento:codigo,totalFotos:1,fotosSubidas:1,ruta}))
const llamadas = []
const visitas = []
window.fetch = async (input, opciones = {}) => {
  const path = new URL(typeof input === 'string' ? input : input.url, location.origin).pathname
  const metodo = opciones.method ?? 'GET'
  llamadas.push({metodo,path,simulada:true})
  let datos
  if (metodo === 'GET' && path === '/api/enrutamiento/categorias') datos = [ruta]
  else if (metodo === 'GET' && path === '/api/catalogos/barrios') datos = []
  else if (metodo === 'GET' && path === '/api/enrutamiento/categorias/1') datos = ruta
  else if (metodo === 'GET' && path === `/api/reportes/${codigo}`) datos = {codigoSeguimiento:codigo,estado:'Recibido',categoria:{idCategoria:1,nombre:ruta.categoria},comuna:4,barrio:{nombre:'Centro'},fotos:[{orden:1,url:'fixture-ui'}]}
  else if ((metodo === 'GET' && path === `/api/reportes/${codigo}/envio`) || (metodo === 'POST' && path === `/api/reportes/${codigo}/enviar`)) datos = {estado:'PENDIENTE',ruta}
  else throw new Error(`Operacion no permitida por la prueba: ${metodo} ${path}`)
  return new Response(JSON.stringify(datos), {status:200,headers:{'Content-Type':'application/json'}})
}
const {default: ReportarLayout} = await import('../src/app/reportar/ReportarLayout.jsx')
const {default: PasoConfirmacion} = await import('../src/app/reportar/PasoConfirmacion.jsx')
const {default: ReporteEnviado} = await import('../src/app/reportar/ReporteEnviado.jsx')

export function Observador() {
  const {pathname} = useLocation()
  const [resultado, setResultado] = useState({estado:'EN_CURSO'})
  useEffect(() => {
    visitas.push(pathname)
    if (pathname !== `/reportar/enviado/${codigo}`) return
    const timer = setTimeout(() => {
      const borrador = JSON.parse(sessionStorage.getItem('notiurba.borrador') ?? '{}')
      const verificaciones = {
        iniciaEnCrearReporte:visitas[0] === '/reportar',
        recuperaConfirmacion:visitas.includes('/reportar/confirmacion'),
        destinoFinal:pathname === `/reportar/enviado/${codigo}`,
        limpiaAvance:sessionStorage.getItem('notiurba.reporteGuardado') === null,
        limpiaBorrador:!borrador.idCategoria && !borrador.descripcion,
        ceroCreaciones:!llamadas.some(r => r.metodo === 'POST' && r.path === '/api/reportes'),
        ceroSubidas:!llamadas.some(r => r.metodo === 'POST' && r.path.endsWith('/fotos')),
        confirmaUnaVez:llamadas.filter(r => r.metodo === 'POST' && r.path.endsWith('/enviar')).length === 1,
        codigoVisible:document.querySelector('.enviado__codigo strong')?.textContent === codigo,
      }
      setResultado({estado:Object.values(verificaciones).every(Boolean)?'APROBADA_UI_AISLADA':'FALLIDA',alcance:'Componentes reales; fetch y almacenamiento en memoria; CSP connect-src none. No integracion HTTP.',verificaciones,visitas:[...visitas],llamadas:[...llamadas],escriturasRed:0})
    }, 250)
    return () => clearTimeout(timer)
  }, [pathname])
  return <aside style={{padding:12,border:'3px solid #176b40'}}>
    <h1>Prueba aislada de navegación NOT-71</h1>
    <p>Sin escrituras a QA. Se recupera el código existente desde Crear reporte. Marque la autorización y confirme para probar la navegación.</p>
    <p>Ruta interna: <strong>{pathname}</strong></p>
    <pre id="resultado-not71" style={{whiteSpace:'pre-wrap'}}>{JSON.stringify(resultado,null,2)}</pre>
  </aside>
}
createRoot(document.getElementById('root')).render(<MemoryRouter initialEntries={['/reportar']}>
  <Observador/>
  <Routes><Route path="/reportar" element={<ReportarLayout/>}>
    <Route index element={<p>ERROR: regreso inesperado a Categoría</p>}/>
    <Route path="confirmacion" element={<PasoConfirmacion/>}/>
    <Route path="enviado/:codigo" element={<ReporteEnviado/>}/>
  </Route></Routes>
</MemoryRouter>)
