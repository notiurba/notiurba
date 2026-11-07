import { Route, Routes } from 'react-router-dom'
import EstadoApi from './paginas/EstadoApi'
import Ingresar from './paginas/Ingresar'
import Proximamente from './paginas/Proximamente'
import CrearReporte from './paginas/CrearReporte'
import ConsultarReporte from './paginas/ConsultarReporte'
import MisReportes from './paginas/MisReportes'
import RutaProtegida from '../compartido/auth/RutaProtegida'

export default function RutasApp() {
  return (
    <Routes>
      <Route index element={<Proximamente />} />
      <Route path="estado" element={<EstadoApi />} />
      <Route path="ingresar" element={<Ingresar />} />
      <Route path="crear-reporte" element={<RutaProtegida><CrearReporte /></RutaProtegida>} />
      <Route path="consultar-reporte" element={<RutaProtegida><ConsultarReporte /></RutaProtegida>} />
      <Route path="mis-reportes" element={<RutaProtegida><MisReportes /></RutaProtegida>} />
    </Routes>
  )
}
