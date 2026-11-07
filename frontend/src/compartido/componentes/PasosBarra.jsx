import './PasosBarra.css'

// "Paso 1 de 2" con dos barritas (la completada en verde) y el nombre del paso a la derecha.
export default function PasosBarra({ paso, total, etiqueta }) {
  return (
    <div className="pasos">
      <div className="pasos__textos">
        <span className="pasos__numero">
          Paso {paso} de {total}
        </span>
        <span className="pasos__etiqueta">{etiqueta}</span>
      </div>
      <div className="pasos__barras" aria-hidden="true">
        {Array.from({ length: total }, (_, i) => (
          <span key={i} className={i < paso ? 'pasos__barra pasos__barra--hecha' : 'pasos__barra'} />
        ))}
      </div>
    </div>
  )
}
