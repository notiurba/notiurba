import Aviso from './Aviso'

// Lo que se muestra mientras una pantalla espera sus listas: «Cargando…» o, si falló, un aviso de error.
// Si no hay nada que mostrar (ya cargó y no falló) no dibuja nada.
export default function EstadoCarga({ cargando, fallo, titulo = 'No pudimos cargar las listas' }) {
  if (fallo) {
    return (
      <Aviso variante="error" titulo={titulo}>
        Revise su conexión y vuelva a abrir esta pantalla.
      </Aviso>
    )
  }
  return cargando ? <p className="cargando-texto">Cargando…</p> : null
}
