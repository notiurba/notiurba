import { useEffect, useRef, useState } from 'react'
import { Navigate, useNavigate } from 'react-router-dom'
import AreaTexto from '../../compartido/componentes/formulario/AreaTexto'
import Aviso from '../../compartido/componentes/avisos/Aviso'
import Boton from '../../compartido/componentes/Boton'
import Campo from '../../compartido/componentes/formulario/Campo'
import Casilla from '../../compartido/componentes/formulario/Casilla'
import EncabezadoPantalla from '../../compartido/componentes/EncabezadoPantalla'
import FormularioPantalla from '../../compartido/componentes/formulario/FormularioPantalla'
import HojaInferior from '../../compartido/componentes/avisos/HojaInferior'
import { IconoCamara, IconoImagen, IconoImagenError, IconoPapelera } from '../../compartido/componentes/Iconos'
import PasosBarra from '../../compartido/componentes/PasosBarra'
import BotonIcono from '../../compartido/componentes/BotonIcono'
import { reducirFoto } from '../../compartido/utilidades/fotos'
import { LIMITES } from '../../compartido/validaciones/campos'
import { MAX_DESCRIPCION, MAX_FOTOS, MIN_DESCRIPCION, validarDetalles, validarFoto, validarUbicacion } from '../../compartido/validaciones/reporte'
import ResumenCategoria from './ResumenCategoria'
import { useReporte } from './useReporte'
import './Reportar.css'
import './PasoDetalles.css'

function MiniaturaFoto({ foto, numero }) {
  const imagen = useRef(null)
  useEffect(() => {
    const url = URL.createObjectURL(foto)
    imagen.current.src = url
    return () => URL.revokeObjectURL(url)
  }, [foto])
  return <img ref={imagen} alt={`Vista previa de la foto ${numero}`} />
}

// Paso 3 de 4: descripción (obligatoria), fotos (opcionales) y matrícula del servicio (opcional).
export default function PasoDetalles() {
  const navigate = useNavigate()
  const { borrador, cambiar, fotos, setFotos, categorias } = useReporte()
  const [errores, setErrores] = useState({})
  const [errorFoto, setErrorFoto] = useState('')
  const [reduciendo, setReduciendo] = useState(false)
  const entradaCamara = useRef(null)
  const entradaGaleria = useRef(null)

  const categoria = categorias?.find((c) => String(c.idCategoria) === borrador.idCategoria)
  if (!borrador.idCategoria) return <Navigate to="/reportar" replace />
  if (!categorias) return <p className="cargando-texto">Cargando…</p>
  if (!categoria) return <Navigate to="/reportar" replace />
  if (Object.keys(validarUbicacion(borrador, categoria)).length) return <Navigate to="/reportar/ubicacion" replace />

  async function agregarFotos(evento) {
    const elegidas = [...evento.target.files]
    evento.target.value = '' // permite volver a elegir el mismo archivo
    let cantidad = fotos.length
    const aceptadas = []
    setReduciendo(true)
    for (const archivo of elegidas) {
      const motivo = validarFoto(archivo, cantidad)
      if (motivo) {
        setErrorFoto(motivo)
        break
      }
      try {
        aceptadas.push(await reducirFoto(archivo)) // queda en 1 MB o menos
        cantidad += 1
      } catch (error) {
        setErrorFoto(error.message)
        break
      }
    }
    setReduciendo(false)
    if (aceptadas.length > 0) setFotos((anteriores) => [...anteriores, ...aceptadas])
  }

  function continuar(evento) {
    evento.preventDefault()
    if (reduciendo) return
    const encontrados = validarDetalles(borrador)
    setErrores(encontrados)
    if (Object.keys(encontrados).length === 0) navigate('/reportar/confirmacion')
  }

  return (
    <>
      <EncabezadoPantalla titulo="Nuevo reporte" atras="/reportar/ubicacion" />

      <FormularioPantalla
        alEnviar={continuar}
        acciones={<Boton type="submit" disabled={reduciendo}>Continuar</Boton>}
      >
        <PasosBarra paso={3} total={4} etiqueta="Descripción y fotografías" />
        <ResumenCategoria categoria={categoria} />
        <h2 className="reportar__titulo">Cuéntenos más</h2>

        <div>
          <AreaTexto
            id="descripcion"
            etiqueta="Descripción"
            placeholder="Describa qué ocurre, desde cuándo y dónde."
            maxLength={MAX_DESCRIPCION}
            value={borrador.descripcion}
            onChange={(e) => {
              cambiar({ descripcion: e.target.value })
              setErrores({})
            }}
            error={errores.descripcion}
            ayuda={`Escriba entre ${MIN_DESCRIPCION} y ${MAX_DESCRIPCION} caracteres. No incluya datos personales de terceros.`}
          />
          <p className="detalles__contador" aria-live="polite">
            {borrador.descripcion.length}/{MAX_DESCRIPCION}
          </p>
        </div>

        <div className="detalles__fotos">
          <span className="detalles__etiqueta">
            Foto del problema <span>(opcional)</span>
          </span>
          <div className="detalles__botones">
            <Boton variante="secundario" disabled={reduciendo} onClick={() => entradaCamara.current.click()}>
              <IconoCamara tamano={20} />
              Tomar foto
            </Boton>
            <Boton variante="secundario" disabled={reduciendo} onClick={() => entradaGaleria.current.click()}>
              <IconoImagen tamano={20} />
              De galería
            </Boton>
          </div>
          <input
            ref={entradaCamara}
            type="file"
            accept="image/jpeg,image/png"
            capture="environment"
            hidden
            onChange={agregarFotos}
          />
          <input ref={entradaGaleria} type="file" accept="image/jpeg,image/png" multiple hidden onChange={agregarFotos} />

          {fotos.map((foto, i) => (
            <div key={`${foto.name}-${i}`} className="detalles__foto">
              <span className="detalles__miniatura">
                <MiniaturaFoto foto={foto} numero={i + 1} />
              </span>
              <div>
                <strong>
                  Foto {i + 1} de {MAX_FOTOS}
                </strong>
                <span>{foto.name}</span>
              </div>
              <BotonIcono
                variante="peligro"
                disabled={reduciendo}
                etiqueta={`Quitar la foto ${i + 1}`}
                onClick={() => setFotos(fotos.filter((_, posicion) => posicion !== i))}
              >
                <IconoPapelera tamano={18} />
              </BotonIcono>
            </div>
          ))}
          {reduciendo && (
            <p className="reportar__mensaje" role="status">
              Preparando foto…
            </p>
          )}
          <p className="reportar__mensaje">Hasta {MAX_FOTOS} fotos JPG o PNG. Prepararemos cada foto para que no supere 1 MB.</p>
        </div>

        <Aviso variante="alerta" titulo="Sus fotos tendrán un enlace público">
          Cualquier persona con el enlace podrá ver cada foto. Evite que se vean rostros, menores de edad y
          placas de vehículos.
        </Aviso>

        {categoria.admiteMatricula && <Campo
          id="matricula"
          maxLength={LIMITES.matricula}
          etiqueta="Matrícula o cuenta del servicio"
          opcional
          placeholder="Ej. N.º de contrato o matrícula"
          ayuda="Es el número que aparece en su factura."
          value={borrador.matricula}
          onChange={(e) => cambiar({ matricula: e.target.value })}
        />}
        <Casilla id="emergencia" checked={borrador.emergencia === true} onChange={e => cambiar({ emergencia: e.target.checked })}>
          Es una emergencia o situación de riesgo inmediato. El correo no es un canal de atención urgente.
        </Casilla>
      </FormularioPantalla>

      <HojaInferior
        abierta={Boolean(errorFoto)}
        alCerrar={() => setErrorFoto('')}
        icono={<IconoImagenError tamano={30} />}
        tono="alerta"
        titulo="No pudimos adjuntar la foto"
        texto={errorFoto}
      >
        <Boton
          onClick={() => {
            setErrorFoto('')
            entradaGaleria.current.click()
          }}
        >
          Elegir otra foto
        </Boton>
        <Boton variante="texto" onClick={() => setErrorFoto('')}>
          Cerrar
        </Boton>
      </HojaInferior>
    </>
  )
}
