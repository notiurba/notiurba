import { useEffect, useState } from 'react'
import './App.css'
import MapaTulua from './components/mapas/MapaTulua'
import { obtenerReportesCiudadanos } from './services/reportesService'

function App() {
  const [reportes, setReportes] = useState([])
  const [cargando, setCargando] = useState(true)

  useEffect(() => {
    obtenerReportesCiudadanos()
      .then(setReportes)
      .finally(() => setCargando(false))
  }, [])

  if (cargando) {
    return (
      <main className="app-loading">
        <span>Cargando mapa ciudadano...</span>
      </main>
    )
  }

  return (
    <main>
      <MapaTulua reportes={reportes} />
    </main>
  )
}

export default App
