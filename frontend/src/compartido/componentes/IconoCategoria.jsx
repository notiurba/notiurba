import { visualCategoria } from '../utilidades/categorias'
import './IconoCategoria.css'

// Cuadrito de color con el ícono de una categoría. Lo usan la lista de categorías y el
// resumen «Tipo de problema» de los pasos del reporte.
export default function IconoCategoria({ idCategoria, tamano = 40 }) {
  const { Icono, tono } = visualCategoria(idCategoria)
  return (
    <span
      className="icono-categoria"
      style={{ width: tamano, height: tamano, background: `var(--nu-categoria-${tono})` }}
      aria-hidden="true"
    >
      <Icono tamano={Math.round(tamano * 0.5)} />
    </span>
  )
}
