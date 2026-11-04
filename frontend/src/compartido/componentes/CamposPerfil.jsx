import Campo from './Campo'
import { IconoCorreo, IconoTelefono } from './Iconos'
import Selector from './Selector'
import './CamposPerfil.css'

// Campos de identidad: nombres, apellidos, documento y teléfono. Los usan el registro (paso 1),
// «Completa tu perfil» (Google) y «Editar mis datos». No guardan nada: reciben `datos`, `errores` y
// `cambiar(campo)` de la pantalla que los contiene. Si se pasa `correo`, se muestra bloqueado
// (en «Editar mis datos» el correo no se puede cambiar).
export function CamposIdentidad({ datos, errores, cambiar, tiposDocumento, correo }) {
  return (
    <>
      <div className="campos-fila">
        <Campo
          id="nombres"
          etiqueta="Nombres"
          placeholder="Ej. María Camila"
          autoComplete="given-name"
          value={datos.nombres}
          onChange={cambiar('nombres')}
          error={errores.nombres}
        />
        <Campo
          id="apellidos"
          etiqueta="Apellidos"
          placeholder="Ej. Gómez Rojas"
          autoComplete="family-name"
          value={datos.apellidos}
          onChange={cambiar('apellidos')}
          error={errores.apellidos}
        />
      </div>

      <div className="campos-fila">
        <Selector
          id="idTipoDocumento"
          etiqueta="Tipo de documento"
          opciones={tiposDocumento.map((t) => ({ valor: t.idTipoDocumento, texto: t.codigo }))}
          value={datos.idTipoDocumento}
          onChange={cambiar('idTipoDocumento')}
          error={errores.idTipoDocumento}
        />
        <Campo
          id="numeroDocumento"
          etiqueta="N.º de documento"
          placeholder="Ej. 1234567890"
          inputMode="numeric"
          value={datos.numeroDocumento}
          onChange={cambiar('numeroDocumento')}
          error={errores.numeroDocumento}
        />
      </div>

      {correo && (
        <Campo
          id="correo"
          etiqueta="Correo electrónico"
          icono={<IconoCorreo />}
          value={correo}
          disabled
          readOnly
          ayuda="El correo no se puede cambiar."
        />
      )}

      <Campo
        id="telefono"
        etiqueta="Teléfono"
        opcional
        icono={<IconoTelefono />}
        type="tel"
        inputMode="numeric"
        autoComplete="tel"
        placeholder="Ej. 3001234567"
        value={datos.telefono}
        onChange={cambiar('telefono')}
        error={errores.telefono}
      />
    </>
  )
}

// Dónde vive: barrio de residencia y dirección de correspondencia.
export function CamposResidencia({ datos, errores, cambiar, barrios }) {
  return (
    <>
      <Selector
        id="idBarrio"
        etiqueta="Barrio de residencia"
        marcador="Seleccione su barrio"
        opciones={barrios.map((b) => ({ valor: b.idBarrio, texto: b.nombre }))}
        value={datos.idBarrio}
        onChange={cambiar('idBarrio')}
        error={errores.idBarrio}
      />
      <Campo
        id="direccionCorrespondencia"
        etiqueta="Dirección de correspondencia"
        placeholder="Ej. Cra 26 #28-40"
        autoComplete="street-address"
        ayuda="Donde podría llegar correspondencia oficial."
        value={datos.direccionCorrespondencia}
        onChange={cambiar('direccionCorrespondencia')}
        error={errores.direccionCorrespondencia}
      />
    </>
  )
}
