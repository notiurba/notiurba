# Especificación — Correo formal, canales por entidad y vista previa del mensaje

> **Fase 1 de 5 del flujo SDD** (Especificación → Planificación → Tareas → Implementación → Pruebas).
> Aquí se define **qué** se construye y **por qué**. El **cómo** va en `plan.md`.
> No se escribe código hasta que esta especificación esté **Aprobada**.

| Campo | Valor |
| --- | --- |
| Estado | **Aprobada** (Borrador → En revisión → Aprobada) |
| Historia / ticket | NOT-130 (Linear) |
| Responsable | Andrés (Figma, backend y frontend) |
| Aprobada por / fecha | Andrés, 2026-11-12, con los cambios de la sección 8 |
| Última actualización | 2026-11-12 |

## 1. Contexto y problema

La función central de NotiUrba es **hacer llegar el reporte a la entidad correspondiente**. Hoy ese último paso falla de cuatro formas:

1. **El correo es confuso.** El del 9 de nov muestra la matrícula siempre («No aportado»), la fecha en texto crudo de Java, coordenadas con 15 decimales, filas vacías, fotos solo como enlaces, el código y el nombre de NotiUrba, y una dependencia que el CAIC no necesita.
2. **Los correos de varias empresas no funcionan** (evidencia del correo de Andrés, 29 sep a 14 oct 2026; ver Anexo A): Celsia rechaza los mensajes; Gases de Occidente exige pulsar un enlace en 24 horas o descarta la solicitud; Veolia no respondió; y el repo tenía mal el correo de Centroaguas (el autorizado para PQR es `info@centroaguas.com`).
3. **El ciudadano solo ve un canal alterno.** Las entidades tienen más canales verificables (formulario PQRSDF de la Alcaldía, PQRSDF anónimo, líneas, WhatsApp, oficinas) y la base solo guarda uno por categoría.
4. **El ciudadano no ve lo que se enviará en su nombre** antes de confirmar.

Esto sostiene la función central; sin un mensaje claro y un canal que responda, el reporte se pierde.

## 2. Objetivo

Que cada reporte llegue con un **mensaje formal y completo** por un canal con evidencia de que responde; que, cuando no hay correo útil o el correo rebota, el ciudadano tenga **otros canales oficiales y su mensaje listo para copiar**; y que **vea el mensaje antes de enviarlo**.

## 3. Usuarios y historias

- Como **ciudadano**, quiero ver el mensaje tal como se enviará y poder corregirlo, para presentar bien mi inconformidad.
- Como **ciudadano** con una entidad sin correo útil, quiero ver sus canales oficiales y copiar mi mensaje, para no volver a escribirlo.
- Como **funcionario de la entidad**, quiero un correo formal, ordenado y con los datos que pide mi oficina, para radicarlo sin pedir más información.
- Como **equipo**, quiero que cada canal guarde su finalidad, horario y fuente, para saber de dónde sale cada dato.

## 4. Requisitos

| ID | Requisito («el sistema deberá…») | Prioridad | Origen |
| --- | --- | --- | --- |
| RF-14 *(modifica)* | Armar el correo con la plantilla del grupo de la categoría (A, B o C; Anexo B), en tono formal y en primera persona del ciudadano. Sin código de seguimiento, sin mención a NotiUrba y sin dependencia. | Esencial | NOT-130; Andrés 12 nov |
| RF-26 *(nuevo)* | Omitir toda fila opcional vacía; mostrar la fecha como «9 de noviembre de 2026, 10:07 a. m.» (hora de Colombia) y las coordenadas con 5 decimales. | Esencial | NOT-130 |
| RF-27 *(nuevo)* | Mostrar hasta 3 miniaturas con enlace y texto alternativo; sin fotos, omitir la sección. Incluir la versión en texto plano con los mismos datos. | Esencial | NOT-130 |
| RF-28 *(nuevo)* | Enviar por correo solo a los destinos con evidencia de que responden (CAIC y Centroaguas). Para las demás entidades, mostrar sus canales. | Esencial | Anexo A |
| RF-17 *(modifica)* | Mostrar **todos** los canales oficiales verificados de la entidad, ordenados, con su finalidad (radicar PQR, reportar daño, anónimo, consultar estado, emergencia), horario y enlace o número. | Esencial | NOT-130 |
| RF-29 *(nuevo)* | Ofrecer «Copiar mi mensaje» (texto de la plantilla) cuando la entidad no recibe correo o el correo falló. | Esencial | Andrés 12 nov |
| RF-30 *(nuevo)* | En el paso «Revise su reporte», mostrar la vista previa de solo lectura del mensaje, con «Editar» hacia el paso donde se captura cada dato. La vista previa no guarda ni envía nada. | Esencial | Andrés 12 nov |
| RF-31 *(nuevo)* | Guardar por canal su finalidad, orden, horario, fuente y fecha de verificación. | Esencial | NOT-130 |
| RN-12 *(modifica)* | El asunto y el cuerpo ya no indican la dependencia: el CAIC redirige según lo que pida el ciudadano. | Esencial | Andrés 12 nov |
| RN-17 *(modifica)* | Asunto formal sin código: «Petición ciudadana – [categoría] – [barrio y comuna]». El código NU-… queda solo en la app y la base. | Esencial | Andrés 12 nov |
| RF-04, RF-12, RF-15, RF-16, RN-07, RN-13, RN-15, RN-16, RN-18 | Se conservan. RN-07: se mantiene la constancia de autorización en una línea. | — | — |

## 5. Criterios de aceptación

**Correo**
- **CA-01** — _Dado_ un reporte de la categoría 1, _cuando_ se envía, _entonces_ el correo está en tono formal, trae nombre, tipo y número de documento, dirección, teléfono y correo del ciudadano, hechos, ubicación, fecha, evidencia, solicitud y firma, y **no** contiene «NotiUrba», el código NU-… ni la dependencia.
- **CA-02** — _Dado_ un reporte de cada grupo (A: categorías 1, 2, 3, 4, 5 y 8; B: 6, 7, 9 y 10; C: 11 a 14), _entonces_ el correo usa la plantilla de su grupo.
- **CA-03** — _Dado_ un dato opcional vacío (dirección del incidente, teléfono, matrícula), _entonces_ su fila no aparece y no dice «No aportado».
- **CA-04** — _Dadas_ la fecha y las coordenadas, _entonces_ se leen «9 de noviembre de 2026, 10:07 a. m.» y `4.08672, -76.19500`.
- **CA-05** — _Dado_ un reporte con 2 fotos, _entonces_ salen 2 miniaturas con enlace; _sin fotos_, la sección no aparece.
- **CA-06** — _Dado_ un reporte del grupo C con matrícula o cuenta, _entonces_ empieza «en calidad de usuario del servicio… (matrícula o cuenta N.º …)»; _sin ella_, empieza «como ciudadano».
- **CA-07** — _Dado_ el correo abierto en Gmail del celular y del computador, _entonces_ no hay desplazamiento horizontal, y la versión en texto plano trae los mismos datos.

**Canales**
- **CA-08** — _Dado_ un reporte de la Alcaldía o de acueducto, _cuando_ se envía, _entonces_ va a `servicioalciudadano@tulua.gov.co` o a `info@centroaguas.com`.
- **CA-09** — _Dado_ un reporte de gas, energía o aseo, _entonces_ no se envía correo, queda «no enviado por correo» y el ciudadano ve los canales de la entidad y «Copiar mi mensaje».
- **CA-10** — _Dados_ los canales de una entidad, _entonces_ aparecen ordenados, con finalidad y horario; los de la Alcaldía incluyen formulario PQRSDF, PQRSDF anónimo, consulta de estado, agente virtual, presencial y teléfono.
- **CA-10b** — _Dado_ un canal sin verificar (hoy el WhatsApp de Veolia), _entonces_ está guardado como inactivo, no se muestra y se activa con un cambio de datos, sin cambiar código.
- **CA-11** — _Dado_ un correo que rebota (webhook de NOT-131), _entonces_ el ciudadano ve los mismos canales y «Copiar mi mensaje».
- **CA-12** — _Dada_ una emergencia de gas, _entonces_ la app indica la línea 164 y no envía correo (RN-16).

**Vista previa**
- **CA-13** — _Dado_ el paso «Revise su reporte», _entonces_ el ciudadano ve el mensaje tal como se enviará; _al tocar_ «Editar» en una sección, vuelve al paso de ese dato y, al regresar, ve el cambio.
- **CA-14** — _Dada_ la vista previa, _entonces_ no crea reporte, no sube fotos y no envía correo.
- **CA-15** — _Dado_ el mismo reporte, _entonces_ el texto de la vista previa y el del correo enviado son idénticos (misma plantilla).
- **CA-16** — Caso de error: _dados_ datos no válidos o sin sesión, _entonces_ responde 400 con `{codigo, mensaje, campo}` o 401, igual que el resto de la API.

**Datos**
- **CA-17** — _Dada_ la migración V21, _entonces_ aplica sobre la base con los reportes existentes sin perder datos; los canales con evidencia en contra quedan **inactivos** con el motivo en `detalle`, no borrados.
- **CA-18** — _Dado_ cada canal, _entonces_ tiene `finalidad`, `orden`, `fuente` y `verificado_en`.

## 6. Reglas y restricciones que aplican

- Reglas de negocio: RN-07, RN-13, RN-15, RN-16, RN-18 (`docs/contexto/05-reglas-de-negocio.md`). Se modifican RN-12 y RN-17.
- Decisiones cerradas: ADR-0002 (envío con Resend), ADR-0008 (autorización única).
- Ley 1581 de 2012: la vista previa muestra solo los datos del propio ciudadano y no los guarda; el correo conserva la constancia de autorización.
- Una sola ruta de envío por reporte: no se mandan copias a varias direcciones de la misma entidad.
- `RESEND_PRUEBA_DESTINO` sigue puesta: **esta historia no activa el envío real** (NOT-131 y NOT-133).
- Interfaz y correo en español, tono formal.
- **Orden de trabajo** (decidido por Andrés el 12 nov): primero **Figma** (se valida con Andrés), después el **backend** siguiendo esta spec y por último el **frontend**, con pruebas funcionales al final.
- La tarjeta de «Revise su reporte» (`PasoConfirmacion`, pantalla de Katherine) la implementan Andrés y Claude junto con el Figma y el backend, porque el cambio es pequeño. Se le avisa a Katherine al integrar.

## 7. Fuera de alcance

- Activar el envío real y el webhook de rebotes (NOT-131, NOT-133).
- Texto libre editable en el mensaje (se evalúa después de usar la vista previa).
- Enviar formularios web de las entidades en nombre del ciudadano.
- Validar los canales con las empresas (15 días): se usa evidencia pública y el correo de Andrés.

## 8. Preguntas abiertas

**Decididas por Andrés el 12 nov 2026**
- Quién implementa la tarjeta del frontend: **Andrés y Claude**, junto con Figma y backend (ver sección 6).
- Texto de las plantillas (Anexo B), con la frase «en ejercicio del derecho de petición (Constitución Política, art. 23)»: **aprobado**.
- Veolia: **sin correo**; el medio preferido es WhatsApp.

**Abiertas**
- [PENDIENTE] **Verificar que el WhatsApp de Veolia (317 372 5078) sea oficial para Tuluá.** Lo que se encontró el 12 nov: aparece en el repo y en una nota de prensa sobre Cali; **no** aparece en `valle.veolia.co` (páginas de Tuluá y «Contáctanos»). Para mientras: se guarda inactivo y Veolia muestra la línea 01 8000 95 00 96, la oficina de Tuluá y el formulario «Contáctanos» de su sitio. Cómo verificar: escribir a ese número preguntando si atiende Tuluá, o mirar la factura impresa. Decide: Andrés.

## 9. Aprobación

- [x] La especificación está completa y sin ambigüedades.
- [x] Las preguntas abiertas están resueltas o aceptadas como riesgo (queda la verificación del WhatsApp de Veolia, que no bloquea).
- [x] **Aprobada por el usuario** (2026-11-12) → se puede pasar a `plan.md`.

---

## Anexo A — Canales y evidencia (12 nov 2026)

| Entidad | Correo | Decisión | Evidencia |
| --- | --- | --- | --- |
| **Alcaldía (CAIC)** | `servicioalciudadano@tulua.gov.co` | ✅ Correo | Respondió al día siguiente con radicado E-.29570 y pidió nombre, cédula, dirección y teléfono. |
| **Centroaguas** | `info@centroaguas.com` | ✅ Correo | Su acto GAC-003660-26 (13 oct) dice que es el único autorizado para PQR; radicado CO260929-3786; respondió en 14 días. `notificacionespqr@` solo es el remitente de sus respuestas. |
| **Gases de Occidente** | `inquietudes@gdo.com.co` | ❌ | Responde con un aviso automático: hay que pulsar «Confirmar envío» en 24 h o no se recibe. |
| **Celsia** | `servicioalcliente@celsia.com` | ❌ | Rechazo 550 5.7.1: la cuenta no recibe correos; remite a Zona Clientes. |
| **Veolia Tuluá** | `pqr.tulua.co@veolia.com` | ❌ (decidido) | Aceptó el correo del 5 oct y no respondió en 38 días. El repo tenía `pqr.tulua@veolia.com`, que no coincide con su sitio. |

| Entidad | Otros canales (finalidad) |
| --- | --- |
| **Alcaldía** | Formulario PQRSDF (tulua.gov.co/6404) · PQRSDF anónimo (tulua.tramiter.co/documentos/seleccionar-tramite-anonimo) · consultar estado (tulua.tramiter.co/documentos/consultar-tramite-web) · agente virtual 24 h · Carrera 25 #25-04, lunes a jueves 7:30–12:00 y 1:30–6:00, viernes hasta las 5:00 · (602) 233 9300 |
| **Centroaguas** | Aplicativo PQRS web · línea 231 7300 (3 = PQR, 116 = daños) · Calle 25 #32A-31, lunes a viernes 7:30–11:30 y 1:30–4:00 |
| **Celsia** | Zona Clientes → PQR (clientes.celsia.com) · línea 01 8000 112 115 · WhatsApp 310 390 3468 (daños) |
| **Gases de Occidente** | Formulario «Registro de PQR» (gdo.com.co/Paginas/Registro-de-pqrs.aspx) · línea 164 (emergencias) · contact center 24/7: (602) 418 7333 y 01 8000 52 88 88 |
| **Veolia Tuluá** | WhatsApp 317 372 5078 (**sin verificar**, inactivo) · línea 01 8000 95 00 96 · Carrera 26 #26-20 local 104, lunes a viernes 8:00–16:00 · formulario «Contáctanos» (valle.veolia.co/contactanos) |

## Anexo B — Plantillas (texto aprobado por Andrés el 12 nov 2026)

Asunto: **A** «Petición ciudadana – {categoría} – {barrio}, Comuna {n}» · **B** «Solicitud ciudadana – {categoría}» · **C** «Petición ciudadana – {servicio}».

**A — Problema en un lugar (categorías 1, 2, 3, 4, 5, 8)**

> Respetados señores:
>
> Yo, {nombre completo}, con {tipo de documento} No. {número}, en ejercicio del derecho de petición (Constitución Política, art. 23), me dirijo a ustedes para informar la siguiente situación:
>
> **Hechos:** {descripción}
> **Ubicación:** {dirección}, barrio {barrio}, Comuna {n} · {enlace al mapa} · {coordenadas}
> **Fecha del registro:** {fecha}
> **Evidencia:** {n} fotografías (enlaces).
>
> Solicito atender esta situación y comunicarme el trámite adelantado y el número de radicado asignado.
>
> Autorizo el uso de mis datos para el trámite de esta petición. Recibiré respuesta en {correo} · {teléfono} · {dirección de correspondencia}.
>
> Atentamente,
> {nombre completo} · {tipo de documento} {número}

**B — Consulta o solicitud (categorías 6, 7, 9, 10):** igual que A, con «**Solicitud:** {descripción}» en lugar de «Hechos», y sin ubicación ni evidencia cuando no las hay.

**C — Falla de servicio público (categorías 11 a 14):** dirigida a {empresa}. Con matrícula o cuenta: «…en calidad de usuario del servicio de {servicio} (matrícula o cuenta No. {X}), me dirijo a ustedes…». Sin ella: «…como ciudadano, me dirijo a ustedes…». El resto igual que A. La solicitud es general: «Solicito atender esta situación y comunicarme el trámite adelantado y el número de radicado asignado.»

## Anexo C — Cambios a documentos existentes al aprobar

`docs/contexto/04` (RF-14, RF-17 y nuevos RF-26 a RF-31), `05` (RN-12, RN-17), `07` (canales), `10` y `11` (modelo y contrato), y los externos: draw.io del ER, Sheet del ER y Google Doc (ver skill `cierre-de-historia`).
