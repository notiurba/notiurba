# Reproducir EDT 5.1

Este paquete es una preparación de cambios, no una certificación de Spring Boot.
No se debe hacer push del historial local reconstruido: primero aplicar el parche en un clon auténtico.

## Aplicar en el repositorio real

Base revisada: 2d6ceca3804ceebe1df95e838ef90bab911bcd9b. Si main cambió, revisar conflictos y conservar el trabajo nuevo.

```powershell
git fetch origin main
git switch -c codex/edt-5-1-validaciones origin/main
git apply --check C:/ruta/NotiUrba-EDT51.patch
git apply C:/ruta/NotiUrba-EDT51.patch
```

No aplicar si --check falla. No reemplazar directorios completos ni resetear cambios de compañeros.

## Pruebas locales

Requiere Java 21 y Maven con salida HTTPS a Maven Central.

```powershell
mvn -B -f backend/pom.xml test
mvn -B -f backend/pom.xml package
```

Con IntelliJ de esta máquina se localizó Maven en:
C:/Program Files/JetBrains/IntelliJ IDEA 2026.2.3/plugins/maven-plugin/lib/maven3/bin/mvn.cmd

Si el repositorio Maven predeterminado no es escribible, usar un directorio permitido:
```powershell
mvn -B "-Dmaven.repo.local=C:/ruta/escribible/maven-cache" -f backend/pom.xml test
```

Prueba sin dependencias, ejecutada satisfactoriamente aquí:
```powershell
javac -d verification-classes backend/src/main/java/com/notiurba/foto/JpegValidator.java backend/src/test/java/com/notiurba/foto/JpegValidatorCheck.java
java "-Djava.awt.headless=true" -cp verification-classes com.notiurba.foto.JpegValidatorCheck
```

## Neon: solo una base aislada

Inyectar TEST_DB_URL, TEST_DB_USER y TEST_DB_PASSWORD en el proceso. Usar URL JDBC, no postgresql://, sin contraseña embebida, con sslmode=verify-full.
Spring no carga .env automáticamente. El archivo backend/.env.validation.example es una plantilla, no credenciales reales.

```powershell
mvn -B -f backend/pom.xml -Pspatial-tests verify
```

La prueba activa validation, no ejecuta Flyway ni DDL y usa solo SELECT sintéticos. La sesión es read-only. También debe probarse por separado una migración completa en una base nueva; este perfil no certifica Flyway/PostgreSQL 18.

Para arrancar el backend de diagnóstico, definir también SPRING_PROFILES_ACTIVE=validation y JWT_SECRET aleatorio de al menos 32 bytes. Esta app necesita una base aislada con el esquema de login si se pretende autenticar por /api/auth/login. El equipo debe aprovisionar exclusivamente identidades sintéticas; no copiar usuarios ciudadanos para una demostración.

## Cloudinary real, a través de Spring MVC

Inyectar CLOUDINARY_CLOUD_NAME, CLOUDINARY_API_KEY y CLOUDINARY_API_SECRET. El test no requiere Neon: usa una slice MVC y el proveedor real. No imprimir esas variables.

```powershell
mvn -B -f backend/pom.xml -Pcloudinary-tests verify
```

Para uso manual en un servicio aislado: PHOTO_DIAGNOSTICS_ENABLED=true y un token ADMIN válido. POST multipart /api/admin/diagnostics/photos (campo file) devuelve id y ruta de recuperación; GET /api/admin/diagnostics/photos/{id}/link devuelve una URL HTTPS firmada por 60 segundos; DELETE /api/admin/diagnostics/photos/{id} elimina solo ese recurso de prueba. Deshabilitar la bandera al terminar.

El navegador sin sesión se prueba con ese enlace firmado y una imagen sintética, sin hacerlo público permanentemente. No pegar enlaces firmados en logs o informes. Una aprobación de HttpClient no acredita por sí sola la prueba del navegador.

## Render y reposo

render.yaml y backend/Dockerfile están preparados, no desplegados. Verificar manualmente que el servicio sea gratuito antes de importarlos. No cambiar servicios existentes.

El blueprint usa validation y requiere TEST_DB_* / JWT_SECRET / CORS_ORIGINS. Habilitar DIAGNOSTICS_ENABLED=true solo durante la verificación. Las variables Cloudinary solo se necesitan si también se habilita PHOTO_DIAGNOSTICS_ENABLED.

En PowerShell 7, definir EDT51_RENDER_URL (origen HTTPS del servicio aislado) y EDT51_ADMIN_TOKEN (JWT de prueba obtenido del login aislado). Nunca guardarlos en el repositorio.

```powershell
./tools/Measure-Edt51Latency.ps1 -Phase before -Evidence edt51-latency.json
# Dejar pasar al menos 16 minutos sin acceder al servicio ni monitorearlo.
./tools/Measure-Edt51Latency.ps1 -Phase after -Evidence edt51-latency.json -ConfirmExclusiveIdle
```

No ejecutar ninguna petición intermedia. Verificar ausencia de otros usuarios, monitores o accesos externos antes de ConfirmExclusiveIdle. La expiración del token debe superar todo el intervalo. El script no refresca tokens ni genera tráfico para mantener el servicio activo.

Cada fase produce cinco muestras UTC/HTTP/ms y validación del resultado espacial. No reescribe fases existentes; si se repite, crear otro archivo. Tras terminar, desactivar diagnósticos.

## CI y PR

Workflow preparado para pull requests, Java 21, mvn test/package, informes Surefire. Las pruebas reales de proveedores están desactivadas sin opt-in; un CI verde con pruebas optativas omitidas no aprueba Neon/Cloudinary.

Después de ejecutar y revisar en un entorno con permisos:
```powershell
git add backend .github render.yaml tools docs/validacion .gitignore
git commit -m "Preparar validaciones técnicas EDT 5.1 y evidencia verificable"
git push -u origin codex/edt-5-1-validaciones
# No crear PR ni fusionar hacia main en esta tarea.
```

La publicación quedó bloqueada aquí por HTTP 403 al crear la rama. No hay PR remoto. La revisión debe incluir compilación completa y resultados reales antes de considerar el código funcional verificado.

## Continuación del 27/10/2026

La rama remota ya existe. Solo render.yaml fue publicado, en 9bd1627. El resto está en el paquete de continuación y el parche completo NotiUrba-EDT51-v2.patch.
Los comandos iniciales git switch -c deben adaptarse si la rama ya existe: en un clon auténtico, hacer fetch, guardar cambios, cambiar a codex/edt-5-1-validaciones y comparar con origin/main. No crear otra rama con el mismo nombre ni forzar push.

Para aplicar el parche completo sobre una rama que ya tiene render.yaml, primero revisar ese archivo: su versión remota ya coincide con la nueva. Puede comprobarse el resto con git apply --check --exclude=render.yaml y luego aplicar con la misma exclusión. No aplicar ningún parche si la comprobación falla.

validation ahora exige esquema existente con ddl-auto=validate. No lo crea; faltas de tablas/tipos deben fallar. Se añadieron pruebas de Point, MultiPolygon y Spring Data JPA.

Migraciones de PostgreSQL 18 en contenedor nuevo, sin Neon:
```powershell
mvn -B -f backend/pom.xml -Pmigration-tests verify
```

Necesita Docker activo. No se omite silenciosamente si falta Docker. El contenedor no monta volúmenes de datos existentes y se elimina por Testcontainers. Mantener V1–V4 sin cambios.

Las 18 comprobaciones locales de YAML/XML/rutas se ejecutaron con SnakeYAML 2.6 ya disponible. Para repetirlas, compilar tools/ValidationConfigurationCheck.java con ese JAR en classpath; esto no sustituye la validación del blueprint por Render.

GitHub Actions aún no se ejecutó: el workflow no está publicado. El CI propuesto no usa secretos Neon/Cloudinary y tiene un job separado para PostgreSQL 18 desechable.

La carga local desde Chrome requiere que el usuario habilite Allow access to file URLs para la extensión. No se cambió ese permiso. El control del navegador se detuvo durante esta continuación y no debe suponerse que terminó la sincronización.

## Ejecucion directa desde el paquete actualizado

Guardar el ZIP actualizado y su parche completo v3 juntos; extraer el ZIP sin reemplazar un repositorio existente. En PowerShell7 con Git, Java21, Maven y Docker activo:

```powershell
# Desde la carpeta extraida. La ruta de Repository debe ser NUEVA o un clon autentico limpio.
./tools/Complete-Edt51.ps1 -Repository C:/proyectos/notiurba -Patch ../NotiUrba-EDT51-v3.patch -Migration
# Con secretos de prueba ya inyectados en el proceso y base aislada pre-migrada:
./tools/Complete-Edt51.ps1 -Repository C:/proyectos/notiurba -Patch ../NotiUrba-EDT51-v3.patch -Migration -Spatial -Cloudinary -Publish
```

No ejecutar -Spatial sobre una base compartida. -Publish publica exclusivamente codex/edt-5-1-validaciones; no crea PR, no fusiona y no despliega. Revisar los logs y Surefire/Failsafe antes de marcar integraciones; los estados del informe no se actualizan automaticamente. Si la aplicacion del parche falla, se detiene sin sobrescribir archivos: comparar el parche con las nuevas versiones de los companeros y resolver manualmente. Las integraciones externas son explicitas y no deben activarse con secretos en PR no confiables. Revisar Actions con gh run list --repo notiurba/notiurba y gh run view ID --log-failed.