import { useEffect, useRef, useState } from 'react'
import Boton from '../../compartido/componentes/Boton'
import { IconoCopiar } from '../../compartido/componentes/Iconos'
import * as api from '../servicios/correoReportes'
import ListaCanales from './ListaCanales'
import { copiarMensaje } from './estadoCorreo'

// Cuando el correo rebotó o falló: los canales de la entidad y el mismo mensaje para pegarlo allí.
export default function OtrosCanales({ codigo, ruta }) {
  const [estado, setEstado] = useState({ texto: '', aviso: '', cargando: false })
  const cuadro = useRef(null)

  // Si no se pudo copiar, el foco va al mensaje (que se selecciona solo) para copiarlo a mano.
  useEffect(() => { if (estado.texto) cuadro.current?.focus() }, [estado.texto])

  async function copiar() {
    setEstado({ texto: '', aviso: '', cargando: true })
    const { copiado, texto } = await copiarMensaje({ obtener: () => api.mensaje(codigo),
      portapapeles: navigator.clipboard, Item: globalThis.ClipboardItem })
    setEstado(copiado
      ? { texto: '', aviso: 'Mensaje copiado. Péguelo en el formulario o en el chat de la entidad.', cargando: false }
      : { texto, aviso: texto ? 'No pudimos copiarlo. Seleccione el mensaje y cópielo.' : 'No pudimos traer su mensaje. Inténtelo de nuevo.', cargando: false })
  }

  return (
    <section className="otros-canales" aria-labelledby="otros-canales-titulo">
      <h2 id="otros-canales-titulo">Hágalo llegar a {ruta?.entidad ?? 'la entidad'} por otro canal</h2>
      <ListaCanales canales={ruta?.canales} />
      <Boton variante="secundario" onClick={copiar} cargando={estado.cargando} textoCargando="Copiando…">
        <IconoCopiar tamano={20} />
        Copiar mi mensaje
      </Boton>
      <p role="status">{estado.aviso}</p>
      {estado.texto && <textarea ref={cuadro} readOnly rows={8} value={estado.texto} aria-label="Su mensaje" onFocus={(e) => e.target.select()} />}
    </section>
  )
}
