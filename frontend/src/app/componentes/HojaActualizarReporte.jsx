import { useState } from 'react'
import useUnaVez from '../../compartido/hooks/useUnaVez'
import { actualizarMiReporte } from '../../compartido/api/reportes'
import Aviso from '../../compartido/componentes/avisos/Aviso'
import Boton from '../../compartido/componentes/Boton'
import Campo from '../../compartido/componentes/formulario/Campo'
import HojaInferior from '../../compartido/componentes/avisos/HojaInferior'
import TarjetaOpcion from '../../compartido/componentes/TarjetaOpcion'
import { LIMITES } from '../../compartido/validaciones/campos'

const OPCIONES = [
  { valor: 'RESPUESTA', titulo: 'Me respondieron' },
  { valor: 'RESUELTO', titulo: 'Ya se resolvió' },
]

// «Actualizar mi reporte»: la persona avisa qué pasó. Su aviso no es el estado oficial:
// el administrador de NotiUrba lo confirma. `reporte` es null cuando la hoja está cerrada.
export default function HojaActualizarReporte({ reporte, alCerrar, alGuardar }) {
  const [tipo, setTipo] = useState('RESPUESTA')
  const [radicado, setRadicado] = useState('')
  const [guardando, setGuardando] = useState(false)
  const guardarUnaVez = useUnaVez(guardar)
  const [fallo, setFallo] = useState(false)

  async function guardar() {
    setGuardando(true)
    setFallo(false)
    try {
      await actualizarMiReporte(reporte.codigoSeguimiento, tipo, radicado.trim())
      setRadicado('')
      setTipo('RESPUESTA')
      alGuardar()
    } catch {
      setFallo(true)
    }
    setGuardando(false)
  }

  return (
    <HojaInferior
      abierta={Boolean(reporte)}
      alCerrar={alCerrar}
      titulo="Actualizar mi reporte"
      texto={reporte && `${reporte.categoria} · ${reporte.codigoSeguimiento}`}
    >
      <fieldset className="grupo-opciones">
        <legend>¿Qué pasó con su reporte?</legend>
        {OPCIONES.map((opcion) => (
          <TarjetaOpcion
            key={opcion.valor}
            grupo="tipo-actualizacion"
            valor={opcion.valor}
            seleccionada={tipo === opcion.valor}
            alElegir={setTipo}
            titulo={opcion.titulo}
          />
        ))}
      </fieldset>

      <Campo
        id="radicado"
        maxLength={LIMITES.radicado}
        etiqueta="Número de radicado"
        opcional
        placeholder="Ej. GAC-003660-26"
        ayuda="Lo encuentra en la respuesta de la entidad."
        value={radicado}
        onChange={(e) => setRadicado(e.target.value)}
      />

      <Aviso variante="info">
        Su actualización queda como aviso. El estado oficial lo confirma el administrador de NotiUrba.
      </Aviso>
      {fallo && <Aviso variante="error">No pudimos guardar la actualización. Inténtelo de nuevo.</Aviso>}

      <Boton onClick={guardarUnaVez} cargando={guardando} textoCargando="Guardando…">
        Guardar actualización
      </Boton>
      <Boton variante="texto" onClick={alCerrar} disabled={guardando}>
        Cancelar
      </Boton>
    </HojaInferior>
  )
}
