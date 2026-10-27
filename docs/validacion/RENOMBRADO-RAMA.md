# Renombrado operativo de la rama EDT 5.1

La rama autorizada actual es feature/edt-5.1-technical-validation. El nombre anterior solo se conserva en evidencias historicas. fb3812a3bb1994e7c40b1bf205ffcf3cfc5e37f2 permanece en el historial, con todos sus cambios.

Se verificaron un arbol inicialmente limpio y el clon autentico. origin/feature/edt-5.1-technical-validation estaba en a599593; los dos commits locales posteriores se conservan para publicarlos por avance directo. origin/main sigue en 2d6ceca3804ceebe1df95e838ef90bab911bcd9b. No se crean PR, fusiones, force push ni commits en main/develop.

Referencias operativas actualizadas: workflow (push y guardias de jobs externos), Publish-Edt51Branch.ps1 (destino/guardia/enlace Actions), Complete-Edt51.ps1, Invoke-Edt51External.ps1, fixture de controles externos, verificador Java, Render y las guias INTEGRACIONES-EXTERNAS.md/README-ejecucion.md. No cambian los jobs, pruebas ni criterios de aprobacion por este renombrado.

La documentacion de Secrets indica ahora seleccionar solo la rama nueva en el Environment edt51-external-validation. No se cambiaron las reglas de Environment ni infraestructura desde esta sesion. El responsable de GitHub debe verificar esa regla antes de ejecutar integraciones externas.

Comprobaciones reales:27 verificaciones de configuracion,8 controles externos locales y parseo de los6 scripts PowerShell sin errores. Inspeccion estatica de las guardias de los3 scripts: correctas y sin referencias al nombre anterior. Evidencias nuevas: logs/branch-rename/. La simulacion de otras ramas sustituyendo git fue rechazada por la revision automatica antes de ejecutarse; no se acredita como una prueba dinamica aprobada. No se cambio de rama para probar main/develop.

El trigger push contiene exclusivamente feature/edt-5.1-technical-validation y las guardias externas exigen refs/heads/feature/edt-5.1-technical-validation. Esto acredita configuracion; una ejecucion real de Actions requiere publicar con exito y consultar el run. No se confunde con integraciones Neon/Cloudinary aprobadas.

Las evidencias previas (informe, JSON, CSV, logs existentes, revision Cloudinary y ejecucion-rama-validaciones.md) permanecen byte por byte intactas, igual que backend, frontend y V1-V4. Solo se agregan estas nuevas evidencias de renombrado.

Si la autenticacion bloquea publicar, ejecutar desde PowerShell normal de Windows:

```powershell
& "C:\Users\Usuario\.codex\.chatgpt-projects\g-p-6aacbd0a1a688191b438845119074d96\edt51-work\notiurba-edt51-publicacion\tools\Publish-Edt51Branch.ps1"
```

El script comprueba rama exacta/arbol limpio, descarga referencias, rechaza avances remotos no incorporados y solo publica HEAD:refs/heads/feature/edt-5.1-technical-validation. No escribe main/develop ni fuerza, fusiona o crea PR.
