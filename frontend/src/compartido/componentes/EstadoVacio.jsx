import './EstadoVacio.css'

// Pantalla (o zona) sin contenido: círculo con ícono, título, texto y una acción opcional.
// Sirve para «Aún no tiene reportes», «Sin conexión a internet», «No se encontró el reporte», etc.
//  - tono: 'exito' (verde) o 'alerta' (amarillo).
export default function EstadoVacio({ icono, tono = 'exito', titulo, texto, children }) {
  return (
    <div className="estado-vacio">
      <span className={`estado-vacio__icono estado-vacio__icono--${tono}`}>{icono}</span>
      <h2>{titulo}</h2>
      {texto && <p>{texto}</p>}
      {children}
    </div>
  )
}
