import { useCallback, useEffect, useRef, useState } from 'react'
import { Link, Navigate, useNavigate } from 'react-router-dom'
import * as api from '../servicios/correoReportes'
import { claveRuta, rutaActual, guardarConEvidencias } from '../servicios/recuperacionReporte'
import { CanalOficial } from '../componentes/ResultadoCorreo'
import VistaPreviaMensaje from '../componentes/VistaPreviaMensaje'
import Aviso from '../../compartido/componentes/avisos/Aviso'
import Boton from '../../compartido/componentes/Boton'
import Casilla from '../../compartido/componentes/formulario/Casilla'
import EncabezadoPantalla from '../../compartido/componentes/EncabezadoPantalla'
import FormularioPantalla from '../../compartido/componentes/formulario/FormularioPantalla'
import HojaConfirmacion from '../../compartido/componentes/avisos/HojaConfirmacion'
import HojaInferior from '../../compartido/componentes/avisos/HojaInferior'
import { IconoAlerta, IconoCorreo } from '../../compartido/componentes/Iconos'
import { EnlacePrivacidad } from '../../compartido/componentes/avisos/NotaPrivacidad'
import PasosBarra from '../../compartido/componentes/PasosBarra'
import useCargar from '../../compartido/hooks/useCargar'
import { armarSolicitudReporte, armarVistaPrevia, validarConfirmacionReporte, validarFoto, validarUbicacion, validarDetalles } from '../../compartido/validaciones/reporte'
import { TEXTOS_FOTO_NECESARIA, textoDuplicado, textoLimiteDiario } from '../../compartido/textos/reporte'
import { reducirFoto } from '../../compartido/utilidades/fotos'
import { esEmergencia } from './catalogo'
import { decidirFalloEnvio } from './falloEnvio'
import { useReporte } from './useReporte'
import useVistaPrevia from './useVistaPrevia'
import './Reportar.css'
import './PasoConfirmacion.css'
import '../paginas/CorreoReportes.css'

function FilaResumen({ etiqueta, children, a }) {
  return <div className="confirmacion__fila"><div><span>{etiqueta}</span><strong>{children}</strong></div>
    {a && <Link to={a}>Cambiar</Link>}</div>
}

export default function PasoConfirmacion() {
  const navigate = useNavigate()
  const { borrador, cambiar, fotos, setFotos, categorias, barrios, guardado, conservarGuardado, cancelarReporte } = useReporte()
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
  // La vista previa se pide de nuevo solo si cambian los datos que van en el mensaje.
  const datosPrevia = !guardado && categoria && ruta?.permiteCorreo
    ? JSON.stringify(armarVistaPrevia(borrador, { emergencia, categoria: { ...categoria, ...ruta }, totalFotos: fotos.length }))
    : null
  const previa = useVistaPrevia(datosPrevia)
  const [aceptacion, setAceptacion] = useState(null)
  const [hoja, setHoja] = useState(false)
  const [enviando, setEnviando] = useState(false)
  const [error, setError] = useState('')
  const [errorAutorizacion, setErrorAutorizacion] = useState('')
  const [preparando, setPreparando] = useState(false)
  // Respuestas del servicio que piden una decisión: reporte parecido (datos base) o límite del día.
  const [parecido, setParecido] = useState(null)
  const [limite, setLimite] = useState(null)
  const ocupado = useRef(false)
  // Se invalida al cambiar la selección/ruta; consentimiento de registro no interviene aquí.
  const claveAceptacion = JSON.stringify([clave, ruta?.idCanal, ruta?.acceso, ruta?.permiteCorreo])
  const autorizaEnvio = aceptacion === claveAceptacion
  const faltanFotos = guardado ? Math.max(0, guardado.totalFotos - guardado.fotosSubidas) : 0
  const archivosListos = !guardado || fotos.length === faltanFotos
  // En este lugar se repite un problema ya resuelto: sin una foto no se puede enviar.
  const faltaFoto = !guardado && fotos.length === 0 && (previa.datos?.requiereEvidencia === true || borrador.fotoObligatoria)
  const pideEvidencia = previa.datos?.requiereEvidencia === true
  // El paso 3 debe decir «(obligatoria)» aunque la persona vuelva por «Cambiar» y no por «Agregar foto».
  useEffect(() => {
    if (pideEvidencia && !borrador.fotoObligatoria) cambiar({ fotoObligatoria: true })
  }, [pideEvidencia, borrador.fotoObligatoria, cambiar])

  if (!guardado) {
    if (!borrador.idCategoria) return <Navigate to="/reportar" replace />
    if (!categorias) return <p className="cargando-texto">Cargando…</p>
    if (!categoria) return <Navigate to="/reportar" replace />
    if (Object.keys(validarUbicacion(borrador, categoria)).length) return <Navigate to="/reportar/ubicacion" replace />
    if (Object.keys(validarDetalles(borrador)).length) return <Navigate to="/reportar/detalles" replace />
  }

  function comprobar() {
    if (!ruta || (!guardado && (consulta.cargando || consulta.error))) {
      setError('Espere un momento a que confirmemos a dónde va su reporte, o inténtelo de nuevo.')
      return false
    }
    if (!autorizaEnvio) {
      setErrorAutorizacion('Autorice el envío de este reporte para continuar.')
      return false
    }
    if (faltaFoto) {
      setError('')
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

  function agregarFoto() {
    cambiar({ fotoObligatoria: true })
    navigate('/reportar/detalles')
  }

  async function enviar(confirmarDuplicado = false) {
    if (ocupado.current || preparando) return
    if (!comprobar()) {
      // Si la hoja tapa el motivo (por ejemplo, hace falta una foto), se cierra para que se vea.
      setHoja(false)
      setParecido(null)
      return
    }
    ocupado.current = true
    setEnviando(true)
    setError('')
    let avance = guardado
    try {
      const resultado = await guardarConEvidencias({
        datos: armarSolicitudReporte(borrador, { autorizaEnvio, emergencia, categoria: { ...categoria, ...ruta },
          totalFotos: fotos.length, confirmarDuplicado }),
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
      navigate('/reportar/enviado/' + resultado.codigoSeguimiento, { replace: true, state: { envio: resultado.envio } })
    } catch (e) {
      const decision = decidirFalloEnvio(e, avance)
      setHoja(false)
      setParecido(decision.parecido)
      setLimite(decision.limite)
      if (decision.fotoObligatoria) cambiar({ fotoObligatoria: true })
      setError(decision.error)
    } finally { ocupado.current = false; setEnviando(false) }
  }

  const barrio = barrios.find(b => String(b.idBarrio) === borrador.idBarrio)
  const lugar = categoria?.requiereUbicacion === false ? 'No requerida'
    : [borrador.direccion, barrio?.nombre, borrador.latitud != null && 'Punto marcado en el mapa'].filter(Boolean).join(' · ')
  const accion = guardado ? 'Reintentar' : ruta?.permiteCorreo ? 'Guardar y enviar' : 'Guardar reporte'
  const avisoParecido = parecido && textoDuplicado(parecido)
  const avisoLimite = limite && textoLimiteDiario(limite)

  return <>
    <EncabezadoPantalla titulo="Nuevo reporte" atras={guardado ? '/app/reportes' : '/reportar/detalles'} />
    <FormularioPantalla alEnviar={pedirEnvio} acciones={<Boton type="submit" disabled={!ruta || !autorizaEnvio || !archivosListos || faltaFoto || preparando || (!guardado && (consulta.cargando || Boolean(consulta.error)))} cargando={enviando} textoCargando="Guardando…"><IconoCorreo tamano={20} />{accion}</Boton>}>
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
      {datosPrevia && <VistaPreviaMensaje previa={previa} entidad={ruta.entidad} />}
      {!guardado && consulta.cargando && <p role="status">Buscando a qué entidad va su reporte…</p>}
      {!guardado && consulta.error && <Aviso variante="error" titulo="No pudimos confirmar la entidad"><button type="button" className="enlace-boton" onClick={consulta.recargar}>Reintentar</button></Aviso>}
      <CanalOficial ruta={ruta} />
      {ruta?.competenciaConfirmada === false && <Aviso variante="alerta">Todavía estamos confirmando qué entidad atiende este tipo de reportes. Si es urgente, no espere: llame a la línea de emergencias.</Aviso>}
      {ruta && <Aviso>{ruta.permiteCorreo ? 'Enviaremos su reporte por correo a la entidad indicada. Eso no asegura que lo lean de inmediato.' : 'Guardaremos su reporte. Esta entidad atiende por otro canal: el que ve arriba.'}</Aviso>}
      {faltaFoto && <Aviso variante="alerta" titulo={TEXTOS_FOTO_NECESARIA.titulo}>{TEXTOS_FOTO_NECESARIA.texto}{' '}
        <button type="button" className="enlace-boton" onClick={agregarFoto}>{TEXTOS_FOTO_NECESARIA.accion}</button></Aviso>}
      {error && <Aviso variante="error" titulo={guardado ? 'Su reporte quedó guardado; falta terminar' : 'No pudimos guardar el reporte'}>{error}</Aviso>}
      <Casilla id="autorizaEnvio" checked={autorizaEnvio} disabled={enviando} error={errorAutorizacion} onChange={e => { setAceptacion(e.target.checked ? claveAceptacion : null); setErrorAutorizacion('') }}>
        Autorizo a NotiUrba a enviar este reporte a la entidad, a mi nombre y con mis datos, la ubicación y las fotos (las fotos van como enlace público). La entidad podrá responderme a mi correo. <EnlacePrivacidad nuevaPestana />
      </Casilla>
      <p className="reportar__mensaje">Este código es de NotiUrba para su seguimiento. El radicado oficial lo asigna la entidad.</p>
    </FormularioPantalla>
    <HojaConfirmacion abierta={hoja} alCancelar={() => !ocupado.current && setHoja(false)} alConfirmar={() => enviar()} icono={<IconoCorreo tamano={30} />} tono="info" titulo={accion} texto={ruta?.permiteCorreo ? 'Guardaremos su reporte y lo enviaremos a ' + ruta.entidad + '. Verá el resultado en la siguiente pantalla.' : 'Guardaremos su reporte. Esta entidad atiende por otro canal; en la siguiente pantalla verá cuál.'} textoConfirmar="Confirmar" textoCancelar="Volver" cargando={enviando} textoCargando="Guardando…" />
    {/* Tocar fuera solo cierra la hoja: el borrador se descarta únicamente con «Cancelar reporte». */}
    <HojaInferior abierta={Boolean(avisoParecido)} alCerrar={() => !ocupado.current && setParecido(null)}
      icono={<IconoAlerta tamano={30} />} tono="alerta" titulo={avisoParecido?.titulo}
      texto={avisoParecido && <>{avisoParecido.texto}{avisoParecido.codigo && <>{' '}
        <Link to={'/app/reportes/' + avisoParecido.codigo}>Ver el reporte {avisoParecido.codigo}</Link></>}</>}>
      <Boton onClick={() => enviar(true)} cargando={enviando} textoCargando="Guardando…">Enviar de todas maneras</Boton>
      <Boton variante="texto" onClick={cancelarReporte} disabled={enviando}>Cancelar reporte</Boton>
    </HojaInferior>
    <HojaInferior abierta={Boolean(avisoLimite)} alCerrar={() => setLimite(null)} icono={<IconoAlerta tamano={30} />}
      tono="alerta" titulo={avisoLimite?.titulo} texto={avisoLimite?.texto}>
      <Boton onClick={() => setLimite(null)}>Entendido</Boton>
      <Boton variante="texto" to="/app/reportes">Ver mis reportes</Boton>
    </HojaInferior>
  </>
}
