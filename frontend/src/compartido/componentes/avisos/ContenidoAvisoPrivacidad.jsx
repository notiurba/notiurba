import { AVISO_PRIVACIDAD } from '../../textos/avisoPrivacidad'
import '../../paginas/AvisoPrivacidad.css'

// Un único contenido v3 para la ruta pública y la consulta dentro del registro.
export default function ContenidoAvisoPrivacidad() {
  return (
    <article className="aviso-privacidad">
      <p className="aviso-privacidad__etiquetas">
        <span className="aviso-privacidad__ley">{AVISO_PRIVACIDAD.cabecera[0]}</span>
        <span className="aviso-privacidad__version">{AVISO_PRIVACIDAD.cabecera[1]}</span>
      </p>
      <p className="aviso-privacidad__intro">{AVISO_PRIVACIDAD.intro}</p>
      <div className="aviso-privacidad__tarjeta">
        {AVISO_PRIVACIDAD.secciones.map((seccion) => (
          <section key={seccion.titulo}>
            <h2>{seccion.titulo}</h2>
            {seccion.parrafos?.map((parrafo) => <p key={parrafo}>{parrafo}</p>)}
            {seccion.items && <ul>{seccion.items.map((item) => <li key={item}>{item}</li>)}</ul>}
          </section>
        ))}
      </div>
    </article>
  )
}
