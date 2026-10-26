import { Route, Routes } from 'react-router-dom'
import MapaComunas from './paginas/MapaComunas'

export default function RutasAdmin() {
  return (
    <Routes>
      <Route index element={<MapaComunas />} />
    </Routes>
  )
}
