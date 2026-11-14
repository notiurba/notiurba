import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import Aviso from '../../compartido/componentes/avisos/Aviso'
import Boton from '../../compartido/componentes/Boton'
import EncabezadoPantalla from '../../compartido/componentes/EncabezadoPantalla'
import FormularioPantalla from '../../compartido/componentes/formulario/FormularioPantalla'
import HojaConfirmacion from '../../compartido/componentes/avisos/HojaConfirmacion'
import HojaInferior from '../../compartido/componentes/avisos/HojaInferior'
import IconoCategoria from '../../compartido/componentes/IconoCategoria'
import { IconoAlerta, IconoLlama, IconoTelefono } from '../../compartido/componentes/Iconos'
import PasosBarra from '../../compartido/componentes/PasosBarra'
import TarjetaOpcion from '../../compartido/componentes/TarjetaOpcion'
import { ID_CATEGORIA_GAS, LINEA_EMERGENCIA_GAS } from '../../compartido/utilidades/categorias'
import { tieneContenido } from './borrador'
import { destinoTrasCategoria } from './destino'
import { useReporte } from './useReporte'
import './Reportar.css'

// Paso 1 de 4: ¿qué problema desea reportar? (una sola categoría).
export default function PasoCategoria() {
  const navigate = useNavigate()
  const { borrador, cambiar, fotos, cancelarReporte, categorias, errorCatalogos, recargarCatalogos } = useReporte()
  const [confirmandoSalida, setConfirmandoSalida] = useState(false)
  const [errorCategoria, setErrorCategoria] = useState('')
  // Solo para gas: '' (sin responder), 'si' (es fuga o emergencia) o 'no'.
  const [respuestaGas, setRespuestaGas] = useState('')
  const seleccionada = categorias?.find((c) => String(c.idCategoria) === borrador.idCategoria)
  const esGas = borrador.idCategoria === String(ID_CATEGORIA_GAS)
  const preguntandoGas = esGas && respuestaGas === ''
  const bloqueadoPorGas = esGas && respuestaGas !== 'no' // con «sí» no se puede enviar el reporte

  function cerrar() {
    if (tieneContenido(borrador, fotos.length)) setConfirmandoSalida(true)
    else navigate('/')
  }

  return (
    <>
      <EncabezadoPantalla titulo="Nuevo reporte" alAtras={cerrar} cerrar />

      <FormularioPantalla
        alEnviar={(evento) => {
          evento.preventDefault()
          const elegida = categorias?.find((c) => String(c.idCategoria) === borrador.idCategoria)
          if (!elegida || errorCatalogos) {
            setErrorCategoria('Seleccione una categoría disponible para continuar.')
            return
          }
          if (bloqueadoPorGas) return
          navigate(destinoTrasCategoria(elegida))
        }}
        acciones={
          <>
            {esGas && respuestaGas === 'si' && (
              <>
                <Aviso variante="error" titulo={`Llame a la línea ${LINEA_EMERGENCIA_GAS}`}>
                  Una fuga de gas es una emergencia y no se reporta por NotiUrba. Llame ahora a la línea{' '}
                  {LINEA_EMERGENCIA_GAS} de Gases de Occidente.
                </Aviso>
                <Boton href={`tel:${LINEA_EMERGENCIA_GAS}`}>
                  <IconoTelefono tamano={20} />
                  Llamar al {LINEA_EMERGENCIA_GAS}
                </Boton>
              </>
            )}
            <Boton type="submit" disabled={!categorias?.some(c => String(c.idCategoria) === borrador.idCategoria) || Boolean(errorCatalogos) || bloqueadoPorGas}>
              Continuar
            </Boton>
            <p className="reportar__pie">
              {seleccionada?.permiteCorreo === false
                ? 'Le mostraremos los canales oficiales de la entidad.'
                : 'Su reporte se guardará en su cuenta.'}
            </p>
          </>
        }
      >
        <PasosBarra paso={1} total={4} etiqueta="Categoría" />
        <h2 className="reportar__titulo">¿Qué problema desea reportar?</h2>
        <p className="reportar__mensaje">Seleccione el tipo de problema que desea reportar.</p>
        {errorCategoria && <p role="alert" className="campo__error">{errorCategoria}</p>}
        {categorias?.length === 0 && <Aviso titulo="No hay categorías disponibles">Inténtelo más tarde.</Aviso>}

        {errorCatalogos && (
          <Aviso variante="error" titulo="No pudimos cargar las categorías">
            Revise su conexión e inténtelo de nuevo.
            <br />
            <button type="button" className="enlace-boton" onClick={recargarCatalogos}>
              Reintentar
            </button>
          </Aviso>
        )}
        {!categorias && !errorCatalogos && <p className="cargando-texto">Cargando…</p>}

        {categorias && (
          <div className="reportar__lista" role="radiogroup" aria-label="Tipo de problema">
            {categorias.map((categoria) => (
              <TarjetaOpcion
                key={categoria.idCategoria}
                grupo="categoria"
                valor={String(categoria.idCategoria)}
                seleccionada={borrador.idCategoria === String(categoria.idCategoria)}
                alElegir={(valor) => {
                  setRespuestaGas('') // otra categoría (o gas de nuevo): la pregunta se vuelve a hacer
                  setErrorCategoria('')
                  cambiar({ idCategoria: valor })
                }}
                icono={<IconoCategoria idCategoria={categoria.idCategoria} />}
                titulo={categoria.etiqueta}
                descripcion={categoria.descripcion}
              />
            ))}
          </div>
        )}

        <Aviso variante="info">
          ¿No encuentra su caso? Seleccione «Otro problema»: revisaremos su clasificación.
        </Aviso>
      </FormularioPantalla>

      <HojaInferior
        abierta={preguntandoGas}
        alCerrar={() => cambiar({ idCategoria: '' })}
        icono={<IconoLlama tamano={30} />}
        tono="alerta"
        titulo="¿Es una fuga o emergencia?"
        texto="Si huele a gas o hay riesgo ahora mismo, no es un reporte: es una emergencia."
      >
        <Boton variante="peligro" onClick={() => setRespuestaGas('si')}>
          Sí, es una fuga o emergencia
        </Boton>
        <Boton variante="secundario" onClick={() => setRespuestaGas('no')}>
          No, es otro problema con el gas
        </Boton>
      </HojaInferior>

      <HojaConfirmacion
        abierta={confirmandoSalida}
        alCancelar={() => setConfirmandoSalida(false)}
        alConfirmar={cancelarReporte}
        icono={<IconoAlerta tamano={30} />}
        tono="peligro"
        titulo="¿Cancelar el reporte?"
        texto="Si sale ahora, perderá la información que ha escrito."
        textoConfirmar="Sí, cancelar"
        textoCancelar="Seguir editando"
        peligro
      />
    </>
  )
}
