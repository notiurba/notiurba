import { useEffect, useState } from 'react'
import { Navigate, useLocation } from 'react-router-dom'
import { obtenerRol } from './sesion'
import {
  borrarRetornoReporte, destinoDesdePerfil, prepararRetornoPerfil, prepararRetornoReporte,
} from './retornoReporte'

export function AccesoConRetorno({ children, perfil = false }) {
  const { state } = useLocation()
  const siguiente = state?.siguiente
  useEffect(() => {
    if (perfil) prepararRetornoPerfil(siguiente)
    else prepararRetornoReporte(siguiente)
  }, [perfil, siguiente])
  return children
}

export function RetornoDesdePerfil({ children }) {
  const { pathname } = useLocation()
  const [destino] = useState(() => destinoDesdePerfil(obtenerRol(), pathname))
  useEffect(() => {
    if (destino) borrarRetornoReporte()
  }, [destino])
  return destino ? <Navigate to={destino} replace /> : children
}
