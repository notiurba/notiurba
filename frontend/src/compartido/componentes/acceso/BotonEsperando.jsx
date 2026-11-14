import { useEffect, useState } from 'react'
import { formatoCuenta } from '../../textos/ingreso'
import Boton from '../Boton'

// «Iniciar sesión» apagado mientras dura el bloqueo, con la cuenta regresiva. Se calcula contra la hora
// real en cada segundo, así no se desfasa si la pestaña estuvo en segundo plano. Al llegar a cero avisa.
export default function BotonEsperando({ fin, alTerminar }) {
  const [ahora, setAhora] = useState(() => Date.now())

  useEffect(() => {
    const reloj = setInterval(() => {
      const momento = Date.now()
      setAhora(momento)
      if (momento >= fin) {
        clearInterval(reloj)
        alTerminar()
      }
    }, 1000)
    return () => clearInterval(reloj)
  }, [fin, alTerminar])

  return (
    <Boton type="submit" disabled>
      Podrá intentarlo en {formatoCuenta((fin - ahora) / 1000)}
    </Boton>
  )
}
