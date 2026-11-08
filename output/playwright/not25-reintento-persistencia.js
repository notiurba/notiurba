async page => {
  page.setDefaultTimeout(25000);
  const folder='C:/notiurba/noti-urba/docs/validacion/NOT-25-final-20261107';
  const code=await page.evaluate(()=>JSON.parse(sessionStorage.getItem('notiurba.reporteGuardado'))?.codigoSeguimiento);
  if(!code)throw Error('No hay código conservado; no recrear el reporte');
  const traffic=[];
  page.on('response',r=>{if(r.url().includes('/api/'))traffic.push({url:r.url(),method:r.request().method(),status:r.status()});});
  const uploaded=page.waitForResponse(r=>r.request().method()==='POST'&&r.url().endsWith('/fotos'));
  await page.getByRole('button',{name:'Reintentar sobre este código',exact:true}).click();
  await page.getByRole('button',{name:'Confirmar',exact:true}).click();
  const response=await uploaded;
  const body=await response.json();
  if(response.status()!==201||body.codigoSeguimiento!==code||!body.foto?.url)throw Error('La subida de reintento no persistió la URL');
  await page.waitForFunction(()=>!document.querySelector('button[aria-busy="true"]'));
  const detail=await page.evaluate(async code=>(await import('/src/app/servicios/correoReportes.js')).consultar(code),code);
  if(detail.codigoSeguimiento!==code||detail.fotos.length!==1||detail.fotos[0].url!==body.foto.url)throw Error('GET no devuelve la foto asociada al mismo código');
  const photo=await page.request.get(body.foto.url);
  if(photo.status()!==200||!photo.headers()['content-type'].startsWith('image/jpeg'))throw Error('La URL local de QA no devuelve un JPEG real');
  const bytes=(await photo.body()).length;
  if(bytes!==577779)throw Error('La URL no sirve los bytes adjuntados');
  const createdAgain=traffic.filter(r=>r.method==='POST'&&r.url.endsWith('/api/reportes'));
  if(createdAgain.length)throw Error('Se creó otro reporte al reintentar');
  await page.screenshot({path:folder+'/03-reintento-final.png',fullPage:true});
  const reloaded=await page.evaluate(async code=>(await import('/src/app/servicios/correoReportes.js')).consultar(code),code);
  return {phase:'reintento-persistencia',date:new Date().toISOString(),reportCode:code,uploadStatus:response.status(),upload:body,detail:reloaded,photoHttp:{url:body.foto.url,status:photo.status(),contentType:photo.headers()['content-type'],bytes},extraCreationsOnRetry:createdAgain.length,traffic,provider:'Almacén local real de archivos para QA, no Cloudinary',scope:'HTTP Spring Boot y PostgreSQL desechable reales; no respuestas API interceptadas'};
}
