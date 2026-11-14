import { Navigate } from 'react-router-dom'
import Aviso from '../../compartido/componentes/avisos/Aviso'
import Boton from '../../compartido/componentes/Boton'
import EncabezadoPantalla from '../../compartido/componentes/EncabezadoPantalla'
import FormularioPantalla from '../../compartido/componentes/formulario/FormularioPantalla'
import ListaCanales from '../componentes/ListaCanales'
import { accionCanal, enlaceCanal } from '../componentes/estadoCorreo'
import { useReporte } from './useReporte'
import './Reportar.css'

// Atajo para las entidades que no reciben correo (gas, energía y aseo): sus canales oficiales, sin
// pedir ubicación, descripción ni autorización y sin crear ningún reporte.
export default function PasoCanales() {
  const { borrador, categorias } = useReporte()
  const categoria = categorias?.find((c) => String(c.idCategoria) === borrador.idCategoria)

  if (!categorias) return <p className="cargando-texto">Cargando…</p>
  if (!categoria || categoria.permiteCorreo !== false) return <Navigate to="/reportar" replace />

  const primero = categoria.canales?.[0]
  const enlace = enlaceCanal(primero)
  const accion = accionCanal(primero)

  return (
    <>
      <EncabezadoPantalla titulo="Nuevo reporte" atras="/reportar" />
      <FormularioPantalla
        alEnviar={(evento) => evento.preventDefault()}
        acciones={
          <>
            {enlace && (
              <Boton href={enlace} target={enlace.startsWith('https:') ? '_blank' : undefined} rel="noopener noreferrer">
                {accion}
              </Boton>
            )}
            <Boton to="/reportar" variante="secundario">Volver a las categorías</Boton>
          </>
        }
      >
        <h2 className="reportar__titulo">Cómo reportar a {categoria.entidad}</h2>
        <p className="reportar__mensaje">
          {categoria.etiqueta}: use uno de estos canales oficiales. La entidad le responde directamente.
        </p>
        {categoria.canales?.length
          ? <ListaCanales canales={categoria.canales} />
          : <Aviso variante="alerta" titulo="No tenemos canales para mostrar">Busque los canales de atención en la página oficial de {categoria.entidad}.</Aviso>}
        <Aviso titulo="¿Por qué no se envía por correo?">
          {categoria.entidad} no recibe reportes por correo, o no confirma que los recibe. Por eso NotiUrba no guarda
          este reporte: si lo enviáramos, nadie lo atendería.
        </Aviso>
        <p className="reportar__mensaje">Tenga a mano la dirección, lo que pasó y, si la tiene, su matrícula o número de cuenta.</p>
      </FormularioPantalla>
    </>
  )
}
