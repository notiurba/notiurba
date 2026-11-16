import { lazy } from 'react'
import { Navigate, Outlet, Route, Routes, useLocation } from 'react-router-dom'
import NoEncontrada from '../compartido/paginas/NoEncontrada'
import AvisoAcceso from '../compartido/componentes/avisos/AvisoAcceso'
import LayoutApp from './componentes/LayoutApp'
import MarcoApp from './componentes/MarcoApp'
import CargaDiferida from '../compartido/componentes/CargaDiferida'
import MisReportes from './paginas/MisReportes'

const Ajustes = lazy(() => import('./paginas/Ajustes'))
const DetalleReporte = lazy(() => import('./paginas/DetalleReporte'))
const EditarDatos = lazy(() => import('./paginas/EditarDatos'))

// «/app» lleva a Mis reportes y conserva el aviso que traiga la redirección (p. ej. «Acceso no permitido»).
function IrAReportes() {
  const { state } = useLocation()
  return <Navigate to="reportes" replace state={state} />
}

// Pantallas de formulario: el mismo marco pero sin la barra de pestañas (no compite con «Guardar cambios»).
function LayoutSinPestanas() {
  return (
    <MarcoApp>
      <Outlet />
    </MarcoApp>
  )
}

// PWA ciudadana (/app/*): las pantallas con barra de pestañas abajo.
// El flujo de «Nuevo reporte» vive aparte, en /reportar (ver ./reportar/rutas.jsx).
export default function RutasApp() {
  return (
    <>
      <AvisoAcceso />
      <Routes>
        <Route element={<LayoutApp />}>
          <Route index element={<IrAReportes />} />
          <Route path="reportes" element={<MisReportes />} />
          <Route path="reportes/:codigo" element={<CargaDiferida><DetalleReporte /></CargaDiferida>} />
          <Route path="ajustes" element={<CargaDiferida><Ajustes /></CargaDiferida>} />
        </Route>
        <Route element={<LayoutSinPestanas />}>
          <Route path="ajustes/datos" element={<CargaDiferida><EditarDatos /></CargaDiferida>} />
        </Route>
        <Route path="*" element={<NoEncontrada />} />
      </Routes>
    </>
  )
}
