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

  return { datos, setDatos, errores, setErrores, cambiar }
}
