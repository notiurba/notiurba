import { useEffect, useState } from 'react'
import { useSearchParams, Link } from 'react-router-dom'
import * as api from '../servicios/correoReportes'
import ResultadoCorreo from '../componentes/ResultadoCorreo'
import './CorreoReportes.css'

export default function ConsultarReporte() {
  const [params, setParams] = useSearchParams()
  const codigo = params.get('codigo') || ''
  const [busqueda, setBusqueda] = useState(codigo)
  const [resultado, setResultado] = useState(null)
  const [error, setError] = useState('')
  const [ocupado, setOcupado] = useState(false)
  const visible = resultado?.reporte.codigoSeguimiento === codigo ? resultado : null
  useEffect(() => {
    if (!codigo || ocupado) return
    let activo = true
    async function cargar() {
      try { const [reporte, envio] = await Promise.all([api.consultar(codigo), api.estadoCorreo(codigo)]); if (activo) setResultado({ reporte, envio }) }
      catch (e) { if (activo) setError(e.message) }
    }
    cargar()
    const timer = setInterval(cargar, 5000)
    return () => { activo = false; clearInterval(timer) }
  }, [codigo, ocupado])
  async function solicitar() {
    setOcupado(true); setError('')
    try { await api.enviar(codigo); const [reporte, envio] = await Promise.all([api.consultar(codigo), api.estadoCorreo(codigo)]); setResultado({ reporte, envio }) }
    catch (e) { setError(e.message) } finally { setOcupado(false) }
  }
  async function agregarFoto(e) {
    const input = e.currentTarget
    const foto = input.files[0]
    if (!foto) return
    if (foto.size > 1048576 || !['image/jpeg', 'image/png'].includes(foto.type)) { setError('Elige una foto JPG o PNG de máximo 1 MB.'); return }
    setOcupado(true)
    try { await api.subirFoto(codigo, foto); setError('') }
    catch (fallo) { setError(fallo.message) }
    finally {
      // La respuesta de la subida puede haberse perdido después de guardarse: consultar antes de reintentar.
      try {
        const reporte = await api.consultar(codigo)
        setResultado(r => r?.reporte.codigoSeguimiento === codigo ? { ...r, reporte } : r)
      } catch { setError(anterior => anterior || 'No pudimos verificar las fotografías guardadas. Consulta el reporte antes de reintentar.') }
      setOcupado(false); input.value = ''
    }
  }
  return <main className="correo-flujo"><form className="correo-busqueda" onSubmit={e => { e.preventDefault(); setError(''); setResultado(null); setParams({ codigo: busqueda.trim().toUpperCase() }) }}><label>Código del reporte<input disabled={ocupado} required pattern="NU-[0-9]{6,}" value={busqueda} onChange={e => setBusqueda(e.target.value.toUpperCase())} placeholder="NU-000123" /></label><button disabled={ocupado} className="correo-secundario">Consultar</button></form>
    {error && <p role="alert" className="correo-error">{error}</p>}
    {visible?.envio.estado === 'SIN_SOLICITAR' && <aside className="correo-aviso"><p>Hay {visible.reporte.fotos.length} fotografías guardadas. Las que hayan fallado al subir no se incluirán en el correo.</p>{visible.reporte.fotos.length < 3 && <label>Agregar fotografía antes de enviar<input type="file" accept="image/jpeg,image/png" disabled={ocupado} onChange={agregarFoto} /></label>}</aside>}
    {visible && <ResultadoCorreo {...visible} onEnviar={solicitar} ocupado={ocupado} />}
    <Link to="/app/crear-reporte">Crear reporte</Link>
  </main>
}
