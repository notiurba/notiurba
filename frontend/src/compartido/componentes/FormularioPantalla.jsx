import './FormularioPantalla.css'

// Estructura de las pantallas con formulario: los campos crecen y la barra de acciones
// (botón principal y «Cancelar»/«Atrás») queda pegada abajo, como en el diseño.
//  - alEnviar: se llama con el evento del formulario.  - acciones: los botones de abajo.
export default function FormularioPantalla({ alEnviar, acciones, children }) {
  return (
    <form className="formulario-pantalla" onSubmit={alEnviar} noValidate>
      <div className="formulario-pantalla__campos">{children}</div>
      <div className="formulario-pantalla__acciones">{acciones}</div>
    </form>
  )
}
