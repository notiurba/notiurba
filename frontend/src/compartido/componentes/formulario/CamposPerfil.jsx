import { useContext } from 'react'
import { LIMITES, documentoEsNumerico, limpiarNumeroDocumento } from '../../validaciones/campos'
import { LimiteDireccionContexto } from './limiteDireccionContexto'
import Campo from './Campo'
import { IconoCorreo, IconoTelefono } from '../Iconos'
import Selector from './Selector'
import './CamposPerfil.css'

// Campos de identidad: nombres, apellidos, documento y teléfono. Los usan el registro (paso 1),
// «Complete su perfil» (Google) y «Editar mis datos». No guardan nada: reciben `datos`, `errores` y
// `cambiar(campo)` de la pantalla que los contiene. Si se pasa `correo`, se muestra bloqueado
// (en «Editar mis datos» el correo no se puede cambiar).
export function CamposIdentidad({ datos, errores, cambiar, tiposDocumento, correo }) {
  // El número solo admite lo que su tipo permite (CC y CE: dígitos; pasaporte: letras y números).
  // Al cambiar el tipo se vuelve a limpiar lo que ya estaba escrito.
  function escribirDocumento(evento) {
    cambiar('numeroDocumento')({ target: { value: limpiarNumeroDocumento(evento.target.value, datos.idTipoDocumento) } })
  }
  function cambiarTipoDocumento(evento) {
    cambiar('idTipoDocumento')(evento)
    cambiar('numeroDocumento')({ target: { value: limpiarNumeroDocumento(datos.numeroDocumento, evento.target.value) } })
  }

  return (
    <>
      <div className="campos-fila">
        <Campo
          id="nombres"
          etiqueta="Nombres"
          placeholder="Ej. María Camila"
          autoComplete="given-name"
          maxLength={LIMITES.nombre}
          ayuda={`Máximo ${LIMITES.nombre} caracteres (${datos.nombres.length}/${LIMITES.nombre}).`}
          value={datos.nombres}
          onChange={cambiar('nombres')}
          error={errores.nombres}
        />
        <Campo
          id="apellidos"
          etiqueta="Apellidos"
          placeholder="Ej. Gómez Rojas"
          autoComplete="family-name"
          maxLength={LIMITES.nombre}
          ayuda={`Máximo ${LIMITES.nombre} caracteres (${datos.apellidos.length}/${LIMITES.nombre}).`}
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
          onChange={cambiarTipoDocumento}
          error={errores.idTipoDocumento}
        />
        <Campo
          id="numeroDocumento"
          etiqueta="N.º de documento"
          placeholder="Ej. 1234567890"
          inputMode={!datos.idTipoDocumento || documentoEsNumerico(datos.idTipoDocumento) ? 'numeric' : 'text'}
          maxLength={LIMITES.documento}
          autoComplete="off"
          value={datos.numeroDocumento}
          onChange={escribirDocumento}
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
        maxLength={LIMITES.telefono}
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
  const limiteRegistro = useContext(LimiteDireccionContexto)
  return (
    <>
      <Selector
        id="idBarrio"
        etiqueta="Barrio de residencia"
        marcador="Seleccione su barrio"
        disabled={barrios.length === 0}
        ayuda={barrios.length === 0 ? 'No hay barrios disponibles para seleccionar. El registro estará disponible cuando se habilite este catálogo.' : undefined}
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
        maxLength={limiteRegistro ?? LIMITES.direccion}
        acciones={<span className="campo__contador" aria-live="polite">{datos.direccionCorrespondencia.length}/{limiteRegistro ?? LIMITES.direccion}</span>}
        ayuda="Donde podría llegar correspondencia oficial."
        value={datos.direccionCorrespondencia}
        onChange={cambiar('direccionCorrespondencia')}
        error={errores.direccionCorrespondencia || (limiteRegistro && datos.direccionCorrespondencia.length > limiteRegistro
          ? `Use máximo ${limiteRegistro} caracteres (${datos.direccionCorrespondencia.length}/${limiteRegistro}). Revise la dirección; no se ha recortado.`
          : undefined)}
      />
    </>
  )
}
