import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { peticion } from '../../compartido/api/cliente'
import './CorreoReportes.css'

export default function MisReportes() {
  const [reportes, setReportes] = useState(null)
  const [error, setError] = useState('')
  useEffect(() => { let activo = true; peticion('/api/reportes').then(r => { if (activo) setReportes(r) }).catch(e => { if (activo) setError(e.message) }); return () => { activo = false } }, [])
  return <main className="correo-flujo"><h1>Mis reportes</h1>{error && <p role="alert">{error}</p>}{reportes === null && !error && <p role="status">Consultando…</p>}{reportes?.length === 0 && <p>No tienes reportes guardados.</p>}<ul className="correo-lista">{reportes?.map(r => <li key={r.codigoSeguimiento}><Link to={`/app/consultar-reporte?codigo=${r.codigoSeguimiento}`}><strong>{r.codigoSeguimiento}</strong> · {r.categoria}<p>{r.estado} · Correo: {r.correo || 'SIN SOLICITAR'}</p></Link></li>)}</ul><Link className="correo-primario" to="/app/crear-reporte">Crear reporte</Link></main>
}
