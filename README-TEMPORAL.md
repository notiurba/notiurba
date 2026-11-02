# README temporal — 🚀 Instalación y ejecución de NotiUrba

### 1. Requisitos

Tener instalado **Node.js**, **pnpm** y **Visual Studio Code**.

### 2. Abrir el proyecto

En la terminal de VS Code (PowerShell):

```powershell
cd "C:\Users\Usuario\.codex\.chatgpt-projects\g-p-6aacbd0a1a688191b438845119074d96\NotiUrba-Evaluacion"
cd frontend
```

> La ruta puede cambiar dependiendo del computador donde esté guardado el proyecto.

### 3. Instalar dependencias

```powershell
pnpm install
```

Solo es necesario hacerlo la primera vez o cuando cambien las dependencias.

### 4. Ejecutar la aplicación

```powershell
node node_modules/vite/bin/vite.js --host 127.0.0.1 --port 5187
```

### 5. Abrir NotiUrba

Ingresar desde el navegador:

[**http://127.0.0.1:5187/app**](http://127.0.0.1:5187/app)

### 6. Rutas disponibles

| Pantalla | Ruta |
| --- | --- |
| Inicio | `/app` |
| Crear reporte | `/app/crear-reporte` |
| Consultar reporte | `/app/consultar-reporte` |

### 7. Detener la aplicación

Presionar `Ctrl + C` en la terminal.

### Solución de errores comunes

Si aparece `Cannot find module vite`, comprobar que estás dentro de `frontend` y ejecutar:

```powershell
pnpm install
```

Si aparece `No se encuentra la ruta frontend`, verificar la ubicación del proyecto con:

```powershell
Get-Location
dir
```

**Nota:** Es un prototipo académico que utiliza datos simulados y no envía reportes reales a la Alcaldía de Tuluá.
