import Aviso from '../../componentes/avisos/Aviso'
import Campo from '../../componentes/formulario/Campo'
import { CamposResidencia } from '../../componentes/formulario/CamposPerfil'
import CampoPassword from '../../componentes/formulario/CampoPassword'
import Casilla from '../../componentes/formulario/Casilla'
import { IconoCorreo } from '../../componentes/Iconos'
import { EnlacePrivacidad } from '../../componentes/avisos/NotaPrivacidad'
import { LIMITES } from '../../validaciones/campos'

// Paso 2 de 2: dónde vive y con qué correo y contraseña entra.
export default function PasoAcceso({ datos, errores, cambiar, barrios }) {
  return (
    <>
      <CamposResidencia datos={datos} errores={errores} cambiar={cambiar} barrios={barrios} />

      <Campo
        id="correo"
        etiqueta="Correo electrónico"
        icono={<IconoCorreo />}
        type="email"
        autoComplete="email"
        maxLength={LIMITES.correo}
        placeholder="nombre@correo.com"
        value={datos.correo}
        onChange={cambiar('correo')}
        error={errores.correo}
      />

      <CampoPassword
        id="password"
        etiqueta="Contraseña"
        autoComplete="new-password"
        placeholder="Cree una contraseña"
        ayuda="Mínimo 8 caracteres, con letras y números. Puede usar símbolos."
        value={datos.password}
        onChange={cambiar('password')}
        error={errores.password}
      />

      <CampoPassword
        id="confirmar"
        etiqueta="Confirmar contraseña"
        autoComplete="new-password"
        placeholder="Repita su contraseña"
        value={datos.confirmar}
        onChange={cambiar('confirmar')}
        error={errores.confirmar}
      />

      <Aviso variante="info">
        Sus datos permiten identificar a quien presenta el reporte. Solo se usan para gestionar sus
        reportes.
      </Aviso>

      <Casilla
        id="aceptaPrivacidad"
        checked={datos.aceptaPrivacidad}
        onChange={cambiar('aceptaPrivacidad')}
        error={errores.aceptaPrivacidad}
      >
        Soy mayor de 18 años y acepto el tratamiento de mis datos personales (Ley 1581). <EnlacePrivacidad />
      </Casilla>
    </>
  )
}
