# Frontend de NOTIURBA

## Descripción

NOTIURBA es un proyecto de software orientado a la gestión de reportes ciudadanos georreferenciados en Tuluá, Valle del Cauca.

El prototipo permite visualizar las diez comunas urbanas, consultar reportes de prueba, aplicar filtros, visualizar un mapa de calor y analizar los límites geográficos.

## Tecnologías utilizadas

- React y Vite.
- Leaflet, React-Leaflet y Leaflet.heat.
- OpenStreetMap.
- JavaScript y CSS.

## Requisitos previos

- Node.js 20.19 o superior y pnpm (se activa con `corepack enable`).
- Git para clonar el repositorio.

## Instalación

### 1. Clonar el repositorio

```powershell
git clone https://github.com/notiurba/noti-urba.git
cd noti-urba/frontend
```

### 2. Instalar las dependencias

```powershell
corepack enable
pnpm install
```

`corepack enable` activa la versión de pnpm declarada en `package.json`. Las bibliotecas del mapa ya están declaradas ahí, por lo que no es necesario instalarlas por separado.

### 3. Ejecutar el proyecto

Este proyecto es el frontend de NOTIURBA, así que el comando `pnpm dev`
siempre debe ejecutarse estando parado en la carpeta `frontend/` (donde está
el `package.json` de este proyecto), no en la raíz del repositorio ni en
`backend/`.

```powershell
cd frontend
pnpm dev
```

Abre en el navegador la dirección que aparezca en la terminal, normalmente [http://localhost:5173/](http://localhost:5173/).

## Comandos útiles

Todos estos comandos se ejecutan dentro de la carpeta `frontend/`.

| Comando | Función |
| --- | --- |
| `pnpm install` | Instala las dependencias. |
| `pnpm dev` | Inicia el servidor de desarrollo. |
| `pnpm lint` | Revisa el código con ESLint. |
| `pnpm build` | Genera la versión de producción. |

## Nota

Los límites de las comunas utilizados en el prototipo provienen de la cartografía del POT 2015 de Tuluá. Su vigencia administrativa está pendiente de confirmación.
