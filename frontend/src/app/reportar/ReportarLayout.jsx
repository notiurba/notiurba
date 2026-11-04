import { useEffect, useState } from 'react'
import { Outlet } from 'react-router-dom'
import { obtenerBarrios } from '../../compartido/api/catalogos'
import { obtenerCategorias } from '../../compartido/api/reportes'
import useCargar from '../../compartido/hooks/useCargar'
import MarcoApp from '../componentes/MarcoApp'
import { borrarBorrador, guardarBorrador, leerBorrador } from './borrador'

// Contenedor de los 4 pasos de «Nuevo reporte». Guarda lo que la persona va escribiendo (borrador y
// fotos) y carga una sola vez las listas de categorías y barrios, para que los pasos las compartan.
export default function ReportarLayout() {
  const [borrador, setBorrador] = useState(leerBorrador)
  const [fotos, setFotos] = useState([])
  const categorias = useCargar(obtenerCategorias)
  const barrios = useCargar(obtenerBarrios)

  useEffect(() => guardarBorrador(borrador), [borrador])

  const contexto = {
    borrador,
    // Cambia solo los campos que se pasan: cambiar({ descripcion: '…' })
    cambiar: (parcial) => setBorrador((anterior) => ({ ...anterior, ...parcial })),
    fotos,
    setFotos,
    // Después de enviar o cancelar, el borrador queda vacío.
    limpiar: () => {
      borrarBorrador()
      setBorrador(leerBorrador())
      setFotos([])
    },
    categorias: categorias.datos,
    // Los barrios son opcionales (la API aún no tiene la lista): sin ellos se reporta solo con el punto del mapa.
    barrios: barrios.datos ?? [],
    errorCatalogos: categorias.error,
    recargarCatalogos: () => {
      categorias.recargar()
      barrios.recargar()
    },
  }

  return (
    <MarcoApp>
      <Outlet context={contexto} />
    </MarcoApp>
  )
}
