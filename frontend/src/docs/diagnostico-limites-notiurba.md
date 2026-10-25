# Diagnostico topologico de limites POT 2015

## Resultado actual (2026-10-11)

- Diez geometrias validas, no vacias, en el GeoJSON y el SHP original. No se corrigieron limites.
- 27 huecos interiores: 250055.971849 m2. 23 superficies de solapamiento: 131.772676 m2, sin sumar dos veces cobertura multiple.
- 8 huecos y 8 superficies de solapamiento superan 0.01 m2. Las restantes son micro-superficies numericas: no equivalen a 34 conflictos administrativos.
- Nueve comunas relacionadas: 01, 02, 03, 04, 05, 06, 07, 09, 10. Las comunas indicadas para huecos son adyacentes, no una asignacion administrativa del espacio vacio.
- Fuente preservada: POT SHA256 b2f9f982c8d75be60b1cd6bf2b4eafcbc06d99025e39c3b0b69f870655615bc6; DANE SHA256 62dc7ca98d53ff0cd8be77143cc56e32d6bfd26533e599a35b1c42d7c40aaccd.

## Metodo y precision

`src/utils/diagnosticarLimites.py` lee el GeoJSON realmente utilizado por Leaflet. Shapely calcula intersecciones de superficie (no lineas compartidas), separa caras de cobertura y agrega remanentes positivos. Procesa exclusivamente componentes poligonales, incluidos los de GeometryCollection. Los huecos son interiores de la union, restando las islas cubiertas; no son todos los espacios sin cobertura del municipio ni vacios conectados al exterior.

La topologia se evalua sobre los vertices EPSG:4326 del archivo, evitando fabricar incidencias al reproyectar antes de intersectar. Cada superficie se transforma a EPSG:3115 (MAGNA Colombia Oeste, identificado en PRJ y metadatos), con ejes XY explicitos y sin transformacion ballpark, para calcular m2. La salida conserva las geometrias geograficas exactas del analisis, sin simplificar, rellenar, recortar ni ajustar vertices administrativos. El umbral 0.01 m2 solo etiqueta micro-superficies; no las elimina. Se comprueba validez de la salida, cobertura de intersecciones y ausencia de doble conteo, con tolerancias numericas GEOS. Las relaciones adyacentes usan una tolerancia de 1e-11 grados, solo para atribucion, no para alterar geometrias.

Comparacion con el SHP del ZIP: 27 huecos y 22 componentes de solapamiento originales, area 131.772598273 m2. La diferencia de conteos se concentra en micro-superficies inestables ante transformacion/operaciones numericas; no son nuevos conflictos administrativos. Desplazamiento maximo de retorno de vertices: 2.124e-9 m. Diferencia total de area de solapamientos menor de 0.0001 m2 y de huecos menor de 0.0002 m2. Los ocho casos apreciables de cada tipo ya existen en el SHP: la transformacion no explica sus dimensiones. Estas mediciones NO expresan precision de levantamiento ni validez legal. No se aplica make_valid, buffer(0), snapping ni precision grid.

## Contraste visual P-28

Se renderizo el PDF local y se superpusieron temporalmente las incidencias apreciables usando su grilla impresa, no el WKT interno discrepante. Este registro visual aproximado no constituye una georreferenciacion definitiva ni tiene un RMS certificado. El plano indica agosto de 2015 y escala de ploteo 1:10000; la leyenda separa hidrologia y fichas normativas, y muestra Aguaclara/C10 al norte.

H001 sigue principalmente el corredor del rio Tulua: **posible exclusion cartografica/hidrografica**, pendiente de confirmacion. Su continuidad explica gran parte del area sin cobertura; no se debe rellenar automaticamente. Los restantes huecos apreciables estan entre comunas y no se puede distinguir discontinuidad de exclusion con certeza. Los solapamientos estrechos no se resuelven como limites definitivos con el grosor de linea a esta escala. Su clasificacion como posible error geometrico no supone error administrativo. Falta el anexo de escala de trabajo y la confirmacion municipal de exclusiones, tolerancias y vigencia. No se utilizo QGIS ni se digitalizo el PDF.

## Incidencias individuales

Areas aproximadas en m2; notacion cientifica para superficies microscopicas. Cada geometria, punto interior de referencia y estado de revision esta en el unico archivo `src/data/geo/diagnostico-limites-POT2015.geojson`.

| ID | Tipo | Area m2 | Comunas adyacentes/involucradas | Posible causa | Recomendacion |
| --- | --- | ---: | --- | --- | --- |
| S001 | solapamiento | 67.885 | 01/02 | Posible error geometrico: doble cobertura | Reportes pendientes; contrastar limite original |
| S002 | solapamiento | 22.787 | 03/10 | Posible error geometrico: doble cobertura | Reportes pendientes; contrastar limite original |
| S003 | solapamiento | 10.204 | 09/10 | Posible error geometrico: doble cobertura | Reportes pendientes; contrastar limite original |
| S004 | solapamiento | 10.148 | 05/06 | Posible error geometrico: doble cobertura | Reportes pendientes; contrastar limite original |
| S005 | solapamiento | 7.940 | 09/10 | Posible error geometrico: doble cobertura | Reportes pendientes; contrastar limite original |
| S006 | solapamiento | 6.642 | 09/10 | Posible error geometrico: doble cobertura | Reportes pendientes; contrastar limite original |
| S007 | solapamiento | 4.867 | 04/06 | Posible error geometrico: doble cobertura | Reportes pendientes; contrastar limite original |
| S008 | solapamiento | 1.299 | 09/10 | Posible error geometrico: doble cobertura | Reportes pendientes; contrastar limite original |
| S009 | solapamiento | 1.169e-6 | 04/05 | Micro-superficie: precision numerica no concluyente | No corregir; revisar tolerancias y fuente |
| S010 | solapamiento | 4.420e-7 | 04/05 | Micro-superficie: precision numerica no concluyente | No corregir; revisar tolerancias y fuente |
| S011 | solapamiento | 2.492e-7 | 02/03 | Micro-superficie: precision numerica no concluyente | No corregir; revisar tolerancias y fuente |
| S012 | solapamiento | 1.288e-7 | 04/06 | Micro-superficie: precision numerica no concluyente | No corregir; revisar tolerancias y fuente |
| S013 | solapamiento | 9.667e-8 | 04/06 | Micro-superficie: precision numerica no concluyente | No corregir; revisar tolerancias y fuente |
| S014 | solapamiento | 6.724e-8 | 04/09 | Micro-superficie: precision numerica no concluyente | No corregir; revisar tolerancias y fuente |
| S015 | solapamiento | 5.869e-8 | 04/07 | Micro-superficie: precision numerica no concluyente | No corregir; revisar tolerancias y fuente |
| S016 | solapamiento | 3.997e-8 | 02/03 | Micro-superficie: precision numerica no concluyente | No corregir; revisar tolerancias y fuente |
| S017 | solapamiento | 1.266e-8 | 04/07 | Micro-superficie: precision numerica no concluyente | No corregir; revisar tolerancias y fuente |
| S018 | solapamiento | 7.431e-9 | 04/07 | Micro-superficie: precision numerica no concluyente | No corregir; revisar tolerancias y fuente |
| S019 | solapamiento | 6.559e-9 | 04/06 | Micro-superficie: precision numerica no concluyente | No corregir; revisar tolerancias y fuente |
| S020 | solapamiento | 5.029e-9 | 04/09 | Micro-superficie: precision numerica no concluyente | No corregir; revisar tolerancias y fuente |
| S021 | solapamiento | 4.237e-9 | 04/07 | Micro-superficie: precision numerica no concluyente | No corregir; revisar tolerancias y fuente |
| S022 | solapamiento | 1.785e-9 | 04/06 | Micro-superficie: precision numerica no concluyente | No corregir; revisar tolerancias y fuente |
| S023 | solapamiento | 1.665e-9 | 02/03 | Micro-superficie: precision numerica no concluyente | No corregir; revisar tolerancias y fuente |
| H001 | hueco | 249614.631 | 01/02/03/04/05/09/10 | Posible exclusion del corredor del rio Tulua (P-28) | Consultar exclusion hidrografica con Planeacion |
| H002 | hueco | 383.082 | 01/02 | Discontinuidad o exclusion pendiente de validacion | Comuna sin determinar; consultar Planeacion |
| H003 | hueco | 33.038 | 06/07 | Discontinuidad o exclusion pendiente de validacion | Comuna sin determinar; consultar Planeacion |
| H004 | hueco | 13.554 | 01/05 | Discontinuidad o exclusion pendiente de validacion | Comuna sin determinar; consultar Planeacion |
| H005 | hueco | 5.833 | 02/03 | Discontinuidad o exclusion pendiente de validacion | Comuna sin determinar; consultar Planeacion |
| H006 | hueco | 3.986 | 01/05 | Discontinuidad o exclusion pendiente de validacion | Comuna sin determinar; consultar Planeacion |
| H007 | hueco | 1.613 | 01/05 | Discontinuidad o exclusion pendiente de validacion | Comuna sin determinar; consultar Planeacion |
| H008 | hueco | 0.234 | 01/05 | Discontinuidad o exclusion pendiente de validacion | Comuna sin determinar; consultar Planeacion |
| H009 | hueco | 2.171e-5 | 05/06 | Micro-superficie: precision numerica no concluyente | No corregir; revisar tolerancias y fuente |
| H010 | hueco | 1.705e-5 | 01/05 | Micro-superficie: precision numerica no concluyente | No corregir; revisar tolerancias y fuente |
| H011 | hueco | 4.495e-7 | 02/03 | Micro-superficie: precision numerica no concluyente | No corregir; revisar tolerancias y fuente |
| H012 | hueco | 1.767e-7 | 02/03 | Micro-superficie: precision numerica no concluyente | No corregir; revisar tolerancias y fuente |
| H013 | hueco | 1.098e-7 | 02/03 | Micro-superficie: precision numerica no concluyente | No corregir; revisar tolerancias y fuente |
| H014 | hueco | 1.067e-7 | 04/07 | Micro-superficie: precision numerica no concluyente | No corregir; revisar tolerancias y fuente |
| H015 | hueco | 9.986e-8 | 04/07 | Micro-superficie: precision numerica no concluyente | No corregir; revisar tolerancias y fuente |
| H016 | hueco | 8.060e-8 | 04/05 | Micro-superficie: precision numerica no concluyente | No corregir; revisar tolerancias y fuente |
| H017 | hueco | 2.651e-8 | 04/09 | Micro-superficie: precision numerica no concluyente | No corregir; revisar tolerancias y fuente |
| H018 | hueco | 2.555e-8 | 04/06/07 | Micro-superficie: precision numerica no concluyente | No corregir; revisar tolerancias y fuente |
| H019 | hueco | 1.456e-8 | 02/03 | Micro-superficie: precision numerica no concluyente | No corregir; revisar tolerancias y fuente |
| H020 | hueco | 1.454e-8 | 04/06 | Micro-superficie: precision numerica no concluyente | No corregir; revisar tolerancias y fuente |
| H021 | hueco | 1.276e-8 | 02/03 | Micro-superficie: precision numerica no concluyente | No corregir; revisar tolerancias y fuente |
| H022 | hueco | 4.505e-9 | 04/07/09 | Micro-superficie: precision numerica no concluyente | No corregir; revisar tolerancias y fuente |
| H023 | hueco | 2.497e-9 | 02/03 | Micro-superficie: precision numerica no concluyente | No corregir; revisar tolerancias y fuente |
| H024 | hueco | 4.786e-10 | 04/05/06 | Micro-superficie: precision numerica no concluyente | No corregir; revisar tolerancias y fuente |
| H025 | hueco | 1.764e-10 | 02/03 | Micro-superficie: precision numerica no concluyente | No corregir; revisar tolerancias y fuente |
| H026 | hueco | 2.393e-12 | 09/10 | Micro-superficie: precision numerica no concluyente | No corregir; revisar tolerancias y fuente |
| H027 | hueco | 4.933e-18 | 04/06 | Micro-superficie: precision numerica no concluyente | No corregir; revisar tolerancias y fuente |

## Efecto en reportes y mapa

Una unica coincidencia interior asigna comuna provisional. Cualquier limite o cobertura multiple produce `pendiente_revision` sin comuna arbitraria. Ninguna coincidencia produce `sin_determinar` y el texto Comuna sin determinar; no implica estar fuera de Tulua. Cambia el anterior identificador interno `fuera_zona_urbana`; el componente consumidor fue actualizado. La tolerancia punto-segmento se normaliza por longitud (1e-10 grados), evitando que segmentos cortos generen falsas bandas anchas de frontera; no es una tolerancia administrativa.

H001/H002 son prioritarios por area sin asignacion; S001/S002 por doble cobertura. El resto de superficies apreciables tambien afecta puntos que caigan exactamente alli. Las micro-superficies tienen efecto practico no concluyente dada la precision de las coordenadas de reportes: se mantienen visibles y pendientes, no se presentan como errores administrativos. La demostracion ahora tiene diez coincidencias unicas, una por comuna, sin pendientes ni casos sin determinar. NTU-010 (4.126, -76.236) se conserva intacto como caso exterior en `reportesFueraCobertura`, excluido del servicio de demostracion y cubierto por pruebas. Las pruebas tambien incluyen puntos interiores reales de todas las incidencias no microscopicas.

El control Diagnostico de limites activa rojo para solapamientos y amarillo para huecos, conservando los contornos de las diez comunas aunque su casilla estuviera apagada. Ambas subcapas son independientes. La lista ordena por m2 descendentes; seleccionar una fila habilita su subcapa, centra el mapa y abre el popup con ID, tipo, comunas, m2, latitud/longitud y estado. Las geometrias no se ensanchan para parecer mayores; a zoom maximo las micro-superficies pueden ser subpixel. El grosor de contorno es simbolizacion, no dimension real. Calor, filtros y marcadores permanecen independientes.

## Reproduccion

Herramientas existentes: Python, Shapely, PyProj, pyshp (comparacion ZIP); PyMuPDF para inspeccion PDF. Ninguna dependencia React adicional.

En esta maquina las herramientas Python estan en el directorio temporal existente:

```powershell
$env:PYTHONPATH="$env:TEMP\\notiurba-pdf-tools"
python src/utils/diagnosticarLimites.py --check --zip "C:\\Users\\Usuario\\Downloads\\Consolidado cartografia formulacion (SHP) EOT2015.zip"
node --test src/utils/geoUtils.test.mjs
npm run lint
npm run build
npm run dev
```

El nombre del ZIP debe sustituirse por el nombre real con sus tildes. Sin --check se regenera solamente el GeoJSON de diagnostico; nunca los originales. Para repetir la misma comparacion debe proporcionarse el ZIP con --zip. El directorio temporal puede desaparecer: el frontend utiliza datos precalculados y no necesita Python para ejecutarse. La cartografia permanece pendiente de validacion administrativa.

## Verificacion del prototipo

Diez pruebas Node satisfactorias: asignacion unica, limites, solapamientos, huecos, Aguaclara, distribucion de un reporte por comuna, conservacion del caso exterior, exclusion del caso exterior en el servicio, superposicion real 03/10 y coherencia del diagnostico con el hash de la fuente. El comando Python --check reproduce exactamente el GeoJSON normalizado. Lint y build correctos; persiste la advertencia de bloque superior a 500 kB, no un error de compilacion.

En navegador: 10 contornos administrativos, 50 geometrias diagnosticas y 50 filas; toggles separados dejan 23 solapamientos o 27 huecos. Seleccion S002 muestra 22.79 m2, comunas 03/10 y coordenadas coincidentes con el GeoJSON. Desactivar el modo elimina su pane sin desmontar el calor; filtros y marcadores permanecen operativos. Se corrigio la prioridad del popup para que las incidencias no tapen su contenido. No hubo errores nuevos tras la recarga final; hubo mensajes transitorios de hot reload durante la separacion de modulos, ya resueltos.

