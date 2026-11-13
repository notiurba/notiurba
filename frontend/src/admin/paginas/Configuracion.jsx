import { useState } from 'react'
import Aviso from '../../compartido/componentes/avisos/Aviso'
import Boton from '../../compartido/componentes/Boton'
import EstadoVacio from '../../compartido/componentes/avisos/EstadoVacio'
import useCargar from '../../compartido/hooks/useCargar'
import { consultarLimites, guardarLimites } from '../servicios/limitesService'
import { aFormulario, aSolicitud, validarLimites } from './limites'
import './Configuracion.css'

function CampoNumero({ id, etiqueta, ayuda, unidad, valor, error, alCambiar, deshabilitado }) {
  const describe = [ayuda && `${id}-ayuda`, error && `${id}-error`].filter(Boolean).join(' ') || undefined
  return (
    <div className="adm-conf__campo">
      {etiqueta && <label htmlFor={id}>{etiqueta}</label>}
      <div className="adm-conf__entrada">
        <input id={id} inputMode="numeric" value={valor} disabled={deshabilitado} aria-invalid={Boolean(error)}
          aria-describedby={describe} onChange={(e) => alCambiar(e.target.value)} />
        {unidad && <span>{unidad}</span>}
      </div>
      {ayuda && <p id={`${id}-ayuda`} className="adm-conf__ayuda">{ayuda}</p>}
      {error && <p id={`${id}-error`} className="adm-conf__error" role="alert">{error}</p>}
    </div>
  )
}

// Configuración del panel: límites de ingreso, de reportes por día y de reportes parecidos.
// Se guardan todos a la vez y aplican desde la siguiente petición.
export default function Configuracion() {
  const { datos, cargando, error, recargar } = useCargar(consultarLimites)
  const [formulario, setFormulario] = useState(null)
  const [errores, setErrores] = useState({})
  const [guardando, setGuardando] = useState(false)
  const [resultado, setResultado] = useState(null) // { variante, texto }

  if (cargando && !datos) return <p className="cargando-texto">Cargando la configuración…</p>
  if (error) {
    return (
      <EstadoVacio titulo="No pudimos cargar la configuración">
        <Boton onClick={recargar}>Reintentar</Boton>
      </EstadoVacio>
    )
  }

  const f = formulario ?? aFormulario(datos)
  const cambiar = (campo) => (valor) => {
    setFormulario({ ...f, [campo]: valor })
    setErrores((e) => ({ ...e, [campo]: '' }))
    setResultado(null)
  }
  const cambiarMetros = (idCategoria) => (valor) => {
    setFormulario({ ...f, categorias: f.categorias.map((c) => (c.idCategoria === idCategoria ? { ...c, metros: valor } : c)) })
    setErrores((e) => ({ ...e, [`categorias.${idCategoria}.metros`]: '' }))
    setResultado(null)
  }

  async function guardar(evento) {
    evento.preventDefault()
    const encontrados = validarLimites(f)
    setErrores(encontrados)
    if (Object.keys(encontrados).length) {
      setResultado({ variante: 'error', texto: 'Revise los valores marcados. No se guardó ningún cambio.' })
      return
    }
    setGuardando(true)
    setResultado(null)
    try {
      const guardados = await guardarLimites(aSolicitud(f))
      setFormulario(aFormulario(guardados))
      setResultado({ variante: 'exito', texto: 'Cambios guardados. Aplican desde ya para los nuevos ingresos y reportes.' })
    } catch (fallo) {
      // Un 400 del servicio dice qué campo no aceptó; lo demás es un problema de conexión o del servicio.
      if (fallo.estado === 400 && fallo.campo) setErrores({ [fallo.campo]: fallo.mensaje })
      setResultado({ variante: 'error', texto: fallo.estado === 400
        ? 'Revise los valores marcados. No se guardó ningún cambio.'
        : 'No pudimos guardar los cambios. Revise su conexión e inténtelo de nuevo.' })
    } finally {
      setGuardando(false)
    }
  }

  return (
    <form className="adm-conf" onSubmit={guardar} noValidate>
      <div className="adm-conf__titulo">
        <h1>Configuración</h1>
        <p>Límites para evitar abusos. Los cambios aplican de inmediato, sin reiniciar nada.</p>
      </div>

      <section className="adm-conf__bloque" aria-labelledby="conf-ingreso">
        <h2 id="conf-ingreso">Ingreso</h2>
        <p className="adm-conf__ayuda">Si alguien falla la contraseña varias veces, ese correo se bloquea un rato.</p>
        <div className="adm-conf__fila">
          <CampoNumero id="intentosMaximos" etiqueta="Intentos fallidos permitidos" ayuda="De 1 a 10."
            valor={f.intentosMaximos} error={errores.intentosMaximos} alCambiar={cambiar('intentosMaximos')} deshabilitado={guardando} />
          <CampoNumero id="ventanaMinutos" etiqueta="Dentro de" unidad="minutos" ayuda="De 1 a 60."
            valor={f.ventanaMinutos} error={errores.ventanaMinutos} alCambiar={cambiar('ventanaMinutos')} deshabilitado={guardando} />
          <CampoNumero id="bloqueoMinutos" etiqueta="Tiempo de bloqueo" unidad="minutos" ayuda="De 1 a 60."
            valor={f.bloqueoMinutos} error={errores.bloqueoMinutos} alCambiar={cambiar('bloqueoMinutos')} deshabilitado={guardando} />
        </div>
      </section>

      <section className="adm-conf__bloque" aria-labelledby="conf-reportes">
        <h2 id="conf-reportes">Reportes</h2>
        <div className="adm-conf__fila">
          <CampoNumero id="reportesMaximosPorDia" etiqueta="Reportes por cuenta al día" ayuda="De 1 a 50. Cuenta el día en hora de Colombia."
            valor={f.reportesMaximosPorDia} error={errores.reportesMaximosPorDia} alCambiar={cambiar('reportesMaximosPorDia')} deshabilitado={guardando} />
        </div>
      </section>

      <section className="adm-conf__bloque" aria-labelledby="conf-parecidos">
        <h2 id="conf-parecidos">Reportes parecidos</h2>
        <p className="adm-conf__ayuda">
          Antes de guardar un reporte avisamos si hay otro de la misma categoría, cerca y reciente, aún sin resolver.
        </p>
        <div className="adm-conf__fila">
          <CampoNumero id="ventanaDias" etiqueta="Se consideran recientes los de los últimos" unidad="días" ayuda="De 1 a 30."
            valor={f.ventanaDias} error={errores.ventanaDias} alCambiar={cambiar('ventanaDias')} deshabilitado={guardando} />
        </div>
        <div className="adm-conf__tabla-caja">
          <table className="adm-conf__tabla">
            <caption>Distancia a la que dos reportes se consideran parecidos (de 5 a 500 metros)</caption>
            <thead>
              <tr><th scope="col">Categoría</th><th scope="col">Distancia</th></tr>
            </thead>
            <tbody>
              {f.categorias.map((c) => {
                const clave = `categorias.${c.idCategoria}.metros`
                return (
                  <tr key={c.idCategoria}>
                    <th scope="row"><label htmlFor={`metros-${c.idCategoria}`}>{c.nombre}</label></th>
                    <td>
                      <CampoNumero id={`metros-${c.idCategoria}`} etiqueta="" unidad="metros" valor={c.metros} error={errores[clave]}
                        alCambiar={cambiarMetros(c.idCategoria)} deshabilitado={guardando} />
                    </td>
                  </tr>
                )
              })}
            </tbody>
          </table>
        </div>
      </section>

      {resultado && <Aviso variante={resultado.variante}>{resultado.texto}</Aviso>}
      <div className="adm-conf__acciones">
        <Boton type="submit" cargando={guardando} textoCargando="Guardando…">Guardar cambios</Boton>
      </div>
    </form>
  )
}
