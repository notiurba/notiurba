import { useState } from 'react'
import Aviso from '../../compartido/componentes/avisos/Aviso'
import { explicarErrorVistaPrevia } from '../reportar/vistaPrevia'
import './VistaPreviaMensaje.css'

// «Así llegará su mensaje»: solo lectura. Cada dato se cambia en su paso con «Cambiar» del resumen.
export default function VistaPreviaMensaje({ previa, entidad }) {
  const [completo, setCompleto] = useState(false)
  const error = previa.error && explicarErrorVistaPrevia(previa.error)
  return (
    <section className="vista-previa" aria-labelledby="vista-previa-titulo">
      <h3 id="vista-previa-titulo">Así llegará su mensaje</h3>
      {previa.cargando && <p role="status" className="vista-previa__estado">Preparando la vista previa…</p>}
      {error && (
        <Aviso variante="error" titulo="No pudimos preparar la vista previa">
          {error.mensaje}
          {error.reintentar && <>{' '}<button type="button" className="enlace-boton" onClick={previa.recargar}>Reintentar</button></>}
        </Aviso>
      )}
      {previa.datos && <>
        <dl className="vista-previa__mensaje">
          <dt>Asunto</dt>
          <dd>{previa.datos.asunto}</dd>
          <dt>Mensaje</dt>
          <dd id="vista-previa-texto" className={completo ? 'vista-previa__texto' : 'vista-previa__texto vista-previa__texto--recortado'}>
            {previa.datos.texto}
          </dd>
        </dl>
        <button type="button" className="enlace-boton" aria-expanded={completo} aria-controls="vista-previa-texto"
          onClick={() => setCompleto((v) => !v)}>
          {completo ? 'Ver menos' : 'Ver completo'}
        </button>
        <Aviso variante="nota">{entidad} recibe este mensaje a su nombre y le responde a su correo.</Aviso>
      </>}
    </section>
  )
}
