import { useState } from 'react'
import useUnaVez from '../hooks/useUnaVez'
import { solicitarRecuperacion } from '../api/auth'
import Aviso from '../componentes/avisos/Aviso'
import Boton from '../componentes/Boton'
import CabeceraAuth from '../componentes/acceso/CabeceraAuth'
import Campo from '../componentes/formulario/Campo'
import EncabezadoPantalla from '../componentes/EncabezadoPantalla'
import { IconoCorreo } from '../componentes/Iconos'
import LayoutAuth from '../componentes/acceso/LayoutAuth'
import PieAuth from '../componentes/acceso/PieAuth'
import enfocarPrimerError from '../componentes/formulario/enfocarPrimerError'
import '../estilos/marca.css'
import { LIMITES, validarCorreo } from '../validaciones/campos'

// Pide el correo y la API manda un enlace. La respuesta es la misma exista o no la cuenta
// (así nadie puede averiguar qué correos están registrados).
export default function RecuperarContrasena() {
  const [correo, setCorreo] = useState('')
  const [error, setError] = useState('')
  const [enviando, setEnviando] = useState(false)
  // Un doble clic o un doble Enter no repite el envío mientras se espera la respuesta.
  const enviarUnaVez = useUnaVez(enviar)
  const [respuesta, setRespuesta] = useState('')
  const [fallo, setFallo] = useState(false)

  async function enviar(evento) {
    evento.preventDefault()
    enfocarPrimerError(evento.currentTarget)
    const errorCorreo = validarCorreo(correo)
    setError(errorCorreo)
    setFallo(false)
    if (errorCorreo) return

    setEnviando(true)
    try {
      const { mensaje } = await solicitarRecuperacion(correo.trim())
      setRespuesta(mensaje)
    } catch {
      setFallo(true)
    }
    setEnviando(false)
  }

  return (
    <LayoutAuth>
      <EncabezadoPantalla atras="/login" />

      <form className="formulario-auth" onSubmit={enviarUnaVez} noValidate>
        <CabeceraAuth
          titulo="Recupere su contraseña"
          texto="Escriba el correo de su cuenta. Le enviaremos un enlace para crear una nueva contraseña."
        />

        {respuesta && (
          <Aviso variante="exito" titulo="Revise su correo">
            {respuesta}
          </Aviso>
        )}
        {fallo && (
          <Aviso variante="error" titulo="No pudimos enviar el enlace">
            Revise su conexión e inténtelo de nuevo.
          </Aviso>
        )}

        <Campo
          id="correo"
          etiqueta="Correo electrónico"
          icono={<IconoCorreo />}
          type="email"
          autoComplete="email"
          maxLength={LIMITES.correo}
          placeholder="nombre@correo.com"
          ayuda="Use el correo con el que creó su cuenta."
          value={correo}
          onChange={(e) => {
            setCorreo(e.target.value)
            setError('')
          }}
          error={error}
        />

        <Boton type="submit" cargando={enviando} textoCargando="Enviando…">
          Enviar enlace
        </Boton>

        <Aviso variante="info">
          Por seguridad, el mensaje es el mismo exista o no la cuenta. El enlace vence en 1 hora y
          solo sirve una vez.
        </Aviso>

        <PieAuth anclado texto="¿Ya recordó?" enlace="Iniciar sesión" to="/login" />
      </form>
    </LayoutAuth>
  )
}
