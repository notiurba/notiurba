import { Navigate, Route, Routes } from 'react-router-dom'
import PasoCategoria from './PasoCategoria'
import PasoConfirmacion from './PasoConfirmacion'
import PasoDetalles from './PasoDetalles'
import PasoUbicacion from './PasoUbicacion'
import ReporteEnviado from './ReporteEnviado'
import ReportarLayout from './ReportarLayout'

// Flujo «Nuevo reporte» (/reportar/*). El layout exige sesión para consultar el catálogo protegido.
export default function RutasReportar() {
  return (
    <Routes>
      <Route element={<ReportarLayout />}>
        <Route index element={<PasoCategoria />} />
        <Route path="ubicacion" element={<PasoUbicacion />} />
        <Route path="detalles" element={<PasoDetalles />} />
        <Route path="confirmacion" element={<PasoConfirmacion />} />
        <Route path="enviado/:codigo" element={<ReporteEnviado />} />
        <Route path="*" element={<Navigate to="/reportar" replace />} />
      </Route>
    </Routes>
  )
}
