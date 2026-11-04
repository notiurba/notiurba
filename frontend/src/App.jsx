import { lazy, Suspense } from 'react'
import { Navigate, Route, Routes } from 'react-router-dom'
import Bienvenida from './compartido/paginas/Bienvenida'
import Login from './compartido/paginas/Login'
import AvisoPrivacidad from './compartido/paginas/AvisoPrivacidad'
import ConfirmarCorreo from './compartido/paginas/ConfirmarCorreo'
import CompletarPerfil from './compartido/paginas/CompletarPerfil'
import NuevaContrasena from './compartido/paginas/NuevaContrasena'
import RecuperarContrasena from './compartido/paginas/RecuperarContrasena'
import Registro from './compartido/paginas/Registro'
import VerificarCorreo from './compartido/paginas/VerificarCorreo'
import RutaProtegida from './compartido/auth/RutaProtegida'
import RutasApp from './app/rutas'
import EstadoApi from './app/paginas/EstadoApi'

// El flujo de reportes (con el mapa) y el panel se descargan solo cuando alguien entra a esas rutas.
const RutasReportar = lazy(() => import('./app/reportar/rutas'))
// El panel se descarga solo cuando alguien entra a /admin (el ciudadano nunca baja ese código).
const RutasAdmin = lazy(() => import('./admin/rutas'))

function App() {
  return (
    <Routes>
      <Route path="/" element={<Bienvenida />} />
      <Route path="/login" element={<Login />} />
      <Route path="/registro" element={<Registro />} />
      <Route path="/verificar-correo" element={<VerificarCorreo />} />
      <Route path="/recuperar" element={<RecuperarContrasena />} />
      <Route path="/nueva-contrasena" element={<NuevaContrasena />} />
      <Route path="/completar-perfil" element={<CompletarPerfil />} />
      <Route path="/aviso-privacidad" element={<AvisoPrivacidad />} />
      <Route
        path="/reportar/*"
        element={
          <Suspense fallback={<p className="app-loading">Cargando…</p>}>
            <RutasReportar />
          </Suspense>
        }
      />
      {/* Direcciones que el backend pone en los correos y en los avisos de sesión (ver .env.example del repo):
          son públicas y apuntan a las pantallas de esta app. */}
      <Route path="/app/verificar-correo" element={<ConfirmarCorreo />} />
      <Route path="/app/nueva-contrasena" element={<NuevaContrasena />} />
      <Route path="/app/ingresar" element={<Navigate to="/login?motivo=vencida" replace />} />
      {/* Prueba de conexión con la API (NOT-18): pública, para poder usarla sin iniciar sesión */}
      <Route path="/app/estado" element={<EstadoApi />} />
      <Route
        path="/app/*"
        element={
          <RutaProtegida>
            <RutasApp />
          </RutaProtegida>
        }
      />
      <Route
        path="/admin/*"
        element={
          <RutaProtegida rol="ADMIN">
            <Suspense fallback={<p className="app-loading">Cargando…</p>}>
              <RutasAdmin />
            </Suspense>
          </RutaProtegida>
        }
      />
    </Routes>
  )
}

export default App
