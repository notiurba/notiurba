import { useEffect, useRef, useState } from 'react'
import { Navigate, useNavigate } from 'react-router-dom'
import Aviso from '../../compartido/componentes/avisos/Aviso'
import Boton from '../../compartido/componentes/Boton'
import Campo from '../../compartido/componentes/formulario/Campo'
import EncabezadoPantalla from '../../compartido/componentes/EncabezadoPantalla'
import FormularioPantalla from '../../compartido/componentes/formulario/FormularioPantalla'
import HojaInferior from '../../compartido/componentes/avisos/HojaInferior'
import { IconoSinUbicacion, IconoUbicacion } from '../../compartido/componentes/Iconos'
import PasosBarra from '../../compartido/componentes/PasosBarra'
import Selector from '../../compartido/componentes/formulario/Selector'
import { validarUbicacion } from '../../compartido/validaciones/reporte'
import MapaUbicacion from './MapaUbicacion'
import ResumenCategoria from './ResumenCategoria'
import { useReporte } from './useReporte'
import { obtenerPosicionGps, TEXTOS_UBICACION } from './ubicacionGps'
import './Reportar.css'
import './PasoUbicacion.css'

// Paso 2 de 4: ¿dónde ocurrió? Punto en el mapa (o GPS), dirección y barrio.
// El backend asigna la comuna; una capa de referencia del mapa no decide el rechazo.
export default function PasoUbicacion() {
  const navigate = useNavigate()
  const { borrador, cambiar, categorias, barrios, cargandoBarrios = false, errorBarrios, recargarCatalogos } = useReporte()
  const [errores, setErrores] = useState({})
  const [buscandoGps, setBuscandoGps] = useState(false)
  const [gpsFallo, setGpsFallo] = useState(null)
  const [gpsIntentado, setGpsIntentado] = useState(false)
  const solicitudGps = useRef(0)

  // Ignora respuestas que lleguen después de salir o de elegir un punto manualmente.
  useEffect(() => () => { solicitudGps.current += 1 }, [])

  const categoria = categorias?.find((c) => String(c.idCategoria) === borrador.idCategoria)
  if (!borrador.idCategoria) return <Navigate to="/reportar" replace />
  if (!categorias) return <p className="cargando-texto">Cargando…</p>
  if (!categoria) return <Navigate to="/reportar" replace />
  const pideUbicacion = categoria.requiereUbicacion !== false

  const barrio = barrios.find((b) => String(b.idBarrio) === borrador.idBarrio)
  const hayPunto = borrador.latitud != null

  function elegirPunto(latitud, longitud) {
    solicitudGps.current += 1
    setBuscandoGps(false)
    setGpsFallo(null)
    cambiar({ latitud, longitud })
    setErrores((anteriores) => ({ ...anteriores, punto: '' }))
  }

  async function usarGps() {
    if (buscandoGps) return
    const solicitud = ++solicitudGps.current
    setGpsFallo(null)
    setGpsIntentado(true)
    setBuscandoGps(true)
    try {
      const { latitud, longitud } = await obtenerPosicionGps(navigator.geolocation)
      if (solicitud === solicitudGps.current) elegirPunto(latitud, longitud)
    } catch (fallo) {
      if (solicitud === solicitudGps.current) {
        setBuscandoGps(false)
        setGpsFallo(fallo)
      }
    }
  }

  function continuar(evento) {
    evento.preventDefault()
    const encontrados = validarUbicacion(borrador, categoria)
    if (encontrados.punto && borrador.latitud == null && borrador.longitud == null && !borrador.idBarrio) {
      encontrados.punto = barrios.length ? TEXTOS_UBICACION.puntoOBarrio : TEXTOS_UBICACION.puntoRequerido
    }
    if (pideUbicacion && borrador.idBarrio && !barrios.some(b => String(b.idBarrio) === borrador.idBarrio)) {
      if (cargandoBarrios) {
        encontrados.punto = 'Espere a que termine de cargar la lista para comprobar el barrio seleccionado.'
      } else if (hayPunto && !encontrados.punto) {
        // Un punto válido permite continuar sin enviar un ID obsoleto del borrador.
        cambiar({ idBarrio: '' })
      } else {
        encontrados.punto = 'Seleccione un barrio disponible o marque el lugar en el mapa.'
      }
    }
    setErrores(encontrados)
    if (Object.keys(encontrados).length === 0) navigate('/reportar/detalles')
  }

  return (
    <>
      <EncabezadoPantalla titulo="Nuevo reporte" atras="/reportar" />

      <FormularioPantalla
        alEnviar={continuar}
        acciones={<Boton type="submit">{pideUbicacion ? 'Continuar' : 'Continuar sin ubicación'}</Boton>}
      >
        <PasosBarra paso={2} total={4} etiqueta="Ubicación" />
        <ResumenCategoria categoria={categoria} />
        <h2 className="reportar__titulo">{pideUbicacion ? '¿Dónde ocurrió?' : 'Ubicación no requerida'}</h2>
        {!pideUbicacion && <Aviso>Esta categoría no requiere ubicación. Puede continuar al paso de descripción y fotografías.</Aviso>}
        {pideUbicacion && <>
        {import.meta.env.DEV && import.meta.env.VITE_USAR_MOCKS === 'true' && (
          <Aviso titulo="Modo demostración">
            La lista contiene barrios de prueba y no representa el catálogo real de Tuluá.
          </Aviso>
        )}
        <p id="ubicacion-explicacion" className="reportar__mensaje">{TEXTOS_UBICACION.antesDelPermiso}</p>
        <p id="ubicacion-instruccion" className="reportar__mensaje">{TEXTOS_UBICACION.instruccionMapa}</p>
        <p id="ubicacion-teclado" className="reportar__mensaje">{TEXTOS_UBICACION.ayudaTeclado}</p>

        <MapaUbicacion latitud={borrador.latitud} longitud={borrador.longitud} alElegir={elegirPunto} error={errores.punto} />
        {errores.punto && (
          <p id="ubicacion-error" className="campo__error" role="alert">
            {errores.punto}
          </p>
        )}

        <Boton variante="secundario" onClick={usarGps} cargando={buscandoGps} textoCargando="Buscando su ubicación…" aria-describedby="ubicacion-explicacion">
          <IconoUbicacion tamano={20} />
          {gpsIntentado || hayPunto ? TEXTOS_UBICACION.volverUbicar : 'Usar mi ubicación actual (GPS)'}
        </Boton>

        {buscandoGps && (
          <div className="ubicacion__buscando" role="status" aria-live="polite">
            <span className="ubicacion__giro" aria-hidden="true" />
            <p>Buscando su ubicación. Esto puede tardar unos segundos. También puede marcar el lugar en el mapa.</p>
          </div>
        )}

        {(hayPunto || barrio) && (
          <Aviso titulo="Ubicación seleccionada">
            {hayPunto ? 'Punto marcado en el mapa. ' : `Barrio ${barrio.nombre}. `}
            El servidor comprobará la cobertura y asignará la comuna al guardar.
          </Aviso>
        )}
        {hayPunto && <Boton variante="texto" onClick={() => elegirPunto(null, null)}>Quitar punto y elegir barrio</Boton>}

        <Campo
          id="direccion"
          etiqueta="Dirección del incidente"
          opcional
          ayuda="Agregue una referencia para encontrar el lugar."
          placeholder="Ej. Cra 26 #28-40"
          autoComplete="off"
          maxLength={300}
          value={borrador.direccion}
          onChange={(e) => cambiar({ direccion: e.target.value })}
        />

        <p className="reportar__mensaje">El mapa no selecciona el barrio automáticamente. Puede elegirlo en la lista; con un punto marcado es opcional.</p>
        {borrador.idBarrio && !barrio && !cargandoBarrios && (
          <Aviso titulo="Revise el barrio anterior">
            No se pudo confirmar el barrio guardado en la lista actual. Puede elegir otro o continuar con el punto del mapa; en ese caso no se enviará el barrio anterior.
          </Aviso>
        )}
        {cargandoBarrios && <p className="cargando-texto" role="status">Cargando barrios…</p>}
        {barrios.length > 0 && (
          <Selector
            id="idBarrio"
            etiqueta="Barrio"
            opcional={hayPunto}
            disabled={cargandoBarrios}
            marcador="Seleccione el barrio"
            ayuda="Si no marcó un lugar, seleccione un barrio disponible."
            opciones={barrios.map((b) => ({ valor: b.idBarrio, texto: b.nombre }))}
            value={borrador.idBarrio}
            onChange={(e) => cambiar({ idBarrio: e.target.value })}
          />
        )}
        {!cargandoBarrios && !errorBarrios && barrios.length === 0 && <Aviso>No hay barrios disponibles. Marque el lugar en el mapa.</Aviso>}
        {errorBarrios && <Aviso variante="error" titulo="No pudimos cargar los barrios">
          Puede marcar el lugar en el mapa. <button type="button" className="enlace-boton" onClick={recargarCatalogos}>Reintentar</button>
        </Aviso>}
        </>}
      </FormularioPantalla>

      <HojaInferior
        abierta={Boolean(gpsFallo)}
        alCerrar={() => setGpsFallo(null)}
        icono={<IconoSinUbicacion tamano={30} />}
        tono="alerta"
        titulo={gpsFallo?.titulo}
        texto={gpsFallo?.texto}
      >
        <Boton onClick={() => setGpsFallo(null)}>Marcar en el mapa</Boton>
        <Boton variante="texto" onClick={usarGps}>
          {TEXTOS_UBICACION.volverUbicar}
        </Boton>
      </HojaInferior>
    </>
  )
}

