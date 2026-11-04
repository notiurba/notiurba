import { IconoAlerta, IconoCheck, IconoError, IconoEscudo, IconoInfo } from './Iconos'
import './Aviso.css'

const ICONOS = {
  error: IconoError,
  alerta: IconoAlerta,
  info: IconoInfo,
  exito: IconoCheck,
  nota: IconoEscudo,
}

// Caja de mensaje con ícono, para errores generales, información y confirmaciones.
//  - variante: 'error' (rojo), 'alerta' (ámbar), 'info' (azul), 'exito' (verde)
//    o 'nota' (escudo gris, sin caja).
//  - titulo: línea en negrita opcional; el resto va en `children`.
// Los errores se anuncian al lector de pantalla enseguida (role="alert"); el resto, con calma.
export default function Aviso({ variante = 'info', titulo, children }) {
  const Icono = ICONOS[variante]

  return (
    <div className={`aviso aviso--${variante}`} role={variante === 'error' ? 'alert' : 'status'}>
      <span className="aviso__icono">
        <Icono tamano={variante === 'nota' ? 18 : 22} />
      </span>
      <div>
        {titulo && <p className="aviso__titulo">{titulo}</p>}
        <p className="aviso__texto">{children}</p>
      </div>
    </div>
  )
}
