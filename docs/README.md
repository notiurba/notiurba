# Documentación de NotiUrba

Carpeta de contexto del proyecto. Es la copia de trabajo para el desarrollo y para
Claude Code. La fuente formal para la universidad es el Google Doc de planeación;
esta carpeta la resume y la aterriza para el código.

> Visión general y estado del proyecto (menos técnica): bóveda de Obsidian de Andrés, nota `wiki/proyectos/noti-urba/noti-urba.md` y las `noti-urba-*` de esa carpeta. Este `docs/` es el detalle técnico; no duplicar entre ambos.

## Contexto

| Archivo | Qué contiene |
| --- | --- |
| [contexto/01-vision-y-alcance.md](contexto/01-vision-y-alcance.md) | Qué es NotiUrba, el problema, objetivos y qué queda fuera del MVP. |
| [contexto/02-stack.md](contexto/02-stack.md) | Tecnologías fijadas, hosting y almacenamiento de fotos. |
| [contexto/03-arquitectura-y-datos.md](contexto/03-arquitectura-y-datos.md) | Componentes, modelo de datos y lo que falta por diseñar. |
| [contexto/04-requisitos.md](contexto/04-requisitos.md) | 25 requisitos funcionales (RF) y 7 no funcionales (RNF). |
| [contexto/05-reglas-de-negocio.md](contexto/05-reglas-de-negocio.md) | 21 reglas de negocio (RN) que el sistema cumple siempre. |
| [contexto/06-comunas-y-territorio.md](contexto/06-comunas-y-territorio.md) | Las 10 comunas, la capa GeoJSON y el flujo territorial. |
| [contexto/07-asignacion-de-correos.md](contexto/07-asignacion-de-correos.md) | Categorías, entidades de destino y enrutamiento. |
| [contexto/08-pendientes.md](contexto/08-pendientes.md) | Lo abierto, por prioridad. |
| [contexto/09-equipo-y-proceso.md](contexto/09-equipo-y-proceso.md) | Roles, sprints, gestión en Linear y control de cambios. |
| [contexto/10-modelo-de-datos.md](contexto/10-modelo-de-datos.md) | Modelo de datos: tablas, reglas de la base y mapa de formularios. |
| [contexto/11-contrato-api.md](contexto/11-contrato-api.md) | Contrato de la API REST: formato de error, los endpoints que existen y los pendientes (NOT-43). |
| [contexto/12-estado-actual-y-traspaso.md](contexto/12-estado-actual-y-traspaso.md) | **Empiece aquí:** qué funciona, qué falta, cómo se corre y se prueba, y cómo se trabaja con Andrés. |
| [contexto/13-sprint-3.md](contexto/13-sprint-3.md) | Sprint 3 (último): 14 historias, reparto, dependencias y plantilla de historia. |
| [glosario.md](glosario.md) | Términos del proyecto. |

## Decisiones (ADR)

Registro de decisiones de arquitectura. Cada una explica el contexto, la decisión y
sus consecuencias. Ver [decisiones/README.md](decisiones/README.md).

- [ADR-0001](decisiones/0001-comunas-no-cuadrantes.md) — Comunas, no cuadrantes.
- [ADR-0002](decisiones/0002-mecanismo-de-correo.md) — Mecanismo de envío de correo: Resend (aceptada).
- [ADR-0003](decisiones/0003-fuente-cartografica-comunas.md) — Fuente cartográfica de las comunas.
- [ADR-0004](decisiones/0004-hosting-gratuito.md) — Hosting en capas gratuitas.
- [ADR-0005](decisiones/0005-estructura-frontend-app-admin.md) — Estructura del frontend `app/` / `admin/` / `compartido/`.
- [ADR-0006](decisiones/0006-almacenamiento-de-fotos-cloudinary.md) — Almacenamiento de fotos: Cloudinary.
- [ADR-0007](decisiones/0007-dominio-para-el-correo.md) — Dominio `notiurba.com` para el correo de Resend (aceptada).
- [ADR-0008](decisiones/0008-privacidad-autorizacion-unica-y-verificacion-de-correo.md) — Privacidad: aviso v3, autorización única, mayores de 18 y verificación de correo.

## Convenciones

- **Pendiente**: aún no decidido. No cerrarlo sin el equipo.
- **Propuesto**: valor o enfoque sugerido, falta confirmar (ej. tiempos de RNF-04).
- IDs estables: `RF-xx` (funcional), `RNF-xx` (no funcional), `RN-xx` (regla de
  negocio), `R.xx` (riesgo), `ADR-xxxx` (decisión).
