export const TEXTOS_UBICACION = {
  antesDelPermiso: 'Podemos utilizar su ubicación para indicar dónde ocurrió el problema. También puede seleccionar el lugar manualmente en el mapa.',
  instruccionMapa: 'Toque el mapa para colocar o cambiar el marcador.',
  ayudaTeclado: 'Con teclado, use las flechas para desplazar el mapa y pulse Enter o Espacio para seleccionar el centro.',
  volverUbicar: 'Volver a obtener mi ubicación',
  puntoRequerido: 'Marque el punto donde ocurrió el problema antes de continuar.',
  puntoOBarrio: 'Marque el punto donde ocurrió el problema o seleccione un barrio antes de continuar.',
}

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
