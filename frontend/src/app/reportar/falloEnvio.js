// Qué hacer cuando falla «Guardar y enviar». Antes de guardar (sin avance) hay tres respuestas que piden
// una decisión de la persona y no un error: reporte parecido, límite del día y foto necesaria.
// Devuelve { parecido, limite, fotoObligatoria, error } (solo una de las cuatro trae algo).
export function decidirFalloEnvio(error, avance) {
  const nada = { parecido: null, limite: null, fotoObligatoria: false, error: '' }
  if (!avance && error?.codigo === 'POSIBLE_DUPLICADO') return { ...nada, parecido: error.datos ?? {} }
  if (!avance && error?.codigo === 'LIMITE_DIARIO_REPORTES') return { ...nada, limite: error.datos ?? {} }
  if (!avance && error?.codigo === 'EVIDENCIA_REQUERIDA') return { ...nada, fotoObligatoria: true }
  if (error?.codigo === 'FUERA_DE_COBERTURA') {
    return { ...nada, error: 'El punto está fuera de la cobertura disponible. Vuelva al paso de ubicación y elija otro lugar o, si este tipo de reporte lo permite, quite el punto.' }
  }
  if (avance) {
    return { ...nada, error: 'Su reporte está guardado. No se completaron las fotos o la solicitud de envío. Reintente sobre este mismo código.' }
  }
  return { ...nada, error: error?.mensaje ?? 'No pudimos guardar su reporte. Sus datos siguen aquí; revise su conexión e inténtelo de nuevo.' }
}
