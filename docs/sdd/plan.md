# Plan técnico — <nombre de la funcionalidad>

> **Fase 2 de 5 del flujo SDD.** Responde **cómo** se construye lo que dice `spec.md`.
> Solo se redacta con la especificación **Aprobada**. No se escribe código hasta que
> este plan esté **Aprobado**.

| Campo | Valor |
| --- | --- |
| Estado | **Borrador** (Borrador → En revisión → Aprobado) |
| Especificación | [spec.md](spec.md) |
| Aprobado por / fecha | _(pendiente)_ |
| Última actualización | 2026-11-11 |

## 1. Enfoque general

_Resumen de la solución en pocas líneas y por qué esta y no otra._

## 2. Stack y límites (fijados, no se cambian)

Java 21 + Spring Boot 3.3 · PostgreSQL 16 + PostGIS 3.4 · JWT · React + Vite + PWA
(pnpm) · Leaflet · Cloudinary (`AlmacenFotos`) · Resend (`EnviadorCorreo`) ·
Netlify + Render + Neon. Si la solución necesita algo fuera de esta lista, se
documenta como **[PENDIENTE]** y se decide con el equipo (nuevo ADR).

## 3. Cambios por capa

### Base de datos
- Migración nueva: `V19__<nombre>.sql` (la siguiente libre; **una migración aplicada no se edita**).
- Tablas/columnas/índices: ___. PostGIS: _¿usa `ST_Covers`/GiST?_

### Backend (`backend/src/main/java/com/notiurba/…`)
- Paquete(s): ___ (`reporte/`, `geo/`, `correo/`, `admin/`…).
- Clases nuevas o modificadas: ___.
- La asignación de comuna sigue en PostGIS; no se mueve al frontend.

### API (contrato en `docs/contexto/11-contrato-api.md`)
| Método y ruta | Rol | Entrada | Salida | Errores |
| --- | --- | --- | --- | --- |
| `GET /api/___` | ___ | ___ | ___ | 400/401/403/404 |

No se inventan endpoints: cada uno se agrega al contrato al aprobar el plan.

### Frontend (`frontend/src/…`)
- Zona: `app/` (ciudadano), `admin/` (panel) o `compartido/` (ADR-0005).
- Páginas/componentes: ___. Textos en español en `compartido/textos/`.

### Integraciones y configuración
- Variables de entorno nuevas (sin valores en el repo): ___.
- Resend / Cloudinary: _¿afecta?_ (`RESEND_PRUEBA_DESTINO` sigue activa).

## 4. Estrategia de pruebas

| Criterio (spec) | Tipo de prueba | Dónde |
| --- | --- | --- |
| CA-01 | Unitaria / integración / componente / manual | `backend/src/test/…` o `frontend/…` |

Casos de error y entradas hostiles incluidos. Las pruebas existentes (backend 268,
frontend 139) deben seguir pasando.

## 5. Riesgos y mitigaciones

| Riesgo | Probabilidad / impacto | Mitigación |
| --- | --- | --- |
| ___ | ___ | ___ |

## 6. Alternativas descartadas

- ___ — por qué no.

## 7. Decisiones pendientes

- [PENDIENTE] ___ — decide: ___.

## 8. Aprobación

- [ ] Cada criterio de `spec.md` tiene un cambio técnico y una prueba.
- [ ] No introduce otro framework ni otra base de datos.
- [ ] Contrato de API y migración revisados.
- [ ] **Aprobado por el usuario** → se puede pasar a `tasks.md`.
