# NotiUrba · prototipo ciudadano integrado

## Ubicación y base

Carpeta: `C:\Users\Usuario\.codex\.chatgpt-projects\g-p-6aacbd0a1a688191b438845119074d96\NotiUrba-Evaluacion`.

Abre `NotiUrba-Evaluacion.code-workspace` en VS Code. Se preparó el espacio de trabajo y se intentó abrirlo; no se pudo confirmar su apertura mediante la interfaz, que informó que la ventana estaba minimizada. La extensión `openai.chatgpt` figura instalada. Codex ejecutó las modificaciones y pruebas de esta carpeta; no se verificó la autenticación de la extensión dentro de VS Code.

Prototipo previo conservado: `..\NotiUrbaPrompt6-Isolated`. La carpeta `sources/` y la copia `edt51-work` no se modificaron.

Rama local: `design-thinking/prototipo-evaluacion`. Base y última comprobación remota mediante GitHub: `2d6ceca3804ceebe1df95e838ef90bab911bcd9b`. No aparecieron nuevos commits entre ambas consultas. `git fetch origin main` falló por `Could not resolve host: github.com`; el conector de GitHub sí permitió comprobar `main`.

La clonación local convencional falló por permisos del proceso auxiliar de Git. Se extrajo el árbol del SHA comprobado a una carpeta nueva y se copiaron sus objetos Git para conservar el historial y la base verificable. En la entrega inicial no se hicieron commits, push ni PR. Posteriormente, el usuario autorizó subir los cambios exclusivamente a `design-thinking/prototipo-evaluacion`, sin fusionar con main. Se preparó un commit local en esa rama. La subida remota está bloqueada: Git no resuelve github.com y el conector devuelve `403: Resource not accessible by integration` al intentar crear un objeto Git. No hay confirmación de cambios publicados. La base de `origin/main` continúa siendo el SHA indicado arriba.

## Figma: acceso real y correspondencia

Las consultas MCP `create_session`, `get_design_context` y `get_screenshot` fueron rechazadas con un error de permisos. No se obtuvo una sesión MCP válida. Sí se accedió en modo lectura al archivo nuevo desde la pestaña de Chrome y se inspeccionaron los frames y sus dimensiones visibles. No se editó Figma ni Stitch.

El archivo **no está organizado como seis variantes desktop/móvil de las tres vistas**. Contiene bienvenida, autenticación, tres etapas móviles, confirmación, Mis reportes, estados y administración. Se excluyeron autenticación, correos y administración del recorrido solicitado.

| Variante implementada | Ruta | Referencia observada / adaptación |
|---|---|---|
| Inicio escritorio | `/app` | Adaptación responsive de `0 · Bienvenida` (`2:2`, 390 × 900); mapa ilustrativo del prototipo anterior |
| Inicio móvil | `/app` | Misma referencia; acceso directo sin login |
| Crear escritorio | `/app/crear-reporte` | `Web · Nuevo reporte` (`6:2`, 1280 × 1000), dos columnas |
| Crear móvil | `/app/crear-reporte` | `1 · ¿Qué pasó?` (`3:126`, 390 × 1041), `2 · ¿Dónde y cómo está?`, `3 · Revisar y enviar`; convertido a cuatro pasos |
| Consultar escritorio | `/app/consultar-reporte` | Adaptación responsive de Mis reportes y estados; no se identificó un frame desktop específico equivalente |
| Consultar móvil | `/app/consultar-reporte` | `Mis reportes` (`5:2`, 390 × 900), estados y confirmación |

Se conservaron azul de identidad, acciones verdes, tarjetas claras, categorías e iconos sencillos. No se extrajeron tokens tipográficos exactos por MCP ni se afirma fidelidad píxel a píxel. Se añadió Parques y árboles, observado en el Figma, conservando las categorías del prototipo. Se omitieron exigencias de login, foto obligatoria y envío de correo para cumplir el alcance académico. El marcador y logo tipográfico del prototipo son una simplificación; no se exportó la ilustración original.

## Implementación

- Inicio con tres ejemplos ficticios, marcadores seleccionables, detalle compacto y navegación.
- Cuatro pasos exclusivos: categoría, ubicación, detalles/fotos, revisión. Atrás conserva los datos. Dirección manual suficiente; mapa SVG opcional, ficticio y sin coordenadas reales. No GPS ni límites oficiales de comunas.
- Descripción de 10–500 caracteres; hasta tres JPG/PNG opcionales, máximo 5 MB por imagen, decodificación antes de previsualizar y eliminación individual.
- Envío en memoria con bloqueo de doble envío, código interno aleatorio, fecha y confirmación. Código consultable, lista de reportes de sesión, estado, línea de tiempo, evidencia desplegable y WhatsApp con destinatario elegido por el usuario.
- `sessionStorage` separado de los servicios reales conserva reportes y borrador al recargar. Si se excede la cuota de almacenamiento, se avisa y el reporte continúa en memoria: con fotografías grandes la persistencia puede fallar. Cerrar la sesión o limpiar sus datos borra los reportes.
- Aviso académico y aclaración de seguimiento interno. Sin login, API, correos ni envío institucional en este recorrido.

Componentes adaptados del prototipo previo: `PrototipoCiudadano`, `CityMap`, `Icon`, `Badge`, `Timeline`, `ReportDetail`. Se reutiliza React Router oficial; no se sustituyó `App.jsx` ni `main.jsx`. El CSS está limitado mediante `@scope (.noti-evaluation)` para evitar alterar administración. Requiere un navegador moderno que admita CSS `@scope`.

Cambios de aplicación: `frontend/src/app/rutas.jsx`; nuevos `frontend/src/app/evaluacion/PrototipoCiudadano.jsx`, `storage.js`, `evaluacion.css`; configuración estática `frontend/vercel.json` y `frontend/public/_redirects`. `frontend/index.html` declara español y el título del prototipo. ESLint y gitignore excluyen `.qa`, que contiene compilados de verificación. Se añaden este informe, guía, espacio de trabajo y scripts/resultados de verificación. Backend, administración, App, main, package.json y bloqueo pnpm se comprobaron sin diferencias respecto al SHA base. Los scripts temporales de preparación y la exportación local permanecen como auxiliares; la aplicación integrada se ejecuta desde frontend.

## Compilación y pruebas

- Build de producción ejecutado con Vite 8.3.4: **correcto**. Advertencia de bundle de aproximadamente 585 kB: incluye el mapa administrativo existente. No se amplió administración para resolverla.
- Lint completo de `frontend` ejecutado con ESLint 10.12.0: **correcto**, sin errores.
- `test-evaluacion.mjs`: **29 comprobaciones correctas**, sobre los componentes reales cargados con Vite y React en jsdom. Incluye límites de descripción, categoría y dirección requeridas, tipo/cantidad de fotos, quitar foto, volver y corregir, doble envío en el mismo ciclo, código/consulta, WhatsApp, fecha, persistencia tras remontar, entrada directa al formulario y aislamiento de sesión.
- La decodificación de imagen se simula en jsdom; no equivale a comprobar el decodificador real ni el selector de archivos de un celular. Remontar la aplicación comprueba la recuperación desde sessionStorage, no una recarga real de Chrome.
- Servidor Vite iniciado en `127.0.0.1:5187`. Chrome no pudo conectarse (`ERR_CONNECTION_TIMED_OUT`). La prueba HTTP local adicional falló con `connect EACCES 127.0.0.1:5188`. La apertura de HTML local fue bloqueada por la política de protocolos del navegador y no se intentó eludirla.
- **Pendiente**: verificación visual real a 390 × 844 y 1366 × 768, consola de Chrome, recarga directa de rutas en hosting y recorrido administrativo en navegador. Se comprobaron los archivos administrativos intactos y su inclusión en el build; esto no certifica ausencia de regresiones visuales o de interacción. No se entregan capturas del prototipo como si esas pruebas hubieran ocurrido.

## Dependencias y ejecución

Se intentó instalar con pnpm, pero no se pudo resolver pnpm 12.15.0 en el espejo aislado; la red de shell tampoco resuelve GitHub. Se recuperaron paquetes públicos de caché local, comprobando SHA-512. **React Router 7.14.0 de caché es inferior al rango ^7.18.4 declarado**. Es una limitación de esta verificación; no se cambió el manifiesto ni el lock del equipo. Los paquetes recuperados se registran en `OFFLINE-DEPENDENCIES.json`. Tailwind no se incorporó: se usó el CSS disponible para evitar modificar dependencias sin una instalación verificable.

Con los node_modules recuperados, desde la terminal de VS Code:

```powershell
cd frontend
node node_modules/vite/bin/vite.js --host 127.0.0.1 --port 5187
# Abrir http://127.0.0.1:5187/app
```

En otra terminal, dentro de frontend:

```powershell
node node_modules/eslint/bin/eslint.js .
node node_modules/vite/bin/vite.js build
```

En un entorno normal con acceso al registro, desde frontend ejecuta `pnpm install --no-frozen-lockfile` y después `pnpm dev`. Revisa cualquier cambio del bloqueo en esta rama aislada: el bloqueo original contiene configuración del gestor, sin resolver las dependencias del frontend. Repite build, lint y la prueba real con las versiones declaradas antes de publicar. El script de pruebas usa jsdom 21.1.2 presente en esta caché (dependencia solo de verificación; no se añadió al manifiesto oficial). Desde la raíz: `node test-evaluacion.mjs`.

## URL para cinco evaluadores

No se publicó nada. Localhost no es una URL pública. Tras resolver la instalación reproducible y comprobar el recorrido en navegador, compila frontend y publica **frontend/dist** en un sitio estático nuevo y aislado, con autorización previa. Puede usarse la carga manual de Netlify sin conectar ni hacer push al repositorio del equipo; `_redirects` se incluye en el build. Para Vercel, usar frontend como raíz y la configuración preparada. Revisa la cuota gratuita vigente en la cuenta antes de autorizar el despliegue; no se contrató ningún plan.

Prueba la URL HTTPS en una ventana nueva y en un celular, abre directamente `/app/crear-reporte` y `/app/consultar-reporte`, actualiza y completa el recorrido antes de compartirla con P1–P5. No utilices el sitio de producción. Entrega `GUIA-EVALUADOR.md` a los participantes. La aplicación es responsive; no se certifica instalación PWA ni funcionamiento offline como PWA.
