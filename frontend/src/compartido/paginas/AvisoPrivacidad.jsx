import { useNavigate } from 'react-router-dom'
import EncabezadoPantalla from '../componentes/EncabezadoPantalla'
import LayoutAuth from '../componentes/acceso/LayoutAuth'
import '../estilos/marca.css'
import { AVISO_PRIVACIDAD } from '../textos/avisoPrivacidad'
import './AvisoPrivacidad.css'

// Aviso de privacidad (Ley 1581). Es público: se abre desde el registro, los ajustes y el envío del reporte.
export default function AvisoPrivacidad() {
  const navigate = useNavigate()

  return (
    <LayoutAuth>
      <EncabezadoPantalla titulo="Aviso de privacidad" alAtras={() => navigate(-1)} />

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
              {seccion.parrafos?.map((parrafo) => (
                <p key={parrafo}>{parrafo}</p>
              ))}
              {seccion.items && (
                <ul>
                  {seccion.items.map((item) => (
                    <li key={item}>{item}</li>
                  ))}
                </ul>
              )}
            </section>
          ))}
        </div>
      </article>
    </LayoutAuth>
  )
}
