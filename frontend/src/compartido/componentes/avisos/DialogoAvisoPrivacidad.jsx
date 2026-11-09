import { useEffect, useRef } from 'react'
import Boton from '../Boton'
import ContenidoAvisoPrivacidad from './ContenidoAvisoPrivacidad'
import './DialogoAvisoPrivacidad.css'

export default function DialogoAvisoPrivacidad({ alCerrar }) {
  const dialogo = useRef(null)

  useEffect(() => {
    const elemento = dialogo.current
    elemento.showModal()
    return () => elemento.close()
  }, [])

  function cerrar() {
    dialogo.current.close()
    alCerrar()
  }

  return (
    <dialog ref={dialogo} className="dialogo-privacidad" aria-labelledby="titulo-aviso-registro"
      onCancel={(evento) => { evento.preventDefault(); cerrar() }}>
      <header className="dialogo-privacidad__cabecera">
        <h2 id="titulo-aviso-registro">Aviso de privacidad</h2>
        <Boton onClick={cerrar} variante="secundario" autoFocus>Volver al formulario</Boton>
      </header>
      <ContenidoAvisoPrivacidad />
      <Boton onClick={cerrar}>Volver al formulario</Boton>
    </dialog>
  )
}
