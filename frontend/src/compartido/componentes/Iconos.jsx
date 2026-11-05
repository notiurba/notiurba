// Íconos del diseño. Todos comparten el mismo envoltorio <svg>, así no se repite en cada uno.
function Icono({ tamano = 22, children }) {
  return (
    <svg
      width={tamano}
      height={tamano}
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="2"
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden="true"
    >
      {children}
    </svg>
  )
}

export const IconoFlecha = (props) => (
  <Icono {...props}>
    <path d="m12 19-7-7 7-7" />
    <path d="M19 12H5" />
  </Icono>
)

export const IconoCorreo = (props) => (
  <Icono {...props}>
    <rect x="3" y="5" width="18" height="14" rx="2" />
    <path d="m3 7 9 6 9-6" />
  </Icono>
)

export const IconoCandado = (props) => (
  <Icono {...props}>
    <rect x="4" y="11" width="16" height="10" rx="2" />
    <path d="M8 11V8a4 4 0 0 1 8 0v3" />
  </Icono>
)

export const IconoOjo = ({ tachado = false, ...props }) => (
  <Icono {...props}>
    <path d="M2 12s3.5-7 10-7 10 7 10 7-3.5 7-10 7S2 12 2 12z" />
    <circle cx="12" cy="12" r="3" />
    {tachado && <path d="m3 3 18 18" />}
  </Icono>
)

export const IconoTelefono = (props) => (
  <Icono {...props}>
    <path d="M22 16.9v3a2 2 0 0 1-2.2 2 19.8 19.8 0 0 1-8.6-3.1 19.5 19.5 0 0 1-6-6A19.8 19.8 0 0 1 2.1 4.2 2 2 0 0 1 4.1 2h3a2 2 0 0 1 2 1.7c.1 1 .4 1.9.7 2.8a2 2 0 0 1-.5 2.1L8.1 9.9a16 16 0 0 0 6 6l1.3-1.3a2 2 0 0 1 2.1-.4c.9.3 1.8.6 2.8.7a2 2 0 0 1 1.7 2z" />
  </Icono>
)

export const IconoChevron = (props) => (
  <Icono {...props}>
    <path d="m6 9 6 6 6-6" />
  </Icono>
)

export const IconoEscudo = (props) => (
  <Icono {...props}>
    <path d="M12 3 5 6v5c0 4.5 3 8.3 7 10 4-1.7 7-5.5 7-10V6l-7-3z" />
  </Icono>
)

export const IconoInfo = (props) => (
  <Icono {...props}>
    <circle cx="12" cy="12" r="9" />
    <path d="M12 11v5" />
    <path d="M12 8h.01" />
  </Icono>
)

export const IconoError = (props) => (
  <Icono {...props}>
    <circle cx="12" cy="12" r="9" />
    <path d="m9 9 6 6" />
    <path d="m15 9-6 6" />
  </Icono>
)

export const IconoCheck = (props) => (
  <Icono {...props}>
    <circle cx="12" cy="12" r="9" />
    <path d="m8 12 3 3 5-6" />
  </Icono>
)

// El logo de Google va con sus cuatro colores de marca, por eso no usa el envoltorio.
export const IconoGoogle = ({ tamano = 22 }) => (
  <svg width={tamano} height={tamano} viewBox="0 0 24 24" aria-hidden="true">
    <path fill="#4285F4" d="M22.6 12.2c0-.8-.1-1.5-.2-2.2H12v4.2h5.9a5 5 0 0 1-2.2 3.3v2.7h3.5c2.1-1.9 3.4-4.7 3.4-8z" />
    <path fill="#34A853" d="M12 23c3 0 5.5-1 7.3-2.7l-3.5-2.7c-1 .7-2.2 1.1-3.8 1.1-2.9 0-5.4-2-6.3-4.6H2.1v2.8A11 11 0 0 0 12 23z" />
    <path fill="#FBBC05" d="M5.7 14.1a6.6 6.6 0 0 1 0-4.2V7.1H2.1a11 11 0 0 0 0 9.8l3.6-2.8z" />
    <path fill="#EA4335" d="M12 5.4c1.6 0 3.1.6 4.2 1.7l3.1-3.1A11 11 0 0 0 2.1 7.1l3.6 2.8C6.6 7.4 9.1 5.4 12 5.4z" />
  </svg>
)

export const IconoAlerta = (props) => (
  <Icono {...props}>
    <path d="M10.3 3.9 2.4 17.5A2 2 0 0 0 4.1 20.5h15.8a2 2 0 0 0 1.7-3L13.7 3.9a2 2 0 0 0-3.4 0z" />
    <path d="M12 9v4" />
    <path d="M12 17h.01" />
  </Icono>
)

// ---- Íconos del sistema de diseño usados por la PWA ciudadana ----
export const IconoChevronDerecha = (props) => (
  <Icono {...props}>
    <path d="m9 6 6 6-6 6" />
  </Icono>
)
export const IconoMas = (props) => (
  <Icono {...props}>
    <path d="M12 5v14M5 12h14" />
  </Icono>
)
export const IconoX = (props) => (
  <Icono {...props}>
    <path d="M18 6 6 18M6 6l12 12" />
  </Icono>
)
export const IconoReloj = (props) => (
  <Icono {...props}>
    <circle cx="12" cy="12" r="9" />
    <path d="M12 7v5l3 2" />
  </Icono>
)
export const IconoEnviar = (props) => (
  <Icono {...props}>
    <path d="m22 2-7 20-4-9-9-4 20-7z" />
    <path d="M22 2 11 13" />
  </Icono>
)
export const IconoBuscar = (props) => (
  <Icono {...props}>
    <circle cx="11" cy="11" r="7" />
    <path d="m20 20-3.5-3.5" />
  </Icono>
)
export const IconoDocumento = (props) => (
  <Icono {...props}>
    <path d="M14 3H7a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h10a2 2 0 0 0 2-2V8l-5-5z" />
    <path d="M14 3v5h5M9 13h6M9 17h6" />
  </Icono>
)
export const IconoAjustes = (props) => (
  <Icono {...props}>
    <circle cx="12" cy="12" r="3" />
    <path d="M19.4 15a1.7 1.7 0 0 0 .3 1.8l.1.1a2 2 0 1 1-2.8 2.8l-.1-.1a1.7 1.7 0 0 0-1.8-.3 1.7 1.7 0 0 0-1 1.5V21a2 2 0 1 1-4 0v-.1a1.7 1.7 0 0 0-1.1-1.5 1.7 1.7 0 0 0-1.8.3l-.1.1a2 2 0 1 1-2.8-2.8l.1-.1a1.7 1.7 0 0 0 .3-1.8 1.7 1.7 0 0 0-1.5-1H3a2 2 0 1 1 0-4h.1a1.7 1.7 0 0 0 1.5-1.1 1.7 1.7 0 0 0-.3-1.8l-.1-.1a2 2 0 1 1 2.8-2.8l.1.1a1.7 1.7 0 0 0 1.8.3H9a1.7 1.7 0 0 0 1-1.5V3a2 2 0 1 1 4 0v.1a1.7 1.7 0 0 0 1 1.5 1.7 1.7 0 0 0 1.8-.3l.1-.1a2 2 0 1 1 2.8 2.8l-.1.1a1.7 1.7 0 0 0-.3 1.8V9a1.7 1.7 0 0 0 1.5 1H21a2 2 0 1 1 0 4h-.1a1.7 1.7 0 0 0-1.5 1z" />
  </Icono>
)
export const IconoCopiar = (props) => (
  <Icono {...props}>
    <rect x="9" y="9" width="11" height="11" rx="2" />
    <path d="M5 15V6a2 2 0 0 1 2-2h9" />
  </Icono>
)
export const IconoLapiz = (props) => (
  <Icono {...props}>
    <path d="M12 20h9" />
    <path d="M16.5 3.5a2.1 2.1 0 0 1 3 3L7 19l-4 1 1-4L16.5 3.5z" />
  </Icono>
)
export const IconoPapelera = (props) => (
  <Icono {...props}>
    <path d="M3 6h18M8 6V4h8v2M6 6l1 14h10l1-14M10 11v5M14 11v5" />
  </Icono>
)
export const IconoCamara = (props) => (
  <Icono {...props}>
    <path d="M3 8a2 2 0 0 1 2-2h2l1.5-2h7L17 6h2a2 2 0 0 1 2 2v10a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V8z" />
    <circle cx="12" cy="13" r="3.5" />
  </Icono>
)
export const IconoImagen = (props) => (
  <Icono {...props}>
    <rect x="3" y="4" width="18" height="16" rx="2" />
    <circle cx="9" cy="10" r="1.5" />
    <path d="m21 16-5-5-9 9" />
  </Icono>
)
export const IconoImagenError = (props) => (
  <Icono {...props}>
    <rect x="3" y="4" width="18" height="16" rx="2" />
    <path d="m3 3 18 18M21 16l-5-5" />
  </Icono>
)
export const IconoUbicacion = (props) => (
  <Icono {...props}>
    <circle cx="12" cy="12" r="3" />
    <circle cx="12" cy="12" r="8" />
    <path d="M12 2v3M12 19v3M2 12h3M19 12h3" />
  </Icono>
)
export const IconoPin = (props) => (
  <Icono {...props}>
    <path d="M20 10c0 6-8 12-8 12s-8-6-8-12a8 8 0 0 1 16 0Z" />
    <circle cx="12" cy="10" r="3" />
  </Icono>
)
export const IconoSinUbicacion = (props) => (
  <Icono {...props}>
    <path d="M12 21s-6-5.5-6-11a6 6 0 0 1 12 0c0 5.5-6 11-6 11z" />
    <path d="m3 3 18 18" />
  </Icono>
)
export const IconoOrdenar = (props) => (
  <Icono {...props}>
    <path d="M7 4v16M3 8l4-4 4 4M17 20V4M13 16l4 4 4-4" />
  </Icono>
)
export const IconoUsuario = (props) => (
  <Icono {...props}>
    <circle cx="12" cy="8" r="4" />
    <path d="M4 21a8 8 0 0 1 16 0" />
  </Icono>
)
export const IconoUsuarioX = (props) => (
  <Icono {...props}>
    <circle cx="10" cy="8" r="4" />
    <path d="M2 21a8 8 0 0 1 12-6.9M17 15l5 5M22 15l-5 5" />
  </Icono>
)
export const IconoSalir = (props) => (
  <Icono {...props}>
    <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4M16 17l5-5-5-5M21 12H9" />
  </Icono>
)
export const IconoSinConexion = (props) => (
  <Icono {...props}>
    <path d="M5 12.5a10 10 0 0 1 4-2.3M2 9a15 15 0 0 1 5-3M19 12.5a10 10 0 0 0-2-1.4M22 9a15 15 0 0 0-8-4M8.5 16a5 5 0 0 1 7 0M12 20h.01M3 3l18 18" />
  </Icono>
)

// Íconos de categoría (los cuadros de color de «¿Qué problema desea reportar?»)
export const IconoVia = (props) => (
  <Icono {...props}>
    <path d="M8 3 4 21M16 3l4 18M12 4v3M12 11v3M12 18v3" />
  </Icono>
)
export const IconoBombillo = (props) => (
  <Icono {...props}>
    <path d="M9 18h6M10 21h4M12 3a6 6 0 0 0-4 10.5c.7.7 1 1.5 1 2.5h6c0-1 .3-1.8 1-2.5A6 6 0 0 0 12 3z" />
  </Icono>
)
export const IconoArbol = (props) => (
  <Icono {...props}>
    <path d="M12 22v-6M12 3 6 11h3l-4 6h14l-4-6h3l-6-8z" />
  </Icono>
)
export const IconoSemaforo = (props) => (
  <Icono {...props}>
    <rect x="8" y="2" width="8" height="20" rx="3" />
    <path d="M12 6h.01M12 12h.01M12 18h.01" />
  </Icono>
)
export const IconoGota = (props) => (
  <Icono {...props}>
    <path d="M12 3s6 6.5 6 11a6 6 0 0 1-12 0c0-4.5 6-11 6-11z" />
  </Icono>
)
export const IconoRayo = (props) => (
  <Icono {...props}>
    <path d="M13 2 4 14h7l-1 8 9-12h-7l1-8z" />
  </Icono>
)
export const IconoLlama = (props) => (
  <Icono {...props}>
    <path d="M12 22a6 6 0 0 0 6-6c0-4-3-5-3-9-3 2-4 4-4 6-1-1-2-2-2-4-2 2-3 4-3 7a6 6 0 0 0 6 6z" />
  </Icono>
)
export const IconoCasa = (props) => (
  <Icono {...props}>
    <path d="m3 11 9-8 9 8M5 10v10h14V10M10 20v-6h4v6" />
  </Icono>
)
export const IconoGorro = (props) => (
  <Icono {...props}>
    <path d="m2 9 10-5 10 5-10 5L2 9zM6 11.5V16c0 1.5 2.7 3 6 3s6-1.5 6-3v-4.5M22 9v6" />
  </Icono>
)
export const IconoPuntos = (props) => (
  <Icono {...props}>
    <path d="M5 12h.01M12 12h.01M19 12h.01" />
  </Icono>
)
export const IconoCuadricula = (props) => (
  <Icono {...props}>
    <rect x="4" y="4" width="6" height="6" rx="1" />
    <rect x="14" y="4" width="6" height="6" rx="1" />
    <rect x="4" y="14" width="6" height="6" rx="1" />
    <rect x="14" y="14" width="6" height="6" rx="1" />
  </Icono>
)
