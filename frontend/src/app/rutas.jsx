import { Route, Routes } from 'react-router-dom'
import Proximamente from './paginas/Proximamente'

export default function RutasApp() {
  return (
    <Routes>
      <Route index element={<Proximamente />} />
    </Routes>
  )
}
