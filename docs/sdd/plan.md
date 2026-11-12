# Plan técnico — NOT-129: API del mapa, del mapa de calor y de las estadísticas

> **Fase 2 de 5 del flujo SDD.** Responde **cómo** se construye lo que dice [spec.md](spec.md).
> No se escribe código hasta que este plan esté **Aprobado** por Andrés.

| Campo | Valor |
| --- | --- |
| Estado | **Aprobado por Andrés el 11 nov 2026** (con las propuestas del §7: 5 por página y máximo 10, `categoriaPrincipal` incluida, atendidos = solo Resuelto) |
| Especificación | [spec.md](spec.md) (aprobada con las decisiones del 11 nov) |
| Rama | `feature/not-129-api-mapa-calor-estadisticas` |
| Última actualización | 2026-11-11 |

## 1. Enfoque general

Tres rutas de solo lectura bajo `/api/admin/**` (ya protegidas por `SecurityConfig`: ROLE_ADMIN).
Se copia el estilo del listado del panel (`ListadoReportesController` / `Servicio` / `Repositorio`):
controlador delgado, servicio que valida y repositorio con SQL escrito a mano sobre
`NamedParameterJdbcTemplate`. **Sin Hibernate Spatial, sin librerías nuevas, sin migración** (salvo el
riesgo de rendimiento del §5).

Las tres rutas comparten el mismo filtro y la misma validación (`comuna` 1 a 10, `estado`, `idCategoria`,
`desde`, `hasta`), así que se escribe **una sola vez** y se reutiliza.

## 2. Stack y límites (fijados, no se cambian)

Java 21 + Spring Boot 3.3, PostgreSQL 16 + PostGIS 3.4, JWT, `NamedParameterJdbcTemplate`. No toca
correo, fotos, frontend ni `RESEND_PRUEBA_DESTINO`.

## 3. Cambios por capa

### Base de datos
- Se usan `reporte`, `categoria`, `estado_reporte` y `barrio`. El plan empezó sin migración nueva (la
  comuna se derivaba en cada lectura con `v_reporte_comuna`); tras medir con 10.000 reportes se
  agregaron **V19 y V20** y la comuna quedó guardada en `reporte.id_comuna` (§12).
- Índices que ya ayudan: `ix_reporte_ubicacion` (GiST), `ix_reporte_vigencia`, `ix_reporte_estado`,
  `ix_reporte_categoria`.

### Backend (`backend/src/main/java/com/notiurba/admin/`)

| Clase nueva | Qué hace |
| --- | --- |
| `MapaAdminController` | Las tres rutas `GET`; solo recibe parámetros y llama al servicio. |
| `MapaAdminServicio` | Valida filtros y paginación (400 `VALIDACION`), arma las respuestas, calcula el porcentaje. |
| `MapaAdminRepositorio` | Las consultas SQL. Un `WHERE` común para los filtros. |
| `FiltroMapa` (record) | `estado`, `idCategoria`, `comuna`, `desde`, `hasta` (+ `vigencia` solo en estadísticas). |
| `MapaResponse`, `CalorResponse`, `EstadisticasResponse` (records) | Forma JSON del contrato (§4). |

- El estado se valida con la misma regla de `ListadoReportesServicio.nombreDeEstado` (acepta
  «En trámite» o `EN_TRAMITE`). Se hace visible desde `EstadoReporte` **sin cambiar su comportamiento**.
- Anidados `categoria` y `barrio`: se reutilizan los de `ReporteDetalleResponse` para que el frontend
  lea el mismo formato que en el listado.
- Constantes: `MAXIMO_PUNTOS = 1000`, página por defecto 5, tamaño máximo 10.

### API (se agrega a `docs/contexto/11-contrato-api.md`, sección 5)

Todas: rol ADMIN; 401 `NO_AUTENTICADO` sin sesión; 403 `SIN_PERMISO` a un ciudadano; errores
`{codigo, mensaje, campo}`.

**Filtros comunes** (todos opcionales): `estado`, `idCategoria`, `comuna` (1 a 10), `desde`, `hasta`
(`AAAA-MM-DD`, hora de Colombia, inclusivos; `desde` > `hasta` → 400).

#### `GET /api/admin/mapa` (RF-22)
Solo reportes **vigentes y con ubicación**, los más recientes primero, hasta 1.000.

```json
{
  "total": 37,
  "truncado": false,
  "reportes": [
    {
      "codigoSeguimiento": "NU-000035",
      "estado": "Recibido",
      "categoria": { "idCategoria": 1, "nombre": "Vías, huecos y andenes" },
      "barrio": { "idBarrio": 12, "nombre": "Salesiano" },
      "comuna": 5,
      "latitud": 4.0861,
      "longitud": -76.1953,
      "creadoEn": "2026-11-05T10:07:00-05:00",
      "vigenteHasta": "2026-12-05T10:07:00-05:00"
    }
  ]
}
```
`total` = cuántos cumplen los filtros; `truncado` = hay más de 1.000. `comuna` y `barrio` pueden ser `null`.

#### `GET /api/admin/mapa/calor` (RF-23)
Mismo conjunto que el mapa (vigentes con ubicación) y mismos filtros. **Cada reporte pesa 1; los del
mismo punto se suman.** Más los parámetros de paginación de las comunas críticas: `pagina` (por defecto 1)
y `tamano` (por defecto 5, máximo 10).

```json
{
  "totalReportes": 10,
  "totalPuntos": 8,
  "truncado": false,
  "sinComuna": 0,
  "puntos": [ { "latitud": 4.0861, "longitud": -76.1953, "peso": 3 } ],
  "comunasCriticas": {
    "pagina": 1, "tamano": 5, "totalComunas": 6, "totalPaginas": 2,
    "items": [
      { "posicion": 1, "comuna": 5, "total": 4,
        "categoriaPrincipal": { "idCategoria": 1, "nombre": "Vías, huecos y andenes" } }
    ]
  }
}
```
- **Comunas críticas** = las comunas con al menos un reporte del conjunto, ordenadas de más a menos
  reportes (empate: número de comuna menor primero). Hay **10 comunas como máximo**, así que con el
  tamaño por defecto son **2 páginas a lo sumo**: la página 1 trae las 5 comunas con más reportes.
- `posicion` es el lugar en el ranking completo (en la página 2 empieza en 6).
- Los reportes sin comuna no entran al ranking: se cuentan en `sinComuna`.
- Si hay más de 1.000 puntos distintos se devuelven los 1.000 de mayor peso y `truncado` pasa a `true`.

#### `GET /api/admin/estadisticas` (RF-24)
Cuenta **todos** los reportes (con o sin ubicación). Filtros comunes más `vigencia`:
`vigentes` (`vigente_hasta` en el futuro), `fuera_de_plazo` (ya vencidos) o `todos` (por defecto).

```json
{
  "vigencia": "todos",
  "total": 12,
  "atendidos": 3,
  "porcentajeAtendidos": 25.0,
  "sinComuna": 0,
  "porEstado": [ { "estado": "Recibido", "total": 5 } ],
  "porCategoria": [ { "idCategoria": 1, "categoria": "Vías, huecos y andenes", "total": 4 } ],
  "porComuna": [ { "comuna": 1, "total": 2 } ]
}
```
- `atendidos` = reportes en estado **Resuelto**; `porcentajeAtendidos` = atendidos ÷ total × 100, un decimal; 0 si no hay reportes.
- `porEstado` trae siempre los 4 estados, `porComuna` las 10 comunas y `porCategoria` todas las categorías activas, **con 0 cuando no hay reportes**, para que las tablas del panel salgan completas.
- Se calcula con **una sola consulta** agrupada por (estado, categoría, comuna) y se suma en Java: las tres tablas salen de la misma foto de los datos y siempre cuadran entre sí.

### Frontend
- **Ninguno** en esta historia. Katherine lo conecta en NOT-135 y NOT-136. Dos cosas le avisamos:
  la lista «Puntos críticos» ahora es **por comuna** (el Figma muestra barrios) y las estadísticas
  tienen un filtro de **vigencia** que el diseño no incluye. Ver §7.

### Integraciones y configuración
- Variables de entorno nuevas: **ninguna**.

## 4. Estrategia de pruebas

Siguiendo la skill `notiurba-pruebas-y-despliegue`: base desechable `postgis/postgis:16-3.4`,
`mvn clean test`, y decir con claridad si algo se omitió por falta de Docker.

| Criterio (spec) | Tipo de prueba | Dónde |
| --- | --- | --- |
| CA-01, CA-02, CA-12, CA-13 | Integración (PostGIS real): vigente/vencido/sin ubicación/sin comuna/tope | `MapaAdminIntegracionTest` (nueva, mismo molde que `ListadoReportesIntegracionTest`) |
| CA-03, CA-04 | Integración: pesos que suman, orden y paginación de comunas | `MapaAdminIntegracionTest` |
| CA-05, CA-06, CA-11 | Integración: totales, ceros, vigencia (3 valores), porcentaje y base vacía | `MapaAdminIntegracionTest` |
| CA-07 | Integración: 401 sin token y 403 con ciudadano en las 3 rutas | `MapaAdminIntegracionTest` |
| CA-10 | Unitaria: filtros y paginación inválidos → 400 con su campo | `MapaAdminServicioTest` (nueva) |
| CA-08 | Integración: 1.000 reportes sembrados con `generate_series`, cada ruta ≤ 2 s | `MapaAdminIntegracionTest` |
| CA-09 | Revisión de `docs/contexto/11-contrato-api.md` | manual en el PR |

Cada defecto se prueba **primero fallando** y luego se corrige. Las 268 pruebas existentes deben seguir
pasando. Al final, una llamada real a una API local con un token ADMIN, y la prueba contra Render solo si
Andrés lo pide (el plan Free duerme).

## 5. Riesgos y mitigaciones

| Riesgo | Prob. / impacto | Mitigación |
| --- | --- | --- |
| **La comuna no se guarda en el reporte**: se calcula con `fn_asignar_comuna` por cada fila (con casteos a `geography`). Con 1.000 reportes podría pasar de 2 s (CA-08). | Media / Alto | Medir primero con 1.000 reportes. Si no cumple, **no cambiar nada por mi cuenta**: proponerte una migración V19 que guarde `id_comuna` en `reporte` (decisión tuya; V19 es la siguiente). **Resultado:** con 1.000 cumplía (~0,3 s), con 10.000 no (31,5 s); aprobado y resuelto con V19 y V20 (§12). |
| Linear dice «la comuna la asigna PostGIS al guardar», pero el código la deriva al leer. | Baja / Bajo | Resuelto con V20: ahora sí se guarda al crear el reporte. |
| `total` y el tope de 1.000 dan resultados distintos si entran reportes entre dos llamadas. | Baja / Bajo | Cada ruta usa una sola consulta con `count(*) OVER()`; aceptado. |
| Pruebas de tiempo inestables en un PC lento. | Media / Bajo | Se mide con 1.000 filas y se reporta el tiempo real; el umbral es de 2 s como pide la historia. |
| Datos de ubicación solo para ADMIN (Ley 1581). | Baja / Alto | Rutas bajo `/api/admin/**`; la respuesta no incluye datos del ciudadano. Hay una prueba que lo verifica. |

## 6. Alternativas descartadas

- **Usar la vista `v_reporte_mapa`:** no trae barrio ni nombre de categoría ni filtra por estado/fechas; se necesita un SQL propio (con el mismo criterio de vigencia).
- **Una ruta por cada tabla de estadísticas:** tres consultas pueden descuadrarse entre sí; una sola es más rápida y consistente.
- **Agrupar el calor en una cuadrícula:** pierde precisión y no lo pidió nadie; basta con sumar los reportes del mismo punto.
- **Mover la asignación de comuna al frontend:** va contra la regla del proyecto (la fuente de verdad es PostGIS).

## 7. Decisiones pendientes y avisos (nada de esto se hace sin tu permiso)

- [PENDIENTE] **Tamaño de página** de comunas críticas: 5 por defecto y 10 máximo.
- [PENDIENTE] **`categoriaPrincipal`** en cada comuna crítica: el Figma la muestra («Comuna 5 · Vías, huecos…») y cuesta una subconsulta. Propuesta: incluirla.
- [PENDIENTE] **«Atendidos»** cuenta solo Resuelto. Se cambia a «Resuelto + En trámite» con una línea si prefieres.
- **Figma** — HECHO el 11 nov con permiso de Andrés: «Puntos críticos» ahora lista comunas (1. Comuna 5, 2. Comuna 2, 3. Comuna 1) con su categoría principal, y el subtítulo dice «Comunas ordenadas por cantidad de reportes vigentes.». El mismo día se agregó la pantalla nueva «Panel administrativo · Estadísticas» (duplicada del mapa de calor): navegación Reportes · Mapa de calor · Estadísticas, filtro de vigencia (Todos, Vigentes, Fuera de plazo), indicadores (total, atendidos y sin comuna) y las secciones por estado, por comuna y por categoría con sus números visibles. Después se rehízo con los valores del Design System (Nunito Sans y sus tamaños, tokens de color semánticos, chips de estado, filtros en píldora de 42 px, tarjetas con borde `borde/tarjeta` y radio 16). Los textos y colores no quedaron enlazados a los estilos y variables de Figma (entraron por SVG).
- **Linear** — HECHO el 11 nov con permiso de Andrés: NOT-129 y NOT-136 dicen «atendidos» y recogen las decisiones (comunas, paginación, peso 1, vigencia, tope).

## 8. Documentos que se actualizan (regla del repo: `docs/` y Google Doc)

- `docs/contexto/11-contrato-api.md` §5 (las tres rutas, ejemplos y errores) — parte de cada PR.
- `docs/contexto/12-estado-actual-y-traspaso.md` (quitar «falta» de esas rutas) al cerrar la historia.
- Google Doc («Diseño técnico y arquitectura») y la nota del vault al cerrar la historia; Andrés ya autorizó editar el Doc.

## 9. Orden de trabajo propuesto (PR pequeños, uno tras otro)

1. **PR A — Mapa:** filtro común, validación, `GET /api/admin/mapa`, pruebas de seguridad y rendimiento. Katherine ya puede empezar NOT-135.
2. **PR B — Calor y comunas críticas:** `GET /api/admin/mapa/calor` con paginación.
3. **PR C — Estadísticas:** `GET /api/admin/estadisticas` con vigencia, ceros y atendidos.

Cada PR: rama `feature/*` hacia `develop`, **sin coautoría de Claude**, y el contrato actualizado.
El detalle por tarea irá en [tasks.md](tasks.md) cuando apruebes este plan.

## 10. Aprobación

- [x] Cada criterio de `spec.md` tiene un cambio técnico y una prueba (tabla del §4).
- [x] No introduce otro framework ni otra base de datos; no hay migración.
- [x] Contrato de API revisado (§3).
- [x] Decisiones pendientes del §7 resueltas.
- [x] **Aprobado por Andrés** (11 nov) → se pasa a `tasks.md` y se empieza a implementar.

## 11. Cambios tras la revisión de código y el QA (11 nov)

- `FiltroMapa` se reemplazó por `reporte/FiltroPanel`, compartido con el listado del panel: una sola
  validación (`FiltroPanel.de`) y un solo armado SQL (`FiltroPanel.condiciones`). El controlador arma el
  filtro y los servicios lo reciben por nombre.
- El calor corre en `REPEATABLE READ` para que sus dos consultas vean los mismos reportes. Desde el 13 nov
  lo pide con `SET TRANSACTION` dentro de la transacción (no por JDBC, que lo cambia en la sesión); las
  estadísticas salen de una sola consulta y usan el aislamiento por defecto.
- Con filtro de categoría, `porCategoria` trae solo esa categoría.
- `geo/ConsultaEspacial.sinJit` apagaba el JIT de PostgreSQL dentro de la transacción en las consultas con
  `v_reporte_comuna` (mapa, calor, estadísticas y listado del panel): era el mayor costo de tiempo. Se
  quitó el 12 nov, cuando la comuna guardada dejó esas consultas lejos del umbral del JIT (§12).
- Guardar `id_comuna` en `reporte` al crearlo quedó como propuesta; se aprobó el 12 nov (§12).
- QA de punta a punta reutilizable: `tools/qa-panel-admin/`.

## 12. V19 y V20: comuna más rápida (APROBADO por Andrés el 12 nov)

**Problema medido (11 nov, base desechable con 10.000 reportes, la mitad fuera de la capa):** la comuna no
se guarda; `v_reporte_comuna` llama a `fn_asignar_comuna` por cada reporte en cada lectura. Un punto dentro
de una comuna es barato (usa el índice GiST), pero uno en un hueco o fuera de la capa calcula la distancia
en `geography` a las 10 comunas convirtiendo cada polígono en cada llamada (~7 ms por punto).

| Consulta (10.000 reportes) | Hoy | A | C |
| --- | --- | --- | --- |
| Asignar la comuna a todos | 31,1 s | 1,97 s | — |
| Estadísticas | 31,5 s | — | 2,2 ms |
| Mapa filtrado por comuna | 16 s | — | 1,3 ms |

**A. `comuna.geog`:** columna `geography` generada (`GENERATED ALWAYS AS (geom::geography) STORED`) con
índice GiST; `fn_asignar_comuna` usa `geog` en vez de convertir `geom` en cada llamada. Mismas reglas:
0 diferencias en los 10.000 reportes. Riesgo bajo (solo agrega).

**C. `reporte.id_comuna`:** se guarda al crear el reporte (trigger con la misma `fn_asignar_comuna`) y se
recalcula si cambian la ubicación o el barrio del reporte, o si cambia el polígono de una comuna (la capa es
provisional). Índice en `reporte(id_comuna)`. `v_reporte_comuna` lee la columna, así que el mapa, el calor,
las estadísticas, el listado, el detalle y el correo se aceleran sin cambiar su código. La migración llena
los reportes que ya existen (en Neon hay 4).

Riesgos: C duplica un dato derivado (lo mantienen los triggers; pruebas que lo verifiquen al crear, al
cambiar ubicación y al cambiar la capa). Migración no destructiva; aun así, branch de respaldo en Neon antes
de desplegar. Siguiente número libre: V19.

**Decisiones de implementación (12 nov):**
- Dos migraciones para commits atómicos: **V19** = A (`comuna.geog` + índice GiST + `fn_asignar_comuna`
  con `geog`); **V20** = C (`reporte.id_comuna` y `reporte.comuna_origen` + índice + triggers + vista).
- La regla sigue en un solo lugar: el trigger de `reporte` llama a `fn_asignar_comuna` y, si no hay
  polígono, usa el barrio. Para recalcular se reasigna la ubicación (`SET ubicacion = ubicacion`), así
  que todos los recálculos pasan por el mismo trigger.
- Se recalcula al cambiar: la ubicación o el barrio del reporte, el polígono de una comuna, la comuna de
  un barrio o `configuracion.comuna_distancia_maxima_m`.
- `v_reporte_comuna` conserva sus columnas (`id_comuna`, `origen`) pero lee lo guardado.
- Las consultas del panel, el detalle y el correo leen `r.id_comuna`; `AsignadorDeComuna` sigue usando la
  función para rechazar puntos fuera de cobertura al crear.
- Si con la comuna guardada el costo estimado ya no activa el JIT, se quita `ConsultaEspacial.sinJit`.

**Resultado (12 nov, base desechable con 10.000 reportes al azar en la capa; 4.923 caen en un polígono):**

| Medida | Antes | Después |
| --- | --- | --- |
| Estadísticas | 31,5 s | 12 ms |
| Mapa filtrado por comuna | 16 s | 2 ms (usa `ix_reporte_comuna`) |
| Mapa sin filtro / puntos del calor | — | 11 ms / 9 ms |
| Crear 10.000 reportes (el trigger asigna cada comuna) | — | 4,5 s |
| Recalcular los 10.000 (cambio de capa o de distancia) | 31,1 s (asignar todos) | 2,6 s |
| Costo estimado de las consultas del panel | pasaba de 100.000 (JIT) | 1.000 a 2.300 |

Con ese costo el JIT ya no se activa: se quitó `ConsultaEspacial.sinJit` y su prueba. QA de punta a punta
contra el jar real: 44 de 44; las cuatro rutas del panel responden en 6 a 11 ms.

**Ajustes tras la revisión con `code-reviewer` (13 nov).** V20 no se había aplicado fuera de bases
desechables (la base local va en V12 y la rama no está en GitHub), así que se corrigió en la misma V20:
- Cada recálculo hace `LOCK TABLE reporte IN SHARE ROW EXCLUSIVE MODE`: espera a los reportes que se están
  creando y frena los nuevos hasta terminar. Costo: mientras se recalcula (2,6 s con 10.000 reportes),
  crear un reporte espera. Pasa solo al cambiar la capa, la comuna de un barrio o la distancia máxima.
- El trigger del reporte también se dispara con `UPDATE OF id_comuna, comuna_origen`: esas columnas son
  de solo lectura en la práctica.
- La distancia máxima recalcula también si su fila se borra, se crea o se renombra.
- El cambio de capa compara el polígono antes y después (tablas de transición): si no cambió, no
  reescribe los reportes.
- No se hizo (y por qué): recalcular solo los reportes cercanos al polígono que cambió. La capa cambia
  muy pocas veces y hoy hay 4 reportes; queda como mejora si la capa empieza a cambiar seguido.
