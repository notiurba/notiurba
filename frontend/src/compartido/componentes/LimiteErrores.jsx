import { Component } from 'react'
import Boton from './Boton'
import EstadoVacio from './avisos/EstadoVacio'

// Si algo falla al dibujar el contenido, muestra un aviso con «Reintentar» en vez de dejar la pantalla en blanco.
// Se reinicia cambiando su `key` (por ejemplo, al cambiar de ruta).
export default class LimiteErrores extends Component {
  state = { fallo: false }

  static getDerivedStateFromError() {
    return { fallo: true }
  }

  reintentar = () => this.setState({ fallo: false })

  render() {
    if (!this.state.fallo) return this.props.children

    return (
      <section role="alert">
        <EstadoVacio
          tono="alerta"
          titulo="No pudimos mostrar esta pantalla"
          texto="Ocurrió un problema al dibujarla. Inténtelo de nuevo; si sigue igual, recargue la página."
        >
          <Boton onClick={this.reintentar}>Reintentar</Boton>
        </EstadoVacio>
      </section>
    )
  }
}
