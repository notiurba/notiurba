# Tareas — Registro e ingreso rápidos y sin abusos

> **Fase 3 de 5 del flujo SDD.** Desglose de `plan.md` en pasos pequeños y
> verificables. Se crea con el plan **Aprobado** y se actualiza durante la
> implementación (Fase 4) y las pruebas (Fase 5).

| Campo | Valor |
| --- | --- |
| Especificación | [spec.md](spec.md) |
| Plan | [plan.md](plan.md) |
| Estado general | **Backend y frontend hechos (T-08 a T-25); faltan las pruebas finales y el cierre** |
| Última actualización | 2026-11-13 |

## Cómo se usa

- Una tarea = un cambio pequeño que se puede probar solo.
- Estados: `[ ]` pendiente · `[~]` en curso · `[x]` hecha · `[!]` bloqueada.
- Cada tarea nombra el criterio de aceptación (CA) que cubre y su prueba.
- **Un solo PR** para la historia (rama `feature/not-132-registro-rapido-y-sin-abusos` desde `develop`), con un commit atómico por tarea (decisión de Andrés, 12 nov).
- Se implementa **en orden: Figma → backend → frontend → pruebas**. Sin el visto bueno de Andrés a las pantallas (T-07) no empieza el backend.
- Si una tarea cambia el plan, se actualiza `plan.md` primero. Al terminar una tarea se marca aquí.
- Revisión de código (`code-reviewer`) solo sobre lo que cambió en la rama; el QA completo se hace al final de todas las historias (Andrés, 13 nov).

## Tareas

### Figma — sin PR; puerta: visto bueno de Andrés

| ID | Tarea | Capa | Cubre | Prueba | Estado |
| --- | --- | --- | --- | --- | --- |
| T-01 | F-1 «Panel · Configuración» (escritorio): Ingreso, Reportes, Duplicados con tabla por categoría; estados cargando, valores, error de rango, guardado, error del servicio | Figma | CA-30, CA-31 | capturas a Andrés | [x] marcos «F-1 · Panel · Configuracion» y «F-1b · … (error de rango)», página Flujo; el menú del mock muestra Reportes, Mapa de calor y Configuración (falta la pestaña Estadísticas) |
| T-02 | F-2 «Ingreso · todos los mensajes» (celular): una variante por situación, bloqueo con cuenta regresiva y «le queda 1 intento» | Figma | CA-32, CA-35, CA-40 | capturas a Andrés | [x] 5 marcos «F-2 · MÓVIL 360 · …» en la página «Login · 360 y 1366»: datos incorrectos con 1 intento, ingreso bloqueado, NotiUrba no responde, sin internet y falla con Google (correo sin confirmar y sesión vencida ya existían) |
| T-03 | F-3 «Confirmación · límite diario alcanzado» | Figma | CA-33 | capturas a Andrés | [x] hoja «F-3 · Modal · Límite diario alcanzado» (botones «Entendido» y «Ver mis reportes») |
| T-04 | F-4 «Confirmación · posible duplicado» como hoja de confirmación, 4 variantes de texto | Figma | CA-34, CA-46 a CA-49 | capturas a Andrés | [x] 4 hojas «F-4 · Modal · Posible duplicado (…)»: otra persona ya enviado, varios parecidos, propio con enlace «Ver el reporte», parecido aún sin enviar |
| T-05 | F-5 «Reportar · descripción y fotos»: ayuda siempre visible, foto reciente y «obligatoria» | Figma | CA-45, CA-50 | capturas a Andrés | [x] 2 marcos «F-5 · Reportar · …» (foto opcional y foto obligatoria); la casilla queda sin texto de ejemplo y la guía va como ayuda visible |
| T-06 | F-6 «Confirmación · foto necesaria» | Figma | CA-44 | capturas a Andrés | [x] marco «F-6 · Confirmación · Foto necesaria» (aviso ámbar y envío apagado) |
| T-07 | **Validación de Andrés** de T-01 a T-06 (con el Design System); sin ella no se empieza el backend | Figma | — | visto bueno escrito | [x] visto bueno de Andrés el 13 nov 2026 |

**Pendientes del diseño (a ajustar con Andrés al validar):** el ícono de las hojas F-3 y F-4 sigue siendo el sobre de «enviar» (debe ser uno de alerta o información); F-1 solo tiene dos tarjetas con una muestra de campos (hay 10 categorías) y no tiene el estado «guardado»; el marco «F-4 · … aún sin enviar» quedó encima de otro marco del tablero; los nombres de algunos marcos muestran mal la tilde. Textos a pulir solo con Andrés.

### Grupo 1 — Base de datos

| ID | Tarea | Capa | Cubre | Prueba | Estado |
| --- | --- | --- | --- | --- | --- |
| T-08 | `V22__limites_y_duplicados.sql`: 5 parámetros con `CHECK`, `distancia_duplicado` (10 filas), `intento_login`, `reporte.requiere_evidencia` | DB | CA-16, CA-21, CA-28 | prueba de migración: valores iniciales, rangos rechazados, 10 filas | [x] |

### Grupo 2 — Base común del backend

| ID | Tarea | Capa | Cubre | Prueba | Estado |
| --- | --- | --- | --- | --- | --- |
| T-09 | `ErrorApi` con `datos` opcional, `ApiException` lo acepta, tres códigos y mensajes en `Codigos`; bean `Clock`; ejecutor asíncrono acotado | Backend | CA-24 | formato `{ codigo, mensaje, campo, datos }`; `datos` ausente si no existe | [x] |
| T-10 | `ParametrosServicio` y `ParametrosController` (`GET`/`PUT /api/admin/configuracion/limites`) con rangos y auditoría | Backend | CA-26 a CA-29 | integración: GET, PUT válido, fuera de rango, todo o nada, 401/403 | [x] |

### Grupo 3 — Registro rápido y Google

| ID | Tarea | Capa | Cubre | Prueba | Estado |
| --- | --- | --- | --- | --- | --- |
| T-11 | Correo de verificación después del commit y en otro hilo; `reenviar` por la misma ruta | Backend | CA-01 a CA-04 | integración con envío de 8 s: responde en < 2 s; sin correo si el registro falla; reenvío funciona | [x] |
| T-12 | Log por paso en `GoogleAuthServicio`, sin datos personales | Backend | CA-05, CA-06 | captura del log: sin correo, nombre, documento, token ni `sub` | [x] |
| T-13 | Diagnóstico del fallo de Google en el celular: preflight CORS, arranque de Render, `GOOGLE_CLIENT_ID`; resultado en `12-estado-actual-y-traspaso.md` | Docs | CA-07 | documento | [x] |

### Grupo 4 — Límite de intentos de login

| ID | Tarea | Capa | Cubre | Prueba | Estado |
| --- | --- | --- | --- | --- | --- |
| T-14 | `IntentosLoginServicio` (`UPSERT` atómico, normalización, limpieza) y su uso en `AuthController.login`, con `intentosRestantes` y `Retry-After` | Backend | CA-08 a CA-13, CA-40 | integración con reloj fijo; correo inexistente = solo-Google = contraseña mala; 20 hilos | [x] |
| T-15 | Restablecer contraseña levanta el bloqueo | Backend | CA-37 | integración: bloquear, restablecer, ingresar | [x] |

### Grupo 5 — Límite diario, duplicados y evidencia

| ID | Tarea | Capa | Cubre | Prueba | Estado |
| --- | --- | --- | --- | --- | --- |
| T-16 | `LimiteDiarioServicio` y bloqueo de la fila del usuario en `ReporteServicio.crear` | Backend | CA-14 a CA-18, CA-23, CA-39 | integración: 2 pasan y el 3.º no; cambio de valor sin reiniciar; día de Bogotá; 2 hilos | [x] |
| T-17 | `DuplicadoServicio` entre todas las cuentas (misma categoría, cercanía, 30 días), datos base y resumen, `confirmarDuplicado` | Backend | CA-19 a CA-22, CA-48, CA-49 | integración PostGIS: bordes de distancia y de 30 días, propio y ajeno, privacidad del cuerpo | [x] |
| T-18 | Evidencia: `repiteResuelto`, `totalFotos`, `requiere_evidencia`, `EVIDENCIA_REQUERIDA` al crear y al enviar, `requiereEvidencia` en la vista previa | Backend | CA-41 a CA-44 | integración: sin foto 422, con foto marcado, sin foto real no se envía, vista previa sin escribir | [x] |
| T-19 | Contrato de API: endpoints, errores y `datos` en `11-contrato-api.md` (estado *PLANEADO* al aprobar el plan, *IMPLEMENTADO* al cerrar) | Docs | CA-24 | revisión | [x] |

### Grupo 6 — Frontend

| ID | Tarea | Capa | Cubre | Prueba | Estado |
| --- | --- | --- | --- | --- | --- |
| T-20 | `ErrorApi` conserva `datos`; `textos/ingreso.js`; `errorLogin.js` por código y estado; `cliente.js` sin la palabra «servidor» | Frontend | CA-35, CA-36 | `errorLogin.test.mjs`; prueba de lenguaje de RN-24 | [x] `errorLogin.test.mjs` (14), `cliente.test.mjs` con `datos` |
| T-21 | `Login.jsx` y `BotonGoogle.jsx`: mensaje por situación, bloqueo con cuenta regresiva, «le queda 1 intento» | Frontend | CA-32, CA-35, CA-40 | componente con errores simulados | [x] `AvisoIngreso.test.mjs`; manual con simulación a 360 px (bloqueo, cuenta regresiva, «le queda 1 intento»); se arregló el doble Enter que gastaba intentos |
| T-22 | `PasoDetalles.jsx`: ayuda siempre visible, foto reciente y «obligatoria» | Frontend | CA-45, CA-50 | componente | [x] manual con simulación a 360 px; textos en `textos/reporte.js` con prueba |
| T-23 | `PasoConfirmacion.jsx`: hoja de duplicado, aviso de límite diario, aviso de foto necesaria; «Cancelar reporte» común | Frontend | CA-33, CA-34, CA-44, CA-46, CA-47 | componente: textos por variante, «Enviar de todas maneras», «Cancelar reporte» | [x] `reporte.test.mjs` (variantes); manual con simulación: parecido → «Enviar de todas maneras», límite del día, foto necesaria → «Agregar foto» |
| T-24 | Pantalla `Configuración` del panel, servicio, ruta y enlace | Frontend | CA-30, CA-31 | componente y manual contra el backend local | [x] `limites.test.mjs`; manual con simulación a 1366 px (rango y guardado). Falta probarla contra el backend local (QA final) |
| T-25 | Datos simulados para los casos nuevos | Frontend | CA-32 a CA-34 | modo `VITE_USAR_MOCKS` | [x] `simulacion.test.mjs` cubre los casos nuevos; disparadores en `simulacion/README.md` |

## Pruebas finales (Fase 5)

- [ ] Todos los criterios CA-01 a CA-50 verificados (tabla de trazabilidad abajo).
- [x] Backend: `mvn clean test` en verde (424 pruebas, 0 fallos, 12 omitidas). Frontend: `pnpm test` (202), `pnpm lint` y `pnpm build` en verde.
- [ ] `tools/qa-limites/`: registro, login bloqueado, 3 reportes en un día, duplicado, evidencia y ajustes del administrador contra el backend local.
- [ ] Medición del registro (antes y después) y `EXPLAIN` de la búsqueda de duplicados.
- [x] Revisión con `code-reviewer` solo sobre lo que cambió: backend (3 fallos medios corregidos con pruebas; el más serio, una ráfaga de logins comparaba 20 contraseñas y ahora compara 3) y frontend (2 fallos medios y faltas de pruebas corregidos; 202 pruebas). Queda [PENDIENTE] el texto «sin internet» (plan, sección 7).
- [ ] Verificación manual en local (360 px y 1366 px, todo en español, sin palabras técnicas).
- [ ] Documentos actualizados: `spec.md`, `plan.md`, `docs/contexto/` (04, 05, 08, 10, 11, 12, 13) y cierre con la skill `cierre-de-historia`.

## Trazabilidad

| Criterio | Tareas | Prueba | Resultado |
| --- | --- | --- | --- |
| CA-01 a CA-04 | T-11 | integración del registro | verificado en backend |
| CA-05, CA-06 | T-12 | captura del log | verificado en backend |
| CA-07 | T-13 | documento | documentado en `12-estado-actual-y-traspaso.md` |
| CA-08 a CA-13, CA-40 | T-14, T-21 | integración del login; componente | pendiente |
| CA-14 a CA-18, CA-23, CA-39 | T-16 | integración del límite diario | verificado en backend |
| CA-19 a CA-22, CA-48, CA-49 | T-17 | integración PostGIS | verificado en backend |
| CA-24 | T-09, T-19 | formato del error; contrato | verificado en backend |
| CA-25 | T-14, T-16 | reloj de prueba | verificado en backend |
| CA-26 a CA-29 | T-10 | integración del administrador | verificado en backend |
| CA-30, CA-31 | T-01, T-24 | Figma; componente | pendiente |
| CA-32 | T-02, T-21 | Figma; componente | pendiente |
| CA-33 | T-03, T-23 | Figma; componente | pendiente |
| CA-34, CA-46, CA-47 | T-04, T-23 | Figma; componente | pendiente |
| CA-35, CA-36 | T-20, T-21 | pruebas de mensajes y de lenguaje | pendiente |
| CA-37 | T-15 | integración de recuperación | verificado en backend |
| CA-41 a CA-44 | T-06, T-18, T-23 | integración; componente | pendiente |
| CA-45, CA-50 | T-05, T-22 | Figma; componente | pendiente |

## Registro de cambios

| Fecha | Cambio | Documento |
| --- | --- | --- |
| 2026-11-13 | Tareas escritas con el plan aprobado; empieza Figma | tasks.md |
| 2026-11-13 | Figma hecho (T-01 a T-06); falta el visto bueno de Andrés | tasks.md |
| 2026-11-13 | Figma validado por Andrés (T-07); backend hecho (T-08 a T-18) con 420 pruebas en verde; contrato de API al día (T-19) | tasks.md, 11-contrato-api.md |
| 2026-11-13 | Revisión de código del backend: el login cuenta el intento antes de comparar la contraseña; frontend hecho (T-20 a T-25) | tasks.md, plan.md |
