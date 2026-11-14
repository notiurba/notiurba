import { useCallback, useEffect, useState } from 'react'

// Carga datos al abrir la pantalla y avisa en qué estado va: cargando, listo o con error.
// `error` es null mientras no falle; si falla trae el error recibido (por ejemplo, un ErrorApi con `estado`).
// `cargador` es una función que devuelve una promesa; debe ser una función fija
// (definida fuera del componente) para que no se vuelva a pedir en cada render.
// `recargar()` vuelve a pedir los datos (por ejemplo, después de guardar un cambio o con «Reintentar»).
export default function useCargar(cargador) {
  const [estado, setEstado] = useState({ datos: null, cargando: true, error: null })
  const [intento, setIntento] = useState(0)

  useEffect(() => {
    let activo = true
    cargador()
      .then((datos) => activo && setEstado({ datos, cargando: false, error: null }))
      .catch((fallo) => activo && setEstado({ datos: null, cargando: false, error: fallo ?? new Error('Falló la carga') }))
    // Si la pantalla se cierra antes de que llegue la respuesta, se ignora.
    return () => {
      activo = false
    }
  }, [cargador, intento])

  const recargar = useCallback(() => {
    setEstado((anterior) => ({ ...anterior, cargando: true, error: null }))
    setIntento((n) => n + 1)
  }, [])

  return { ...estado, recargar }
}
