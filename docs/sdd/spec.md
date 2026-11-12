# Especificación — NOT-129: API del mapa, del mapa de calor y de las estadísticas

> **Fase 1 de 5 del flujo SDD** (Especificación → Planificación → Tareas → Implementación → Pruebas).
> Aquí se define **qué** se construye y **por qué**. El **cómo** va en `plan.md`.
> No se escribe código hasta que esta especificación y el plan estén **Aprobados**.
> Las plantillas vacías están en `docs/sdd/plantillas/`. Mismo formato que las historias de Linear
> (Historia, Decisiones, Criterios Dado-Cuando-Entonces, Dependencia, Sin resolver, Fuentes).

| Campo | Valor |
| --- | --- |
| Estado | **Aprobada con las decisiones de Andrés del 11 nov 2026** (chat de la sesión) |
| Historia / ticket | NOT-129 `[EDT 8.3]` (Linear, ciclo 3, 5 puntos, prioridad Urgente, ya «In Progress») |
| Responsable | Andrés (backend/GIS) |
| Rama | `feature/not-129-api-mapa-calor-estadisticas` (desde `develop` en `2dd1345`) |
| Última actualización | 2026-11-11 |

## 1. Contexto y problema

El panel de administración ya lista reportes, cambia estados y fija la vigencia, pero su
**mapa usa datos de prueba**: las rutas del mapa, del mapa de calor y de las estadísticas
**no existen** en el backend (verificado en el código el 11 de nov). Katherine no puede conectar
el mapa de reportes (NOT-135) ni el mapa de calor y las estadísticas (NOT-136) hasta que esta API
exista, y esas dos historias bloquean las pruebas del mapa (NOT-140). Por eso NOT-129 está en la
**ruta crítica** del sprint. Ni NOT-135 ni NOT-136 están empezadas (Todo, sin ramas ni PR de
Katherine en GitHub al 11 nov).

Sostiene la función central («ubicar el reporte por comuna»): el administrador necesita ver
dónde se concentran los reportes y cuántos se han atendido.

## 2. Objetivo

Que un administrador con sesión pueda pedir, con los mismos filtros, (a) los reportes vigentes
como puntos del mapa, (b) los puntos del mapa de calor con la lista de comunas críticas y (c) las
estadísticas por estado, categoría y comuna, con datos reales y en menos de 2 s.

## 3. Usuarios e historias

- Como **administrador**, quiero ver en el mapa los reportes vigentes por zona y estado, para saber dónde hay problemas.
- Como **administrador**, quiero un mapa de calor y una lista de comunas críticas, para ver dónde se concentran más reportes.
- Como **administrador**, quiero totales por estado, categoría y comuna y el porcentaje de reportes atendidos, para saber cuántos se han atendido.
- Como **Katherine (frontend)**, quiero un contrato claro con ejemplos, para conectar NOT-135 y NOT-136 sin adivinar.

## 4. Decisiones de Andrés (11 nov 2026)

1. **Puntos críticos por comuna** (no por barrio): es la filosofía de la app. La lista es **paginada**.
2. **Peso del calor:** cada reporte cuenta y pesa 1; los reportes en el mismo punto se suman.
3. **Estadísticas con filtro de vigencia:** «vigentes» (dentro del plazo, menos de 30 días) y «fuera de plazo» (más de 30 días), además de «todos».
4. **Porcentaje de atendidos:** se llama **atendidos**, no «solucionados». Fórmula: reportes en estado Resuelto ÷ total de reportes del filtro.
5. **Mismos filtros** en las tres rutas (mapa, calor y estadísticas): comuna, categoría, estado y fechas.
6. **Tope de puntos:** es el límite de filas que la API devuelve por consulta (ver §6).
7. **Envío real:** queda fuera de esta historia; pasó a NOT-133 (la última de Andrés), después de la solicitud de información a la Alcaldía.
8. **Figma:** no se edita sin avisar a Andrés. Solo se podría ajustar si Katherine no lo implementó en GitHub y su tarea no está en progreso.

## 5. Requisitos

| ID | Requisito («el sistema deberá…») | Prioridad | Origen |
| --- | --- | --- | --- |
| RF-22 | Devolver los reportes **vigentes** con ubicación como marcadores, filtrables por comuna, categoría, estado y fechas. | Esencial | Linear NOT-129; contrato §5 |
| RF-23 | Devolver los puntos del mapa de calor con su peso y la lista paginada de comunas críticas, con los mismos filtros. | Esencial | Linear NOT-129; contrato §5 |
| RF-24 | Devolver totales por estado, por categoría y por comuna y el porcentaje de atendidos, con los mismos filtros y un filtro de vigencia. | Condicional (se entrega, porque lo piden NOT-129 y NOT-136) | Linear NOT-129; RF-24 |
| RN-20 | El mapa muestra solo reportes vigentes (`vigente_hasta > now()`); el listado del panel conserva los vencidos. | Regla | `docs/contexto/05`; Doc «Reglas de estados…» |
| RNF-03 | Las tres rutas exigen JWT con rol ADMIN (401 sin sesión, 403 a un ciudadano). | Esencial | `docs/contexto/04` |
| RNF-04 | Cada consulta responde en ≤ 2 s con 1.000 reportes de prueba. | Esencial | Linear NOT-129 |

Rutas previstas (ya nombradas en el contrato y en el Doc): `GET /api/admin/mapa`,
`GET /api/admin/mapa/calor`, `GET /api/admin/estadisticas`.

## 6. Criterios de aceptación

- **CA-01** — _Dado_ un administrador con sesión, _cuando_ pide el mapa con filtros por comuna, categoría, estado y fechas, _entonces_ recibe solo reportes vigentes con código, categoría, estado, barrio, comuna y coordenadas.
- **CA-02** — _Dado_ un reporte vencido (`vigente_hasta` ya pasó) o sin ubicación, _cuando_ se consulta el mapa, _entonces_ no aparece; el vencido sigue en `GET /api/admin/reportes`.
- **CA-03** — _Dado_ un administrador, _cuando_ pide el mapa de calor, _entonces_ recibe los puntos con su peso (cada reporte pesa 1 y los del mismo punto se suman) y, aparte, la lista de comunas críticas ordenada de más a menos reportes, con su comuna y su total.
- **CA-04** — _Dado_ que hay más comunas críticas que el tamaño de página, _cuando_ se pide la página siguiente, _entonces_ recibe las siguientes comunas en el mismo orden y los datos para paginar (página, tamaño y total).
- **CA-05** — _Dado_ un administrador, _cuando_ pide las estadísticas, _entonces_ recibe los totales por estado, por categoría y por comuna y el porcentaje de atendidos (Resuelto ÷ total).
- **CA-06** — _Dado_ el filtro de vigencia de las estadísticas, _cuando_ elige «vigentes», «fuera de plazo» o «todos», _entonces_ los totales cuentan solo esos reportes (vigentes: `vigente_hasta` en el futuro; fuera de plazo: ya vencido). Sin elegir, cuenta «todos».
- **CA-07** — _Dado_ un ciudadano o un usuario sin sesión, _cuando_ llama a estas rutas, _entonces_ recibe 403 `SIN_PERMISO` o 401 `NO_AUTENTICADO`, en el formato `{codigo, mensaje, campo}` del contrato.
- **CA-08** — _Dada_ una base con 1.000 reportes de prueba, _entonces_ cada una de las tres consultas responde en 2 s o menos.
- **CA-09** — _Dado_ el contrato de la API, _entonces_ `docs/contexto/11-contrato-api.md` incluye las tres rutas con parámetros, ejemplos de respuesta y errores.
- **CA-10** (error) — _Dado_ un filtro no válido (comuna fuera de 1 a 10, estado desconocido, fecha mal formada, `desde` > `hasta`, vigencia desconocida o página/tamaño fuera de rango), _entonces_ responde 400 `VALIDACION` con el campo, igual que `GET /api/admin/reportes`.
- **CA-11** — _Dado_ que ningún reporte cumple los filtros, _entonces_ las rutas responden 200 con listas vacías y totales en 0 (el porcentaje de atendidos es 0), sin error.
- **CA-12** — _Dado_ un reporte vigente cuya comuna no se pudo determinar, _entonces_ aparece en el mapa con `comuna` nula y se cuenta en un campo aparte (`sinComuna`), como muestra el Figma («0 con comuna sin determinar»).
- **CA-13** (tope) — _Dado_ que más de 1.000 reportes cumplen los filtros, _entonces_ el mapa y el calor devuelven como máximo 1.000, indican `total` (cuántos cumplen) y `truncado: true`, para que el panel avise «mostrando 1.000 de N».

## 7. Reglas y restricciones que aplican

- **Vigencia:** la base ya define el criterio en la vista `v_reporte_mapa` (`ubicacion IS NOT NULL AND vigente_hasta > now()`); las rutas aplican el mismo criterio en el servidor. «Vigente» se mide con `vigente_hasta` de cada reporte, no con «30 días» fijos: el plazo (hoy 30 días) lo fija `configuracion.vigencia_dias` y cada reporte guarda su fecha.
- **Comuna:** la asigna PostGIS al guardar el reporte (`fn_asignar_comuna`, guardada en `reporte.id_comuna` desde V20); no se recalcula en el frontend. La tabla `comuna` solo tiene el número (1 a 10), sin nombre. La capa y los 133 barrios son provisionales y el docente aceptó esa limitación.
- **Privacidad (Ley 1581):** no se devuelven datos personales del ciudadano. El mapa público sigue fuera del MVP.
- **Estados** (`EstadoReporte`): Recibido, Enviado, En trámite, Resuelto.
- **Formato** de error único, camelCase, fechas ISO-8601 (contrato §1 y §2). Fechas de filtro en hora de Colombia, ambas inclusive (igual que `/api/admin/reportes`).
- **Stack fijado:** Spring Boot + PostGIS, consultas con `NamedParameterJdbcTemplate` como el listado del panel. Sin migración nueva salvo que el plan la justifique (la siguiente sería V19).
- No toca correo ni `RESEND_PRUEBA_DESTINO`.

## 8. Fuera de alcance

- Las pantallas del mapa, del calor y de las estadísticas (NOT-135 y NOT-136, de Katherine).
- Editar el Figma o las historias de Linear de otras personas sin avisar a Andrés.
- Un mapa público o acceso de la Alcaldía/empresas al panel.
- Validación oficial de comunas y barrios con Planeación.
- Envío real de correo (NOT-133, tras la solicitud a la Alcaldía).
- Cambiar la vigencia de un reporte individual.

## 9. Preguntas abiertas

Las que quedaban las resolvió Andrés (§4). Quedan detalles que propongo en `plan.md` para su visto bueno:

- [PENDIENTE] Tamaño de página de las comunas críticas (propuesta: 5 por defecto, máximo 10).
- [PENDIENTE] Si «atendidos» debe contar solo Resuelto (lo dicho) o también En trámite (propuesta: solo Resuelto, como se dijo; se puede cambiar con una línea).
- [PENDIENTE] Cambios de texto en Linear (NOT-129 y NOT-136 hablan de «solucionados» y de «filtros» sin la vigencia) y en el Figma («Puntos críticos» lista barrios): **no se editan sin permiso de Andrés**.

## 10. Fuentes revisadas (11 nov 2026)

Linear NOT-129, 133, 135, 136 (conector); Google Doc (pestañas «Diseño técnico y arquitectura», «Reglas de negocio», «Requisitos funcionales…», «EDT/WBS»); Figma («Panel administrativo · Mapa de calor» y «· Reportes»); GitHub (PR y ramas); `docs/contexto/11-contrato-api.md` §5; migraciones V1, V2 y V13 (`reporte`, `comuna`, `v_reporte_mapa`, `v_reporte_comuna`); código (`ListadoReportesController`, `ListadoReportesRepositorio`, `ListadoReportesServicio`, `EstadoReporte`, `SecurityConfig`).

## 11. Aprobación

- [x] Especificación completa y decisiones abiertas resueltas por Andrés (11 nov).
- [ ] **Plan técnico aprobado por Andrés** (`plan.md`) → recién entonces se escribe código.
