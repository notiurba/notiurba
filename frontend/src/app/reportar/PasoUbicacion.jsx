import { useState } from 'react'
import { Navigate, useNavigate } from 'react-router-dom'
import Aviso from '../../compartido/componentes/Aviso'
import Boton from '../../compartido/componentes/Boton'
import Campo from '../../compartido/componentes/Campo'
import EncabezadoPantalla from '../../compartido/componentes/EncabezadoPantalla'
import FormularioPantalla from '../../compartido/componentes/FormularioPantalla'
import HojaInferior from '../../compartido/componentes/HojaInferior'
import { IconoSinUbicacion, IconoUbicacion } from '../../compartido/componentes/Iconos'
import PasosBarra from '../../compartido/componentes/PasosBarra'
import Selector from '../../compartido/componentes/Selector'
import { comunasTulua } from '../../compartido/geo/comunasTulua'
import { identificarComunaPorCoordenadas } from '../../compartido/utils/geoUtils'
import { validarUbicacion } from '../../compartido/validaciones/reporte'
import MapaUbicacion from './MapaUbicacion'
import ResumenCategoria from './ResumenCategoria'
import { useReporte } from './useReporte'
import './Reportar.css'
import './PasoUbicacion.css'

const MENSAJE_FUERA_DE_TULUA =
  'Ese punto está fuera de las comunas de Tuluá. Marque un lugar dentro de la ciudad o acérquese al límite si está justo en el borde.'

// Paso 2 de 4: ¿dónde ocurrió? Punto en el mapa (o GPS), dirección y barrio.
// La comuna sale del barrio elegido (POT 2015, provisional); la fuente de verdad es el backend.
export default function PasoUbicacion() {
  const navigate = useNavigate()
  const { borrador, cambiar, categorias, barrios } = useReporte()
  const [errores, setErrores] = useState({})
  const [buscandoGps, setBuscandoGps] = useState(false)
  const [gpsFallo, setGpsFallo] = useState(false)

  const categoria = categorias?.find((c) => String(c.idCategoria) === borrador.idCategoria)
  if (!borrador.idCategoria) return <Navigate to="/reportar" replace />
  if (!categoria) return <p className="cargando-texto">Cargando…</p>

  const barrio = barrios.find((b) => String(b.idBarrio) === borrador.idBarrio)
  const hayPunto = borrador.latitud != null
  // Comuna según el punto del mapa (capa POT 2015, provisional); null si cae en un límite o fuera de la capa.
  const comunaDelMapa = hayPunto
    ? identificarComunaPorCoordenadas({ latitud: borrador.latitud, longitud: borrador.longitud }, comunasTulua).comuna
    : null
  const fueraDeTulua = hayPunto && !comunaDelMapa

  function elegirPunto(latitud, longitud) {
    cambiar({ latitud, longitud })
    setErrores((anteriores) => ({ ...anteriores, punto: '' }))
  }

  function usarGps() {
    setGpsFallo(false)
    if (!navigator.geolocation) {
      setGpsFallo(true)
      return
    }
    setBuscandoGps(true)
    navigator.geolocation.getCurrentPosition(
      ({ coords }) => {
        elegirPunto(coords.latitude, coords.longitude)
        setBuscandoGps(false)
      },
      () => {
        setBuscandoGps(false)
        setGpsFallo(true)
      },
      { enableHighAccuracy: true, timeout: 15000 },
    )
  }

  function continuar(evento) {
    evento.preventDefault()
    const encontrados = validarUbicacion(borrador)
    if (fueraDeTulua) encontrados.punto = MENSAJE_FUERA_DE_TULUA
    setErrores(encontrados)
    if (Object.keys(encontrados).length === 0) navigate('/reportar/detalles')
  }

  return (
    <>
      <EncabezadoPantalla titulo="Nuevo reporte" atras="/reportar" />

      <FormularioPantalla
        alEnviar={continuar}
        acciones={
          <>
            <Boton type="submit">Continuar</Boton>
          </>
        }
      >
        <PasosBarra paso={2} total={4} etiqueta="Ubicación" />
        <ResumenCategoria categoria={categoria} />
        <h2 className="reportar__titulo">¿Dónde ocurrió?</h2>

        <MapaUbicacion latitud={borrador.latitud} longitud={borrador.longitud} alElegir={elegirPunto} />
        {(errores.punto || fueraDeTulua) && (
          <p className="campo__error" role="alert">
            {errores.punto || MENSAJE_FUERA_DE_TULUA}
          </p>
        )}

        <Boton variante="secundario" onClick={usarGps} cargando={buscandoGps} textoCargando="Buscando…">
          <IconoUbicacion tamano={20} />
          Usar mi ubicación actual (GPS)
        </Boton>

        {hayPunto && (barrio || comunaDelMapa) && (
          <Aviso variante="exito" titulo="Ubicación detectada">
            <strong>
              {barrio
                ? `Barrio ${barrio.nombre} · Comuna ${barrio.idComuna}`
                : `Comuna ${Number(comunaDelMapa.id)} (según el mapa)`}
            </strong>
            <br />
            La comuna se asigna según su ubicación GPS o el barrio seleccionado (POT 2015, provisional).
          </Aviso>
        )}

        <Campo
          id="direccion"
          etiqueta="Dirección"
          placeholder="Ej. Cra 26 #28-40"
          autoComplete="street-address"
          value={borrador.direccion}
          onChange={(e) => cambiar({ direccion: e.target.value })}
        />

        {barrios.length > 0 && (
          <Selector
            id="idBarrio"
            etiqueta="Barrio"
            marcador="Seleccione el barrio"
            opciones={barrios.map((b) => ({ valor: b.idBarrio, texto: b.nombre }))}
            value={borrador.idBarrio}
            onChange={(e) => {
              cambiar({ idBarrio: e.target.value })
              setErrores((anteriores) => ({ ...anteriores, idBarrio: '' }))
            }}
            error={errores.idBarrio}
          />
        )}
      </FormularioPantalla>

      {buscandoGps && (
        <div className="ubicacion__buscando" role="status">
          <div className="ubicacion__tarjeta">
            <span className="ubicacion__giro" aria-hidden="true" />
            <strong>Buscando su ubicación…</strong>
            <span>Esto puede tardar unos segundos.</span>
          </div>
        </div>
      )}

      <HojaInferior
        abierta={gpsFallo}
        alCerrar={() => setGpsFallo(false)}
        icono={<IconoSinUbicacion tamano={30} />}
        tono="alerta"
        titulo="No pudimos obtener su ubicación"
        texto="Active el permiso de ubicación en su teléfono o marque el punto en el mapa y elija su barrio."
      >
        <Boton onClick={() => setGpsFallo(false)}>Marcar en el mapa</Boton>
        <Boton variante="texto" onClick={usarGps}>
          Reintentar con GPS
        </Boton>
      </HojaInferior>
    </>
  )
}

