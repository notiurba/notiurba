# Estado actual y traspaso (9 de noviembre de 2026)

> Para quien llegue nuevo (persona o IA): lea este archivo primero. Resume qué funciona, qué
> falta, cómo se corre y se prueba todo, y qué se aprendió trabajando con Andrés. El detalle
> técnico sigue en los demás archivos de `docs/contexto/`. Si algo de aquí contradice al código,
> manda el código: corrija este archivo.

Foto tomada con `develop` en el commit `2c39a3b` (PR #31 mezclado). El Sprint 2 está cerrado
y presentado al docente; el Sprint 3 (10 al 29 de noviembre) es el **último**. Ver
[13-sprint-3.md](13-sprint-3.md).

## 1. Qué es y dónde vive

- PWA de reporte ciudadano georreferenciado de Tuluá. Repo: `notiurba/notiurba` (GitHub).
- Frontend: https://notiurba.netlify.app (Netlify; vistas previas en
  `deploy-preview-N--notiurba.netlify.app`). Despliega desde `develop`.
- Backend: https://notiurba-api.onrender.com (Render Free: se duerme y tarda 60 a 90 s en
  despertar; despliega solo con cada commit a la rama enlazada).
- Base de datos: Neon (PostgreSQL 16 + PostGIS). Fotos: Cloudinary. Correo: Resend con el
  dominio `reportes.notiurba.com`. Ingreso con Google: Google Identity Services.
- Gestión: Linear (equipo «NotiUrba», proyecto «NotiUrba - mvp», claves `NOT-n`). Documento
  formal: Google Doc de planeación. Resumen de contexto para humanos: vault de Obsidian de
  Andrés (`wiki/proyectos/noti-urba/`).

## 2. Qué funciona hoy (verificado en producción o con pruebas)

- Registro con correo y contraseña (con verificación de correo), ingreso, ingreso y registro
  con Google, recuperar contraseña (correo real por Resend), perfil (ver y editar),
  eliminar mis datos y eliminar mi cuenta (también borra las fotos de Cloudinary).
- Crear reporte en 4 pasos: categoría, ubicación (GPS o tocando el mapa; opcional para
  Vivienda), detalles con hasta 3 fotos y confirmación con el código `NU-000001`.
  El paso 2 se muestra siempre. 14 categorías (10 de Alcaldía + 4 de empresas). Gas, energía y aseo
  no reciben correo: tras el paso 1 van a «Cómo reportar a {entidad}» con sus canales oficiales y no se
  crea reporte (NOT-130). El paso 4 muestra la vista previa del mensaje («Así llegará su mensaje»).
- Asignación de comuna en PostGIS (`fn_asignar_comuna`): contiene, solape (menor número),
  hueco cercano (hasta 50 m) y `FUERA_DE_COBERTURA` (422).
- «Mis reportes», detalle, «Actualizar mi reporte» (nota en el historial; no cambia el
  estado oficial) y consulta del estado del envío por correo.
- Panel de administración: listado con filtros (estado, categoría, comuna, fechas, «sin
  respuesta 10 días»), cambio de estado con observación, vigencia configurable (30 días) y
  un mapa por comunas que **todavía usa datos de prueba** en el frontend. La API real del mapa,
  del mapa de calor (con comunas críticas) y de las estadísticas ya existe en el backend
  (`/api/admin/mapa`, `/mapa/calor`, `/estadisticas`; contrato §5.2 a §5.4).
- Envío por correo: cola persistente con reintentos y enrutamiento por categoría
  (`EnvioReporteServicio`, `EnrutamientoServicio`, `PlantillaReporte`). Está **apagado por
  defecto** y en producción todo sale a la bandeja de pruebas (ver §5). Desde NOT-130 el correo es
  una carta formal en primera persona (plantillas A, B y C, sin «NotiUrba» ni código) y solo se envía
  al CAIC y a Centroaguas (`info@`). Si rebota o falla, la pantalla final ofrece los canales y «Copiar
  mi mensaje». Probado el 12/11/2026 con correos reales a la bandeja de pruebas (Gmail, 360 px y escritorio).
- Catálogos públicos: tipos de documento y 133 barrios históricos (PMGR 2020).

## 3. Qué falta (lo que cubre el Sprint 3)

| Falta | Historia en Linear |
| --- | --- |
| ~~Rutas del mapa, del mapa de calor y de estadísticas~~ hechas en la rama `feature/not-129-api-mapa-calor-estadisticas` (falta mezclar en `develop`) | NOT-129 |
| Mapa de reportes y mapa de calor del panel con datos reales | NOT-135, NOT-136 |
| Correo a la entidad confuso (matrícula siempre, fecha cruda, coordenadas largas, filas vacías, fotos solo como enlaces) | NOT-130 |
| Envío real a las entidades, rebotes (falta `RESEND_WEBHOOK_SECRET`) | NOT-131 |
| Registro lento (~10 s), error de Google en celular, límite de intentos de login, límite diario y duplicados (R.06) | NOT-132 |
| Despliegue final, respaldo de la base, administrador real (hoy usa `ADMIN_DEV_PASSWORD`) | NOT-133 |
| La app no es PWA instalable (no hay manifiesto ni service worker) | NOT-134 |
| Pulido: «servidor despertando», reintento de foto, GPS 10 s, atribución OSM, tono y accesibilidad | NOT-137, NOT-138, NOT-139 |
| Pruebas del panel, de punta a punta y colección Postman al día | NOT-140, NOT-141 |
| Informe final y manuales | NOT-142 |

Fuera del equipo: validación jurídica del aviso de privacidad. Barrios y comunas son
provisionales y **el docente aceptó esa limitación** (no se espera validación de Planeación).

## 4. Cómo correr y probar

### Backend (`backend/`, Java 21, Maven)

- Base local: `docker compose up -d` (puerto **5433**, ver `.env.example`). Variables de
  entorno en `.env.example`; nunca se suben los valores.
- Pruebas: `mvn clean test`. Use `clean` al cambiar de rama (un `target/` viejo trae migraciones
  de otra rama y rompe Flyway). Las pruebas de integración usan `BaseDeDatosTemporal` y
  necesitan `DB_URL`, `DB_USER` y `DB_PASSWORD`; las de administración de Amelia necesitan
  `NOTIURBA_TEST_DB_URL` y una base `notiurba_test_admin_bootstrap` en `127.0.0.1`.
- `MigrationIntegrationTest` falla si las variables `DB_*` del `.env` están exportadas en el
  mismo comando: ejecútelo en un entorno limpio (`env -i ...`).
- Una base desechable: contenedor `postgis/postgis:16-3.4`. Docker Desktop debe estar abierto.
- Última corrida completa (12/11/2026, rama de NOT-130): 360 pruebas, 0 fallos, 2 omitidas
  (proveedores reales). Las 9 de enrutamiento y la de migración de enrutamiento solo corren con
  `NOTIURBA_TEST_DB_URL` apuntando a una base `notiurba_test*` (con `postgis` y `pgcrypto`); sin ella
  se omiten y no cuentan como pasadas.
- QA de punta a punta del panel contra el jar real: `tools/qa-panel-admin/qa-panel.sh` (44 verificaciones).
- QA de punta a punta del mensaje a la entidad: `tools/qa-correo-reporte/qa-correo.sh` (40 verificaciones;
  no envía correos).
- Errores de la API: formato único `{codigo, mensaje, campo}` (`ApiException`; en pruebas use
  `getReason()`).

### Frontend (`frontend/`, React 19 + Vite 8, pnpm)

- `pnpm install`, `pnpm dev`, `pnpm test` (node --test, 156 pruebas), `pnpm lint`, `pnpm build`.
  No use npm ni suba `package-lock.json`.
- Con `VITE_USAR_MOCKS=true` en desarrollo la API se simula (`src/simulacion/`); el build de
  producción no incluye esa carpeta.

## 5. Producción: variables y reglas de oro

- Render: `DB_URL`, `DB_USER`, `DB_PASSWORD`, `JWT_SECRET`, `ADMIN_DEV_PASSWORD`,
  `GOOGLE_CLIENT_ID`, `CORS_ORIGINS` (`https://notiurba.netlify.app,https://deploy-preview-*--notiurba.netlify.app`),
  `RECUPERACION_ENLACE`, `VERIFICACION_ENLACE` (`https://notiurba.netlify.app/app/verificar-correo`),
  `RESEND_API_KEY`, `RESEND_REMITENTE`, `RESEND_PRUEBA_DESTINO`, `NOTIURBA_ENVIO_REPORTES_HABILITADO`,
  `CLOUDINARY_*`. `MAIL_FROM` y `MAIL_TEST_TO` son restos sin uso. `RESEND_WEBHOOK_SECRET` **no está puesta**.
- **`RESEND_PRUEBA_DESTINO` debe seguir puesta** mientras dure la etapa de pruebas: con valor,
  el correo de cada reporte sale a la bandeja de pruebas de Andrés (asunto «[PRUEBA]») y no a
  la entidad real. Quitarla exige autorización del equipo y del docente (historia NOT-131).
- Netlify: `VITE_API_URL` y `VITE_GOOGLE_CLIENT_ID`.
- Las migraciones aplicadas **no se editan**: se corrige con una migración nueva. Hoy van
  V1 a V20 en `develop` y V21 en la rama de NOT-130 (la siguiente es V22).

## 6. Datos de negocio que se confunden fácil

- **CAIC** = servicio al ciudadano de la Alcaldía de Tuluá. Recibe los reportes de las
  categorías 1 a 10 y los redirige a la dependencia competente (`servicioalciudadano@tulua.gov.co`).
  Las empresas de servicios públicos (Centroaguas, gas, energía, aseo) son entidades aparte: gas
  va al correo de GDO; Centroaguas, energía y aseo usan un canal alterno sin correo; las
  emergencias nunca salen por correo.
- La **matrícula del servicio** (a veces llamada «código de contrato») solo aplica a empresas
  de servicios públicos. No debe aparecer en un reporte a la Alcaldía.
- Tope de **3 fotos** por reporte (backend `FotoServicio.MAXIMO_FOTOS` y frontend `MAX_FOTOS`).
- Categorías 8 (riesgo de desastres) y 9 (vivienda) y la consulta de la Ley 1581 ya están
  validadas por el equipo. Las diapositivas y algunos textos todavía dicen 15 categorías y
  «sin validar»: se corrigen en NOT-142.
- Barrios: 133 del PMGR 2020 (V17), provisionales. Capa de comunas: sintética (V14).

## 7. Problemas conocidos y lo que se averiguó

- «No pudimos comunicarnos con el servidor» sale **solo** cuando el navegador no recibe
  respuesta (`fetch` falla): Render dormido, mala red o CORS. No es un error de lógica. El ingreso
  con Google falló así en un celular; desde otro equipo `/api/auth/google` respondía bien (CORS
  correcto y 401 ante un token falso). La causa no está confirmada.
- El registro tarda ~10 s: BCrypt con poca CPU y el correo de verificación se envía dentro de la
  transacción de `RegistroServicio.registrar` (`VerificacionServicio.emitirYEnviar`).
- La categoría 15 («Solicitud de información») está activa sin ruta de envío: crear un reporte
  funciona, pero la vista previa responde 400. Decide el equipo: desactivarla o darle ruta al CAIC.
- El rebote real llega después por el webhook (NOT-131): mostrar canales y «Copiar mi mensaje» en el
  detalle del reporte queda para esa historia.
- **Canales (V21, 12/11/2026):** cada canal guarda finalidad, orden, horario, fuente y fecha de
  verificación. Uno que no sirve no se borra: queda inactivo con el motivo. Revíselos cada cierto
  tiempo (cambian sin aviso).
- `PlantillaReporte.enlaceSeguro` solo admite enlaces `res.cloudinary.com` y `www.openstreetmap.org`.
- **Comuna guardada en el reporte (V19 y V20, 12/11/2026):** antes la comuna se calculaba en cada
  lectura (`v_reporte_comuna`) y un punto en un hueco o fuera de la capa costaba ~7 ms. Con 10.000
  reportes las estadísticas tardaban 31,5 s y el mapa filtrado 16 s; además PostGIS declara costos
  altos y PostgreSQL compilaba esas consultas con JIT (1,2 s para 13 reportes), por eso se apagaba en la
  transacción. Ahora un trigger llena `reporte.id_comuna` y la recalcula cuando cambian el punto, el
  barrio, la capa o la distancia máxima. Con 10.000 reportes: estadísticas 12 ms, mapa filtrado 2 ms,
  crear los 10.000 reportes 4,5 s y recalcularlos todos 2,6 s. El costo estimado de las consultas quedó
  en 1.000 a 2.300 (el JIT empieza en 100.000), así que se quitó el apagado del JIT. **Neon:** `jit = on`
  pero `pg_jit_available() = false` (no tiene LLVM): en producción nunca hubo JIT. Antes de desplegar
  V19 y V20, branch de respaldo en Neon (no borran nada; V20 llena la comuna de los reportes que hay).
  **Ojo al cambiar la capa, la comuna de un barrio o la distancia máxima:** el recálculo bloquea la
  creación de reportes hasta terminar (2,6 s con 10.000), para que ninguno quede con la regla vieja.
  `id_comuna` y `comuna_origen` no se escriben a mano: el trigger las vuelve a calcular.

## 8. Cómo se trabaja con Andrés (aprendido)

- Todo en **español claro**, frases cortas, también en las preguntas.
- Trabajo en ramas `feature/*` o `docs/*`, PR hacia `develop`, **sin líneas de coautoría de
  Claude** en commits ni PR. Prefiera PR pequeños y separados. No mezcle nada que cambie datos
  de producción sin que Andrés lo apruebe.
- Lo que toca pantallas de Katherine (frontend) se señala expresamente para su revisión.
- No cierre decisiones abiertas por su cuenta: pregunte o déjelas marcadas «por decidir».
- Nunca imprima secretos. No cree historias en Linear sin su visto bueno.
- Historias de Linear: máximo 5 por persona, sin subtareas, puntos Fibonacci, criterios en
  Dado–Cuando–Entonces. Plantilla: [13-sprint-3.md](13-sprint-3.md).
- Flujos repetibles documentados como skills: `linear-historias-sprint` y `traspaso-de-sesion` (globales, en
  `~/.claude/skills/`), `notiurba-pruebas-y-despliegue` (en `.claude/skills/` de este repo) y `auditar-pr`
  (global, ya existía). Copias de los globales en [../skills-globales/](../skills-globales/).
