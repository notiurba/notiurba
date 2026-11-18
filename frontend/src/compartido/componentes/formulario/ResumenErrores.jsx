import Aviso from '../avisos/Aviso'
import { ResumenErroresContexto } from './resumenErroresContexto'

// Envuelve los campos de un formulario: al haber errores, los anuncia juntos y de inmediato.
// Cada campo conserva su mensaje enlazado con aria-invalid y aria-describedby.
export default function ResumenErrores({ errores, children }) {
  const mensajes = Object.values(errores).filter(Boolean)

  return (
    <ResumenErroresContexto value>
      {mensajes.length > 0 && (
        <Aviso variante="error" titulo="Revise los campos">
          {mensajes.join('. ')}.
        </Aviso>
      )}
      {children}
    </ResumenErroresContexto>
  )
}
