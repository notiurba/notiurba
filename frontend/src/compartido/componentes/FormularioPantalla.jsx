import './FormularioPantalla.css'

// Estructura de las pantallas con formulario: los campos crecen y la barra de acciones
// (botón principal y «Cancelar»/«Atrás») queda pegada abajo, como en el diseño.
//  - alEnviar: se llama con el evento del formulario.  - acciones: los botones de abajo.
export default function FormularioPantalla({ alEnviar, acciones, children }) {
  // Después de enviar, si quedó algún campo con error, la pantalla va al primero y lo enfoca
  // (se espera un instante para que React ya haya pintado los errores).
  function enviar(evento) {
    const formulario = evento.currentTarget
    alEnviar(evento)
    setTimeout(() => {
      formulario.querySelector('[aria-invalid="true"]')?.focus()
    }, 0)
  }

  return (
    <form className="formulario-pantalla" onSubmit={enviar} noValidate>
      <div className="formulario-pantalla__campos">{children}</div>
      <div className="formulario-pantalla__acciones">{acciones}</div>
    </form>
  )
}
