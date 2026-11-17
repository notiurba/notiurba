import { lazy } from 'react'
import { Navigate, Route, Routes } from 'react-router-dom'
import CargaDiferida from '../../compartido/componentes/CargaDiferida'
import PasoCategoria from './PasoCategoria'
import ReportarLayout from './ReportarLayout'

const PasoCanales = lazy(() => import('./PasoCanales'))
const PasoConfirmacion = lazy(() => import('./PasoConfirmacion'))
const PasoDetalles = lazy(() => import('./PasoDetalles'))
const PasoUbicacion = lazy(() => import('./PasoUbicacion'))
const ReporteEnviado = lazy(() => import('./ReporteEnviado'))

// Flujo «Nuevo reporte» (/reportar/*). El layout exige sesión para consultar el catálogo protegido.
export default function RutasReportar() {
  return (
    <Routes>
      <Route element={<ReportarLayout />}>
        <Route index element={<PasoCategoria />} />
        <Route path="canales" element={<CargaDiferida><PasoCanales /></CargaDiferida>} />
        <Route path="ubicacion" element={<CargaDiferida><PasoUbicacion /></CargaDiferida>} />
        <Route path="detalles" element={<CargaDiferida><PasoDetalles /></CargaDiferida>} />
        <Route path="confirmacion" element={<CargaDiferida><PasoConfirmacion /></CargaDiferida>} />
        <Route path="enviado/:codigo" element={<CargaDiferida><ReporteEnviado /></CargaDiferida>} />
        <Route path="*" element={<Navigate to="/reportar" replace />} />
      </Route>
    </Routes>
  )
}
