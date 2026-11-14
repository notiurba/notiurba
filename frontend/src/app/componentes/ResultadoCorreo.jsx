import { useState } from 'react'
import { Link } from 'react-router-dom'
import { enlaceCanal, presentacionCorreo } from './estadoCorreo'
import ListaCanales from './ListaCanales'
import iconoCorreo from '../../compartido/assets/correo/mail.svg'
import iconoCerrar from '../../compartido/assets/correo/close.svg'
import iconoCopiar from '../../compartido/assets/correo/copy.svg'
import iconoRecibido from '../../compartido/assets/correo/clock.svg'
import iconoEnviado from '../../compartido/assets/correo/send.svg'
import iconoInfo from '../../compartido/assets/correo/info.svg'

export function CanalOficial({ ruta }) {
  if (!ruta) return null
  const enlace = enlaceCanal(ruta)
  return <aside className="correo-aviso"><strong>{ruta.entidad}</strong><p>{ruta.dependencia}</p><p>{ruta.mensaje}</p>
    {!ruta.permiteCorreo && (ruta.canales?.length ? <ListaCanales canales={ruta.canales} /> : enlace ? <a href={enlace} target={enlace.startsWith('https:') ? '_blank' : undefined} rel="noopener noreferrer">Abrir canal oficial</a> : <p>{ruta.acceso}</p>)}
    {ruta.revisionClasificacion && <p>Revisaremos que su reporte esté en la categoría correcta.</p>}
  </aside>
}

export default function ResultadoCorreo({ reporte, envio, onEnviar, ocupado }) {
  const estado = presentacionCorreo(envio)
  const [copia, setCopia] = useState('')
  async function copiar() {
    try { await navigator.clipboard.writeText(reporte.codigoSeguimiento); setCopia('Código copiado') }
    catch { setCopia('Selecciona el código para copiarlo') }
  }
  return <section className="correo-resultado" aria-labelledby="titulo-resultado">
    <header><Link className="correo-cerrar" to="/app" aria-label="Cerrar resultado"><img src={iconoCerrar} alt="" /></Link></header>
    <div className="correo-contenido">
      <div className="correo-hero" aria-hidden="true"><img src={iconoCorreo} alt="" /></div>
      <h1 id="titulo-resultado">{estado.titulo}</h1><p className="correo-intro" role="status">{estado.mensaje}</p>
      <dl className="correo-resumen"><div><dt>Código de seguimiento NotiUrba</dt><dd className="correo-codigo">{reporte.codigoSeguimiento}<button type="button" onClick={copiar} aria-label="Copiar código"><img src={iconoCopiar} alt="" /></button></dd></div>
        <div><dt>Estado</dt><dd><span className={`correo-chip ${reporte.estado === 'Enviado' ? 'enviado' : ''}`}>{['Recibido', 'Enviado'].includes(reporte.estado) && <img src={reporte.estado === 'Enviado' ? iconoEnviado : iconoRecibido} alt="" />}{reporte.estado}</span></dd></div>
        <div><dt>Tipo de problema</dt><dd>{reporte.categoria?.nombre}</dd></div>
        <div><dt>Lugar</dt><dd>{reporte.barrio?.nombre || 'Sin barrio'}{reporte.comuna ? ` · Comuna ${reporte.comuna}` : ''}</dd></div>
      </dl>{copia && <p role="status">{copia}</p>}
      <div className={`correo-estado ${estado.clase}`} aria-live="polite"><strong>CORREO · {estado.etiqueta}</strong><h2>{estado.subtitulo}</h2><p>{envio?.rebote ? `El reporte conserva su estado «${reporte.estado}». La falla está registrada por separado.` : 'El reporte se conserva con su código de seguimiento. No necesitas registrarlo otra vez.'}</p></div>
      {(envio?.estado === 'SIN_SOLICITAR' || (envio?.estado === 'FALLIDO' && envio.reintentable && !envio.rebote)) && <button className="correo-secundario" disabled={ocupado} onClick={onEnviar}>{ocupado ? 'Solicitando…' : 'Reintentar envío'}</button>}
      {envio?.estado === 'NO_ENVIADO_CORREO' && <CanalOficial ruta={envio.ruta} />}
      <p className="correo-aviso correo-aviso-icono"><img src={iconoInfo} alt="" /><span>Este código es de NotiUrba, no el radicado de la entidad. Guárdelo para consultar el avance.</span></p>
    </div>
    <footer><Link className="correo-primario" to="/app/mis-reportes">Ver mis reportes</Link><Link className="correo-secundario" to="/app/crear-reporte">Hacer otro reporte</Link></footer>
  </section>
}
