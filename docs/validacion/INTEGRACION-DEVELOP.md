# IntegraciÃ³n EDT 5.1 sobre develop (revisiÃ³n)

- Rama temporal: `integration/edt51-develop`.
- `develop` prevalece en Dockerfile, flujo ciudadano, Google OAuth, JWT, APIs y configuraciÃ³n Resend/Cloudinary.
- Se incorporan las pruebas/diagnÃ³sticos y evidencias de EDT 5.1.
- Maven incluye Testcontainers y perfiles de diagnÃ³stico; el SDK Cloudinary es **solo para diagnÃ³sticos aislados**, no reemplaza el servicio ciudadano.
- Se ajusta MigrationIntegrationTest para las migraciones V1â€“V12 de `develop` (no solo V1â€“V4).
- Los diagnÃ³sticos estÃ¡n apagados por defecto.
- No se ejecutan perfiles `spatial-tests` ni `cloudinary-tests` en CI ordinaria: requieren autorizaciÃ³n y recursos aislados.
- No se conecta a Neon oficial `NotiUrba`. La base de pruebas permitida es `NotiUrba-EDT51-isolated`, pero este proceso NO la utiliza. `migration-tests` crea su propia base desechable Testcontainers.
- No se incluye `render.yaml` de la rama EDT en el PR para evitar un blueprint de validaciÃ³n que apunta a otra rama. Revisar su diseÃ±o por separado.
- No se declara apto para producciÃ³n hasta que CI y la revisiÃ³n funcional completen su evaluaciÃ³n.