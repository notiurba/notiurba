import './PasosBarra.css'

// "Paso 1 de 2" con dos barritas (la completada en verde) y el nombre del paso a la derecha.
export default function PasosBarra({ paso, total, etiqueta, refEncabezado }) {
  return (
    <div className="pasos">
      <h2 className="pasos__textos" tabIndex={-1} ref={refEncabezado}>
        <span className="pasos__numero">
          Paso {paso} de {total}
        </span>
        <span className="pasos__etiqueta">{etiqueta}</span>
      </h2>
      <div className="pasos__barras" aria-hidden="true">
        {Array.from({ length: total }, (_, i) => (
          <span key={i} className={i < paso ? 'pasos__barra pasos__barra--hecha' : 'pasos__barra'} />
        ))}
      </div>
    </div>
  )
}
