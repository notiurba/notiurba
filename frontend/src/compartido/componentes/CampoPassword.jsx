import { useState } from 'react'
import Campo from './Campo'
import { IconoCandado, IconoOjo } from './Iconos'

// Campo de contraseña con candado y el ojo para mostrar u ocultar lo escrito.
export default function CampoPassword(props) {
  const [visible, setVisible] = useState(false)

  return (
    <Campo
      {...props}
      type={visible ? 'text' : 'password'}
      icono={<IconoCandado />}
      final={
        <button
          type="button"
          className="campo__ojo"
          onClick={() => setVisible(!visible)}
          aria-label={visible ? 'Ocultar contraseña' : 'Mostrar contraseña'}
          aria-pressed={visible}
        >
          <IconoOjo tachado={visible} />
        </button>
      }
    />
  )
}
