import { useEffect, useRef } from 'react'
import './HojaInferior.css'

// Ventana modal que sube desde abajo (en computador, queda centrada). Usa el <dialog> del
// navegador, que ya se encarga de lo difícil: atrapa el foco, se cierra con Escape y oscurece el fondo.
//  - abierta: true/false.  - alCerrar: se llama con Escape o al tocar fuera.
//  - icono + tono: círculo de color arriba (tono = clave de aviso: info, alerta, peligro, exito).
//  - titulo, texto: encabezado; los botones y campos van en `children`.
export default function HojaInferior({ abierta, alCerrar, icono, tono = 'exito', titulo, texto, children }) {
  const ref = useRef(null)

  useEffect(() => {
    const dialogo = ref.current
    if (!dialogo) return
    if (abierta && !dialogo.open) dialogo.showModal()
    if (!abierta && dialogo.open) dialogo.close()
  }, [abierta])

  function alPulsarFondo(evento) {
    // Un clic directo sobre el <dialog> (y no sobre su contenido) es un clic en el fondo oscuro.
    if (evento.target === ref.current) alCerrar?.()
  }

  return (
    <dialog
      ref={ref}
      className="hoja"
      aria-labelledby="hoja-titulo"
      onClose={() => abierta && alCerrar?.()}
      onClick={alPulsarFondo}
    >
      {abierta && (
        <div className="hoja__cuerpo">
          <span className="hoja__asa" aria-hidden="true" />
          {icono && <span className={`hoja__icono hoja__icono--${tono}`}>{icono}</span>}
          <h2 id="hoja-titulo" className="hoja__titulo">
            {titulo}
          </h2>
          {texto && <p className="hoja__texto">{texto}</p>}
          {children}
        </div>
      )}
    </dialog>
  )
}
