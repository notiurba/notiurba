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
| [glosario.md](glosario.md) | Términos del proyecto. |

## Decisiones (ADR)

Registro de decisiones de arquitectura. Cada una explica el contexto, la decisión y
sus consecuencias. Ver [decisiones/README.md](decisiones/README.md).

- [ADR-0001](decisiones/0001-comunas-no-cuadrantes.md) — Comunas, no cuadrantes.
- [ADR-0002](decisiones/0002-mecanismo-de-correo.md) — Mecanismo de envío de correo (abierta).
- [ADR-0003](decisiones/0003-fuente-cartografica-comunas.md) — Fuente cartográfica de las comunas.
- [ADR-0004](decisiones/0004-hosting-gratuito.md) — Hosting en capas gratuitas.
- [ADR-0005](decisiones/0005-estructura-frontend-app-admin.md) — Estructura del frontend `app/` / `admin/` / `compartido/`.

## Convenciones

- **Pendiente**: aún no decidido. No cerrarlo sin el equipo.
- **Propuesto**: valor o enfoque sugerido, falta confirmar (ej. tiempos de RNF-04).
- IDs estables: `RF-xx` (funcional), `RNF-xx` (no funcional), `RN-xx` (regla de
  negocio), `R.xx` (riesgo), `ADR-xxxx` (decisión).
