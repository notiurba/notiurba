import './Separador.css'

// Línea con un texto en medio: «O ingrese con», «O complete sus datos».
export default function Separador({ texto }) {
  return (
    <div className="separador">
      <span />
      {texto}
      <span />
    </div>
  )
}
