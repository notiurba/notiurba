async page => {
  page.setDefaultTimeout(12000);
  if(page.url().endsWith('/login')){
    await page.getByRole('textbox',{name:'Correo electrónico'}).fill('nuevo@correo.com');
    await page.getByRole('textbox',{name:'Contraseña',exact:true}).fill('Clave1234');
    await page.getByRole('button',{name:'Iniciar sesión',exact:true}).click();
    await page.waitForURL('**/app/reportes');
  }
  const ctx=page.context();
  const origin='http://127.0.0.1:5173';
  const normal=await ctx.newPage();
  normal.setDefaultTimeout(12000);
  await normal.goto(origin+'/reportar');
  await normal.getByRole('radio').first().waitFor();
  await normal.getByRole('radio').first().check();
  await normal.getByRole('button',{name:'Continuar',exact:true}).click();
  await normal.locator('#idBarrio').waitFor();
  const comparison=await normal.evaluate(async()=>{
    const {obtenerBarrios}=await import('/src/compartido/api/catalogos.js');
    const mocked=await obtenerBarrios();
    return {date:new Date().toISOString(),apiStatus:200,actual:[],actualEvidence:'docs/validacion/evidencias-barrios-frontend/api-barrios-vite.body.json (GET real verificado previamente)',mocked,options:[...document.querySelector('#idBarrio').options].map(o=>({id:o.value,name:o.textContent})),simulationNotice:document.body.innerText.includes('Modo demostración')};
  });
  await normal.locator('#idBarrio').selectOption('8');
  await normal.locator('.mapa-ubicacion').click({position:{x:140,y:140}});
  const draft=await normal.evaluate(()=>JSON.parse(sessionStorage.getItem('notiurba.borrador')));
  await normal.screenshot({path:'output/playwright/barrios-despues-mocks.png',fullPage:true});
  const empty=await ctx.newPage();
  empty.setDefaultTimeout(12000);
  let replayed=false;
  await empty.route('**/src/compartido/api/catalogos.js*',async r=>{
    const response=await r.fetch();
    const body=await response.text();
    const changed=body.replace(/export const obtenerBarrios = \(\) => peticion\(([^;]+)\);?/, 'export const obtenerBarrios = async () => '+JSON.stringify(comparison.actual)+';');
    replayed=changed!==body;
    await r.fulfill({response,body:changed});
  });
  await empty.addInitScript(d=>sessionStorage.setItem('notiurba.borrador',JSON.stringify(d)),draft);
  await empty.goto(origin+'/reportar/ubicacion');
  await empty.locator('.mapa-ubicacion').waitFor();
  await empty.waitForTimeout(1200);
  const staleNotice=await empty.getByText('Revise el barrio anterior',{exact:true}).isVisible();
  await empty.screenshot({path:'output/playwright/barrios-despues-id-obsoleto.png',fullPage:true});
  await empty.getByRole('button',{name:'Continuar',exact:true}).click();
  await empty.waitForURL('**/reportar/detalles');
  const stale=await empty.evaluate(async()=>{
    const b=JSON.parse(sessionStorage.getItem('notiurba.borrador'));
    const {armarSolicitudReporte}=await import('/src/compartido/validaciones/reporte.js');
    const request=armarSolicitudReporte(b,{categoria:{requiereUbicacion:true,admiteMatricula:false}});
    return {url:location.pathname,errors:[...document.querySelectorAll('.campo__error')].map(e=>e.innerText),staleIdCleared:b.idBarrio==='',pointPreserved:Number.isFinite(b.latitud)&&Number.isFinite(b.longitud),payloadOmitsStaleId:!Object.hasOwn(request,'idBarrio'),payloadKeepsPoint:Object.hasOwn(request,'latitud')&&Object.hasOwn(request,'longitud')};
  });
  await empty.screenshot({path:'output/playwright/barrios-despues-continua.png',fullPage:true});
  const delayed=await ctx.newPage();
  delayed.setDefaultTimeout(12000);
  let delayedModule=false;
  await delayed.route('**/src/compartido/api/catalogos.js*',async r=>{
    const response=await r.fetch();
    const body=await response.text();
    const changed=body.replace(/export const obtenerBarrios = \(\) => peticion\(([^;]+)\);?/, 'export const obtenerBarrios = async () => { await new Promise(r=>setTimeout(r,5000)); return peticion($1); };');
    if(changed===body){await r.fulfill({response});return;}
    delayedModule=true;
    await r.fulfill({response,body:changed});
  });
  await delayed.addInitScript(d=>sessionStorage.setItem('notiurba.borrador',JSON.stringify({...d,idBarrio:''})),draft);
  await delayed.goto(origin+'/reportar/ubicacion');
  await delayed.locator('.mapa-ubicacion').waitFor();
  const loading=await delayed.evaluate(()=>({falseEmpty:document.body.innerText.includes('No hay barrios disponibles.'),loadingShown:document.body.innerText.includes('Cargando barrios'),selectorExists:!!document.querySelector('#idBarrio')}));
  await delayed.screenshot({path:'output/playwright/barrios-despues-cargando.png',fullPage:true});
  await delayed.locator('#idBarrio').waitFor();
  return {comparison,stale:{...stale,staleNotice,replayedActualPayload:replayed,replayScope:'Solo esta pestaña: catálogo real reproducido en simulador para aislar el frontend; no es una prueba integral sin mocks'},loading:{...loading,explicitDelayInjected:delayedModule},mapDoesNotAutoAssignBarrio:draft.idBarrio==='8',gpsRequested:false,reportSubmitted:false,passed:comparison.simulationNotice&&staleNotice&&stale.staleIdCleared&&stale.pointPreserved&&stale.payloadOmitsStaleId&&stale.payloadKeepsPoint&&replayed&&!loading.falseEmpty&&loading.loadingShown&&delayedModule};
}
