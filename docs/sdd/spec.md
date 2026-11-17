# Especificación — Correcciones de cierre QA y despliegue

> **Fase 1 de 5 del flujo SDD** (Especificación → Planificación → Tareas → Implementación → Pruebas).
> Aquí se define **qué** debe quedar corregido y verificado. El **cómo** se decidirá en `plan.md`.
> No se modifica código, configuración externa ni datos hasta que esta especificación esté **Aprobada**.

| Campo | Valor |
| --- | --- |
| Estado | **Aprobada** (Borrador → En revisión → Aprobada) |
| Historia / ticket | Sin historia de Linear; correcciones de cierre QA detectadas durante NOT-141 |
| Responsable | Andrés (coordinación y despliegue), con revisión de Katherine y validación de Amelia donde corresponda |
| Aprobada por / fecha | Andrés, 2026-11-16 |
| Última actualización | 2026-11-16 |

## 1. Contexto y problema

La validación de punta a punta dejó comportamientos que impiden considerar confiable la versión desplegada:

- El mapa administrativo sustituye silenciosamente un fallo de la API por reportes ficticios. Un administrador puede interpretar datos de demostración como información real.
- En celular, el distintivo público de Netlify puede superponerse al botón «Continuar».
- La solicitud de recuperación de contraseña responde como si hubiera sido atendida, pero Andrés no recibió el correo y hoy no hay evidencia suficiente para distinguir entre una cuenta no elegible, el límite de solicitudes, una configuración incorrecta, un rechazo de Resend o un problema de entrega.
- No está confirmada la revisión conjunta del commit desplegado, el entorno autorizado para QA y la presencia de las variables necesarias sin revelar sus valores.
- Quedan verificaciones dirigidas del almacenamiento de reportes y fotografías, el correo a destinos de prueba y el flujo administrativo del reporte `NU-000010`.
- Las cuentas y permisos de QA necesarios para registro tradicional, recuperación y eliminación de datos no están formalmente definidos.

La aplicación y la API públicas responden, pero esa disponibilidad no prueba los flujos anteriores. Las pruebas móviles ya ejecutadas y las 35 solicitudes temporales de Postman ya tienen evidencia y no forman parte de esta corrección.

## 2. Objetivo

Eliminar los datos ficticios y la obstrucción móvil detectada, restablecer y comprobar la recuperación de contraseña por correo, y obtener evidencia reproducible de los flujos dirigidos de backend, correo y administración sin enviar mensajes a entidades reales ni exponer secretos o datos personales.

## 3. Usuarios e historias

- Como **administrador**, quiero que un fallo al cargar reportes se muestre como error reintentable, para no tomar decisiones sobre datos ficticios.
- Como **ciudadano con cuenta tradicional**, quiero recibir un enlace de recuperación válido, para recuperar el acceso sin depender de soporte manual.
- Como **equipo de QA**, quiero conocer la versión y el entorno probados, para que la evidencia corresponda exactamente al software desplegado.
- Como **administrador**, quiero ver los avisos del ciudadano y registrar el estado oficial, para que el seguimiento sea consistente en el panel y en «Mis reportes».
- Como **equipo**, quiero probar correos únicamente en un buzón QA, para validar la integración sin contactar a la Alcaldía ni a empresas reales.

## 4. Requisitos

| ID | Requisito («el sistema deberá…») | Prioridad | Origen |
| --- | --- | --- | --- |
| RF-CQA-01 | Mostrar un estado de error cuando falle la carga de reportes administrativos, sin sustituirla por datos ficticios, y permitir reintentar la consulta | Esencial | H141-01; RF-19, RF-22 |
| RF-CQA-02 | Mantener los datos simulados aislados de cualquier build y comportamiento de producción | Esencial | H141-01; ADR-0005 |
| RF-CQA-03 | Mantener utilizable el botón «Continuar» en una pantalla móvil aunque Netlify inyecte elementos públicos | Esencial | H141-02 |
| RF-CQA-04 | Enviar un enlace de recuperación a una cuenta tradicional elegible mediante Resend, con vigencia de una hora y un solo uso, sin revelar si una cuenta existe | Esencial | RF-02; ADR-0002; contrato API §4 |
| RF-CQA-05 | Registrar evidencia operativa suficiente para distinguir una solicitud no elegible, limitada, aceptada o fallida, sin registrar el token, la contraseña ni datos personales innecesarios | Esencial | H141; RNF-02 |
| RF-CQA-06 | Evitar que un fallo del proveedor deje un enlace utilizable no entregado o agote indebidamente las oportunidades de recuperación de la cuenta | Esencial | H141; seguridad de recuperación |
| RF-CQA-07 | Identificar para cada prueba el commit de frontend, el commit de backend y el entorno autorizado, sin exponer valores de variables o secretos | Esencial | Pendiente NOT-141; RNF-despliegue |
| RF-CQA-08 | Verificar mediante API que un reporte QA y sus fotografías quedan almacenados y pueden consultarse por la cuenta autorizada | Esencial | RF-05, RF-08; ADR-0006 |
| RF-CQA-09 | Enviar las pruebas de Alcaldía y de empresa únicamente al buzón QA; la empresa con correo será Centroaguas y los canales alternativos no se tratarán como correos | Esencial | RF-12, RF-14, RF-28; RN-15, RN-18 |
| RF-CQA-10 | Mostrar al administrador el aviso pendiente del ciudadano en `NU-000010`, permitir registrar el cambio oficial a «Resuelto» y reflejarlo con su historial en «Mis reportes» | Esencial | RF-19; RN-19; contrato API §4 y §5 |
| RF-CQA-11 | Disponer de cuentas QA tradicionales y permisos controlados para registro, recuperación, administración y eliminación de datos, sin usar cuentas personales ajenas al equipo | Esencial | Pendiente NOT-141; ADR-0008 |

Los identificadores `RF-CQA-*` son locales a esta corrección y no crean nuevos requisitos permanentes del producto. Al cerrar se enlazarán con los requisitos estables que validan.

## 5. Criterios de aceptación

- **CA-01 — Error real en el mapa.** _Dado_ que `GET /api/admin/reportes` falla por red, autenticación, autorización o error del servidor, _cuando_ el administrador abre el mapa, _entonces_ no se muestra ningún reporte ficticio, se presenta un mensaje claro y aparece una acción «Reintentar».
- **CA-02 — Reintento.** _Dado_ el error anterior, _cuando_ la API vuelve a estar disponible y el administrador pulsa «Reintentar», _entonces_ se muestran únicamente los reportes devueltos por la API y se retira el error.
- **CA-03 — Producción sin datos ficticios.** _Dado_ el build de producción, _cuando_ se inspeccionan sus recursos y se fuerza un fallo de la API en un entorno autorizado, _entonces_ los textos, códigos y coordenadas de `reportesPrueba` no se usan ni aparecen como resultado del fallo.
- **CA-04 — Botón móvil accesible.** _Dada_ una pantalla móvil de 360 px de ancho con el distintivo público de Netlify visible, _cuando_ el usuario completa el paso correspondiente, _entonces_ «Continuar» permanece visible, enfocable y pulsable sin quedar cubierto.
- **CA-05 — Recuperación tradicional.** _Dada_ una cuenta QA tradicional, verificada y con contraseña, _cuando_ solicita una recuperación dentro del límite, _entonces_ Resend acepta el envío, el mensaje llega al buzón QA y el enlace abre la ruta desplegada para crear una nueva contraseña.
- **CA-06 — Enlace seguro.** _Dado_ el enlace recibido, _cuando_ se usa una vez dentro de la hora siguiente, _entonces_ permite cambiar la contraseña, elimina el bloqueo de ingreso si existía y no vuelve a funcionar después; un enlace vencido o reutilizado devuelve el error definido por el contrato.
- **CA-07 — Privacidad de la solicitud.** _Dadas_ una cuenta inexistente y una cuenta creada solo con Google, _cuando_ se solicita recuperación, _entonces_ la respuesta pública es la misma que para una cuenta tradicional y no se envía ningún correo.
- **CA-08 — Fallo del proveedor.** _Dada_ una falla controlada de Resend en un entorno no productivo, _cuando_ se solicita la recuperación de una cuenta QA elegible, _entonces_ la respuesta pública continúa siendo genérica, el diagnóstico interno permite reconocer el fallo sin revelar el token ni la contraseña y el intento fallido no impide una solicitud posterior válida.
- **CA-09 — Versión y configuración verificables.** _Dado_ cada despliegue usado en QA, _entonces_ la evidencia identifica rama y commit de Netlify y Render, y confirma solo la presencia y forma esperada de las variables necesarias, nunca sus valores.
- **CA-10 — Reporte y fotografías.** _Dada_ una cuenta QA autorizada, _cuando_ crea un reporte con fotografía, _entonces_ la API devuelve su código, el reporte aparece en su consulta, la fotografía queda asociada y su recurso se puede recuperar; no se usan datos personales reales innecesarios.
- **CA-11 — Correos dirigidos al buzón QA.** _Dados_ un reporte de categoría de Alcaldía y uno de Centroaguas, _cuando_ se procesan sus envíos con el destino de prueba activo, _entonces_ ambos llegan exclusivamente al buzón QA, se conserva la evidencia del proveedor y ningún mensaje llega a la entidad real.
- **CA-12 — Aviso administrativo.** _Dado_ que el ciudadano de `NU-000010` dejó un aviso como última entrada del historial, _cuando_ el administrador consulta el listado y el detalle, _entonces_ ve «Aviso del ciudadano» y su contenido sin que el aviso cambie por sí mismo el estado oficial.
- **CA-13 — Resolución oficial.** _Dado_ `NU-000010` con aviso pendiente, _cuando_ el administrador lo cambia oficialmente a «Resuelto», _entonces_ el historial registra actor, fecha, estado y observación, y el ciudadano lo ve en «Mis reportes» dentro de «Resueltos».
- **CA-14 — Eliminación controlada.** _Dada_ una cuenta QA desechable con un reporte y una fotografía, _cuando_ se prueban «Eliminar mis datos» y, en otra preparación independiente, «Eliminar mi cuenta», _entonces_ se cumplen las respuestas y restricciones del contrato, incluida la eliminación de las fotografías, sin afectar cuentas o reportes ajenos.
- **CA-15 — Evidencia sin repetición.** _Dado_ el cierre de las correcciones, _entonces_ existe una matriz por criterio con fecha, entorno, commit, resultado y evidencia; no se repiten las pruebas móviles ni la importación de las 35 solicitudes de Postman ya aceptadas.

## 6. Reglas y restricciones que aplican

- La respuesta de recuperación seguirá siendo genérica para impedir enumeración de cuentas.
- `RESEND_PRUEBA_DESTINO` permanecerá activo durante todas las pruebas de reportes. No se contactará a entidades reales.
- La recuperación se probará con una cuenta QA tradicional cuyo correo sea el propio buzón QA; el desvío de reportes no se asumirá aplicable a correos de recuperación.
- No se leerán, imprimirán ni documentarán valores de secretos de Render, Resend, Neon, Cloudinary o Google.
- Las pruebas que creen, cambien o eliminen información usarán únicamente cuentas y reportes QA autorizados.
- Una nota del ciudadano no cambia el estado oficial; solo el administrador puede registrar «Resuelto».
- RN-15: gas, energía y aseo conservan sus canales alternativos; Centroaguas representa el caso de empresa con correo.
- ADR-0002 fija Resend; ADR-0006 fija Cloudinary; ADR-0008 fija privacidad, verificación y eliminación.
- Interfaz en español, mensajes claros y sin detalles técnicos sensibles.
- Las operaciones externas con efectos reales se ejecutarán únicamente en la fase de pruebas, después de aprobar spec, plan y tareas.

## 7. Fuera de alcance

- Repetir las pruebas móviles ya evidenciadas por Amelia.
- Reimportar o volver a ejecutar en bloque las 35 solicitudes temporales de Postman.
- Enviar correos al CAIC, Centroaguas u otra entidad real.
- Activar el envío real retirando `RESEND_PRUEBA_DESTINO`.
- Cambiar de Resend, Cloudinary, Render, Netlify o Neon, o contratar un plan de pago.
- Resolver el arranque en frío de Render o cerrar NOT-133.
- Crear notificaciones por correo o push para el administrador; el aviso requerido vive en el panel.
- Cambiar las reglas funcionales de recuperación para cuentas creadas solo con Google.
- Modificar datos de producción que no pertenezcan a las cuentas QA autorizadas.

## 8. Decisiones cerradas para esta corrección

- **Entorno QA autorizado:** producción actual con cuentas, buzones y reportes controlados. Los fallos intencionales de proveedores y las pruebas previas se ejecutan localmente con PostGIS desechable y dobles de prueba.
- **Identidades QA:** Andrés autorizó dos buzones propios y confirmó la cuenta administradora. En el repositorio y las evidencias se nombran `Cuenta QA-A`, `Cuenta QA-B` y `Administrador QA`; no se escriben correos ni credenciales.

## 9. Aprobación

- [x] La especificación está completa y sin ambigüedades.
- [x] Las preguntas abiertas están resueltas o aceptadas como riesgo.
- [x] **Aprobada por el usuario** → se puede redactar `plan.md`.
