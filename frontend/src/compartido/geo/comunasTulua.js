import comunasTuluaRaw from './comunas-tulua-POT2015-candidato.geojson?raw'

const comunasTuluaGeoJson = JSON.parse(comunasTuluaRaw)

export const COMUNAS_TULUA_SOURCE = {
  name: 'POT Tulua 2015 - capa provisional',
  url: 'https://www.colombiaot.gov.co/pot/buscador.html?u=76834',
  catalogRecord: 30349,
  originalLayer: '01_P_Comunas_urbanas_2014',
  originalCrs: 'EPSG:3115',
  downloadedAt: '2026-10-11',
  municipalityCode: '76834',
  crs: 'EPSG:4326',
  status: 'pendiente_validacion_administrativa',
  notes:
    'Diez comunas del insumo POT 2015. Referencia provisional; no confirma la division administrativa vigente en 2026.',
}

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
