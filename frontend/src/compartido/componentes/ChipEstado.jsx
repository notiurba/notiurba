import { IconoBuscar, IconoCheck, IconoEnviar, IconoReloj } from './Iconos'
import './ChipEstado.css'

// Cada estado conocido: qué color y qué ícono lleva. «Pendiente de envío» NO es un estado del
// reporte: viene del envío del correo y comparte el color de «Recibido».
const ESTADOS = {
  Recibido: { clave: 'recibido', Icono: IconoReloj },
  'Pendiente de envío': { clave: 'recibido', Icono: IconoReloj },
  Enviado: { clave: 'enviado', Icono: IconoEnviar },
  'En trámite': { clave: 'tramite', Icono: IconoBuscar },
  Resuelto: { clave: 'resuelto', Icono: IconoCheck },
}

// Píldora con el estado de un reporte. Siempre lleva ícono y texto: el estado nunca se indica solo con color.
export default function ChipEstado({ estado }) {
  const { clave, Icono } = ESTADOS[estado] ?? ESTADOS.Recibido
  return (
    <span className={`chip-estado chip-estado--${clave}`}>
      <Icono tamano={14} />
      {estado}
    </span>
  )
}
