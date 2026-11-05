// Atributos de accesibilidad que comparten Campo y Selector: avisan al lector de pantalla
// si el campo tiene error y cuál es el texto que lo explica o lo ayuda.
export function ariaCampo(id, error, ayuda) {
  return {
    'aria-invalid': error ? 'true' : undefined,
    'aria-describedby': error ? `${id}-error` : ayuda ? `${id}-ayuda` : undefined,
  }
}
