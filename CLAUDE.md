# NotiUrba — guía para trabajar con Claude Code

NotiUrba es una PWA de reporte ciudadano georreferenciado para la zona urbana de
Tuluá (Valle del Cauca). El ciudadano crea un reporte con foto y GPS; el sistema
lo clasifica en una de las **10 comunas urbanas** y lo hace llegar por correo a la
entidad competente de la Alcaldía o a la empresa de servicios públicos. Proyecto
académico de la materia Gestión de Proyectos (29 sep – 13 dic 2026).

## Cómo usar este repositorio con Claude Code

1. Lee siempre primero `docs/contexto/12-estado-actual-y-traspaso.md` (qué funciona, qué falta, cómo se corre y
   se prueba, cómo se trabaja con Andrés), luego `docs/README.md` y el resto de `docs/contexto/`. El sprint en
   curso está en `docs/contexto/13-sprint-3.md`.
2. Las decisiones cerradas están en `docs/decisiones/` (ADR). No las revivas.
3. Lo que está **sin decidir** está marcado como *pendiente* o *propuesto*. No lo
   cierres por tu cuenta: pregunta o déjalo señalado.
4. La documentación de planeación vive en un Google Doc (fuente formal para la
   universidad). `docs/` es la copia de trabajo para el código. Si cambia algo de
   fondo, se actualiza en ambos.
5. Al terminar una historia, sigue la skill del repo `cierre-de-historia` (sección «Cierre de historia»).

## Flujo Spec-Driven Development (SDD)

Todo cambio de funcionalidad sigue este orden y **no se salta ninguna fase**:

**Especificación → Planificación → Tareas → Implementación → Pruebas.**

| Fase | Documento | Qué responde | Puerta para avanzar |
| --- | --- | --- | --- |
| 1. Especificación | `docs/sdd/spec.md` | **Qué** y por qué (criterios Dado-Cuando-Entonces) | El usuario la aprueba |
| 2. Planificación | `docs/sdd/plan.md` | **Cómo** (capas, migración, API, pruebas, riesgos) | El usuario la aprueba |
| 3. Tareas | `docs/sdd/tasks.md` | Pasos pequeños, ligados a criterios y a PR | Plan aprobado |
| 4. Implementación | código | Una tarea a la vez, en orden | Tareas escritas |
| 5. Pruebas | `tasks.md` (trazabilidad) | Cada criterio verificado | Todo en verde |

Reglas del flujo:

- **No escribas código** sin la especificación y el plan aprobados por el usuario.
  Una aprobación vale para ese documento, no para el siguiente.
- Trabaja paso a paso: presenta el documento, espera la respuesta, avanza.
- Mantén los documentos al día: si la implementación cambia algo, actualiza primero
  `spec.md` o `plan.md` y avisa. Marca el avance en `tasks.md` al cerrar cada tarea.
- Lo que no esté decidido se marca **[PENDIENTE]**; no lo cierres por tu cuenta.
- Reutiliza los IDs de `docs/contexto/` (`RF-xx`, `RNF-xx`, `RN-xx`, `ADR-xxxx`).
- Los tres archivos de `docs/sdd/` describen **una** funcionalidad a la vez. Al
  terminarla, se archivan en `docs/sdd/historico/<NOT-nn>-<nombre>/` y se parte de
  las plantillas para la siguiente.
- **Un solo PR por historia** (rama `feature/…` desde `develop`), con commits atómicos por tarea. Decidido por Andrés el 12 nov 2026: no quiere integrar muchos PR.

## Revisión de código al terminar (obligatoria)

- **Siempre** que una tarea haya escrito o cambiado código, al final de la tarea y antes de darla por
  cerrada se usa el subagente **`code-reviewer`** (`.claude/agents/code-reviewer.md`) sobre todos los
  cambios de la rama. Sus hallazgos se corrigen con pruebas y se reporta el antes/después.
- El hook del equipo en `.claude/settings.json` (`PostToolUse`, matcher `Edit|Write`, script
  `.claude/hooks/recordar-revision.mjs`) lo recuerda cuando se edita código en `backend/src`,
  `frontend/src` o `tools/`. Necesita Node (el mismo del frontend).
- Comentarios en el código: pocos, cortos y solo donde hagan falta. No se citan tareas ni historias
  (NOT-xx, EDT) en el código: eso va en commits, PR y `docs/`.

## Cierre de historia (obligatorio)

- Una historia no está cerrada hasta que su documentación está al día. Al terminarla se sigue la skill
  del repo **`cierre-de-historia`** (`.claude/skills/cierre-de-historia/`): `docs/`, archivo del SDD,
  modelo de datos (draw.io, Sheet y Doc), Linear, Figma, vault y memoria.
- La skill vive **en el repo, para todo el equipo** (Andrés, Katherine y Luz Amelia) y para cualquier IA
  que trabaje aquí: se invoca con el nombre `cierre-de-historia` o leyendo su `SKILL.md`. No la copies a
  la carpeta personal de nadie; si hay que mejorarla, se cambia en el repo con un PR.
- Si trabajas una historia de otra persona, la lista aplica igual: lo que no puedas actualizar tú (Figma,
  Google Doc, draw.io) se entrega al dueño con el texto listo.

## Reglas de trabajo

- No inventes endpoints, nombres de tablas ni reglas: revisa `docs/contexto/`.
- Respeta el stack fijado (abajo). No introduzcas otro framework ni otra base de
  datos sin una decisión explícita.
- Español en toda la interfaz. Código y nombres técnicos en inglés está bien.
- La asignación de comuna (punto-en-polígono) es la función central: la fuente de
  verdad es PostGIS en el backend, no el frontend.
- No subas secretos (claves de Resend, Cloudinary, base de datos): van en
  variables de entorno.

## Stack fijado

- **Backend:** Java 21 + Spring Boot 3.3 (web, data-jpa, security, validation).
- **Geo:** PostgreSQL 16 + PostGIS 3.4 (índice GiST), Hibernate Spatial.
- **Seguridad:** Spring Security + JWT (jjwt).
- **Frontend:** React 19 + Vite 8 + react-router 7 (`vite-plugin-pwa` todavía no está instalado: la app aún no es instalable, NOT-134). Gestor de paquetes: **pnpm** (no usar npm; no subir `package-lock.json`).
- **Mapas:** Leaflet 1.9 + OpenStreetMap + Leaflet.heat.
- **Fotos:** Cloudinary (plan Free), decidido el 29/10/2026 (ADR-0006). Mega descartado. Integrado por su API HTTP detrás de la interfaz `AlmacenFotos` (paquete `foto/`); probado con una subida real el 01/11/2026.
- **Hosting:** Netlify (frontend) + Render (backend) + Neon (PostgreSQL/PostGIS).
  - Netlify despliega desde la rama `develop` con base `frontend` y publish `dist`.
    La redirección SPA vive en `frontend/public/_redirects` (`/*  /index.html  200`);
    sin ella, las rutas directas (p. ej. `/app/nueva-contrasena`) dan 404 al recargar.
  - Variables de entorno, **sin valores en el repo**:
    - Netlify: `VITE_API_URL`.
    - Render: `DB_URL`, `DB_USER`, `DB_PASSWORD`, `JWT_SECRET`, `ADMIN_DEV_PASSWORD`,
      `GOOGLE_CLIENT_ID`, `CORS_ORIGINS`, `RECUPERACION_ENLACE`, `RESEND_API_KEY`,
      `RESEND_REMITENTE`, `VERIFICACION_ENLACE`, `CLOUDINARY_CLOUD_NAME`,
      `CLOUDINARY_API_KEY`, `CLOUDINARY_API_SECRET` (solo la dirección, p. ej. del subdominio `reportes.notiurba.com`).
- **Correo:** Resend (decidido el 29/10/2026, ADR-0002). Requiere dominio verificado; plan gratuito 100 correos/día. Se descartó el borrador que el ciudadano enviaba. Dominio `notiurba.com` comprado el 31/10/2026 (a nombre de Andrés Mauricio Peña López, ADR-0007); se envía desde el subdominio `reportes.notiurba.com`, verificado en Resend. El envío está implementado con la API HTTP de Resend (`EnviadorCorreo` / `EnviadorCorreoResend`) y se probó con un correo real el 01/11/2026 desde local; ya se probó desde Render. Mientras dure la etapa de pruebas, `RESEND_PRUEBA_DESTINO` debe seguir puesta (el correo sale a la bandeja de pruebas de Andrés, no a la entidad real).

## Equipo

- **Andrés Mauricio Peña López** — líder y backend/GIS.
- **Katherine López Unás** — frontend.
- **Luz Amelia Ibarguen** — UX/UI y pruebas.
- Docente y aprobador de alcance: **Jairo Rodríguez Martínez**.

## Reglas de colaboración (aprendidas con Andrés)

- Español claro y frases cortas, también en las preguntas.
- Ramas `feature/*` o `docs/*` y PR hacia `develop`; **sin líneas de coautoría de Claude** en commits ni PR.
  Un PR por historia, con commits atómicos. No mezcle nada que cambie datos de producción sin su aprobación.
- Lo que toca pantallas de Katherine se señala expresamente para su revisión.
- Nunca imprima secretos. No cree historias en Linear sin su visto bueno (formato en `docs/contexto/13-sprint-3.md`;
  skill global `linear-historias-sprint`).
- Pruebas y despliegue: skill del repo `.claude/skills/notiurba-pruebas-y-despliegue/`.
- Al cerrar una sesión larga: skill global `traspaso-de-sesion`.

## Decisiones abiertas (no cerrar sin el equipo)

- Validación jurídica del aviso de privacidad v3 (ya es el vigente; puntos por validar en ADR-0008).
- Valores del límite diario de reportes, intentos de login y distancia de duplicados (NOT-132).
- Cuándo y con qué autorización se activa el envío real a las entidades (NOT-131).
- La categoría 15 («Solicitud de información») está activa sin ruta de envío: desactivarla o enviarla al CAIC.
- Verificar el WhatsApp de Gases de Occidente (315 665 8536) antes de activarlo (hoy inactivo).

Ya decididas (no reabrir): categorías 8 y 9 y la consulta de la Ley 1581 están validadas por el equipo; el docente
aceptó que los barrios (PMGR 2020, V17) y la capa de comunas (V14) queden provisionales (ADR-0003).
