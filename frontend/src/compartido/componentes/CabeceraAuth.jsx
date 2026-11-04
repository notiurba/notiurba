import logo from '../assets/logo-notiurba.png'
import './CabeceraAuth.css'

// Logo, título y texto de apoyo que abren las pantallas de cuenta (login, recuperar, nueva contraseña).
export default function CabeceraAuth({ titulo, texto }) {
  return (
    <>
      <img className="cabecera-auth__logo" src={logo} alt="Logo de NotiUrba" />
      <div className="cabecera-auth__titulo">
        <h1>{titulo}</h1>
        {texto && <p>{texto}</p>}
      </div>
    </>
  )
}
