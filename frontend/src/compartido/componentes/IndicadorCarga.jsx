import './IndicadorCarga.css'

// Aviso breve para operaciones que bloquean una acción; no inventa porcentaje ni etapas.
export default function IndicadorCarga({ texto }) {
  return (
    <p className="indicador-carga" role="status" aria-live="polite" aria-busy="true">
      <span className="indicador-carga__giro" aria-hidden="true" />
      {texto}
    </p>
  )
}
