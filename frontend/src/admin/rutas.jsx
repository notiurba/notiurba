import { lazy } from 'react'
import { Route, Routes } from 'react-router-dom'
import NoEncontrada from '../compartido/paginas/NoEncontrada'
import CargaDiferida from '../compartido/componentes/CargaDiferida'
import LayoutAdmin from './componentes/LayoutAdmin'
import Reportes from './paginas/Reportes'

const Configuracion = lazy(() => import('./paginas/Configuracion'))
const MapaComunas = lazy(() => import('./paginas/MapaComunas'))
const Estadisticas = lazy(() => import('./paginas/Estadisticas'))

// Panel de administración: Reportes (por defecto), Mapa de calor y Configuración.
export default function RutasAdmin() {
  return (
    <Routes>
      <Route element={<LayoutAdmin />}>
        <Route index element={<Reportes />} />
        <Route path="mapa" element={<CargaDiferida><MapaComunas /></CargaDiferida>} />
        <Route path="puntos-criticos" element={<CargaDiferida><MapaComunas /></CargaDiferida>} />
        <Route path="estadisticas" element={<CargaDiferida><Estadisticas /></CargaDiferida>} />
        <Route path="configuracion" element={<CargaDiferida><Configuracion /></CargaDiferida>} />
      </Route>
      <Route path="*" element={<NoEncontrada />} />
    </Routes>
  )
}
