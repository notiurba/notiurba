import logo from '../assets/logo-notiurba.png'
import { ETIQUETAS_MARCA } from '../textos/marca'
import './PanelMarca.css'

// Portada verde de la izquierda en computador (login y registro), como la portada del Figma.
// En el celular no se muestra.
export default function PanelMarca() {
  return (
    <aside className="panel-marca">
      <span className="panel-marca__circulo panel-marca__circulo--grande" aria-hidden="true" />
      <span className="panel-marca__circulo panel-marca__circulo--mediano" aria-hidden="true" />

      <div className="panel-marca__logo">
        <span className="panel-marca__linea" aria-hidden="true" />
        <img src={logo} alt="" />
      </div>

      <div className="panel-marca__texto">
        <p className="panel-marca__sobretitulo">Plataforma ciudadana</p>
        <h2>NotiUrba</h2>
        <p className="panel-marca__subtitulo">Reporte ciudadano georreferenciado · Tuluá</p>
        <ul>
          {ETIQUETAS_MARCA.map((etiqueta) => (
            <li key={etiqueta}>{etiqueta}</li>
          ))}
        </ul>
      </div>
    </aside>
  )
}
