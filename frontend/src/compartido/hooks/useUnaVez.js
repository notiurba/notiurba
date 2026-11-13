import { useRef } from 'react'
import { ejecutarUnaVez } from '../utilidades/unaVez'

// Envuelve un manejador que llama al servicio para que un doble clic o un doble Enter no lo repita
// mientras espera la respuesta. El candado vive entre renders; la acción es siempre la del último render.
export default function useUnaVez(accion) {
  const candado = useRef(false)
  return (...args) => ejecutarUnaVez(candado, accion, args)
}
