import { useCallback } from 'react'
import useCargar from '../../compartido/hooks/useCargar'
import { vistaPrevia } from '../servicios/correoReportes'

// datos: la petición ya armada como texto JSON (o null si no hay correo); solo se repite si cambia.
export default function useVistaPrevia(datos) {
  const cargar = useCallback(async () => (datos ? vistaPrevia(JSON.parse(datos)) : null), [datos])
  return useCargar(cargar)
}
