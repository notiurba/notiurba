import { useEffect, useRef, useState } from 'react'
import './SinConexion.css'

export default function SinConexion() {
  const [enLinea, setEnLinea] = useState(() => navigator.onLine)
  const ref = useRef(null)

  useEffect(() => {
    const actualizar = () => setEnLinea(navigator.onLine)
    window.addEventListener('online', actualizar)
    window.addEventListener('offline', actualizar)
    actualizar()
    return () => {
      window.removeEventListener('online', actualizar)
      window.removeEventListener('offline', actualizar)
    }
  }, [])

  useEffect(() => {
    const dialogo = ref.current
    if (!enLinea && !dialogo.open) dialogo.showModal()
    if (enLinea && dialogo.open) dialogo.close()
  }, [enLinea])

  return (
    <dialog ref={ref} className="sin-conexion"
      aria-labelledby="sin-conexion-titulo" aria-describedby="sin-conexion-descripcion"
      onCancel={(evento) => evento.preventDefault()}>
      <h1 id="sin-conexion-titulo">Sin conexión</h1>
      <p id="sin-conexion-descripcion">No tenemos internet en este momento. Revise su conexión y vuelva a intentarlo.</p>
      <button type="button" onClick={() => setEnLinea(navigator.onLine)}>Intentar de nuevo</button>
    </dialog>
  )
}
