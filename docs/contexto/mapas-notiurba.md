# Modulo de mapas NOTIURBA

## Capa activa: POT 2015 provisional (2026-10-11)

El mapa utiliza `src/data/geo/comunas-tulua-POT2015-candidato.geojson`, con diez comunas y codigos de texto `01` a `10`. Es una referencia provisional del POT 2015, pendiente de validacion administrativa; NO confirma la division vigente en 2026. Los apartados de investigacion que siguen registran etapas anteriores, no el estado actual de la capa activa.

El candidato mencionado no estaba en el proyecto ni en Descargas al revisarlos. Se genero directamente desde el ZIP proporcionado por el usuario `Consolidado cartografia formulacion (SHP) EOT2015.zip`, sin digitalizar el PDF ni modificar vertices originales en el CRS de origen:

- Catalogo Colombia OT: https://www.colombiaot.gov.co/pot/buscador.html?u=76834 , registro 30349, atribuido a la Alcaldia de Tulua, instrumento POT2015, publicacion catalogada 18-12-2015. El titulo contiene EOT2015; esa discrepancia se conserva documentada.
- Capa original: `9_Norma/01_P_Comunas_urbanas_2014.shp`, con SHX, DBF, PRJ y XML. Atributos originales: `OBJECTID`, `Comuna`, `C_Abrev`, `Shape_Leng`, `Shape_Area`. Sus registros identifican expresamente Comuna 1 a Comuna 10.
- El PRJ define `MAGNA_Colombia_Oeste`, meridiano -77.07750791666666, factor 1 y falsos este/norte 1000000: EPSG:3115. Se leyo el WKT real; no se utilizo la referencia contradictoria del GeoPDF.
- Conversion con PyProj: EPSG:3115 a EPSG:4326, orden longitud/latitud, sin transformaciones ballpark. Operacion seleccionada: inversa de Transverse Mercator mas MAGNA-SIRGAS to WGS 84 (1); exactitud declarada de la operacion 1 m. Esto NO representa la precision de los limites, que no ha sido certificada.
- El XML vincula la capa con la entrega municipal de agosto de 2015 y registra copia/creacion 02-10-2015, ademas de procesos de 2014. No usar fechas de sincronizacion heredadas del XML como prueba de vigencia administrativa.
- Propiedades compatibles agregadas: `COD_MPIO=76834`, `COD_LOC_COM=01..10`, `NOM_LOC_COM=Comuna N`, `Tipo_Localidad_Comuna=Comuna`. Se preservaron atributos originales y se agregaron fuente y condicion provisional. El codigo municipal se normalizo a partir de la procedencia del insumo, no existia en el DBF.
- Se utilizaron PyShp, PyProj y Shapely solo en temporales para la conversion y validacion. No se agregaron dependencias al frontend ni se utilizo QGIS.

Resultados de validacion geometrica:

- Diez geometrías no vacias y validas antes y despues de transformar; codigos unicos 01 a 10.
- Comuna 10: extension WGS84 longitud -76.20167295 a -76.18498037, latitud 4.09813146 a 4.12134567. Queda al norte de Tulua, en el sector Aguaclara representado en P-28. Punto interior calculado desde su geometria: latitud 4.10975003647537, longitud -76.19584751183994; la utilidad lo identifica como comuna 10.
- `Shape_Area` y `Shape_Leng` del registro 10 valen cero en el DBF, aunque su geometria es valida y no vacia. Esos atributos estan desactualizados y no se usan para validar su extension.
- Intersecciones de area mayores a 0,01 m2, medidas en EPSG:3115: 01/02 = 67,88 m2; 03/10 = 22,79 m2; 09/10 = 26,09 m2; 04/06 = 4,87 m2; 05/06 = 10,15 m2. La union presenta 27 anillos interiores (huecos); no se ha determinado su causa administrativa. No se repararon ni recortaron automaticamente.
- Comparacion con DANE 2018, proporcion interseccion/union por codigo: 01=0,6798; 02=0,5400; 03=0,5713; 04=0,9301; 05=0,8663; 06=0,6480; 07=0,5885; 08=0,8748; 09=0,8761. Comuna 10 no existe en el insumo DANE. Son diferencias de cobertura, no mediciones de exactitud ni evidencia de que una fuente sea administrativamente vigente.
- DANE preservado byte por byte: SHA-256 `62DC7CA98D53FF0CD8BE77143CC56E32D6BFD26533E599A35B1C42D7C40AACCD`.
- Candidato generado: SHA-256 `B2F9F982C8D75BE60B1CD6BF2B4EAFCBC06D99025E39C3B0B69F870655615BC6`.

Asignacion de reportes y estados:

- Distribucion de demostracion actualizada: NTU-001..009 pertenecen exclusivamente a 01..09, y NTU-011 a 10. Sus coordenadas ficticias son puntos interiores representativos de la cobertura exclusiva de cada poligono POT, calculados con Shapely (`difference` solo en memoria para seleccionar puntos, seguido de `representative_point`, no centroides). No se alteraron los poligonos originales. `comunaEsperada` es una expectativa de prueba; la asignacion mostrada se calcula de manera independiente mediante punto-en-poligono.
- NTU-010 fue creado intencionalmente como caso exterior (su descripcion lo indica). Se conservan intactos identificador, coordenadas [4.126, -76.236] y demas atributos en `reportesFueraCobertura`. Su resultado es `sin_determinar`, comuna null; no implica estar fuera del municipio. El servicio mantiene su importacion de `reportesPrueba`, que ahora contiene exclusivamente los diez reportes de demostracion. El caso exterior solo participa en pruebas automatizadas.
- Un punto con multiples coincidencias o sobre cualquier borde devuelve `status=pendiente_revision`, `comuna=null` y la lista de `comunasCandidatas`. El popup muestra Pendiente de revision, nunca la primera comuna arbitrariamente.
- El diagnostico posterior reemplaza `fuera_zona_urbana` por `sin_determinar`: la interfaz informa Comuna sin determinar, sin inferir que el punto esta fuera de Tulua. Ver [diagnostico de limites](diagnostico-limites-notiurba.md) para el inventario actualizado, areas y comparacion visual P-28.
- Los popups de asignaciones interiores corresponden a la capa provisional. La interfaz identifica expresamente POT 2015 y su validacion pendiente. Comuna 10 tiene un color propio.

Comprobaciones: `node --test src/utils/geoUtils.test.mjs` prueba interiores, exteriores, huecos, limites compartidos, solapamientos, el punto interior de comuna 10, un solapamiento real 03/10 y los reportes ficticios. `npm run lint` y `npm run build` pasan. La comprobacion en React-Leaflet mostro diez poligonos y diez marcadores; popup Comuna 10 con codigo 10, filtros por Basuras/En revision, calor y controles de capas funcionan sin errores de consola.

Pendiente: contrastar limites y denominaciones con Planeacion Municipal, confirmar su equivalencia administrativa y vigencia, revisar solapamientos/huecos y definir precision aceptable para puntos cercanos a limites. La fuente completa se usa provisionalmente sin mezclar una comuna POT con nueve DANE.

## Herramientas utilizadas

- React 19 y Vite.
- Leaflet 1.9.4 para el motor de mapas.
- React-Leaflet 5.0.0 para integrar Leaflet con React.
- OpenStreetMap como mapa base, conservando atribucion.
- leaflet.heat 0.2.0 para la capa de concentracion espacial.

## Datos geograficos

Fuente vectorial usada:

- DANE, Geoportal DANE, servicio `Comuna_Localidad/Serv_Comuna_Localidad_2018`.
- Capa: `Comuna y Localidad`.
- URL: https://geoportal.dane.gov.co/mparcgis/rest/services/Comuna_Localidad/Serv_Comuna_Localidad_2018/FeatureServer/0
- Filtro aplicado: `COD_MPIO='76834'`.
- Salida solicitada: `f=geojson&outSR=4326`.
- Archivo guardado: `src/data/geo/comunas-tulua-dane-2018.geojson`.

Validacion realizada:

- El GeoJSON contiene 9 entidades con `COD_MPIO=76834`.
- Todas tienen `Tipo_Localidad_Comuna=Comuna`.
- Las coordenadas descargadas estan en longitud/latitud compatibles con Leaflet (EPSG:4326).
- La extension de coordenadas queda aproximadamente entre longitud -76.2259 y -76.1707, latitud 4.0582 y 4.1113.

Limitaciones:

- El servicio DANE entrega nombres genericos (`Comuna 1` a `Comuna 9`) y no incluye nombres barriales.
- No se encontraron, durante esta etapa, nombres oficiales enriquecidos por comuna dentro del mismo GeoJSON.
- Si la Alcaldia de Tulua publica una capa POT o SIG municipal mas reciente, debe compararse contra este insumo DANE antes de adoptarla como fuente definitiva.

### Investigacion de la comuna 10 (2026-10-11)

Resultado de la primera busqueda: no se localizo una geometria vectorial oficial verificable de la comuna 10 en las fuentes publicas consultadas. La segunda revision, documentada a continuacion, encontro un recurso SHP en Colombia OT pendiente de descarga e inspeccion. La capa DANE conservada en el proyecto tiene cobertura incompleta respecto de las diez comunas reconocidas en documentos municipales.

Fuentes y evidencia:

- Alcaldia, Plan Municipal de Gestion del Riesgo, version 3, diciembre de 2020: reconoce diez comunas; la tabla de division politico-administrativa, pagina 21, identifica la comuna 10 con San Francisco, Urbanizacion La Paz, El Paraiso y Aguaclara, con fuente DAPM Tulua. Documento oficial: https://tulua.gov.co/loader.php?lServicio=Tools2&lTipo=descargas&lFuncion=exposeDocument&idFile=12112 . La referencia textual fue consultada en el indice publico; se comprobo que el enlace entrega un PDF. No proporciona por si misma una geometria importable.
- Alcaldia, noticia del 12 de noviembre de 2023 sobre el coliseo de Aguaclara: identifica expresamente ese sector como comuna 10. https://tulua.gov.co/publicaciones/800/el-corregimiento-de-aguaclara-ya-cuenta-con-coliseo-cubierto-gracias-a-la-gestion-del-alcalde-john-jairo-gomez-aguirre-alcaldia-de-tulua/ . Confirma su reconocimiento, no sus limites.
- POT municipal: https://tulua.gov.co/publicaciones/5778/plan-de-ordenamiento-territorial-del-municipio-de-tulua/ . El catalogo de cartografia https://tulua.gov.co/publicaciones/146/cartografia-alcaldia-de-tulua/ enlaza el plano `P-28_Fichas_normativas_de_comunas_(FCN)`, archivo 1600. Se comprobo la descarga como PDF (5.350.857 bytes): https://tulua.gov.co/loader.php?lServicio=Tools2&lTipo=descargas&lFuncion=exposeDocument&idFile=1600 . No se verificaron su georreferenciacion interna ni la equivalencia entre fichas normativas y limites administrativos; no se extrajeron coordenadas.
- Correcciones cartograficas del POT: https://tulua.gov.co/publicaciones/149/correcciones-cartograficas-alcaldia-de-tulua/ . Se revisaron los enlaces del catalogo; no se identifico una descarga vectorial de la comuna 10.
- SIG municipal: https://tulua.gov.co/documentos/256/sistema-de-informacion-geografica-sig/ . El listado consultado ofrece `Instructivo-Tatuk-actualizado.pdf`; no se localizo en el listado una capa descargable de limites de comunas.
- CVC, informe tecnico de arbolado de Tulua: https://ecopedia.cvc.gov.co/sites/default/files/archivosAdjuntos/Informe%20t%C3%A9cnico%20tulu%C3%A1.pdf . El indice publico muestra un mapa con las diez comunas; la apertura del PDF fallo durante esta consulta (502/descarga sin completar), por lo que no se verifico su referencia espacial ni se extrajo geometria.
- CVC, servicio de arbolado urbano: https://geo.cvc.gov.co/arcgis/rest/services/GESTION_AMBIENTAL/Arbolado_Urbano/MapServer/layers . Los metadatos publicos describen puntos de arboles con `CD_COMUNA`; estos puntos no son limites administrativos. El acceso directo al catalogo CVC fallo por resolucion de DNS, lo cual limita la revision de sus servicios.
- SIG departamental: https://infraestructura.valledelcauca.gov.co/server/rest/services/Tulu%C3%A1/MapServer . Se consultaron sus diez capas: ejes, sitios criticos, superficie, estado, drenaje, muros, PRS, fotoejes, puentes y tuneles. No contiene una capa de comunas.

Decision y validacion:

- No se agrego una entidad vacia ni un poligono aproximado de la comuna 10. No se modifico el GeoJSON ni se crearon carpetas adicionales.
- Archivo existente: 9 entidades `Polygon`, codigos `01` a `09`. SHA-256 de referencia: `62DC7CA98D53FF0CD8BE77143CC56E32D6BFD26533E599A35B1C42D7C40AACCD`.
- La comprobacion de solapamientos de la comuna 10 queda pendiente: sin una geometria candidata no es posible calcular intersecciones con las otras nueve.
- Estar fuera de estos nueve poligonos significa estar fuera de la cobertura cargada; no demuestra estar fuera de la zona urbana ni descarta pertenecer a la comuna 10.
- Los mapas PDF requieren georreferenciacion (o comprobacion de la referencia espacial interna, si existe), digitalizacion y validacion tecnica y administrativa antes de usarse como geometria definitiva. Un contorno de una ficha normativa no debe asumirse equivalente a una comuna sin confirmacion.

Insumo pendiente: solicitar a Planeacion Municipal / SIG de Tulua el poligono oficial de la comuna 10 o la capa completa de comunas en SHP, GeoPackage, GeoJSON o servicio WFS/FeatureServer, con sistema de referencia, escala, fecha, metadatos y acto administrativo que respalda los limites. Si solo entregan planos, solicitar tambien los archivos vectoriales originales del POT. No se envio ninguna solicitud externa durante esta investigacion.

Cuando exista el insumo, comprobar su validez geometrica y procedencia, transformar a EPSG:4326 con la transformacion de datum adecuada y medir las intersecciones con cada poligono DANE en un CRS proyectado apropiado. Distinguir limites compartidos de solapamientos de area; documentar las discrepancias antes de integrar o justificar cualquier cambio en las nueve comunas existentes.

### Segunda revision: Colombia OT y preparacion para QGIS

Colombia OT: https://www.colombiaot.gov.co/pot/buscador.html?u=76834 . Se consulto el catalogo municipal con `codigo=76834`; entrego 20 registros. Se verifico tambien el recurso en la interfaz del navegador:

- Registro `30349`, nombre literal `Consolidado cartografia formulacion (SHP) EOT2015`, instrumento `POT2015`, formato `.zip`, responsable `Alcaldia de Tulua`, publicacion `2015-12-18`, codigo `76834`.
- El nombre del recurso contiene `EOT2015` mientras el instrumento indica `POT2015`: esta discrepancia del catalogo debe contrastarse con el contenido del ZIP. El titulo no demuestra que contenga una capa de comunas.
- El boton `Descargar` exige iniciar sesion. La solicitud sin sesion devolvio `status: false`. No se descargaron ni inspeccionaron los shapefiles; no se afirma todavia que el recurso contenga la comuna 10.
- El servicio DANE se consulto nuevamente: conserva los codigos `01` a `09`, sin `10`.

QGIS no se detecto en Program Files, Program Files (x86), programas locales del usuario, OSGeo4W, PATH o registros de desinstalacion habituales. Esto no descarta una copia portable en otra ruta. No se utilizo QGIS ni se instalo. Si existe una copia portable, debe indicarse su ejecutable; de otro modo hay que instalar QGIS desde https://qgis.org/download/ para ejecutar el procedimiento manual.

Insumos conservados en la carpeta geografica existente:

- `src/data/geo/plano-p28-tulua-2015.pdf`: PDF municipal original, una pagina, 5.350.857 bytes. SHA-256 `6FA6C48DDF6573A7C3323B6417D6C9B00CA1D08F98F7A1FA6F9E98F7897DB4AB`.
- `src/data/geo/plano-p28-tulua-2015-300dpi.png`: pagina completa renderizada a 300 ppp, 8269 x 11813 pixeles, sin recorte ni georreferenciacion asignada. Conserva mapa, cuadricula, escala y cajetin. El PDF contiene trazados vectoriales, por lo que no tiene una resolucion raster unica; 300 ppp es la resolucion elegida para el trabajo manual, no una precision medida.
- PyMuPDF 1.28.2 se instalo unicamente en una carpeta temporal para leer los objetos PDF y renderizar la pagina. No se agregaron dependencias a React ni archivos de soporte al frontend.

Inspeccion visual y estructural del P-28:

- El plano muestra `C 10` en el sector rotulado `Aguaclara`; la leyenda identifica `Comuna 10: Aguaclara`.
- Titulo real: `Fichas normativas de planificacion (FCP)`. La leyenda identifica delimitacion de fichas normativas de planificacion, no certifica por si sola que el contorno coincida con el limite administrativo vigente.
- Fecha cartografica impresa: agosto de 2015. Escala de ploteo: `1:10.000`. Escala de trabajo: `Ver anexo`. La precision del levantamiento no se conoce sin ese anexo. El cajetin deja sin completar el numero de acuerdo; no constituye prueba suficiente del acto de adopcion.
- Creacion PDF: ArcMap 10.3.1, `2017-01-25`. No confundir fecha de exportacion PDF con vigencia de los limites.
- Cuadricula impresa: estes entre 1095000 y 1101000, nortes entre 940000 y 948000, en metros.
- Cajetin: `Magna_Colombia_Oeste`, Transverse Mercator, meridiano central `-77.07750792`, latitud de origen `4.59620042`, falsos este/norte 1000000. Estos parametros corresponden a MAGNA-SIRGAS / Colombia West zone, EPSG:3115; referencia institucional: https://www.cali.gov.co/planeacion/publicaciones/105289/proyecciones-transformaciones-cartograficas-idesc/ . Comprobar tambien el factor de escala antes de adoptarlo.
- El PDF SI contiene `/VP`, `/Measure`, `/GPTS`, `/LPTS` y `/GCS`. Sin embargo, el WKT de `/GCS` declara `GAUSS_BTA_MAGNA` y meridiano central `-74.077507917`, diferente del cajetin. Las esquinas `/GPTS` son coordenadas cercanas a Tulua, pero no se ha medido su concordancia con la cuadricula impresa. No utilizar automaticamente esa referencia interna.
- No se midieron residuos, RMSE ni error independiente de georreferenciacion. No se digitalizo ningun poligono ni se realizaron pruebas de solapamiento de la comuna 10.

Procedimiento manual preparado (pendiente de ejecucion):

1. Abrir Inicio de Windows, buscar `QGIS Desktop` y abrirlo si esta instalado. Crear proyecto con `Proyecto > Nuevo`. Si no aparece, la aplicacion no esta disponible por ese acceso; no asumir que el georreferenciador se ejecuto.
2. Priorizar el ZIP SHP: en Colombia OT, seleccionar Tulua y localizar el registro 30349; iniciar sesion y pulsar `Descargar`. Abrir el archivo y revisar capas de comunas/FCP, atributos y `.prj`. Si contiene limites administrativos originales verificables, usar esos datos antes de digitalizar el PNG. La clasificacion debe basarse en metadatos y contenido, no solo en nombres de archivo.
3. Para el respaldo PDF, en QGIS ir a `Capa > Georreferenciador` (los nombres pueden variar segun version/idioma), pulsar `Abrir raster` y seleccionar `plano-p28-tulua-2015-300dpi.png`. No asignar EPSG:4326 al PNG ni confiar en el CRS interno del PDF.
4. Pulsar `Anadir punto` y seleccionar cruces EXACTOS de la cuadricula impresa. Introducir X=Este y Y=Norte, usando EPSG:3115 solo despues de contrastar los parametros del cajetin. Cruces de control propuestos, cuyas etiquetas se leen en el plano: (1096000,947000), (1100000,947000), (1096000,941000), (1100000,941000), (1098000,944000) y (1098000,946000). Estos son valores de la cuadricula, no coordenadas inventadas de vertices; todavia no se han seleccionado sus pixeles ni validado el ajuste. Conservar cruces adicionales como puntos independientes de comprobacion, especialmente alrededor de Aguaclara.
5. En `Configuracion > Configuracion de transformacion`, seleccionar `Polinomial 1` como primera prueba para esta pagina digital, remuestreo `Vecino mas proximo`, CRS de destino EPSG:3115, salida GeoTIFF, `Guardar puntos de control`, informe PDF y `Cargar en QGIS cuando termine`. No usar TPS para forzar concordancia. Guardar GCP y resultados junto a los insumos existentes, solo al ejecutar el proceso.
6. Pulsar `Iniciar georreferenciacion`. Registrar numero y distribucion de GCP, residuos por punto, RMSE, unidades y maximo error. Evaluar puntos independientes: un RMSE bajo en los puntos utilizados no demuestra exactitud del limite. Contrastar con cartografia oficial de referencia, no ajustar el plano para hacerlo coincidir obligatoriamente con los nueve poligonos DANE.
7. Cargar el GeoJSON DANE desde `Capa > Anadir capa > Anadir capa vectorial`. Revisar coincidencia de rios, vias y cruces de la cuadricula. Resolver con Planeacion la discrepancia de CRS y confirmar que el contorno FCP 10 es aplicable al limite administrativo de la comuna 10. Detenerse si no se puede resolver o si el limite es ilegible.
8. Solo tras superar esas verificaciones: `Capa > Crear capa > Nueva capa GeoPackage`, geometria poligono, CRS EPSG:3115. Crear campos de texto `COD_MPIO`, `COD_LOC_COM`, `NOM_LOC_COM`, `Tipo_Localidad_Comuna` con valores `76834`, `10`, `Comuna 10`, `Comuna`. Mantener `COD_LOC_COM` como texto para compatibilidad con el frontend. Registrar procedencia, fecha y condicion de digitalizacion derivada en metadatos.
9. Activar el lapiz `Conmutar edicion`, pulsar `Anadir poligono`, recorrer el limite visible de C 10 y terminar con clic derecho. No deducir limites por barrios ni recortar automaticamente contra DANE. Guardar y desactivar edicion.
10. En `Procesos > Caja de herramientas`, ejecutar `Comprobar validez` y `Interseccion` con cada comuna DANE, trabajando en un CRS metrico apropiado. Medir area de cada solapamiento y revisar limites compartidos, huecos y diferencias entre fechas/fuentes; no corregir automaticamente los nueve originales.
11. Clic derecho sobre la capa > `Exportar > Guardar objetos como`, formato `GeoJSON`, CRS EPSG:4326. Documentar la operacion de transformacion usada. Mantener el original DANE y crear un archivo combinado separado solo cuando la candidata se acepte: diez codigos unicos y geometria valida. Marcarlo como capa mixta/provisional, pendiente de contraste municipal, si incorpora una digitalizacion derivada.
12. Integrar el combinado en `comunasTulua.js` y probar dibujo, zoom, popup de Comuna 10, asignacion de puntos interiores/exteriores/de borde, filtros y mapa de calor. Estos pasos de integracion siguen pendientes y no se han ejecutado.

Referencia del procedimiento: https://docs.qgis.org/3.40/en/docs/user_manual/managing_data_source/georeferencer.html . El plano no se considera georreferenciado correctamente por el mero hecho de contener metadatos GeoPDF. A escala nominal 1:10.000, 300 ppp representa aproximadamente 0,85 m por pixel en papel sin reescalado; esto NO es la exactitud cartografica ni el error del poligono.

## Funcionalidades completas

- Mapa base centrado inicialmente en Tulua `[4.0847, -76.1954]`.
- Zoom, desplazamiento y controles Leaflet visibles.
- Capa de comunas GeoJSON con colores, popup y resaltado al pasar el cursor.
- Ajuste automatico del mapa a la extension de las comunas cargadas.
- Marcadores de reportes con popup.
- Controles para mostrar/ocultar comunas, marcadores y mapa de calor.
- Filtros por categoria y estado.
- Identificacion de comuna por punto usando los poligonos cargados.
- Manejo de puntos fuera de la zona urbana y puntos sobre limites.
- Capa de calor con `leaflet.heat`, configurable y sincronizada con reportes filtrados.

## Datos ficticios

Los reportes en `src/data/reportes/reportesPrueba.js` son datos de prueba y no provienen del backend ni de reportes reales. Incluyen:

- Identificador.
- Categoria.
- Descripcion.
- Latitud y longitud.
- Fecha de creacion.
- Estado.

## Pendientes recomendados

- Confirmar con Planeacion Municipal o Alcaldia de Tulua si existe una capa POT/SIG oficial mas reciente que el servicio DANE 2018.
- Incorporar pruebas automatizadas cuando se defina el framework de testing del frontend.
- Conectar `src/services/reportesService.js` con la API REST del backend.
- Normalizar categorias y estados contra catalogos oficiales del sistema.
- Definir simbologia final y reglas de intensidad para el mapa de calor con datos reales.
