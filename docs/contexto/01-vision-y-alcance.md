# Visión y alcance

## El problema

El ciudadano de Tuluá que quiere reportar un incidente urbano (malla vial, residuos
sólidos, servicios públicos, eventos comunitarios) no tiene una herramienta sencilla
y con evidencia. Las pruebas del equipo sobre los canales existentes mostraron:

- Tuluá App solo redirige al sitio web de la Alcaldía; no permite adjuntar foto ni ubicación.
- El registro en el portal institucional no pudo completarse.
- El correo electrónico, como alternativa, no exige estructura mínima: los reportes
  llegan con datos incompletos.
- Los canales están dispersos: cada entidad maneja su propia modalidad.

## Qué es NotiUrba

Un **facilitador**, no un reemplazo de los canales oficiales. Centraliza la
elaboración del reporte (categoría, foto, ubicación georreferenciada) y lo
estructura para remitirlo por el canal oficial correspondiente. Cada reporte se
clasifica por comuna (las 10 comunas urbanas de Tuluá) para ordenar las solicitudes
por sector e identificar las zonas con mayor concentración de incidentes.

## Objetivo general

Desarrollar, entre el 29 de septiembre y el 13 de diciembre de 2026, un MVP web
progresivo (PWA) que permita reportar incidentes urbanos con foto y coordenadas GPS,
clasificarlos en una de las 10 comunas y generar el reporte estructurado para su
envío a la Alcaldía y a las empresas de servicios públicos, respetando los canales
establecidos sin reemplazarlos.

## Objetivos específicos (SMART)

1. **Etapa de planificación** (meta: 18 de octubre de 2026). Cuatro entregables:
   requisitos y reglas de negocio, diseño técnico, prototipo navegable en Figma y
   propuesta de las 10 comunas (Alcaldía, 2019).
2. **Desarrollo** (20 de octubre – 29 de noviembre). PWA, backend y panel
   administrativo: registrar reportes con foto, ubicación y categoría, asignarlos a
   una comuna y visualizarlos en un mapa.
3. **Validación** (meta: 6 de diciembre). Mínimo 10 casos de prueba funcionales y 5
   sesiones con usuarios potenciales.

## Función central (criterio de alcance)

Un requisito entra al MVP solo si sostiene la función central: **ubicar el reporte
por comuna y hacerlo llegar a la entidad correspondiente.**

## Fuera del MVP

- App nativa iOS/Android: solo PWA.
- Radicación o integración con Orfeo, PQRSD u otros sistemas institucionales, y
  garantía del estado oficial del trámite.
- Verificación de fotos con IA: es manual.
- Despacho de policía o atención de emergencias en tiempo real.
- Sectorización por los 18 cuadrantes de la Policía (no se asume equivalencia con
  las comunas).
- Cobertura fuera de la zona urbana y barrios posteriores a 2019.
- Pagos y trámites ajenos al reporte.
- Soporte sin conexión y multilenguaje.
- Envío de correos sin intervención del ciudadano.
- Acceso de la Alcaldía/empresas al panel: el MVP tiene un solo administrador.
- Mapa público de reportes: expondría ubicaciones y fotos (Ley 1581 de 2012).
- Reportes anónimos: quien no quiera identificarse usa el PQRSDF anónimo de la Alcaldía.

## Restricciones y supuestos

- Plazo: 29 sep – 13 dic 2026, 3 sprints + 2 semanas de cierre, una entrega por sprint.
- Equipo de 3 personas con disponibilidad parcial.
- Presupuesto US$0: solo herramientas gratuitas, sin dominio propio.
- No hay vectores oficiales de las comunas para descarga directa.
- El docente aprobó (05/10/2026) usar las 10 comunas y no esperar respuesta de las entidades.
- La Alcaldía acepta el correo como canal válido si va a la entidad correcta y desde
  la cuenta del usuario.
- El ciudadano inicia sesión, porque el correo sale con su identidad.

Fuente: pestaña «1. Acta de constitución» del Google Doc.
