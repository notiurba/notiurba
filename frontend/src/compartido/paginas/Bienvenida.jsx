import { Link } from 'react-router-dom'
import logo from '../assets/logo-notiurba.png'
import Boton from '../componentes/Boton'
import FondoCiudad from '../componentes/FondoCiudad'
import '../estilos/marca.css'
import { DESCRIPCION_MARCA, ETIQUETAS_MARCA, TITULO_MARCA } from '../textos/marca'
import './Bienvenida.css'

// Pantalla de inicio: arriba la ciudad con el logo, abajo el mensaje y el botón para comenzar.
export default function Bienvenida() {
  return (
    <main className="bienvenida">
      <section className="bienvenida__cielo">
        <FondoCiudad className="bienvenida__ciudad" />
        <p className="bienvenida__lugar">TULUÁ · VALLE DEL CAUCA</p>
        <span className="bienvenida__marcador bienvenida__marcador--rosa" aria-hidden="true" />
        <span className="bienvenida__marcador bienvenida__marcador--amarillo" aria-hidden="true" />
        <div className="bienvenida__tarjeta-logo">
          <img src={logo} alt="Logo de NotiUrba" />
        </div>
      </section>

      <section className="bienvenida__contenido">
        <p className="bienvenida__marca">NotiUrba</p>
        <h1>{TITULO_MARCA}</h1>
        <p className="bienvenida__descripcion">{DESCRIPCION_MARCA}</p>
        <ul className="bienvenida__etiquetas">
          {ETIQUETAS_MARCA.map((etiqueta) => (
            <li key={etiqueta}>{etiqueta}</li>
          ))}
        </ul>
        <Boton to="/login">Comenzar</Boton>
        <p className="bienvenida__sesion">
          ¿Ya tiene cuenta? <Link to="/login">Inicie sesión</Link>
        </p>
      </section>
    </main>
  )
}
