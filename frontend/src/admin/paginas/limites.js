// Reglas de la pantalla «Configuración» del panel: los mismos rangos que hace respetar el servicio,
// para avisar junto a cada campo antes de guardar.
export const RANGOS = {
  intentosMaximos: [1, 10],
  ventanaMinutos: [1, 60],
  bloqueoMinutos: [1, 60],
  reportesMaximosPorDia: [1, 50],
  ventanaDias: [1, 30],
  metros: [5, 500],
}

const entero = (texto) => (/^\d+$/.test(String(texto ?? '').trim()) ? Number(String(texto).trim()) : null)

function revisar(valor, [minimo, maximo], unidad = '') {
  const numero = entero(valor)
  if (numero === null) return 'Escriba un número entero.'
  if (numero < minimo || numero > maximo) return `Use un valor entre ${minimo} y ${maximo}${unidad}.`
  return ''
}

// Lo que llega del servicio → lo que muestra el formulario (todo como texto, para poder editarlo).
export function aFormulario(limites) {
  return {
    intentosMaximos: String(limites.login.intentosMaximos),
    ventanaMinutos: String(limites.login.ventanaMinutos),
    bloqueoMinutos: String(limites.login.bloqueoMinutos),
    reportesMaximosPorDia: String(limites.reportesMaximosPorDia),
    ventanaDias: String(limites.duplicados.ventanaDias),
    categorias: limites.duplicados.categorias.map((c) => ({ idCategoria: c.idCategoria, nombre: c.nombre, metros: String(c.metros) })),
  }
}

// Errores por campo; la distancia de cada categoría usa la clave del servicio: categorias.<id>.metros.
export function validarLimites(f) {
  const errores = {
    intentosMaximos: revisar(f.intentosMaximos, RANGOS.intentosMaximos),
    ventanaMinutos: revisar(f.ventanaMinutos, RANGOS.ventanaMinutos, ' minutos'),
    bloqueoMinutos: revisar(f.bloqueoMinutos, RANGOS.bloqueoMinutos, ' minutos'),
    reportesMaximosPorDia: revisar(f.reportesMaximosPorDia, RANGOS.reportesMaximosPorDia),
    ventanaDias: revisar(f.ventanaDias, RANGOS.ventanaDias, ' días'),
  }
  for (const c of f.categorias) errores[`categorias.${c.idCategoria}.metros`] = revisar(c.metros, RANGOS.metros, ' metros')
  return Object.fromEntries(Object.entries(errores).filter(([, mensaje]) => mensaje))
}

// Formulario ya validado → cuerpo del PUT.
export function aSolicitud(f) {
  return {
    login: { intentosMaximos: entero(f.intentosMaximos), ventanaMinutos: entero(f.ventanaMinutos), bloqueoMinutos: entero(f.bloqueoMinutos) },
    reportesMaximosPorDia: entero(f.reportesMaximosPorDia),
    duplicados: {
      ventanaDias: entero(f.ventanaDias),
      categorias: f.categorias.map((c) => ({ idCategoria: c.idCategoria, metros: entero(c.metros) })),
    },
  }
}
