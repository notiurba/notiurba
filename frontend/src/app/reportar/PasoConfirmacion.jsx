import { useCallback, useRef, useState } from 'react'
import { Link, Navigate, useNavigate } from 'react-router-dom'
import * as api from '../servicios/correoReportes'
import { claveRuta, rutaActual, guardarConEvidencias } from '../servicios/recuperacionReporte'
import { CanalOficial } from '../componentes/ResultadoCorreo'
import Aviso from '../../compartido/componentes/avisos/Aviso'
import Boton from '../../compartido/componentes/Boton'
import Casilla from '../../compartido/componentes/formulario/Casilla'
import EncabezadoPantalla from '../../compartido/componentes/EncabezadoPantalla'
import FormularioPantalla from '../../compartido/componentes/formulario/FormularioPantalla'
import HojaConfirmacion from '../../compartido/componentes/avisos/HojaConfirmacion'
import { IconoCorreo } from '../../compartido/componentes/Iconos'
import { EnlacePrivacidad } from '../../compartido/componentes/avisos/NotaPrivacidad'
import PasosBarra from '../../compartido/componentes/PasosBarra'
import useCargar from '../../compartido/hooks/useCargar'
import { armarSolicitudReporte, validarConfirmacionReporte, validarFoto, validarUbicacion, validarDetalles } from '../../compartido/validaciones/reporte'
import { reducirFoto } from '../../compartido/utilidades/fotos'
import { esEmergencia } from './catalogo'
import { useReporte } from './useReporte'
import './Reportar.css'
import './PasoConfirmacion.css'
import '../paginas/CorreoReportes.css'

function FilaResumen({ etiqueta, children, a }) {
  return <div className="confirmacion__fila"><div><span>{etiqueta}</span><strong>{children}</strong></div>
    {a && <Link to={a}>Cambiar</Link>}</div>
}

export default function PasoConfirmacion() {
  const navigate = useNavigate()
  const { borrador, fotos, setFotos, limpiar, categorias, barrios, guardado, conservarGuardado } = useReporte()
  const categoria = categorias?.find(c => String(c.idCategoria) === borrador.idCategoria)
  const idCategoria = String(borrador.idCategoria)
  const emergencia = Boolean(esEmergencia(borrador, categoria))
  const clave = String(claveRuta(idCategoria, emergencia))
  const cargarRuta = useCallback(async () => {
    if (!idCategoria) return null
    return { clave, ruta: await api.resolver(idCategoria, emergencia) }
  }, [idCategoria, emergencia, clave])
  const consulta = useCargar(cargarRuta)
  const ruta = guardado?.ruta ?? rutaActual(consulta.datos, clave)
  const [aceptacion, setAceptacion] = useState(null)
  const [hoja, setHoja] = useState(false)
  const [enviando, setEnviando] = useState(false)
  const [error, setError] = useState('')
  const [errorAutorizacion, setErrorAutorizacion] = useState('')
  const [preparando, setPreparando] = useState(false)
  const ocupado = useRef(false)
  // Se invalida al cambiar la selección/ruta; consentimiento de registro no interviene aquí.
  const claveAceptacion = JSON.stringify([clave, ruta?.idCanal, ruta?.acceso, ruta?.permiteCorreo])
  const autorizaEnvio = aceptacion === claveAceptacion
  const faltanFotos = guardado ? Math.max(0, guardado.totalFotos - guardado.fotosSubidas) : 0
  const archivosListos = !guardado || fotos.length === faltanFotos

  if (!guardado) {
    if (!borrador.idCategoria) return <Navigate to="/reportar" replace />
    if (!categorias) return <p className="cargando-texto">Cargando…</p>
    if (!categoria) return <Navigate to="/reportar" replace />
    if (Object.keys(validarUbicacion(borrador, categoria)).length) return <Navigate to="/reportar/ubicacion" replace />
    if (Object.keys(validarDetalles(borrador)).length) return <Navigate to="/reportar/detalles" replace />
  }

  function comprobar() {
    if (!ruta || (!guardado && (consulta.cargando || consulta.error))) {
      setError('Espere a que se confirme el canal de atención o vuelva a intentarlo.')
      return false
    }
    if (!autorizaEnvio) {
      setErrorAutorizacion('Autorice el envío de este reporte para continuar.')
      return false
    }
    if (!archivosListos) {
      setError('Vuelva a seleccionar únicamente las fotos pendientes antes de reintentar.')
      return false
    }
    if (!guardado) {
      const errores = validarConfirmacionReporte(borrador, { ...categoria, ...ruta }, autorizaEnvio)
      if (Object.keys(errores).length) { setError(Object.values(errores)[0]); return false }
    }
    return true
  }

  function pedirEnvio(evento) {
    evento.preventDefault()
    if (ocupado.current || preparando) return
    setError('')
    if (comprobar()) setHoja(true)
  }

  async function recuperarFotos(evento) {
    const elegidas = [...evento.target.files]
    evento.target.value = ''
    if (elegidas.length !== faltanFotos) {
      setError('Seleccione las ' + faltanFotos + ' fotos que faltan. Las fotos ya subidas se conservan.')
      return
    }
    setPreparando(true)
    try {
      const recuperadas = []
      for (const archivo of elegidas) {
        const motivo = validarFoto(archivo, guardado.fotosSubidas + recuperadas.length)
        if (motivo) throw new Error(motivo)
        recuperadas.push(await reducirFoto(archivo))
      }
      setFotos(recuperadas)
      setError('')
    } catch (e) { setError(e.message) } finally { setPreparando(false) }
  }

  async function enviar() {
    if (ocupado.current || preparando || !comprobar()) return
    ocupado.current = true
    setEnviando(true)
    setError('')
    let avance = guardado
    try {
      const resultado = await guardarConEvidencias({
        datos: armarSolicitudReporte(borrador, { autorizaEnvio, emergencia, categoria: { ...categoria, ...ruta } }),
        fotos, guardado, api,
        conservar: creado => {
          avance = avance ?? { ...creado, totalFotos: fotos.length, fotosSubidas: 0, ruta }
          conservarGuardado(avance)
        },
        alSubirFoto: () => {
          avance = { ...avance, fotosSubidas: avance.fotosSubidas + 1 }
          conservarGuardado(avance)
          setFotos(pendientes => pendientes.slice(1))
        },
      })
      limpiar()
      navigate('/reportar/enviado/' + resultado.codigoSeguimiento, { replace: true, state: { envio: resultado.envio } })
    } catch (e) {
      setError(e.codigo === 'FUERA_DE_COBERTURA'
        ? 'El punto está fuera de la cobertura disponible. Vuelva al paso de ubicación y elija otro lugar.'
        : avance
          ? 'Su reporte está guardado. No se completaron las fotos o la solicitud de envío. Reintente sobre este mismo código.'
          : e.mensaje ?? 'No pudimos guardar su reporte. Sus datos siguen aquí; revise su conexión e inténtelo de nuevo.')
      setHoja(false)
    } finally { ocupado.current = false; setEnviando(false) }
  }

  const barrio = barrios.find(b => String(b.idBarrio) === borrador.idBarrio)
  const lugar = categoria?.requiereUbicacion === false ? 'No requerida'
    : [borrador.direccion, barrio?.nombre, borrador.latitud != null && 'Punto marcado en el mapa'].filter(Boolean).join(' · ')
  const accion = guardado ? 'Reintentar sobre este código' : ruta?.permiteCorreo ? 'Guardar y solicitar envío' : 'Guardar reporte'

  return <>
    <EncabezadoPantalla titulo="Nuevo reporte" atras={guardado ? '/app/reportes' : '/reportar/detalles'} />
    <FormularioPantalla alEnviar={pedirEnvio} acciones={<Boton type="submit" disabled={!ruta || !autorizaEnvio || !archivosListos || preparando || (!guardado && (consulta.cargando || Boolean(consulta.error)))} cargando={enviando} textoCargando="Guardando…"><IconoCorreo tamano={20} />{accion}</Boton>}>
      <PasosBarra paso={4} total={4} etiqueta="Revisar y confirmar" />
      <h2 className="reportar__titulo">{guardado ? 'Su reporte está guardado' : 'Revise su reporte'}</h2>
      {guardado ? <>
        <Aviso titulo={'Código ' + guardado.codigoSeguimiento}>No vuelva a registrar este reporte. Se conservan {guardado.fotosSubidas} de {guardado.totalFotos} fotos.</Aviso>
        {!archivosListos && <label>Fotografías pendientes ({faltanFotos})<input type="file" accept="image/jpeg,image/png" multiple disabled={preparando || enviando} onChange={recuperarFotos} /></label>}
        <Boton to={'/app/reportes/' + guardado.codigoSeguimiento} variante="texto">Consultar este reporte</Boton>
      </> : <div className="confirmacion__resumen">
        <FilaResumen etiqueta="Tipo de problema" a="/reportar">{categoria.nombre}</FilaResumen>
        <FilaResumen etiqueta="Lugar" a="/reportar/ubicacion">{lugar}</FilaResumen>
        <FilaResumen etiqueta="Descripción" a="/reportar/detalles">{borrador.descripcion.trim()}</FilaResumen>
        <FilaResumen etiqueta="Fotos" a="/reportar/detalles">{fotos.length ? fotos.length + ' fotos adjuntas' : 'Sin fotos'}</FilaResumen>
      </div>}
      {!guardado && consulta.cargando && <p role="status">Comprobando canal de atención…</p>}
      {!guardado && consulta.error && <Aviso variante="error" titulo="No pudimos confirmar el canal"><button type="button" className="enlace-boton" onClick={consulta.recargar}>Reintentar</button></Aviso>}
      <CanalOficial ruta={ruta} />
      {ruta?.competenciaConfirmada === false && <Aviso variante="alerta">La competencia de esta categoría está pendiente de validación. No es un canal de atención inmediata.</Aviso>}
      {ruta && <Aviso>{ruta.permiteCorreo ? 'Se solicitará el envío a la entidad indicada. La aceptación del proveedor no confirma entrega, lectura ni radicación.' : 'Se guardará el reporte sin correo automático. Continúe por el canal oficial indicado.'}</Aviso>}
      {error && <Aviso variante="error" titulo={guardado ? 'Reporte guardado; operación pendiente' : 'No pudimos guardar el reporte'}>{error}</Aviso>}
      <Casilla id="autorizaEnvio" checked={autorizaEnvio} disabled={enviando} error={errorAutorizacion} onChange={e => { setAceptacion(e.target.checked ? claveAceptacion : null); setErrorAutorizacion('') }}>
        Autorizo a NotiUrba a enviar este reporte a la entidad competente, a mi nombre y con mis datos, ubicación y fotos (con enlace público). La entidad podrá responderme a mi correo. <EnlacePrivacidad nuevaPestana />
      </Casilla>
      <p className="reportar__mensaje">El código de NotiUrba no es un radicado oficial. El consentimiento de registro no sustituye esta autorización de envío.</p>
    </FormularioPantalla>
    <HojaConfirmacion abierta={hoja} alCancelar={() => !ocupado.current && setHoja(false)} alConfirmar={enviar} icono={<IconoCorreo tamano={30} />} tono="info" titulo={accion} texto={ruta?.permiteCorreo ? 'Se guardará y solicitará el envío a ' + ruta.entidad + '. Puede quedar pendiente o fallar; consulte el resultado.' : 'El reporte se guardará sin envío automático de correo. Use el canal oficial indicado.'} textoConfirmar="Confirmar" textoCancelar="Volver" cargando={enviando} textoCargando="Guardando…" />
  </>
}

