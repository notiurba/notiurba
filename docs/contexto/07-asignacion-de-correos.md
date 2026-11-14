# Asignación de correos

A qué correo llega cada reporte. Los reportes a la **Alcaldía** van todos al **CAIC**
(`servicioalciudadano@tulua.gov.co`), que los redirige según lo que pida el ciudadano: el asunto y el
cuerpo **no** indican la dependencia (NOT-130). Por correo solo se envía a quien tiene evidencia de que
responde: el **CAIC** y **Centroaguas**. Gas, energía y aseo no reciben correo: al elegir la categoría, la
app muestra sus canales oficiales y no crea reporte (atajo de NOT-130). Respaldo legal: Ley 1755 de 2015
(petición por correo).

## Categorías de la Alcaldía (1–10)

Los correos de dependencia son **de referencia**; el envío siempre es al CAIC.

| # | Categoría | Dependencia | Correo de referencia | Estado |
| --- | --- | --- | --- | --- |
| 1 | Vías, huecos, andenes, infraestructura | Hábitat e Infraestructura | infraestructura@tulua.gov.co | Confirmado |
| 2 | Alumbrado, espacio público, convivencia y seguridad | Gobierno, Convivencia y Seguridad | gobierno@tulua.gov.co | Confirmado |
| 3 | Basuras, ambiente, árboles, contaminación | SEDAMA | sedama@tulua.gov.co | Confirmado |
| 4 | Señalización, semáforos, tránsito, movilidad | Movilidad y Seguridad Vial | movilidad@tulua.gov.co | Confirmado |
| 5 | Riesgo sanitario o salud pública | Salud | salud@tulua.gov.co | Confirmado |
| 6 | Uso del suelo, planeación, licencias urbanas | Planeación Municipal | planeacion@tulua.gov.co | Confirmado |
| 7 | Instituciones o infraestructura educativa | Educación | educacion@tulua.gov.co | Confirmado |
| 8 | Riesgo de desastres, emergencias (propuesta) | Gobierno, Gestión del Riesgo | gestiondelriesgo@tulua.gov.co | Correo confirmado; **categoría por validar** |
| 9 | Vivienda (propuesta) | Planeación, Área de Vivienda | vivienda@tulua.gov.co | Correo confirmado; **categoría por validar** |
| 10 | Otro / no clasificado | CAIC (respaldo) | servicioalciudadano@tulua.gov.co | Confirmado |

El directorio completo del Oficio S-43215 (todas las dependencias) está en la pestaña
«Asignación de correos» del Google Doc.

## Empresas de servicios públicos

No son de la Alcaldía; no van al CAIC. Canales verificados el 12/11/2026 (migración V21; fuente y fecha
de verificación en `canal_atencion.fuente` y `verificado_en`):

| Servicio | Empresa | Correo | Canal que se muestra primero | Evidencia |
| --- | --- | --- | --- | --- |
| Acueducto y alcantarillado | Centroaguas | **info@centroaguas.com** (envía) | — (se envía por correo) | Acto GAC-003660-26: único correo autorizado para PQR. `notificacionespqr@` solo envía sus respuestas (inactivo). |
| Gas natural | Gases de Occidente | Sin correo | Formulario «Registro de PQR»; fugas: **línea 164** | `inquietudes@` exige confirmar en 24 h (inactivo). WhatsApp 315 665 8536 sin verificar (inactivo). |
| Energía eléctrica | Celsia | Sin correo | Zona Clientes → PQR; WhatsApp 310 390 3468 para daños | `servicioalcliente@` rechaza correos externos (550 5.7.1). |
| Aseo | Veolia Aseo Tuluá | Sin correo | **WhatsApp 317 372 5078** («Dto Palmira Buga Tuluá» → «Residuos» → «Aseo Valle») | WhatsApp verificado por Andrés el 12/11. `pqr.tulua.co@veolia.com` no respondió en 38 días (inactivo). |

La Alcaldía también tiene canales sin correo, que se muestran si un correo rebota: formulario PQRSDF,
PQRSDF anónimo, consulta de trámite, agente virtual, oficina en la Carrera 25 #25-04 y (602) 233 9300.

## El mensaje (plantillas A, B y C, NOT-130)

Carta formal en primera persona del ciudadano, sin «NotiUrba», sin código NU- y sin dependencia
(texto aprobado: anexo B de la spec de NOT-130, archivada en `docs/sdd/historico/`).

- **A** (Alcaldía con ubicación: 1–5 y 8): asunto «Petición ciudadana – {categoría} – {barrio}, Comuna {n}»,
  «Hechos», ubicación con mapa y coordenadas, fecha, evidencia.
- **B** (Alcaldía sin ubicación: 6, 7, 9 y 10): asunto «Solicitud ciudadana – {categoría}», «Solicitud».
- **C** (empresa; hoy solo la 11): «Señores {empresa}», asunto «Petición ciudadana – {servicio}» y, con
  matrícula o cuenta, «en calidad de usuario del servicio… (matrícula o cuenta No. …)».

Datos de la Ley 1755 (art. 16) que lleva: nombre, tipo y número de documento, dirección, correo y teléfono;
hechos o solicitud; ubicación; fecha en hora de Colombia; fotos como miniatura con enlace; autorización
de datos y firma. Se omite toda fila vacía. El ciudadano ve el mensaje antes de enviarlo (vista previa) y,
si el correo rebota o falla, puede copiarlo y usar otro canal.

## Pendientes

- Validar las categorías 8 y 9 con la Alcaldía.
- Confirmar el tamaño máximo de adjuntos del buzón del CAIC.
- Verificar el WhatsApp de Gases de Occidente (315 665 8536) antes de activarlo.
- La categoría 15 («Solicitud de información») está activa sin ruta de envío (decide el equipo).
- Activar el envío real a las entidades con [14-activar-envio-real.md](14-activar-envio-real.md); hoy todo sale a la bandeja de pruebas.

Fuente: pestañas «Asignación de correos» y «Modelado» del Google Doc; oficios S-43215 y
GAC-003660-26.
