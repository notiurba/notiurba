import { Navigate, Outlet, Route, Routes, useLocation } from 'react-router-dom'
import AvisoAcceso from '../compartido/componentes/AvisoAcceso'
import LayoutApp from './componentes/LayoutApp'
import MarcoApp from './componentes/MarcoApp'
import Ajustes from './paginas/Ajustes'
import DetalleReporte from './paginas/DetalleReporte'
import EditarDatos from './paginas/EditarDatos'
import MisReportes from './paginas/MisReportes'

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
          <Route path="reportes/:codigo" element={<DetalleReporte />} />
          <Route path="ajustes" element={<Ajustes />} />
        </Route>
        <Route element={<LayoutSinPestanas />}>
          <Route path="ajustes/datos" element={<EditarDatos />} />
        </Route>
      </Routes>
    </>
  )
}
