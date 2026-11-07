import '../../compartido/estilos/marca.css'
import './MarcoApp.css'

// Marco común de las pantallas de la PWA ciudadana, que está diseñada para celular: ocupa todo el ancho
// en celulares (320 a 430 px) y en pantallas grandes queda como una columna de celular centrada.
// El panel de administración es el que está diseñado para web.
//  - pie: barra de pestañas fija abajo.
export default function MarcoApp({ pie, children }) {
  return (
    <div className="marco-app">
      <main className="marco-app__contenido">{children}</main>
      {pie}
    </div>
  )
}
