import './Campo.css'

// Lo común de cualquier campo del formulario: etiqueta arriba (con "opcional" y un espacio
// para un enlace a la derecha), la caja con borde, y debajo el error o la ayuda.
// Campo, CampoPassword y Selector solo ponen su "control" dentro de `children`.
export default function CampoBase({ id, etiqueta, opcional, acciones, error, ayuda, alta = false, children }) {
  return (
    <div className="campo">
      <div className="campo__cabecera">
        <label htmlFor={id}>
          {etiqueta}
          {opcional && <span className="campo__opcional"> (opcional)</span>}
        </label>
        {acciones}
      </div>
      <div className={['campo__caja', error && 'campo__caja--error', alta && 'campo__caja--alta'].filter(Boolean).join(' ')}>
        {children}
      </div>
      {error ? (
        <p id={`${id}-error`} className="campo__error" role="alert">
          {error}
        </p>
      ) : (
        ayuda && (
          <p id={`${id}-ayuda`} className="campo__ayuda">
            {ayuda}
          </p>
        )
      )}
    </div>
  )
}
