export const opcionesConSeleccion = (opciones, seleccion, predeterminada) =>
  seleccion === predeterminada || opciones.some((opcion) => String(opcion) === String(seleccion))
    ? opciones
    : [...opciones, seleccion]
