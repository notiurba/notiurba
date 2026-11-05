import { useLocation } from 'react-router-dom'
import Aviso from './Aviso'

// Muestra el aviso que dejó RutaProtegida al redirigir (por ejemplo «Acceso no permitido»).
export default function AvisoAcceso() {
  const { state } = useLocation()
  if (!state?.aviso) return null

  return (
    <div style={{ padding: 'var(--nu-esp-16)' }}>
      <Aviso variante="error">{state.aviso}</Aviso>
    </div>
  )
}
