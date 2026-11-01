import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { peticion } from '../../compartido/api/cliente'
import { RUTA_POR_ROL, guardarToken } from '../../compartido/auth/sesion'

// Versión mínima y funcional del ingreso con correo y contraseña; el diseño final es de Figma.
export default function Ingresar() {
  const navegar = useNavigate()
  const [correo, setCorreo] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState(null)
  const [enviando, setEnviando] = useState(false)

  async function enviar(evento) {
    evento.preventDefault()
    setError(null)
    setEnviando(true)
    try {
      const { token, rol } = await peticion('/api/auth/login', {
        method: 'POST',
        body: JSON.stringify({ correo, password }),
      })
      guardarToken(token)
      navegar(RUTA_POR_ROL[rol] ?? '/app', { replace: true })
    } catch (e) {
      setError(e.message)
    } finally {
      setEnviando(false)
    }
  }

  return (
    <main className="app-loading">
      <form onSubmit={enviar} style={{ display: 'grid', gap: '0.75rem', maxWidth: '22rem', padding: '1rem' }}>
        <h1>Ingresar</h1>
        <label>
          Correo
          <input type="email" value={correo} onChange={(e) => setCorreo(e.target.value)}
            autoComplete="email" required />
        </label>
        <label>
          Contraseña
          <input type="password" value={password} onChange={(e) => setPassword(e.target.value)}
            autoComplete="current-password" required />
        </label>
        {error && <p role="alert">{error}</p>}
        <button type="submit" disabled={enviando}>{enviando ? 'Ingresando…' : 'Ingresar'}</button>
      </form>
    </main>
  )
}
