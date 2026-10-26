import { Navigate, Route, Routes } from 'react-router-dom'
import RutasApp from './app/rutas'
import RutasAdmin from './admin/rutas'

function App() {
  return (
    <Routes>
      <Route path="/" element={<Navigate to="/app" replace />} />
      <Route path="/app/*" element={<RutasApp />} />
      <Route path="/admin/*" element={<RutasAdmin />} />
    </Routes>
  )
}

export default App
