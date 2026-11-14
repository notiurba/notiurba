import { useCallback, useEffect, useState } from 'react'
import { vistaPrevia } from '../servicios/correoReportes'
import { estadoVistaPrevia } from './vistaPrevia'

// datos: la petición ya armada como texto JSON (o null si no hay correo); solo se repite si cambia.
export default function useVistaPrevia(datos) {
  const [resultado, setResultado] = useState(null)
  const [intento, setIntento] = useState(0)

  useEffect(() => {
    if (!datos) return undefined
    let activo = true
    vistaPrevia(JSON.parse(datos))
      .then((respuesta) => activo && setResultado({ clave: datos, datos: respuesta, error: null }))
      .catch((fallo) => activo && setResultado({ clave: datos, datos: null, error: fallo ?? new Error('Falló la vista previa') }))
    return () => { activo = false }
  }, [datos, intento])

  const recargar = useCallback(() => {
    setResultado(null)
    setIntento((n) => n + 1)
  }, [])

  return { ...estadoVistaPrevia(resultado, datos), recargar }
}
