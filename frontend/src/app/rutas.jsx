import { Route, Routes } from 'react-router-dom'
import EstadoApi from './paginas/EstadoApi'
import Proximamente from './paginas/Proximamente'

export default function RutasApp() {
  return (
    <Routes>
      <Route index element={<Proximamente />} />
      <Route path="estado" element={<EstadoApi />} />
    </Routes>
  )
}
