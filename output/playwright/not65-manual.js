async page => {
  await page.getByRole('radio').first().check();
  await page.getByRole('button',{name:'Continuar',exact:true}).click();
  await page.waitForURL('**/reportar/ubicacion');
  const map=page.getByRole('application',{name:'Mapa para marcar el lugar del problema'});
  // The map's role is inspected from its actual markup if Leaflet exposes another role.
  const canvas=page.locator('.mapa-ubicacion');
  await canvas.waitFor({state:'visible'});
  await page.waitForFunction(()=>[...document.querySelectorAll('img.leaflet-tile')].length>0&&[...document.querySelectorAll('img.leaflet-tile')].every(i=>i.complete&&i.naturalWidth>0));
  const out={browser:await page.evaluate(()=>navigator.userAgent),cases:[],responsive:[]};
  const point=()=>page.evaluate(()=>{const b=JSON.parse(sessionStorage.getItem('notiurba.borrador'));return [b.latitud,b.longitud]});
  out.cases.push({id:'MAP-01',passed:true,tiles:await page.locator('img.leaflet-tile').count()});
  await page.screenshot({path:'output/playwright/not65-desktop-01-mapa.png',fullPage:true});
  await canvas.click({position:{x:140,y:140}});
  await page.locator('.leaflet-marker-icon').waitFor();
  const first=await point();
  out.cases.push({id:'MAP-05',passed:first.every(Number.isFinite),markers:await page.locator('.leaflet-marker-icon').count()});
  await page.screenshot({path:'output/playwright/not65-desktop-05-manual.png',fullPage:true});
  await canvas.click({position:{x:260,y:180}});
  const second=await point();
  out.cases.push({id:'MAP-06',passed:JSON.stringify(first)!==JSON.stringify(second),markers:await page.locator('.leaflet-marker-icon').count()});
  await page.screenshot({path:'output/playwright/not65-desktop-06-cambio.png',fullPage:true});
  await page.getByRole('button',{name:'Continuar',exact:true}).click();
  await page.waitForURL('**/reportar/detalles');
  const detailText=await page.locator('body').innerText();
  await page.locator('a[href="/reportar/ubicacion"]').click();
  await page.waitForURL('**/reportar/ubicacion');
  const after=await point();
  out.cases.push({id:'MAP-08',passed:JSON.stringify(second)===JSON.stringify(after),detailsObserved:detailText.includes('Descripción y fotografías')});
  await page.screenshot({path:'output/playwright/not65-desktop-08-retorno.png',fullPage:true});
  for(const [width,height] of [[360,800],[1366,768]]){
    await page.setViewportSize({width,height});
    await page.evaluate(()=>scrollTo(0,0));
    const measure=await page.evaluate(()=>{
      const rect=e=>{const r=e.getBoundingClientRect(),s=getComputedStyle(e);return {x:r.x,y:r.y,width:r.width,height:r.height,fontSize:s.fontSize,overflowX:s.overflowX}};
      const m=document.querySelector('.mapa-ubicacion');
      return {innerWidth,innerHeight,dpr:devicePixelRatio,clientWidth:document.documentElement.clientWidth,scrollWidth:document.documentElement.scrollWidth,map:rect(m),zoom:[...document.querySelectorAll('.leaflet-control-zoom a')].map(rect),gps:[...document.querySelectorAll('button')].filter(b=>b.innerText.includes('Volver a obtener')).map(rect),horizontalOverflow:document.documentElement.scrollWidth>document.documentElement.clientWidth};
    });
    await page.screenshot({path:`output/playwright/not65-responsive-${width}-inicio.png`});
    await canvas.scrollIntoViewIfNeeded();
    await page.screenshot({path:`output/playwright/not65-responsive-${width}-mapa.png`});
    out.responsive.push({id:`RESP-${width}`,requested:{width,height},measured:measure,passed:measure.innerWidth===width&&measure.innerHeight===height&&!measure.horizontalOverflow});
  }
  out.permission=await page.evaluate(async()=>(await navigator.permissions.query({name:'geolocation'})).state);
  return out;
}
