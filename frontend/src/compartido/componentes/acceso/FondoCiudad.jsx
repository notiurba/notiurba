// Ciudad dibujada (sol, edificios y ventanas): solo decoración detrás de los textos azules.
// La usan la Bienvenida y el panel azul de las pantallas de cuenta en computador.
export default function FondoCiudad({ className }) {
  return (
    <svg
      className={className}
      viewBox="0 0 640 800"
      preserveAspectRatio="xMidYMax slice"
      aria-hidden="true"
    >
      <circle cx="470" cy="250" r="110" fill="#FB923C" opacity="0.9" />
      <circle cx="470" cy="250" r="160" fill="#fff" opacity="0.16" />
      <g fill="#04204F" opacity="0.55">
        <rect x="0" y="560" width="70" height="240" />
        <rect x="80" y="500" width="60" height="300" />
        <rect x="150" y="590" width="80" height="210" />
        <rect x="240" y="530" width="56" height="270" />
        <rect x="306" y="600" width="90" height="200" />
        <rect x="406" y="520" width="64" height="280" />
        <rect x="480" y="580" width="72" height="220" />
        <rect x="560" y="540" width="80" height="260" />
      </g>
      <g fill="#FDE68A" opacity="0.85">
        <rect x="20" y="590" width="8" height="10" />
        <rect x="100" y="530" width="8" height="10" />
        <rect x="170" y="620" width="8" height="10" />
        <rect x="258" y="560" width="8" height="10" />
        <rect x="330" y="630" width="8" height="10" />
        <rect x="424" y="550" width="8" height="10" />
        <rect x="500" y="610" width="8" height="10" />
        <rect x="590" y="570" width="8" height="10" />
      </g>
    </svg>
  )
}
