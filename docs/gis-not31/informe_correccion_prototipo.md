# NOT-31 — Partición experimental del prototipo académico

**SINTÉTICA, NO OFICIAL.** Fecha: 2026-11-04. Estado de validación: **OK**. No se integró al backend ni se cambió Linear.

## Perímetro declarado y reglas reproducibles

Se utiliza exclusivamente la capa original con SHA256 `b2f9f982c8d75be60b1cd6bf2b4eafcbc06d99025e39c3b0b69f870655615bc6`. Se transforma a EPSG:3115 con XY explícito y sin ballpark; se cuantiza en una rejilla **real de 0,01 m** como decisión del modelo experimental, no como exactitud oficial ni tolerancia para ocultar errores. Se disuelven los diez polígonos cuantizados y se toman sus anillos exteriores, eliminando sus anillos interiores para definir el dominio que DEBE cubrirse. No se usa envolvente convexa, buffer expansivo ni se unen componentes separados artificialmente. Si un componente exterior incluye un vacío, ese vacío pasa explícitamente al dominio experimental y debe asignarse. El archivo de perímetro representa esa decisión sintética, no el perímetro urbano oficial.

La rejilla cambia coordenadas y puede colapsar microincidencias: no se afirma que todas fueran errores numéricos. Cada vértice de entrada tiene su destino cuantizado y desplazamiento en registro_vertices_precision_prototipo.csv; máximo observado: 0.00704274767376 m. Las áreas agregadas/retiradas por comuna durante esa etapa se registran separadamente. No se descartan superficies con un umbral de área o de visualización.

En solapamientos, la prioridad determinista es código menor 01→10. Se conserva el interior que ya tenía cobertura, salvo cambios explícitos de rejilla y resolución de doble cobertura. Para zonas sin asignación se construyen sitios de frontera con los vértices y muestras cada <=10 m de cada anillo de las comunas cuantizadas. Sitios coincidentes y empates usan código menor y orden de coordenadas. Dentro de esos vacíos se introducen bisectrices de Voronoi; cada cara recibe la comuna del sitio más próximo. Es aproximación a proximidad de frontera muestreada (error de distancia por muestreo acotado por 5 m, más cuantización), **no una frontera territorial inferida**. Las bisectrices sólo se utilizan en vacíos: no se reemplazan todos los interiores por un Voronoi global.

La red completa de límites se noda y poligoniza con precisión centimétrica. Cada cara interior se asigna exactamente una vez mediante esas reglas y se agrupa por ID. Antes de exportar se insertan todos los nodos compartidos colineales usando aritmética entera en centímetros; no se altera la geometría métrica. Cada coordenada compartida se transforma de manera común a EPSG:4326. Esto evita que una frontera se reproyecte como A-C en una comuna y A-B-C en su vecina, generando microvacíos. También se noda el perímetro declarado, sin cambiar su trazado métrico.

## H001 y superficies sintéticas

H001 y el resto de zonas sin asignación se identifican con el inventario existente. Se autoriza aquí su reparto **sintético para pruebas**, sin confirmar ni negar la hipótesis hidrográfica. H001 no se asigna completa a una sola comuna. Reparto medido por intersección del H001 inventariado con el resultado métrico: `{"01": 70032.28266629517, "02": 6409.865925010646, "03": 30110.20847815512, "04": 27294.55487725724, "05": 62717.66974494851, "09": 33932.1947813004, "10": 19117.85492926328}` m². Estas cifras incluyen cualquier cobertura previa de tamaño numérico y la etapa de rejilla; el registro de huecos asignados distingue la nueva atribución desde la base cuantizada.

| Comuna | Vacíos asignados m² | Solapes cedidos m² | Cambio neto frente al original m² |
| --- | ---: | ---: | ---: |
| 01 | 70235.485331 | 0.000000 | 70236.929574 |
| 02 | 6604.342686 | 67.562799 | 6534.547610 |
| 03 | 30112.203600 | 0.000000 | 30116.105899 |
| 04 | 27294.648350 | 0.000000 | 27292.880442 |
| 05 | 62729.342533 | 0.059300 | 62719.468793 |
| 06 | 15.402344 | 15.164035 | 10.680371 |
| 07 | 17.406056 | 0.000000 | 19.236570 |
| 08 | 0.000000 | 0.000000 | -3.428567 |
| 09 | 33930.873684 | 0.000000 | 33931.150852 |
| 10 | 19117.903416 | 48.358636 | 19071.613502 |

superficies_reasignadas_prototipo.csv incluye además el cambio agregado/retirado por rejilla. Los nuevos cruces también se cuantizan durante el nodificado de la red: eso puede mover pequeñas franjas de las fronteras de base. Se registran como ganado_por_nodificado_m2, cedido_por_nodificado_m2 y prioridad_modificada_por_nodificado_m2, sin atribuirlas falsamente a huecos o solapamientos originales. La prioridad se aplica a cada cara de esa red sintética; las desviaciones geométricas frente a una prioridad de polígonos sin redondear quedan cuantificadas. Los vacíos asignados se miden por intersección con el vacío de referencia, no por el área entera de una cara que lo atraviese.

areas-reasignadas-prototipo.geojson permite inspeccionar cada etapa; esas capas de auditoría pueden superponerse entre etapas y no deben sumarse como área única ni confundirse con la partición final. Sus componentes poligonales se exportan individualmente. Si una microgeometría de auditoría se degenera al pasar a WGS84, el registro conserva superficie y WKT EPSG:3115 con geometría visual nula, sin eliminar el registro ni confundirlo con una falta de cobertura. Recuento: {"registros": 1888, "registros_solo_metricos": 49, "todas_areas_registradas": true, "nota": "Registros no representables en WGS84 conservan WKT EPSG3115 y superficie; no son huecos de la particion"}. Las diez geometrías de la partición final son válidas y no nulas.

registro_caras_prototipo.csv identifica regla, ID y superficie de cada cara. registro_vertices_resultado_prototipo.csv identifica todos los vértices finales, anillo, ID y coordenadas en ambos CRS. Los sitios se conservan en sitios_proximidad_prototipo.csv y resultados_puntos_prototipo.csv vincula los cincuenta puntos de anomalías a sus ID H/S originales. Los campos heredados Shape_Area/Shape_Leng siguen siendo atributos del insumo; NOT31_AREA_M2 y el CSV contienen las superficies del resultado. Ninguna de estas fronteras sintéticas es oficial.

## Validación obligatoria, sin reutilizar conteos posteriores

Antes se conservan los conteos documentados 27/23; **después se vuelve a calcular** unión, huecos y las 45 intersecciones por parejas tanto en la partición EPSG:3115 como en EPSG:4326. No se acepta una superficie residual distinta de cero, ni una tolerancia de visualización. También se comprueba coverage_is_valid, geometrías válidas/no vacías, diez ID/nombres originales, nulos residuales del perímetro menos cobertura y cobertura menos perímetro. Se releen los GeoJSON escritos y se comprueba igualdad exacta de sus coordenadas con las validadas en memoria. La cobertura completa se comprueba contra el perímetro independiente derivado de los anillos originales cuantizados, no redefiniéndolo para ocultar huecos del resultado.

- EPSG:3115: 0 huecos; 0 solapes; sin asignar 0 m²; fuera del perímetro 0 m².
- EPSG:4326 exportado: 0 huecos; 0 solapes; sin asignar 0 m²; fuera 0 m² (medidos tras proyectar las diferencias a EPSG:3115).
- Puntos existentes: 74/74 coherentes con el nuevo dominio; diez interiores originales deben conservar su ID y los tres exteriores seguir fuera. Los puntos antiguos de anomalías se reclasifican según la partición sintética; sus expectativas previas no se sobrescriben. Los bordes pueden tener dos candidatos sin que exista solapamiento de superficie. Ver resultados_puntos_prototipo.csv.
- QGIS: `{"archivo": "NotiUrba_Prototipo_Experimental.qgz", "capas_validas": 6, "crs": "EPSG:4326", "original_sin_cambios": true}`.

## Reproducción y límites de uso

Desde la copia aislada ejecutar con `C:\Program Files\QGIS 3.44.15\bin\python-qgis-ltr.bat` el script `docs/gis-not31/construir_prototipo_experimental.py`. Requiere Shapely 2.1, GEOS >=3.12, PyProj y PyQGIS existentes. Parámetros: EPSG:3115, rejilla 0,01 m, muestreo <=10 m, prioridad ascendente por ID. Los resultados corresponden a estas decisiones; cambiar parámetros crea otra partición, no mayor certeza administrativa.

La capa es apta únicamente para ensayos geométricos académicos cuando resultado_validacion_prototipo.json indica OK. No certifica límites, barrios, exclusiones de ríos, cobertura oficial, vigencia o aceptación del equipo. No debe usarse para gestión territorial real, decisiones administrativas o asignación oficial de reportes. No se sustituyen originales, no se toca producción/backend/migraciones y no se hicieron commits, push o PR. Los informes previos describen la capa original y se conservan como respaldo; sus bloqueos administrativos no se presentan como resueltos por esta simulación.

## Comprobacion de reproducibilidad

La ultima ejecucion integral se comparo con la anterior: el SHA256 del GeoJSON final permanecio identico. Se comprobaron los hashes de los once artefactos registrados y la representabilidad valida de los componentes de auditoria; los registros visualmente nulos conservan su WKT metrico. No se exige igualdad binaria del ZIP QGZ, que contiene metadatos de fecha.
