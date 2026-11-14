// Después de enviar un formulario, lleva el foco al primer campo con error (si hay alguno).
// Se espera un instante para que React ya haya pintado los errores.
export default function enfocarPrimerError(formulario) {
  setTimeout(() => formulario.querySelector('[aria-invalid="true"]')?.focus(), 0)
}
