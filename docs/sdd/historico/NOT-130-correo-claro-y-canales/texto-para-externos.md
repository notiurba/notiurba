# NOT-130 — Texto listo para el draw.io, el Sheet y el Doc

Andrés pega esto a mano (Claude no edita esos archivos). Fecha: 12 nov 2026.

## 1. Draw.io «Diagrama Entidad-Relacion» (Drive, carpeta NotiUrba)

> El diagrama no se actualiza desde el Sprint 2: también le faltan los cambios de V16 a V20
> (enrutamiento y envíos, barrio interno, catálogo PMGR, comuna en el reporte). Abajo va solo V21.

**Tabla nueva `finalidad_canal`** (catálogo)
- `id_finalidad_canal` SMALLSERIAL PK
- `nombre` VARCHAR(40) NOT NULL UNIQUE — Radicar PQR, Reportar daño, PQR anónimo, Consultar estado,
  Emergencia, Información

**Tabla `canal_atencion`: columnas nuevas**
- `id_finalidad_canal` SMALLINT NOT NULL DEFAULT 6, FK → `finalidad_canal` (ON DELETE RESTRICT)
- `orden` SMALLINT NOT NULL DEFAULT 100 — en qué orden se recomienda dentro de la entidad
- `horario` VARCHAR(200) NULL
- `fuente` VARCHAR(500) NULL — de dónde sale el dato (URL oficial o respuesta de la entidad)
- `verificado_en` DATE NULL

**Relación nueva:** `finalidad_canal` 1 —— N `canal_atencion`.

## 2. Google Sheet «Tablas Diagrama ER en excel»

**Pestaña `v5`** — fila nueva para `finalidad_canal` (catálogo, 2 columnas, 6 filas) y 5 columnas nuevas
en `canal_atencion` (las de arriba).

**Pestaña `Diccionario`**

| Tabla | Columna | Tipo | Nulo | Descripción |
| --- | --- | --- | --- | --- |
| finalidad_canal | id_finalidad_canal | SMALLSERIAL | No | Identificador |
| finalidad_canal | nombre | VARCHAR(40) | No | Para qué sirve un canal (único) |
| canal_atencion | id_finalidad_canal | SMALLINT | No | FK a finalidad_canal; por defecto 6 (Información) |
| canal_atencion | orden | SMALLINT | No | Orden de recomendación dentro de la entidad (por defecto 100) |
| canal_atencion | horario | VARCHAR(200) | Sí | Horario de atención en texto |
| canal_atencion | fuente | VARCHAR(500) | Sí | Origen del dato (URL o evidencia) |
| canal_atencion | verificado_en | DATE | Sí | Fecha en que se verificó el canal |

**Pestaña `Reglas v5`**
- Un canal que no sirve no se borra: queda `activo = FALSE` con el motivo en `detalle`.
- La app muestra los canales activos no-correo de la entidad ordenados por `orden`.
- Solo se envía correo a la Alcaldía (CAIC) y a Centroaguas (`info@centroaguas.com`). Gas, energía y aseo
  no reciben correo y no se crea reporte (atajo a sus canales).
- Todo canal activo de una entidad tiene `fuente` y `verificado_en`.

**Pestaña `Diferencias Figma`**
- Pantalla nueva «1b · Cómo reportar a la entidad (sin correo)».
- «4 · Confirmación»: tarjeta «Así llegará su mensaje».
- «¡Listo! · Correo no entregado (rebote)»: canales y «Copiar mi mensaje».

## 3. Google Doc «NotiUrba»

**Pestaña «Asignación de correos»** — reemplazar la tabla de empresas por:

| Servicio | Empresa | Correo | Canal que se muestra primero | Evidencia |
| --- | --- | --- | --- | --- |
| Acueducto y alcantarillado | Centroaguas | info@centroaguas.com (envía) | — | Acto GAC-003660-26: único correo autorizado para PQR |
| Gas natural | Gases de Occidente | Sin correo | Formulario «Registro de PQR»; fugas: 164 | `inquietudes@` exige confirmar en 24 h |
| Energía eléctrica | Celsia | Sin correo | Zona Clientes → PQR | `servicioalcliente@` rechaza correos externos |
| Aseo | Veolia Aseo Tuluá | Sin correo | WhatsApp 317 372 5078 | Sin respuesta por correo en 38 días; WhatsApp verificado el 12/11 |

Y agregar: «El correo es una carta formal en primera persona del ciudadano, sin "NotiUrba", sin código y sin
dependencia. Plantillas A (problema en un lugar), B (consulta o solicitud) y C (servicio público). El
ciudadano ve el mensaje antes de enviarlo.»

**Pestaña «Modelado»** — lo del punto 1 (finalidad_canal y las 5 columnas de canal_atencion).

**Pestaña «Diseño técnico y arquitectura»** — endpoints nuevos:
- `POST /api/reportes/vista-previa`: arma el mensaje sin guardar nada.
- `GET /api/reportes/{codigo}/mensaje`: el mensaje del reporte, para «Copiar mi mensaje».
- La ruta de cada categoría trae `canales` y `esEmpresa`.
