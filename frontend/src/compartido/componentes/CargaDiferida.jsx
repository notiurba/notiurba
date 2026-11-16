import { Suspense } from 'react'

// Mantiene el límite de carga dentro de la ruta para conservar su layout y el borrador compartido.
export default function CargaDiferida({ children }) {
  return (
    <Suspense fallback={<p className="cargando-texto" role="status">Cargando pantalla…</p>}>
      {children}
    </Suspense>
  )
}
