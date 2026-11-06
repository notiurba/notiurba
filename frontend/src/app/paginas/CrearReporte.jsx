import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import * as api from '../servicios/correoReportes'
import { CanalOficial } from '../componentes/ResultadoCorreo'
import { TEXTO_CASILLA_ENVIO } from '../../compartido/textos/avisoPrivacidad'
import './CorreoReportes.css'

export default function CrearReporte() {
  const navigate = useNavigate()
  const [opciones, setOpciones] = useState([])
  const [barrios, setBarrios] = useState([])
  const [paso, setPaso] = useState(1)
  const [datos, setDatos] = useState({ idCategoria: '', descripcion: '', latitud: '', longitud: '', idBarrio: '', direccionIncidente: '', matriculaCuentaServicio: '', emergencia: false, autorizaEnvio: false })
  const [ruta, setRuta] = useState(null)
  const [fotos, setFotos] = useState([])
  const [error, setError] = useState('')
  const [ocupado, setOcupado] = useState(false)
  const [guardado, setGuardado] = useState(null)
  useEffect(() => { let activo = true; Promise.all([api.categorias(), api.barrios()]).then(([c, b]) => { if (activo) { setOpciones(c); setBarrios(b) } }).catch(e => { if (activo) setError(e.message) }); return () => { activo = false } }, [])
  const seleccion = opciones.find(c => c.idCategoria === Number(datos.idCategoria))
  const riesgoGas = Boolean(seleccion?.esGas && /(fuga|olor.*gas|escape.*gas)/i.test(datos.descripcion))
  useEffect(() => {
    let activo = true
    if (datos.idCategoria) api.resolver(datos.idCategoria, datos.emergencia || riesgoGas).then(r => { if (activo) setRuta(r) }).catch(e => { if (activo) setError(e.message) })
    return () => { activo = false }
  }, [datos.idCategoria, datos.emergencia, riesgoGas])
  function campo(e) { const { name, value, type, checked } = e.target; setDatos(d => ({ ...d, [name]: type === 'checkbox' ? checked : value, ...(name === 'idCategoria' ? { autorizaEnvio: false, matriculaCuentaServicio: '' } : {}) })) }
  function ubicar() {
    if (!navigator.geolocation) { setError('Este navegador no ofrece GPS. Puedes elegir un barrio.'); return }
    navigator.geolocation.getCurrentPosition(p => setDatos(d => ({ ...d, latitud: String(p.coords.latitude), longitud: String(p.coords.longitude) })), () => setError('No pudimos obtener GPS. Elige un barrio o indica las coordenadas.'))
  }
  async function continuar(e) {
    e.preventDefault(); setError('')
    if (paso < 4) { setPaso(paso + 1); return }
    if (!datos.autorizaEnvio || !ruta) return
    setOcupado(true)
    try {
      // Conservar el código antes de cualquier subida o intento: jamás crear otro reporte por un fallo de correo.
      const creado = guardado || await api.guardarReporte({ ...datos, idCategoria: Number(datos.idCategoria), idBarrio: datos.idBarrio ? Number(datos.idBarrio) : null, latitud: datos.latitud ? Number(datos.latitud) : null, longitud: datos.longitud ? Number(datos.longitud) : null, emergencia: datos.emergencia || riesgoGas })
      setGuardado(creado)
      for (const foto of fotos) await api.subirFoto(creado.codigoSeguimiento, foto)
      await api.enviar(creado.codigoSeguimiento)
      navigate(`/app/consultar-reporte?codigo=${encodeURIComponent(creado.codigoSeguimiento)}`)
    } catch (e) { setError(e.message) } finally { setOcupado(false) }
  }
  return <main className="correo-flujo"><h1>Crear reporte</h1><p>Paso {paso} de 4</p>
    {error && <p role="alert" className="correo-error">{error}</p>}
    {guardado ? <section><h2>Tu reporte está guardado</h2><p>Código: {guardado.codigoSeguimiento}. No lo registres otra vez. Puedes revisar las fotos y solicitar el envío desde su consulta.</p><button className="correo-primario" onClick={() => navigate(`/app/consultar-reporte?codigo=${guardado.codigoSeguimiento}`)}>Consultar reporte guardado</button></section> : <form onSubmit={continuar}>
    {paso === 1 && <><label>Categoría<select name="idCategoria" required value={datos.idCategoria} onChange={campo}><option value="">Selecciona</option>{opciones.map(c => <option key={c.idCategoria} value={c.idCategoria}>{c.codigo} · {c.categoria}</option>)}</select></label><CanalOficial ruta={ruta} /><label className="correo-check"><input type="checkbox" name="emergencia" checked={datos.emergencia} onChange={campo} />Es una emergencia o situación de riesgo inmediato</label></>}
    {paso === 2 && <><p>Ubicación del incidente. Usa GPS, coordenadas o un barrio disponible. No se modifica la cartografía.</p><button type="button" className="correo-secundario" onClick={ubicar}>Usar mi ubicación GPS</button><div className="correo-columnas"><label>Latitud<input name="latitud" type="number" step="any" min="-90" max="90" value={datos.latitud} onChange={campo} /></label><label>Longitud<input name="longitud" type="number" step="any" min="-180" max="180" value={datos.longitud} onChange={campo} /></label></div><label>Barrio<select name="idBarrio" value={datos.idBarrio} onChange={campo} required={seleccion?.requiereUbicacion && !datos.latitud}><option value="">Selecciona un barrio</option>{barrios.map(b => <option key={b.idBarrio} value={b.idBarrio}>{b.nombre} · Comuna {b.idComuna}</option>)}</select></label><label>Dirección del incidente<input name="direccionIncidente" maxLength="300" value={datos.direccionIncidente} onChange={campo} /></label></>}
    {paso === 3 && <><label>Descripción del problema<textarea name="descripcion" minLength="10" maxLength="500" required value={datos.descripcion} onChange={campo} /></label>{seleccion?.admiteMatricula && <label>Matrícula del servicio (opcional)<input name="matriculaCuentaServicio" maxLength="50" value={datos.matriculaCuentaServicio} onChange={campo} /></label>}<label>Fotografías opcionales (hasta 3, JPG/PNG, 1 MB cada una)<input type="file" accept="image/jpeg,image/png" multiple onChange={e => { const archivos = Array.from(e.target.files); if (archivos.length > 3 || archivos.some(f => f.size > 1048576 || !['image/jpeg', 'image/png'].includes(f.type))) { setError('Selecciona hasta 3 fotos JPG o PNG de máximo 1 MB.'); e.target.value = ''; setFotos([]) } else { setError(''); setFotos(archivos) } }} /></label></>}
    {paso === 4 && <><h2>Revisa tu reporte</h2><p>{seleccion?.categoria}</p><p>{datos.descripcion}</p><p>{datos.direccionIncidente}</p><p>{fotos.length} evidencias seleccionadas</p><CanalOficial ruta={ruta} /><label className="correo-check"><input type="checkbox" name="autorizaEnvio" required checked={datos.autorizaEnvio} onChange={campo} />{TEXTO_CASILLA_ENVIO}</label><p>Los datos del ciudadano se tomarán de tu cuenta autenticada. No es un canal de emergencias.</p></>}
    {(datos.emergencia || riesgoGas) && <p role="alert" className="correo-emergencia">{seleccion?.esGas ? 'Emergencia de gas: llama al 164' : 'No uses el correo como sustituto de la atención urgente.'} Se guardará el reporte sin envío automático.</p>}
    <div className="correo-acciones">{paso > 1 && <button type="button" className="correo-secundario" disabled={ocupado} onClick={() => setPaso(paso - 1)}>Atrás</button>}<button className="correo-primario" disabled={ocupado || (paso === 4 && (!datos.autorizaEnvio || !ruta))}>{ocupado ? 'Guardando…' : paso === 4 ? 'Guardar reporte' : 'Continuar'}</button></div>
    </form>}
  </main>
}
