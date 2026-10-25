# Arquitectura y modelo de datos

> Estado: **parcial**. Este es el entregable «Diseño técnico» del Sprint 1 y es el
> que más falta por completar. Lo definido viene de las pestañas «Modelado» y «Stack
> tecnológico»; lo marcado como *falta* todavía no existe en el proyecto.

## Componentes (visión actual)

- **PWA (React + Vite)**: registro/sesión del ciudadano, creación de reporte (foto +
  GPS), consulta de «Mis reportes», y el panel administrativo (mapa, filtros, mapa de
  calor, gestión de estados).
- **API (Spring Boot)**: autenticación JWT, reportes, asignación de comuna,
  enrutamiento y envío de correo, registro de envíos.
- **PostgreSQL + PostGIS**: usuarios, reportes (con geometría), comunas (polígonos),
  catálogo de categorías y entidades de destino, historial de estados, registro de envíos.
- **Cloudinary**: almacenamiento de fotos (devuelve URL + id).
- **Servicio de correo (Resend / SMTP)**: envío al CAIC o a la empresa prestadora.
- **OpenStreetMap**: teselas del mapa base.

## Modelo de datos definido (enrutamiento de correos)

De la pestaña «Modelado». Tablas configurables, no quemadas en el código:

- `categoria`: id, nombre, descripcion, dependencia_id, entidad_destino_id, activa.
- `dependencia`: id, nombre, area (Secretaría o Departamento), correo_referencia.
  Guarda el directorio del Oficio S-43215; el correo es **referencial**, no destino.
- `entidad_destino`: id, nombre, tipo (`alcaldia` | `empresa_servicio_publico`),
  correo_envio, correo_alterno, canal_alterno (formulario, línea o WhatsApp), activa.
- `configuracion`: correo_general_alcaldia (`servicioalciudadano@tulua.gov.co`).

Regla: si la categoría no tiene dependencia competente, el correo va al CAIC y el
reporte queda marcado para revisión. La tabla debe validarse con la Alcaldía antes de
producción.

## Lo que falta diseñar (tareas de la EDT)

- [ ] **Diagrama de arquitectura de componentes** (PWA, API, PostGIS, Cloudinary,
      correo, OSM) — EDT 5.1.
- [ ] **Diagrama de despliegue** (Vercel, Render, Neon).
- [ ] **Modelo de datos completo**. Tablas aún sin definir:
      `usuario`, `reporte` (coordenadas PostGIS, foto_url, categoria, estado,
      fecha_creacion, vigencia, codigo_seguimiento), `comuna` (id, nombre, geometría),
      historial de estados, registro de envíos de correo.
- [ ] **Diagrama entidad-relación** que una lo anterior con las tablas de correo.
- [ ] **Diseño de la API REST** (EDT 5.2): endpoints, métodos, entradas/salidas,
      autenticación JWT, códigos de error.
- [ ] **Flujo de estados del reporte** (EDT 5.3).
- [ ] **Diseño de notificaciones** (EDT 5.5). Depende de ADR-0002 (mecanismo de correo).

## Notas de implementación

- La **asignación de comuna** es punto-en-polígono. En el MVP la fuente de verdad es
  PostGIS (`ST_Covers`), porque la comuna se guarda con el reporte y el correo la usa.
  El prototipo del frontend ya resuelve esto en JavaScript: sirve como especificación
  de comportamiento y casos de prueba (dentro / en borde / fuera → pendiente_revision /
  sin_determinar). Ver `06-comunas-y-territorio.md`.
- Correo en segundo plano (`@Async`) para no bloquear la respuesta de «crear reporte».
- Secretos (Resend, Cloudinary, base de datos) en variables de entorno (RNF-03).

Fuente: pestañas «Modelado» y «2. EDT/WBS» del Google Doc.
