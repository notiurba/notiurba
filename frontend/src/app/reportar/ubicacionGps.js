export const TEXTOS_UBICACION = {
  instruccionMapa: 'Toque el mapa para marcar el lugar.',
  etiquetaMapa: 'Mapa para marcar el lugar del problema',
  ayudaTeclado: 'Con teclado, use las flechas para mover el mapa y Enter para marcar el centro.',
  ubicacionOpcional: 'En este tipo de reporte marcar el lugar es opcional.',
  volverUbicar: 'Volver a obtener mi ubicación',
  puntoRequerido: 'Marque el punto donde ocurrió el problema antes de continuar.',
  puntoOBarrio: 'Marque el punto donde ocurrió el problema o seleccione un barrio antes de continuar.',
}

// El botón del paso 2: «Continuar sin ubicación» solo si es opcional y la persona no puso nada.
export const etiquetaContinuarUbicacion = ({ pideUbicacion, hayPunto, hayBarrio, hayDireccion }) =>
  pideUbicacion || hayPunto || hayBarrio || hayDireccion ? 'Continuar' : 'Continuar sin ubicación'

// Se interpreta el código de Geolocation, nunca el texto técnico del navegador.
export function describirErrorGps(error) {
  if (error?.code === 1) return {
    codigo: 'PERMISO_DENEGADO', titulo: 'Permiso de ubicación denegado',
    texto: 'No tenemos permiso para acceder a su ubicación. Puede seleccionar el lugar manualmente en el mapa.',
  }
  if (error?.code === 3) return {
    codigo: 'TIEMPO_AGOTADO', titulo: 'La búsqueda tardó demasiado',
    texto: 'La búsqueda de ubicación tardó demasiado. Inténtelo nuevamente o seleccione el punto en el mapa.',
  }
  return {
    codigo: 'NO_DISPONIBLE', titulo: 'No pudimos obtener su ubicación',
    texto: 'No pudimos obtener su ubicación. Compruebe que la ubicación esté activada o marque el lugar en el mapa.',
  }
}

// Solo se llama por una acción explícita. No altera permisos ni decide cobertura geográfica.
export function obtenerPosicionGps(geolocalizacion) {
  return new Promise((resolver, rechazar) => {
    if (!geolocalizacion?.getCurrentPosition) {
      rechazar(describirErrorGps())
      return
    }
    try {
      geolocalizacion.getCurrentPosition(({ coords }) => {
        const { latitude: latitud, longitude: longitud } = coords
        if (!Number.isFinite(latitud) || !Number.isFinite(longitud) || Math.abs(latitud) > 90 || Math.abs(longitud) > 180) {
          rechazar(describirErrorGps())
          return
        }
        resolver({ latitud, longitud })
      }, error => rechazar(describirErrorGps(error)),
      { enableHighAccuracy: true, timeout: 15000, maximumAge: 0 })
    } catch (error) {
      rechazar(describirErrorGps({ code: error?.name === 'SecurityError' ? 1 : 2 }))
    }
  })
}
