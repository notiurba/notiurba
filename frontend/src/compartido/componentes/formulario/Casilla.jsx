import './Casilla.css'

// Casilla de aceptación con su texto (por ejemplo, el tratamiento de datos de la Ley 1581).
// Si hay `error`, se muestra debajo en rojo, igual que en los campos.
export default function Casilla({ id, error, children, ...entrada }) {
  return (
    <div className="casilla">
      <label className="casilla__fila" htmlFor={id}>
        <input
          id={id}
          type="checkbox"
          aria-invalid={error ? 'true' : undefined}
          aria-describedby={error ? `${id}-error` : undefined}
          {...entrada}
        />
        <span>{children}</span>
      </label>
      {error && (
        <p id={`${id}-error`} className="campo__error" role="alert">
          {error}
        </p>
      )}
    </div>
  )
}
