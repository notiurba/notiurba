// Error que lanzan las llamadas a la API cuando algo falla. Lo comparten el cliente real (cliente.js)
// y los datos simulados (simulacion/index.js), así las pantallas reciben siempre lo mismo.
// Sigue el contrato de la API (docs/contexto/11-contrato-api.md, sección 2): { codigo, mensaje, campo }.
//   error.estado  → número HTTP (401, 409, 500...); 0 si no hubo red
//   error.codigo  → texto estable del contrato ('CORREO_YA_REGISTRADO', 'VALIDACION', 'SIN_CONEXION'...)
//   error.campo   → casilla a la que se refiere el error ('correo', 'numeroDocumento'...) o null
//   error.message → texto en español para mostrar a la persona (también disponible como error.mensaje)
export class ErrorApi extends Error {
  constructor({ estado, codigo, mensaje, campo = null }) {
    super(mensaje)
    this.name = 'ErrorApi'
    this.estado = estado
    this.codigo = codigo
    this.campo = campo
    this.mensaje = mensaje
  }
}

const MENSAJE_RESPUESTA_RARA = 'El servidor respondió algo inesperado. Intenta de nuevo en un momento.'

// Para los datos simulados: arma un ErrorApi con el cuerpo que daría el servidor.
export function crearErrorApi(estado, cuerpo = {}, ruta = '') {
  return new ErrorApi({
    estado,
    codigo: cuerpo.codigo ?? (estado === 400 ? 'VALIDACION' : `HTTP_${estado}`),
    mensaje: cuerpo.mensaje ?? `${MENSAJE_RESPUESTA_RARA} (${estado} ${ruta})`.trim(),
    campo: cuerpo.campo ?? null,
  })
}
