async page => {
  const message='Su reporte está guardado. No se completaron las fotos o la solicitud de envío. Reintente sobre este mismo código.';
  await page.getByText(message,{exact:true}).waitFor();
  const retry=await page.getByRole('button',{name:'Reintentar sobre este código',exact:true}).isEnabled();
  const state=await page.evaluate(async()=>{
    const guardado=JSON.parse(sessionStorage.getItem('notiurba.reporteGuardado'));
    if(!guardado)throw Error('Falta el código guardado');
    const detail=await(await import('/src/app/servicios/correoReportes.js')).consultar(guardado.codigoSeguimiento);
    const timing=performance.getEntriesByType('resource').filter(r=>r.name.endsWith('/api/reportes')||r.name.endsWith('/fotos')).map(r=>({url:r.name,status:r.responseStatus,startUtc:new Date(performance.timeOrigin+r.startTime).toISOString(),endUtc:new Date(performance.timeOrigin+r.responseEnd).toISOString(),duration:r.duration}));
    return{guardado,detail,timing};
  });
  if(!retry||state.detail.fotos.length!==0||!state.timing.some(t=>t.url.endsWith('/fotos')&&t.status===502))throw Error('Fallo y recuperación no demostrados');
  await page.screenshot({path:'C:/notiurba/noti-urba/docs/validacion/NOT-25-final-20261107/02-fallo-y-reintento.png',fullPage:true});
  return{phase:'fallo',date:new Date().toISOString(),reportCode:state.guardado.codigoSeguimiento,message,retryEnabled:retry,photosAfterFailure:state.detail.fotos.length,guardado:state.guardado,timing:state.timing,scope:'Captura del fallo 502 ya ocurrido; no se repitió la subida ni se creó otro reporte'};
}
