import Boton from './Boton'
import { IconoGoogle } from './Iconos'

// «Continuar con Google». El inicio de sesión real con Google todavía no está conectado al backend:
// con la simulación activa lleva a «Completa tu perfil» para poder ver esa pantalla; sin ella queda apagado.
const SIMULANDO = import.meta.env.VITE_USAR_MOCKS === 'true'

export default function BotonGoogle() {
  const propiedades = SIMULANDO
    ? { to: '/completar-perfil', state: { correo: 'nombre@gmail.com' } }
    : { disabled: true, title: 'Próximamente' }

  return (
    <Boton variante="secundario" {...propiedades}>
      <IconoGoogle />
      Continuar con Google
    </Boton>
  )
}
