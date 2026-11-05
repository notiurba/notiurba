import PanelMarca from './PanelMarca'
import './LayoutAuth.css'

// Estructura de las pantallas de acceso.
//  - web: solo Login y Registro (las 2 pantallas web del diseño). En computador llevan el panel de marca a
//    la izquierda y el formulario a la derecha; en celular, una sola columna.
//  - sin web: las demás (recuperar, nueva contraseña, completar perfil, verificar correo, aviso de
//    privacidad) son de celular: una columna de 430 px máximo, también en computador.
export default function LayoutAuth({ web = false, children }) {
  return (
    <main className={web ? 'auth' : 'auth auth--celular'}>
      {web && <PanelMarca />}
      <section className="auth__panel">{children}</section>
    </main>
  )
}
