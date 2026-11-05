import logo from '../../assets/logo-notiurba.png'
import { DESCRIPCION_MARCA, ETIQUETAS_MARCA, TITULO_MARCA } from '../../textos/marca'
import FondoCiudad from './FondoCiudad'
import './PanelMarca.css'

// Panel azul de la izquierda en computador (login y registro). En el celular no se muestra.
export default function PanelMarca() {
  return (
    <aside className="panel-marca">
      <FondoCiudad className="panel-marca__ciudad" />
      <svg className="panel-marca__pin" width="38" height="50" viewBox="0 0 24 32" aria-hidden="true">
        <path d="M12 0a10 10 0 0 0-10 10c0 7 10 18 10 18s10-11 10-18A10 10 0 0 0 12 0z" fill="currentColor" />
        <circle cx="12" cy="10" r="4" fill="#fff" />
      </svg>
      <p className="panel-marca__nombre">
        <img src={logo} alt="" />
        NotiUrba
      </p>
      <div className="panel-marca__texto">
        <h2>{TITULO_MARCA}</h2>
        <p>{DESCRIPCION_MARCA}</p>
        <ul>
          {ETIQUETAS_MARCA.map((etiqueta) => (
            <li key={etiqueta}>{etiqueta}</li>
          ))}
        </ul>
      </div>
    </aside>
  )
}
