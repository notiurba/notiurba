async page => {
  page.setDefaultTimeout(25000);
  const evidence='C:/notiurba/noti-urba/docs/validacion/NOT-25-final-20261107';
  const traffic=[];
  page.on('response', r=>{if(r.url().includes('/api/'))traffic.push({url:r.url(),method:r.request().method(),status:r.status()});});
  await page.waitForFunction(async()=>{try {const r=await fetch('/api/health');const body=await r.json();return r.ok&&body.baseDeDatos==='ok'} catch {return false}},null,{timeout:60000,polling:2000});
  await page.locator('#correo').fill('not25@qa.invalid');
  await page.locator('#password').fill('SoloPruebasNOT25-123');
  await page.getByRole('button',{name:'Iniciar sesión',exact:true}).click();
  await page.waitForURL(u=>u.pathname.startsWith('/app'));
  const setup=await page.evaluate(async()=>{
    const r=await fetch('/api/catalogos/barrios');const barrios=await r.json();
    const barrio=barrios.find(b=>b.nombre==='Barrio tecnico NOT25, solo QA aislado');
    if(!barrio)throw Error('No existe barrio de QA');
    const draft={idCategoria:'11',idBarrio:String(barrio.idBarrio),latitud:null,longitud:null,direccion:'Entorno aislado NOT25',descripcion:'Validación aislada NOT-25 de fotografía pública de prueba; no corresponde a un reporte ciudadano real.',matricula:'0000000000',emergencia:false};
    sessionStorage.setItem('notiurba.borrador',JSON.stringify(draft));
    return {barrio:barrio.idBarrio,origen:'Datos técnicos en PostgreSQL desechable; no V17 ni datos geográficos oficiales'};
  });
  await page.goto('http://127.0.0.1:5175/reportar/detalles');
  await page.locator('input[type=file][multiple]').setInputFiles('C:/notiurba/noti-urba/docs/validacion/NOT-88-commons-20261108T011359Z/reducidas/foto-1.jpg');
  await page.getByRole('button',{name:'Continuar',exact:true}).click();
  await page.waitForURL('**/reportar/confirmacion');
  await page.locator('#autorizaEnvio').check();
  const uploadRequest=page.waitForRequest(r=>r.method()==='POST'&&r.url().endsWith('/fotos'));
  const uploadResponse=page.waitForResponse(r=>r.request().method()==='POST'&&r.url().endsWith('/fotos'));
  await page.getByRole('button',{name:'Guardar reporte',exact:true}).click();
  await page.getByRole('button',{name:'Confirmar',exact:true}).click();
  const request=await uploadRequest;
  await page.locator('button[aria-busy="true"]').first().waitFor();
  const loading=await page.locator('button[aria-busy="true"]').evaluateAll(es=>es.map(e=>({text:e.textContent,disabled:e.disabled,visible:!!e.getClientRects().length})));
  await page.screenshot({path:evidence+'/01-subida-cargando.png',fullPage:true});
  const response=await uploadResponse;
  if(response.status()!==503)throw Error('Se esperaba fallo controlado 503, recibido '+response.status());
  await page.getByRole('button',{name:'Reintentar sobre este código',exact:true}).waitFor();
  const message='Su reporte está guardado. No se completaron las fotos o la solicitud de envío. Reintente sobre este mismo código.';
  await page.getByText(message,{exact:true}).waitFor();
  const state=await page.evaluate(async()=>{
    const guardado=JSON.parse(sessionStorage.getItem('notiurba.reporteGuardado'));
    const {consultar}=await import('/src/app/servicios/correoReportes.js');
    const detalle=await consultar(guardado.codigoSeguimiento);
    return {guardado,detalle};
  });
  await page.screenshot({path:evidence+'/02-fallo-y-reintento.png',fullPage:true});
  const creations=traffic.filter(t=>t.method==='POST'&&t.url.endsWith('/api/reportes'));
  if(creations.length!==1||state.detalle.fotos.length!==0||!loading.some(e=>e.visible&&e.disabled))throw Error('No se demostró carga, conservación o fallo');
  return {phase:'fallo-carga',date:new Date().toISOString(),setup,loading,photoRequest:request.url(),failureStatus:response.status(),message,reportCode:state.guardado.codigoSeguimiento,storedPhotosAfterFailure:state.detalle.fotos.length,createdReportsHttp:creations.length,traffic,scope:'Chrome y API reales, PostgreSQL aislado, fallo controlado de almacén local; no es prueba de caída de Cloudinary'};
}
