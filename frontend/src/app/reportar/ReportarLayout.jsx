import { useCallback, useEffect, useState } from 'react'
import { Navigate, Outlet, useLocation, useNavigate } from 'react-router-dom'
import { obtenerBarrios } from '../../compartido/api/catalogos'
import { estadoSesion } from '../../compartido/auth/sesion'
import Aviso from '../../compartido/componentes/avisos/Aviso'
import Boton from '../../compartido/componentes/Boton'
import * as api from '../servicios/correoReportes'
import useCargar from '../../compartido/hooks/useCargar'
import MarcoApp from '../componentes/MarcoApp'
import { borrarBorrador, guardarBorrador, leerBorrador, guardarAvance, leerGuardado } from './borrador'
import { prepararCategorias } from './catalogo'
import { redireccionPorCategoria } from './destino'

const cargarCategorias = async () => estadoSesion() === 'activa' ? prepararCategorias(await api.categorias()) : []
const cargarBarrios = () => estadoSesion() === 'activa' ? obtenerBarrios() : Promise.resolve([])

// Contenedor de los 4 pasos de «Nuevo reporte». Guarda lo que la persona va escribiendo (borrador y
// fotos) y carga una sola vez las listas de categorías y barrios, para que los pasos las compartan.
export default function ReportarLayout() {
  const [borrador, setBorrador] = useState(leerBorrador)
  const [fotos, setFotos] = useState([])
  const [guardado, setGuardado] = useState(leerGuardado)
  const categorias = useCargar(cargarCategorias)
  const barrios = useCargar(cargarBarrios)
  const { pathname } = useLocation()
  const navigate = useNavigate()

  useEffect(() => guardarBorrador(borrador), [borrador])

  const limpiar = useCallback(() => {
    borrarBorrador()
    setBorrador(leerBorrador())
    setFotos([])
    guardarAvance(null)
    setGuardado(null)
  }, [])

  // Sincronizar el borrador con la navegacion ya completada, nunca antes de salir del paso 4.
  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect -- La URL confirma el fin del flujo; el guardado se consume una sola vez.
    if (guardado && pathname === '/reportar/enviado/' + guardado.codigoSeguimiento) limpiar()
  }, [pathname, guardado, limpiar])

  function conservarGuardado(valor) {
    guardarAvance(valor)
    setGuardado(valor)
  }

  const contexto = {
    borrador,
    // Cambia solo los campos que se pasan: cambiar({ descripcion: '…' })
    cambiar: (parcial) => setBorrador((anterior) => {
      if (parcial.idCategoria !== undefined && parcial.idCategoria !== anterior.idCategoria) {
        return { ...anterior, ...parcial, matricula: '', emergencia: false, fotoObligatoria: false,
          latitud: null, longitud: null, idBarrio: '', direccion: '' }
      }
      // Otro lugar puede no tener un problema resuelto cerca: la vista previa lo vuelve a decir.
      const otroLugar = (parcial.latitud !== undefined && parcial.latitud !== anterior.latitud)
        || (parcial.longitud !== undefined && parcial.longitud !== anterior.longitud)
      return { ...anterior, ...(otroLugar ? { fotoObligatoria: false } : {}), ...parcial }
    }),
    fotos,
    setFotos,
    guardado,
    conservarGuardado,
    // Después de enviar o cancelar, el borrador queda vacío.
    limpiar,
    // «Cancelar reporte»: descarta el borrador y vuelve al inicio (paso 1 y aviso de reporte parecido).
    cancelarReporte: () => {
      limpiar()
      navigate('/')
    },
    categorias: categorias.datos,
    // Si no hay barrios disponibles se conserva la alternativa del punto en el mapa.
    barrios: barrios.datos ?? [],
    cargandoBarrios: barrios.cargando,
    errorBarrios: barrios.error,
    errorCatalogos: categorias.error,
    recargarCatalogos: () => {
      categorias.recargar()
      barrios.recargar()
    },
  }

  if (estadoSesion() !== 'activa') {
    return <MarcoApp>
      <Aviso titulo="Inicie sesión para crear un reporte">
        Para crear un reporte necesita una cuenta. Lo que ya escribió se conserva.
      </Aviso>
      <Boton to="/login" state={{ siguiente: pathname }}>Iniciar sesión</Boton>
      <Boton to="/registro" state={{ siguiente: pathname }} variante="secundario">Crear cuenta</Boton>
    </MarcoApp>
  }

  // Tras crear, solo se recupera ese código. No se permite volver a editar y duplicar la creación.
  if (guardado && pathname !== '/reportar/confirmacion' && !pathname.startsWith('/reportar/enviado/')) {
    return <Navigate to="/reportar/confirmacion" replace />
  }

  // Gas, energía y aseo no tienen pasos 2 a 4: alguien que llegue ahí por URL va a sus canales.
  const elegida = categorias.datos?.find((c) => String(c.idCategoria) === borrador.idCategoria)
  const redireccion = redireccionPorCategoria(pathname, elegida, guardado)
  if (redireccion) return <Navigate to={redireccion} replace />

  if (categorias.error && !pathname.startsWith('/reportar/enviado/') && !guardado) {
    return <MarcoApp><Aviso variante="error" titulo="No pudimos cargar las categorías">
      Revise su conexión e inténtelo de nuevo.
    </Aviso><Boton onClick={categorias.recargar}>Reintentar</Boton></MarcoApp>
  }

  return (
    <div className="reportar-marco"><MarcoApp>
      <Outlet context={contexto} />
    </MarcoApp></div>
  )
}
