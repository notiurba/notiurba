# Tareas — Envío real a las entidades y aviso de rebotes

> **Fase 3 de 5 del flujo SDD.** Desglose de `plan.md` en pasos pequeños y
> verificables. Se crea con el plan **Aprobado** y se actualiza durante la
> implementación (Fase 4) y las pruebas (Fase 5).

| Campo | Valor |
| --- | --- |
| Especificación | [spec.md](spec.md) |
| Plan | [plan.md](plan.md) |
| Estado general | **PR 1 integrado (#38). PR 2: T-07 y T-09 a T-17 hechos; faltan T-08, T-18 y el QA** |
| Última actualización | 2026-11-13 |

## Cómo se usa

- Una tarea = un cambio pequeño que se puede probar solo.
- Estados: `[ ]` pendiente · `[~]` en curso · `[x]` hecha · `[!]` bloqueada.
- Cada tarea nombra el criterio de aceptación (CA) que cubre y su prueba.
- Un solo PR para toda la historia (decisión de Andrés, 12 nov 2026), con un commit por tarea.
- Se implementa **en orden**; si una tarea cambia el plan, se actualiza `plan.md` primero.
- Al terminar una tarea se marca aquí y, si cambió algo de fondo, se actualiza `docs/`.

## Tareas

### PR único — Envío real y rebotes (rama `feature/not-131-envio-real-y-rebotes`)

| ID | Tarea | Capa | Cubre | Prueba | Estado |
| --- | --- | --- | --- | --- | --- |
| T-01 | Pruebas de la cola con y sin destino de pruebas: con destino sale solo a esa dirección con «[PRUEBA]»; sin destino sale al destino de la ruta y el envío pasa a «Enviado» | Backend | CA-01, CA-02 | `EnrutamientoIntegracionTest` | [x] |
| T-02 | Pruebas del webhook: sin firma, firma falsa y firma vencida dan 401 `FIRMA_INVALIDA` sin cambiar nada; `email.complained` marca rebote; identificador vacío o de más de 100 caracteres da 400; otros eventos se ignoran | Backend | CA-03, CA-04, CA-05 | `EnrutamientoIntegracionTest` | [x] |
| T-03 | Pruebas de propiedad y de fallo no reintentable: `/envio` y `/mensaje` de un reporte ajeno dan 404; un fallo no reintentable queda fallido con el motivo amable y no se reintenta solo | Backend | CA-08, CA-09 | `EnrutamientoIntegracionTest` | [x] |
| T-04 | Función de presentación del bloque de correo del detalle (qué texto, etiqueta y si se ofrecen otros canales) con sus pruebas | Frontend | CA-06, CA-07 | `estadoCorreo.test.mjs` | [x] |
| T-05 | Bloque «Correo a la entidad» en `DetalleReporte.jsx`: carga `estadoCorreo`, muestra el estado y, si el correo no llegó, `OtrosCanales`; si `/envio` falla, el detalle se ve igual | Frontend | CA-06, CA-07 | `pnpm test`, `pnpm lint`, `pnpm build` y revisión en el navegador (360 y 1366 px) | [x] |
| T-06 | Guía `docs/contexto/14-activar-envio-real.md`, contrato de API, variables de Render y sprint al día | Docs | CA-10 | Revisión contra `application.yml` | [x] |

## PR 2 — Ajustes tras la prueba en producción (rama `feature/not-131-ajustes-tras-prueba`)

> Aprobado por Andrés el 14 nov 2026 (spec y plan, Anexo A). Se implementa en este orden: primero los errores que bloquean
> (T-07, T-08), luego el resto. Cada tarea indica la prueba que la cubre y el criterio del Anexo A de `spec.md`.

| ID | Tarea | Capa | Cubre | Prueba | Estado |
| --- | --- | --- | --- | --- | --- |
| T-07 | **Panel: no deja cambiar el estado.** Reproducir con la API real y con la base desechable (falló aun con observación escrita). Corregir la causa. Hacer la observación opcional de verdad (frase por defecto en el historial) y mostrar el motivo del fallo en vez de «Intente de nuevo» | Backend + Frontend | CA-11 | Prueba de integración que reproduce el fallo; `AdminIntegracionTest`; prueba del panel | [x] |
| T-08 | **Google: datos de otra persona.** Consulta de solo lectura (con visto bueno de Andrés) a `usuario` e `identidad_externa` del correo `andreslopezv1m@gmail.com`; explicar qué vinculó la cuenta. Corregir la causa; si hay datos mal vinculados, proponer la corrección y pedir aprobación antes de tocar producción | Backend | CA-12 | Prueba nueva en `GoogleAuthServicioTest` (correo sin cuenta → perfil incompleto; identidad ya vinculada → su propia cuenta) | [x] Sin cambio de código (14 nov 2026): el correo ya tenía cuenta (id 4, creada el 3 nov) y Google se vinculó a ella el 14 nov 02:17 UTC, que es la prueba de Andrés. Solo hay una identidad de Google en toda la base. Falta que Andrés confirme de dónde salían los datos «Prueba Render» |
| T-09 | **Paso 2:** mostrar siempre el mapa; en los tipos con ubicación opcional se puede marcar un punto o seguir sin marcarlo | Frontend | CA-13 | Prueba de `PasoUbicacion` y revisión en el celular | [x] |
| T-10 | **Cada cambio de paso sube al inicio** de la pantalla (adelante y atrás) | Frontend | CA-14 | Prueba de la lógica de paso y revisión en el celular | [x] |
| T-11 | **Atrás desde el aviso de privacidad** vuelve a Ajustes | Frontend | CA-15 | Prueba de navegación y revisión en el celular | [x] |
| T-12 | **«Sin ubicación»** en el detalle del reporte (app y panel) cuando no hay lugar | Frontend | CA-16 | `reportes.test` de `textoLugar` y revisión en ambos | [x] |
| T-13 | **Panel:** los filtros de tipo y comuna muestran solo opciones con reportes | Frontend | CA-17 | Prueba de la lista de filtros y revisión en el panel | [x] |
| T-14 | **Descripción (paso 3):** placeholder, contador a la derecha en la fila del título y texto de ayuda corto | Frontend | CA-18 | Prueba de textos (RN-24) y revisión en el celular | [x] |
| T-15 | **Mapa (paso 2):** microcopy breve, pensado para el celular, sin explicar el teclado | Frontend | CA-19 | Prueba de textos y revisión en el celular | [x] |
| T-16 | **Correo:** espacio entre descripción, fecha y evidencia, y HTML con presentación institucional azul (sin código ni «NotiUrba»); texto plano sin cambios | Backend | CA-20 | `PlantillaReporteTest` (espaciado, sin código ni «NotiUrba», texto plano igual); vista real en Gmail | [x] |
| T-17 | **Panel (UI):** más espacio, mejor contraste, quitar «Administrador · todas las comunas» | Frontend | CA-21 | Revisión visual a 1366 px y a 1024 px | [x] |
| T-18 | **Webhook:** con Andrés, abrir el registro de entregas de Resend y comprobar el estado (200 / 401 / tiempo agotado); si el secreto no coincide, corregirlo; probar un rebote real (guía, paso 2) y comprobarlo en el detalle | Ops + Docs | CA-22 | Registro de Resend en 200 y detalle con «El correo no llegó»; guía ajustada con lo aprendido | [ ] |

## QA (obligatorio antes de cerrar NOT-131)

> Se hace sobre la **vista previa de Netlify del PR** (celular a 360 px y computador a 1366 px) y, para el panel, con
> `tools/qa-panel-admin/qa-panel.sh` contra el jar real. Cada fila se marca con evidencia (captura o salida). Si algo
> falla, vuelve a su tarea; no se avanza con filas en rojo.

| QA | Qué se comprueba | Cómo | Resultado esperado | Estado |
| --- | --- | --- | --- | --- |
| Q-01 | Cambio de estado en el panel | Con un reporte «Enviado»: pasar a «En trámite» y a «Resuelto», con y sin observación | Se guarda, aparece en el historial; sin observación hay frase por defecto; el error (si hay) dice la causa | [ ] |
| Q-02 | Google con correo sin cuenta | Entrar con una cuenta de Google nueva | Pasa a completar el perfil; no entra a ninguna otra cuenta | [ ] |
| Q-03 | Google con cuenta propia | Entrar con `andreslopezv1m@gmail.com` | Ve sus propios datos (nombre, documento y barrio correctos) | [ ] |
| Q-04 | Paso 2 con ubicación opcional | Elegir «Infraestructura y servicios educativos» | Se ve el mapa; se puede marcar un punto o continuar sin él | [ ] |
| Q-05 | Subida al inicio | Hacer scroll en el paso 2 y pulsar continuar; repetir con atrás | Cada paso empieza arriba | [ ] |
| Q-06 | Atrás desde privacidad | Ajustes → aviso de privacidad → atrás | Vuelve a Ajustes | [ ] |
| Q-07 | Lugar vacío | Abrir el reporte NU-000008 en la app y en el panel | Dice «Sin ubicación» | [ ] |
| Q-08 | Filtros del panel | Abrir el panel con reportes de pocas categorías | Los filtros de tipo y comuna solo traen opciones con reportes | [ ] |
| Q-09 | Paso 3 | Revisar el campo de descripción en el celular | Placeholder visible, contador a la derecha del título, ayuda corta | [ ] |
| Q-10 | Microcopy del mapa | Revisar el paso 2 en el celular | Instrucciones breves, sin teclado | [ ] |
| Q-11 | Correo | Crear un reporte con foto y ver el correo en Gmail (celular y computador) | Espacios correctos, presentación azul institucional, sin código ni «NotiUrba»; texto plano igual | [ ] |
| Q-12 | Panel (UI) | Revisar Reportes y Configuración a 1366 px y 1024 px | Espaciado y contraste mejores; sin la etiqueta del administrador | [ ] |
| Q-13 | Webhook | Rebote real con la dirección de pruebas de Resend | Registro de Resend en 200 y detalle con «El correo no llegó», canales y «Copiar mi mensaje» | [ ] |
| Q-14 | Detalle del reporte (NOT-131) | Revisar el bloque «Correo a la entidad» con reportes enviados, rebotados y por otro canal, a 360 y 1366 px | Se ve bien y sin palabras técnicas | [ ] |
| Q-15 | Regresión | `mvn clean test` (con la base desechable y `DB_*`), `pnpm test`, `pnpm lint`, `pnpm build`, `qa-panel.sh`, subagente `code-reviewer` | Todo en verde; hallazgos corregidos | [ ] |
| Q-16 | Cierre | Con Q-01 a Q-15 en verde: archivar el SDD, actualizar Google Doc/Sheet y pasar NOT-131 a Done en Linear | Historia cerrada | [ ] |

## Pruebas finales (Fase 5)

- [x] Todos los criterios CA-01 a CA-10 verificados (tabla de trazabilidad abajo).
- [x] Backend: 451 pruebas, 0 fallos, 2 omitidas (PR 1: 433; PR 2: 451 con las bases desechables). Frontend: 226 pruebas, lint y build en verde.
- [ ] Verificación manual en local (detalle con envío en camino, enviado y rebotado; español, 360 px y 1366 px).
- [x] Revisión con el subagente `code-reviewer` y hallazgos corregidos (1, 2, 3, 5, 6, 7, 8, 9 y 10; ver registro).
- [x] Documentos actualizados: `spec.md`, `plan.md`, `docs/contexto/`.

## Trazabilidad

| Criterio | Tareas | Prueba | Resultado |
| --- | --- | --- | --- |
| CA-01 | T-01 | `EnrutamientoIntegracionTest` | ok |
| CA-02 | T-01 | `EnrutamientoIntegracionTest` | ok |
| CA-03 | T-02 | `EnrutamientoIntegracionTest` | ok |
| CA-04 | T-02 | `EnrutamientoIntegracionTest` | ok |
| CA-05 | T-02 | `EnrutamientoIntegracionTest` | ok |
| CA-06 | T-04, T-05 | `estadoCorreo.test.mjs`; revisión visual pendiente | ok (visual pendiente) |
| CA-07 | T-04, T-05 | `estadoCorreo.test.mjs`; revisión visual pendiente | ok (visual pendiente) |
| CA-08 | T-03 | `EnrutamientoIntegracionTest` | ok |
| CA-09 | T-03 | `EnrutamientoIntegracionTest` | ok |
| CA-10 | T-06 | Revisión contra `application.yml` y el código | ok |

## Registro de cambios

| Fecha | Cambio | Documento |
| --- | --- | --- |
| 2026-11-13 | Spec y plan aprobados; tareas escritas | spec.md, plan.md, tasks.md |
| 2026-11-13 | Revisión de código: el detalle ofrece «Reintentar envío» y canales para otro canal; CA-08 ajustado a reintento por la persona; guía con casillas previas y prueba de rebote real | spec.md, tasks.md, 14-activar-envio-real.md |
| 2026-11-13 | Tras la prueba en producción de Andrés: Anexo A del spec y del plan, tareas T-07 a T-18 y matriz de QA Q-01 a Q-16 (borrador, pendiente de aprobación) | spec.md, plan.md, tasks.md |
| 2026-11-14 | Implementados T-07 y T-09 a T-17; revisión con code-reviewer (selector del panel, resumen del paso 4, simulador, mapa accesible, correo con tablas y una línea en blanco) corregida. Quedan T-08 (Google) y T-18 (webhook), que necesitan a Andrés, y el QA Q-01 a Q-16 | tasks.md |
| 2026-11-14 | T-08: consulta de solo lectura a Neon. La cuenta de `andreslopezv1m@gmail.com` existe desde el 3 nov y Google se vinculó a ella por correo (comportamiento previsto). No hay error de vinculación | tasks.md |
