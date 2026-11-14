// Reglas de navegación que usan varias pantallas.

// ¿Hay una pantalla anterior de esta app a la que volver? Con la ruta abierta directo («default») no la hay.
export const hayPantallaAnterior = (ubicacion) => ubicacion?.key !== 'default'

// Cada pantalla nueva empieza arriba.
export const subirAlInicio = (ventana = globalThis.window) => ventana?.scrollTo?.(0, 0)
