import { useState } from 'react'
import Boton from '../../compartido/componentes/Boton'
import { IconoCopiar } from '../../compartido/componentes/Iconos'
import * as api from '../servicios/correoReportes'
import ListaCanales from './ListaCanales'
import { textoParaCopiar } from './estadoCorreo'

// Cuando el correo rebotó o falló: los canales de la entidad y el mismo mensaje para pegarlo allí.
export default function OtrosCanales({ codigo, ruta }) {
  const [estado, setEstado] = useState({ texto: '', aviso: '', cargando: false })

  async function copiar() {
    setEstado({ texto: '', aviso: '', cargando: true })
    let texto = ''
    try {
      texto = textoParaCopiar(await api.mensaje(codigo))
      await navigator.clipboard.writeText(texto)
      setEstado({ texto: '', aviso: 'Mensaje copiado. Péguelo en el formulario o en el chat de la entidad.', cargando: false })
    } catch {
      // Sin permiso para copiar se muestra el texto para seleccionarlo a mano.
      setEstado({ texto, aviso: texto ? 'Seleccione el mensaje y cópielo.' : 'No pudimos traer su mensaje. Inténtelo de nuevo.', cargando: false })
    }
  }

  return (
    <section className="otros-canales" aria-labelledby="otros-canales-titulo">
      <h2 id="otros-canales-titulo">Hágalo llegar por otro canal</h2>
      <ListaCanales canales={ruta?.canales} />
      <Boton variante="secundario" onClick={copiar} cargando={estado.cargando} textoCargando="Copiando…">
        <IconoCopiar tamano={20} />
        Copiar mi mensaje
      </Boton>
      {estado.aviso && <p role="status">{estado.aviso}</p>}
      {estado.texto && <textarea readOnly rows={8} value={estado.texto} aria-label="Su mensaje" onFocus={(e) => e.target.select()} />}
    </section>
  )
}
