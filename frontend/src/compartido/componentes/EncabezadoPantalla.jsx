import BotonIcono from './BotonIcono'
import { IconoFlecha, IconoX } from './Iconos'
import './EncabezadoPantalla.css'

// Barra de arriba de las pantallas de cuenta: flecha redonda para volver y, si hay, un título.
// `sinVolver` quita la flecha (pantallas de las pestañas); `cerrar` la cambia por una X. Para volver pasa `atras` (una ruta) o `alAtras` (una función, por ejemplo para volver al paso 1).
export default function EncabezadoPantalla({ titulo, atras, alAtras, sinVolver = false, cerrar = false }) {
  const volver = alAtras ? { onClick: alAtras } : { to: atras ?? '/' }

  return (
    <header className={titulo ? 'encabezado encabezado--con-titulo' : 'encabezado'}>
      {!sinVolver && (
        <span className="encabezado__volver">
          <BotonIcono etiqueta={cerrar ? 'Cerrar' : 'Volver'} {...volver}>
            {cerrar ? <IconoX tamano={20} /> : <IconoFlecha tamano={20} />}
          </BotonIcono>
        </span>
      )}
      {titulo && <h1 className="encabezado__titulo">{titulo}</h1>}
    </header>
  )
}
