import { useState } from 'react'
import useUnaVez from '../../compartido/hooks/useUnaVez'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { eliminarMiCuenta, eliminarMisDatos, obtenerPerfil } from '../../compartido/api/usuario'
import { cerrarSesion } from '../../compartido/auth/sesion'
import Aviso from '../../compartido/componentes/avisos/Aviso'
import Boton from '../../compartido/componentes/Boton'
import EncabezadoPantalla from '../../compartido/componentes/EncabezadoPantalla'
import HojaConfirmacion from '../../compartido/componentes/avisos/HojaConfirmacion'
import {
  IconoAlerta,
  IconoChevronDerecha,
  IconoEscudo,
  IconoPapelera,
  IconoSalir,
  IconoUsuario,
  IconoUsuarioX,
} from '../../compartido/componentes/Iconos'
import useCargar from '../../compartido/hooks/useCargar'
import FilaAjuste from '../componentes/FilaAjuste'
import './Ajustes.css'

const AVISO_CORREOS = 'Los correos que ya se enviaron a las entidades no se pueden retirar.'

const iniciales = (perfil) => `${perfil.nombres[0] ?? ''}${perfil.apellidos[0] ?? ''}`.toUpperCase()

// Ajustes: perfil, edición de datos, privacidad, eliminación de datos y cuenta, y cerrar sesión.
export default function Ajustes() {
  const navigate = useNavigate()
  const { state } = useLocation()
  const { datos: perfil } = useCargar(obtenerPerfil)
  const [hoja, setHoja] = useState(null) // 'datos' | 'cuenta' | null
  const [trabajando, setTrabajando] = useState(false)
  // Un doble clic o un doble Enter no repite el envío mientras se espera la respuesta.
  const confirmarUnaVez = useUnaVez(confirmar)
  const [fallo, setFallo] = useState(false)
  const [mensaje, setMensaje] = useState(state?.mensaje ?? '')

  function salir() {
    cerrarSesion()
    navigate('/', { replace: true })
  }

  async function confirmar() {
    setTrabajando(true)
    setFallo(false)
    try {
      if (hoja === 'datos') {
        await eliminarMisDatos()
        setMensaje('Sus reportes y fotos fueron eliminados.')
        setHoja(null)
      } else {
        await eliminarMiCuenta()
        salir()
        return
      }
    } catch {
      setFallo(true)
    }
    setTrabajando(false)
  }

  return (
    <>
      <EncabezadoPantalla titulo="Ajustes" sinVolver />

      <div className="ajustes">
        {mensaje && <Aviso variante="exito">{mensaje}</Aviso>}
        {fallo && <Aviso variante="error">No pudimos completar la acción. Inténtelo de nuevo.</Aviso>}

        {perfil && (
          <Link to="/app/ajustes/datos" className="ajustes__perfil">
            <span className="ajustes__avatar" aria-hidden="true">
              {iniciales(perfil)}
            </span>
            <span className="ajustes__nombre">
              <strong>
                {perfil.nombres} {perfil.apellidos}
              </strong>
              <span>{perfil.correo}</span>
            </span>
            <IconoChevronDerecha tamano={18} />
          </Link>
        )}

        <h2 className="ajustes__seccion">Mi cuenta</h2>
        <div className="ajustes__grupo">
          <FilaAjuste
            to="/app/ajustes/datos"
            icono={<IconoUsuario tamano={20} />}
            titulo="Editar mis datos"
            descripcion="Nombre, documento, teléfono y dirección"
          />
        </div>

        <h2 className="ajustes__seccion">Privacidad y datos</h2>
        <div className="ajustes__grupo">
          <FilaAjuste
            to="/aviso-privacidad"
            icono={<IconoEscudo tamano={20} />}
            titulo="Aviso de privacidad"
            descripcion="Cómo protegemos sus datos (Ley 1581)"
          />
          <FilaAjuste
            peligro
            alPulsar={() => setHoja('datos')}
            icono={<IconoPapelera tamano={20} />}
            titulo="Eliminar mis datos"
            descripcion="Borra sus reportes y fotos"
          />
          <FilaAjuste
            peligro
            alPulsar={() => setHoja('cuenta')}
            icono={<IconoUsuarioX tamano={20} />}
            titulo="Eliminar mi cuenta"
            descripcion="Borra su cuenta y todos sus datos"
          />
        </div>

        <Boton variante="secundario" onClick={salir}>
          <IconoSalir tamano={20} />
          Cerrar sesión
        </Boton>
      </div>

      <HojaConfirmacion
        abierta={hoja === 'datos'}
        alCancelar={() => setHoja(null)}
        alConfirmar={confirmarUnaVez}
        icono={<IconoAlerta tamano={30} />}
        tono="peligro"
        titulo="¿Eliminar sus datos?"
        texto="Se borrarán sus reportes y sus fotos. Su cuenta seguirá activa."
        aviso={AVISO_CORREOS}
        textoConfirmar="Sí, eliminar mis datos"
        peligro
        cargando={trabajando}
        textoCargando="Eliminando…"
      />
      <HojaConfirmacion
        abierta={hoja === 'cuenta'}
        alCancelar={() => setHoja(null)}
        alConfirmar={confirmarUnaVez}
        icono={<IconoAlerta tamano={30} />}
        tono="peligro"
        titulo="¿Eliminar su cuenta?"
        texto="Se borrarán su perfil, sus reportes y sus fotos. Esta acción no se puede deshacer."
        aviso={AVISO_CORREOS}
        textoConfirmar="Sí, eliminar mi cuenta"
        textoCancelar="No, volver"
        peligro
        cargando={trabajando}
        textoCargando="Eliminando…"
      />
    </>
  )
}
