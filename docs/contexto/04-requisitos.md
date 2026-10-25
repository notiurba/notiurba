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
  aceptación del aviso de privacidad antes de completar el registro y guardar fecha y
  hora de la aceptación.
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
- **RF-12** (Esencial, Puede cambiar — depende del dominio | EDT 7.6): con dominio
  verificado, enviar con Resend (nombre del ciudadano en el remitente, su correo en
  Reply-To y copia). Si Resend rechaza, registra el fallo (RF-15).
- **RF-13** (Esencial, Puede cambiar — desaparece si se verifica el dominio | EDT 7.6):
  sin dominio verificado, abrir en el correo del ciudadano un borrador con
  destinatario, asunto y cuerpo completos.
- **RF-14** (Esencial, Estable | EDT 7.6): el correo incluye comuna, categoría,
  descripción, coordenadas, enlace al mapa, enlace a la foto y código de seguimiento.
- **RF-15** (Esencial, Estable | EDT 7.6, 7.2): enviar en segundo plano y registrar de
  cada envío destinatario, fecha, hora y resultado (enviado/fallido).
- **RF-16** (Condicional, Estable | EDT 6.6, 7.6): mostrar al ciudadano si el correo fue
  enviado o falló.
- **RF-17** (Condicional, Puede cambiar | EDT 7.6): para categorías sin destinatario
  confirmado, mostrar el canal oficial de la entidad en lugar de enviar el correo.

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
- **Dominio**: sin dominio verificado, Resend solo envía al dueño de la cuenta; RF-12
  no es posible y el MVP opera solo con el modo borrador (RF-13). Decisión antes del 8 de noviembre.
- **Mapa público**: propuesta de no incluirlo en el MVP (expondría datos, Ley 1581).
- **Verificación del correo**: decidir entre confirmar por enlace (gasta cuota) o
  aceptar el riesgo en el MVP.
- **Datos personales**: falta la respuesta del docente sobre si aplica la Ley 1581 al
  pedir documento y dirección.
- **Eventos comunitarios**: sin dependencia ni destinatario; decidir si se quitan del
  MVP o se registran sin envío.

Fuente: pestaña «Requisitos funcionales y limitación de comunas» del Google Doc.
