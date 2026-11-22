import { Suspense } from 'react'

// Mantiene el límite de carga dentro de la ruta para conservar su layout y el borrador compartido.
export default function CargaDiferida({ children, mensaje = 'Cargando pantalla…' }) {
  return (
    <Suspense fallback={<p className="cargando-texto" role="status">{mensaje}</p>}>
      {children}
    </Suspense>
  )
}
