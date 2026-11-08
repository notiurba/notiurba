# NotiUrba — especificación de Crear reporte y revisión independiente de RF-03

Fecha: 7 de noviembre de 2026, Colombia. Rama: `fix/registro-pr20`.
HEAD inspeccionado: `e5d73c43da8331f3e6f524eda0724652471c4f83`.

**Actualización:** las secciones 1-12 conservan el diagnóstico de la revisión inicial. La sección 13 registra la implementación local y la 14, su validación posterior. La sección 15 documenta la corrección local del retorno tras autenticación y prevalece para ese comportamiento; los bloqueos de integración de la sección 14 continúan pendientes. Las matrices iniciales no describen ya todos los archivos actuales.

## 1. Veredicto y alcance

| Entregable | Estado | Motivo |
|---|---|---|
| Especificación escrita de Crear reporte | CONFORME | Este informe define 14 opciones del catálogo de enrutamiento, orden, campos, textos, estados y criterios de aceptación. No implica conformidad del producto. |
| Crear reporte: correspondencia del producto | REQUIERE AJUSTES | La ruta activa usa el catálogo general de 15 categorías; los canales y estados de envío no corresponden al enrutamiento integrado. |
| Actualización e inspección visual de Figma 4.1 | BLOQUEADO | El acceso al archivo principal fue rechazado; no se pudieron leer nodos, verificar la versión 4.1 ni editar pantallas. |
| RF-03 integral | PENDIENTE DE VERIFICACIÓN | Estado operativo: PENDIENTE DE INTEGRACIÓN. Hay evidencia histórica de persistencia, pero faltan alta nueva tradicional/Google, rechazo HTTP, revisión visual y despliegue de estos cambios locales. |

En la revisión inicial no se modificaron React, backend, migraciones, configuración de producción ni Figma. La continuación de la sección 13 sí modifica el frontend activo. No se hicieron commits, push, PR, fusiones, cambios de rama ni escrituras a la base. El documento temporal de privacidad se conserva y se amplía con hallazgos verificados.

## 2. Git y avances protegidos

`git status --short --branch` confirmó la rama obligatoria, asociada localmente a `origin/develop`, sin diferencias respecto al contexto recibido. Los cinco archivos modificados previamente y el componente nuevo siguen presentes:

- `frontend/src/compartido/componentes/avisos/NotaPrivacidad.jsx`
- `frontend/src/compartido/paginas/CompletarPerfil.jsx`
- `frontend/src/compartido/paginas/registro/PasoAcceso.jsx`
- `frontend/src/compartido/textos/avisoPrivacidad.js`
- `frontend/src/compartido/textos/avisoPrivacidad.test.mjs`
- Nuevo: `frontend/src/compartido/componentes/formulario/ConsentimientoRegistro.jsx`

El PR [#20 de Katherine](https://github.com/notiurba/notiurba/pull/20) está fusionado en `develop`: consulta GitHub de solo lectura, `merged=true`, merge `5e438067fd6088016e098517c4c8cd479cfa396a`. No se volvió a integrar.

## 3. Fuentes y límites de evidencia

1. Código local y contrato `docs/contexto/11-contrato-api.md`, especialmente 3.3–3.5. El contrato todavía describe 15 categorías generales y contiene comentarios históricos de funcionalidades ya incorporadas; no se tomó como prueba de despliegue.
2. Consultas SELECT reales al proyecto Neon `NotiUrba`, proyecto `restless-cake-46112011`, rama `production` (`br-proud-waterfall-b44u8y7p`), base `neondb`. Flyway informa V13–V16 aplicadas correctamente. No se leyeron nombres, correos, documentos ni credenciales de usuarios.
3. Se consultaron `categoria`, `configuracion_enrutamiento`, sus relaciones activas, metadatos del aviso y conteos agregados de usuarios y barrios. Son evidencia de base real; **no sustituyen una respuesta HTTP autenticada de Render**, inaccesible desde esta sesión.
4. Figma principal: [NotiUrba · Prototipo](https://www.figma.com/design/KKZ8DxmuORHgSKBJBkorwi/NotiUrba-%C2%B7-Prototipo?node-id=0-1). La creación de sesión y lectura de metadatos respondieron falta de acceso de edición. No se obtuvieron identificadores de pantallas. El intento de abrirlo en navegador agotó el tiempo. El [Figma anterior](https://www.figma.com/design/qRLcQ7jjC71F52CI6S9m1j/prototipo---notiurba?node-id=1-3) se mantuvo sin cambios y no se usó como sustituto del principal.
5. El acceso visual posterior al sitio público también se desconectó. No existen capturas verificadas de escritorio o móvil de esta revisión.

## 4. Hallazgo central: 15 categorías generales y 14 de enrutamiento

`GET /api/categorias` devuelve las activas, ordenadas por ID, con `idCategoria`, `nombre`, `descripcion`, `requiereUbicacion`, `requiereEvidencia`. Su controlador usa `findByActivaTrueOrderByIdCategoria`; las 15 filas actuales están activas.

`GET /api/enrutamiento/categorias` usa `EnrutamientoServicio.listar()`: une configuración, categorías, dependencias, entidades y canales alternos activos. Su consulta real en la base produjo **14 filas, IDs 1–14**, ordenadas por código A01–A10/E01–E04. Incluye `categoria` en lugar de `nombre`, además de `admiteMatricula`, `esGas`, `permiteCorreo`, `competenciaConfirmada`, `revisionClasificacion` y canal. No devuelve `descripcion` ni `requiereEvidencia`: no deben darse por presentes al adaptar el cliente.

La categoría 15, «Solicitud de información», sigue activa en el catálogo general, pero no tiene configuración de enrutamiento. No se debe borrarla ni reasignar su ID. Para este flujo de 14 opciones se recomienda consumir la lista de enrutamiento o conciliar ambas respuestas por `idCategoria`, coordinándolo con Katherine y Andrés. Ningún cambio de contrato o de visibilidad en backend fue ejecutado.

Los dos endpoints exigen sesión en `SecurityConfig`. Esto contradice el inicio público de `/reportar/*` y el mensaje «Deberá iniciar sesión solo al enviar el reporte». El equipo debe decidir entre pedir sesión antes de cargar las opciones o autorizar explícitamente un catálogo público mediante un cambio backend independiente.

## 5. Orden visible propuesto de las 14 categorías

Se conserva el orden visual de Katherine, retirando únicamente del alcance propuesto la 15 sin ruta. Es una especificación de diseño, todavía no aplicada. No se renumeran IDs ni se supone que el orden de la API sea el orden visual.

Todas las filas siguientes están activas **y** disponibles en la consulta real de enrutamiento. «REQUIERE AJUSTES» señala la integración del flujo/copy, no una categoría deshabilitada. El frontend activo imprime el nombre real recibido; los mocks conservan varios nombres anteriores.

| Orden | ID real / código | Nombre del catálogo real | Etiqueta UX propuesta | Ayuda breve propuesta | Frontend de Katherine | Estado |
|---|---|---|---|---|---|---|
| 1 | 1 / A01 | Vías, huecos, andenes e infraestructura | Vías, huecos y andenes | Huecos, grietas, andenes rotos y daños de infraestructura. | Posición 1; imprime nombre API; mock anterior | REQUIERE AJUSTES |
| 2 | 2 / A02 | Alumbrado, espacio público y convivencia | Alumbrado y convivencia | Luminarias apagadas, ocupación del espacio público o problemas de convivencia. | Posición 2; imprime nombre API; mock anterior | REQUIERE AJUSTES |
| 3 | 3 / A03 | Basuras, árboles y afectaciones ambientales | Basuras y ambiente | Acumulación de residuos, quemas, tala o afectaciones ambientales. | Posición 3; separar del servicio de recolección, ID 14 | REQUIERE AJUSTES |
| 4 | 4 / A04 | Señalización vial, semáforos y movilidad | Señales, semáforos y movilidad | Semáforos dañados, señales caídas o problemas de movilidad. | Posición 4; imprime nombre API | REQUIERE AJUSTES |
| 5 | 11 / E01 | Acueducto y alcantarillado | Agua y alcantarillado | Fugas de agua, falta del servicio o alcantarillas obstruidas. | Posición 5; no representa su canal alterno | REQUIERE AJUSTES |
| 6 | 13 / E03 | Energía eléctrica | Energía eléctrica | Apagones o daños en la red eléctrica; ante riesgo inmediato use el canal de emergencia. | Posición 6; pantalla final promete correo genérico | REQUIERE AJUSTES |
| 7 | 12 / E02 | Gas natural | Gas natural | Fallas no urgentes del servicio. Si huele a gas o hay fuga, llame al 164. | Posición 7; existe pregunta específica de emergencia | REQUIERE AJUSTES |
| 8 | 14 / E04 | Recolección de basuras y barrido | Recolección de basuras y barrido | El camión no pasó o hay una falla del servicio de aseo. | Posición 8; mock «Aseo (falla del servicio)» | REQUIERE AJUSTES |
| 9 | 8 / A08 | Desastres, inundaciones y deslizamientos | Inundaciones y deslizamientos | Reporte afectaciones; la competencia está pendiente de validación. No es atención inmediata. | Posición 9; no muestra advertencia de competencia | REQUIERE AJUSTES |
| 10 | 9 / A09 | Vivienda | Vivienda | Problemas de vivienda; la competencia está pendiente de validación. | Posición 10; no muestra advertencia de competencia | REQUIERE AJUSTES |
| 11 | 5 / A05 | Riesgos sanitarios y salud pública | Riesgos sanitarios y salud pública | Plagas, aguas estancadas u otras condiciones que afecten la salud pública. | Posición 11; imprime nombre API | REQUIERE AJUSTES |
| 12 | 6 / A06 | Uso del suelo y licencias urbanas | Construcciones y licencias | Construcciones sin licencia o problemas con el uso del suelo. | Posición 12; salta el paso de ubicación | REQUIERE AJUSTES |
| 13 | 7 / A07 | Infraestructura y servicios educativos | Colegios y servicios educativos | Problemas de infraestructura o servicios en colegios y escuelas. | Posición 13; salta ubicación | REQUIERE AJUSTES |
| 14 | 10 / A10 | Otro / no clasificado | Otro problema | Describa el caso si no encuentra una categoría. Se revisará su clasificación. | Actualmente posición 15, después de ID 15; debe ser última de estas 14 | REQUIERE AJUSTES |

No ofrecer la 15 como opción de este nuevo flujo hasta resolver su integración. No se afirma que esté inactiva. Las etiquetas UX son alias de presentación: en el resumen debe poder consultarse el nombre completo del catálogo. No cambiar el valor enviado.

### Flags y canales comprobados

- Ubicación requerida: IDs 1, 2, 3, 4, 5, 8, 11, 12, 13 y 14. No requerida: 6, 7, 9 y 10. Fotografías opcionales en las 14 (`requiere_evidencia=false`).
- `admite_matricula=true` solo para 11 en el enrutamiento actual. El servicio de creación general aún admite matrícula para categorías de empresas; la restricción de presentación debe usar el flag de ruta y debe coordinarse la diferencia.
- La expresión de `permiteCorreo` evaluada con las relaciones reales es verdadera para 1–10 y 12; falsa para 11, 13 y 14. «Activa» no equivale a «correo habilitado». No presentar estos tres casos como envíos de correo automáticos.
- 8 y 9 tienen `competenciaConfirmada=false`; 10 tiene `revisionClasificacion=true`. Mantener esas advertencias visibles sin presumir que la competencia está validada.
- En gas urgente, el resolvedor usa 164 y `permiteCorreo=false`. No basta con la ruta no urgente; reevaluar al cambiar la emergencia o al detectar riesgo en la descripción.

## 6. Especificación exacta para actualizar las pantallas existentes de Figma

**Estado de aplicación en Figma: BLOQUEADO. Cambios realizados: ninguno.** Conservar todos los IDs actuales cuando se recupere acceso; actualizar instancias existentes de móvil/escritorio y sus estados, sin crear otro proyecto ni sustituir las pantallas por imágenes. La versión «4.1» no pudo verificarse.

### Estructura común

- Encabezado «Crear reporte», acción «Cancelar» y título del paso. Progreso textual «Paso N de 4» con pasos «Categoría», «Ubicación», «Descripción y fotografías», «Revisar y confirmar». Marcar el actual semánticamente (`aria-current=step`) en la implementación futura.
- Paso 1: 14 opciones de selección única según la tabla. Nombre corto, ayuda de una o dos líneas y marca de selección; conservar radio real, teclado, foco y nombre accesible. No mostrar IDs/códigos como texto principal al ciudadano.
- Paso 2: mapa con punto editable, acción «Usar mi ubicación», dirección y barrio opcionales con los límites reales. Admitir la alternativa de barrio que soporta el servidor, cuando existan opciones; no bloquear preventivamente por una capa territorial distinta del backend. Sin ubicación requerida, mostrar «Esta categoría no requiere ubicación» y permitir «Continuar sin ubicación». Esto mantiene cuatro pasos visibles; difiere del salto actual 1→3 y debe acordarse con Katherine.
- Paso 3: descripción obligatoria, contador, fotos opcionales y matrícula únicamente si `admiteMatricula`. Permitir quitar cada archivo. Mostrar nombre/miniatura, estado de preparación/subida y reintento. No perder archivos válidos por una selección incorrecta adicional.
- Paso 4: categoría y nombre completo, ubicación o «No requerida», descripción, fotos y canal de atención resuelto; enlaces «Cambiar» al paso correspondiente. Una sola casilla de autorización de envío, inicialmente desmarcada, con acceso al aviso v3. Mantenerla separada del consentimiento de registro.
- Acción final «Guardar y solicitar envío» si `permiteCorreo`; «Guardar reporte» si solo hay canal alterno. En este último caso mostrar «Abrir canal oficial» como acción separada y explicar que requiere continuar allí. Para gas urgente destacar «Llamar al 164» sin prometer correo. No mostrar «Alcaldía / CAIC» de forma fija para categorías de empresas.

### Distribución y accesibilidad que debe quedar especificada

| Aspecto | Móvil | Escritorio |
|---|---|---|
| Tamaños de verificación pendientes | 360×800 y 390×844, más 320 px sin desbordamiento horizontal | 1280×800 y 1440×900 |
| Contenido | Una columna; márgenes 16 px; grupos compactos, separación 12–16 px | Contenedor centrado hasta 1120 px, margen 24–32 px; categorías en 2 columnas; mapa/datos en 2 columnas; lectura de texto limitada |
| Categorías | Tarjetas compactas, altura según texto; desplazamiento natural para 14 opciones | Rejilla 2 columnas en orden de lectura fila a fila; no renumerar valores |
| Acciones | Pie pegajoso con zona segura; se vuelve estático con teclado/poca altura | Pie alineado con el formulario; «Atrás» secundario y «Continuar» primario |
| Interacción | Objetivos de al menos 44×44 px, etiquetas fuera del placeholder | Igual; recorrido lógico con Tab, flechas en radios y activación con teclado |
| Lectura | Tipografía de cuerpo 16 px, ayudas 14 px; sin texto cortado al 200 % | Igual; no estirar líneas a todo el ancho |
| Color y foco | Verde de marca, foco visible; error con icono/texto, no solo rojo | Igual; medir contraste 4.5:1 en texto normal y 3:1 en componentes/texto grande |

Son requisitos de diseño, **no mediciones realizadas**. El CSS actual mantiene el marco en 430 px también en escritorio: es una diferencia verificada por código, no por captura. No intentar encajar 14 categorías sin ningún desplazamiento sacrificando legibilidad; eliminar espacio innecesario y conservar el desplazamiento que requiera el contenido.

### Navegación y estados

«Atrás» conserva campos; «Cancelar» solicita confirmación solo cuando hay contenido. Al cambiar categoría, reevaluar ruta, ubicación requerida y matrícula; invalidar la autorización de envío si cambian datos relevantes. Al pasar de paso, colocar foco en el título; al fallar validación, foco en el primer campo y ayuda/error asociados con `aria-describedby` y `aria-invalid`.

Catálogo: cargando, disponible, vacío, fallo con «Reintentar» y sesión vencida con regreso al borrador. Lista vacía no debe presentarse como éxito sin opciones. No habilitar continuación con categoría no presente en la lista vigente.

Registro de reporte: distinguir «Guardando reporte», «Preparando foto», «Subiendo foto N de M» y «Solicitando envío». Conservar `codigoSeguimiento` en cuanto exista. Un fallo de foto o correo no debe crear otro reporte. Reutilizar `guardarConEvidencias` o adaptar su lógica, sin sustituir el flujo de Katherine por la otra página completa.

Confirmación inicial «Reporte guardado» y código de seguimiento NotiUrba. Solo mostrar «Correo aceptado por el proveedor» cuando el estado real lo permita; nunca afirmar entrega, lectura o radicación oficial. Estados separados: pendiente, aceptado por proveedor, fallo, rebote y canal externo sin correo.

## 7. Textos finales propuestos para el diseño

Estos textos quedan definidos para la especificación; aún no están aplicados en Figma ni en React. Se usa «usted» para mantener consistencia con Katherine. Los mensajes del backend siguen conservando su contrato.

| Campo / estado | Etiqueta / placeholder | Ayuda | Error y condición exacta |
|---|---|---|---|
| Categoría | «Tipo de problema»; sin placeholder en radios | «Seleccione el tipo de problema que desea reportar.» | «Seleccione una categoría para continuar.» si está vacía; «Esta categoría ya no está disponible. Elija otra.» si no pertenece al catálogo actual |
| Catálogo cargando | «Cargando categorías…» | Estado anunciado sin mover el foco | Sin error mientras carga; no habilitar selección |
| Catálogo vacío | «No hay categorías disponibles» | «Inténtelo más tarde.» | Solo respuesta válida con lista vacía; no confundir con error de red |
| Catálogo fallido | «No pudimos cargar las categorías» | «Revise su conexión e inténtelo de nuevo.» | Fallo de red/servidor; botón «Reintentar». Con 401 mostrar acceso requerido, no un error genérico repetitivo |
| Ubicación | «Ubicación del problema» | «Marque el lugar en el mapa o use su ubicación. También puede elegir un barrio disponible.» | «Marque una ubicación o seleccione un barrio para continuar.» si la categoría exige lugar y no hay par de coordenadas ni barrio |
| Par de coordenadas | No exponer campos técnicos en el flujo principal | — | «Indique la ubicación completa.» si solo hay latitud o longitud. Rangos API: latitud −90…90, longitud −180…180 |
| Ubicación no requerida | «Ubicación opcional» | «Esta categoría no requiere ubicación.» | No generar error por ausencia; acción «Continuar sin ubicación» |
| GPS denegado / no disponible | Acción «Usar mi ubicación» | «También puede marcar el lugar en el mapa.» | «No pudimos obtener su ubicación. Marque el punto en el mapa o elija un barrio.» si el navegador falla, no tiene GPS, vence el tiempo o no da permiso |
| Fuera de cobertura | «Ubicación fuera de cobertura» | «Revise el punto marcado.» | «El punto está fuera de la cobertura disponible. Elija otra ubicación.» únicamente con respuesta 422 `FUERA_DE_COBERTURA`; no derivarlo solo de una capa frontend distinta |
| Dirección | «Dirección del incidente (opcional)»; «Ej. Cra. 26 # 28-40» | «Agregue una referencia para encontrar el lugar.» | «Use máximo 300 caracteres.» si supera el límite API. La pantalla actual limita a 150: diferencia a acordar, no cambio automático |
| Barrio | «Barrio (opcional si marcó el lugar)»; «Seleccione un barrio» | «Si no marcó un lugar, seleccione un barrio disponible.» | «Seleccione un barrio o marque el lugar.» cuando falta ubicación requerida. Si catálogo vacío: «No hay barrios disponibles. Marque el lugar en el mapa.» |
| Descripción | «Descripción del problema»; «Describa qué ocurre, desde cuándo y cómo afecta a la comunidad.» | «Escriba entre 10 y 500 caracteres. No incluya datos personales de terceros.» | Vacía: «Escriba una descripción del problema.»; 1–9 tras recortar espacios: «Escriba al menos 10 caracteres.»; más de 500: «Use máximo 500 caracteres.» |
| Fotografías | «Fotografías del problema (opcional)» | «Agregue hasta 3 fotos JPG o PNG. Prepararemos cada foto para que no supere 1 MB.» | Cuarta foto: «Puede adjuntar hasta 3 fotos por reporte.»; MIME no admitido/415: «Seleccione una imagen JPG o PNG.»; archivo vacío/400: «La foto está vacía. Elija otra.» |
| Foto ilegible | «No pudimos leer la foto» | Acción «Elegir otra foto» | «No pudimos leer esta foto. Pruebe con otra.» si falla la lectura de imagen |
| Foto no reducible / 413 | «La foto supera el tamaño permitido» | «Elija una foto de menor tamaño.» | «No pudimos preparar esta foto para que pese hasta 1 MB. Pruebe con otra.» si falla la reducción o el servidor rechaza el tamaño; 1 MB = 1 048 576 bytes |
| Subida de foto fallida / 502 o red | «No pudimos subir la foto» | «Su reporte está guardado. Puede reintentar esta foto.» | Activar tras fallo de subida de una foto, conservando código y evidencias ya subidas |
| Privacidad de fotos | «Las fotos tendrán un enlace público» | «Cualquier persona con el enlace podrá verlas. Evite rostros, menores, documentos y placas.» | Advertencia informativa, no una casilla adicional |
| Matrícula | «Matrícula o cuenta del servicio (opcional)»; «Ej. número de cuenta» | «Es el número que aparece en su factura.» | «Use máximo 50 caracteres.» si supera 50; ocultar y limpiar cuando `admiteMatricula=false` |
| Gas | «¿Es una fuga o emergencia?» | «Si huele a gas o hay riesgo inmediato, llame al 164.» | «Una emergencia de gas no se atiende por correo. Llame al 164.» al elegir emergencia o detectar riesgo; acción «Llamar al 164» |
| Autorización de envío | Una casilla, sin placeholder | «Puede consultar el aviso de privacidad antes de autorizar.» | «Autorice el envío de este reporte para continuar.» al intentar envío sin aceptar; inicialmente falsa; no inferirla por crear cuenta |
| Fallo al crear | «No pudimos guardar su reporte» | «Sus datos siguen aquí.» | «Revise su conexión e inténtelo de nuevo.» solo si aún no existe código. No reutilizar este mensaje para un fallo de correo posterior |
| Fallo de correo | «El reporte está guardado, pero no pudimos solicitar el envío» | Mostrar código y acceso a seguimiento/reintento | Tras error de solicitud de envío; nunca reiniciar la creación |

Texto único de autorización de envío conservado del contrato: «Autorizo a NotiUrba a enviar este reporte a la entidad competente, a mi nombre y con mis datos, ubicación y fotos (con enlace público). La entidad podrá responderme a mi correo.» Añadir enlace visible «Aviso de privacidad» al v3 sin perder el borrador. Para casos sin correo automático, acordar con el equipo cómo describir la autorización y acción final sin afirmar un envío inexistente; no cambiar unilateralmente su alcance legal.

## 8. Matriz API / Figma / React

| Elemento | API / base comprobada | Figma | Frontend activo | Resultado |
|---|---|---|---|---|
| Categorías | General: 15 activas; enrutamiento: 14 disponibles | BLOQUEADO | Consume `/api/categorias` | REQUIERE AJUSTES |
| IDs y nombres | IDs 1–14 conservados, V16 renombra opciones | BLOQUEADO | Usa IDs reales y nombres de respuesta; mock anterior | REQUIERE AJUSTES para alias, mock y selección del catálogo |
| Orden | General por ID; enrutamiento por código | BLOQUEADO | `[1,2,3,4,11,13,12,14,8,9,5,6,7,15,10]` | REQUIERE AJUSTES: propuesta de 14 en sección 5 |
| Acceso | Ambos catálogos protegidos | BLOQUEADO | Inicio público, sesión al enviar | REQUIERE AJUSTES |
| Flujo | Ubicación condicional según categoría | BLOQUEADO | Cuatro pantallas, salto 1→3 si no requiere lugar | REQUIERE AJUSTES frente a cuatro pasos siempre visibles solicitados |
| Ubicación | GPS o barrio; PostGIS decide cobertura | BLOQUEADO | Exige punto, valida con POT frontend, bloquea si no identifica comuna | REQUIERE AJUSTES |
| Descripción | 10–500 tras trim | BLOQUEADO | Coincide en validación; ayuda solo dice mínimo | CONFORME en límites por código; copy REQUIERE AJUSTES |
| Dirección | Hasta 300 | BLOQUEADO | Hasta 150 | REQUIERE AJUSTES / acordar límite UX |
| Fotos | Opcionales, máximo 3, JPG/PNG, 1 MB por subida | BLOQUEADO | Reduce antes de subir; no rechaza peso inicial; contador; fallo parcial contado | CONFORME en límites por código; recuperación integrada PENDIENTE DE VERIFICACIÓN |
| Matrícula | Enrutamiento: solo ID 11 habilitado | BLOQUEADO | Se muestra para todas; ayuda dice agua, gas o energía | REQUIERE AJUSTES |
| Emergencia gas | Resolvedor evita correo, usa 164 | BLOQUEADO | Pregunta solo en paso 1; no consulta ruta al cambiar descripción | REQUIERE AJUSTES |
| Canal/destinatario | 11/13/14 sin correo automático; 8/9 competencia pendiente | BLOQUEADO | Confirma CAIC fijo, no consulta la ruta | REQUIERE AJUSTES |
| Consentimiento de envío | `autorizaEnvio=true` exigido en request | BLOQUEADO | Casilla falsa inicialmente; botón bloqueado, payload fija true; falta enlace | REQUIERE AJUSTES: validación del handler y consulta v3 |
| Confirmación | Guardado, solicitud aceptada y entrega son estados distintos | BLOQUEADO | «Reporte enviado» incluso si falló solicitar correo | REQUIERE AJUSTES |
| Responsive | No prescribe ancho | BLOQUEADO | CSS máximo 430 px también en escritorio | REQUIERE AJUSTES para diseño escritorio; visual PENDIENTE DE VERIFICACIÓN |
| Teclado/lectores | Contrato no define presentación | BLOQUEADO | Radios nativos y errores alert; falta verificar foco y recorrido | PENDIENTE DE VERIFICACIÓN |

## 9. Ajustes concretos para Katherine, sin modificar su código

Las líneas son referencias del HEAD y archivos locales inspeccionados; pueden desplazarse al editar.

| Prioridad | Archivo / punto | Diferencia y corrección recomendada |
|---|---|---|
| Alta | `frontend/src/App.jsx:18`, `:34`; `frontend/src/app/rutas.jsx` | Solo `/reportar/*` está conectado. `app/paginas/CrearReporte.jsx` no tiene importación/ruta. Integrar selectivamente capacidades de enrutamiento en los pasos existentes; no dar por usadas sus pruebas solo por existir. |
| Alta | `frontend/src/compartido/api/reportes.js:79`; `app/reportar/ReportarLayout.jsx` | Elegir la lista de enrutamiento de 14 o adaptar por ID; conciliar `nombre`/`categoria` y ausencia de descripción; resolver autenticación con Andrés. |
| Alta | `frontend/src/compartido/utilidades/categorias.js:49`; `src/simulacion/datos/catalogos/categorias.json` | Orden incluye ID 15 y mocks conservan nombres previos. Aplicar tabla acordada sin reasignar IDs; mantener legado fuera del alcance del nuevo flujo. |
| Alta | `frontend/src/app/reportar/PasoUbicacion.jsx:44`, `:74`; `compartido/validaciones/reporte.js` | No tratar límites/solapes o diferencias de capa como fuera de Tuluá definitivamente. Permitir barrio conforme al servidor y mostrar 422 real. |
| Alta | `frontend/src/app/reportar/PasoDetalles.jsx:157` | Mostrar matrícula según `admiteMatricula`; limpiar valor si ya no aplica. No usar solo la ayuda para restringir datos. |
| Alta | `frontend/src/app/reportar/PasoConfirmacion.jsx:122`, `:160` | Sustituir CAIC fijo por entidad y canal resueltos; advertir competencias pendientes y clasificación de Otro. No afirmar correo para 11/13/14. |
| Alta | `frontend/src/compartido/api/reportes.js:58`, `:68`; `app/reportar/ReporteEnviado.jsx` | No ocultar fallos de solicitud de envío con `.catch(() => null)` para luego declarar «Reporte enviado». Conservar código y distinguir guardado/pendiente/canal externo/fallo; reutilizar recuperación ya disponible. |
| Alta | `frontend/src/app/reportar/PasoConfirmacion.jsx:59`, `:93`, `:142` | El bloqueo está en botón; `pedirEnvio` no comprueba aceptación y `armarSolicitudReporte` fuerza true. Validar explícitamente la casilla antes de abrir confirmación y antes de enviar; enlazar v3. Es un hallazgo de código, no un ataque HTTP ejecutado. |
| Media | `frontend/src/app/reportar/PasoCategoria.jsx:44`, `:64` | Handler debe validar selección vigente y gas, no depender solo de disabled. Definir estado vacío y mensaje de selección; no habilitar si el catálogo falló y hay un ID antiguo. |
| Media | `frontend/src/app/reportar/PasoCategoria.jsx:48`; `PasoDetalles.jsx` | Acordar paso 2 informativo para categorías sin ubicación para conservar cuatro pasos visibles sin imponer dato no requerido. |
| Media | `frontend/src/app/reportar/PasoDetalles.jsx:27`, `:57` | «Continuar» no depende de `reduciendo`; revisar avance durante preparación. Fotos viven en memoria del layout y pueden perderse si se desmonta al ir al login: verificar y conservar de forma explícita, sin almacenar datos sensibles innecesarios. |
| Media | `frontend/src/app/componentes/MarcoApp.css`; `app/reportar/Reportar.css` | Ancho 430 px y categorías en una columna en escritorio. Aplicar composición responsive de sección 6 tras comparar Figma. |
| Media | `frontend/src/app/paginas/CrearReporte.jsx:49`, `:55`, `:57` | Página alternativa usa otra consulta y orden de API; navega a `/app/consultar-reporte`, ausente del router actual. No activarla sin adaptar rutas y preservar el flujo de Katherine. |

## 10. RF-03: revisión separada

| Comprobación | Evidencia | Estado |
|---|---|---|
| Componente único tradicional/Google | `ConsentimientoRegistro.jsx`, reutilizado en `PasoAcceso` y `CompletarPerfil` | CONFORME por inspección |
| Inicialmente desmarcada | `DATOS_PERFIL_VACIOS.aceptaPrivacidad=false` | CONFORME por inspección; visual pendiente |
| Texto y mayoría de edad | «Soy mayor de 18 años. He leído el aviso de privacidad y autorizo el tratamiento de mis datos personales para las finalidades informadas.» | CONFORME en código |
| Bloqueo frontend tradicional | `validarPasoAcceso` genera error antes de llamar a API | CONFORME en prueba de validación |
| Bloqueo Google y payload | `CompletarPerfil.jsx:37` retorna por errores; envía ambos booleanos del estado, no constantes | CONFORME por inspección; flujo real PENDIENTE DE VERIFICACIÓN |
| Enlace y conservación | `/aviso-privacidad`, `target=_blank`, `rel=noopener noreferrer`; no apunta a propuesta HTML | CONFORME por inspección; navegación real PENDIENTE DE VERIFICACIÓN |
| Aviso exacto | Prueba local V12 + comparación de texto vigente de base | CONFORME en correspondencia textual |
| Servidor acepta solo true | Requests de registro y Google: `@NotNull @AssertTrue`; controladores usan validación | CONFORME por inspección; ejecución HTTP BLOQUEADO |
| Persistencia histórica | Base: 4 usuarios con v3 y 4 con timestamp; 1 histórico con 1.0 y timestamp | CONFORME para existencia de datos históricos, no para alta nueva |
| Nuevo registro / Google y evidencia de fecha-versión | Falta ejecutar alta real aislada, leer filas y comprobar rechazo sin autorización | PENDIENTE DE VERIFICACIÓN |
| Catálogo de barrios | Base: 1 barrio total, 0 no internos; ambos registros exigen uno | BLOQUEADO para completar alta con catálogo real actual |
| Errores | `useFormulario.errorDeCampo` maneja campos existentes; `declaraMayorEdad` no forma parte de `datos` y caería en mensaje general | REQUIERE AJUSTES menor de correspondencia de campo; no se modificó |
| Escritorio/móvil y Google Identity | Navegador no permitió completar inspección; Google requiere configuración efectiva del cliente | BLOQUEADO visual; configuración desplegada PENDIENTE DE VERIFICACIÓN |
| Despliegue de cambios locales | Cambios sin commit/push; no se publicó nada | PENDIENTE DE VERIFICACIÓN |

La consulta del aviso vigente devolvió versión `3`; MD5 del texto con CRLF normalizado a LF: `bda5e59f541ffb1181eec839ac7fa601`, idéntico a `avisoComoTextoPlano()` local. Es una comprobación de igualdad, no una firma ni una auditoría jurídica.

`RegistroServicio` usa `catalogos.avisoPrivacidadVigente()` y `OffsetDateTime.now()`. `GoogleAuthServicio` usa el mismo mecanismo; `Usuario` asigna `idAvisoPrivacidad` y `aceptoPrivacidadEn`. Los timestamps históricos v3 van de `2026-11-03T04:33:04.128Z` a `2026-11-03T05:11:37.194Z`. Ninguna alta fue creada en esta revisión.

La prueba `completarSinAceptarPrivacidadResponde400` de `GoogleAuthControllerTest` envía realmente `aceptaPrivacidad=true` y `declaraMayorEdad=false`: no demuestra rechazo de `aceptaPrivacidad=false`. Registrar la carencia para el responsable de backend; no modificar pruebas o producción para ocultarla. La prueba de integración del aviso comprueba catálogo/versión, no realiza por sí sola altas nuevas de ambos flujos.

La propuesta documental `propuesta 2026-11-06` y su HTML se conservan separados. No reemplazar v3 sin validación, coordinación y una migración/versionado nuevos. Documento y dirección siguen sujetos a decisión del equipo y del profesor.

## 11. Maven, Docker, pruebas y despliegue

### Maven / Testcontainers

- Java disponible por ruta: Temurin OpenJDK 21.0.13. Maven Wrapper instalado: Apache Maven 3.9.9. `JAVA_HOME` no estaba configurado en el shell; establecerlo **solo en el proceso** permitió `mvn -version` correcto.
- `backend/pom.xml` fija `testcontainers.version=1.21.4`. La versión existe en [Maven Central](https://central.sonatype.com/artifact/org.testcontainers/testcontainers-bom/1.21.4).
- No existe 1.21.4 en las dos cachés inspeccionadas: `C:/Users/Usuario/.m2/repository` y `C:/notiurba/validation-tools/m2`. Hay otras versiones, que no se sustituyeron.
- El settings de validación configura la caché writable, repositorio local `file:///C:/Users/Usuario/.m2/repository` y Central `https://repo.maven.apache.org/maven2`; no se detectó en ese archivo un mirror que redirija Central.
- `curl` a Central devolvió `Could not resolve host`. Maven **sin modo offline**, con `-U` y la selección de pruebas de registro/Google/aviso, volvió a fallar al construir el modelo: `testcontainers-bom:1.21.4 (absent)`, `Host desconocido (repo.maven.apache.org)`. Los errores de versión de junit-jupiter/postgresql son consecuencia de no resolver el BOM. No llegaron a ejecutarse las pruebas.
- Docker Client 29.8.2 accesible, contexto `desktop-linux`; el motor devolvió `permission denied ... npipe:////./pipe/dockerDesktopLinuxEngine`. Esto no prueba que Docker esté apagado en la sesión de Windows del usuario.
- No se pidió elevación ni se modificaron POM, versiones, repositorios o exclusiones. Tampoco se ejecutaron integraciones contra producción: las pruebas de base temporal crean/borran bases y requieren un servidor de pruebas accesible.

### Pruebas ejecutadas en esta revisión

Se reutilizaron seis archivos, sin crear tests ni repetir las 75 pruebas completas. `node --test` con:

```text
src/compartido/textos/avisoPrivacidad.test.mjs
src/compartido/validaciones/registro.test.mjs
src/compartido/validaciones/reporte.test.mjs
src/compartido/utilidades/fotos.test.mjs
src/app/componentes/estadoCorreo.test.mjs
src/app/servicios/recuperacionReporte.test.mjs
```

Resultado: **33 aprobadas, 0 fallidas, 0 omitidas**. Separación: 13 de aviso/registro; 11 de validaciones de reporte/fotos; 9 de estado y recuperación del correo. Estas últimas prueban funciones de apoyo; no certifican que el router activo las use. No cubren captura visual, interacción Google real, HTTP autenticado, subida real ni persistencia de una nueva aceptación. Las 75 pruebas/build/lint anteriores se mantienen como antecedente y no se presentan como verificaciones visuales nuevas. Solo se modificó documentación durante esta revisión, por lo que no se repitió compilación ni lint de React.

### Despliegue existente, sin cambios

La conexión Netlify disponible solo listó `notiurba-prototipo-evaluacion` (sitio `f3884c7d-91a2-4c0c-a1e2-c56d0a86030e`), un proyecto independiente. Su deploy `6ae7f0aec2daf1283537a5eb` está `ready`, publicado el 2 de noviembre, origen CLI, sin `branch` ni `commit_ref`. No sirve como prueba del despliegue del repositorio oficial.

El sitio oficial documentado es [notiurba.netlify.app](https://notiurba.netlify.app); se intentó consultar `/registro`, sin conseguir inspección visual. La consulta de Render tampoco pudo resolver DNS desde terminal. No fue posible identificar de forma fiable el commit desplegado del sitio oficial ni comparar bundles o verificar sus redirecciones. No se cambió configuración ni se publicó versión alguna.

## 12. Cierre pendiente y aceptación verificable

### Crear reporte

1. Recuperar acceso al Figma principal, localizar sus pantallas de Crear reporte y comprobar versión 4.1. Aplicar esta especificación a los nodos existentes y registrar IDs y capturas antes/después.
2. Acordar catálogo de enrutamiento de 14 frente al general de 15 y política de autenticación. Confirmar respuesta HTTP real de ambos endpoints y del resolvedor, con sesión de pruebas.
3. Katherine adapta el flujo existente, respetando IDs: tabla, estados vacíos, barrio/GPS, matrícula, canales y resultado del envío. Este informe no autoriza ni ejecuta cambios de contrato backend.
4. Validar los 14 IDs, selección única, categorías no disponibles, errores y cuatro pasos; verificar 8/9 con competencia pendiente, 10 con revisión, gas urgente y 11/13/14 sin correo.
5. Comprobar escritorio/móvil, teclado, foco, contraste y 200 %; verificar reintento de foto/correo sin duplicar el reporte y conservación del borrador al login. Hasta entonces el producto REQUIERE AJUSTES y la correspondencia con Figma queda PENDIENTE DE VERIFICACIÓN.

### RF-03

1. Resolver DNS/acceso de dependencias y permiso al entorno de pruebas; mantener Java 21, Maven 3.9.9 y Testcontainers 1.21.4.
2. Disponer de barrios reales seleccionables y configuración de Google para pruebas, sin usar el barrio interno como atajo.
3. En base aislada, probar registro tradicional y perfil Google con false, ausente y true; los dos primeros deben rechazar sin crear usuario. Para true, comprobar fila, timestamp de servidor, versión vigente y texto asociado.
4. Verificar visualmente consentimiento inicialmente falso, marcar/desmarcar, errores, aviso v3 en nueva pestaña y conservación de campos. Probar registro, verificación de correo y Google, sin sustituir por mocks.
5. Una vez autorizado un despliegue futuro, verificar implementación exacta y enlace desde el sitio oficial; no usar el prototipo independiente como evidencia.

`docs/privacidad/PENDIENTE_INTEGRACION_FRONTEND.md` **permanece** por instrucción expresa del usuario y porque RF-03 sigue pendiente de integración. No se elimina aunque este informe complete la especificación escrita.

## 13. Continuación: implementación local sobre Katherine

Fecha: 7 de noviembre de 2026. Rama y HEAD confirmados sin cambios: `fix/registro-pr20`, `e5d73c43da8331f3e6f524eda0724652471c4f83`, base `origin/develop` que ya incorpora main y el PR #20. No se cambiaron de rama ni se fusionaron otra vez. No se creó otro flujo o proyecto: `/reportar/*` sigue apuntando a los pasos de Katherine.

### 13.1 Problemas corregidos localmente

| Área | Cambio actual | Alcance comprobado / pendiente |
|---|---|---|
| Catálogo | El layout llama a `GET /api/enrutamiento/categorias` y adapta `categoria` a `nombre`, conservando nombre completo, ID y flags. Solo presenta las rutas disponibles de los IDs 1–14. No fabrica opciones ausentes, no escribe en el catálogo y no toca ID 15. | Pruebas unitarias de adaptación aprobadas; respuesta HTTP real pendiente. |
| Orden y copy | Alias y ayudas de la sección 5 aplicados mediante `catalogo.js`. Orden `[1,2,3,4,11,13,12,14,8,9,5,6,7,10]`. El resumen conserva el nombre completo de la API. | Probado sin mutar respuesta de entrada. El catálogo general y su mock histórico de 15 no se eliminan ni se renumeran. |
| Acceso | Sin sesión, el layout muestra acceso a login/registro en lugar de intentar repetidamente un catálogo protegido. Login tradicional conserva `state.siguiente`. | Decisión frontend provisional que respeta la API actual; el equipo debe confirmar si desea catálogo público y, en ese caso, autorizar aparte el backend. Google sigue su navegación existente: retorno al reporte requiere validación real. |
| Cuatro pasos | Selección siempre lleva a Ubicación; si no se requiere, el paso 2 lo explica y ofrece continuar sin ubicación. Atrás desde Descripción vuelve al paso 2. | Aplicado en el router/pasos existentes; navegación visual pendiente. |
| Ubicación | Acepta par GPS válido o barrio cuando se exige lugar. Permite omitirlo si no se exige. Retira rechazo local por POT/solape; cobertura final corresponde al servidor. Maneja 422 al crear. Dirección admite 300 caracteres. Se informa catálogo de barrios vacío o fallido. | Pruebas de barrio, coordenadas incompletas/fuera de rango y categoría sin ubicación aprobadas; HTTP/Mapa real pendiente. Al quitar punto se retira el marcador. |
| Matrícula | Se muestra únicamente según `admiteMatricula`; cambiar categoría limpia matrícula y ubicación previa. El payload también omite matrícula no admitida y ubicación no requerida usando la ruta confirmada. | Pruebas de payload aprobadas. No se cambió la regla backend. |
| Gas y emergencia | Se conserva pregunta de gas y llamada 164. Una emergencia explícita o indicio de fuga en la descripción vuelve a resolver la ruta en el paso 4. Respuestas antiguas no habilitan la nueva selección. | Tests de detección/clave y respuesta tardía aprobados; canales urgentes reales pendientes. No se sustituye la atención de emergencia por correo. |
| Destinatario | Confirmación usa `CanalOficial` con la ruta resuelta: entidad, dependencia, canal, revisión de clasificación y advertencia de competencia pendiente. Se retiran CAIC y Alcaldía fijos. | UI conectada a datos/flags de API; no implica validación administrativa de 8/9 ni de canales pendientes. |
| Autorización de envío | Casilla inicialmente falsa, distinta del registro; se comprueba tanto en submit como antes de confirmar. Payload no infiere true. Cambiar selección, emergencia o datos del canal invalida la aceptación. Añade enlace v3 en pestaña nueva reutilizando `EnlacePrivacidad`. | Pruebas de validación/payload aprobadas; interacción de casilla y navegación visual pendientes. Texto legal de envío existente conservado. |
| Guardado y recuperación | El flujo activo reutiliza `guardarConEvidencias`. Conserva código antes de subir fotos, confirma progreso de cada subida y guarda avance en sessionStorage. Fallar foto/solicitud deja el código y permite reintentar solo pendientes. Tras recargar puede volver a adjuntar archivos pendientes. | Tests de fallo parcial, reintento sin nueva creación, estado externo y respuesta perdida de foto aprobados. No se probó contra HTTP real. |
| Conciliación | En reintento consulta el reporte y su lista de fotos antes de subir, para no repetir una subida cuya respuesta se perdió. El POST de solicitud se deja idempotente según servidor. | Test con API simulada aprobado; comportamiento real pendiente. |
| Resultado | Mantiene la pantalla final existente, consulta `/api/reportes/{codigo}/envio` y usa `presentacionCorreo` para SIN_SOLICITAR/PENDIENTE/ENVIADO/FALLIDO/rebote/NO_ENVIADO_CORREO. Permite actualizar estado. No anuncia entrega o radicación por el mero guardado. | Lógica de estados probada; vista, HTTP y actualización real pendientes. |
| Responsive | Ampliación limitada a este flujo: contenedor de hasta 960 px y dos columnas de categorías desde 1024 px. Móvil conserva los componentes existentes. | Cambio CSS, sin medición visual ni certificación de contraste. |

El método heredado `publicarReporte` de `compartido/api/reportes.js` permanece sin llamadas desde los pasos activos. No se usa su antigua supresión de errores para cerrar el nuevo flujo. La página alternativa `app/paginas/CrearReporte.jsx` sigue desconectada; no se activó ni se sustituyó la implementación de Katherine.

### 13.2 Archivos intervenidos en esta continuación

| Archivo (relativo a frontend/) | Motivo |
|---|---|
| `src/app/reportar/catalogo.js` (nuevo) | Alias, ayudas y orden del catálogo de enrutamiento; detección de emergencia. |
| `src/app/reportar/catalogo.test.mjs` (nuevo) | IDs/flags conservados, orden, opciones ausentes y emergencia. |
| `src/app/reportar/ReportarLayout.jsx` | Carga de rutas, autenticación, conservación y recuperación por código. |
| `src/app/reportar/PasoCategoria.jsx` | Opciones adaptadas, validación de selección y acceso al segundo paso. |
| `src/app/reportar/PasoUbicacion.jsx` | GPS o barrio, paso no requerido, catálogo vacío y cobertura del servidor. |
| `src/app/reportar/MapaUbicacion.jsx` | Retirar marcador al quitar punto; mantiene capa solo como referencia visual. |
| `src/app/reportar/PasoDetalles.jsx` | Matrícula condicional, copy, emergencia y bloqueo al preparar fotos. |
| `src/app/reportar/PasoConfirmacion.jsx` | Canal real, consentimiento independiente, recuperación sin duplicar código. |
| `src/app/reportar/ReporteEnviado.jsx` | Estado real de correo y canal externo, sin promesa fija de envío. |
| `src/app/reportar/Reportar.css` | Ampliación responsive exclusiva del flujo. |
| `src/app/reportar/borrador.js` | Persistencia de progreso/código de recuperación. |
| `src/app/reportar/rutas.jsx` | Comentario actualizado de acceso al catálogo protegido. |
| `src/app/servicios/recuperacionReporte.js` y su `.test.mjs` | Comunicación por foto y resultado de solicitud; conciliación y casos de fallo. |
| `src/app/componentes/estadoCorreo.js` y su `.test.mjs` | Tolerar estado externo sin ruta disponible, sin afirmar envío. |
| `src/compartido/validaciones/reporte.js` y su `.test.mjs` | Barrio/GPS, autorización explícita, validación final y payload condicionado. |

Además se actualizan este informe y `docs/privacidad/PENDIENTE_INTEGRACION_FRONTEND.md`. Los seis archivos originales de consentimiento no se editaron durante esta continuación. Backend, migraciones, dependencias, catálogo general y configuración de producción permanecen sin cambios.

### 13.3 Pruebas nuevas y límites

Ejecución nueva acotada de siete archivos (`node --test`):

```text
src/app/reportar/catalogo.test.mjs
src/compartido/validaciones/reporte.test.mjs
src/app/servicios/recuperacionReporte.test.mjs
src/app/componentes/estadoCorreo.test.mjs
src/compartido/textos/avisoPrivacidad.test.mjs
src/compartido/validaciones/registro.test.mjs
src/compartido/utilidades/fotos.test.mjs
```

**Resultado: 41 aprobadas, 0 fallidas, 0 omitidas.** Distribución: 3 catálogo, 11 validación de reporte, 6 recuperación, 5 estado de correo, 4 aviso v3, 9 registro, 3 fotos. Las primeras 23 pruebas de esta continuación fueron una ejecución previa, no se suman a las 41. No se repitió la suite completa de 75.

Los casos que requieren errores parciales usan un adaptador API simulado: son pruebas de lógica real del cliente y del servicio compartido, no HTTP contra Spring Boot. No prueban consentimiento Google de extremo a extremo, persistencia de una alta nueva ni funcionamiento de Resend. La comparación v3/V12 se volvió a ejecutar; los conteos históricos de base conservados en sección 10 no se presentan como nuevas consultas.

Se ejecutaron ESLint sobre los archivos modificados y compilación de producción Vite. El lint detectó inicialmente un problema de memoización en el callback de ruta; se corrigió usando dependencias primitivas y la ejecución posterior pasó. No se desactivaron reglas de lint ni se alteraron versiones. El resultado de compilación no equivale a conformidad funcional o visual.

### 13.4 Bloqueos reintentados y evidencias de entorno

- Central sigue inaccesible desde terminal: `curl` a la URL del BOM 1.21.4 devolvió `Could not resolve host: repo.maven.apache.org`. No se reintentó toda la suite Maven ni se sustituyó la dependencia para eludir el problema. **Pruebas backend/HTTP/persistencia: NO EJECUTADAS en esta continuación.**
- `docker version` devuelve Client 29.8.2 pero falla al conectar al motor con `permission denied ... dockerDesktopLinuxEngine`. No se afirmó que Docker estuviera apagado para el usuario.
- Se inició un preview del build local en `127.0.0.1:4176`. Al abrir `/registro`, el navegador respondió `ERR_CONNECTION_TIMED_OUT`. No hay prueba visual móvil/escritorio ni navegación real al aviso. No se inventaron capturas ni se usó SSR como sustituto de revisión visual.
- Figma volvió a rechazar edición al iniciar sesión de trabajo. La pestaña existente del archivo principal sí permitió una lectura parcial del panel de capas: botón **«Solicitar editar»**, frames **«Web · Nuevo reporte»**, **«1 · ¿Qué pasó?»**, **«2 · ¿Dónde y cómo está?»**, **«3 · Revisar y enviar»**. Indica una discrepancia a revisar con el flujo de cuatro pasos, pero no se comprobó todo el lienzo ni la versión 4.1. El intento de expandir capas no se completó. **No se editó ningún nodo ni se crearon diseños sustitutos.**
- No se crearon usuarios ni se ejecutaron escrituras en producción. No hubo pruebas de alta aislada, despliegues, commits, push ni PR.

### 13.5 Decisiones y pendientes de Katherine y Andrés

1. Confirmar el acceso inicial autenticado como decisión de producto o autorizar por separado un catálogo público. No se relajó la autenticación backend. Verificar el regreso al reporte desde login Google/registro y conservación del borrador.
2. Validar HTTP real del catálogo y resolvedor, incluyendo 14 rutas, ID 15 fuera del flujo nuevo, matrícula, competencias pendientes y canales externos. Mantener inalterada la categoría 15 general.
3. Resolver falta de barrios reales seleccionables sin ofrecer el interno. Validar alta tradicional y Google en base aislada autorizada, incluyendo false/ausente/true, ausencia de alta al rechazar, timestamp y versión exacta.
4. Completar el test Google de consentimiento falso que falta en backend y ejecutar las pruebas disponibles cuando DNS y permisos lo permitan. No se editó ese código.
5. Dar acceso de edición al Figma principal y conciliar las capas de tres pasos observadas con los cuatro pasos pedidos. Verificar vistas y accesibilidad en tamaños móviles y escritorio antes de declarar conformidad.
6. Acordar copy/alcance de autorización para canales sin correo: el texto contractual se conserva y se explica que se guarda sin envío automático; no se inventó una nueva autorización legal.
7. Verificar la recuperación en HTTP real, incluida recarga con fotos pendientes. Si la respuesta de **creación** se pierde antes de recibir el código, el cliente no puede por sí solo saber si el servidor creó el reporte: requiere acordar idempotencia o reconciliación backend; no se promete resolver ese caso con almacenamiento local. Si el navegador bloquea sessionStorage y se recarga, el avance en memoria se pierde; consultar Mis reportes antes de registrar de nuevo.
8. Verificar el despliegue oficial únicamente cuando se autorice publicar estos cambios; todavía están locales. La página alternativa no activa y los comentarios/método heredados fuera de la ruta quedan como deuda de mantenimiento, sin rehacerlos en esta tarea.

**Estado actual Crear reporte: PENDIENTE DE VERIFICACIÓN de integración/visual; hay correcciones locales implementadas y probadas en su lógica, y diferencias de Figma/decisiones de equipo aún REQUIEREN AJUSTES. RF-03: PENDIENTE DE INTEGRACIÓN / PENDIENTE DE VERIFICACIÓN independiente.** El documento temporal se conserva. La rama sigue sin commit, push, PR o despliegue.

## 14. Validación final posterior — 7 de noviembre de 2026

### Resultado breve

**Crear reporte: PENDIENTE DE VERIFICACIÓN** visual y HTTP, con ajustes de navegación/autenticación por confirmar. **RF-03: PENDIENTE DE INTEGRACIÓN / PENDIENTE DE VERIFICACIÓN**, de forma independiente. Esta revisión no modifica código ni backend: únicamente añade evidencias a los dos documentos de seguimiento. Las 75, 33 y 41 pruebas anteriores son antecedentes, no ejecuciones nuevas.

### Pruebas y diagnósticos nuevos

| Comprobación ejecutada | Resultado real | Qué permite concluir |
|---|---|---|
| Rama, HEAD y estado Git | `fix/registro-pr20`, `e5d73c43da8331f3e6f524eda0724652471c4f83`, cambios locales existentes | No hubo cambio de rama, commit, push, PR o despliegue. |
| Listener HTTP mínimo en Node | Abrió `127.0.0.1` en un puerto efímero; fetch al mismo proceso falló con `connect EACCES` | Se puede abrir el listener dentro del proceso, pero la conexión de retorno está denegada en este entorno. El fallo no depende de React ni del router de Vite. No se determinó qué regla concreta de aislamiento/red causa la denegación. |
| Preview del build existente en 4180 | Vite reportó `127.0.0.1:4180`, PID 16764; abrir `/registro` en navegador devolvió `ERR_CONNECTION_TIMED_OUT` | No existe evidencia de renderizado del registro. El reporte interno de listener no demuestra que el navegador pueda alcanzarlo. |
| Preview autocontenido en 4181, con contador de peticiones | `PREVIEW_LISTEN ... 4181`, `CONNECT_ERROR EACCES`, `REQUESTS_RECEIVED 0`; servidor cerrado al terminar | La petición de prueba no llegó al manejador HTTP: no es un 404/500 de la aplicación. No se cambió host a público, firewall, proxy ni configuración de Vite para ocultar el bloqueo. |
| Tabla TCP desde el ejecutor | No mostró listeners 4176/4180/8080 en esa consulta | No hay prueba de un backend local de integración accesible. La visibilidad desde el ejecutor y el listener reportado por Vite no bastan para acreditar acceso desde el navegador. |
| DNS Maven Central | `ENOTFOUND repo.maven.apache.org`; `Resolve-DnsName` devolvió acceso denegado | Bloqueo de resolución en la sesión. No se ejecutó otra suite Maven ni se cambió Testcontainers 1.21.4. |
| Docker | Client 29.8.2; motor Linux rechazó acceso a `dockerDesktopLinuxEngine` con `permission denied` | No se pudo usar una base desechable local. No implica que Docker esté apagado en Windows del usuario. |
| Recuperación: `node --test src/app/servicios/recuperacionReporte.test.mjs` | **6 aprobadas, 0 fallidas, 0 omitidas** | Valida funciones de recuperación, respuesta tardía, fallo de foto/encolado y conciliación con API simulada; no ejecuta HTTP real. |
| Borrador y avance: comprobación adicional con Map como sessionStorage | Conservó descripción, categoría, código sintético y número de fotos subidas; borrar avance no borró borrador | Round-trip de almacenamiento simulado aprobado. No se escribió en almacenamiento de usuario, navegador ni base. No certifica navegación después del login. |
| `git diff --check` | Sin errores | Comprobación de integridad del diff, no prueba funcional. |

No se repitió build/lint ni la suite completa: el código no cambió durante esta validación. Los resultados de compilación/lint de la sección 13 siguen siendo antecedentes válidos para esos mismos bytes.

### API real: respuestas obtenidas y comprobaciones no ejecutadas

| Endpoint / función | Intento y resultado | Estado |
|---|---|---|
| `GET https://notiurba-api.onrender.com/api/health` | Fetch directo falló con `ENOTFOUND`; sin código HTTP | BLOQUEADO |
| `GET /api/categorias` | Fetch directo falló con `ENOTFOUND`. El intento de lectura desde navegador se interrumpió sin respuesta verificable | BLOQUEADO; no se registra un 200/401 inventado |
| `GET /api/enrutamiento/categorias` | Fetch directo falló con `ENOTFOUND`; sin código HTTP | BLOQUEADO |
| `GET /api/catalogos/barrios` | Fetch directo falló con `ENOTFOUND`; sin código HTTP | BLOQUEADO |
| `GET /api/enrutamiento/categorias/{id}?emergencia=...` | No ejecutado con autenticación en un backend de pruebas accesible | PENDIENTE DE VERIFICACIÓN |
| Crear reporte / ubicación / cobertura: `POST /api/reportes` | No ejecutado; no se crearon reportes ficticios en producción | PENDIENTE DE VERIFICACIÓN |
| Fotografías: `POST /api/reportes/{codigo}/fotos` | No ejecutado contra servidor real ni Cloudinary | PENDIENTE DE VERIFICACIÓN |
| Envío y estado: `POST .../enviar`, `GET .../envio` | No ejecutados contra un reporte de prueba autorizado | PENDIENTE DE VERIFICACIÓN |
| Registro tradicional y completar Google, con false/ausente/true | No ejecutados por HTTP contra base aislada | PENDIENTE DE VERIFICACIÓN; RF-03 no se certifica |

Un error de DNS o conexión **no es una respuesta HTTP del backend**. Las consultas SQL siguientes no sustituyen las pruebas anteriores ni acreditan compatibilidad completa del frontend con el servidor desplegado.

### Barrios y entorno aislado: consultas SELECT nuevas

- Producción, proyecto NotiUrba, rama `production`: consulta agregada a `barrio` devolvió **1 total, 0 seleccionables (`NOT interno`)**. Confirma que el registro sigue bloqueado por el catálogo actual. No se obtuvieron datos personales ni se modificó la base.
- Entorno aislado existente `NotiUrba-EDT51-isolated`, rama `edt51-isolated`, base `notiurba_edt51`: se confirmó disponibilidad mediante metadatos y SELECT. Entre las cinco tablas comprobadas existen `usuario`, `categoria` y `aviso_privacidad`; **no existen `configuracion_enrutamiento` ni `flyway_schema_history`** en `public`. El catálogo de avisos contiene únicamente **versión 1.0**.
- Ese entorno no tiene el esquema/aviso actual requerido para esta validación. No se aplicaron migraciones, no se crearon bases/usuarios y no se aprovecharon permisos de propietario para cambiarlo sin coordinación. Hace falta un backend de pruebas conectado a un esquema actual autorizado, con datos sintéticos y envío externo controlado. La disponibilidad de una base aislada antigua no convierte las pruebas de la versión actual en ejecutables.

### Pantallas y recuperación tras login

**Pantallas verificadas visualmente en esta revisión: ninguna.** El acceso al preview falló antes de renderizar. Registro tradicional, Google, casillas, enlace v3, los cuatro pasos, las 14 tarjetas, GPS, barrio, fotos, confirmación y recuperación permanecen sin comprobación visual en escritorio/móvil. No se sustituyó esa revisión por lectura de código, screenshots inventados o pruebas de almacenamiento.

La inspección del retorno al reporte confirma:

| Flujo | Evidencia de código | Estado de validación |
|---|---|---|
| Login tradicional | `ReportarLayout` pasa `state.siguiente`; `Login.jsx:67` lo usa para ciudadanos | Cableado existente; funcionamiento de extremo a extremo PENDIENTE DE VERIFICACIÓN |
| Login Google | `BotonGoogle.jsx:52` navega por rol; al perfil incompleto envía la respuesta de Google sin ruta siguiente | **REQUIERE AJUSTES** para retorno automático al reporte |
| Completar perfil Google | `CompletarPerfil.jsx:54` navega por rol, no por la ruta del borrador | **REQUIERE AJUSTES**, preservando el componente y consentimiento existentes |
| Registro y verificación de correo | `Registro.jsx:67` lleva correo a verificación, sin propagar ruta siguiente | Retorno automático al reporte PENDIENTE; acordar con Katherine |
| Borrador/código guardado | sessionStorage conserva datos de texto y progreso según comprobación simulada; fotos pendientes viven en memoria | No demuestra que archivos sobrevivan a desmontaje, expiración o login. En recuperación tras recarga se deben volver a adjuntar pendientes; comprobar en navegador |

No se modificaron esas rutas durante una tarea de validación ni se declaró resuelta la diferencia con Google.

### Figma y decisiones del equipo

El nuevo intento de acceder al archivo principal mediante la sesión de diseño volvió a devolver falta de permiso de edición. No se editaron nodos, no se cambió de archivo y no se creó un sustituto. Las capas de tres pasos observadas en la revisión anterior siguen siendo **evidencia previa parcial**, no una inspección nueva completa ni una comprobación de versión 4.1.

Katherine debe confirmar navegación de retorno y correspondencia de cuatro pasos con el diseño; el propietario del Figma principal debe habilitar edición a la cuenta conectada si se desea aplicar allí la especificación. Andrés debe coordinar catálogo público frente a sesión inicial, barrios válidos, esquema/backend aislado vigente y acceso de pruebas. La política de red/permisos de esta sesión debe permitir HTTP local, DNS y motor Docker para ejecutar las verificaciones restantes. No se atribuye el problema específicamente al firewall de Windows sin evidencia.

### Cierre de esta revisión

Se precisó el diagnóstico del timeout: hay denegación de conexión local antes de que Vite reciba peticiones. **No se corrigió código funcional** ni se consideran resueltos los bloqueos de infraestructura. Se actualizan únicamente este informe y `PENDIENTE_INTEGRACION_FRONTEND.md`, que permanece físicamente presente.

Los hashes del código frontend se comparan con la captura inicial de esta validación para asegurar que se preservaron todos los cambios, incluidos los seis archivos de consentimiento. Backend y migraciones permanecen sin modificaciones. Git continúa en `fix/registro-pr20`, HEAD `e5d73c43da8331f3e6f524eda0724652471c4f83`, sin commit, push, PR o despliegue.


## 15. Corrección local del retorno tras autenticación - 7 de noviembre de 2026

Se leyó la sección 14 antes de editar. Se confirmó la rama `fix/registro-pr20`, HEAD `e5d73c43da8331f3e6f524eda0724652471c4f83` y el árbol con cambios locales existentes. No se cambió de rama, no se revirtieron avances y no se modificaron backend, migraciones ni producción. No hay commit, push, PR o despliegue.

### Implementación y alcance comprobable

| Flujo | Cambio local |
|---|---|
| Entrada desde reporte | Tanto «Iniciar sesión» como «Crear cuenta» transmiten `state.siguiente` con el paso actual. |
| Login tradicional | Valida el destino antes de navegar. Sin retorno válido usa la navegación habitual por rol. ADMIN conserva `/admin`. |
| Google | Propaga el destino al perfil incompleto; con perfil completo resuelve y consume el retorno después de guardar la sesión. Se adaptó también el enlace del modo simulado, sin confundirlo con una autenticación real. |
| Completar perfil | Una envoltura de ruta conserva la intención. Tras la autenticación, la llegada del ciudadano a `/app` se redirige al paso pendiente. `CompletarPerfil.jsx` permanece byte por byte intacto. |
| Registro y correo | Registro transmite el retorno a la espera del correo. Sus enlaces de regreso y los de error de confirmación lo conservan. La confirmación satisfactoria consulta la intención temporal de esa pestaña y la consume. |
| Login directo | Al entrar sin `state.siguiente` se descarta una intención anterior; no se deduce el destino por la mera presencia de un borrador. |

Los nuevos módulos `frontend/src/compartido/auth/retornoReporte.js` y `NavegacionAcceso.jsx` reutilizan React Router, `state.siguiente`, `RUTA_POR_ROL` y sessionStorage. La lista permitida contiene exactamente `/reportar`, `/reportar/ubicacion`, `/reportar/detalles` y `/reportar/confirmacion`. Se rechazan URLs externas, rutas administrativas, parámetros, fragmentos y rutas ambiguas.

La clave nueva `notiurba.retornoReporte` guarda únicamente ruta, marca temporal y etapa (acceso/perfil), con vigencia de 30 minutos para la recuperación desde almacenamiento. No copia credenciales, tokens, correo ni contenido del reporte. Al resolver la autenticación se descarta la intención; el enlace explícito por estado sigue siendo validado. Datos corruptos, caducados o almacenamiento bloqueado no impiden la navegación normal.

El borrador y el código del reporte ya guardado mantienen sus claves y contenido: no se limpian al preparar, descartar o consumir el retorno. **Las fotos pendientes continúan en memoria y deben volver a adjuntarse al desmontar/recargar la pantalla**; este cambio no introduce persistencia de archivos. No se emite un nuevo POST de reporte durante el retorno.

### Pruebas ejecutadas en esta corrección

Desde `frontend`:

```text
node --test src/compartido/auth/retornoReporte.test.mjs src/compartido/auth/sesion.test.mjs src/app/servicios/recuperacionReporte.test.mjs src/compartido/validaciones/registro.test.mjs src/compartido/textos/avisoPrivacidad.test.mjs
```

**Resultado nuevo: 37 aprobadas, 0 fallidas, 0 omitidas.** Incluye 12 pruebas nuevas de lógica de retorno, 6 de sesión, 6 de recuperación del reporte, 9 de registro y 4 del aviso. Las nuevas cubren rutas válidas/ausentes/inválidas, rol ADMIN, secuencias de Google/perfil y registro/correo, caducidad, datos corruptos, almacenamiento bloqueado y conservación exacta de borrador/código. Usan almacenamiento simulado; no ejecutan Google, correo ni servidor reales ni efectos de navegación en un navegador.

ESLint mediante su API pasó en los **11 archivos de código/pruebas modificados por esta tarea**, sin errores ni advertencias. Compilación mediante `import { build } from 'vite'; await build()`: **correcta, 230 módulos**. El primer intento detectó colisión de resolución en Windows entre nombres JS/JSX similares; se corrigió usando el nombre inequívoco `NavegacionAcceso.jsx` y la compilación final pasó. `git diff --check` pasó.

### Preservación de los seis archivos de consentimiento

SHA256 capturados antes de editar y comparados al finalizar: **6/6 idénticos**.

| Archivo bajo frontend/src/compartido | SHA256 |
|---|---|
| componentes/avisos/NotaPrivacidad.jsx | `740f98915dc1a06817dd28aa1e2a58d31c3e3539e3f646501924852b6e086557` |
| componentes/formulario/ConsentimientoRegistro.jsx | `92c612752b2460d1beb505327639f40415212e1a9a3eda7f45467563a5eae8bc` |
| paginas/CompletarPerfil.jsx | `45e9b673bf9049f083c22e210601e7bbe77ab2a20b5f4c514471710bff764982` |
| paginas/registro/PasoAcceso.jsx | `d8ce3c4143185ba893b994c226124133020eaa755e9561ed4d1ad1234d0183a2` |
| textos/avisoPrivacidad.js | `8c1005f771b501b3c65a551357c5de59c810a4bd47d9b3a8a4a953c093dcb59f` |
| textos/avisoPrivacidad.test.mjs | `9920c2c42222d842252309c2abc4874edf2fe6d10beb22b0d186996a1d689e43` |

### Pendientes y veredicto independiente

- **Retorno: implementación local corregida; PENDIENTE DE VERIFICACIÓN de extremo a extremo.** Falta recorrer login tradicional, Google completo/incompleto y registro/correo real en un entorno autorizado, en escritorio y móvil. No se ejecutaron pruebas visuales en esta corrección.
- Confirmar el correo en otra pestaña/dispositivo, tras caducar la intención o sin sessionStorage disponible puede llevar a la zona habitual por rol. No se comparte el borrador entre dispositivos; la limitación es deliberada y no se añadió localStorage para ello. La pestaña original conserva el borrador disponible.
- Katherine debe revisar la navegación en navegador y el comportamiento al volver a adjuntar fotografías; Andrés debe habilitar el entorno vigente de integración y barrios seleccionables. Los bloqueos de infraestructura y persistencia de la sección 14 no se reintentaron ni se consideran resueltos.
- **Crear reporte: PENDIENTE DE VERIFICACIÓN** funcional/visual integral y conciliación con Figma. Esta corrección no acredita el flujo completo.
- **RF-03: PENDIENTE DE INTEGRACIÓN / PENDIENTE DE VERIFICACIÓN.** No se verificó persistencia real del consentimiento ni se certificó el requisito. Se conserva físicamente `docs/privacidad/PENDIENTE_INTEGRACION_FRONTEND.md`.

Archivos editados por esta corrección: `frontend/src/App.jsx`, `app/reportar/ReportarLayout.jsx`, `compartido/componentes/acceso/BotonGoogle.jsx`, `PieAuth.jsx`, `compartido/paginas/Login.jsx`, `Registro.jsx`, `VerificarCorreo.jsx`, `ConfirmarCorreo.jsx` (rutas relativas a `frontend/src` salvo el primero), los tres módulos nuevos de autenticación indicados (incluida su prueba), este informe y el documento de pendientes. Los restantes cambios locales existentes se conservan.

## 16. Registro e ingreso: corrección local y límites de QA (7 de noviembre de 2026)

Rama confirmada antes y después: `fix/registro-pr20`, HEAD `e5d73c43da8331f3e6f524eda0724652471c4f83`. Se preservaron los cambios locales recibidos. Sin modificaciones de backend, migraciones, Render, Neon o producción; sin commit, push, PR o despliegue. El proxy Vite existente permanece intacto. `.env.local` está ignorado por Git y no figura en archivos versionados; no se imprimió su contenido.

### Resultado por problema

| Problema | Causa comprobada / evidencia | Corrección de esta tarea | Estado |
|---|---|---|---|
| Barrios vacíos | El usuario observó HTTP 200 con `[]`; antecedente agregado: cero barrios seleccionables. `obtenerBarrios` además tenía respaldo al barrio interno en 401/403/404. Ambos registros exigen `idBarrio`. | Eliminado exclusivamente el respaldo de barrios. Lista vacía conserva su significado; aviso específico en registro y ayuda en el selector compartido de residencia, también usado por completar perfil. No se inventan opciones. Registro impide enviar sin catálogo y valida pertenencia del barrio. | React corregido; alta nueva BLOQUEADA por datos. |
| Nombres y apellidos | `LIMITES.nombre=60`, `maxLength` en ambos inputs y validación compartida de más de 60. No se comprobó visualmente un desbordamiento. | Ayuda visible con límite y contador en ambos campos. Se conserva el límite 60 y las restricciones de API; pruebas de 60/61 caracteres en nombres y apellidos. | Validación aprobada; escritura/pegado reales y geometría PENDIENTES. |
| Contraseñas | El componente compartido ya cambia `type`, conserva `value`, tiene estado independiente y botón `type=button`. El clic del ratón podía desplazar el foco al ojo. No hay evidencia nueva de bordes cortados o iconos superpuestos. | El ojo evita el cambio de foco por `mousedown`, usa actualización funcional y `aria-controls`. Aplica a las dos contraseñas del registro y al login. | Implementado; interacción real de los tres campos PENDIENTE. |
| Paso 2 | El formulario esperaba tipos Y barrios para mostrar el paso 1. La validación del paso 1 exige nombres, apellidos, tipo, documento válido y teléfono válido solo si se escribe; no exige barrio. | Paso 1 depende únicamente de tipos disponibles; resumen de validación y errores específicos existentes. Se permite avanzar con datos personales válidos aun sin barrios; paso 2 distingue carga/fallo/vacío y ofrece reintento ante fallo. Se revalidan ambos pasos al enviar. Estado de valores sigue en el padre al retroceder. | Lógica y pruebas aprobadas; recorrido real PENDIENTE. |
| Aviso de privacidad | Ruta pública `/aviso-privacidad` registrada; página importa `AVISO_PRIVACIDAD` v3. Enlaces de nota y consentimiento usan `_blank` y `noopener noreferrer`, tanto en registro como completar perfil. | Sin cambio: no se reprodujo un fallo de ruta ni de enlace. La propuesta documental y el HTML alternativo no sustituyen v3. | Revisión de código aprobada; apertura real y conservación de campos PENDIENTES. |
| Login tradicional | `leerError` produce el mensaje inesperado cuando no recibe el cuerpo `{codigo,mensaje}` o hay 5xx. El login asumía credenciales inválidas para cualquier 401 y podía mostrar mensajes arbitrarios del cuerpo en otros errores. Un éxito JSON mal formado tampoco se validaba. | Mensajes ciudadanos controlados y distintos para 400/401/403/429/5xx/red; solo `401 CREDENCIALES_INVALIDAS` marca revisión de ambos campos. No afirma qué credencial falló. JSON ilegible se convierte en `ErrorApi`; se exige token no vacío y rol antes de guardar sesión. El cuerpo no puede sobreescribir el estado HTTP. | Manejo React aprobado con respuestas simuladas; causa HTTP desplegada PENDIENTE. |
| Google | No se encontró un valor de `VITE_GOOGLE_CLIENT_ID` en `.env.local` ni `.env.example`; mocks locales no están activos. Sin ID no se carga el botón real. | Mensaje visible de indisponibilidad en lugar de depender solo de «Próximamente». Se conserva el retorno seguro existente, sin IDs inventados ni activar mocks. | PENDIENTE configuración OAuth y validación real. |
| Responsive | La única captura obtenida mostró registro en «Cargando…» en Chrome, sin campos. No se reprodujeron bordes cortados, superposición ni desbordamientos. | No se cambió CSS sin evidencia. | PENDIENTE móvil/escritorio y mediciones. |

### Pruebas y HTTP

Selección ejecutada con `node --test`: `api/cliente.test.mjs`, `auth/errorLogin.test.mjs`, `auth/retornoReporte.test.mjs`, `validaciones/campos.test.mjs`, `validaciones/registro.test.mjs` y `textos/avisoPrivacidad.test.mjs`: **59 aprobadas, 0 fallidas, 0 omitidas**. Incluye 400/401/403/429/500 sin cuerpo del contrato, 500 con texto técnico, estado no sobreescribible, éxito sin token/JSON ilegible, catálogo vacío/errores sin respaldo, solicitud pública de login, límite 60/61 y avance sin datos del paso 2. Fetch y almacenamiento están simulados en estas pruebas; no son integración HTTP, pruebas DOM o de persistencia.

ESLint pasó en los **12 archivos de código/pruebas de esta corrección**, con 0 errores y 0 advertencias mediante su API. `npm run lint` tampoco imprimió incidencias. Compilación final `npm run build`: **correcta, 231 módulos, 2,17 s**. Un intento anterior de compilación coincidió con otro build y falló por `EPERM` al limpiar `dist/assets`; la repetición posterior sin concurrencia compiló correctamente. No se cambió configuración para sortear ese fallo.

Se intentaron de forma segura GET de barrios y **un único POST de login con cuerpo vacío `{}`**, sin credenciales reales ni cabecera de sesión, desde la sesión de ejecución: ambos fallaron por DNS (`Could not resolve host: notiurba-api.onrender.com`), HTTP 000. No se recibió un estado HTTP real; **no se atribuye el mensaje observado a credenciales, CORS ni a un estado específico**. No se repitió el diagnóstico CORS; se conserva la evidencia del usuario de tipos de documento HTTP 200 mediante el proxy local.

Chrome permitió abrir una pestaña nueva de `/registro` y tomar una captura de escritorio (1366 × 599) en estado «Cargando…». La captura no contenía inputs. La observación de red no produjo respuestas verificables, después hubo timeout/desconexión. La recuperación nativa se detuvo porque no se pudo verificar con suficiente confianza la URL activa de Chrome. **No hay pruebas visuales aprobadas** de campos, móvil, contraseñas, aviso o conservación de valores. No se enviaron formularios del navegador ni se crearon usuarios.

### Protección y coordinación

Los seis SHA256 de la sección 15 se capturaron antes de editar y se compararon al terminar: **6/6 idénticos**, incluidos `CompletarPerfil.jsx` y `PasoAcceso.jsx`. Los cambios de residencia y contraseña se hacen en componentes compartidos, sin tocar el consentimiento protegido. `git diff --check` pasó.

- **Andrés, datos/backend:** habilitar barrios ciudadanos seleccionables en el catálogo vigente sin ofrecer el barrio interno; coordinar un backend aislado actual para altas tradicional/Google, rechazos y persistencia de fecha/hora/versión del consentimiento. Este bloqueo queda registrado aquí; no se envió ningún mensaje externo.
- **Configuración OAuth:** proporcionar el ID web real acordado con backend y verificar en la consola del proyecto los orígenes exactos de desarrollo usados (`http://localhost:5173` y otros únicamente si se usan). No se tuvo acceso a la consola OAuth ni se verificaron orígenes autorizados. Después probar carga GSI y los retornos de perfil completo/incompleto.
- **QA pendiente:** ejecutar escritura y pegado de más de 60 caracteres, avance/retroceso sin pérdida, tres ojos sin submit y con foco/contenido conservados, aviso v3 en pestaña nueva; medir 320/360/390 px y escritorio. Capturar la respuesta real del login sin exponer cuerpos sensibles y sin intentos repetidos con cuentas reales.

**RF-03 permanece PENDIENTE DE INTEGRACIÓN / PENDIENTE DE VERIFICACIÓN.** Las pruebas unitarias y la lectura de rutas no certifican el registro real ni la persistencia del consentimiento. Se conserva `PENDIENTE_INTEGRACION_FRONTEND.md`.

## 17. Dirección, geometría de campos y consulta de privacidad dentro del registro

Fecha: 7 de noviembre de 2026. Rama `fix/registro-pr20`, HEAD sin cambios `e5d73c43da8331f3e6f524eda0724652471c4f83`. El usuario confirmó que podía avanzar al paso 2 y consultar v3. Se conservaron todos los cambios locales anteriores. Esta sección actualiza la evidencia de la sección 16 para el registro; no certifica integración ni producción.

### Protección previa y solución elegida

Antes de editar se comprobaron los seis SHA256 de la sección 15: coincidencia 6/6. No fue necesario modificarlos: el límite exclusivo del registro se proporciona mediante un contexto React consumido por `CamposResidencia`, y el clic normal/Enter de los enlaces protegidos se atiende en el contenedor del registro. Se mantiene el href público y el comportamiento de apertura independiente con clic modificado. No se cambió texto ni versión de v3. Comparación final: 6/6 idénticos.

| Problema | Causa y corrección | Resultado / límite |
|---|---|---|
| Dirección | Antes usaba el límite compartido 150. Registro proporciona 100 mediante `LimiteDireccionContexto`; añade contador y validación exclusiva antes del POST. Fuera de ese proveedor se conservan 150 y la ayuda anterior. No hay `slice`, recorte de valores ni cambio de API. | Escritura del carácter 101 dejó longitud 100 en Chrome. Un pegado de 130 mediante la herramienta entró completo y mostró error explícito, sin ensanchar la caja ni recortarlo. Pruebas 100/101 y valor anterior largo aprobadas. |
| Bordes/recuadros | En Chrome a 320 px se midieron cajas de 44 px e inputs de 44 px, sin descontar el borde exterior. El input sobresalía verticalmente del espacio interior; `content-box` y alturas móviles coincidían con la altura externa. | Inputs/selects ahora usan `border-box`, altura del 100 % interior, ancho flexible con base 0, mínimo 0 y máximo 100 %. Caja mínima 48 px para alojar el ojo de 44 px y bordes; se retiró el margen negativo del ojo. No se añadió `overflow-x:hidden` global. |
| Reflujo | Etiquetas de identidad usaban elipsis y dos columnas aun en anchos estrechos; cabeceras de campo no envolvían enlaces. | Identidad pasa a una columna bajo 480 px, etiquetas completas, cabeceras con wrap, mínimos/ancho controlados en formulario y panel; padding lateral 12 px bajo 360. Ayuda/texto largo permiten partir palabras. |
| Regreso del aviso | Una pestaña nueva no tiene el historial ni el estado de la pestaña del registro. | Clic normal/Enter abre un `<dialog>` modal nativo, reutilizando el contenido v3 de la ruta pública y manteniendo montado el formulario. Botones «Volver al formulario» y Escape cierran sin cambiar paso ni valores; el foco retorna al enlace. La ruta pública ahora vuelve a `/`, sin depender del historial de otra pestaña. |
| Catálogo vacío | Continúa sin barrios seleccionables; no es un fallo de conexión. | Se mantiene «No hay barrios disponibles», selector y «Crear cuenta» deshabilitados. Acciones visibles «Volver a datos personales» y «Volver al inicio de NotiUrba». La salida al inicio público fue ejecutada sin autenticarse. No se crearon usuarios ni se alteraron autorizaciones. |

### Pruebas automatizadas afectadas

```text
node --test src/compartido/validaciones/direccionRegistro.test.mjs src/compartido/componentes/formulario/residenciaRegistro.test.mjs
```

**8 aprobadas, 0 fallidas, 0 omitidas.** Cubren 100/101, conservación de dirección larga, conteo de espacios, requisitos existentes de dirección, aislamiento respecto a 150 global, renderizado de ayuda/contador, selector vacío sin barrio ficticio y reutilización exacta del contenido entre ruta pública y consulta del aviso. Las pruebas SSR son de renderizado; no simulan interacción DOM. Un primer intento del arnés SSR falló al cargar directamente el paquete CommonJS del router a través de Vite; se corrigió importando `MemoryRouter` normalmente y la ejecución final pasó.

No se repitieron las 59 pruebas anteriores: sus módulos no cambiaron. `npm run lint`: **correcto, código 0**. `npm run build`: **correcto, 236 módulos, 712 ms, código 0**. `git diff --check` sin errores.

### Interacciones ejecutadas en Chrome

Pestaña de prueba nueva en `http://localhost:5173/registro`, con datos sintéticos, sin enviar formularios de alta ni autenticación:

1. Llenar identidad y teléfono; avanzar al paso 2 con catálogo vacío.
2. Pegar una cadena continua larga en dirección: valor 130 conservado, mensaje de exceso visible y geometría sin desbordamiento. Reemplazar por 100 e intentar escribir un carácter adicional: quedó en 100, contador 100/100.
3. Llenar correo y ambas contraseñas sintéticas. Cambiar mostrar/ocultar independientemente: `type` cambió, no hubo submit y el foco de ratón permaneció en el input. Se comprobó visualmente el mismo contenido sintético revelado en ambos campos después de regresar del aviso y de retroceder/avanzar. Las lecturas DOM de correo/contraseñas están enmascaradas por la herramienta y no se usaron sus comparaciones literales para inferir pérdida de datos.
4. Abrir v3 desde el paso 2: modal presente, formulario aún montado, Tab permaneció dentro. Cerrar por botón y por Escape; se recuperó el paso 2 y el foco volvió al enlace. Casilla de consentimiento permaneció desmarcada. Se conservó la dirección y se observaron correo/contraseñas sintéticos sin pérdida.
5. Pulsar «Volver a datos personales»: nombres, apellidos y documento sintéticos permanecieron; el teléfono siguió informado. Avanzar otra vez: dirección 100, correo y contraseñas permanecieron; «Crear cuenta» siguió deshabilitado. No se pudo probar conservación de un barrio seleccionado porque no existe ninguna opción real; el estado vacío se conservó.
6. Pulsar «Volver al inicio de NotiUrba»: se observó `/` con bienvenida pública, sin iniciar sesión.

### Matriz de anchura y zoom: evidencia y pendientes separados

Se midió `documentElement.scrollWidth === documentElement.clientWidth`, caja dentro del ancho del documento e input/select dentro de su caja. En el registro no se encontraron campos fuera de sus cajas en estas mediciones. Se inspeccionaron capturas en cada anchura con dirección continua larga y ambos campos de contraseña presentes.

| Ancho solicitado | Registro al 100 %: client/scroll (px) | Reflujo equivalente 200 %: viewport CSS | client/scroll (px) | Campos fuera de caja |
|---|---|---|---|---|
| 320 | 305 / 305 | 160 | 145 / 145 | 0 |
| 360 | 345 / 345 | 180 | 165 / 165 | 0 |
| 390 | 375 / 375 | 195 | 180 / 180 | 0 |
| 428 | 413 / 413 | 214 | 199 / 199 | 0 |
| 1440 | 1425 / 1425 | 720 | 705 / 705 | 0 |

Los 15 px de diferencia corresponden a la barra vertical. **La columna de 200 % es una prueba de reflujo por reducción a la mitad del viewport, no una certificación de zoom nativo.** Los atajos de zoom enviados por la automatización no alteraron `devicePixelRatio=1` ni el viewport; por eso no se declararon como zoom 200 real. Pendiente comprobar zoom nativo de Chrome 200 %, autofill real y la pantalla de login con el CSS compartido: la conexión se interrumpió al intentar abrir esa última prueba. No se cambiaron preferencias permanentes de zoom. Las comprobaciones de la ruta pública en esta tarea son de renderizado; el usuario ya había confirmado su visualización, sin nueva prueba independiente de navegación en esta sesión.

### Memoria y seguridad del formulario

Los valores siguen exclusivamente en el `useState` del formulario padre; al consultar privacidad se monta únicamente el diálogo, sin copiar los datos a otro almacenamiento. Se revisaron `Registro`, `useFormulario`, `CamposPerfil` y los componentes nuevos: no escriben contraseñas ni tokens en localStorage/sessionStorage ni logs. La utilidad anterior de retorno solo maneja ruta/etapa/fecha; no se modificó. No se promete recuperación después de recargar, salir del registro o abrir otra pestaña.

La instrumentación dinámica `DOMStorage.enable` no está disponible mediante el acceso de diagnóstico proporcionado; **no se afirma una auditoría dinámica completa de escrituras de almacenamiento**. La ausencia de nuevas escrituras se comprobó por revisión de código; las interacciones conservaron valores en memoria.

### Archivos de esta corrección y pendientes

Rutas relativas a `frontend/src/compartido`:

- Modificados: `paginas/Registro.jsx`, `paginas/AvisoPrivacidad.jsx`, `paginas/AvisoPrivacidad.css`, `componentes/formulario/CamposPerfil.jsx`, `Campo.css`, `CamposPerfil.css`, `FormularioPantalla.css` (estos tres últimos bajo `componentes/formulario`), `componentes/acceso/LayoutAuth.css`.
- Nuevos: `validaciones/direccionRegistro.js`, `validaciones/direccionRegistro.test.mjs`, `componentes/formulario/limiteDireccionContexto.js`, `componentes/formulario/residenciaRegistro.test.mjs`, `componentes/avisos/ContenidoAvisoPrivacidad.jsx`, `componentes/avisos/DialogoAvisoPrivacidad.jsx`, `componentes/avisos/DialogoAvisoPrivacidad.css`.
- Seguimiento: este informe y `docs/privacidad/PENDIENTE_INTEGRACION_FRONTEND.md`, que se conserva.

**Andrés:** sigue pendiente cargar/habilitar barrios reales seleccionables y disponer del entorno de integración para validar altas y persistencia del consentimiento. No se tocaron backend, migraciones, Render, Neon ni Cloudinary. No hubo commit, push, PR o despliegue. **RF-03 permanece PENDIENTE DE INTEGRACIÓN / PENDIENTE DE VERIFICACIÓN.**

## NOT-59 — cierre de pruebas documentadas, 2026-11-08

**NOT-59 LISTA PARA DONE como tarea de pruebas y documentación.** Esto no aprueba el alta integral en producción ni cierra el bloqueo del catálogo. La rama sigue `fix/registro-pr20` y todos los cambios locales previos se conservan. Se añade esta sección al informe existente, sin crear otro informe duplicado.

Se reutilizaron los JSON/capturas de registro de `evidencias-nueve-tareas-20261107-163018/` y las verificaciones de este mismo informe. `2026-11-07-consolidado-siete-tareas.md` no estaba presente al auditar; no se recreó ni se usó como evidencia no disponible. No se repitieron campos vacíos, consentimiento, teclado, conservación, responsive ni pruebas Android aprobadas. Solo se ejecutó el caso pendiente de duplicado contra un backend real aislado.

| Caso | Resultado | Evidencia |
|---|---|---|
| Correo duplicado | Aprobado: POST real 409 `CORREO_YA_REGISTRADO`, campo `correo`; mensaje español visible; una cuenta antes y después, mismo ID, cero usuarios con el documento nuevo y cero tokens de verificación | [Resultado real](evidencias-NOT-59-20261108/correo-duplicado-real.json), [captura 1366](evidencias-NOT-59-20261108/correo-duplicado-real-1366.png), [SQL antes](evidencias-NOT-59-20261108/sql-antes.json), [SQL después](evidencias-NOT-59-20261108/sql-despues.json) |
| Campos vacíos | Aprobado previamente: cuatro errores de obligatorios en español y conservación del paso inicial | [Xiaomi físico](evidencias-nueve-tareas-20261107-163018/mobile-registro-vacios.json), [escritorio](evidencias-nueve-tareas-20261107-163018/registro-desktop-resultados.json) |
| Aviso sin marcar | Aprobado previamente: validación de privacidad/mayoría de edad; no permite continuar el envío sin aceptación | [Móvil real, validación frontend](evidencias-nueve-tareas-20261107-163018/mobile-registro-validacion.json), [escritorio](evidencias-nueve-tareas-20261107-163018/registro-desktop-resultados.json) |
| Teclado | Aprobado previamente: Tab desde correo alcanza contraseña; comprobación CDP en el teléfono. No se afirma conexión de un teclado físico externo | [Móvil](evidencias-nueve-tareas-20261107-163018/mobile-registro-teclado.json), [escritorio](evidencias-nueve-tareas-20261107-163018/registro-desktop-resultados.json) |
| Conservación de datos | Aprobado previamente: nombres/acceso conservados en móvil y avance/retroceso conservados en escritorio | [Móvil](evidencias-nueve-tareas-20261107-163018/mobile-registro-validacion.json), [escritorio](evidencias-nueve-tareas-20261107-163018/registro-desktop-resultados.json) |
| Responsive 360 px | Aprobado para la referencia CSS: Chrome de escritorio 360×800, client/scroll 345/345, sin desbordamiento. No se presenta como teléfono físico de 360 px | [Medidas](evidencias-nueve-tareas-20261107-163018/registro-desktop-resultados.json), [captura 360](evidencias-nueve-tareas-20261107-163018/registro-360.png) |
| Responsive 1366 px | Aprobado previamente: Chrome de escritorio 1366×768, client/scroll 1351/1351, sin desbordamiento | [Medidas](evidencias-nueve-tareas-20261107-163018/registro-desktop-resultados.json), [captura 1366](evidencias-nueve-tareas-20261107-163018/registro-1366.png) |

### Alcance móvil y reutilización

La prueba física corresponde al Xiaomi `DEQCAQEUVS9PNRDQ`, **375 px CSS nativos y scrollWidth 375**, no 360. La referencia responsive exacta de 360 está cubierta por Chrome de escritorio; no se alteró el dispositivo ni se llamó «físico» a una emulación. No se certifica hardware nativo de 360 px. La matriz documenta separadamente el teléfono real y la referencia de diseño, sin ocultar esa diferencia.

Los resultados antiguos de validaciones usan API simulada; eso no invalida las observaciones de campos, foco o geometría, pero su antiguo error de correo duplicado no acreditaba backend real. Ese hueco se cubre ahora con el caso aislado nuevo. La prueba de duplicado real se hizo en escritorio; no se afirma haber repetido ese caso contra backend real en el teléfono.

### Caso nuevo: duplicado real y ausencia de segunda cuenta

Entorno: frontend del checkout en localhost:5176 con `VITE_USAR_MOCKS=false`, caché separada; Spring Boot real del checkout en localhost:19059; PostgreSQL/PostGIS desechable `not59_qa` en localhost:55459. No se interceptaron respuestas API ni se sustituyó el repositorio de usuarios por mocks. El backend mantuvo proveedores de correo/fotos sin credenciales reales y envío de reportes deshabilitado.

Se reutilizó la cuenta de desarrollo `admin.dev@notiurba.local` sembrada por V4 en esa base, no una cuenta ciudadana real. Se intentó registrar otra identidad sintética con el mismo correo y documento de prueba distinto `9000000059`. El endpoint respondió `409`, `{codigo: CORREO_YA_REGISTRADO, mensaje: Ya existe una cuenta con este correo, campo: correo}`. Se inspeccionó la captura y el mensaje apareció bajo el campo de correo.

Como la validación de barrio precede a la de duplicado, se preparó **una sola entrada de referencia auténtica**, «Franciscanos», comuna 2, tomada literalmente del CSV histórico municipal ya disponible. Fue exclusivamente un fixture de la base desechable, no un barrio inventado, una carga de V17 ni una aprobación de vigencia de ese catálogo en 2026. El administrador interno no se ofreció en el selector. [SQL del fixture](evidencias-NOT-59-20261108/fixture-referencia.sql). No se solucionó ni se tocó el catálogo de producción.

Antes y después: `users=1`, `matchingEmail=1`, `matchingIds=[1]`, `newDocument=0`, `verificationTokens=0`, `flywayMax=16`. El rechazo ocurrió antes de crear usuario o iniciar verificación. No se creó una segunda cuenta ni se enviaron correos. [Resumen verificable y matriz](evidencias-NOT-59-20261108/resultados.json), [script del único caso nuevo](evidencias-NOT-59-20261108/verificacion-duplicado.js).

### Incidencias preparadas para Katherine y bloqueo separado

- **B-01, datos/backend:** el catálogo productivo está vacío según el contexto confirmado y la auditoría previa; el aviso «No hay barrios disponibles» es el estado correcto y no un defecto nuevo del selector. No sustituirlo por ejemplos ni habilitar el barrio administrativo. V17 sigue pendiente de autorización y no se aplicó.
- **Alcance visual:** conservar el error de duplicado junto al correo, en español, y la información introducida; el caso nuevo no mostró un defecto que requiera cambiar el formulario. Distinguir móvil real a 375 de la referencia de escritorio a 360 al comunicar resultados. No se envió un mensaje a Katherine ni se consultó Linear; quedan las incidencias documentadas para entrega.
- **Integración:** el registro completo en producción, su correo de verificación y persistencia de una nueva aceptación siguen bloqueados por los datos y requieren entorno/autorización específicos. No forman parte del rechazo de duplicado demostrado. **RF-03/NOT-71 no se declaran aprobadas por cerrar NOT-59.**

No se modificó código funcional, backend ni flujos antiguos: solo herramientas de QA aisladas y esta sección del informe. No hubo nuevas suites ni pruebas repetidas de los módulos ya aprobados; se comprobaron las evidencias existentes y la igualdad de los conteos SQL. Sin cambios de rama, Neon/Render productivos, commits, push, PR o despliegues.
