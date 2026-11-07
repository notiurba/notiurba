import { useEffect } from 'react'

// Pone el título de la pestaña del navegador («Crear cuenta · NotiUrba»). Los lectores de pantalla
// lo anuncian al cambiar de pantalla; sin esto todas las pantallas se llamarían igual.
export default function useTituloPagina(titulo) {
  useEffect(() => {
    if (titulo) document.title = titulo.includes('NotiUrba') ? titulo : `${titulo} · NotiUrba`
  }, [titulo])
}
