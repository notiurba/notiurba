// Una carga compartida, con tiempo límite y reintento tras un error de red.
export function crearGoogleSdk(ventana, documento, plazo = 15000) {
  let carga
  let cliente
  let receptor
  function cargar() {
    if (ventana.google?.accounts?.id) return Promise.resolve(ventana.google.accounts.id)
    if (carga) return carga
    carga = new Promise((resolve, reject) => {
      const script = documento.createElement('script')
      const terminar = (error) => {
        clearTimeout(reloj)
        script.onload = script.onerror = null
        if (error) { script.remove(); reject(error) }
        else resolve(ventana.google.accounts.id)
      }
      const reloj = setTimeout(() => terminar(new Error('Google no respondió')), plazo)
      script.onload = () => terminar(ventana.google?.accounts?.id ? null : new Error('Google no disponible'))
      script.onerror = () => terminar(new Error('No se pudo cargar Google'))
      script.src = 'https://accounts.google.com/gsi/client'
      script.async = true
      documento.head.appendChild(script)
    }).catch((error) => { carga = undefined; throw error })
    return carga
  }
  function conectar(idCliente, callback) {
    const api = ventana.google.accounts.id
    if (cliente && cliente !== idCliente) throw new Error('Client ID inconsistente')
    receptor = callback
    if (!cliente) {
      api.initialize({ client_id: idCliente, ux_mode: 'popup', callback: (respuesta) => receptor?.(respuesta) })
      cliente = idCliente
    }
    return () => { if (receptor === callback) receptor = undefined }
  }
  return { cargar, conectar }
}

let instancia
export function googleSdk() {
  instancia ??= crearGoogleSdk(window, document)
  return instancia
}
