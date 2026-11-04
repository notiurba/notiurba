import { useEffect, useState } from 'react'
import { URL_BASE } from '../../compartido/api/cliente'

const RUTA_HEALTH = '/api/health'
// Render Free tarda ~50 s en despertar: se espera más que eso antes de rendirse
const TIEMPO_MAXIMO_MS = 90_000

export default function EstadoApi() {
  const url = `${URL_BASE}${RUTA_HEALTH}`
  const [estado, setEstado] = useState('cargando')
  const [codigo, setCodigo] = useState(null)
  const [cuerpo, setCuerpo] = useState(null)
  const [fallo, setFallo] = useState(null)

  useEffect(() => {
    const control = new AbortController()
    let desmontado = false
    const temporizador = setTimeout(() => control.abort(), TIEMPO_MAXIMO_MS)

    async function consultar() {
      try {
        const respuesta = await fetch(url, { signal: control.signal })
        const json = await respuesta.json().catch(() => null)
        if (desmontado) return
        setCodigo(respuesta.status)
        // el 503 también trae JSON: se muestra igual que el 200
        setCuerpo(json)
        setEstado(respuesta.ok ? 'ok' : 'error')
      } catch (e) {
        if (desmontado) return
        setFallo(e.name === 'AbortError' ? 'La API no respondió a tiempo.' : e.message)
        setEstado('error')
      } finally {
        clearTimeout(temporizador)
      }
    }
    consultar()

    return () => {
      desmontado = true
      clearTimeout(temporizador)
      control.abort()
    }
  }, [url])

  return (
    <main className="app-loading">
      <section aria-live="polite" style={{ maxWidth: '40rem', padding: '1rem' }}>
        <h1>Estado de la API</h1>
        <p>URL consultada: <code>{url}</code></p>

        {estado === 'cargando' && (
          <p>Consultando… la API en Render Free puede tardar unos 50 segundos en despertar.</p>
        )}

        {estado !== 'cargando' && (
          <>
            <p>Resultado: <strong>{estado === 'ok' ? 'conectado' : 'con error'}</strong></p>
            <p>Código HTTP: <code>{codigo ?? 'sin respuesta'}</code></p>
            {fallo && <p>{fallo}</p>}
            {cuerpo && <pre>{JSON.stringify(cuerpo, null, 2)}</pre>}
          </>
        )}
      </section>
    </main>
  )
}
