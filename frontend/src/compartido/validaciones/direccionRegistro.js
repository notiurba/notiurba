import { validarDireccion } from './campos.js'

// Restricción de presentación exclusiva del registro; no cambia el contrato ni otras pantallas.
export const MAX_DIRECCION_REGISTRO = 100

export function validarDireccionRegistro(valor) {
  if (String(valor ?? '').length > MAX_DIRECCION_REGISTRO) {
    return 'Use máximo 100 caracteres en la dirección. Revise el texto; no se ha recortado.'
  }
  return validarDireccion(valor)
}
