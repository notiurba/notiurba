import { Route, Routes } from 'react-router-dom'
import NoEncontrada from '../compartido/paginas/NoEncontrada'
import LayoutAdmin from './componentes/LayoutAdmin'
import Configuracion from './paginas/Configuracion'
import MapaComunas from './paginas/MapaComunas'
import Reportes from './paginas/Reportes'

// Panel de administración: Reportes (por defecto), Mapa de calor y Configuración.
export default function RutasAdmin() {
  return (
    <Routes>
      <Route element={<LayoutAdmin />}>
        <Route index element={<Reportes />} />
        <Route path="mapa" element={<MapaComunas />} />
        <Route path="configuracion" element={<Configuracion />} />
      </Route>
      <Route path="*" element={<NoEncontrada />} />
    </Routes>
  )
}
