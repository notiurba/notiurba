import { useState } from 'react'
import { Link, Navigate, useNavigate } from 'react-router-dom'
import { publicarReporte } from '../../compartido/api/reportes'
import { obtenerPerfil } from '../../compartido/api/usuario'
import { estadoSesion } from '../../compartido/auth/sesion'
import Aviso from '../../compartido/componentes/avisos/Aviso'
import Boton from '../../compartido/componentes/Boton'
import Casilla from '../../compartido/componentes/formulario/Casilla'
import EncabezadoPantalla from '../../compartido/componentes/EncabezadoPantalla'
import FormularioPantalla from '../../compartido/componentes/formulario/FormularioPantalla'
import HojaConfirmacion from '../../compartido/componentes/avisos/HojaConfirmacion'
import HojaInferior from '../../compartido/componentes/avisos/HojaInferior'
import { IconoCorreo, IconoUsuario } from '../../compartido/componentes/Iconos'
import PasosBarra from '../../compartido/componentes/PasosBarra'
import useCargar from '../../compartido/hooks/useCargar'
import { armarSolicitudReporte, MIN_DESCRIPCION } from '../../compartido/validaciones/reporte'
import { useReporte } from './useReporte'
import './Reportar.css'
import './PasoConfirmacion.css'

// Una fila del resumen: etiqueta, valor y enlace «Cambiar» al paso donde se edita.
function FilaResumen({ etiqueta, children, a }) {
  return (
    <div className="confirmacion__fila">
      <div>
        <span>{etiqueta}</span>
        <strong>{children}</strong>
      </div>
      <Link to={a}>Cambiar</Link>
    </div>
  )
}

const sinSesion = () => Promise.resolve(null)

// Paso 4 de 4: resumen, autorizaciones y envío. Solo aquí se pide la sesión.
export default function PasoConfirmacion() {
  const navigate = useNavigate()
  const { borrador, fotos, limpiar, categorias, barrios } = useReporte()
  const conSesion = estadoSesion() === 'activa'
  // Con sesión se muestra «Sesión iniciada como…». Sin sesión, la función fija devuelve null.
  const { datos: perfil } = useCargar(conSesion ? obtenerPerfil : sinSesion)
  const [autorizaEnvio, setAutorizaEnvio] = useState(false)
  const [hoja, setHoja] = useState(null) // 'enviar' | 'sesion' | null
  const [enviando, setEnviando] = useState(false)
  const [errorEnvio, setErrorEnvio] = useState('')
  const [enviado, setEnviado] = useState(false)

  const categoria = categorias?.find((c) => String(c.idCategoria) === borrador.idCategoria)
  const barrio = barrios?.find((b) => String(b.idBarrio) === borrador.idBarrio)
  const pideUbicacion = categoria?.requiereUbicacion !== false
  // Ya se envió: el borrador se vació y no hay nada que mostrar mientras se abre la pantalla final.
  if (enviado) return null
  if (!borrador.idCategoria) return <Navigate to="/reportar" replace />
  if (!categoria) return <p className="cargando-texto">Cargando…</p>
  if (pideUbicacion && borrador.latitud == null) return <Navigate to="/reportar/ubicacion" replace />
  if (borrador.descripcion.trim().length < MIN_DESCRIPCION) return <Navigate to="/reportar/detalles" replace />

  function pedirEnvio(evento) {
    evento.preventDefault()
    setHoja(conSesion ? 'enviar' : 'sesion')
  }

  async function enviar() {
    setEnviando(true)
    setErrorEnvio('')
    try {
      // Una categoría sin ubicación no lleva punto ni barrio, aunque la persona haya marcado uno antes de cambiarla.
      const datos = pideUbicacion ? borrador : { ...borrador, latitud: null, longitud: null, idBarrio: '' }
      const { codigoSeguimiento, fotosFallidas } = await publicarReporte(armarSolicitudReporte(datos), fotos)
      setEnviado(true)
      limpiar()
      navigate(`/reportar/enviado/${codigoSeguimiento}`, { replace: true, state: { fotosFallidas } })
    } catch (error) {
      setErrorEnvio(error.mensaje ?? 'No pudimos enviar su reporte. Su información sigue aquí; inténtelo de nuevo.')
      setHoja(null)
      setEnviando(false)
    }
  }

  const lugar = pideUbicacion
    ? [borrador.direccion.trim(), barrio && `Barrio ${barrio.nombre} · Comuna ${barrio.idComuna}`].filter(Boolean)
    : []
  if (pideUbicacion && lugar.length === 0) lugar.push('Punto marcado en el mapa')

  return (
    <>
      <EncabezadoPantalla titulo="Nuevo reporte" atras="/reportar/detalles" />

      <FormularioPantalla
        alEnviar={pedirEnvio}
        acciones={
          <Boton type="submit" disabled={!autorizaEnvio}>
            <IconoCorreo tamano={20} />
            Enviar reporte
          </Boton>
        }
      >
        <PasosBarra paso={4} total={4} etiqueta="Confirmación" />
        <h2 className="reportar__titulo">Revise su reporte</h2>

        <div className="confirmacion__resumen">
          <FilaResumen etiqueta="Tipo de problema" a="/reportar">
            {categoria.nombre}
          </FilaResumen>
          {pideUbicacion && (
            <FilaResumen etiqueta="Lugar" a="/reportar/ubicacion">
              {lugar.map((linea) => (
                <span key={linea}>{linea}</span>
              ))}
            </FilaResumen>
          )}
          <FilaResumen etiqueta="Descripción" a="/reportar/detalles">
            «{borrador.descripcion.trim()}»
          </FilaResumen>
          <FilaResumen etiqueta="Fotos" a="/reportar/detalles">
            {fotos.length === 0 ? 'Sin fotos' : `${fotos.length} ${fotos.length === 1 ? 'foto adjunta' : 'fotos adjuntas'}`}
          </FilaResumen>
        </div>

        <Aviso variante="info" titulo="¿Cómo se envía?">
          NotiUrba envía el reporte por usted a la Alcaldía de Tuluá (CAIC) con su correo como dirección
          de respuesta. La entidad le responderá directamente a ese correo.
        </Aviso>

        {conSesion ? (
          <Aviso variante="exito" titulo="Sesión iniciada como">
            {perfil?.correo ?? '…'}
          </Aviso>
        ) : (
          <Aviso variante="alerta" titulo="Falta iniciar sesión">
            Necesitamos su correo para que la entidad pueda responderle. Se lo pediremos al enviar.
          </Aviso>
        )}

        {errorEnvio && (
          <Aviso variante="error" titulo="No pudimos enviar el reporte">
            {errorEnvio}
          </Aviso>
        )}

        <Casilla id="autorizaEnvio" checked={autorizaEnvio} onChange={(e) => setAutorizaEnvio(e.target.checked)}>
          Autorizo a NotiUrba a enviar este reporte a la entidad competente, a mi nombre y con mis datos,
          ubicación y fotos (con enlace público). La entidad podrá responderme a mi correo.
        </Casilla>

        <p className="reportar__mensaje">
          NotiUrba no radica en sistemas oficiales ni reemplaza los canales de la Alcaldía. El código de
          seguimiento es de NotiUrba.
        </p>
      </FormularioPantalla>

      <HojaConfirmacion
        abierta={hoja === 'enviar'}
        alCancelar={() => setHoja(null)}
        alConfirmar={enviar}
        icono={<IconoCorreo tamano={30} />}
        tono="info"
        titulo="¿Enviar el reporte?"
        texto="NotiUrba lo envía a la Alcaldía de Tuluá (CAIC) con su correo como dirección de respuesta. La entidad le responderá directamente a usted."
        textoConfirmar="Sí, enviar"
        textoCancelar="Volver"
        cargando={enviando}
        textoCargando="Enviando…"
      />

      <HojaInferior
        abierta={hoja === 'sesion'}
        alCerrar={() => setHoja(null)}
        icono={<IconoUsuario tamano={30} />}
        tono="info"
        titulo="Inicie sesión para enviar"
        texto="Necesitamos su correo para que la entidad pueda responderle."
      >
        <Boton to="/login" state={{ siguiente: '/reportar/confirmacion' }}>
          Iniciar sesión
        </Boton>
        <Boton to="/registro" variante="secundario">
          Crear cuenta
        </Boton>
        <Boton variante="texto" onClick={() => setHoja(null)}>
          Volver
        </Boton>
      </HojaInferior>
    </>
  )
}

