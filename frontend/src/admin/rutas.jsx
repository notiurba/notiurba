import { Route, Routes } from 'react-router-dom'
import NoEncontrada from '../compartido/paginas/NoEncontrada'
import LayoutAdmin from './componentes/LayoutAdmin'
import MapaComunas from './paginas/MapaComunas'
import Reportes from './paginas/Reportes'

// Panel de administración: Reportes (por defecto) y Mapa de calor.
export default function RutasAdmin() {
  return (
    <Routes>
      <Route element={<LayoutAdmin />}>
        <Route index element={<Reportes />} />
        <Route path="mapa" element={<MapaComunas />} />
      </Route>
      <Route path="*" element={<NoEncontrada />} />
    </Routes>
  )
}
