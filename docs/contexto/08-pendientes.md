# Pendientes

Actualizado el 9 de noviembre de 2026 (Sprint 2 cerrado; Sprint 3 en curso). Esta lista antes
reflejaba la pestaña «Pendientes» del Google Doc y tenía muchos puntos ya resueltos; se rehízo.
Lo que está en Linear lleva su clave `NOT-n`. Estado general: [12-estado-actual-y-traspaso.md](12-estado-actual-y-traspaso.md).

## Dentro del Sprint 3 (último)

Detalle y dueños en [13-sprint-3.md](13-sprint-3.md).

- API y pantallas del mapa de reportes, del mapa de calor y de las estadísticas (NOT-129, 135, 136).
- Correo claro: matrícula solo para empresas, fecha y coordenadas legibles, sin filas vacías,
  fotos como miniaturas (NOT-130).
- Envío real a las entidades y aviso de rebotes; hoy todo sale a la bandeja de pruebas (NOT-131).
- Registro rápido (~10 s hoy), diagnóstico del error de Google en celular, límite de intentos de
  login, límite diario y duplicados R.06 (NOT-132).
- Despliegue final, respaldo de la base y administrador real (NOT-133).
- App instalable (PWA): no hay manifiesto ni service worker (NOT-134).
- Pulido de la app: servidor despertando, reintento de foto, GPS de 10 s, atribución del mapa,
  usabilidad, tono y accesibilidad (NOT-137, 138, 139).
- Pruebas del panel, de punta a punta y colección Postman (NOT-140, 141).
- Informe final, manuales y Google Doc al día (NOT-142).

## Fuera del equipo (no bloquean el MVP)

- **Validación jurídica del aviso de privacidad v3** (ADR-0008): vigente, pero su texto legal
  sigue sin validar con el docente o un asesor jurídico.
- **Barrios y comunas oficiales:** la capa de comunas (V14) es sintética y los 133 barrios (V17)
  son del PMGR 2020. El docente aceptó esta limitación; reemplazarlos es opcional con los
  scripts `backend/scripts/generar-capa-comunas.py` y `generar-barrios.py` (migración V19 en adelante).
- Confirmar con la Alcaldía/CAIC el tamaño máximo de adjuntos y el correo de Centroaguas;
  respuestas pendientes de Gases de Occidente y Veolia (ver [07](07-asignacion-de-correos.md)).

## Ya resuelto (no volver a abrir)

- Mecanismo de correo: Resend (ADR-0002); dominio `notiurba.com` (ADR-0007), verificado, y envío
  probado desde Render.
- Categorías 8 (riesgo de desastres) y 9 (vivienda) y la consulta sobre la Ley 1581: validadas.
- Stack desplegado en Netlify + Render + Neon (ADR-0004); fotos en Cloudinary (ADR-0006).
- Registro, ingreso, Google, recuperación, verificación de correo, perfil, eliminar datos y cuenta,
  «Mis reportes», «Actualizar mi reporte», panel con listado/filtros/estados/vigencia.
- Asignación de comuna en el backend (NOT-55, V13) y catálogo público de barrios.
- Se quitó el barrio «Administración NotiUrba» (V18). Nombres y apellidos solo con letras (2 a 60)
  y contraseña con mensajes claros (8 a 72 bytes).
- Textos legalistas suavizados; el paso 2 del reporte se muestra siempre; la ubicación es
  opcional para Vivienda.
- Orígenes CORS con comodín para las vistas previas de Netlify.

## Deuda técnica conocida

- `RutaProtegida` ya envuelve `/app` y `/admin`; el panel muestra datos de prueba en el mapa por
  comunas hasta conectar la API real (NOT-135).
- Mover o sacar del historial los archivos pesados del prototipo
  (`plano-p28-tulua-2015-300dpi.png`, ~39 MB, y el PDF, ~5 MB). Decisión aparte: reescribir historial.
- El panel «Diagnóstico de límites» (`admin/componentes/DiagnosticoLimites.jsx`) es interno:
  ocultarlo en producción.
- Normalizar tildes en los textos de la interfaz (parte de NOT-138).
- Colección Postman (`docs/api/`) sin los endpoints nuevos (NOT-141).
- Quitar de Render `MAIL_FROM` y `MAIL_TEST_TO` (sin uso) y rotar la clave de administrador
  de desarrollo (NOT-133).
- Pull request #27 de Amelia (`fix/registro-pr20`) quedó superada por el #30: contiene evidencias
  pesadas (`docs/validacion`, `output/`, PDF) y 11 conflictos con `develop`. Conviene cerrarla y
  rehacer solo lo útil en PR pequeños. Su prueba `CatalogoCargaBarriosIT` espera 135 barrios; son 133.
- Las diapositivas del Sprint 2: corregir «15 categorías» (la app ofrece 14) y la nota N.05 que
  dice que las categorías 8 y 9 no están validadas (NOT-142).

## Reglas de negocio por definir

- Valores del límite diario de reportes, intentos de login y distancia de duplicados (NOT-132).
- Pasar a las RN la regla de huecos y solapes de la capa de comunas (ya está en `fn_asignar_comuna`).

## Notas operativas que conviene recordar

- El puerto de Postgres local es **5433** (el de Andrés tenía otro contenedor en 5432).
- Auto-Deploy de Render redespliega con cada commit a la rama enlazada: un cambio de migración
  llega a Neon solo. Las migraciones aplicadas **no se editan** (checksum de Flyway).
- Una migración destructiva (como un `DROP COLUMN`) pide antes un branch de respaldo en Neon.
- Revisar que `.postman/`, `postman/` y los YAML no traigan secretos antes de un commit.
