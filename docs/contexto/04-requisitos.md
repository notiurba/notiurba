# Requisitos del MVP

Formato IEEE 830: cada requisito tiene ID único, se redacta como «el sistema
deberá…», incluye la respuesta ante datos no válidos y un criterio de verificación.
Prioridad: **Esencial** (función central) o **Condicional** (si el cronograma lo
permite). Estabilidad: **Estable** o **Puede cambiar** (con su dependencia).
Valores marcados como *propuestos* (500 caracteres, 1 MB, tiempos de RNF-04) deben
confirmarse con el equipo.

## Funcionales (RF) — 25 en el MVP

### Cuenta del ciudadano

- **RF-01** (Esencial, Estable | EDT 6.1): registrar al ciudadano con nombre,
  documento, dirección, correo y contraseña. Si falta un dato o el correo ya existe,
  rechaza el registro e indica el campo con error.
- **RF-02** (Esencial, Estable | EDT 6.1, 7.1): iniciar y cerrar sesión con correo y
  contraseña; impedir crear reportes sin sesión. Con credenciales inválidas, niega el
  acceso sin decir cuál dato falló.
- **RF-03** (Esencial, Puede cambiar — texto legal, Ley 1581 | EDT 6.1): exigir la
  aceptación del aviso de privacidad y la declaración de ser mayor de 18 años (una sola
  casilla) antes de completar el registro, y guardar fecha y hora de la aceptación.
- **RF-04** (Esencial, Estable | EDT 6.1, 7.6): incluir en cada correo a la entidad el
  nombre, documento y dirección del ciudadano que reporta.

### Reporte

- **RF-05** (Esencial, Estable; rango *propuesto* | EDT 6.2): crear un reporte con una
  categoría de la lista vigente y descripción de 10 a 500 caracteres. Fuera de rango o
  sin categoría, lo rechaza.
- **RF-06** (Esencial, Puede cambiar — almacenamiento de fotos; 1 MB *propuesto* | EDT
  6.3, 7.2): adjuntar foto JPG/PNG y reducirla a ≤1 MB antes de subir. Otro formato se
  rechaza.
- **RF-07** (Esencial, Estable | EDT 6.4): obtener GPS del dispositivo y permitir mover
  el marcador antes de enviar. Si niega el permiso, permitir fijar el punto tocando el mapa.
- **RF-08** (Esencial, Puede cambiar — capa GeoJSON | EDT 7.3): asignar la comuna (1 a
  10) cuyo polígono contiene las coordenadas. Si queda fuera de las 10 comunas, rechaza
  el reporte e informa «fuera de cobertura».
- **RF-09** (Esencial, Estable | EDT 6.5): generar un código de seguimiento único por
  reporte y mostrarlo al ciudadano.
- **RF-10** (Esencial, Estable | EDT 6.6): mostrar estado y observaciones de sus
  reportes y permitir buscar por código. Si no existe entre los suyos, lo informa.

### Envío por correo

- **RF-11** (Esencial, Puede cambiar — respuestas de empresas | EDT 7.6, 7.3): mantener
  una tabla categoría → correo de destino y usarla para dirigir cada reporte.
- **RF-12** (Esencial, Puede cambiar — depende del dominio | EDT 7.6): enviar con Resend
  desde un dominio verificado (nombre del ciudadano en el remitente, su correo en
  Reply-To; no se le envía copia). Decidido el 29/10/2026 (ADR-0002). Si Resend rechaza, registra el fallo (RF-15).
- **RF-13** (**Descartado el 29/10/2026**, ADR-0002): el borrador que el ciudadano enviaba
  desde su correo se reemplaza por el envío con Resend (RF-12).
- **RF-14** (Esencial, Estable | EDT 7.6; modificado en NOT-130): el correo es una carta formal en
  primera persona del ciudadano con la plantilla de su grupo (A, B o C): datos del ciudadano, hechos o
  solicitud, ubicación con comuna, coordenadas y mapa, fecha y fotos. Sin código de seguimiento, sin
  «NotiUrba» y sin dependencia.
- **RF-15** (Esencial, Estable | EDT 7.6, 7.2): enviar en segundo plano y registrar de
  cada envío destinatario, fecha, hora y resultado (enviado/fallido).
- **RF-16** (Condicional, Estable | EDT 6.6, 7.6): mostrar al ciudadano si el correo fue
  enviado o falló.
- **RF-17** (Esencial; modificado en NOT-130): mostrar todos los canales oficiales verificados de la
  entidad, ordenados, con su finalidad, horario y acción directa (WhatsApp, teléfono, web, oficina).
- **RF-26** (NOT-130): omitir filas vacías; fecha «9 de noviembre de 2026, 10:07 a. m.» y coordenadas
  con 5 decimales.
- **RF-27** (NOT-130): hasta 3 miniaturas con enlace y texto alternativo; versión en texto plano.
- **RF-28** (NOT-130): enviar por correo solo al CAIC y a Centroaguas (evidencia de respuesta).
- **RF-29** (NOT-130): «Copiar mi mensaje» cuando el correo rebotó o falló.
- **RF-30** (NOT-130): vista previa de solo lectura del mensaje antes de enviar; no guarda nada.
- **RF-31** (NOT-130): cada canal guarda finalidad, orden, horario, fuente y fecha de verificación.
- **RF-32** (NOT-130): atajo: gas, energía y aseo van del paso 1 a sus canales, sin crear reporte.

### Panel administrativo

- **RF-18** (Esencial, Estable | EDT 8.1): login del administrador con credenciales
  propias; negar el panel a quien no sea administrador.
- **RF-19** (Esencial, Estable | EDT 8.2): listar reportes y filtrarlos por comuna,
  categoría, estado y fecha. Sin coincidencias, «Sin resultados».
- **RF-20** (Esencial, Estable | EDT 7.4, 8.2): cambiar el estado de un reporte y
  registrar observaciones, visibles para el ciudadano (RF-10).
- **RF-21** (Esencial, Puede cambiar — alcance de «vigencia» | EDT 7.5): definir la
  vigencia en días; al vencer, sale del mapa pero sigue en el listado del administrador.
- **RF-22** (Esencial, Estable | EDT 8.3): mostrar los reportes vigentes como
  marcadores en OpenStreetMap con el contorno de las 10 comunas y la atribución visible.
- **RF-23** (Esencial, Puede cambiar — Leaflet.heat | EDT 8.4): mapa de calor de los
  reportes vigentes, filtrable por categoría y comuna.
- **RF-24** (Condicional, Estable | EDT 8.5): total de reportes por comuna, categoría y estado.
- **RF-25** (Condicional, Estable | EDT 8.2, 7.6): editar el correo de destino de cada
  categoría; rechazar un valor sin formato de correo.

## No funcionales (RNF)

- **RNF-01**: web responsive y PWA, solo en español. Verificación: se instala como PWA
  en Chrome Android, se ve sin scroll horizontal a 360 px y 1366 px, todo en español.
- **RNF-02**: requiere internet; sin modo offline y el service worker no guarda teselas
  de OSM. Verificación: la caché del SW no contiene URL de tile.openstreetmap.org.
- **RNF-03**: seguridad con JWT, HTTPS y claves en variables de entorno. Verificación:
  endpoint protegido sin token → 401; solo carga por HTTPS; el repo no contiene claves.
- **RNF-04**: rendimiento; correo en segundo plano y carga optimizada del mapa de calor.
  Verificación: «crear reporte» responde en ≤2 s sin esperar el correo, y el mapa de
  calor con 500 reportes carga en ≤3 s (*valores propuestos*).
- **RNF-05**: bajo consumo de datos; fotos comprimidas antes de subir (igual que RF-06).
- **RNF-06**: atribución a OpenStreetMap siempre visible, en móvil y computador.
- **RNF-07**: datos personales; aviso de privacidad, autorización y trazabilidad de
  cada envío (RF-03, RF-15).

## Requisitos deseables (solo si el Sprint 3 lo permite)

Tiempo medio de respuesta; correo al ciudadano al cambiar de estado; detección de
duplicados por cercanía (R.06); opción «barrio no listado» (R.02); webhooks de Resend
para rebotes (R.05).

## Contradicciones y pendientes

- **Categorías**: el Doc de Alcance trae 3, la planificación 4, y «Asignación de
  correos» 10 de la Alcaldía + 4 de empresas. Propuesta MVP: categorías 1–7 y 10 de la
  Alcaldía y las 4 de empresas. Las **categorías 8 y 9 siguen por validar**.
- **Dominio**: sin dominio verificado, Resend solo envía al dueño de la cuenta, así que
  RF-12 no es posible. El borrador ya no es respaldo (ADR-0002): conseguir el dominio es
  requisito. Decisión antes del 8 de noviembre.
- **Mapa público**: propuesta de no incluirlo en el MVP (expondría datos, Ley 1581).
- **Verificación del correo**: decidir entre confirmar por enlace (gasta cuota) o
  aceptar el riesgo en el MVP.
- **Datos personales**: falta la respuesta del docente sobre si aplica la Ley 1581 al
  pedir documento y dirección.
- **Eventos comunitarios**: sin dependencia ni destinatario; decidir si se quitan del
  MVP o se registran sin envío.

## Agregados en NOT-132 (13/11/2026)

- **RNF-04 (ajuste)**: el registro responde en 2 s o menos sin esperar el correo de verificación (medido: 0,35 s; antes ~10 s).
- **RF-01 (ajuste)**: si el correo de verificación falla, la cuenta igual queda creada y se puede pedir otro enlace.
- **RF-33**: el log registra en qué paso falló «Continuar con Google», sin datos personales.
- **RF-34**: 3 intentos fallidos de ingreso en 5 minutos bloquean ese correo 5 minutos.
- **RF-35**: máximo 2 reportes por cuenta al día (día de Colombia).
- **RF-36**: aviso de reporte parecido: misma categoría, cerca (5 a 15 m según la categoría), últimos 30 días, de cualquier cuenta y sin resolver. Se puede enviar de todas maneras.
- **RF-37**: el administrador consulta y ajusta todos estos valores en «Configuración».
- **RF-38**: cada situación del ingreso muestra un mensaje claro, sin lenguaje técnico.
- **RF-39**: si se repite un problema ya resuelto en ese lugar, el reporte exige al menos una foto.

Fuente: pestaña «Requisitos funcionales y limitación de comunas» del Google Doc.
