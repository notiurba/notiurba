import { useState } from 'react'

// Estado común de los formularios: lo escrito, los errores por campo y el manejador `cambiar`.
// Lo usan el registro, «Complete su perfil», «Editar mis datos» y los pasos del reporte.
export default function useFormulario(inicial) {
  const [datos, setDatos] = useState(inicial)
  const [errores, setErrores] = useState({})

  // Devuelve el manejador de un campo: guarda lo escrito y borra el error de ese campo.
  function cambiar(campo) {
    return (evento) => {
      const { type, checked, value } = evento.target
      setDatos((anteriores) => ({ ...anteriores, [campo]: type === 'checkbox' ? checked : value }))
      setErrores((anteriores) => ({ ...anteriores, [campo]: '' }))
    }
  }

  // Si la API rechazó una casilla concreta (error.campo), muestra su mensaje bajo ella y devuelve true.
  // Si el error no es de una casilla, no hace nada y devuelve false (la pantalla muestra un aviso general).
  function errorDeCampo(error) {
    if (!error.campo || !(error.campo in datos)) return false
    setErrores({ [error.campo]: error.mensaje ?? 'Revise este dato' })
    return true
  }

  return { datos, errores, setErrores, cambiar, errorDeCampo }
}
