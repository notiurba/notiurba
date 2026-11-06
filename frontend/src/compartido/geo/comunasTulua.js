import comunasTuluaRaw from './comunas-tulua-POT2015-candidato.geojson?raw'

const comunasTuluaGeoJson = JSON.parse(comunasTuluaRaw)

// Centro de Tuluá [latitud, longitud]: donde arrancan los dos mapas.
export const CENTRO_TULUA = [4.0847, -76.1954]

// Procedencia de la capa (POT Tuluá 2015, provisional): docs/decisiones/0003-fuente-cartografica-comunas.md

const expectedCodes = Array.from({ length: 10 }, (_, index) =>
  String(index + 1).padStart(2, '0'),
)
const actualCodes = comunasTuluaGeoJson.features
  .map((feature) => feature.properties.COD_LOC_COM)
  .sort()

if (JSON.stringify(actualCodes) !== JSON.stringify(expectedCodes)) {
  throw new Error('La capa POT provisional debe contener las comunas 01 a 10 sin duplicados.')
}

export const comunasTulua = {
  ...comunasTuluaGeoJson,
  features: [...comunasTuluaGeoJson.features].sort((a, b) =>
    a.properties.COD_LOC_COM.localeCompare(b.properties.COD_LOC_COM),
  ),
}

export const getComunaProperties = (feature) => ({
  id: feature.properties.COD_LOC_COM,
  nombre: feature.properties.NOM_LOC_COM,
  codigoMunicipio: feature.properties.COD_MPIO,
  tipo: feature.properties.Tipo_Localidad_Comuna,
})
