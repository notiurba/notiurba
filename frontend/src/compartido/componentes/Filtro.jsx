import './Filtro.css'

// Píldora de filtro (Activos / Resueltos / Todos). `activo` marca la opción elegida.
export default function Filtro({ activo = false, children, ...resto }) {
  return (
    <button
      type="button"
      className={activo ? 'filtro filtro--activo' : 'filtro'}
      aria-pressed={activo}
      {...resto}
    >
      {children}
    </button>
  )
}
